package uk.gov.hmcts.reform.pip.account.management.utils;

import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.pip.account.management.database.AuditRepository;
import uk.gov.hmcts.reform.pip.account.management.database.MediaApplicationRepository;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;

public class IntegrationBasicTestBase extends IntegrationCommonTestBase {
    @MockBean
    protected UserRepository userRepository;

    @MockBean
    protected MediaApplicationRepository mediaApplicationRepository;

    @MockBean
    protected AuditRepository auditRepository;
}
