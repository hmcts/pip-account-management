package uk.gov.hmcts.reform.pip.account.management.service;

import com.google.gson.JsonPrimitive;
import com.microsoft.applicationinsights.web.dependencies.apachecommons.lang3.RandomStringUtils;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.ObjectIdentity;
import com.microsoft.graph.models.PasswordProfile;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionPage;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.config.ClientConfiguration;
import uk.gov.hmcts.reform.pip.account.management.config.UserConfiguration;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that wraps any interacts with Azure active directory.
 */
@Service
public class AzureUserService {

    @Autowired
    private GraphServiceClient<Request> graphClient;

    @Autowired
    private UserConfiguration userConfiguration;

    @Autowired
    private ClientConfiguration clientConfiguration;

    /**
     * Creates a new azureAccount in the Azure active directory.
     * @param azureAccount          The azureAccount to add in the azure active directory.
     * @param useSuppliedPassword   Create password using the supplied value.
     * @return The created user if it was successful.
     * @throws AzureCustomException thrown if there is an error with communicating with Azure.
     */
    public User createUser(AzureAccount azureAccount, boolean useSuppliedPassword) throws AzureCustomException {
        try {
            User user = createUserObject(azureAccount, useSuppliedPassword);
            return graphClient.users()
                .buildRequest()
                .post(user);
        } catch (GraphServiceException e) {
            throw new AzureCustomException("Error when persisting account into Azure. "
                                               + "Check that the user doesn't already exist in the directory");
        }
    }

    private User createUserObject(AzureAccount azureAccount, boolean useSuppliedPassword) {
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

        if (useSuppliedPassword) {
            passwordProfile.password = azureAccount.getPassword();
            passwordProfile.forceChangePasswordNextSignIn = false;
        } else {
            passwordProfile.password = RandomStringUtils.randomAscii(20);
            passwordProfile.forceChangePasswordNextSignIn = true;
        }
        user.passwordProfile = passwordProfile;
        user.identities = identitiesList;

        return user;
    }

    /**
     * Get a azureAccount information from the Azure active directory.
     * @param email The azureAccount email address.
     * @return The created user if it was successful.
     * @throws AzureCustomException thrown if theres an error with communicating with Azure.
     */
    public User getUser(String email) throws AzureCustomException {
        try {
            UserCollectionPage users = graphClient.users()
                .buildRequest()
                .filter(String.format("identities/any(c:c/issuerAssignedId eq '%s' and c/issuer eq '%s')", email,
                                      clientConfiguration.getB2cUrl()))
                .get();

            User returnUser = null;
            if (users.getCurrentPage().stream().count() != 0) {
                returnUser = users.getCurrentPage().get(0);
            }
            return returnUser;

        } catch (GraphServiceException e) {
            throw new AzureCustomException("Error when checking account into Azure.");
        }
    }

    /**
     * Delete an account from the Azure active directory.
     * @param userId The userId of the account to delete.
     * @return The deleted user if it was successful.
     * @throws AzureCustomException thrown if there is an error with communicating with Azure.
     */
    public User deleteUser(String userId) throws AzureCustomException {
        try {
            return graphClient.users(userId).buildRequest().delete();
        } catch (GraphServiceException e) {
            throw new AzureCustomException("Error when deleting account in Azure.");
        }
    }

    /**
     * Updates an account in the Azure active directory.
     * @param provenanceUserId The provenanceUserId of the account to update.
     * @param role The updated role for the user.
     * @return The update user if it was successful.
     * @throws AzureCustomException thrown if there is an error with communicating with Azure.
     */
    public User updateUserRole(String provenanceUserId, String role) throws AzureCustomException {
        try {
            User user = new User();
            user.additionalDataManager().put(
                "extension_" + clientConfiguration.getExtensionId().replace("-", "") + "_UserRole",
                new JsonPrimitive(role)
            );

            return graphClient.users(provenanceUserId)
                .buildRequest()
                .patch(user);
        } catch (GraphServiceException e) {
            throw new AzureCustomException("Error when updating account in Azure.");
        }
    }
}
