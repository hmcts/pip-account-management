package uk.gov.hmcts.reform.demo.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import uk.gov.hmcts.reform.demo.validation.annotations.ValidEmail;
import uk.gov.hmcts.reform.demo.validation.annotations.ValidName;

/**
 * Model that represents a subscriber.
 */
@Data
@ValidName
public class Subscriber {

    /**
     * The is the object ID that is returned from azure.
     */
    @ApiModelProperty(hidden = true)
    private String azureSubscriberId;

    /**
     * This is the ID for the subscriber in the table store.
     */
    @ApiModelProperty(hidden = true)
    private String tableSubscriberId;

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
