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

package io.apicurio.common.apps.storage.sql;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;

import io.apicurio.common.apps.config.DynamicConfigPropertyDto;
import io.apicurio.common.apps.config.DynamicConfigStorage;
import io.apicurio.common.apps.mt.TenantContext;
import io.apicurio.common.apps.storage.exceptions.NotFoundException;
import io.apicurio.common.apps.storage.sql.jdbi.Handle;
import io.apicurio.common.apps.storage.sql.jdbi.HandleFactory;
import io.apicurio.common.apps.storage.sql.jdbi.mappers.DynamicConfigPropertyDtoMapper;

/**
 * @author eric.wittmann@gmail.com
 */
public abstract class AbstractSqlStorage<S extends CommonSqlStatements> implements DynamicConfigStorage {

    @Inject
    protected Logger log;

    @Inject
    protected HandleFactory handles;

    @Inject
    protected S sqlStatements;

    @Inject
    protected TenantContext tenantContext;

    @ConfigProperty(name = "app.sql.init", defaultValue = "true")
    boolean initDB;

    @ConfigProperty(name = "quarkus.datasource.jdbc.url")
    String jdbcUrl;

    /**
     * @return the expected version of the DB
     */
    protected abstract int dbVersion();

    /**
     * Does any initialization logic for the SQL storage.  This might try to upgrade the DB or simply
     * verify that the DB is correct.
     */
    protected void doInitialize() {
        log.info("    JDBC URL: " + jdbcUrl);
        handles.withHandle((handle) -> {
            if (initDB) {
                if (!isDatabaseInitialized(handle)) {
                    log.info("Database not initialized.");
                    initializeDatabase(handle);
                } else {
                    log.info("Database was already initialized, skipping.");
                }

                if (!isDatabaseCurrent(handle)) {
                    log.info("Old database version detected, upgrading.");
                    upgradeDatabase(handle);
                }
            } else {
                if (!isDatabaseInitialized(handle)) {
                    log.error("Database not initialized.  Please use the DDL scripts to initialize the database before starting the application.");
                    throw new RuntimeException("Database not initialized.");
                }

                if (!isDatabaseCurrent(handle)) {
                    log.error("Detected an old version of the database.  Please use the DDL upgrade scripts to bring your database up to date.");
                    throw new RuntimeException("Database not upgraded.");
                }
            }
            return null;
        });
    }

    /**
     * @return true if the database has already been initialized
     */
    private boolean isDatabaseInitialized(Handle handle) {
        log.info("Checking to see if the DB is initialized.");
        int count = handle.createQuery(this.sqlStatements.isDatabaseInitialized()).mapTo(Integer.class).one();
        return count > 0;
    }

    /**
     * @return true if the database has already been initialized
     */
    private boolean isDatabaseCurrent(Handle handle) {
        log.info("Checking to see if the DB is up-to-date.");
        log.info("Build's DB version is {}", dbVersion());
        int version = this.getDatabaseVersion(handle);
        return version == dbVersion();
    }

    private void initializeDatabase(Handle handle) {
        log.info("Initializing the database.");
        log.info("\tDatabase type: " + this.sqlStatements.dbType());

        final List<String> statements = this.sqlStatements.databaseInitialization();
        log.debug("---");

        statements.forEach( statement -> {
            log.debug(statement);
            handle.createUpdate(statement).execute();
        });
        log.debug("---");
    }

    /**
     * Upgrades the database by executing a number of DDL statements found in DB-specific
     * DDL upgrade scripts.
     */
    private void upgradeDatabase(Handle handle) {
        log.info("Upgrading the database.");

        int fromVersion = this.getDatabaseVersion(handle);
        int toVersion = dbVersion();

        log.info("\tDatabase type: {}", this.sqlStatements.dbType());
        log.info("\tFrom Version:  {}", fromVersion);
        log.info("\tTo Version:    {}", toVersion);

        final List<String> statements = this.sqlStatements.databaseUpgrade(fromVersion, toVersion);
        log.debug("---");
        statements.forEach( statement -> {
            log.debug(statement);

            if (statement.startsWith("UPGRADER:")) {
                String cname = statement.substring(9).trim();
                applyUpgrader(handle, cname);
            } else {
                handle.createUpdate(statement).execute();
            }
        });
        log.debug("---");
    }

    /**
     * Instantiates an instance of the given upgrader class and then invokes it.  Used to perform
     * advanced upgrade logic when upgrading the DB (logic that cannot be handled in simple SQL
     * statements).
     * @param handle
     * @param cname
     */
    private void applyUpgrader(Handle handle, String cname) {
        try {
            @SuppressWarnings("unchecked")
            Class<IDbUpgrader> upgraderClass = (Class<IDbUpgrader>) Class.forName(cname);
            IDbUpgrader upgrader = upgraderClass.getConstructor().newInstance();
            upgrader.upgrade(handle);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reuturns the current DB version by selecting the value in the 'apicurio' table.
     */
    private int getDatabaseVersion(Handle handle) {
        try {
            int version = handle.createQuery(this.sqlStatements.getDatabaseVersion())
                    .bind(0, "db_version")
                    .mapTo(Integer.class)
                    .one();
            return version;
        } catch (Exception e) {
            log.error("Error getting DB version.", e);
            return 0;
        }
    }

    /**
     * @see io.apicurio.common.apps.config.DynamicConfigStorage#getConfigProperties()
     */
    @Override
    public List<DynamicConfigPropertyDto> getConfigProperties() {
        log.debug("Getting all config properties.");
        return handles.withHandle( handle -> {
            String sql = sqlStatements.selectConfigProperties();
            return handle.createQuery(sql)
                    .bind(0, tenantContext.getTenantId())
                    .map(DynamicConfigPropertyDtoMapper.instance)
                    .list()
                    .stream()
                    // Filter out possible null values.
                    .filter(item -> item != null)
                    .collect(Collectors.toList());
        });
    }

    /**
     * @see io.apicurio.common.apps.config.DynamicConfigStorage#getConfigProperty(java.lang.String)
     */
    @Override
    public DynamicConfigPropertyDto getConfigProperty(String propertyName) {
        log.debug("Selecting a single config property: {}", propertyName);
        return handles.withHandle( handle -> {
            String sql = sqlStatements.selectConfigPropertyByName();
            Optional<DynamicConfigPropertyDto> res = handle.createQuery(sql)
                    .bind(0, tenantContext.getTenantId())
                    .bind(1, propertyName)
                    .map(DynamicConfigPropertyDtoMapper.instance)
                    .findOne();
            return res.orElseThrow(() -> new NotFoundException("Dynamic configuration property not found: " + propertyName));
        });
    }

    /**
     * @see io.apicurio.common.apps.config.DynamicConfigStorage#setConfigProperty(io.apicurio.common.apps.config.DynamicConfigPropertyDto)
     */
    @Override
    @Transactional
    public void setConfigProperty(DynamicConfigPropertyDto property) {
        log.debug("Setting a config property with name: {}  and value: {}", property.getName(), property.getValue());
        handles.withHandle( handle -> {
            String propertyName = property.getName();
            String propertyValue = property.getValue();

            // First delete the property row from the table
            String sql = sqlStatements.deleteConfigProperty();
            handle.createUpdate(sql)
                  .bind(0, tenantContext.getTenantId())
                  .bind(1, property.getName())
                  .execute();

            // Then create the row again with the new value
            sql = sqlStatements.insertConfigProperty();
            handle.createUpdate(sql)
                  .bind(0, tenantContext.getTenantId())
                  .bind(1, propertyName)
                  .bind(2, propertyValue)
                  .bind(3, java.lang.System.currentTimeMillis())
                  .execute();

            return null;
        });
    }

    /**
     * @see io.apicurio.common.apps.config.DynamicConfigStorage#deleteConfigProperty(java.lang.String)
     */
    @Override
    @Transactional
    public void deleteConfigProperty(String propertyName) {
        log.debug("Deleting a config property from storage: {}", propertyName);
        handles.withHandle(handle -> {
            String sql = sqlStatements.deleteConfigProperty();
            int rows = handle.createUpdate(sql)
                    .bind(0, tenantContext.getTenantId())
                    .bind(1, propertyName)
                    .execute();
            if (rows == 0) {
                throw new NotFoundException("Property value not currently set: " + propertyName);
            }
            return null;
        });
    }

    protected List<String> getTenantsWithStaleConfigProperties(Instant since) {
        log.debug("Getting all tenant IDs with stale config properties.");
        return handles.withHandle( handle -> {
            String sql = sqlStatements.selectTenantIdsByConfigModifiedOn();
            return handle.createQuery(sql)
                    .bind(0, since.toEpochMilli())
                    .mapTo(String.class)
                    .list();
        });
    }

}
