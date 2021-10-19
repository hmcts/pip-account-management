package uk.gov.hmcts.reform.pip.account.management.service;

import com.azure.core.http.rest.PagedIterable;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.TableEntity;
import com.azure.data.tables.models.TableServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.model.AccountStatus;
import uk.gov.hmcts.reform.pip.account.management.model.Subscriber;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureTableServiceTest {

    @Mock
    TableClient tableClient;

    @Mock
    PagedIterable<TableEntity> tableEntities;

    @InjectMocks
    AzureTableService azureTableService;

    private static final String FIRST_NAME = "First name";
    private static final String SURNAME = "Surname";
    private static final String TITLE = "Title";
    private static final String EMAIL = "a@b.com";

    @Test
    void createValidSubscriber() {
        Subscriber subscriber = new Subscriber();
        subscriber.setFirstName(FIRST_NAME);
        subscriber.setSurname(SURNAME);
        subscriber.setTitle(TITLE);
        subscriber.setEmail(EMAIL);

        when(tableClient.listEntities(any(), any(), any())).thenReturn(tableEntities);
        when(tableEntities.stream()).thenReturn(Stream.empty());

        Optional<String> subscriberId = azureTableService.createUser(subscriber);

        assertTrue(subscriberId.isPresent(), "Subscriber ID has been generated");
    }

    @Test
    void verifyArgumentsPassedToCreate() {
        Subscriber subscriber = new Subscriber();
        subscriber.setFirstName(FIRST_NAME);
        subscriber.setSurname(SURNAME);
        subscriber.setTitle(TITLE);
        subscriber.setEmail(EMAIL);

        when(tableClient.listEntities(any(), any(), any())).thenReturn(tableEntities);
        when(tableEntities.stream()).thenReturn(Stream.empty());

        azureTableService.createUser(subscriber);

        ArgumentCaptor<TableEntity> captor = ArgumentCaptor.forClass(TableEntity.class);
        verify(tableClient, times(1)).createEntity(captor.capture());
        TableEntity tableEntity = captor.getValue();

        assertEquals(TITLE, tableEntity.getProperty("title").toString(),
                     "Subscriber contains expected title");
        assertEquals(FIRST_NAME, tableEntity.getProperty("firstName").toString(),
                     "Subscriber contains expected first name");
        assertEquals(SURNAME, tableEntity.getProperty("surname").toString(),
                     "Subscriber contains expected surname");
        assertEquals(EMAIL, tableEntity.getProperty("email").toString(),
                     "Subscriber contains expected email");
        assertEquals(AccountStatus.ACTIVE, tableEntity.getProperty("status"),
                     "Subscriber contains expected status");
    }

    @Test
    void createASubscriberThatAlreadyExists() {
        Subscriber subscriber = new Subscriber();
        subscriber.setFirstName(FIRST_NAME);
        subscriber.setSurname(SURNAME);
        subscriber.setTitle(TITLE);
        subscriber.setEmail(EMAIL);

        when(tableClient.listEntities(any(), any(), any())).thenReturn(tableEntities);
        when(tableEntities.stream()).thenReturn(Stream.of(new TableEntity("a", "b")));

        Optional<String> returnedString = azureTableService.createUser(subscriber);

        assertFalse(returnedString.isPresent(), "Table ID is not present");
    }

    @Test
    void createASubscriberTableServiceException() {

        Subscriber subscriber = new Subscriber();
        subscriber.setFirstName(FIRST_NAME);
        subscriber.setSurname(SURNAME);
        subscriber.setTitle(TITLE);
        subscriber.setEmail(EMAIL);

        when(tableClient.listEntities(any(), any(), any()))
            .thenThrow(new TableServiceException("Response", null));

        Optional<String> returnedString = azureTableService.createUser(subscriber);

        assertFalse(returnedString.isPresent(),
                    "Table ID is not present when azure exception occurs");
    }

}
