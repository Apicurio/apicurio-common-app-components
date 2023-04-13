/*
 * Copyright 2021 Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apicurio.common.apps.multitenancy;

import io.apicurio.common.apps.config.Info;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.util.Optional;

/**
 * @author Carles Arnal
 */
@ApplicationScoped
public class MultitenancyProperties {

    @Inject
    @ConfigProperty(name = "app.enable.multitenancy", defaultValue = "false")
    @Info(category = "mt", description = "Enable multitenancy", availableSince = "2.0.0.Final")
    boolean multitenancyEnabled;

    @Inject
    @ConfigProperty(name = "app.multitenancy.authorization.enabled", defaultValue = "true")
    @Info(category = "mt", description = "Enable multitenancy authorization", availableSince = "2.1.0.Final")
    boolean mtAuthorizationEnabled;

    @Inject
    @ConfigProperty(name = "app.multitenancy.types.context-path.enabled", defaultValue = "true")
    @Info(category = "mt", description = "Enable multitenancy context path type", availableSince = "2.1.0.Final")
    boolean mtContextPathEnabled;

    @Inject
    @ConfigProperty(name = "app.multitenancy.types.subdomain.enabled", defaultValue = "false")
    @Info(category = "mt", description = "Enable multitenancy subdomain type", availableSince = "2.1.0.Final")
    boolean mtSubdomainEnabled;

    @Inject
    @ConfigProperty(name = "app.multitenancy.types.request-header.enabled", defaultValue = "true")
    @Info(category = "mt", description = "Enable multitenancy request header type", availableSince = "2.1.0.Final")
    boolean mtRequestHeaderEnabled;

    @Inject
    @ConfigProperty(name = "app.multitenancy.types.context-path.base-path", defaultValue = "t")
    @Info(category = "mt", description = "Multitenancy context path type base path", availableSince = "2.1.0.Final")
    String nameMultitenancyBasePath;

    @Inject
    @ConfigProperty(name = "app.multitenancy.types.subdomain.location", defaultValue = "header")
    @Info(category = "mt", description = "Multitenancy subdomain type location", availableSince = "2.1.0.Final")
    String subdomainMultitenancyLocation;

    @Inject
    @ConfigProperty(name = "app.multitenancy.types.subdomain.header-name", defaultValue = "Host")
    @Info(category = "mt", description = "Multitenancy subdomain type header name", availableSince = "2.1.0.Final")
    String subdomainMultitenancyHeaderName;

    @Inject
    @ConfigProperty(name = "app.multitenancy.types.subdomain.pattern", defaultValue = "(\\w[\\w\\d\\-]*)\\.localhost\\.local")
    @Info(category = "mt", description = "Multitenancy subdomain type pattern", availableSince = "2.1.0.Final")
    String subdomainMultitenancyPattern;

    @Inject
    @ConfigProperty(name = "app.multitenancy.types.request-header.name", defaultValue = "X-Registry-Tenant-Id")
    @Info(category = "mt", description = "Multitenancy request header type name", availableSince = "2.1.0.Final")
    String tenantIdRequestHeader;

    @Inject
    @ConfigProperty(name = "app.multitenancy.reaper.every")
    @Info(category = "mt", description = "Multitenancy reaper every", availableSince = "2.1.0.Final")
    Optional<String> reaperEvery;

    @Inject
    @ConfigProperty(name = "app.multitenancy.reaper.period-seconds", defaultValue = "10800")
    @Info(category = "mt", description = "Multitenancy reaper period seconds", availableSince = "2.1.0.Final")
    Long reaperPeriodSeconds;

    @Inject
    @ConfigProperty(name = "app.tenant.manager.url")
    @Info(category = "mt", description = "Tenant manager URL", availableSince = "2.0.0.Final")
    Optional<String> tenantManagerUrl;

    @Inject
    @ConfigProperty(name = "app.tenant.manager.ssl.ca.path")
    @Info(category = "mt", description = "Tenant manager SSL Ca path", availableSince = "2.2.0.Final")
    Optional<String> tenantManagerCAFilePath;

    @Inject
    @ConfigProperty(name = "app.tenant.manager.auth.enabled")
    @Info(category = "mt", description = "Tenant manager auth enabled", availableSince = "2.1.0.Final")
    Optional<Boolean> tenantManagerAuthEnabled;

    @Inject
    @ConfigProperty(name = "app.tenant.manager.auth.url.configured")
    @Info(category = "mt", description = "Tenant manager auth url configured", availableSince = "2.1.0.Final")
    Optional<String> tenantManagerAuthUrl;

    @Inject
    @ConfigProperty(name = "app.tenant.manager.auth.client-id")
    @Info(category = "mt", description = "Tenant manager auth client ID", availableSince = "2.1.0.Final")
    Optional<String> tenantManagerClientId;

    @Inject
    @ConfigProperty(name = "app.tenant.manager.auth.client-secret")
    @Info(category = "mt", description = "Tenant manager auth client secret", availableSince = "2.1.0.Final")
    Optional<String> tenantManagerClientSecret;

    @Inject
    @ConfigProperty(name = "app.tenant.manager.auth.token.expiration.reduction.ms")
    @Info(category = "mt", description = "Tenant manager auth token expiration reduction ms", availableSince = "2.2.0.Final")
    Optional<Long> tenantManagerAuthTokenExpirationReductionMs;

    @PostConstruct
    void init() {
        this.reaperEvery.orElseThrow(() -> new IllegalArgumentException("Missing required configuration property 'app.multitenancy.reaper.every'"));
    }

    /**
     * @return the multitenancyEnabled
     */
    public boolean isMultitenancyEnabled() {
        return multitenancyEnabled;
    }

    /**
     * @return true if multitenancy authorization is enabled
     */
    public boolean isMultitenancyAuthorizationEnabled() {
        return mtAuthorizationEnabled;
    }

    /**
     * @return true if multitenancy context paths are enabled
     */
    public boolean isMultitenancyContextPathEnabled() {
        return mtContextPathEnabled;
    }

    /**
     * @return true if multitenancy subdomains are enabled
     */
    public boolean isMultitenancySubdomainEnabled() {
        return mtSubdomainEnabled;
    }

    /**
     * @return true if multitenancy request headers are enabled
     */
    public boolean isMultitenancyRequestHeaderEnabled() {
        return mtRequestHeaderEnabled;
    }

    /**
     * @return the nameMultitenancyBasePath
     */
    public String getNameMultitenancyBasePath() {
        return nameMultitenancyBasePath;
    }

    /**
     * @return the subdomain location (e.g. "header" or "serverName")
     */
    public String getSubdomainMultitenancyLocation() {
        return subdomainMultitenancyLocation;
    }

    /**
     * @return the subdomain header name (when the location is "header")
     */
    public String getSubdomainMultitenancyHeaderName() {
        return subdomainMultitenancyHeaderName;
    }

    /**
     * @return the subdomain pattern
     */
    public String getSubdomainMultitenancyPattern() {
        return subdomainMultitenancyPattern;
    }

    /**
     * @return the HTTP request header containing a tenant ID
     */
    public String getTenantIdRequestHeader() {
        return tenantIdRequestHeader;
    }

    public Duration getReaperPeriod() {
        return Duration.ofSeconds(reaperPeriodSeconds);
    }

    /**
     * @return the tenantManagerUrl
     */
    public Optional<String> getTenantManagerUrl() {
        return tenantManagerUrl;
    }

    /**
     * @return the tenantManagerCAFilePath
     */
    public Optional<String> getTenantManagerCAFilePath() {
        return tenantManagerCAFilePath;
    }

    /**
     * @return true if tenant management authentication is enabled
     */
    public boolean isTenantManagerAuthEnabled() {
        return tenantManagerAuthEnabled.orElse(Boolean.FALSE);
    }

    /**
     * @return the tenant manager authentication server url
     */
    public Optional<String> getTenantManagerAuthUrl() {
        return tenantManagerAuthUrl;
    }

    /**
     * @return the tenant manager auth client id
     */
    public Optional<String> getTenantManagerClientId() {
        return tenantManagerClientId;
    }

    /**
     * @return the tenant manager auth client secret
     */
    public Optional<String> getTenantManagerClientSecret() {
        return tenantManagerClientSecret;
    }

    public Optional<Long> getTenantManagerAuthTokenExpirationReductionMs() {
        return tenantManagerAuthTokenExpirationReductionMs;
    }
}