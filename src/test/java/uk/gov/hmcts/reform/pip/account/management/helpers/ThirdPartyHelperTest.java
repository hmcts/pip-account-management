package uk.gov.hmcts.reform.pip.account.management.helpers;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ThirdPartyHelperTest {
    @ParameterizedTest
    @MethodSource("parameters")
    void shouldCreateSecretKeyPrefix(String name, String userId, String expected) {
        assertThat(ThirdPartyHelper.createSecretKeyPrefix(name, userId))
            .as("Secret key prefix value does not match")
            .isEqualTo(expected);
    }

    private static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of("abc", "123", "abc-123-"),
            Arguments.of("ab@c.com", "12   3", "ab-c-com-12-3-"),
            Arguments.of("a---bC", "123---", "a-bc-123-"),
            Arguments.of("ABC@abc.com", "1-2-3-4", "abc-abc-com-1-2-3-4-")
        );
    }
}
