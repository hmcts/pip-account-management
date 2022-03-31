package uk.gov.hmcts.reform.pip.account.management.service;

import com.google.gson.JsonPrimitive;
import com.microsoft.applicationinsights.web.dependencies.apachecommons.lang3.RandomStringUtils;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.ObjectIdentity;
import com.microsoft.graph.models.PasswordProfile;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pip.account.management.config.ClientConfiguration;
import uk.gov.hmcts.reform.pip.account.management.config.UserConfiguration;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that wraps any interacts with Azure active directory.
 */
@Component
public class AzureUserService {

    @Autowired
    private GraphServiceClient<Request> graphClient;

    @Autowired
    private UserConfiguration userConfiguration;

    @Autowired
    private ClientConfiguration clientConfiguration;

    /**
     * Creates a new azureAccount in the Azure active directory.
     * @param azureAccount The azureAccount to add in the azure active directory.
     * @return The created user if it was successful.
     * @throws AzureCustomException thrown if theres an error with communicating with Azure.
     */
    public User createUser(AzureAccount azureAccount) throws AzureCustomException {
        try {
            User user = createUserObject(azureAccount);
            return graphClient.users()
                .buildRequest()
                .post(user);
        } catch (GraphServiceException e) {
            throw new AzureCustomException("Error when persisting account into Azure. "
                                               + "Check that the user doesn't already exist in the directory");
        }
    }

    private User createUserObject(AzureAccount azureAccount) {
        User user = new User();
        user.accountEnabled = true;
        user.displayName = azureAccount.getFirstName() + " " + azureAccount.getSurname();
        user.givenName = azureAccount.getFirstName();
        user.surname = azureAccount.getSurname();
        user.additionalDataManager().put(
            "extension_" + clientConfiguration.getExtensionId().replace("-", "") + "_UserRole",
            new JsonPrimitive(azureAccount.getRole().toString()));

        ObjectIdentity identity = new ObjectIdentity();
        identity.signInType = userConfiguration.getSignInType();
        identity.issuer = userConfiguration.getIdentityIssuer();
        identity.issuerAssignedId = azureAccount.getEmail();

        List<ObjectIdentity> identitiesList = new ArrayList<>();
        identitiesList.add(identity);

        PasswordProfile passwordProfile = new PasswordProfile();

        passwordProfile.password = RandomStringUtils.randomAscii(20);
        passwordProfile.forceChangePasswordNextSignIn = true;
        user.passwordProfile = passwordProfile;
        user.identities = identitiesList;

        return user;
    }

}
