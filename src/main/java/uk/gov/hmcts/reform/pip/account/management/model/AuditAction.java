package uk.gov.hmcts.reform.pip.account.management.model;

/**
 * Enum that contains the different actions that are audited.
 */
public enum AuditAction {
    ATTEMPT_SYSTEM_ADMIN_CREATION,
    SYSTEM_ADMIN_CREATION,
    REFERENCE_DATA_UPLOAD,
    REFERENCE_DATA_DOWNLOAD,
    VIEW_THIRD_PARTY_USERS,
    MANAGE_THIRD_PARTY_USER_VIEW,
    MANAGE_THIRD_PARTY_USER_SUBSCRIPTIONS,
    USER_MANAGEMENT_VIEW,
    MANAGE_USER,
    UPDATE_USER,
    DELETE_USER,
    VIEW_BLOB_EXPLORER,
    BULK_MEDIA_UPLOAD
}
