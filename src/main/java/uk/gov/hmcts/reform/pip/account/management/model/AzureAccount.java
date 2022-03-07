package uk.gov.hmcts.reform.pip.account.management.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.ValidEmail;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * Model that represents a Azure AzureAccount.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AzureAccount {

    /**
     * The is the object ID that is returned from azure.
     */
    @ApiModelProperty(hidden = true)
    private String azureAccountId;

    /**
     * The email address for the account.
     */
    @ValidEmail
    private String email;

    /**
     * The first name of the account.
     */
    @NotEmpty
    private String firstName;

    /**
     * The surname of the account.
     */
    @NotEmpty
    private String surname;

    /**
     * The role of the account.
     */
    @NotNull
    private Roles role;




}
