package uk.gov.hmcts.reform.pip.account.management.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.dto.MiReportData;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.helpers.EmailHelper;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.util.List;

import static uk.gov.hmcts.reform.pip.model.account.Roles.ALL_NON_RESTRICTED_ADMIN_ROLES;
import static uk.gov.hmcts.reform.pip.model.account.Roles.ALL_NON_THIRD_PARTY_ROLES;
import static uk.gov.hmcts.reform.pip.model.account.UserProvenances.ALL_NON_THIRD_PARTY_PROVENANCES;

@Service
public class AccountFilteringService {
    private final UserRepository userRepository;

    @Autowired
    public AccountFilteringService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Method which will retrieve MI reporting data for all accounts.
     *
     * @return A list of MI Data objects for all accounts.
     */
    public List<MiReportData> getAccManDataForMiReporting() {
        return userRepository.getAccManDataForMI();
    }

    /**
     * Method which will retrieve all accounts which are third party.
     *
     * @return The list of found PiUser accounts.
     */
    public List<PiUser> findAllThirdPartyAccounts() {
        return userRepository.findAllByUserProvenance(UserProvenances.THIRD_PARTY);
    }

    /**
     * Request a page of users that can be filtered down with the supplied parameters.
     *
     * @param pageable The pageable object.
     * @param email The email to query by.
     * @param userProvenanceId The user provenance id to query by.
     * @param userProvenances A list of user provenances to query by.
     * @param roles A list of roles to query by.
     * @param userId The user id to query by.
     * @return A page with a list of piUsers.
     */
    public Page<PiUser> findAllAccountsExceptThirdParty(Pageable pageable, String email, String userProvenanceId,
                                                        List<UserProvenances> userProvenances, List<Roles> roles,
                                                        String userId) {

        // If a user ID is supplied then only find by that
        if (!userId.isBlank()) {
            return userRepository.findByUserIdPageable(userId, pageable);
        }

        //  If a list of user provenances are supplied then use that else use all non third party user provenances
        List<UserProvenances> userProvenancesToQuery = ALL_NON_THIRD_PARTY_PROVENANCES;
        if (!userProvenances.isEmpty()) {
            userProvenancesToQuery = userProvenances;
        }

        // If a list of roles are supplied then use that else use all non third party roles
        List<Roles> rolesToQuery = ALL_NON_THIRD_PARTY_ROLES;
        if (!roles.isEmpty()) {
            rolesToQuery = roles;
        }

        // If user provenance id is supplied then find by an exact match
        String userProvenanceIdToQuery = "%%";
        if (!userProvenanceId.isBlank()) {
            userProvenanceIdToQuery = userProvenanceId;
        }

        return userRepository.findAllByEmailLikeIgnoreCaseAndUserProvenanceInAndRolesInAndProvenanceUserIdLike(
            "%" + email + "%",
            userProvenancesToQuery,
            rolesToQuery,
            userProvenanceIdToQuery,
            pageable
        );
    }

    /**
     * This method retrieves an admin user (excluding system admin) by their email and provenance.
     * @param email The email of the user to retrieve
     * @param provenance The provenance of the user to retrieve
     * @return The user that is found.
     * @throws NotFoundException if a user is not found.
     */
    public PiUser getAdminUserByEmailAndProvenance(String email, UserProvenances provenance) {
        return userRepository.findByEmailIgnoreCaseAndUserProvenanceAndRolesIn(email, provenance,
                                                                               ALL_NON_RESTRICTED_ADMIN_ROLES)
            .orElseThrow(() -> new NotFoundException(String.format(
                "No user found with the email: %s and provenance %s", EmailHelper.maskEmail(email), provenance)));
    }
}
