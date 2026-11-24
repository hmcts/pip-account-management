package uk.gov.hmcts.reform.pip.account.management.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.account.UserWithSubscriptions;
import uk.gov.hmcts.reform.pip.account.management.service.MigrationSupportService;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MigrationSupportControllerTest {
    private static final LocalDate FROM = LocalDate.of(2024, 1, 10);
    private static final LocalDate TO = LocalDate.of(2024, 1, 20);

    @Mock
    private MigrationSupportService migrationSupportService;

    @InjectMocks
    private MigrationSupportController migrationSupportController;

    @Test
    void testGetAllUsersWithSubscriptionsReturnsOk() {
        List<UserWithSubscriptions> users = List.of(new UserWithSubscriptions(new PiUser(), List.of(), null));
        when(migrationSupportService.getAllUsersWithSubscriptions(FROM, TO)).thenReturn(users);

        ResponseEntity<List<UserWithSubscriptions>> response =
            migrationSupportController.getAllUsersWithSubscriptions(FROM, TO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(users);
    }
}
