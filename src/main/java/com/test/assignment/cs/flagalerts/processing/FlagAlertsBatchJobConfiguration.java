package com.test.assignment.cs.flagalerts.processing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Batch Job consisting of below steps:
 * 1. Parsing the logfile for Log Events {@link com.test.assignment.cs.flagalerts.processing.parser.ParseLogEntryStepConfiguration#parseLogsEntriesStep}
 * 2. Flag Events and Persist found Event Alerts into LOG_EVENT_ALERT Table {@link com.test.assignment.cs.flagalerts.processing.alerts.FlagAlertStepConfiguration#flagEventsForAlertsStep}
 */
@Configuration
@EnableBatchProcessing
@Slf4j
public class FlagAlertsBatchJobConfiguration {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Bean
    public Job parseLogEventsForAlertsJob(JobCompletionNotificationListener listener,
                                          @Qualifier("parseLogsEntriesStep") Step parseLogsEntriesStep,
                                          @Qualifier("flagEventsForAlertsStep") Step flagEventsForAlertsStep) {
        return jobBuilderFactory.get("parseLogEventsForAlertsJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(parseLogsEntriesStep)
                .next(flagEventsForAlertsStep)
                .end()
                .build();
    }


}
