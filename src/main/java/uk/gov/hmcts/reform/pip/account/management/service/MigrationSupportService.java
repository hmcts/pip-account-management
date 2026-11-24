package uk.gov.hmcts.reform.pip.account.management.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionListTypeRepository;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.account.UserWithSubscriptions;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionListType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
public class MigrationSupportService {
    // Keeps the number of bind parameters in each IN clause well below the Postgres limit
    private static final int USER_ID_BATCH_SIZE = 1000;
    private static final Sort USER_SORT = Sort.by("createdDate", "userId");

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionListTypeRepository subscriptionListTypeRepository;

    /**
     * Retrieve all users, along with their subscriptions and subscription list type, optionally filtered by
     * created date.
     *
     * @param createdDateFrom Only include users created on or after this date (inclusive). Ignored if null.
     * @param createdDateTo Only include users created on or before this date (inclusive). Ignored if null.
     * @return The matching users with their subscriptions.
     */
    public List<UserWithSubscriptions> getAllUsersWithSubscriptions(LocalDate createdDateFrom,
                                                                    LocalDate createdDateTo) {
        if (createdDateFrom != null && createdDateTo != null && createdDateFrom.isAfter(createdDateTo)) {
            throw new IllegalArgumentException("createdDateFrom must not be after createdDateTo");
        }

        List<PiUser> users = findUsers(createdDateFrom, createdDateTo);
        List<UUID> userIds = users.stream().map(PiUser::getUserId).toList();

        Map<UUID, List<Subscription>> subscriptionsByUser = new HashMap<>();
        Map<UUID, SubscriptionListType> listTypeByUser = new HashMap<>();

        for (int i = 0; i < userIds.size(); i += USER_ID_BATCH_SIZE) {
            List<UUID> batch = userIds.subList(i, Math.min(i + USER_ID_BATCH_SIZE, userIds.size()));
            subscriptionRepository.findByUserIdIn(batch).forEach(subscription ->
                subscriptionsByUser.computeIfAbsent(subscription.getUserId(), id -> new ArrayList<>())
                    .add(subscription)
            );
            subscriptionListTypeRepository.findByUserIdIn(batch).forEach(listType ->
                listTypeByUser.putIfAbsent(listType.getUserId(), listType)
            );
        }

        return users.stream()
            .map(user -> new UserWithSubscriptions(
                user,
                subscriptionsByUser.getOrDefault(user.getUserId(), List.of()),
                listTypeByUser.get(user.getUserId())
            ))
            .toList();
    }

    private List<PiUser> findUsers(LocalDate createdDateFrom, LocalDate createdDateTo) {
        LocalDateTime from = createdDateFrom == null ? null : createdDateFrom.atStartOfDay();
        // Exclusive upper bound of the following day so the whole of createdDateTo is included
        LocalDateTime toExclusive = createdDateTo == null ? null : createdDateTo.plusDays(1).atStartOfDay();

        if (from != null && toExclusive != null) {
            return userRepository.findAllByCreatedDateGreaterThanEqualAndCreatedDateBefore(from, toExclusive,
                                                                                           USER_SORT);
        }
        if (from != null) {
            return userRepository.findAllByCreatedDateGreaterThanEqual(from, USER_SORT);
        }
        if (toExclusive != null) {
            return userRepository.findAllByCreatedDateBefore(toExclusive, USER_SORT);
        }
        return userRepository.findAll(USER_SORT);
    }
}
