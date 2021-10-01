package uk.gov.hmcts.reform.rsecheck.service;

import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.ObjectIdentity;
import com.microsoft.graph.models.PasswordProfile;
import com.microsoft.graph.models.Request;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionRequest;
import com.microsoft.graph.requests.UserCollectionRequestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.demo.config.UserConfiguration;
import uk.gov.hmcts.reform.demo.model.Subscriber;
import uk.gov.hmcts.reform.demo.service.AzureUserService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
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
    private GraphServiceClient<Request> graphClient;

    @InjectMocks
    private AzureUserService azureUserService;

    private static final String ID = "1234";
    private static final String EMAIL = "a@b.com";

    @Test
    void testValidRequestReturnsUser() {
        User user = new User();
        user.id = ID;

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenReturn(user);

        Subscriber subscriber = new Subscriber();
        Optional<User> returnedUser = azureUserService.createUser(subscriber);

        assertTrue(returnedUser.isPresent(), "The returned user from the service is present");
        assertEquals(ID, returnedUser.get().id, "The ID is equal to the expected user ID");
    }

    @Test
    void testInvalidUserRequest() {
        User user = new User();
        user.id = ID;

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenThrow(graphServiceException);

        Subscriber subscriber = new Subscriber();
        Optional<User> returnedUser = azureUserService.createUser(subscriber);

        assertFalse(returnedUser.isPresent(), "No user is returned on an invalid request");
    }

    @Test
    void testArgumentSetsBaseUserDetails() {
        User userToReturn = new User();
        userToReturn.id = ID;

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenReturn(userToReturn);

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(EMAIL);
        subscriber.setTitle("Title");
        subscriber.setFirstName("First Name");
        subscriber.setSurname("Surname");
        azureUserService.createUser(subscriber);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userCollectionRequest, times(1)).post(captor.capture());

        User user = captor.getValue();

        assertTrue(user.accountEnabled, "Account is marked as enabled");
        assertEquals(EMAIL, user.displayName, "Display name is set as the email");
        assertEquals("TitleFirst Name", user.givenName, "Given name is set as the title"
            + " and firstname");
        assertEquals("Surname", user.surname, "Lastname is set as the surname");
    }

    @Test
    void testArgumentIdentityDetails() {
        User userToReturn = new User();
        userToReturn.id = ID;

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenReturn(userToReturn);
        when(userConfiguration.getSignInType()).thenReturn("SignInType");
        when(userConfiguration.getIdentityIssuer()).thenReturn("IdentityIssuer");

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(EMAIL);
        azureUserService.createUser(subscriber);

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
    void testArgumentPasswordDetails() {
        User userToReturn = new User();
        userToReturn.id = ID;

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenReturn(userToReturn);
        when(userConfiguration.getPasswordPolicy()).thenReturn("PasswordPolicy");

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(EMAIL);
        azureUserService.createUser(subscriber);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userCollectionRequest, times(1)).post(captor.capture());
        User user = captor.getValue();

        PasswordProfile passwordProfile = user.passwordProfile;

        assertNotNull(passwordProfile, "The password profile is present");
        assertNotNull(passwordProfile.password, "The password has been set");
        String password = passwordProfile.password;
        assertEquals(20, password.length(), "The password has the right complexity");
        assertFalse(passwordProfile.forceChangePasswordNextSignIn, "The force chain password is set to false");
        assertEquals("PasswordPolicy", user.passwordPolicies, "The password policy is set correctly");
    }

}
