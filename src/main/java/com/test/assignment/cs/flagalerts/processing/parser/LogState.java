package com.test.assignment.cs.flagalerts.processing.parser;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum LogState {
    STARTED("STARTED"),FINISHED("FINISHED");
    private final String state;
}
