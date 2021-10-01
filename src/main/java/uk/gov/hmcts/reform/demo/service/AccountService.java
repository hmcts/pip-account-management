package uk.gov.hmcts.reform.demo.service;

import com.microsoft.graph.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.demo.model.CreationEnum;
import uk.gov.hmcts.reform.demo.model.Subscriber;

import java.util.*;

/**
 * Service layer that deals with the creation of accounts.
 * The storage mechanism (e.g Azure) is seperated into a seperate class.
 */
@Component
public class AccountService {

    @Autowired
    AzureUserService azureUserService;

    /**
     * Method to create new subscribers.
     * @return Returns a map which contains two lists:
     * -Errored Subscribers
     * -Created Subscribers
     * Created subscribers will have their object ID set.
     */
    public Map<CreationEnum, List<Subscriber>> createSubscribers(List<Subscriber> subscribers) {

        Map<CreationEnum, List<Subscriber>> processedAccounts = new HashMap<>();

        List<Subscriber> createdAccounts = new ArrayList<>();
        List<Subscriber> erroredAccounts = new ArrayList<>();

        for (Subscriber subscriber : subscribers) {
            Optional<User> optionalUser = azureUserService.createUser(subscriber);
            if (optionalUser.isPresent()) {
                subscriber.setSubscriberObjectId(optionalUser.get().id);
                createdAccounts.add(subscriber);
            } else {
                erroredAccounts.add(subscriber);
            }
        }

        processedAccounts.put(CreationEnum.CREATED_ACCOUNTS, createdAccounts);
        processedAccounts.put(CreationEnum.ERRORED_ACCOUNTS, erroredAccounts);

        return processedAccounts;
    }

}
