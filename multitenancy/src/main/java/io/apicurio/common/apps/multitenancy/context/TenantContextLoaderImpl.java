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

package io.apicurio.common.apps.multitenancy.context;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import io.apicurio.common.apps.config.Info;
import io.apicurio.common.apps.multitenancy.*;
import io.apicurio.common.apps.multitenancy.exceptions.TenantNotFoundException;
import io.apicurio.common.apps.multitenancy.limits.TenantLimitsConfiguration;
import io.apicurio.common.apps.multitenancy.limits.TenantLimitsConfigurationService;
import io.apicurio.tenantmanager.api.datamodel.ApicurioTenant;
import io.apicurio.tenantmanager.api.datamodel.TenantStatusValue;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.common.constraint.NotNull;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Component responsible for creating instances of {@link ApicurioTenantContext} so they can be set with {@link TenantContext}
 *
 * @author Carles Arnal
 */
@ApplicationScoped
public class TenantContextLoaderImpl implements TenantContextLoader {

    //NOTE for now we are just storing per tenant configurations in the context, this allows us to cache the instances
    //but if in the future we store session scoped or request scoped information the caching strategy should change
    private LoadingCache<String, ApicurioTenantContext> contextsCache;
    private ApicurioTenantContext defaultTenantContext;

    @Inject
    TenantManagerService tenantMetadataService;

    @Inject
    TenantLimitsConfigurationService limitsConfigurationService;

    @Inject
    Logger log;

    @Inject
    @ConfigProperty(defaultValue = "60000", name = "app.tenants.context.cache.check-period")
    @Info(category = "mt", description = "Tenants context cache check period", availableSince = "2.1.0.Final")
    Long cacheCheckPeriod;

    @Inject
    @ConfigProperty(defaultValue = "1000", name = "app.tenants.context.cache.max-size")
    @Info(category = "mt", description = "Tenants context cache max size", availableSince = "2.4.1.Final")
    Long cacheMaxSize;

    @Inject
    MultitenancyProperties properties;

    public void onStart(@Observes StartupEvent ev) {
        contextsCache = CacheBuilder
                .newBuilder()
                .expireAfterWrite(cacheCheckPeriod, TimeUnit.MILLISECONDS)
                .maximumSize(cacheMaxSize)
                .build(getCacheLoader());
    }

    private CacheLoader<String, ApicurioTenantContext> getCacheLoader() {

        CacheLoader<String, ApicurioTenantContext> standaloneLoader = new CacheLoader<>() {
            @Override
            public ApicurioTenantContext load(@NotNull String tenantId) {
                ApicurioTenant tenantMetadata = new ApicurioTenant();
                tenantMetadata.setStatus(TenantStatusValue.READY);
                tenantMetadata.setTenantId(tenantId);
                tenantMetadata.setOrganizationId(tenantId);
                tenantMetadata.setCreatedBy(tenantId);
                tenantMetadata.setName(tenantId);

                TenantLimitsConfiguration limitsConfiguration = limitsConfigurationService.fromTenantMetadata(tenantMetadata);
                return new ApicurioTenantContextImpl(tenantId, tenantMetadata.getCreatedBy(), limitsConfiguration, tenantMetadata.getStatus(), String.valueOf(tenantMetadata.getOrganizationId()));
            }
        };


        CacheLoader<String, ApicurioTenantContext> remoteLoader = new CacheLoader<>() {
            @Override
            public ApicurioTenantContext load(@NotNull String tenantId) {
                ApicurioTenant tenantMetadata = tenantMetadataService.getTenant(tenantId);
                TenantLimitsConfiguration limitsConfiguration = limitsConfigurationService.fromTenantMetadata(tenantMetadata);
                return new ApicurioTenantContextImpl(tenantId, tenantMetadata.getCreatedBy(), limitsConfiguration, tenantMetadata.getStatus(), String.valueOf(tenantMetadata.getOrganizationId()));
            }
        };


        if (properties.isStandaloneMultitenancyEnabled()) {
            return standaloneLoader;
        } else {
            return remoteLoader;
        }
    }

    /**
     * Used for internal stuff where there isn't a JWT token from the user request available
     * This won't perform any authorization check.
     *
     * @param tenantId
     * @return
     */
    @Override
    public ApicurioTenantContext loadBatchJobContext(String tenantId) {
        return loadRequestContext(tenantId);
    }

    /**
     * Loads the tenant context from the cache or computes it
     *
     * @param tenantId
     */
    @Override
    public ApicurioTenantContext loadRequestContext(String tenantId) {
        if (tenantId.equals(TenantContext.DEFAULT_TENANT_ID)) {
            return defaultTenantContext();
        }

        try {
            return contextsCache.get(tenantId);
        } catch (ExecutionException | UncheckedExecutionException e) {
            if (e.getCause() instanceof TenantNotFoundException) {
                throw (TenantNotFoundException) e.getCause();
            } else {
                log.warn("Error trying to load the tenant context for tenant id: {}.", tenantId, e);
                throw new TenantNotFoundException(tenantId);
            }
        }
    }

    @Override
    public ApicurioTenantContext defaultTenantContext() {
        if (defaultTenantContext == null) {
            defaultTenantContext = new ApicurioTenantContextImpl(TenantContext.DEFAULT_TENANT_ID, null, limitsConfigurationService.defaultConfigurationTenant(), TenantStatusValue.READY, null);
        }
        return defaultTenantContext;
    }

    @Override
    public void invalidateTenantInCache(String tenantId) {
        contextsCache.invalidate(tenantId);
    }

    @Override
    public void invalidateTenantCache() {
        contextsCache.invalidateAll();
    }
}
