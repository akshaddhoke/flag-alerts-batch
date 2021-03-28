package com.test.assignment.cs.flagalerts.processing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Job completion listener to log completion status
 */
@Component
@Slf4j
public class JobCompletionNotificationListener extends JobExecutionListenerSupport {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JobCompletionNotificationListener(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED && log.isInfoEnabled()) {
            log.info("!!! JOB FINISHED !!!");
            log.info("Event Count By Alert - {}", jdbcTemplate.queryForList("select ALERT, count(*) as EVENT_COUNT from LOG_EVENT_ALERT group by ALERT;"));
        }
    }
}
