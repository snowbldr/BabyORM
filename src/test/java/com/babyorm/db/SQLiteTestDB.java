package com.babyorm.db;

import java.util.Arrays;
import java.util.List;

public class SQLiteTestDB extends TestDB{

    public SQLiteTestDB() {
        super("org.sqlite.JDBC", "jdbc:sqlite:sqliteBabyOrm", null);
    }

    @Override
    protected List<String> initSql() {
        return Arrays.asList(
                "drop table if exists baby",
                "drop table if exists no_autogen",
                "drop table if exists parent",
                "create table baby (pk INTEGER PRIMARY KEY, name text, hair_color text, numberOfToes INTEGER, parent text)",
                "create table no_autogen (pk text, name text )",
                "create table parent (pk text, name text )"
        );
    }
}
