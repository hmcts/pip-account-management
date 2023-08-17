package uk.gov.hmcts.reform.pip.account.management.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.AzureEmail;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.NotSystemAdmin;
import uk.gov.hmcts.reform.pip.model.account.Roles;

/**
 * Model that represents an Azure Account.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@NotSystemAdmin
public class AzureAccount {

    /**
     * The object ID that is returned from azure.
     */
    @Schema(hidden = true)
    private String azureAccountId;

    /**
     * The email address for the account.
     */
    @NotNull
    @NotEmpty
    @AzureEmail
    private String email;

    /**
     * The password for the account (used for testing support only).
     */
    private String password;

    /**
     * The first name of the account.
     */
    @NotEmpty
    private String firstName;

    /**
     * The surname of the account.
     */
    private String surname;

    /**
     * The role of the account.
     */
    @NotNull
    private Roles role;

    private String displayName;
}
