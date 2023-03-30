package io.apicurio.common.apps.storage.sql.jdbi.mappers;

import io.quarkus.runtime.StartupEvent;

import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

@ApplicationScoped
public class MapperLoader {

    @Inject
    Instance<RowMapper<?>> mappers;

    void init(@Observes StartupEvent ev) {
        MapperLoaderHolder.getInstance().setMapperLoader(this);
    }

    public List<RowMapper<?>> getMappers() {
        return mappers.stream().collect(Collectors.toList());
    }
}
