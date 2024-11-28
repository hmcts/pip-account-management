package uk.gov.hmcts.reform.pip.account.management.utils;

import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.pip.account.management.service.PublicationService;
import uk.gov.hmcts.reform.pip.account.management.service.SubscriptionService;

public class IntegrationTestBase extends IntegrationCommonTestBase {
    @MockBean
    protected SubscriptionService subscriptionService;

    @MockBean
    protected PublicationService publicationService;
}
