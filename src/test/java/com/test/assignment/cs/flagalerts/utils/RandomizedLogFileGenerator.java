package com.test.assignment.cs.flagalerts.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.assignment.cs.flagalerts.processing.parser.LogEventEntry;
import com.test.assignment.cs.flagalerts.processing.parser.LogState;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@UtilityClass
public class RandomizedLogFileGenerator {
    public void generateLogFile(String fileName, long maxFileSizeBytes) throws IOException {
        Path filePath = Paths.get(fileName);
        createEmptyFile(filePath);

        writeLogEntriesToFile(filePath, maxFileSizeBytes);
    }

    private static void writeLogEntriesToFile(Path filePath, long maxFileSize) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        try (BufferedWriter bw = Files.newBufferedWriter(filePath)) {
            while (Files.size(filePath) < maxFileSize) {
                for (LogEventEntry logEventEntry : getRandomizedLogEntries(10)) {
                    bw.write(objectMapper.writeValueAsString(logEventEntry));
                    bw.newLine();
                }
            }
        }
    }

    private List<LogEventEntry> getRandomizedLogEntries(int logEntryPairCount) {
        List<LogEventEntry> logEventEntries = new ArrayList<>(logEntryPairCount*2);
        for (int i = 0; i < logEntryPairCount; i++) {
            logEventEntries.addAll(createLogEntryPair());
        }
        Collections.shuffle(logEventEntries);
        return logEventEntries;
    }

    private List<LogEventEntry> createLogEntryPair() {
        String id = UUID.randomUUID().toString();
        Long timeStamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        String host = null;
        String type = null;
        if(new Random().nextBoolean()) {
            host = RandomStringUtils.randomAlphabetic(10);
            type = RandomStringUtils.randomAlphabetic(10);
        }

        LogEventEntry startedLogEventEntry = new LogEventEntry();
        startedLogEventEntry.setId(id);
        startedLogEventEntry.setState(LogState.STARTED);
        startedLogEventEntry.setTimestamp(timeStamp);
        startedLogEventEntry.setHost(host);
        startedLogEventEntry.setType(type);

        LogEventEntry finishedLogEventEntry = new LogEventEntry();
        finishedLogEventEntry.setId(id);
        finishedLogEventEntry.setState(LogState.FINISHED);
        finishedLogEventEntry.setTimestamp(timeStamp+Double.valueOf(Math.random()*10).longValue());
        finishedLogEventEntry.setHost(host);
        finishedLogEventEntry.setType(type);

        return Arrays.asList(startedLogEventEntry, finishedLogEventEntry);

    }

    private void createEmptyFile(Path filePath) throws IOException {
        if(filePath.toFile().exists()) {
            filePath.toFile().delete();
        }
        Files.createFile(filePath);
    }
}
