package uk.gov.hmcts.reform.pip.account.management.service;

import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.ObjectIdentity;
import com.microsoft.graph.models.PasswordProfile;
import com.microsoft.graph.models.Request;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionRequest;
import com.microsoft.graph.requests.UserCollectionRequestBuilder;
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
import uk.gov.hmcts.reform.pip.account.management.model.Roles;

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
@SuppressWarnings("PMD.LawOfDemeter")
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

    @InjectMocks
    private AzureUserService azureUserService;

    private static final String ID = "1234";
    private static final String EMAIL = "a@b.com";
    private static final String EXTENSION_ID = "1234-1234";

    @BeforeEach
    public void setup() {
        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(clientConfiguration.getExtensionId()).thenReturn(EXTENSION_ID);
    }

    @Test
    void testValidRequestReturnsUser() throws AzureCustomException {
        User user = new User();
        user.id = ID;

        when(userCollectionRequest.post(any())).thenReturn(user);

        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);
        User returnedUser = azureUserService.createUser(azureAccount);

        assertEquals(ID, returnedUser.id, "The ID is equal to the expected user ID");
    }

    @Test
    void testInvalidUserRequest() {
        User user = new User();
        user.id = ID;

        when(userCollectionRequest.post(any())).thenThrow(graphServiceException);

        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);

        AzureCustomException azureCustomException = assertThrows(AzureCustomException.class, () -> {
            azureUserService.createUser(azureAccount);
        });

        assertEquals("Error when persisting account into Azure. "
                         + "Check that the user doesn't already exist in the directory",
                     azureCustomException.getMessage(),
                     "Error message should be present when failing to communicate with the AD service");
    }

    @Test
    void testArgumentSetsBaseUserDetails() throws AzureCustomException {
        User userToReturn = new User();
        userToReturn.id = ID;

        when(userCollectionRequest.post(any())).thenReturn(userToReturn);

        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setFirstName("First Name");
        azureAccount.setSurname("Surname");
        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);
        azureUserService.createUser(azureAccount);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userCollectionRequest, times(1)).post(captor.capture());

        User user = captor.getValue();

        assertTrue(user.accountEnabled, "AzureAccount is marked as enabled");
        assertEquals(EMAIL, user.displayName, "Display name is set as the email");
        assertEquals("First Name", user.givenName, "Given name is set as the firstname");
        assertEquals("Surname", user.surname, "Lastname is set as the surname");
        assertEquals(Roles.INTERNAL_ADMIN_CTSC.name(),
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

        when(userCollectionRequest.post(any())).thenReturn(userToReturn);
        when(userConfiguration.getSignInType()).thenReturn("SignInType");
        when(userConfiguration.getIdentityIssuer()).thenReturn("IdentityIssuer");

        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);
        azureUserService.createUser(azureAccount);

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

        when(userCollectionRequest.post(any())).thenReturn(userToReturn);

        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);
        azureUserService.createUser(azureAccount);

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

}
