package com.test.assignment.cs.flagalerts.processing.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.listener.SkipListenerSupport;
import org.springframework.stereotype.Component;

/**
 * Logs entries that were skipped during read(failed to be parsed), or processing( failed validation)
 */
@Component
@Slf4j
public class InvalidLogEntrySkipListener extends SkipListenerSupport<LogEventEntry, LogEventEntry> {

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("Skipped row as parsing failed while reading - {}", t.getMessage());
        log.debug("Skipped row stacktrace", t);
    }

    @Override
    public void onSkipInProcess(LogEventEntry item, Throwable t) {
        log.warn("Skipped row as validation failed while processing - {}", t.getMessage());
        log.debug("Skipped row stacktrace", t);
    }
}
