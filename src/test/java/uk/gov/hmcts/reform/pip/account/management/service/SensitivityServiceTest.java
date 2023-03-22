package uk.gov.hmcts.reform.pip.account.management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = {
        "GENERAL_THIRD_PARTY", "VERIFIED_THIRD_PARTY_CRIME", "VERIFIED_THIRD_PARTY_CFT",
        "VERIFIED_THIRD_PARTY_PRESS", "VERIFIED_THIRD_PARTY_CRIME_CFT", "VERIFIED_THIRD_PARTY_CRIME_PRESS",
        "VERIFIED_THIRD_PARTY_CFT_PRESS", "VERIFIED_THIRD_PARTY_ALL"
    })
    void checkPublicReturnsTrueForAllThirdPartyRoles(Roles roles) {
        PiUser piUser = new PiUser();
        piUser.setRoles(roles);
        piUser.setUserProvenance(UserProvenances.THIRD_PARTY);

        assertTrue(
            sensitivityService.checkAuthorisation(piUser, ListType.CIVIL_DAILY_CAUSE_LIST, Sensitivity.PUBLIC),
            "Returned false for public sensitivity with role " + roles.name());
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

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = {
        "VERIFIED", "GENERAL_THIRD_PARTY", "VERIFIED_THIRD_PARTY_CRIME", "VERIFIED_THIRD_PARTY_CFT",
        "VERIFIED_THIRD_PARTY_PRESS", "VERIFIED_THIRD_PARTY_CRIME_CFT", "VERIFIED_THIRD_PARTY_CRIME_PRESS",
        "VERIFIED_THIRD_PARTY_CFT_PRESS", "VERIFIED_THIRD_PARTY_ALL"
    })
    void checkPrivateReturnsTrueForAllVerifiedRoles(Roles roles) {
        PiUser piUser = new PiUser();
        piUser.setRoles(roles);
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

    @ParameterizedTest
    @MethodSource("parameters")
    void checkClassifiedReturnsTrueForAllowedThirdPartyRolesOnly(ListType listType, Roles roles,
                                                                 boolean isAuthorised) {
        PiUser piUser = new PiUser();
        piUser.setRoles(roles);
        piUser.setUserProvenance(UserProvenances.THIRD_PARTY);

        assertEquals(isAuthorised, sensitivityService.checkAuthorisation(piUser, listType, Sensitivity.CLASSIFIED),
                     String.format("Should return %s for list type %s and role %s", isAuthorised, listType, roles));
    }

    private static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of(ListType.SJP_PUBLIC_LIST, Roles.VERIFIED_THIRD_PARTY_ALL, true),
            Arguments.of(ListType.SJP_PUBLIC_LIST, Roles.VERIFIED_THIRD_PARTY_PRESS, true),
            Arguments.of(ListType.SJP_PRESS_LIST, Roles.VERIFIED_THIRD_PARTY_CRIME, false),
            Arguments.of(ListType.SJP_PRESS_LIST, Roles.VERIFIED_THIRD_PARTY_CRIME_CFT, false),
            Arguments.of(ListType.CROWN_DAILY_LIST, Roles.VERIFIED_THIRD_PARTY_ALL, true),
            Arguments.of(ListType.CROWN_FIRM_LIST, Roles.VERIFIED_THIRD_PARTY_CRIME, true),
            Arguments.of(ListType.CROWN_WARNED_LIST, Roles.VERIFIED_THIRD_PARTY_CRIME_PRESS, true),
            Arguments.of(ListType.MAGISTRATES_PUBLIC_LIST, Roles.VERIFIED_THIRD_PARTY_CFT, false),
            Arguments.of(ListType.MAGISTRATES_STANDARD_LIST, Roles.VERIFIED_THIRD_PARTY_CFT_PRESS, false),
            Arguments.of(ListType.CIVIL_DAILY_CAUSE_LIST, Roles.VERIFIED_THIRD_PARTY_ALL, true),
            Arguments.of(ListType.FAMILY_DAILY_CAUSE_LIST, Roles.VERIFIED_THIRD_PARTY_CFT, true),
            Arguments.of(ListType.CIVIL_AND_FAMILY_DAILY_CAUSE_LIST, Roles.VERIFIED_THIRD_PARTY_CRIME_CFT, true),
            Arguments.of(ListType.COP_DAILY_CAUSE_LIST, Roles.VERIFIED_THIRD_PARTY_CRIME, false),
            Arguments.of(ListType.SSCS_DAILY_LIST, Roles.VERIFIED_THIRD_PARTY_CRIME_PRESS, false),
            Arguments.of(ListType.SSCS_DAILY_LIST_ADDITIONAL_HEARINGS, Roles.VERIFIED_THIRD_PARTY_CRIME_PRESS, false),
            Arguments.of(ListType.IAC_DAILY_LIST, Roles.VERIFIED_THIRD_PARTY_CRIME_PRESS, false)
        );
    }
}
