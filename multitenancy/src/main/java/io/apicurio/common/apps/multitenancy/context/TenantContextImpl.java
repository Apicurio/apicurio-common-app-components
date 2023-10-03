/*
 * Copyright 2020 Red Hat
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

package io.apicurio.common.apps.multitenancy.context;

import io.apicurio.common.apps.multitenancy.ApicurioTenantContext;
import io.apicurio.common.apps.multitenancy.MultitenancyProperties;
import io.apicurio.common.apps.multitenancy.TenantContext;
import io.apicurio.common.apps.multitenancy.TenantIdResolver;
import io.apicurio.common.apps.multitenancy.limits.TenantLimitsConfiguration;
import io.apicurio.tenantmanager.api.datamodel.TenantStatusValue;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.http.HttpServerRequest;
import org.slf4j.MDC;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Optional;

/**
 * @author eric.wittmann@gmail.com
 */
@RequestScoped
public class TenantContextImpl implements TenantContext {

    private static final String TENANT_ID_KEY = "tenantId";
    private Optional<ApicurioTenantContext> current = Optional.empty();

    private static final ApicurioTenantContext EMPTY_CONTEXT = new ApicurioTenantContextImpl(DEFAULT_TENANT_ID, null, null, TenantStatusValue.READY, null);
    private static final ThreadLocal<ApicurioTenantContext> CURRENT = ThreadLocal.withInitial(() -> EMPTY_CONTEXT);

    @Inject
    TenantContextLoaderImpl contextLoader;

    @Inject
    TenantIdResolver tenantIdResolver;

    @Inject
    CurrentVertxRequest request;

    @Inject
    MultitenancyProperties multitenancyProperties;

    @PostConstruct
    public void load() {
        ApicurioTenantContext loadedContext;
        if (multitenancyProperties.isMultitenancyEnabled() && request.getCurrent() != null) {
            HttpServerRequest req = request.getCurrent().request();
            String requestURI = req.uri();

            Optional<String> tenantIdOpt = tenantIdResolver.resolveTenantId(
                    // Request URI
                    requestURI,
                    // Function to get an HTTP request header value
                    req::getHeader,
                    // Function to get the serverName from the HTTP request
                    req::host, null);

            loadedContext = tenantIdOpt.map(tenantId -> contextLoader.loadRequestContext(tenantId))
                    .orElse(contextLoader.defaultTenantContext());

        } else {
            loadedContext = contextLoader.defaultTenantContext();
        }

        setContext(loadedContext);
    }

    /**
     * @see io.apicurio.common.apps.multitenancy.TenantContext#tenantId()
     */
    @Override
    public String tenantId() {
        return current.map(ApicurioTenantContext::getTenantId)
                .orElse(CURRENT.get().getTenantId());
    }

    /**
     * @see TenantContext#currentContext()
     */
    @Override
    public ApicurioTenantContext currentContext() {
        return current.orElse(CURRENT.get());
    }

    /**
     * @see  io.apicurio.common.apps.multitenancy.TenantContext#tenantOwner()
     */
    @Override
    public String tenantOwner() {
        return current.map(ApicurioTenantContext::getTenantOwner)
                .orElse(CURRENT.get().getTenantOwner());
    }

    /**
     * @see  io.apicurio.common.apps.multitenancy.TenantContext#limitsConfig()
     */
    @Override
    public TenantLimitsConfiguration limitsConfig() {
        return current.map(ApicurioTenantContext::getLimitsConfiguration)
                .orElse(CURRENT.get().getLimitsConfiguration());
    }

    /**
     * @see  io.apicurio.common.apps.multitenancy.TenantContext#setContext(ApicurioTenantContext)
     */
    @Override
    public void setContext(ApicurioTenantContext ctx) {
        current = Optional.of(ctx);
        MDC.put(TENANT_ID_KEY, ctx.getTenantId());
    }

    /**
     * @see io.apicurio.common.apps.multitenancy.TenantContext#clearContext()
     */
    @Override
    public void clearContext() {
        current = Optional.of(EMPTY_CONTEXT);
        MDC.remove(TENANT_ID_KEY);
    }

    @Override
    public boolean isLoaded() {
        return !tenantId().equals(DEFAULT_TENANT_ID);
    }

    @Override
    public TenantStatusValue getTenantStatus() {
        return current.map(ApicurioTenantContext::getStatus)
                .orElse(CURRENT.get().getStatus());
    }
}
