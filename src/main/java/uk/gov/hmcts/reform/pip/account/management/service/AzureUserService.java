package uk.gov.hmcts.reform.pip.account.management.service;

import com.microsoft.applicationinsights.web.dependencies.apachecommons.lang3.RandomStringUtils;
import com.microsoft.graph.models.ObjectIdentity;
import com.microsoft.graph.models.PasswordProfile;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.config.ClientConfiguration;
import uk.gov.hmcts.reform.pip.account.management.config.UserConfiguration;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A class that wraps any interacts with Azure active directory.
 */
@Service
public class AzureUserService {

    private final GraphServiceClient graphClient;

    private final UserConfiguration userConfiguration;

    private final ClientConfiguration clientConfiguration;

    @Autowired
    public AzureUserService(
        GraphServiceClient graphClient,
        UserConfiguration userConfiguration,
        ClientConfiguration clientConfiguration
    ) {
        this.graphClient = graphClient;
        this.userConfiguration = userConfiguration;
        this.clientConfiguration = clientConfiguration;
    }

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
                .post(user);
        } catch (ApiException e) {
            throw new AzureCustomException("Error when persisting account into Azure. "
                                               + "Check that the user doesn't already exist in the directory");
        }
    }

    private User createUserObject(AzureAccount azureAccount, boolean useSuppliedPassword) {
        User user = new User();
        user.setAccountEnabled(true);
        user.setDisplayName(azureAccount.getFirstName() + " " + azureAccount.getSurname());
        user.setGivenName(azureAccount.getFirstName());
        user.setSurname(azureAccount.getSurname());
        user.setAdditionalData(Map.of(
            "extension_" + clientConfiguration.getExtensionId().replace("-", "") + "_UserRole",
            azureAccount.getRole().toString()));

        ObjectIdentity identity = new ObjectIdentity();
        identity.setSignInType(userConfiguration.getSignInType());
        identity.setIssuer(userConfiguration.getIdentityIssuer());
        identity.setIssuerAssignedId(azureAccount.getEmail());

        List<ObjectIdentity> identitiesList = new ArrayList<>();
        identitiesList.add(identity);

        PasswordProfile passwordProfile = new PasswordProfile();

        if (useSuppliedPassword) {
            passwordProfile.setPassword(azureAccount.getPassword());
            passwordProfile.setForceChangePasswordNextSignIn(false);
        } else {
            passwordProfile.setPassword(RandomStringUtils.randomAscii(20));
            passwordProfile.setForceChangePasswordNextSignIn(true);
        }
        user.setPasswordProfile(passwordProfile);
        user.setIdentities(identitiesList);

        return user;
    }

    /**
     * Get a azureAccount information from the Azure active directory.
     * @param email The azureAccount email address.
     * @return The created user if it was successful.
     * @throws AzureCustomException thrown if there's an error with communicating with Azure.
     */
    public User getUser(String email) throws AzureCustomException {
        try {
            UserCollectionResponse users = graphClient.users()
                .get((configuration) -> {
                    configuration.queryParameters.filter = String.format(
                        "identities/any(c:c/issuerAssignedId eq '%s' and c/issuer eq '%s')",
                        email, clientConfiguration.getB2cUrl());
                });

            User returnUser = null;
            if (users.getValue() != null && !users.getValue().isEmpty()) {
                returnUser = users.getValue().get(0);
            }
            return returnUser;

        } catch (ApiException e) {
            throw new AzureCustomException("Error when checking account into Azure.");
        }
    }

    /**
     * Delete an account from the Azure active directory.
     * @param userId The userId of the account to delete.
     * @throws AzureCustomException thrown if there is an error with communicating with Azure.
     */
    public void deleteUser(String userId) throws AzureCustomException {
        try {
            graphClient.users().byUserId(userId).delete();
        } catch (ApiException e) {
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
            user.setAdditionalData(Map.of(
                "extension_" + clientConfiguration.getExtensionId().replace("-", "") + "_UserRole",
                role
            ));

            return graphClient.users().byUserId(provenanceUserId)
                .patch(user);
        } catch (ApiException e) {
            throw new AzureCustomException("Error when updating account in Azure.");
        }
    }
}
