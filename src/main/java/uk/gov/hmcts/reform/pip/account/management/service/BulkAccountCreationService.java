package uk.gov.hmcts.reform.pip.account.management.service;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.CsvParseException;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.MediaCsv;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Service
public class BulkAccountCreationService {
    @Autowired
    AccountService accountService;

    @Autowired
    AzureAccountService azureAccountService;

    @Autowired
    AccountModelMapperService accountModelMapperService;

    public Map<CreationEnum, List<?>> uploadMediaFromCsv(MultipartFile mediaCsv, String issuerId) { //NOSONAR
        List<MediaCsv> mediaList;

        try (InputStreamReader inputStreamReader = new InputStreamReader(mediaCsv.getInputStream());
             Reader reader = new BufferedReader(inputStreamReader)) {

            CsvToBean<MediaCsv> csvToBean = new CsvToBeanBuilder<MediaCsv>(reader)
                .withType(MediaCsv.class)
                .build();

            mediaList = csvToBean.parse();
        } catch (Exception ex) {
            throw new CsvParseException(ex.getMessage());
        }

        return addToAzureAndPiUsers(mediaList, issuerId);
    }

    private Map<CreationEnum, List<?>> addToAzureAndPiUsers(List<MediaCsv> accounts, String issuerId) {
        Map<CreationEnum, List<? extends AzureAccount>> azureAccounts;
        Map<CreationEnum, List<?>> piUserAccounts;
        Map<CreationEnum, List<?>> completedAccounts = new ConcurrentHashMap<>();

        azureAccounts = azureAccountService.addAzureAccounts(
            accountModelMapperService.createAzureUsersFromCsv(accounts), issuerId, true, false
        );
        piUserAccounts = accountService.addUsers(
            accountModelMapperService
                .createPiUsersFromAzureAccounts(azureAccounts.get(CreationEnum.CREATED_ACCOUNTS)),
            issuerId
        );

        completedAccounts.put(
            CreationEnum.CREATED_ACCOUNTS,
            piUserAccounts.get(CreationEnum.CREATED_ACCOUNTS)
        );
        completedAccounts.put(
            CreationEnum.ERRORED_ACCOUNTS,
            Stream.concat(
                azureAccounts.get(CreationEnum.ERRORED_ACCOUNTS).stream(),
                piUserAccounts.get(CreationEnum.ERRORED_ACCOUNTS).stream()
            ).distinct().toList()
        );
        return completedAccounts;
    }
}
