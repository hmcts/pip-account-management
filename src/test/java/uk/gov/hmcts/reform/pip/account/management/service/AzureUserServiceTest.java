package uk.gov.hmcts.reform.pip.account.management.service;

import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.ObjectIdentity;
import com.microsoft.graph.models.PasswordProfile;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionPage;
import com.microsoft.graph.requests.UserCollectionRequest;
import com.microsoft.graph.requests.UserCollectionRequestBuilder;
import com.microsoft.graph.requests.UserRequest;
import com.microsoft.graph.requests.UserRequestBuilder;
import okhttp3.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.config.ClientConfiguration;
import uk.gov.hmcts.reform.pip.account.management.config.UserConfiguration;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.model.account.Roles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.LawOfDemeter", "PMD.TooManyMethods"})
class AzureUserServiceTest {

    @Mock
    private UserCollectionRequestBuilder userCollectionRequestBuilder;

    @Mock
    private UserCollectionRequest userCollectionRequest;

    @Mock
    private GraphServiceException graphServiceException;

    @Mock
    private UserConfiguration userConfiguration;

    @Mock
    private ClientConfiguration clientConfiguration;

    @Mock
    private GraphServiceClient<Request> graphClient;

    @Mock
    private UserRequestBuilder userRequestBuilder;

    @Mock
    private UserRequest userRequest;

    @InjectMocks
    private AzureUserService azureUserService;

    private static final String ID = "1234";
    private static final String EMAIL = "a@b.com";
    private static final String FIRST_NAME = "First Name";
    private static final String SURNAME = "Surname";
    private static final String EXTENSION_ID = "1234-1234";
    private static final String DISPLAY_NAME = "Display Name";
    private static final String B2C_URL = "URL";
    private static final String ERROR_MESSAGE = "Error message should be present when failing to communicate with the"
        + " AD service";

    AzureAccount azureAccount;

    @BeforeEach
    public void setup() {
        azureAccount = new AzureAccount();
        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);
    }

    @Test
    void testValidRequestReturnsUser() throws AzureCustomException {
        User user = new User();
        user.id = ID;

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(clientConfiguration.getExtensionId()).thenReturn(EXTENSION_ID);
        when(userCollectionRequest.post(any())).thenReturn(user);

        User returnedUser = azureUserService.createUser(azureAccount, false);

        assertEquals(ID, returnedUser.id, "The ID is equal to the expected user ID");
    }

    @Test
    void testInvalidUserRequest() {
        User user = new User();
        user.id = ID;

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(clientConfiguration.getExtensionId()).thenReturn(EXTENSION_ID);
        when(userCollectionRequest.post(any())).thenThrow(graphServiceException);

        AzureCustomException azureCustomException = assertThrows(AzureCustomException.class, () -> {
            azureUserService.createUser(azureAccount, false);
        });

        assertEquals("Error when persisting account into Azure. "
                         + "Check that the user doesn't already exist in the directory",
                     azureCustomException.getMessage(),
                     ERROR_MESSAGE);
    }

    @Test
    void testArgumentSetsBaseUserDetails() throws AzureCustomException {
        User userToReturn = new User();
        userToReturn.id = ID;

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(clientConfiguration.getExtensionId()).thenReturn(EXTENSION_ID);
        when(userCollectionRequest.post(any())).thenReturn(userToReturn);

        azureAccount.setEmail(EMAIL);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setSurname(SURNAME);
        azureUserService.createUser(azureAccount, false);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userCollectionRequest, times(1)).post(captor.capture());

        User user = captor.getValue();

        assertTrue(user.accountEnabled, "AzureAccount is marked as enabled");
        assertEquals(FIRST_NAME + " " + SURNAME, user.displayName,
                     "Display name is set as the first name + surname");
        assertEquals(FIRST_NAME, user.givenName, "Given name is set as the firstname");
        assertEquals(SURNAME, user.surname, "Lastname is set as the surname");
        assertEquals(
            Roles.INTERNAL_ADMIN_CTSC.name(),
            user.additionalDataManager().get("extension_"
                                                      + EXTENSION_ID.replace("-", "")
                                                          + "_UserRole").getAsString(),
            "User role has not been returned as expected"
        );
    }

    @Test
    void testArgumentIdentityDetails() throws AzureCustomException {
        User userToReturn = new User();
        userToReturn.id = ID;

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(clientConfiguration.getExtensionId()).thenReturn(EXTENSION_ID);
        when(userCollectionRequest.post(any())).thenReturn(userToReturn);
        when(userConfiguration.getSignInType()).thenReturn("SignInType");
        when(userConfiguration.getIdentityIssuer()).thenReturn("IdentityIssuer");

        azureAccount.setEmail(EMAIL);
        azureUserService.createUser(azureAccount, false);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userCollectionRequest, times(1)).post(captor.capture());
        User user = captor.getValue();

        List<ObjectIdentity> objectIdentityList = user.identities;
        assertEquals(1, objectIdentityList.size(), "One identity has been returned");

        ObjectIdentity objectIdentity = objectIdentityList.get(0);

        assertEquals("SignInType", objectIdentity.signInType, "The sign in type is set correctly");
        assertEquals("IdentityIssuer", objectIdentity.issuer, "The identity issuer is set correctly");
        assertEquals(EMAIL, objectIdentity.issuerAssignedId, "The issuer assigned id is "
            + "set to the email");
    }

    @Test
    void testArgumentPasswordDetails() throws AzureCustomException {
        User userToReturn = new User();
        userToReturn.id = ID;

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(clientConfiguration.getExtensionId()).thenReturn(EXTENSION_ID);
        when(userCollectionRequest.post(any())).thenReturn(userToReturn);

        azureAccount.setEmail(EMAIL);
        azureUserService.createUser(azureAccount, false);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userCollectionRequest, times(1)).post(captor.capture());
        User user = captor.getValue();

        PasswordProfile passwordProfile = user.passwordProfile;

        assertNotNull(passwordProfile, "The password profile is present");
        assertNotNull(passwordProfile.password, "The password has been set");
        String password = passwordProfile.password;
        assertEquals(20, password.length(), "The password has the right complexity");
        assertTrue(passwordProfile.forceChangePasswordNextSignIn, "The force chain password is set to false");
    }

    @Test
    void testValidRequestReturnsUserByEmail() throws AzureCustomException {
        User user = new User();
        user.id = ID;
        user.displayName = DISPLAY_NAME;
        List<User> users = new ArrayList<>();
        users.add(user);

        UserCollectionPage userCollectionPage = new UserCollectionPage(users, userCollectionRequestBuilder);

        when(clientConfiguration.getB2cUrl()).thenReturn(B2C_URL);
        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.filter(any())).thenReturn(userCollectionRequest);
        when(userCollectionRequest.get()).thenReturn(userCollectionPage);

        User returnedUser = azureUserService.getUser(EMAIL);

        assertEquals(DISPLAY_NAME, returnedUser.displayName, "The ID is equal to the expected user ID");
    }

    @Test
    void testValidRequestReturnsNoUserByEmail() throws AzureCustomException {
        List<User> users = new ArrayList<>();

        UserCollectionPage userCollectionPage = new UserCollectionPage(users, userCollectionRequestBuilder);

        when(clientConfiguration.getB2cUrl()).thenReturn(B2C_URL);
        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.filter(any())).thenReturn(userCollectionRequest);
        when(userCollectionRequest.get()).thenReturn(userCollectionPage);

        User returnedUser = azureUserService.getUser(EMAIL);

        assertEquals(null, returnedUser, "The ID is equal to the expected user ID");
    }

    @Test
    void testInvalidUserByEmailRequest() {

        when(clientConfiguration.getB2cUrl()).thenReturn(B2C_URL);
        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.filter(any())).thenReturn(userCollectionRequest);
        when(userCollectionRequest.get()).thenThrow(graphServiceException);

        AzureCustomException azureCustomException = assertThrows(AzureCustomException.class, () -> {
            azureUserService.getUser(EMAIL);
        });

        assertEquals("Error when checking account into Azure.",
                     azureCustomException.getMessage(),
                     ERROR_MESSAGE);
    }

    @Test
    void testDeleteUser() throws AzureCustomException {
        User userToReturn = new User();
        userToReturn.id = ID;

        when(graphClient.users(userToReturn.id)).thenReturn(userRequestBuilder);
        when(userRequestBuilder.buildRequest()).thenReturn(userRequest);
        when(userRequest.delete()).thenReturn(userToReturn);

        assertEquals(userToReturn, azureUserService.deleteUser(userToReturn.id),
                     "Returned user does not match expected");
    }

    @Test
    void testDeleteUserErrors() {
        User userToReturn = new User();
        userToReturn.id = ID;

        when(graphClient.users(userToReturn.id)).thenReturn(userRequestBuilder);
        when(userRequestBuilder.buildRequest()).thenReturn(userRequest);
        when(userRequest.delete()).thenThrow(graphServiceException);

        AzureCustomException azureCustomException = assertThrows(AzureCustomException.class, () -> {
            azureUserService.deleteUser(userToReturn.id);
        });

        assertEquals("Error when deleting account in Azure.",
                     azureCustomException.getMessage(),
                     ERROR_MESSAGE);
    }

    @Test
    void testUpdateUser() throws AzureCustomException {
        User userToReturn = new User();
        userToReturn.id = ID;

        when(clientConfiguration.getExtensionId()).thenReturn(EXTENSION_ID);
        when(graphClient.users(userToReturn.id)).thenReturn(userRequestBuilder);
        when(userRequestBuilder.buildRequest()).thenReturn(userRequest);
        when(userRequest.patch(any())).thenReturn(userToReturn);

        assertEquals(userToReturn, azureUserService.updateUserRole(userToReturn.id, Roles.SYSTEM_ADMIN.toString()),
                     "Returned user does not match expected");
    }

    @Test
    void testUpdateUserError() {
        User userToReturn = new User();
        userToReturn.id = ID;

        when(clientConfiguration.getExtensionId()).thenReturn(EXTENSION_ID);
        when(graphClient.users(userToReturn.id)).thenReturn(userRequestBuilder);
        when(userRequestBuilder.buildRequest()).thenReturn(userRequest);
        when(userRequest.patch(any())).thenThrow(graphServiceException);

        AzureCustomException azureCustomException = assertThrows(AzureCustomException.class, () -> {
            azureUserService.updateUserRole(userToReturn.id, Roles.SYSTEM_ADMIN.toString());
        });

        assertEquals("Error when updating account in Azure.",
                     azureCustomException.getMessage(),
                     ERROR_MESSAGE);
    }

}
