package uk.gov.hmcts.reform.rsecheck.service;

import com.azure.core.http.rest.PagedIterable;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.TableEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.demo.model.Subscriber;
import uk.gov.hmcts.reform.demo.service.AzureTableService;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AzureTableServiceTest {

    @Mock
    TableClient tableClient;

    @Mock
    PagedIterable<TableEntity> tableEntities;

    @InjectMocks
    AzureTableService azureTableService;

    @Test
    public void createValidSubscriber() {
        Subscriber subscriber = new Subscriber();
        subscriber.setFirstName("First name");
        subscriber.setSurname("Surname");
        subscriber.setTitle("Title");
        subscriber.setEmail("a@b.com");

        when(tableClient.listEntities(any(), any(), any())).thenReturn(tableEntities);
        when(tableEntities.stream()).thenReturn(Stream.empty());

        Optional<String> returnedString = azureTableService.createUser(subscriber);

        assertTrue(returnedString.isPresent());
    }

    @Test
    public void verifyArgumentsPassedToCreate() {

    }

    @Test
    public void createASubscriberThatAlreadyExists() {
        Subscriber subscriber = new Subscriber();
        subscriber.setFirstName("First name");
        subscriber.setSurname("Surname");
        subscriber.setTitle("Title");
        subscriber.setEmail("a@b.com");

        when(tableClient.listEntities(any(), any(), any())).thenReturn(tableEntities);
        when(tableEntities.stream()).thenReturn(Stream.of(new TableEntity("a", "b")));

        Optional<String> returnedString = azureTableService.createUser(subscriber);

        assertFalse(returnedString.isPresent());
    }



}
