package uk.gov.hmcts.reform.rsecheck.service;

import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.Request;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionRequest;
import com.microsoft.graph.requests.UserCollectionRequestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.demo.model.Subscriber;
import uk.gov.hmcts.reform.demo.service.AzureUserService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AzureUserServiceTest {

    @Mock
    private UserCollectionRequestBuilder userCollectionRequestBuilder;

    @Mock
    private UserCollectionRequest userCollectionRequest;

    @Mock
    private GraphServiceException graphServiceException;

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



}
