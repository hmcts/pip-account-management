package uk.gov.hmcts.reform.pip.account.management.model;

/**
 * Provenance of where the user being added is coming from, if they have been added through the IDAM's or P&I, then
 * the userProvenanceId in PiUser relates to the unique identifier of the Provenance selected.
 */
public enum UserProvenances {
    CFT_IDAM,
    CRIME_IDAM,
    PI_AAD,
    THIRD_PARTY
}
