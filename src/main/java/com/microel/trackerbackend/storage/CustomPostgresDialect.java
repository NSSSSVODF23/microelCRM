package com.microel.trackerbackend.storage;

import org.hibernate.dialect.PostgreSQL10Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.stereotype.Service;

@Service
public class CustomPostgresDialect extends PostgreSQLDialect {
    public CustomPostgresDialect() {
        super();
        registerFunction("fts", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "to_tsvector('russian', ?1) @@ websearch_to_tsquery('russian', ?2)"));
    }
}
