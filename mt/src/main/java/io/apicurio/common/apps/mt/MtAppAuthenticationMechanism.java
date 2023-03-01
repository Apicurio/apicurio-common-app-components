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

import java.util.Optional;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.slf4j.Logger;

import io.apicurio.common.apps.auth.authn.AppAuthenticationMechanism;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

/**
 * A version of the application authentication mechanism that is multi-tenant aware. If the app is configured
 * for multi-tenancy, this auth mechanism will do all the Tenant Context work. This includes: 1) Resolving the
 * tenantId 2) Loading the tenant info 3) Authorizing the user to access the tenant 4) Configuring the
 * TenantContext
 *
 * @author eric.wittmann@gmail.com
 */
public abstract class MtAppAuthenticationMechanism extends AppAuthenticationMechanism {

    @Inject
    Logger log;

    @Inject
    MtProperties mtProperties;

    @Inject
    TenantIdResolver tenantIdResolver;

    @Inject
    TenantInfoLoader tenantInfoLoader;

    @Inject
    TenantAuthorizer tenantAuthorizer;

    @Inject
    TenantContext tenantContext;

    /**
     * @see io.apicurio.common.apps.auth.authn.AppAuthenticationMechanism#authenticate(io.vertx.ext.web.RoutingContext,
     *      io.quarkus.security.identity.IdentityProviderManager)
     */
    @Override
    @ActivateRequestContext
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        Uni<SecurityIdentity> identity = super.authenticate(context, identityProviderManager);

        if (mtProperties.isMultitenancyEnabled()) {
            log.info("Multitenancy is enabled!");
            HttpServerRequest request = context.request();
            Optional<String> tenantId = tenantIdResolver.resolveTenantId(request.absoluteURI(),
                    (headerName) -> request.getHeader(headerName),
                    () -> request.host());
            if (tenantId.isEmpty()) {
                log.warn("Multi-tenancy is enabled but the tenantId could not be resolved.");
                throw new ForbiddenException("Could not resolve tenant ID.");
            }
            Optional<TenantInfo> info = tenantInfoLoader.loadInfo(tenantId.get());
            if (info.isEmpty()) {
                throw new ForbiddenException("Unknown tenant: " + tenantId.get());
            }
            if (mtProperties.isMultitenancyAuthorizationEnabled()) {
                tenantAuthorizer.authorizeTenant(info.get(), identity);
            }
            tenantContext.setTenantInfo(info.get());
        }

        return identity;
    }

}
