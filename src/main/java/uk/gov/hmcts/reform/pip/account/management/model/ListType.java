package uk.gov.hmcts.reform.pip.account.management.model;

public enum ListType {
    SJP_PUBLIC_LIST,
    SJP_PRESS_LIST(UserProvenances.PI_AAD),
    CROWN_DAILY_LIST,
    CROWN_FIRM_LIST,
    CROWN_WARNED_LIST,
    MAGS_PUBLIC_LIST,
    MAGS_STANDARD_LIST,
    CIVIL_DAILY_CAUSE_LIST,
    FAMILY_DAILY_CAUSE_LIST,
    TRIBUNAL_DAILY_CAUSE_LIST;

    public final UserProvenances allowedProvenance;

    ListType(UserProvenances allowedProvenance) {
        this.allowedProvenance = allowedProvenance;
    }

    ListType() {
        this.allowedProvenance = null;
    }
}
