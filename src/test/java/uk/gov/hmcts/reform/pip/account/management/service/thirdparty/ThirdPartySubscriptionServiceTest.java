package uk.gov.hmcts.reform.pip.account.management.service.thirdparty;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.database.ApiSubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiSubscription;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThirdPartySubscriptionServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private ApiSubscriptionRepository apiSubscriptionRepository;

    @InjectMocks
    private ThirdPartySubscriptionService service;

    @Test
    void testCreateThirdPartySubscriptions() {
        ApiSubscription sub1 = new ApiSubscription();
        ApiSubscription sub2 = new ApiSubscription();
        List<ApiSubscription> subs = Arrays.asList(sub1, sub2);

        when(apiSubscriptionRepository.saveAll(subs)).thenReturn(subs);

        List<ApiSubscription> result = service.createThirdPartySubscriptions(subs);

        assertThat(result)
            .as("Should return the saved subscriptions")
            .isEqualTo(subs);
        verify(apiSubscriptionRepository).saveAll(subs);
    }

    @Test
    void testFindThirdPartySubscriptionsByUserIdWhenSubscriptionsExist() {
        List<ApiSubscription> subs = Collections.singletonList(new ApiSubscription());
        when(apiSubscriptionRepository.findAllByUserId(USER_ID)).thenReturn(subs);

        List<ApiSubscription> result = service.findThirdPartySubscriptionsByUserId(USER_ID);

        assertThat(result)
            .as("Should return the found subscriptions")
            .isEqualTo(subs);

        verify(apiSubscriptionRepository).findAllByUserId(USER_ID);
    }

    @Test
    void testFindThirdPartySubscriptionsByUserIdWhenNoSubscriptionsExist() {
        when(apiSubscriptionRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.findThirdPartySubscriptionsByUserId(USER_ID))
            .as("Should throw NotFoundException if no subscriptions found")
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining(USER_ID.toString());
    }

    @Test
    void testUpdateThirdPartySubscriptionsByUserId() {
        List<ApiSubscription> subs = Collections.singletonList(new ApiSubscription());

        service.updateThirdPartySubscriptionsByUserId(USER_ID, subs);

        verify(apiSubscriptionRepository).deleteAllByUserId(USER_ID);
        verify(apiSubscriptionRepository).saveAll(subs);
    }

    @Test
    void testDeleteThirdPartySubscriptionsByUserId() {
        service.deleteThirdPartySubscriptionsByUserId(USER_ID);

        verify(apiSubscriptionRepository).deleteAllByUserId(USER_ID);
    }
}
