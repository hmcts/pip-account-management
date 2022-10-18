package uk.gov.hmcts.reform.pip.account.management.service.helpers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeHelper {
    private DateTimeHelper() {
    }

    public static LocalDateTime zonedDateTimeStringToLocalDateTime(String str) {
        Instant instant = Instant.parse(str);
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("UTC"));
        return zonedDateTime.toLocalDateTime();
    }

    public static String localDateTimeToDateString(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
    }
}
