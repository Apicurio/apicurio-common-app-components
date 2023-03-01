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

import jakarta.enterprise.context.ApplicationScoped;

/**
 * @author eric.wittmann@gmail.com
 */
@ApplicationScoped
public class TenantInfoLoader {

    public Optional<TenantInfo> loadInfo(String tenantId) {
        // TODO Load the tenant info from the tenant manager
        TenantInfo info = new TenantInfo();
        info.setId(tenantId);
        info.setOrgId(null);
        info.setOwner(null);
        info.setStatus(TenantStatus.READY);
        return Optional.of(info);
    }

}
