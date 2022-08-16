package uk.gov.hmcts.reform.pip.account.management.service.helpers;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeHelperTest {
    @Test
    void testLocalDateTimeToDateString() {
        LocalDateTime dateTime = LocalDateTime.of(2022, 2, 10, 8, 30, 10, 20);
        assertThat(DateTimeHelper.localDateTimeToDateString(dateTime))
            .as("Date format does not match")
            .isEqualTo("10 February 2022");
    }
}
