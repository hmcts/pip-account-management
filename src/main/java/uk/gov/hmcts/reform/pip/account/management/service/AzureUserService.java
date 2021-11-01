package uk.gov.hmcts.reform.pip.account.management.service;

import com.microsoft.applicationinsights.web.dependencies.apachecommons.lang3.RandomStringUtils;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.ObjectIdentity;
import com.microsoft.graph.models.PasswordProfile;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pip.account.management.config.UserConfiguration;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.model.Subscriber;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that wraps any interacts with Azure active directory.
 */
@Component
public class AzureUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureUserService.class);

    @Autowired
    private GraphServiceClient<Request> graphClient;

    @Autowired
    private UserConfiguration userConfiguration;

    /**
     * Creates a new subscriber in the Azure active directory.
     * @param subscriber The subscriber to add in the azure active directory.
     * @return The created user if it was successful.
     * @throws AzureCustomException thrown if theres an error with communicating with Azure.
     */
    public User createUser(Subscriber subscriber) throws AzureCustomException {
        try {
            User user = createUserObject(subscriber);
            return graphClient.users()
                .buildRequest()
                .post(user);
        } catch (GraphServiceException e) {
            LOGGER.error(e.getMessage());
            throw new AzureCustomException("Error when persisting subscriber into Azure");
        }
    }

    private User createUserObject(Subscriber subscriber) {
        User user = new User();
        user.accountEnabled = true;
        user.displayName = subscriber.getEmail();
        user.givenName = subscriber.getFirstName();
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
