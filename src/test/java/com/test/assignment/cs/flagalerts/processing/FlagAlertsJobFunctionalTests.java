package com.test.assignment.cs.flagalerts.processing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.assignment.cs.flagalerts.processing.parser.ParseLogEntryStepConfiguration;
import com.test.assignment.cs.flagalerts.utils.RandomizedLogFileGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Functional/ Integration tests for the flag alerts job
 */
@ActiveProfiles("test")
@SpringBootTest({"spring.batch.job.enabled=false", "flag-alerts.parser.invalid-entry.skip-limit=1"})
@SpringBatchTest
@Slf4j
public class FlagAlertsJobFunctionalTests {

    public static final String SQL_SELECT_FROM_LOG_EVENT_ALERT = "SELECT EVENT_ID,EVENT_DURATION,EVENT_HOST,EVENT_TYPE,ALERT from LOG_EVENT_ALERT";
    public static final String SQL_COUNT_EVENT = "SELECT COUNT(EVENT_ID) from %s";

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void truncateLogAlertTable() {
        log.debug("truncating LOG_EVENT_ALERT, TMP_LOG_EVENT_FINISHED, TMP_LOG_EVENT_STARTED");
        jdbcTemplate.execute("truncate table LOG_EVENT_ALERT");
        jdbcTemplate.execute("truncate table TMP_LOG_EVENT_FINISHED");
        jdbcTemplate.execute("truncate table TMP_LOG_EVENT_STARTED");
    }

    /**
     * Tests the job execution for data shared as example in assignment
     */
    @Test
    public void testJobExecution_valid_assignmentExample() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(getJobParameters("./src/test/resources/logfile-assignment-example.txt"));
        Assert.assertEquals("COMPLETED", jobExecution.getExitStatus().getExitCode());

        // JSON Assert with ignore order will match the alert entry values in table against expected values
        List<Map<String, Object>> actualLogAlerts = jdbcTemplate.queryForList(SQL_SELECT_FROM_LOG_EVENT_ALERT);
        String actualLogAlertsJson = objectMapper.writeValueAsString(actualLogAlerts);
        String expectedLogAlertsJson = new String(Files.readAllBytes(Paths.get("./src/test/resources/logfile-assignment-example-expected.json")), StandardCharsets.UTF_8);
        JSONAssert.assertEquals(expectedLogAlertsJson, actualLogAlertsJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    /**
     * Generates a randomized valid log file( approx 10KB), and does simple validation on count in output tables
     */
    @Test
    public void testJobExecution_valid_generatedFile_small() throws Exception {
        // Tests approx 10 KB file. Use a large size to generate a file for stress test
        testJobExecution_valid_generatedFile((long) 10 * 1024);
    }

    private void testJobExecution_valid_generatedFile(long fileSizeBytes) throws Exception {
        final Path tempLogFile = Files.createTempFile("logfile", ".txt");
        tempLogFile.toFile().deleteOnExit();
        RandomizedLogFileGenerator.generateLogFile(tempLogFile.toString(), fileSizeBytes);

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(getJobParameters(tempLogFile.toString()));
        Assert.assertEquals("COMPLETED", jobExecution.getExitStatus().getExitCode());

        Long logAlertRowCount = jdbcTemplate.queryForObject(String.format(SQL_COUNT_EVENT, "LOG_EVENT_ALERT"), Long.class);
        Long logFinishedEntryCount = jdbcTemplate.queryForObject(String.format(SQL_COUNT_EVENT, "TMP_LOG_EVENT_FINISHED"), Long.class);
        Long logStartedEntryCount = jdbcTemplate.queryForObject(String.format(SQL_COUNT_EVENT, "TMP_LOG_EVENT_STARTED"), Long.class);

        Assert.assertEquals("Finished and started entry count mismatch", logFinishedEntryCount, logStartedEntryCount);
        Assert.assertEquals("Log Alerts != Finished count", logFinishedEntryCount, logAlertRowCount);
    }

    /**
     * Tests for fault tolerance. One invalid record will be skipped, and job marked as complete
     */
    @Test
    public void testJobExecution_skip_invalid_complete() throws Exception {

        final Path tempLogFile = Files.createTempFile("logfile", ".txt");
        tempLogFile.toFile().deleteOnExit();
        Files.write(tempLogFile, "{testInvalid}".getBytes(StandardCharsets.UTF_8));
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(getJobParameters(tempLogFile.toString()));
        Assert.assertEquals("COMPLETED", jobExecution.getExitStatus().getExitCode());

        List<Map<String, Object>> actualLogAlerts = jdbcTemplate.queryForList(SQL_SELECT_FROM_LOG_EVENT_ALERT);
        Assert.assertEquals("No Alerts were expected", 0, actualLogAlerts.size());
    }

    /**
     * Tests for fault tolerance beyond the configured skip limit(1) for tests.
     * Job should be marked as failed, with no entries in the alert table
     */
    @Test
    public void testJobExecution_skipLimit_fail() throws Exception {

        final Path tempLogFile = Files.createTempFile("logfile", ".txt");
        tempLogFile.toFile().deleteOnExit();
        Files.write(tempLogFile, String.format("{testInvalid}%n{testInvalid2}").getBytes(StandardCharsets.UTF_8));
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(getJobParameters(tempLogFile.toString()));
        Assert.assertEquals("FAILED", jobExecution.getExitStatus().getExitCode());

        List<Map<String, Object>> actualLogAlerts = jdbcTemplate.queryForList(SQL_SELECT_FROM_LOG_EVENT_ALERT);
        Assert.assertEquals("No Alerts were expected", 0, actualLogAlerts.size());
    }

    private JobParameters getJobParameters(String logEventsFile) {
        JobParametersBuilder parametersBuilder = new JobParametersBuilder();
        parametersBuilder.addString(ParseLogEntryStepConfiguration.PARAM_LOG_EVENT_FILE_READER, logEventsFile);
        return parametersBuilder.toJobParameters();
    }
}