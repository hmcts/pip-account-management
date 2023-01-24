package uk.gov.hmcts.reform.pip.account.management.model;

/**
 * Enum that contains the different actions that are audited.
 */
public enum AuditAction {
    ATTEMPT_SYSTEM_ADMIN_CREATION,
    REFERENCE_DATA_UPLOAD,
    REFERENCE_DATA_DOWNLOAD,
    MANAGE_THIRD_PARTY,
    MANAGE_THIRD_PARTY_SUBSCRIPTIONS,
    USER_MANAGEMENT_VIEW,
    MANAGE_USER,
    UPDATE_USER,
    DELETE_USER,
    VIEW_BLOB_EXPLORER,
    BULK_MEDIA_UPLOAD
}
