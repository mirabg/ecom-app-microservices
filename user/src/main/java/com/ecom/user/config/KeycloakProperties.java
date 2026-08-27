package com.ecom.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Connection details for the Keycloak Admin REST API, used by
 * {@link com.ecom.user.client.KeycloakAdminClient} to provision users.
 */
@Component
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    /** Base URL of the Keycloak server, e.g. http://localhost:8180 (no trailing slash). */
    private String serverUrl;

    /** Realm that application end-users live in, e.g. ecom-app. */
    private String realm;

    /**
     * Confidential client id whose service account is used solely for Admin
     * REST API calls (must have the realm-management "manage-users" role).
     */
    private String adminClientId;

    private String adminClientSecret;

    /** Realm role assigned to every newly created user, if present in the realm. */
    private String defaultRole;

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getAdminClientId() {
        return adminClientId;
    }

    public void setAdminClientId(String adminClientId) {
        this.adminClientId = adminClientId;
    }

    public String getAdminClientSecret() {
        return adminClientSecret;
    }

    public void setAdminClientSecret(String adminClientSecret) {
        this.adminClientSecret = adminClientSecret;
    }

    public String getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(String defaultRole) {
        this.defaultRole = defaultRole;
    }
}

