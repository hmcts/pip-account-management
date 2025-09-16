package uk.gov.hmcts.reform.pip.account.management.controllers.subscription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionListType;
import uk.gov.hmcts.reform.pip.account.management.service.subscription.SubscriptionListTypeService;
import uk.gov.hmcts.reform.pip.model.publication.ListType;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class SubscriptionListTypeControllerTest {

    private static final String RETURNED_SUBSCRIPTION_NOT_MATCHED =
        "Returned subscription does not match expected subscription";
    private static final List<String> LIST_TYPES = List.of(ListType.CIVIL_DAILY_CAUSE_LIST.name());
    private static final List<String> LIST_LANGUAGE = List.of("ENGLISH");
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    SubscriptionListTypeService subscriptionListTypeService;

    @InjectMocks
    SubscriptionListTypeController subscriptionListTypeController;

    SubscriptionListType subscriptionListType;

    @BeforeEach
    void setup() {
        subscriptionListType = new SubscriptionListType(USER_ID, LIST_TYPES, LIST_LANGUAGE);
    }

    @Test
    void testAddListTypesForSubscription() {
        doNothing().when(subscriptionListTypeService).addListTypesForSubscription(subscriptionListType,
                                                                                  USER_ID.toString());

        assertEquals(
            new ResponseEntity<>(
                String.format("Location list Type successfully added for user %s", USER_ID),
                HttpStatus.CREATED
            ),
            subscriptionListTypeController.addListTypesForSubscription(
                USER_ID.toString(), USER_ID.toString(), subscriptionListType
            ),
            RETURNED_SUBSCRIPTION_NOT_MATCHED
        );
    }

    @Test
    void testConfigureListTypesForSubscription() {
        doNothing().when(subscriptionListTypeService).configureListTypesForSubscription(subscriptionListType,
                                                                                        USER_ID.toString());

        assertEquals(
            new ResponseEntity<>(
                String.format("Location list Type successfully updated for user %s", USER_ID),
                HttpStatus.OK
            ),
            subscriptionListTypeController.configureListTypesForSubscription(
                USER_ID.toString(), USER_ID.toString(), subscriptionListType
            ),
            RETURNED_SUBSCRIPTION_NOT_MATCHED
        );
    }
}
