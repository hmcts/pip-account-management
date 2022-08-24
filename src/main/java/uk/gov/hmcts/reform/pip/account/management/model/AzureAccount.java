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
     * The email address for the account. Regex comes from RFC 5322.
     */
    @NotNull
    @NotEmpty
    @Email(message = "Invalid email provided", regexp = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}"
        + "~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-"
        + "\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2"
        + "[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:"
        + "[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")
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
