/*
 * Copyright 2022 Red Hat
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

package apicurio.common.app.components.config.index.it;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.apicurio.common.apps.config.Dynamic;
import io.apicurio.common.apps.config.DynamicConfigPropertyDto;
import io.apicurio.common.apps.config.DynamicConfigPropertyIndex;

@Path("/config")
@ApplicationScoped
public class ConfigIndexResource {
    // add some rest methods here

    @Inject
    DynamicConfigPropertyIndex dynamicPropertyIndex;
    @Inject
    InMemoryDynamicConfigStorage storage;
    @Inject
    Config config;

    @Dynamic
    @ConfigProperty(name = "app.properties.dynamic.string", defaultValue = "_DEFAULT_")
    Supplier<String> dynamicString;

    @Dynamic
    @ConfigProperty(name = "app.properties.dynamic.int", defaultValue = "0")
    Supplier<Integer> dynamicInt;

    @Dynamic
    @ConfigProperty(name = "app.properties.dynamic.long", defaultValue = "17")
    Supplier<Long> dynamicLong;

    @Dynamic
    @ConfigProperty(name = "app.properties.dynamic.bool", defaultValue = "false")
    Supplier<Boolean> dynamicBool;

    @Dynamic(requires = "app.properties.dynamic.bool=true")
    @ConfigProperty(name = "app.properties.dynamic.bool.dep", defaultValue = "false")
    Supplier<Boolean> dynamicBoolDep;

    @ConfigProperty(name = "app.properties.static.string", defaultValue = "default-value")
    String staticString;

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    public ConfigProps getAllProperties() {
        return new ConfigProps(dynamicPropertyIndex.getPropertyNames().stream()
                .map(pname -> new ConfigProp(pname, getPropertyValue(pname)))
                .collect(Collectors.toList()));
    }

    @GET
    @Path("/accepted")
    @Produces(MediaType.APPLICATION_JSON)
    public ConfigProps getAcceptedProperties() {
        return new ConfigProps(dynamicPropertyIndex.getAcceptedPropertyNames().stream()
                .map(pname -> new ConfigProp(pname, getPropertyValue(pname)))
                .collect(Collectors.toList()));
    }

    @Path("/all/{propertyName}")
    @GET
    @Produces("application/json")
    public ConfigProp getConfigProperty(@PathParam("propertyName") String propertyName) {
        String name = propertyName;
        String value = null;
        if ("app.properties.dynamic.string".equals(propertyName)) {
            value = dynamicString.get();
        }
        if ("app.properties.dynamic.int".equals(propertyName)) {
            value = dynamicInt.get().toString();
        }
        if ("app.properties.dynamic.long".equals(propertyName)) {
            value = dynamicLong.get().toString();
        }
        if ("app.properties.dynamic.bool".equals(propertyName)) {
            value = dynamicBool.get().toString();
        }
        if ("app.properties.dynamic.bool.dep".equals(propertyName)) {
            value = dynamicBoolDep.get().toString();
        }
        return new ConfigProp(name, value);
    }

    @Path("/update")
    @GET
    public void updateBooleanProperty() {
        DynamicConfigPropertyDto dto = new DynamicConfigPropertyDto("app.properties.dynamic.bool", "true");
        this.storage.setConfigProperty(dto);
    }

    private String getPropertyValue(String propertyName) {
        Optional<String> optionalValue = config.getOptionalValue(propertyName, String.class);
        return optionalValue.orElseGet(() -> this.dynamicPropertyIndex.getProperty(propertyName).getDefaultValue());
    }

}
