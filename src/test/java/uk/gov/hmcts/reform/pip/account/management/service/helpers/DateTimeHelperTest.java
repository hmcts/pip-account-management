package uk.gov.hmcts.reform.pip.account.management.service.helpers;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateTimeHelperTest {
    @Test
    void testFormatValidDateTimeStringInGmt() {
        assertThat(DateTimeHelper.zonedDateTimeStringToLocalDateTime("2022-01-01T10:21:10.912Z"))
            .as("Incorrect date time conversion")
            .isEqualTo("2022-01-01T10:21:10.912");
    }

    @Test
    void testFormatValidDateTimeStringInBst() {
        assertThat(DateTimeHelper.zonedDateTimeStringToLocalDateTime("2022-08-01T10:21:10.912Z"))
            .as("Incorrect date time conversion")
            .isEqualTo("2022-08-01T10:21:10.912");
    }

    @Test
    void testFormatInvalidDateTimeString() {
        assertThatThrownBy(() -> DateTimeHelper.zonedDateTimeStringToLocalDateTime("NotDateTime"))
            .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    void testLocalDateTimeToDateString() {
        LocalDateTime dateTime = LocalDateTime.of(2022, 2, 10, 8, 30, 10, 20);
        assertThat(DateTimeHelper.localDateTimeToDateString(dateTime))
            .as("Date format does not match")
            .isEqualTo("10 February 2022");
    }
}
