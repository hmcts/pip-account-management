package uk.gov.hmcts.reform.pip.account.management.utils;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.pip.account.management.database.AuditRepository;
import uk.gov.hmcts.reform.pip.account.management.database.MediaApplicationRepository;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;

public class IntegrationBasicTestBase extends IntegrationTestBase {
    @MockitoBean
    protected UserRepository userRepository;

    @MockitoBean
    protected MediaApplicationRepository mediaApplicationRepository;

    @MockitoBean
    protected AuditRepository auditRepository;
}
