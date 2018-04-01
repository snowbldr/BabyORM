package com.babyorm.db;

import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres;

import java.util.Arrays;
import java.util.List;

import static ru.yandex.qatools.embed.postgresql.distribution.Version.Main.V9_6;

public class PostgresTestDB extends TestDB {
    private static EmbeddedPostgres postgres;

    public PostgresTestDB() {
        super("org.postgresql.Driver", initPostgres());
    }

    private static String initPostgres(){
        try {
            postgres = new EmbeddedPostgres(V9_6);
            return postgres.start("localhost", 5432, "babyOrm", "userName", "password");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void tearDown() {
        postgres.stop();
    }

    @Override
    protected List<String> initSql() {
        return Arrays.asList(
                "drop table if exists baby",
                "drop table if exists no_autogen",
                "create table baby (pk SERIAL, name text, hair_color text, numberOfToes INT )",
                "create table no_autogen (pk text, name text )");
    }
}
