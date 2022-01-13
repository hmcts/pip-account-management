package uk.gov.hmcts.reform.pip.account.management.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.ValidEmail;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.ValidName;

/**
 * Model that represents a subscriber.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ValidName
public class Subscriber {

    /**
     * The is the object ID that is returned from azure.
     */
    @ApiModelProperty(hidden = true)
    private String azureSubscriberId;

    /**
     * The email address for the subscriber.
     */
    @ValidEmail
    private String email;

    /**
     * The title of the subscriber.
     */
    private String title;

    /**
     * The first name of the subscriber.
     */
    private String firstName;

    /**
     * The surname of the subscriber.
     */
    private String surname;


}
