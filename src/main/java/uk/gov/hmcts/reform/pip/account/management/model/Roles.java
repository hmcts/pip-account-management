package uk.gov.hmcts.reform.pip.account.management.model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enum of the roles allowed for users of P&I, verified are media members, Internal are different levels of admin,
 * and the third party ones are consumers of P&I service such as Courtel.
 */
public enum Roles {
    VERIFIED,
    INTERNAL_SUPER_ADMIN_CTSC,
    INTERNAL_SUPER_ADMIN_LOCAL,
    INTERNAL_ADMIN_CTSC,
    INTERNAL_ADMIN_LOCAL,
    GENERAL_THIRD_PARTY,
    VERIFIED_THIRD_PARTY_CRIME,
    VERIFIED_THIRD_PARTY_CFT,
    VERIFIED_THIRD_PARTY_PRESS,
    VERIFIED_THIRD_PARTY_CRIME_CFT,
    VERIFIED_THIRD_PARTY_CRIME_PRESS,
    VERIFIED_THIRD_PARTY_CFT_PRESS,
    VERIFIED_THIRD_PARTY_ALL,
    SYSTEM_ADMIN;

    static final List<Roles> ALL_VERIFIED_THIRD_PARTY_CRIME_ROLES = List.of(
        VERIFIED_THIRD_PARTY_CRIME,
        VERIFIED_THIRD_PARTY_CRIME_CFT,
        VERIFIED_THIRD_PARTY_CRIME_PRESS,
        VERIFIED_THIRD_PARTY_ALL
    );

    static final List<Roles> ALL_VERIFIED_THIRD_PARTY_CFT_ROLES = List.of(
        VERIFIED_THIRD_PARTY_CFT,
        VERIFIED_THIRD_PARTY_CRIME_CFT,
        VERIFIED_THIRD_PARTY_CFT_PRESS,
        VERIFIED_THIRD_PARTY_ALL
    );

    static final List<Roles> ALL_VERIFIED_THIRD_PARTY_PRESS_ROLES = List.of(
        VERIFIED_THIRD_PARTY_PRESS,
        VERIFIED_THIRD_PARTY_CRIME_PRESS,
        VERIFIED_THIRD_PARTY_CFT_PRESS,
        VERIFIED_THIRD_PARTY_ALL
    );

    public static final List<Roles> ALL_THIRD_PARTY_ROLES = Stream.of(
            ALL_VERIFIED_THIRD_PARTY_CRIME_ROLES,
            ALL_VERIFIED_THIRD_PARTY_CFT_ROLES,
            ALL_VERIFIED_THIRD_PARTY_PRESS_ROLES,
            Collections.singletonList(GENERAL_THIRD_PARTY))
        .flatMap(Collection::stream)
        .distinct()
        .collect(Collectors.toList());

    public static final List<Roles> ALL_VERIFIED_ROLES = Stream.of(
            ALL_THIRD_PARTY_ROLES,
            Collections.singletonList(VERIFIED))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());

    public static final List<Roles> ALL_NON_THIRD_PARTY_ROLES = List.of(
        VERIFIED,
        INTERNAL_SUPER_ADMIN_CTSC,
        INTERNAL_SUPER_ADMIN_LOCAL,
        INTERNAL_ADMIN_CTSC,
        INTERNAL_ADMIN_LOCAL,
        SYSTEM_ADMIN
    );
}
