package uk.gov.hmcts.reform.pip.account.management.service;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.ListEntitiesOptions;
import com.azure.data.tables.models.TableEntity;
import com.azure.data.tables.models.TableServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pip.account.management.config.TableConfiguration;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.model.AccountStatus;
import uk.gov.hmcts.reform.pip.account.management.model.Subscriber;

import java.util.UUID;

/**
 * A class that integrates with the Azure Table Service to manage subscribers.
 */
@Component
public class AzureTableService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureTableService.class);

    @Autowired
    TableConfiguration tableConfiguration;

    @Autowired
    TableClient tableClient;

    /**
     * Method to create a subscriber.
     * @param subscriber The subscriber to create.
     * @return The ID of the subscriber if created.
     */
    public String createUser(Subscriber subscriber) throws AzureCustomException {
        try {
            if (subscriberExists(tableClient, subscriber.getEmail())) {
                throw new AzureCustomException("A user with this email already exists in the table");
            }

            String key = UUID.randomUUID().toString();
            TableEntity tableEntity = new TableEntity(key, key)
                .addProperty("email", subscriber.getEmail())
                .addProperty("title", subscriber.getTitle())
                .addProperty("firstName", subscriber.getFirstName())
                .addProperty("surname", subscriber.getSurname())
                .addProperty("lastLoggedIn", "")
                .addProperty("status", AccountStatus.ACTIVE);


            tableClient.createEntity(tableEntity);
            return key;
        } catch (TableServiceException e) {
            LOGGER.error(e.getMessage());
            throw new AzureCustomException("Error while persisting subscriber into the table service");
        }
    }

    private boolean subscriberExists(TableClient tableClient, String email) {

        ListEntitiesOptions options = new ListEntitiesOptions()
            .setFilter(String.format("email eq '%s'", email));

        return tableClient.listEntities(options, null, null)
            .stream().count() > 0;

    }

}
