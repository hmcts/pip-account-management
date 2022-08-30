package uk.gov.hmcts.reform.pip.account.management.model;

@SuppressWarnings("PMD.NullAssignment")
public enum ListType {
    SJP_PUBLIC_LIST(UserProvenances.PI_AAD),
    SJP_PRESS_LIST(UserProvenances.PI_AAD),
    SJP_PRESS_REGISTER(UserProvenances.PI_AAD),
    CROWN_DAILY_LIST(UserProvenances.CRIME_IDAM),
    CROWN_FIRM_LIST(UserProvenances.CRIME_IDAM),
    CROWN_WARNED_LIST(UserProvenances.CRIME_IDAM),
    MAGS_PUBLIC_LIST(UserProvenances.CRIME_IDAM),
    MAGS_STANDARD_LIST(UserProvenances.CRIME_IDAM),
    CIVIL_DAILY_CAUSE_LIST(UserProvenances.CFT_IDAM),
    FAMILY_DAILY_CAUSE_LIST(UserProvenances.CFT_IDAM),
    CIVIL_AND_FAMILY_DAILY_CAUSE_LIST(UserProvenances.CFT_IDAM),
    COP_DAILY_CAUSE_LIST(UserProvenances.CFT_IDAM),
    SSCS_DAILY_LIST(UserProvenances.CFT_IDAM);

    public final UserProvenances allowedProvenance;

    ListType(UserProvenances allowedProvenance) {
        this.allowedProvenance = allowedProvenance;
    }
}
