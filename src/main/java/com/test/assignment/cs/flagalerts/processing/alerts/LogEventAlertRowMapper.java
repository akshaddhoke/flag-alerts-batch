package com.test.assignment.cs.flagalerts.processing.alerts;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

@RequiredArgsConstructor
public class LogEventAlertRowMapper implements RowMapper<LogEventAlert> {
	@NonNull
	final private Long alertThreshold;

	public static final String EVENT_ID_COLUMN = "EVENT_ID";
	public static final String EVENT_DURATION_COLUMN = "EVENT_DURATION";
	public static final String EVENT_TYPE_COLUMN = "EVENT_TYPE";
	public static final String EVENT_HOST_COLUMN = "EVENT_HOST";

	@Override
	public LogEventAlert mapRow(ResultSet rs, int rowNum) throws SQLException {
		LogEventAlert logEventAlert = new LogEventAlert();

		logEventAlert.setEventId(rs.getString(EVENT_ID_COLUMN));
		logEventAlert.setEventDuration(rs.getLong(EVENT_DURATION_COLUMN));
		logEventAlert.setEventType(rs.getString(EVENT_TYPE_COLUMN));
		logEventAlert.setEventHost(rs.getString(EVENT_HOST_COLUMN));
		logEventAlert.setAlertThreshold(alertThreshold);

		return logEventAlert;
	}
}