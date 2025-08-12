package uk.gov.hmcts.reform.pip.account.management.service.account;

import com.microsoft.graph.models.ObjectIdentity;
import com.microsoft.graph.models.PasswordProfile;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.UsersRequestBuilder;
import com.microsoft.graph.users.item.UserItemRequestBuilder;
import com.microsoft.kiota.ApiException;
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
import uk.gov.hmcts.reform.pip.account.management.model.account.AzureAccount;
import uk.gov.hmcts.reform.pip.model.account.Roles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureUserServiceTest {

    @Mock
    private UsersRequestBuilder usersRequestBuilder;

    @Mock
    private UserItemRequestBuilder userItemRequestBuilder;

    @Mock
    private ApiException apiException;

    @Mock
    private UserConfiguration userConfiguration;

    @Mock
    private ClientConfiguration clientConfiguration;

    @Mock
    private GraphServiceClient graphClient;

    @InjectMocks
    private AzureUserService azureUserService;

    private static final String ID = "1234";
    private static final String EMAIL = "a@b.com";
    private static final String FIRST_NAME = "First Name";
    private static final String SURNAME = "Surname";
    private static final String EXTENSION_ID = "1234-1234";
    private static final String DISPLAY_NAME = "Display Name";
    private static final String ERROR_MESSAGE = "Error message should be present when failing to communicate with the"
        + " AD service";

    AzureAccount azureAccount;

    @BeforeEach
    public void setup() {
        azureAccount = new AzureAccount();
    }

    @Test
    void testValidRequestReturnsUser() throws AzureCustomException {
        User user = new User();
        user.setId(ID);

        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(clientConfiguration.getExtensionId()).thenReturn(EXTENSION_ID);
        when(usersRequestBuilder.post(any())).thenReturn(user);

        User returnedUser = azureUserService.createUser(azureAccount, false);

        assertEquals(ID, returnedUser.getId(), "The ID is equal to the expected user ID");
    }

    @Test
    void testInvalidUserRequest() {
        User user = new User();
        user.setId(ID);

        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(clientConfiguration.getExtensionId()).thenReturn(EXTENSION_ID);
        when(usersRequestBuilder.post(any())).thenThrow(apiException);

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
        userToReturn.setId(ID);

        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(clientConfiguration.getExtensionId()).thenReturn(EXTENSION_ID);
        when(usersRequestBuilder.post(any())).thenReturn(userToReturn);

        azureAccount.setEmail(EMAIL);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setSurname(SURNAME);
        azureUserService.createUser(azureAccount, false);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(usersRequestBuilder, times(1)).post(captor.capture());

        User user = captor.getValue();

        assertTrue(user.getAccountEnabled(), "AzureAccount is marked as enabled");
        assertEquals(FIRST_NAME + " " + SURNAME, user.getDisplayName(),
                     "Display name is set as the first name + surname");
        assertEquals(FIRST_NAME, user.getGivenName(), "Given name is set as the firstname");
        assertEquals(SURNAME, user.getSurname(), "Lastname is set as the surname");
        assertEquals(
            Roles.VERIFIED.name(),
            user.getAdditionalData().get("extension_" + EXTENSION_ID.replace("-", "") + "_UserRole").toString(),
            "User role has not been returned as expected"
        );
    }

    @Test
    void testArgumentIdentityDetails() throws AzureCustomException {
        User userToReturn = new User();
        userToReturn.setId(ID);

        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(clientConfiguration.getExtensionId()).thenReturn(EXTENSION_ID);
        when(usersRequestBuilder.post(any())).thenReturn(userToReturn);
        when(userConfiguration.getSignInType()).thenReturn("SignInType");
        when(userConfiguration.getIdentityIssuer()).thenReturn("IdentityIssuer");

        azureAccount.setEmail(EMAIL);
        azureUserService.createUser(azureAccount, false);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(usersRequestBuilder, times(1)).post(captor.capture());
        User user = captor.getValue();

        List<ObjectIdentity> objectIdentityList = user.getIdentities();
        assertEquals(1, objectIdentityList.size(), "One identity has been returned");

        ObjectIdentity objectIdentity = objectIdentityList.get(0);

        assertEquals("SignInType", objectIdentity.getSignInType(), "The sign in type is set correctly");
        assertEquals("IdentityIssuer", objectIdentity.getIssuer(), "The identity issuer is set correctly");
        assertEquals(EMAIL, objectIdentity.getIssuerAssignedId(), "The issuer assigned id is "
            + "set to the email");
    }

    @Test
    void testArgumentPasswordDetails() throws AzureCustomException {
        User userToReturn = new User();
        userToReturn.setId(ID);

        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(clientConfiguration.getExtensionId()).thenReturn(EXTENSION_ID);
        when(usersRequestBuilder.post(any())).thenReturn(userToReturn);

        azureAccount.setEmail(EMAIL);
        azureUserService.createUser(azureAccount, false);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(usersRequestBuilder, times(1)).post(captor.capture());
        User user = captor.getValue();

        PasswordProfile passwordProfile = user.getPasswordProfile();

        assertNotNull(passwordProfile, "The password profile is present");
        assertNotNull(passwordProfile.getPassword(), "The password has been set");
        String password = passwordProfile.getPassword();
        assertEquals(20, password.length(), "The password has the right complexity");
        assertTrue(passwordProfile.getForceChangePasswordNextSignIn(), "The force chain password is set to false");
    }

    @Test
    void testValidRequestReturnsUserByEmail() throws AzureCustomException {
        User user = new User();
        user.setId(ID);
        user.setDisplayName(DISPLAY_NAME);
        List<User> users = new ArrayList<>();
        users.add(user);

        UserCollectionResponse userCollectionResponse = new UserCollectionResponse();
        userCollectionResponse.setValue(users);

        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.get(any())).thenReturn(userCollectionResponse);

        User returnedUser = azureUserService.getUser(EMAIL);

        assertEquals(DISPLAY_NAME, returnedUser.getDisplayName(), "The ID is equal to the expected user ID");
    }

    @Test
    void testValidRequestReturnsNoUserByEmail() throws AzureCustomException {
        List<User> users = new ArrayList<>();

        UserCollectionResponse userCollectionResponse = new UserCollectionResponse();
        userCollectionResponse.setValue(users);

        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.get(any())).thenReturn(userCollectionResponse);

        User returnedUser = azureUserService.getUser(EMAIL);

        assertEquals(null, returnedUser, "The ID is equal to the expected user ID");
    }

    @Test
    void testInvalidUserByEmailRequest() {
        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.get(any())).thenThrow(apiException);

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
        userToReturn.setId(ID);

        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.byUserId(userToReturn.getId())).thenReturn(userItemRequestBuilder);
        doNothing().when(userItemRequestBuilder).delete();

        azureUserService.deleteUser(userToReturn.getId());

        verify(userItemRequestBuilder, times(1)).delete();
    }

    @Test
    void testDeleteUserErrors() {
        User userToReturn = new User();
        userToReturn.setId(ID);

        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.byUserId(userToReturn.getId())).thenReturn(userItemRequestBuilder);
        doThrow(apiException).when(userItemRequestBuilder).delete();

        AzureCustomException azureCustomException = assertThrows(AzureCustomException.class, () -> {
            azureUserService.deleteUser(userToReturn.getId());
        });

        assertEquals("Error when deleting account in Azure.",
                     azureCustomException.getMessage(), ERROR_MESSAGE);
    }

}
