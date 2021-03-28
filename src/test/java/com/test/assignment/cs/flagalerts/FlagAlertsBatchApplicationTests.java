package com.test.assignment.cs.flagalerts;

import com.test.assignment.cs.flagalerts.utils.RandomizedLogFileGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
@Slf4j
class FlagAlertsBatchApplicationTests {

	@Test
	void contextLoads() throws IOException {
//		RandomizedLogFileGenerator.generateLogFile("D:\\Study\\Workspaces\\IdeaProjects\\log-analyzer\\log-analyzer\\src\\main\\resources\\logfile-generated.txt", (long)1024 * 1024 * 1024);
		log.debug("Application Context Loaded!");
	}

}
