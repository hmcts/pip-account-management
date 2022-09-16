package uk.gov.hmcts.reform.pip.account.management.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

import static uk.gov.hmcts.reform.pip.account.management.model.Roles.ALL_VERIFIED_THIRD_PARTY_CFT_ROLES;
import static uk.gov.hmcts.reform.pip.account.management.model.Roles.ALL_VERIFIED_THIRD_PARTY_CRIME_ROLES;
import static uk.gov.hmcts.reform.pip.account.management.model.Roles.ALL_VERIFIED_THIRD_PARTY_PRESS_ROLES;

@AllArgsConstructor
@Getter
public enum ListType {
    SJP_PUBLIC_LIST(UserProvenances.PI_AAD, ALL_VERIFIED_THIRD_PARTY_PRESS_ROLES),
    SJP_PRESS_LIST(UserProvenances.PI_AAD, ALL_VERIFIED_THIRD_PARTY_PRESS_ROLES),
    SJP_PRESS_REGISTER(UserProvenances.PI_AAD, ALL_VERIFIED_THIRD_PARTY_PRESS_ROLES),
    CROWN_DAILY_LIST(UserProvenances.CRIME_IDAM, ALL_VERIFIED_THIRD_PARTY_CRIME_ROLES),
    CROWN_FIRM_LIST(UserProvenances.CRIME_IDAM, ALL_VERIFIED_THIRD_PARTY_CRIME_ROLES),
    CROWN_WARNED_LIST(UserProvenances.CRIME_IDAM, ALL_VERIFIED_THIRD_PARTY_CRIME_ROLES),
    MAGISTRATES_PUBLIC_LIST(UserProvenances.CRIME_IDAM, ALL_VERIFIED_THIRD_PARTY_CRIME_ROLES),
    MAGISTRATES_STANDARD_LIST(UserProvenances.CRIME_IDAM, ALL_VERIFIED_THIRD_PARTY_CRIME_ROLES),
    CIVIL_DAILY_CAUSE_LIST(UserProvenances.CFT_IDAM, ALL_VERIFIED_THIRD_PARTY_CFT_ROLES),
    FAMILY_DAILY_CAUSE_LIST(UserProvenances.CFT_IDAM, ALL_VERIFIED_THIRD_PARTY_CFT_ROLES),
    CIVIL_AND_FAMILY_DAILY_CAUSE_LIST(UserProvenances.CFT_IDAM, ALL_VERIFIED_THIRD_PARTY_CFT_ROLES),
    COP_DAILY_CAUSE_LIST(UserProvenances.CFT_IDAM, ALL_VERIFIED_THIRD_PARTY_CFT_ROLES),
    SSCS_DAILY_LIST(UserProvenances.CFT_IDAM, ALL_VERIFIED_THIRD_PARTY_CFT_ROLES),
    IAC_DAILY_LIST(UserProvenances.CFT_IDAM, ALL_VERIFIED_THIRD_PARTY_CFT_ROLES),
    PRIMARY_HEALTH_LIST(UserProvenances.CFT_IDAM, ALL_VERIFIED_THIRD_PARTY_CFT_ROLES),
    CARE_STANDARDS_LIST(UserProvenances.CFT_IDAM, ALL_VERIFIED_THIRD_PARTY_CFT_ROLES);

    private final UserProvenances allowedProvenance;
    private final List<Roles> allowedThirdPartyRoles;
}
