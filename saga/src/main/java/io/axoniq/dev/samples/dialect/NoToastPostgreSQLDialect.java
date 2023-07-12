package io.axoniq.dev.samples.dialect;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.BinaryJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.Types;

/**
 * Hibernate 6 based {@link PostgreSQLDialect}, used to enforce the {@link SqlTypes#BLOB} format to {@code BYTEA}.
 * Without this dialect, PostgreSQL defaults to the so-called <a href="https://wiki.postgresql.org/wiki/TOAST">TOAST</a>
 * behavior, causing LOB formats to move to a dedicated private table and replacing the LOB column for the OID (Object
 * Identifier) column.
 */
@SuppressWarnings("unused") // Used through the application.properties file
public class NoToastPostgreSQLDialect extends PostgreSQLDialect {

    public NoToastPostgreSQLDialect() {
        super();
    }

    @Override
    protected String columnType(int sqlTypeCode) {
        return sqlTypeCode == SqlTypes.BLOB ? "BYTEA" : super.columnType(sqlTypeCode);
    }

    @Override
    protected String castType(int sqlTypeCode) {
        return sqlTypeCode == SqlTypes.BLOB ? "BYTEA" : super.castType(sqlTypeCode);
    }

    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);
        JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
        jdbcTypeRegistry.addDescriptor(Types.BLOB, BinaryJdbcType.INSTANCE);
    }
}
