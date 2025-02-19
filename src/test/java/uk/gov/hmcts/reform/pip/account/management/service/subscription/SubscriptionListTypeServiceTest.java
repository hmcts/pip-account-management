package uk.gov.hmcts.reform.pip.account.management.service.subscription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionListTypeRepository;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionListType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.model.publication.ListType.CIVIL_DAILY_CAUSE_LIST;

@ExtendWith(MockitoExtension.class)
class SubscriptionListTypeServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String ACTIONED_USER_ID = "actionId1";
    private static final String SUBSCRIPTION_CREATED_ERROR = "The returned subscription does "
        + "not match the expected subscription";

    private SubscriptionListType mockSubscriptionListType;

    @Mock
    SubscriptionListTypeRepository subscriptionListTypeRepository;

    @InjectMocks
    SubscriptionListTypeService subscriptionListTypeService;

    @BeforeEach
    void setup() {
        mockSubscriptionListType = new SubscriptionListType(USER_ID, List.of(CIVIL_DAILY_CAUSE_LIST.name()),
                                                            List.of("ENGLISH"));
    }

    @Test
    void testConfigureListTypesForLocationSubscription() {
        subscriptionListTypeService.configureListTypesForSubscription(mockSubscriptionListType, ACTIONED_USER_ID);

        assertEquals(USER_ID, mockSubscriptionListType.getUserId(),
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testConfigureEmptyListTypesForLocationSubscription() {
        mockSubscriptionListType.setListType(new ArrayList<>());
        subscriptionListTypeService.configureListTypesForSubscription(mockSubscriptionListType, ACTIONED_USER_ID);

        assertEquals(USER_ID, mockSubscriptionListType.getUserId(),
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testAddListTypesForLocationSubscription() {
        when(subscriptionListTypeRepository.save(mockSubscriptionListType))
            .thenReturn(mockSubscriptionListType);
        subscriptionListTypeService.addListTypesForSubscription(mockSubscriptionListType, USER_ID.toString());

        assertEquals(USER_ID, mockSubscriptionListType.getUserId(),
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testDeleteSubscriptionListTypeIfExists() {
        when(subscriptionListTypeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(mockSubscriptionListType));

        subscriptionListTypeService.deleteListTypesForSubscription(USER_ID);
        verify(subscriptionListTypeRepository).deleteByUserId(USER_ID);
    }

    @Test
    void testDeleteSubscriptionListTypeIfNotExists() {
        when(subscriptionListTypeRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        subscriptionListTypeService.deleteListTypesForSubscription(USER_ID);
        verify(subscriptionListTypeRepository, never()).deleteByUserId(USER_ID);
    }
}
