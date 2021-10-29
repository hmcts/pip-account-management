package uk.gov.hmcts.reform.pip.account.management.service;

import com.microsoft.graph.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.ErroredSubscriber;
import uk.gov.hmcts.reform.pip.account.management.model.Subscriber;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service layer that deals with the creation of accounts.
 * The storage mechanism (e.g Azure) is seperated into a seperate class.
 */
@Component
public class AccountService {

    @Autowired
    Validator validator;

    @Autowired
    AzureUserService azureUserService;

    @Autowired
    AzureTableService azureTableService;

    /**
     * Method to create new subscribers.
     * @return Returns a map which contains two lists, Errored and Created Subscribers. Created will have object ID set.
     **/
    public Map<CreationEnum, List<? extends Subscriber>> createSubscribers(List<Subscriber> subscribers) {

        Map<CreationEnum, List<? extends Subscriber>> processedAccounts = new ConcurrentHashMap<>();

        List<Subscriber> createdAccounts = new ArrayList<>();
        List<ErroredSubscriber> erroredAccounts = new ArrayList<>();

        for (Subscriber subscriber : subscribers) {

            Set<ConstraintViolation<Subscriber>> constraintViolationSet = validator.validate(subscriber);
            if (!constraintViolationSet.isEmpty()) {
                ErroredSubscriber erroredSubscriber = new ErroredSubscriber(subscriber);
                erroredSubscriber.setErrorMessages(constraintViolationSet
                                                       .stream().map(ConstraintViolation::getMessage)
                                                       .collect(Collectors.toList()));
                erroredAccounts.add(erroredSubscriber);
                continue;
            }

            try {
                User user = azureUserService.createUser(subscriber);
                subscriber.setAzureSubscriberId(user.id);

                String rowId = azureTableService.createUser(subscriber);
                subscriber.setTableSubscriberId(rowId);

                createdAccounts.add(subscriber);
            } catch (AzureCustomException azureCustomException) {
                ErroredSubscriber erroredSubscriber = new ErroredSubscriber(subscriber);
                erroredSubscriber.setErrorMessages(List.of(azureCustomException.getMessage()));
                erroredAccounts.add(erroredSubscriber);
            }

        }

        processedAccounts.put(CreationEnum.CREATED_ACCOUNTS, createdAccounts);
        processedAccounts.put(CreationEnum.ERRORED_ACCOUNTS, erroredAccounts);

        return processedAccounts;
    }

}
