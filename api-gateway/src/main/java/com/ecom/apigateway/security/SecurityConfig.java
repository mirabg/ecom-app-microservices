package com.ecom.apigateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, OAuth2ResourceServerProperties oAuth2ResourceServerProperties) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchange -> exchange
                    // Route-level role checks (order matters: first match wins,
                    // so specific path rules must precede the anyExchange() catch-all).
                    // hasRole("X") checks for authority "ROLE_X", which matches what
                    // jwtAuthenticationConverter() produces (it already prefixes
                    // realm roles with "ROLE_").
                    .pathMatchers("/api/users/**").hasRole("USER")
                    .pathMatchers("/api/products/**").hasRole("PRODUCT")
                    .pathMatchers("/api/orders/**", "/api/cart/**").hasRole("ORDER")
                    .anyExchange().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Extract roles from the JWT and convert them to GrantedAuthority
            List<String> roles = extractRealmRoles(jwt);
            List<GrantedAuthority> authorities = roles.stream()
                    .map(role -> "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .map(GrantedAuthority.class::cast)
                    .toList();
            log.info("Extracted roles from JWT (sub={}): {}", jwt.getSubject(), authorities);
            return Flux.fromIterable(authorities);
        });
        return converter;
    }

    /**
     * Keycloak places realm-level roles under a nested "realm_access.roles" claim
     * rather than a flat top-level "roles" claim, e.g.:
     * {
     *   "realm_access": { "roles": ["PRODUCT", "offline_access", ...] }
     * }
     * Falls back to a flat "roles" claim for compatibility with other issuers.
     */
    private List<String> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> rolesList) {
            return rolesList.stream().map(String::valueOf).toList();
        }
        List<String> flatRoles = jwt.getClaimAsStringList("roles");
        return flatRoles != null ? flatRoles : Collections.emptyList();
    }
}
