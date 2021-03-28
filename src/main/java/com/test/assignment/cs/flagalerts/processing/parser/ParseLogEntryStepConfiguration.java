package com.test.assignment.cs.flagalerts.processing.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.support.ClassifierCompositeItemWriter;
import org.springframework.batch.item.support.builder.ClassifierCompositeItemWriterBuilder;
import org.springframework.batch.item.validator.BeanValidatingItemProcessor;
import org.springframework.batch.item.validator.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import javax.sql.DataSource;

/**
 * Batch Step Configuration for: <br>
 * 1. Reading the log entries from logfile - {@link #logEventFileReader(String)},<br>
 * 2. Persisting them by state to temporary tables TMP_LOG_EVENT_FINISHED, TMP_LOG_EVENT_STARTED - {@link #logEventJdbcWriter(JdbcBatchItemWriter, JdbcBatchItemWriter)}
 */
@Configuration
@Slf4j
public class ParseLogEntryStepConfiguration {

    public static final String PARAM_LOG_EVENT_FILE_READER = "log-events.file";
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    /**
     * Fault tolerant step configuration for parsing log entries, and persisting to temporary table
     *
     * @param logEventJdbcWriter          {@link #logEventJdbcWriter(JdbcBatchItemWriter, JdbcBatchItemWriter)}
     * @param logEntryValidator           {@link #logEntryValidator()}
     * @param invalidLogEntrySkipListener {@link InvalidLogEntrySkipListener}
     * @param skipLimit                   Number of records with exceptions to be skipped before job failure
     */
    @Bean("parseLogsEntriesStep")
    public Step parseLogsEntriesStep(ClassifierCompositeItemWriter<LogEventEntry> logEventJdbcWriter,
                                     BeanValidatingItemProcessor<LogEventEntry> logEntryValidator,
                                     InvalidLogEntrySkipListener invalidLogEntrySkipListener,
                                     @Value("${flag-alerts.parser.invalid-entry.skip-limit:10}") int skipLimit) {

        return stepBuilderFactory.get("parseLogsEntriesStep")
                .<LogEventEntry, LogEventEntry>chunk(10)
                .reader(logEventFileReader(null))
                .processor(logEntryValidator)
                .writer(logEventJdbcWriter)
                .faultTolerant()
                .skip(FlatFileParseException.class)
                .skip(ValidationException.class)
                .skipLimit(skipLimit)
                .listener(invalidLogEntrySkipListener)
                .build();
    }

    /**
     * Log file reader, mapping parsed json records to {@link LogEventEntry}
     *
     * @param inputLogEventsFile input log events file. Configurable via Job Parameter "log-events.file"
     */
    @Bean
    @JobScope
    public FlatFileItemReader<LogEventEntry> logEventFileReader(@Value("#{jobParameters['log-events.file']}") String inputLogEventsFile) {
        if(StringUtils.isEmpty(inputLogEventsFile)) {
            inputLogEventsFile = "logfile.txt";
        }
        log.info("Initializing logEventFileReader for file {}", inputLogEventsFile);
        return new FlatFileItemReaderBuilder<LogEventEntry>()
                .name("logEventFileReader")
                .resource(new FileSystemResource(inputLogEventsFile))
                .lineMapper((line, lineNumber) -> objectMapper.readValue(line, LogEventEntry.class))
                .build();
    }

    /**
     * Based on state, persists the log event entry to TMP_LOG_EVENT_FINISHED, TMP_LOG_EVENT_STARTED
     *
     * @param startedLogEventEntryWriter  {@link #startedLogEventEntryWriter(DataSource)}
     * @param finishedLogEventEntryWriter {@link #finishedLogEventEntryWriter(DataSource)}
     */
    @Bean
    public ClassifierCompositeItemWriter<LogEventEntry> logEventJdbcWriter(
            @Qualifier("startedLogEventEntryWriter") JdbcBatchItemWriter<LogEventEntry> startedLogEventEntryWriter,
            @Qualifier("finishedLogEventEntryWriter") JdbcBatchItemWriter<LogEventEntry> finishedLogEventEntryWriter) {

        return new ClassifierCompositeItemWriterBuilder<LogEventEntry>()
                .classifier(logEventEntry -> LogState.FINISHED.equals(logEventEntry.getState()) ? finishedLogEventEntryWriter : startedLogEventEntryWriter)
                .build();
    }

    /**
     * Jdbc writer for STARTED state log entry to TMP_LOG_EVENT_STARTED
     */
    @Bean("startedLogEventEntryWriter")
    public JdbcBatchItemWriter<LogEventEntry> startedLogEventEntryWriter(DataSource dataSource) {
        return createLogEventEntryJdbcWriter(dataSource, LogState.STARTED.getState());
    }

    /**
     * Jdbc writer for FINISHED State log entry to TMP_LOG_EVENT_FINISHED
     */
    @Bean("finishedLogEventEntryWriter")
    public JdbcBatchItemWriter<LogEventEntry> finishedLogEventEntryWriter(DataSource dataSource) {
        return createLogEventEntryJdbcWriter(dataSource, LogState.FINISHED.getState());
    }

    /**
     * Creates a JdbcBatchItemWriter for inserting log entries to TMP_LOG_EVENT_[STATE]
     */
    private JdbcBatchItemWriter<LogEventEntry> createLogEventEntryJdbcWriter(DataSource dataSource, final String state) {

        final String insertLogEntrySql =
                String.format("INSERT INTO TMP_LOG_EVENT_%s (EVENT_ID, EVENT_STATE, EVENT_TIMESTAMP, EVENT_HOST, EVENT_TYPE) " +
                        "VALUES (:id, :stateAsString, :timestamp, :host, :type)", state);
        log.debug("Initializing Log Event JDBC writer with SQL - {}", insertLogEntrySql);

        return new JdbcBatchItemWriterBuilder<LogEventEntry>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql(insertLogEntrySql)
                .dataSource(dataSource)
                .build();
    }

    /**
     * JSR Bean Validator for {@link LogEventEntry}
     */
    @Bean
    public BeanValidatingItemProcessor<LogEventEntry> logEntryValidator() {
        return new BeanValidatingItemProcessor<>();
    }

}
