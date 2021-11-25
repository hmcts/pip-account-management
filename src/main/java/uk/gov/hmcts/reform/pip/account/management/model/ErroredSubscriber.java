package uk.gov.hmcts.reform.pip.account.management.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Class which represents a Subscriber that has failed to be created.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ErroredSubscriber extends Subscriber {

    /**
     * This is the error messages for why the subscriber has failed to be created.
     */
    private List<String> errorMessages;

    /**
     * Constructor that takes in an existing subscriber and converts it to an errored subscriber.
     * @param subscriber The subscriber to be converted to an errored subscriber.
     */
    public ErroredSubscriber(Subscriber subscriber) {
        super(subscriber.getAzureSubscriberId(),
              subscriber.getEmail(),
              subscriber.getTitle(),
              subscriber.getFirstName(),
              subscriber.getSurname());
    }

}
