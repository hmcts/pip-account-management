package uk.gov.hmcts.reform.pip.account.management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ViewServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private ViewService viewService;

    @Test
    void refreshViewTest() {
        viewService.refreshView();
        verify(userRepository).refreshAccountView();
        verify(subscriptionRepository).refreshSubscriptionView();
    }

}
