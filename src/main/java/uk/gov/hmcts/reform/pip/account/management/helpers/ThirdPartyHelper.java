package uk.gov.hmcts.reform.pip.account.management.helpers;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

public final class ThirdPartyHelper {
    private ThirdPartyHelper() {
    }

    public static String createSecretKeyPrefix(String name, String userId) {
        String keyPrefix = name + '-' + userId + '-';
        String safeKeyPrefix = keyPrefix
            .toLowerCase(Locale.UK)
            .replaceAll("[^a-z0-9-]", "-")  // Replace invalid characters with 'hyphen'
            .replaceAll("(-)\\1+","-");     // Replace multiple 'hyphens' with single hyphen
        return StringUtils.stripStart(safeKeyPrefix, "-");
    }
}
