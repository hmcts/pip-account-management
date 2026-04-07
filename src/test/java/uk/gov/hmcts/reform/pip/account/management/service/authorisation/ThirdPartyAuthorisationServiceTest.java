package uk.gov.hmcts.reform.pip.account.management.service.authorisation;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThirdPartyAuthorisationServiceTest {
    private static final UUID REQUESTER_ID = UUID.randomUUID();
    private static final String ERROR_LOG = "User with ID %s is not authorised to manage third parties";
    private static final String CAN_MANAGE_THIRD_PARTY_MESSAGE = "User should be able to manage third parties";
    private static final String CANNOT_MANAGE_THIRD_PARTY_MESSAGE = "User should not be able to manage third parties";
    private static final String LOG_EMPTY_MESSAGE = "Error log should be empty";
    private static final String LOG_NOT_EMPTY_MESSAGE = "Error log should not be empty";
    private static final String LOG_MATCHED_MESSAGE = "Error log message does not match";

    private static PiUser requester = new PiUser();

    @Mock
    private AuthorisationCommonService authorisationCommonService;

    @InjectMocks
    private ThirdPartyAuthorisationService thirdPartyAuthorisationService;

    @BeforeAll
    static void setup() {
        requester.setUserId(REQUESTER_ID);
    }

    @BeforeEach
    void setupEach() {
        lenient().when(authorisationCommonService.hasOAuthAdminRole()).thenReturn(true);
    }

    @Test
    void testSystemAdminCanManageThirdParty() {
        requester.setRoles(Roles.SYSTEM_ADMIN);
        when(authorisationCommonService.hasOAuthAdminRole()).thenReturn(true);
        when(authorisationCommonService.isSystemAdmin(REQUESTER_ID)).thenReturn(true);

        try (LogCaptor logCaptor = LogCaptor.forClass(ThirdPartyAuthorisationService.class)) {
            assertThat(thirdPartyAuthorisationService.userCanManageThirdParty(REQUESTER_ID))
                .as(CAN_MANAGE_THIRD_PARTY_MESSAGE)
                .isTrue();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @Test
    void testNonSystemAdminCannotManageThirdParty() {
        requester.setRoles(Roles.SYSTEM_ADMIN);
        when(authorisationCommonService.hasOAuthAdminRole()).thenReturn(true);
        when(authorisationCommonService.isSystemAdmin(REQUESTER_ID)).thenReturn(false);

        try (LogCaptor logCaptor = LogCaptor.forClass(ThirdPartyAuthorisationService.class)) {
            assertThat(thirdPartyAuthorisationService.userCanManageThirdParty(REQUESTER_ID))
                .as(CANNOT_MANAGE_THIRD_PARTY_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().get(0))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(ERROR_LOG, REQUESTER_ID));
        }
    }

    @Test
    void testUserCannotManageThirdPartyIfNoOAuthAdminRole() {
        requester.setRoles(Roles.SYSTEM_ADMIN);
        when(authorisationCommonService.hasOAuthAdminRole()).thenReturn(false);

        try (LogCaptor logCaptor = LogCaptor.forClass(ThirdPartyAuthorisationService.class)) {
            assertThat(thirdPartyAuthorisationService.userCanManageThirdParty(REQUESTER_ID))
                .as(CANNOT_MANAGE_THIRD_PARTY_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().get(0))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(ERROR_LOG, REQUESTER_ID));
        }
    }
}
