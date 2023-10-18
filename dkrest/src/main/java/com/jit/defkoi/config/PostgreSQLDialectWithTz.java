package com.jit.defkoi.config;

import org.hibernate.dialect.PostgreSQL10Dialect;

import java.sql.Types;

public class PostgreSQLDialectWithTz extends PostgreSQL10Dialect {
  public PostgreSQLDialectWithTz() {
    super();
    registerColumnType(Types.TIMESTAMP, "timestamp with time zone");
  }
}

