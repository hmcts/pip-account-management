package uk.gov.hmcts.reform.pip.account.management.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.CsvParseException;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkAccountCreationServiceTest {
    private static final String EMAIL = "test@hmcts.net";
    private static final String ID = "1234";

    private static final PiUser PI_USER = new PiUser();
    private static final AzureAccount AZURE_ACCOUNT = new AzureAccount();

    @Mock
    private AzureAccountService azureAccountService;

    @Mock
    private AccountService accountService;

    @Mock
    private AccountModelMapperService accountModelMapperService;

    @InjectMocks
    private BulkAccountCreationService bulkAccountCreationService;

    @BeforeAll
    static void setup() {
        PI_USER.setUserId(UUID.randomUUID());
        PI_USER.setUserProvenance(UserProvenances.PI_AAD);
        PI_USER.setProvenanceUserId(ID);
        PI_USER.setEmail(EMAIL);

        AZURE_ACCOUNT.setEmail(EMAIL);
        AZURE_ACCOUNT.setRole(Roles.INTERNAL_ADMIN_CTSC);
    }

    @Test
    void testUploadMediaFromCsv() throws IOException {
        List<PiUser> azureUsers = List.of(PI_USER, PI_USER);
        List<AzureAccount> azureAccounts = List.of(AZURE_ACCOUNT, AZURE_ACCOUNT);

        when(accountModelMapperService.createAzureUsersFromCsv(any())).thenReturn(azureAccounts);
        when(accountModelMapperService.createPiUsersFromAzureAccounts(azureAccounts)).thenReturn(azureUsers);
        when(azureAccountService.addAzureAccounts(azureAccounts, EMAIL, true, false))
            .thenReturn(Map.of(
                CreationEnum.CREATED_ACCOUNTS, azureAccounts,
                CreationEnum.ERRORED_ACCOUNTS, Collections.emptyList()
            ));
        when(accountService.addUsers(azureUsers, EMAIL))
            .thenReturn(Map.of(
                CreationEnum.CREATED_ACCOUNTS, azureUsers,
                CreationEnum.ERRORED_ACCOUNTS, Collections.emptyList()
            ));

        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("csv/valid.csv")) {
            MultipartFile multipartFile = new MockMultipartFile("file", "TestFileName",
                                                                "text/plain",
                                                                IOUtils.toByteArray(inputStream)
            );

            Map<CreationEnum, List<?>> results = bulkAccountCreationService.uploadMediaFromCsv(multipartFile, EMAIL);
            assertThat(results.get(CreationEnum.CREATED_ACCOUNTS))
                .as("Created accounts should match")
                .hasSize(2)
                .isEqualTo(azureUsers);
            assertThat(results.get(CreationEnum.ERRORED_ACCOUNTS))
                .as("Error accounts should be empty")
                .isEmpty();
        }
    }

    @Test
    void testUploadMediaFromInvalidCsv() throws IOException {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("csv/invalidCsv.txt")) {
            MultipartFile multipartFile = new MockMultipartFile("file", "TestFileName",
                                                                "text/plain",
                                                                IOUtils.toByteArray(inputStream)
            );

            CsvParseException ex = assertThrows(CsvParseException.class, () ->
                                                    bulkAccountCreationService.uploadMediaFromCsv(multipartFile, EMAIL),
                                                "Should throw CsvParseException");
            assertTrue(ex.getMessage().contains("Failed to parse CSV File due to"), "Messages should match");
        }
    }
}
