package uk.gov.hmcts.reform.pip.account.management.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.model.account.Roles.ALL_NON_RESTRICTED_ADMIN_ROLES;
import static uk.gov.hmcts.reform.pip.model.account.UserProvenances.PI_AAD;

@ExtendWith(MockitoExtension.class)
class AccountFilteringServiceTest {
    private static final String EMAIL = "test@hmcts.net";
    private static final String ID = "1234";
    private static final List<String> EXAMPLE_CSV = List.of(
        "2fe899ff-96ed-435a-bcad-1411bbe96d2a,string,CFT_IDAM,INTERNAL_ADMIN_CTSC,2022-01-19 13:45:50.873778,"
            + "2023-01-25 14:22:56.434343");
    private static final PiUser PI_USER = new PiUser();

    private static final String USER_NOT_FOUND_EXCEPTION_MESSAGE =
        "The exception when a user has not been found has been thrown";
    private static final String RETURN_USER_ERROR = "Returned user does not match expected user";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountFilteringService accountFilteringService;

    @BeforeAll
    static void setup() {
        PI_USER.setUserId(UUID.randomUUID());
        PI_USER.setUserProvenance(PI_AAD);
        PI_USER.setProvenanceUserId(ID);
        PI_USER.setEmail(EMAIL);
    }

    @Test
    void testMiService() {
        when(userRepository.getAccManDataForMI()).thenReturn(EXAMPLE_CSV);
        String testString = accountFilteringService.getAccManDataForMiReporting();

        assertThat(testString)
            .as("Json parsing has probably failed")
            .contains("CTSC")
            .hasLineCount(2);

        String[] splitLineString = testString.split("(\r\n|\r|\n)");
        long countLine1 = splitLineString[0].chars().filter(character -> character == ',').count();

        assertThat(splitLineString[0])
            .as("Header row does not match")
            .isEqualTo("user_id,provenance_user_id,user_provenance,roles,created_date,last_signed_in_date");

        assertThat(splitLineString)
            .as("Data must be missing, are only headers printing? or Wrong data")
            .hasSizeGreaterThanOrEqualTo(2)
            .allSatisfy(
                e -> assertThat(e.chars().filter(character -> character == ',').count()).isEqualTo(countLine1));
    }

    @Test
    void testFindAllThirdPartyAccounts() {

        PiUser thirdPartyUser = new PiUser();
        thirdPartyUser.setUserId(UUID.randomUUID());
        List<PiUser> usersList = List.of(thirdPartyUser);

        when(userRepository.findAllByUserProvenance(UserProvenances.THIRD_PARTY)).thenReturn(usersList);

        List<PiUser> returnedUsers = accountFilteringService.findAllThirdPartyAccounts();

        assertEquals(usersList, returnedUsers, "Returned users does not match expected users");
    }

    @Test
    void testFindAllAccountsExceptThirdPartyEmptyParams() {
        Pageable pageable = PageRequest.of(0, 25);
        List<UserProvenances> emptyUserProvenancesList = new ArrayList<>();
        List<Roles> emptyRoleList = new ArrayList<>();
        Page<PiUser> page = new PageImpl<>(List.of(PI_USER), pageable, List.of(PI_USER).size());
        when(userRepository.findAllByEmailLikeIgnoreCaseAndUserProvenanceInAndRolesInAndProvenanceUserIdLike(
            argThat(arg -> "%%".equals(arg)),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(page);

        Page<PiUser> response = accountFilteringService.findAllAccountsExceptThirdParty(
            pageable, "", "", emptyUserProvenancesList, emptyRoleList, ""
        );

        verify(userRepository, never()).findByUserIdPageable(
            any(), any());

        assertEquals(page, response, "Returned page did not match expected");
    }

    @Test
    void testFindAllAccountsExceptThirdPartyFullParams() {
        Pageable pageable = PageRequest.of(0, 25);
        List<UserProvenances> userProvenancesList = List.of(PI_AAD);
        List<Roles> roleList = List.of(Roles.VERIFIED);
        Page<PiUser> page = new PageImpl<>(List.of(PI_USER), pageable, List.of(PI_USER).size());
        when(userRepository.findAllByEmailLikeIgnoreCaseAndUserProvenanceInAndRolesInAndProvenanceUserIdLike(
            any(),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(page);

        Page<PiUser> response = accountFilteringService.findAllAccountsExceptThirdParty(
            pageable, "test", ID, userProvenancesList, roleList, ""
        );

        verify(userRepository, never()).findByUserIdPageable(
            any(), any());

        assertEquals(page, response, "Returned page did not match expected");
    }

    @Test
    void testFindAllAccountsExceptThirdPartyOnlyUserId() {
        Pageable pageable = PageRequest.of(0, 25);
        PiUser user = new PiUser();
        user.setUserId(UUID.randomUUID());
        List<UserProvenances> emptyUserProvenancesList = new ArrayList<>();
        List<Roles> emptyRoleList = new ArrayList<>();
        Page<PiUser> page = new PageImpl<>(List.of(user), pageable, List.of(user).size());

        when(userRepository.findByUserIdPageable(user.getUserId().toString(), pageable)).thenReturn(page);

        Page<PiUser> response = accountFilteringService.findAllAccountsExceptThirdParty(
            pageable, "", "", emptyUserProvenancesList, emptyRoleList, user.getUserId().toString()
        );

        verify(userRepository, never())
            .findAllByEmailLikeIgnoreCaseAndUserProvenanceInAndRolesInAndProvenanceUserIdLike(
                any(), any(), any(), any(), any());

        assertEquals(page, response, "Returned page did not match expected");
    }

    @Test
    void testGetAdminUserByEmailAndProvenance() {
        PiUser user = new PiUser();
        user.setEmail(EMAIL);
        user.setUserProvenance(PI_AAD);

        when(userRepository.findByEmailIgnoreCaseAndUserProvenanceAndRolesIn(EMAIL, PI_AAD,
                                                                             ALL_NON_RESTRICTED_ADMIN_ROLES))
            .thenReturn(Optional.of(user));

        PiUser returnedUser = accountFilteringService.getAdminUserByEmailAndProvenance(
            EMAIL, PI_AAD
        );
        assertEquals(user, returnedUser, RETURN_USER_ERROR);
    }

    @Test
    void testGetAdminUserByEmailAndProvenanceNotFound() {
        PiUser user = new PiUser();
        user.setEmail(EMAIL);
        user.setUserProvenance(PI_AAD);

        when(userRepository.findByEmailIgnoreCaseAndUserProvenanceAndRolesIn(EMAIL, PI_AAD,
                                                                             ALL_NON_RESTRICTED_ADMIN_ROLES))
            .thenReturn(Optional.empty());

        NotFoundException notFoundException = assertThrows(
            NotFoundException.class,
            () -> accountFilteringService.getAdminUserByEmailAndProvenance(EMAIL, PI_AAD),
            USER_NOT_FOUND_EXCEPTION_MESSAGE
        );

        assertTrue(notFoundException.getMessage().contains("t***@hmcts.net"),
                   "Exception message thrown does not contain email");

        assertTrue(notFoundException.getMessage().contains(PI_AAD.toString()),
                   "Exception message thrown does not contain provenance");
    }
}
