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

package io.apicurio.common.apps.logging.sentry;

import java.util.logging.LogManager;

import javax.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.StartupEvent;
import io.sentry.Sentry;
import io.sentry.jul.SentryHandler;

/**
 * @author Fabian Martinez
 */
public abstract class AbstractSentryConfiguration {

    Logger log = LoggerFactory.getLogger(this.getClass());

    @ConfigProperty(name = "app.enable.sentry", defaultValue = "false")
    Boolean enableSentry;

    public void onStart(@Observes StartupEvent ev) throws Exception {
        if (enableSentry) {
            java.lang.System.setProperty("sentry.release", getReleaseVersion());
            //Sentry will pick its configuration from env variables
            Sentry.init();
            LogManager manager = org.jboss.logmanager.LogManager.getLogManager();
            manager.getLogger("").addHandler(new SentryHandler());
            log.info("Sentry initialized");
        }
    }

    protected abstract String getReleaseVersion();

}
