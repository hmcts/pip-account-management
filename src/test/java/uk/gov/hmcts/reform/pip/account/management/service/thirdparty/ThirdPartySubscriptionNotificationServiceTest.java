package uk.gov.hmcts.reform.pip.account.management.service.thirdparty;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.database.ApiOauthConfigurationRepository;
import uk.gov.hmcts.reform.pip.account.management.database.ApiSubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiOauthConfiguration;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiSubscription;
import uk.gov.hmcts.reform.pip.account.management.service.PublicationService;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartyAction;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartySubscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThirdPartySubscriptionNotificationServiceTest {
    private static final UUID ARTEFACT_ID1 = UUID.randomUUID();
    private static final UUID ARTEFACT_ID2 = UUID.randomUUID();
    private static final UUID USER_ID1 = UUID.randomUUID();
    private static final UUID USER_ID2 = UUID.randomUUID();
    private static final String DESTINATION_URL1 = "https://destination.url1.com";
    private static final String DESTINATION_URL2 = "https://destination.url2.com";
    private static final String TOKEN_URL1 = "https://token.url1.com";
    private static final String TOKEN_URL2 = "https://token.url2.com";
    private static final String CLIENT_ID_KEY1 = "client-id-1";
    private static final String CLIENT_ID_KEY2 = "client-id-2";
    private static final String CLIENT_SECRET_KEY1 = "client-secret-1";
    private static final String CLIENT_SECRET_KEY2 = "client-secret-2";
    private static final String SCOPE_KEY1 = "scope-1";
    private static final String SCOPE_KEY2 = "scope-2";
    private static final String ERROR_LOG_MESSAGE = "Error log does not match";

    private Artefact artefact1 = new Artefact();
    private Artefact artefact2 = new Artefact();
    private ApiSubscription apiSubscription1 = new ApiSubscription();
    private ApiSubscription apiSubscription2 = new ApiSubscription();
    private ApiOauthConfiguration apiOauthConfiguration1 = new ApiOauthConfiguration();
    private ApiOauthConfiguration apiOauthConfiguration2 = new ApiOauthConfiguration();

    private final LogCaptor logCaptor = LogCaptor.forClass(ThirdPartySubscriptionNotificationService.class);

    @Mock
    private ApiSubscriptionRepository apiSubscriptionRepository;

    @Mock
    private ApiOauthConfigurationRepository apiOauthConfigurationRepository;

    @Mock
    private PublicationService publicationService;

    @InjectMocks
    private ThirdPartySubscriptionNotificationService thirdPartySubscriptionNotificationService;

    @BeforeEach
    void setup() {
        artefact1.setArtefactId(ARTEFACT_ID1);
        artefact1.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);
        artefact1.setSensitivity(Sensitivity.PRIVATE);
        artefact1.setSupersededCount(0);

        artefact2.setArtefactId(ARTEFACT_ID2);
        artefact2.setListType(ListType.FAMILY_DAILY_CAUSE_LIST);
        artefact2.setSensitivity(Sensitivity.CLASSIFIED);
        artefact2.setSupersededCount(1);

        apiSubscription1.setUserId(USER_ID1);
        apiSubscription1.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);
        apiSubscription1.setSensitivity(Sensitivity.PRIVATE);

        apiSubscription2.setUserId(USER_ID2);
        apiSubscription2.setListType(ListType.FAMILY_DAILY_CAUSE_LIST);
        apiSubscription2.setSensitivity(Sensitivity.CLASSIFIED);

        apiOauthConfiguration1.setUserId(USER_ID1);
        apiOauthConfiguration1.setDestinationUrl(DESTINATION_URL1);
        apiOauthConfiguration1.setTokenUrl(TOKEN_URL1);
        apiOauthConfiguration1.setClientIdKey(CLIENT_ID_KEY1);
        apiOauthConfiguration1.setClientSecretKey(CLIENT_SECRET_KEY1);
        apiOauthConfiguration1.setScopeKey(SCOPE_KEY1);

        apiOauthConfiguration2.setUserId(USER_ID2);
        apiOauthConfiguration2.setDestinationUrl(DESTINATION_URL2);
        apiOauthConfiguration2.setTokenUrl(TOKEN_URL2);
        apiOauthConfiguration2.setClientIdKey(CLIENT_ID_KEY2);
        apiOauthConfiguration2.setClientSecretKey(CLIENT_SECRET_KEY2);
        apiOauthConfiguration2.setScopeKey(SCOPE_KEY2);
    }

    @Test
    void testHandleThirdPartySubscriptionForNewPublication() {
        when(apiSubscriptionRepository.findByListTypeAndSensitivityIn(
            ListType.CIVIL_DAILY_CAUSE_LIST, List.of(Sensitivity.CLASSIFIED, Sensitivity.PRIVATE))
        ).thenReturn(List.of(apiSubscription1));

        when(apiOauthConfigurationRepository.findByUserId(USER_ID1)).thenReturn(Optional.of(apiOauthConfiguration1));

        thirdPartySubscriptionNotificationService.handleThirdPartySubscription(artefact1);

        verify(publicationService).sendThirdPartySubscription(new ThirdPartySubscription(
            any(), ARTEFACT_ID1, ThirdPartyAction.NEW_PUBLICATION
        ));
        assertThat(logCaptor.getErrorLogs())
            .as(ERROR_LOG_MESSAGE)
            .isEmpty();
    }

    @Test
    void testHandleThirdPartySubscriptionForUpdatedPublication() {
        when(apiSubscriptionRepository.findByListTypeAndSensitivityIn(
            ListType.FAMILY_DAILY_CAUSE_LIST, List.of(Sensitivity.CLASSIFIED))
        ).thenReturn(List.of(apiSubscription2));

        when(apiOauthConfigurationRepository.findByUserId(USER_ID2)).thenReturn(Optional.of(apiOauthConfiguration2));

        thirdPartySubscriptionNotificationService.handleThirdPartySubscription(artefact2);

        verify(publicationService).sendThirdPartySubscription(new ThirdPartySubscription(
            any(), ARTEFACT_ID2, ThirdPartyAction.UPDATE_PUBLICATION
        ));
        assertThat(logCaptor.getErrorLogs())
            .as(ERROR_LOG_MESSAGE)
            .isEmpty();
    }

    @Test
    void testHandleThirdPartySubscriptionForDeletedPublication() {
        when(apiSubscriptionRepository.findByListTypeAndSensitivityIn(
            ListType.CIVIL_DAILY_CAUSE_LIST, List.of(Sensitivity.CLASSIFIED, Sensitivity.PRIVATE))
        ).thenReturn(List.of(apiSubscription1));

        when(apiOauthConfigurationRepository.findByUserId(USER_ID1)).thenReturn(Optional.of(apiOauthConfiguration1));

        thirdPartySubscriptionNotificationService.handleThirdPartySubscriptionForDeletedPublication(artefact1);

        verify(publicationService).sendThirdPartySubscription(new ThirdPartySubscription(
            any(), ARTEFACT_ID1, ThirdPartyAction.DELETE_PUBLICATION
        ));
        assertThat(logCaptor.getErrorLogs())
            .as(ERROR_LOG_MESSAGE)
            .isEmpty();
    }

    @Test
    void testHandleThirdPartySubscriptionWithNoOauthConfiguration() {
        when(apiSubscriptionRepository.findByListTypeAndSensitivityIn(
            ListType.CIVIL_DAILY_CAUSE_LIST, List.of(Sensitivity.CLASSIFIED, Sensitivity.PRIVATE))
        ).thenReturn(List.of(apiSubscription1));

        when(apiOauthConfigurationRepository.findByUserId(USER_ID1)).thenReturn(Optional.empty());

        thirdPartySubscriptionNotificationService.handleThirdPartySubscription(artefact1);

        verify(publicationService, never()).sendThirdPartySubscription(any());
        assertThat(logCaptor.getErrorLogs())
            .as(ERROR_LOG_MESSAGE)
            .hasSize(1)
            .anySatisfy(log ->
                assertThat(log).contains("No OAuth configuration found for third-party user with ID " + USER_ID1));
    }
}
