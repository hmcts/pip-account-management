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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AzureUserServiceTest {

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

    @Test
    public void testValidRequestReturnsUser() {
        User user = new User();
        user.id = "1234";

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenReturn(user);

        Subscriber subscriber = new Subscriber();
        Optional<User> returnedUser = azureUserService.createUser(subscriber);

        assertTrue(returnedUser.isPresent());
        assertEquals("1234", returnedUser.get().id);
    }

    @Test
    public void testInvalidUserRequest() {
        User user = new User();
        user.id = "1234";

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenThrow(graphServiceException);

        Subscriber subscriber = new Subscriber();
        Optional<User> returnedUser = azureUserService.createUser(subscriber);

        assertFalse(returnedUser.isPresent());
    }

    @Test
    public void testArgumentSetsBaseUserDetails() {
        User userToReturn = new User();
        userToReturn.id = "1234";

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenReturn(userToReturn);

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail("a@b.com");
        subscriber.setTitle("Title");
        subscriber.setFirstName("First Name");
        subscriber.setSurname("Surname");
        azureUserService.createUser(subscriber);

        verify(userCollectionRequest, times(1)).post(captor.capture());

        User user = captor.getValue();

        assertTrue(user.accountEnabled);
        assertEquals("a@b.com", user.displayName);
        assertEquals("TitleFirst Name", user.givenName);
        assertEquals("Surname", user.surname);
    }

    @Test
    public void testArgumentIdentityDetails() {
        User userToReturn = new User();
        userToReturn.id = "1234";

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenReturn(userToReturn);
        when(userConfiguration.getSignInType()).thenReturn("SignInType");
        when(userConfiguration.getIdentityIssuer()).thenReturn("IdentityIssuer");

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail("a@b.com");
        azureUserService.createUser(subscriber);

        verify(userCollectionRequest, times(1)).post(captor.capture());
        User user = captor.getValue();

        List<ObjectIdentity> objectIdentityList = user.identities;
        assertEquals(1, objectIdentityList.size());

        ObjectIdentity objectIdentity = objectIdentityList.get(0);

        assertEquals("SignInType", objectIdentity.signInType);
        assertEquals("IdentityIssuer", objectIdentity.issuer);
        assertEquals("a@b.com", objectIdentity.issuerAssignedId);
    }

    @Test
    public void testArgumentPasswordDetails() {
        User userToReturn = new User();
        userToReturn.id = "1234";

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenReturn(userToReturn);
        when(userConfiguration.getPasswordPolicy()).thenReturn("PasswordPolicy");

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail("a@b.com");
        azureUserService.createUser(subscriber);

        verify(userCollectionRequest, times(1)).post(captor.capture());
        User user = captor.getValue();

        PasswordProfile passwordProfile = user.passwordProfile;

        assertNotNull(passwordProfile);
        assertNotNull(passwordProfile.password);
        assertEquals(20, passwordProfile.password.length());
        assertFalse(passwordProfile.forceChangePasswordNextSignIn);
        assertEquals("PasswordPolicy", user.passwordPolicies);
    }

}
