package uk.gov.hmcts.reform.pip.account.management.service.helpers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeHelper {
    private DateTimeHelper() {
    }

    public static String localDateTimeToDateString(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
    }
}
