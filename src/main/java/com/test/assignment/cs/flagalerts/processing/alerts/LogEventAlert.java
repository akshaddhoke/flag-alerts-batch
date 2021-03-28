package com.test.assignment.cs.flagalerts.processing.alerts;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class LogEventAlert {

	@NotBlank
	private String eventId;
	@NotNull
	@Min(0)
	private Long eventDuration;
	private String eventType;
	private String eventHost;

	@Value("${flagalerts.alerts.event-duration.threshold-ms:4}")
	@Min(0)
	private Long alertThreshold;

	/**
	 * Used in TODO
	 * @return true if eventDuration > alertThreshold
	 */
	public boolean getAlert() {
		return eventDuration > alertThreshold;
	}
}
