package uk.gov.hmcts.reform.pip.account.management.helpers;

import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionListType;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.publication.ListType.CIVIL_DAILY_CAUSE_LIST;

public final class SubscriptionUtils {

    private SubscriptionUtils() {
    }

    public static Subscription createMockSubscription(UUID userId, String locationId, Channel channel,
                                                      LocalDateTime createdDate) {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setUserId(userId);
        subscription.setSearchValue(locationId);
        subscription.setChannel(channel);
        subscription.setId(UUID.randomUUID());
        subscription.setCreatedDate(createdDate);
        subscription.setSearchType(SearchType.LOCATION_ID);

        return subscription;
    }

    public static List<Subscription> createMockSubscriptionList(LocalDateTime createdDate) {
        final int caseIdInterval = 3;
        final int caseUrnInterval = 6;

        List<Subscription> subs = new ArrayList<>();
        Map<Integer, UUID> mockData = Map.of(
            0, UUID.randomUUID(),
            1, UUID.randomUUID(),
            2, UUID.randomUUID(),
            3, UUID.randomUUID(),
            4, UUID.randomUUID(),
            5, UUID.randomUUID(),
            6, UUID.randomUUID(),
            7, UUID.randomUUID(),
            8, UUID.randomUUID()
        );

        for (int i = 0; i < 8; i++) {
            Subscription subscription = createMockSubscription(mockData.get(i), String.format("%s", i),
                                                               i < 3 ? Channel.API_COURTEL : Channel.EMAIL,
                                                               createdDate);
            subscription.setChannel(i < 3 ? Channel.API_COURTEL : Channel.EMAIL);
            subscription.setCaseName("test name");
            subscription.setUrn("321" + i);
            subscription.setCaseNumber("123" + i);
            if (i < caseIdInterval) {
                subscription.setSearchType(SearchType.CASE_ID);
            } else if (i < caseUrnInterval) {
                subscription.setSearchType(SearchType.CASE_URN);
            } else {
                subscription.setSearchType(SearchType.LOCATION_ID);
                subscription.setCaseName(null);
                subscription.setUrn(null);
                subscription.setCaseNumber(null);
                subscription.setLocationName("test court name");
            }
            subs.add(subscription);
        }
        return subs;
    }

    public static Subscription findableSubscription() {
        Subscription subscription = new Subscription();
        subscription.setUserId(UUID.randomUUID());
        subscription.setId(UUID.randomUUID());
        subscription.setSearchType(SearchType.LOCATION_ID);
        subscription.setSearchValue("1");
        return subscription;
    }

    public static List<SubscriptionListType> createMockSubscriptionListType(UUID userId) {
        List<SubscriptionListType> subscriptionListTypes = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            subscriptionListTypes.add(new SubscriptionListType(userId,
                List.of(CIVIL_DAILY_CAUSE_LIST.name()), List.of("ENGLISH")));
        }
        return subscriptionListTypes;
    }

}
