package com.test.assignment.cs.flagalerts.processing.alerts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.validator.BeanValidatingItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Batch Step Configuration for flagging alerts: <br>
 * 1. Reading the events from tables TMP_LOG_EVENT_FINISHED, TMP_LOG_EVENT_STARTED joined by EVENT_ID - {@link #logAlertsJdbcReader(DataSource, LogEventAlertRowMapper)},<br>
 * 2. Inserting {@link LogEventAlert} to table LOG_EVENT_ALERT - {@link #logAlertsJdbcWriter(DataSource)}
 */
@Configuration
@Slf4j
public class FlagAlertStepConfiguration {

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    /**
     * Step configuration for flagging alerts and inserting alerts into LOG_EVENT_ALERT
     *
     * @param logAlertsJdbcReader {@link #logAlertsJdbcReader(DataSource, LogEventAlertRowMapper)}
     * @param logAlertsValidator  {@link #logAlertValidator()}
     * @param logAlertsJdbcWriter {@link #logAlertsJdbcWriter(DataSource)}
     */
    @Bean("flagEventsForAlertsStep")
    public Step flagEventsForAlertsStep(JdbcCursorItemReader<LogEventAlert> logAlertsJdbcReader,
                                        BeanValidatingItemProcessor<LogEventAlert> logAlertsValidator,
                                        JdbcBatchItemWriter<LogEventAlert> logAlertsJdbcWriter) {
        return stepBuilderFactory.get("flagEventsForAlertsStep")
                .<LogEventAlert, LogEventAlert>chunk(10)
                .reader(logAlertsJdbcReader)
                .processor(logAlertsValidator)
                .writer(logAlertsJdbcWriter)
                .build();
    }

    /**
     * JDBC reader for reading alerts from TMP_LOG_EVENT_FINISHED, TMP_LOG_EVENT_STARTED joined by EVENT_ID
     *
     * @param logEventAlertRowMapper mapper with threshold of event duration for flagging event as alert. Defaults to 4 ms
     */
    @Bean("logAlertsJdbcReader")
    public JdbcCursorItemReader<LogEventAlert> logAlertsJdbcReader(
            DataSource dataSource, LogEventAlertRowMapper logEventAlertRowMapper) {

        final String joinEntriesForAlertsSql =
                "SELECT fe.EVENT_ID, fe.EVENT_TIMESTAMP - se.EVENT_TIMESTAMP as EVENT_DURATION, fe.EVENT_TYPE, fe.EVENT_HOST " +
                        " FROM TMP_LOG_EVENT_FINISHED fe, TMP_LOG_EVENT_STARTED se where se.EVENT_ID=fe.EVENT_ID";

        log.debug("Initializing Log Alert JDBC reader with SQL - {}", joinEntriesForAlertsSql);
        return new JdbcCursorItemReaderBuilder<LogEventAlert>()
                .dataSource(dataSource)
                .fetchSize(100)
                .name("logEntriesForAlertsJdbcReader")
                .sql(joinEntriesForAlertsSql)
                .rowMapper(logEventAlertRowMapper)
                .build();

    }

    /**
     * JDBC writer for persisting found alerts into Table LOG_EVENT_ALERT
     */
    @Bean
    public JdbcBatchItemWriter<LogEventAlert> logAlertsJdbcWriter(DataSource dataSource) {

        final String insertLogAlertSql =
                "INSERT INTO LOG_EVENT_ALERT (EVENT_ID, EVENT_DURATION, EVENT_HOST, EVENT_TYPE, ALERT) " +
                        "VALUES (:eventId, :eventDuration, :eventHost, :eventType, :alert)";
        log.debug("Initializing Log Alert JDBC writer with SQL - {}", insertLogAlertSql);

        return new JdbcBatchItemWriterBuilder<LogEventAlert>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql(insertLogAlertSql)
                .dataSource(dataSource)
                .build();
    }

    /**
     * JSR Bean Validator for {@link LogEventAlert}
     */
    @Bean
    public BeanValidatingItemProcessor<LogEventAlert> logAlertValidator() {
        return new BeanValidatingItemProcessor<>();
    }

    @Bean
    public LogEventAlertRowMapper logEventAlertRowMapper(
            @Value("${flag-alerts.alerts.event-duration.threshold-ms:4}") Long alertThreshold) {
        return new LogEventAlertRowMapper(alertThreshold);
    }


}
