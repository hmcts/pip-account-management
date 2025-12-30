package uk.gov.hmcts.reform.pip.account.management.utils;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.pip.account.management.database.ApiOauthConfigurationRepository;
import uk.gov.hmcts.reform.pip.account.management.database.ApiSubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.database.ApiUserRepository;
import uk.gov.hmcts.reform.pip.account.management.database.AuditRepository;
import uk.gov.hmcts.reform.pip.account.management.database.MediaApplicationRepository;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionListTypeRepository;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;

@ActiveProfiles(profiles = "integration-basic", inheritProfiles = false)
public class IntegrationBasicTestBase extends IntegrationTestBase {
    @MockitoBean
    protected UserRepository userRepository;

    @MockitoBean
    protected MediaApplicationRepository mediaApplicationRepository;

    @MockitoBean
    protected SubscriptionRepository subscriptionRepository;

    @MockitoBean
    protected SubscriptionListTypeRepository subscriptionListTypeRepository;

    @MockitoBean
    protected ApiUserRepository apiUserRepository;

    @MockitoBean
    protected ApiSubscriptionRepository apiSubscriptionRepository;

    @MockitoBean
    protected ApiOauthConfigurationRepository apiOauthConfigurationRepository;

    @MockitoBean
    protected AuditRepository auditRepository;
}
