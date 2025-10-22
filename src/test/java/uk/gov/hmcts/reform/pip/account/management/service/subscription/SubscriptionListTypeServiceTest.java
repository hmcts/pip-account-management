package uk.gov.hmcts.reform.pip.account.management.service.subscription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionListTypeRepository;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.UserNotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionListType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.model.publication.ListType.CIVIL_DAILY_CAUSE_LIST;

@ExtendWith(MockitoExtension.class)
class SubscriptionListTypeServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_USER_ID = UUID.randomUUID();
    private static final UUID ACTIONED_USER_ID = UUID.randomUUID();
    private static final String SUBSCRIPTION_CREATED_ERROR = "The returned subscription does "
        + "not match the expected subscription";

    private SubscriptionListType mockSubscriptionListType;

    @Mock
    SubscriptionListTypeRepository subscriptionListTypeRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    SubscriptionListTypeService subscriptionListTypeService;

    @BeforeEach
    void setup() {
        mockSubscriptionListType = new SubscriptionListType(USER_ID, List.of(CIVIL_DAILY_CAUSE_LIST.name()),
                                                            List.of("ENGLISH"));

        Mockito.lenient().when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(new PiUser()));
        Mockito.lenient().when(userRepository.findByUserId(UNKNOWN_USER_ID)).thenReturn(Optional.empty());
    }

    @Test
    void testConfigureListTypesForLocationSubscription() {
        subscriptionListTypeService.configureListTypesForSubscription(mockSubscriptionListType, ACTIONED_USER_ID);

        assertEquals(USER_ID, mockSubscriptionListType.getUserId(),
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testConfigureListTypesForLocationSubscriptionWhenUnknownUser() {
        mockSubscriptionListType.setUserId(UNKNOWN_USER_ID);
        assertThrows(UserNotFoundException.class, () ->
            subscriptionListTypeService.configureListTypesForSubscription(mockSubscriptionListType, ACTIONED_USER_ID),
                     "UserNotFoundException not thrown when user is not present");
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
        subscriptionListTypeService.addListTypesForSubscription(mockSubscriptionListType, ACTIONED_USER_ID);

        assertEquals(USER_ID, mockSubscriptionListType.getUserId(),
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testAddListTypesForLocationSubscriptionWhenUnknownUser() {
        mockSubscriptionListType.setUserId(UNKNOWN_USER_ID);
        assertThrows(UserNotFoundException.class, () ->
                         subscriptionListTypeService.addListTypesForSubscription(
                             mockSubscriptionListType, ACTIONED_USER_ID),
                     "UserNotFoundException not thrown when user is not present");
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
