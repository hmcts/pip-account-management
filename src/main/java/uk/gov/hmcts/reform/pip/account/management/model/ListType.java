package uk.gov.hmcts.reform.pip.account.management.model;

@SuppressWarnings("PMD.NullAssignment")
public enum ListType {
    SJP_PUBLIC_LIST(true),
    SJP_PRESS_LIST(UserProvenances.PI_AAD),
    CROWN_DAILY_LIST(true),
    CROWN_FIRM_LIST,
    CROWN_WARNED_LIST,
    MAGS_PUBLIC_LIST(true),
    MAGS_STANDARD_LIST,
    CIVIL_DAILY_CAUSE_LIST(true),
    FAMILY_DAILY_CAUSE_LIST(true),
    TRIBUNAL_DAILY_CAUSE_LIST(true);

    public final UserProvenances allowedProvenance;

    public final boolean isPublic;

    /**
     * Used to determine the provenances that can see classified lists.
     * @param allowedProvenance UserProvenance enum of allowed provenance.
     */
    ListType(UserProvenances allowedProvenance) {
        this.allowedProvenance = allowedProvenance;
        this.isPublic = false;
    }

    /**
     * Used to determine if the list type is public.
     * @param isPublic bool to flag if list is public.
     */
    ListType(boolean isPublic) {
        this.isPublic = true;
        this.allowedProvenance = null;
    }

    /**
     * default constructor that sets the values to negative of their checks, use to create private lists.
     */
    ListType() {
        this.allowedProvenance = null;
        this.isPublic = false;
    }
}
