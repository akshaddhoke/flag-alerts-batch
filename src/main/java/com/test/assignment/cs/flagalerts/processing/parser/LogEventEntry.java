package com.test.assignment.cs.flagalerts.processing.parser;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.sql.DataSource;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Bean for Log Event Entry
 * @see ParseLogEntryStepConfiguration
 */
@Data
public class LogEventEntry {

    @NotBlank
    private String id;
    @NotNull
    private LogState state;
    @NotNull
    @Min(1)
    private Long timestamp;
    private String type;
    private String host;

    /**
     * Returns the state value as string. Used
     * in the writer for mapping value in insert statement
     * {@link ParseLogEntryStepConfiguration#finishedLogEventEntryWriter(DataSource)}
     */
    @JsonIgnore
    public String getStateAsString() {
        return this.state.getState();
    }

}
