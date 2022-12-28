package uk.gov.hmcts.reform.pip.account.management.model;

import lombok.Data;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pip.account.management.model.Roles.ALL_VERIFIED_THIRD_PARTY_CFT_ROLES;
import static uk.gov.hmcts.reform.pip.account.management.model.Roles.ALL_VERIFIED_THIRD_PARTY_CRIME_ROLES;
import static uk.gov.hmcts.reform.pip.account.management.model.Roles.ALL_VERIFIED_THIRD_PARTY_PRESS_ROLES;
import static uk.gov.hmcts.reform.pip.account.management.model.Roles.GENERAL_THIRD_PARTY;
import static uk.gov.hmcts.reform.pip.account.management.model.Roles.VERIFIED;

@Data
public class CombinedRoles {
    protected final List<Roles> allThirdPartyRoles = Stream.of(
            ALL_VERIFIED_THIRD_PARTY_CRIME_ROLES,
            ALL_VERIFIED_THIRD_PARTY_CFT_ROLES,
            ALL_VERIFIED_THIRD_PARTY_PRESS_ROLES,
            Collections.singletonList(GENERAL_THIRD_PARTY))
        .flatMap(Collection::stream)
        .distinct()
        .toList();

    protected final List<Roles> allVerifiedRoles = Stream.of(
            allThirdPartyRoles,
            Collections.singletonList(VERIFIED))
        .flatMap(Collection::stream)
        .toList();
}
