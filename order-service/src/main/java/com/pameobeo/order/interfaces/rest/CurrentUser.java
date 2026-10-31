package com.pameobeo.order.interfaces.rest;

import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Who is calling, read from the verified JWT (claims issued by auth-service). */
record CurrentUser(UUID id, String email, String name, boolean admin) {

    static CurrentUser from(JwtAuthenticationToken token) {
        List<String> roles = token.getToken().getClaimAsStringList("roles");
        return new CurrentUser(
                UUID.fromString(token.getToken().getSubject()),
                token.getToken().getClaimAsString("email"),
                token.getToken().getClaimAsString("name"),
                roles != null && roles.contains("ADMIN"));
    }
}
