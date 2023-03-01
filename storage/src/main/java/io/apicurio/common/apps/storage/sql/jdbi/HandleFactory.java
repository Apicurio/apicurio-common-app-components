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

package io.apicurio.common.apps.storage.sql.jdbi;

import java.sql.Connection;
import java.sql.SQLException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.agroal.api.AgroalDataSource;
import io.apicurio.common.apps.core.AppException;
import io.apicurio.common.apps.storage.exceptions.AlreadyExistsException;
import io.apicurio.common.apps.storage.exceptions.StorageException;
import io.apicurio.common.apps.storage.sql.CommonSqlStatements;

/**
 * @author eric.wittmann@gmail.com
 */
@ApplicationScoped
public class HandleFactory {

    @Inject
    AgroalDataSource dataSource;

    @Inject
    CommonSqlStatements sqlStatements;

    private <R, X extends Exception> R _withHandle(HandleCallback<R, X> callback) throws X, SQLException {
        try (Connection connection = dataSource.getConnection()) {
            Handle handleImpl = new HandleImpl(connection);
            return callback.withHandle(handleImpl);
        }
    }

    public <R, X extends Exception> R withHandle(HandleCallback<R, X> callback) throws X, AppException, StorageException, AlreadyExistsException {
        try {
            return _withHandle(callback);
        } catch (SQLException e) {
            if (sqlStatements.isPrimaryKeyViolation(e) || sqlStatements.isForeignKeyViolation(e)) {
                throw new AlreadyExistsException(e);
            } else {
                throw new StorageException(e);
            }
        } catch (StorageException e) {
            if (sqlStatements.isPrimaryKeyViolation(e) || sqlStatements.isForeignKeyViolation(e)) {
                throw new AlreadyExistsException(e);
            } else {
                throw e;
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

}
