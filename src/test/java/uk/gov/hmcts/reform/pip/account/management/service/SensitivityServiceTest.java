package uk.gov.hmcts.reform.pip.account.management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.model.ListType;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Roles;
import uk.gov.hmcts.reform.pip.account.management.model.Sensitivity;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class SensitivityServiceTest {

    SensitivityService sensitivityService = new SensitivityService();

    @Test
    void checkPublicReturnsTrueWhenVerified() {
        PiUser piUser = new PiUser();
        piUser.setRoles(Roles.VERIFIED);
        piUser.setUserProvenance(UserProvenances.PI_AAD);

        assertTrue(
            sensitivityService.checkAuthorisation(piUser, ListType.CIVIL_DAILY_CAUSE_LIST, Sensitivity.PUBLIC),
            "Returned false for public sensitivity");
    }

    @Test
    void checkPublicReturnsTrueWhenNotVerified() {
        PiUser piUser = new PiUser();
        piUser.setRoles(Roles.INTERNAL_ADMIN_CTSC);
        piUser.setUserProvenance(UserProvenances.PI_AAD);

        assertTrue(
            sensitivityService.checkAuthorisation(piUser, ListType.CIVIL_DAILY_CAUSE_LIST, Sensitivity.PUBLIC),
            "Returned false for public sensitivity");
    }

    @Test
    void checkPrivateReturnsFalseWhenNotVerified() {
        PiUser piUser = new PiUser();
        piUser.setRoles(Roles.INTERNAL_ADMIN_CTSC);
        piUser.setUserProvenance(UserProvenances.PI_AAD);

        assertFalse(
            sensitivityService.checkAuthorisation(piUser, ListType.CIVIL_DAILY_CAUSE_LIST, Sensitivity.PRIVATE),
            "Returned true for private sensitivity when not verified");
    }

    @Test
    void checkPrivateReturnsTrueWhenVerified() {
        PiUser piUser = new PiUser();
        piUser.setRoles(Roles.VERIFIED);
        piUser.setUserProvenance(UserProvenances.PI_AAD);

        assertTrue(
            sensitivityService.checkAuthorisation(piUser, ListType.CIVIL_DAILY_CAUSE_LIST, Sensitivity.PRIVATE),
            "Returned false for private sensitivity when verified");
    }

    @Test
    void checkClassifiedReturnsTrueWhenVerifiedAndCorrectProvenance() {
        PiUser piUser = new PiUser();
        piUser.setRoles(Roles.VERIFIED);
        piUser.setUserProvenance(UserProvenances.CFT_IDAM);

        assertTrue(
            sensitivityService.checkAuthorisation(piUser, ListType.CIVIL_DAILY_CAUSE_LIST, Sensitivity.CLASSIFIED),
            "Returned false for classified sensitivity when verified and correct provenance");
    }

    @Test
    void checkClassifiedReturnsFalseWhenNotVerified() {
        PiUser piUser = new PiUser();
        piUser.setRoles(Roles.INTERNAL_ADMIN_CTSC);
        piUser.setUserProvenance(UserProvenances.CFT_IDAM);

        assertFalse(
            sensitivityService.checkAuthorisation(piUser, ListType.CIVIL_DAILY_CAUSE_LIST, Sensitivity.CLASSIFIED),
            "Returned true for classified sensitivity when not verified");
    }

    @Test
    void checkClassifiedReturnsFalseWhenVerifiedButNotMatchingProvenance() {
        PiUser piUser = new PiUser();
        piUser.setRoles(Roles.INTERNAL_ADMIN_CTSC);
        piUser.setUserProvenance(UserProvenances.PI_AAD);

        assertFalse(
            sensitivityService.checkAuthorisation(piUser, ListType.CIVIL_DAILY_CAUSE_LIST, Sensitivity.CLASSIFIED),
            "Returned true for classified sensitivity when verified with incorrect provenance");
    }







}
