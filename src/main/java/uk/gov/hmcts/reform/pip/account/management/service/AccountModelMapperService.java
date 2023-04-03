package uk.gov.hmcts.reform.pip.account.management.service;

import com.microsoft.applicationinsights.core.dependencies.google.common.base.Strings;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.MediaCsv;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.util.ArrayList;
import java.util.List;

@Service
public class AccountModelMapperService {

    public List<AzureAccount> createAzureUsersFromCsv(List<MediaCsv> csvList) {
        List<AzureAccount> azureAccounts = new ArrayList<>();
        csvList.forEach(csvEntry -> {
            AzureAccount azureAccount = new AzureAccount();
            azureAccount.setEmail(csvEntry.getEmail());
            azureAccount.setSurname(csvEntry.getSurname());
            azureAccount.setFirstName(Strings.isNullOrEmpty(csvEntry.getFirstName())
                                          ? csvEntry.getEmail() : csvEntry.getFirstName());
            azureAccount.setRole(Roles.VERIFIED);
            azureAccounts.add(azureAccount);
        });
        return azureAccounts;
    }

    public List<PiUser> createPiUsersFromAzureAccounts(List<? extends AzureAccount> azureAccounts) {
        List<PiUser> piUsers = new ArrayList<>();
        azureAccounts.forEach(azureAccount -> {
            PiUser piUser = new PiUser();
            piUser.setUserProvenance(UserProvenances.PI_AAD);
            piUser.setProvenanceUserId(azureAccount.getAzureAccountId());
            piUser.setEmail(azureAccount.getEmail());
            piUser.setRoles(Roles.VERIFIED);
            piUsers.add(piUser);
        });
        return piUsers;
    }
}
