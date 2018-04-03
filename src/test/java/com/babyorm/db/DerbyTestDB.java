package com.babyorm.db;

import java.util.Arrays;
import java.util.List;

public class DerbyTestDB extends TestDB {

    public DerbyTestDB() {
        super("org.apache.derby.jdbc.EmbeddedDriver", "jdbc:derby:memory:babyorm;create=true");
    }

    @Override
    protected List<String> initSql() {
        return Arrays.asList(
                "create table baby (pk INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), name VARCHAR(20), parent_pk int, hair_color VARCHAR(20), numberOfToes INT )",
                "create table parent (pk INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), name VARCHAR(20))",
                "create table no_autogen (pk VARCHAR(20), name VARCHAR(20) )");
    }
}