package com.babyorm;

import java.sql.Connection;

@FunctionalInterface
public interface ConnectionSupplier {
    Connection getConnection();
}
