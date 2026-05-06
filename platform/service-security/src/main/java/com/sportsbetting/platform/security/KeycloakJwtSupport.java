package com.sportsbetting.platform.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Keycloak-compatible JWT converter: merges {@code realm_access.roles} as {@code ROLE_*} authorities
 * with standard OAuth2 {@code scope} claims ({@link JwtGrantedAuthoritiesConverter}).
 */
public final class KeycloakJwtSupport {

    private KeycloakJwtSupport() {}

    public static JwtAuthenticationConverter realmRolesJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> realmRolesPlusScopes(scopes, jwt));
        return converter;
    }

    private static Collection<GrantedAuthority> realmRolesPlusScopes(
            JwtGrantedAuthoritiesConverter scopes,
            Jwt jwt
    ) {
        Collection<GrantedAuthority> combined = new ArrayList<>(scopes.convert(jwt));
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return Collections.unmodifiableCollection(combined);
        }
        Object rolesObj = realmAccess.get("roles");
        if (!(rolesObj instanceof List<?> roles)) {
            return Collections.unmodifiableCollection(combined);
        }
        for (Object r : roles) {
            if (r == null) {
                continue;
            }
            String name = r.toString();
            if (name.isBlank()) {
                continue;
            }
            String authority = name.startsWith("ROLE_") ? name : "ROLE_" + name;
            combined.add(new SimpleGrantedAuthority(authority));
        }
        return Collections.unmodifiableCollection(combined);
    }
}
