package com.babyorm.db;

import java.util.Arrays;
import java.util.List;

public class DerbyTestDB extends TestDB {

    public DerbyTestDB() {
        super("org.apache.derby.jdbc.EmbeddedDriver", "jdbc:derby:memory:babyorm;create=true", null);
    }

    @Override
    protected List<String> initSql() {
        return Arrays.asList(
                "create table baby (pk INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) primary key, name VARCHAR(36), parent varchar(36), hair_color VARCHAR(36), numberOfToes INT )",
                "create table parent (pk varchar(36) primary key, name VARCHAR(36))",
                "create table no_autogen (pk VARCHAR(20), colName VARCHAR(36) )");
    }
}