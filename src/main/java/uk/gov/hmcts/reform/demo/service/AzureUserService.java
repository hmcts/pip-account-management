package uk.gov.hmcts.reform.demo.service;

import com.microsoft.applicationinsights.web.dependencies.apachecommons.lang3.RandomStringUtils;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.ObjectIdentity;
import com.microsoft.graph.models.PasswordProfile;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.demo.config.UserConfiguration;
import uk.gov.hmcts.reform.demo.model.Subscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A class that wraps any interacts with Azure active directory.
 */
@Component
public class AzureUserService {

    @Autowired
    private GraphServiceClient<Request> graphClient;

    @Autowired
    private UserConfiguration userConfiguration;

    /**
     * Creates a new subscriber in the Azure active directory.
     * @param subscriber The subscriber to add in the azure active directory.
     * @return The created user if it was successful. An empty optional if it was not - it can fail for many reasons,
     *         one is that the user already exists.
     */
    public Optional<User> createUser(Subscriber subscriber) {
        try {
            User user = createUserObject(subscriber);
            User createdUser = graphClient.users()
                .buildRequest()
                .post(user);

            return Optional.of(createdUser);
        } catch (GraphServiceException e) {
            return Optional.empty();
        }
    }

    private User createUserObject(Subscriber subscriber) {
        User user = new User();
        user.accountEnabled = true;
        user.displayName = subscriber.getEmail();
        user.givenName = subscriber.getTitle() + subscriber.getFirstName();
        user.surname = subscriber.getSurname();

        ObjectIdentity identity = new ObjectIdentity();
        identity.signInType = userConfiguration.getSignInType();
        identity.issuer = userConfiguration.getIdentityIssuer();
        identity.issuerAssignedId = subscriber.getEmail();

        List<ObjectIdentity> identitiesList = new ArrayList<>();
        identitiesList.add(identity);

        PasswordProfile passwordProfile = new PasswordProfile();

        passwordProfile.password = RandomStringUtils.randomAscii(20);
        passwordProfile.forceChangePasswordNextSignIn = false;
        user.passwordProfile = passwordProfile;
        user.passwordPolicies = userConfiguration.getPasswordPolicy();

        user.identities = identitiesList;

        return user;
    }

}
