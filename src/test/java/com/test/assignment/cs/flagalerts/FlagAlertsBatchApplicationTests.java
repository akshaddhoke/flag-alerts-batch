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
		log.debug("Application Context Loaded!");
	}

}
