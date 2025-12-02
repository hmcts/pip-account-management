package uk.gov.hmcts.reform.pip.account.management.validation.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ProvenanceValidatorTest {

    private static final String ROLE_PROVENANCE_TRUE_MSG = "Incorrectly returned true for "
        + "role and provenance combination";
    private static final String ROLE_PROVENANCE_FALSE_MSG = "Incorrectly returned false"
        + "for role and provenance combination";
    private static final ProvenanceValidator PROVENANCE_VALIDATOR = new ProvenanceValidator();

    private PiUser createUser(UserProvenances provenance, Roles role) {
        PiUser piUser = new PiUser();
        piUser.setUserProvenance(provenance);
        piUser.setRoles(role);
        piUser.setEmail("test-email@test-email.com");
        piUser.setProvenanceUserId(UUID.randomUUID().toString());
        return piUser;
    }

    @Test
    void testVerifiedReturnsFalseIfUserIsSsoProvenance() {
        PiUser piUser = createUser(UserProvenances.SSO, Roles.VERIFIED);
        assertFalse(PROVENANCE_VALIDATOR.isValid(piUser, null), ROLE_PROVENANCE_TRUE_MSG);
    }

    @Test
    void testVerifiedReturnsTrueIfUserIfNotSsoProvenance() {
        PiUser piUser = createUser(UserProvenances.PI_AAD, Roles.VERIFIED);
        assertTrue(PROVENANCE_VALIDATOR.isValid(piUser, null), ROLE_PROVENANCE_FALSE_MSG);
    }

    @Test
    void testThirdPartyReturnsFalseIfUserIsSsoProvenance() {
        PiUser piUser = createUser(UserProvenances.SSO, Roles.GENERAL_THIRD_PARTY);
        assertFalse(PROVENANCE_VALIDATOR.isValid(piUser, null), ROLE_PROVENANCE_TRUE_MSG);
    }

    @Test
    void testAdminReturnsFalseIfUserIsPiAadProvenance() {
        PiUser piUser = createUser(UserProvenances.PI_AAD, Roles.INTERNAL_ADMIN_CTSC);
        assertFalse(PROVENANCE_VALIDATOR.isValid(piUser, null), ROLE_PROVENANCE_TRUE_MSG);
    }

    @Test
    void testGeneralThirdPartyReturnsTrueIfUserIsThirdPartyProvenance() {
        PiUser piUser = createUser(UserProvenances.THIRD_PARTY, Roles.GENERAL_THIRD_PARTY);
        assertTrue(PROVENANCE_VALIDATOR.isValid(piUser, null), ROLE_PROVENANCE_FALSE_MSG);
    }

    @Test
    void testInternalSuperAdminCtscReturnsTrueIfUserIsSsoProvenance() {
        PiUser piUser = createUser(UserProvenances.SSO, Roles.INTERNAL_SUPER_ADMIN_CTSC);
        assertTrue(PROVENANCE_VALIDATOR.isValid(piUser, null), ROLE_PROVENANCE_FALSE_MSG);
    }

}
