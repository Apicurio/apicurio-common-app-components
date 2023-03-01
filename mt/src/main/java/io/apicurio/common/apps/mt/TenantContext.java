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

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;

/**
 * @author eric.wittmann@gmail.com
 */
@RequestScoped
public class TenantContext {

    private static final String DEFAULT_TENANT_ID = "_";

    @Inject
    Logger log;

    private TenantInfo info;

    @PostConstruct
    void init() {
    }

    public void setTenantInfo(TenantInfo info) {
        log.debug("Setting info: " + info + " on " + this);
        this.info = info;
    }

    public String getTenantId() {
        if (info == null) {
            return DEFAULT_TENANT_ID;
        }
        return info.getId();
    }

}
