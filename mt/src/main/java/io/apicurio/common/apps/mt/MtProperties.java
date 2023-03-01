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

package io.apicurio.common.apps.mt;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Optional;

/**
 * @author Fabian Martinez
 */
@ApplicationScoped
public class MtProperties {

    @Inject
    @ConfigProperty(name = "app.multitenancy.enabled", defaultValue = "false")
    boolean multitenancyEnabled;

    @Inject
    @ConfigProperty(name = "app.multitenancy.authorization.enabled", defaultValue = "true")
    boolean mtAuthorizationEnabled;

    @Inject
    @ConfigProperty(name = "app.multitenancy.types.context-path.enabled", defaultValue = "true")
    boolean mtContextPathEnabled;

    @Inject
    @ConfigProperty(name = "app.multitenancy.types.subdomain.enabled", defaultValue = "false")
    boolean mtSubdomainEnabled;

    @Inject
    @ConfigProperty(name = "app.multitenancy.types.request-header.enabled", defaultValue = "true")
    boolean mtRequestHeaderEnabled;

    @Inject
    @ConfigProperty(name = "app.multitenancy.types.context-path.base-path", defaultValue = "t")
    String nameMultitenancyBasePath;

    @Inject
    @ConfigProperty(name = "app.multitenancy.types.subdomain.location", defaultValue = "header")
    String subdomainMultitenancyLocation;

    @Inject
    @ConfigProperty(name = "app.multitenancy.types.subdomain.header-name", defaultValue = "Host")
    String subdomainMultitenancyHeaderName;

    @Inject
    @ConfigProperty(name = "app.multitenancy.types.subdomain.pattern", defaultValue = "(\\w[\\w\\d\\-]*)\\.localhost\\.local")
    String subdomainMultitenancyPattern;

    @Inject
    @ConfigProperty(name = "app.multitenancy.types.request-header.name", defaultValue = "X-Tenant-Id")
    String tenantIdRequestHeader;

    @Inject
    @ConfigProperty(name = "app.multitenancy.reaper.every")
    Optional<String> reaperEvery;

    @Inject
    @ConfigProperty(name = "app.multitenancy.reaper.period-seconds", defaultValue = "10800")
    Long reaperPeriodSeconds;

    @Inject
    @ConfigProperty(name = "app.tenant.manager.url")
    Optional<String> tenantManagerUrl;

    @Inject
    @ConfigProperty(name = "app.tenant.manager.auth.enabled")
    Optional<Boolean> tenantManagerAuthEnabled;

    @Inject
    @ConfigProperty(name = "app.tenant.manager.auth.url.configured")
    Optional<String> tenantManagerAuthUrl;

    @Inject
    @ConfigProperty(name = "app.tenant.manager.auth.client-id")
    Optional<String> tenantManagerClientId;

    @Inject
    @ConfigProperty(name = "app.tenant.manager.auth.client-secret")
    Optional<String> tenantManagerClientSecret;

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
}
