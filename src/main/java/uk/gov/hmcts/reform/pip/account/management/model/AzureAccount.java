package uk.gov.hmcts.reform.pip.account.management.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
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
    @ApiModelProperty(hidden = true)
    private String azureAccountId;

    /**
     * The email address for the account.
     */
    @Email
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
