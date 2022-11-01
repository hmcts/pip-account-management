package uk.gov.hmcts.reform.pip.account.management.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.AzureEmail;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * Model that represents an Azure Account.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AzureAccount {

    /**
     * The is the object ID that is returned from azure.
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




}
