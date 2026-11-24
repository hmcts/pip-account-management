package uk.gov.hmcts.reform.pip.account.management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionListTypeRepository;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.account.UserWithSubscriptions;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionListType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MigrationSupportServiceTest {
    private static final LocalDate FROM = LocalDate.of(2024, 1, 10);
    private static final LocalDate TO = LocalDate.of(2024, 1, 20);
    private static final LocalDateTime FROM_START = LocalDateTime.of(2024, 1, 10, 0, 0);
    private static final LocalDateTime DAY_AFTER_TO_START = LocalDateTime.of(2024, 1, 21, 0, 0);

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionListTypeRepository subscriptionListTypeRepository;

    @InjectMocks
    private MigrationSupportService migrationSupportService;

    private static PiUser user() {
        PiUser user = new PiUser();
        user.setUserId(UUID.randomUUID());
        return user;
    }

    @Test
    void testNoFiltersReturnsAllUsersWithSubscriptionsAndListType() {
        PiUser userWithSubs = user();
        PiUser userWithoutSubs = user();

        Subscription subscription1 = new Subscription();
        subscription1.setUserId(userWithSubs.getUserId());
        Subscription subscription2 = new Subscription();
        subscription2.setUserId(userWithSubs.getUserId());
        SubscriptionListType listType = new SubscriptionListType(userWithSubs.getUserId(),
                                                                 List.of("CIVIL_DAILY_CAUSE_LIST"),
                                                                 List.of("ENGLISH"));

        when(userRepository.findAll(any(Sort.class))).thenReturn(List.of(userWithSubs, userWithoutSubs));
        when(subscriptionRepository.findByUserIdIn(anyCollection())).thenReturn(List.of(subscription1, subscription2));
        when(subscriptionListTypeRepository.findByUserIdIn(anyCollection())).thenReturn(List.of(listType));

        List<UserWithSubscriptions> result = migrationSupportService.getAllUsersWithSubscriptions(null, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUser()).isEqualTo(userWithSubs);
        assertThat(result.get(0).getSubscriptions()).containsExactly(subscription1, subscription2);
        assertThat(result.get(0).getSubscriptionListType()).isEqualTo(listType);

        assertThat(result.get(1).getUser()).isEqualTo(userWithoutSubs);
        assertThat(result.get(1).getSubscriptions()).isEmpty();
        assertThat(result.get(1).getSubscriptionListType()).isNull();
    }

    @Test
    void testBothFiltersUseInclusiveDayRange() {
        PiUser user = user();
        when(userRepository.findAllByCreatedDateGreaterThanEqualAndCreatedDateBefore(
            eq(FROM_START), eq(DAY_AFTER_TO_START), any(Sort.class))).thenReturn(List.of(user));

        assertThat(migrationSupportService.getAllUsersWithSubscriptions(FROM, TO))
            .extracting(UserWithSubscriptions::getUser)
            .containsExactly(user);
    }

    @Test
    void testFromFilterOnly() {
        PiUser user = user();
        when(userRepository.findAllByCreatedDateGreaterThanEqual(eq(FROM_START), any(Sort.class)))
            .thenReturn(List.of(user));

        assertThat(migrationSupportService.getAllUsersWithSubscriptions(FROM, null))
            .extracting(UserWithSubscriptions::getUser)
            .containsExactly(user);
    }

    @Test
    void testToFilterOnly() {
        PiUser user = user();
        when(userRepository.findAllByCreatedDateBefore(eq(DAY_AFTER_TO_START), any(Sort.class)))
            .thenReturn(List.of(user));

        assertThat(migrationSupportService.getAllUsersWithSubscriptions(null, TO))
            .extracting(UserWithSubscriptions::getUser)
            .containsExactly(user);
    }

    @Test
    void testSameFromAndToDateIsAllowed() {
        when(userRepository.findAllByCreatedDateGreaterThanEqualAndCreatedDateBefore(
            eq(FROM_START), eq(FROM_START.plusDays(1)), any(Sort.class))).thenReturn(List.of());

        assertThat(migrationSupportService.getAllUsersWithSubscriptions(FROM, FROM)).isEmpty();
    }

    @Test
    void testFromAfterToThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> migrationSupportService.getAllUsersWithSubscriptions(TO, FROM))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("createdDateFrom must not be after createdDateTo");

        verifyNoInteractions(userRepository, subscriptionRepository, subscriptionListTypeRepository);
    }

    @Test
    void testNoUsersDoesNotQuerySubscriptions() {
        when(userRepository.findAll(any(Sort.class))).thenReturn(List.of());

        assertThat(migrationSupportService.getAllUsersWithSubscriptions(null, null)).isEmpty();
        verifyNoInteractions(subscriptionRepository, subscriptionListTypeRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSubscriptionsAreFetchedInBatches() {
        List<PiUser> users = IntStream.range(0, 2500).mapToObj(i -> user()).toList();
        when(userRepository.findAll(any(Sort.class))).thenReturn(users);

        List<UserWithSubscriptions> result = migrationSupportService.getAllUsersWithSubscriptions(null, null);

        ArgumentCaptor<Collection<UUID>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(subscriptionRepository, times(3)).findByUserIdIn(captor.capture());
        verify(subscriptionListTypeRepository, times(3)).findByUserIdIn(anyCollection());

        assertThat(captor.getAllValues()).extracting(Collection::size).containsExactly(1000, 1000, 500);
        assertThat(result).hasSize(2500);
    }
}
