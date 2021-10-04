package uk.gov.hmcts.reform.demo.service;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.ListEntitiesOptions;
import com.azure.data.tables.models.TableEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.demo.config.TableConfiguration;
import uk.gov.hmcts.reform.demo.model.Subscriber;

import java.util.Optional;
import java.util.UUID;

/**
 * A class that integrates with the Azure Table Service to manage subscribers.
 */
@Component
public class AzureTableService {

    @Autowired
    TableConfiguration tableConfiguration;

    @Autowired
    TableClient tableClient;

    /**
     * Method to create a subscriber.
     * @param subscriber The subscriber to create.
     * @return The ID of the subscriber if created.
     */
    public Optional<String> createUser(Subscriber subscriber) {

        if (subscriberExists(tableClient, subscriber.getEmail())) {
            return Optional.empty();
        }

        String key = UUID.randomUUID().toString();
        TableEntity tableEntity = new TableEntity(key, key)
            .addProperty("email", subscriber.getEmail())
            .addProperty("title", subscriber.getTitle())
            .addProperty("firstName", subscriber.getFirstName())
            .addProperty("surname", subscriber.getSurname())
            .addProperty("lastLoggedIn", "")
            .addProperty("status", "active");

        tableClient.createEntity(tableEntity);

        return Optional.of(key);
    }

    private boolean subscriberExists(TableClient tableClient, String email) {

        ListEntitiesOptions options = new ListEntitiesOptions()
            .setFilter(String.format("email eq '%s'", email));

        return tableClient.listEntities(options, null, null)
            .stream().count() > 0;

    }

}
