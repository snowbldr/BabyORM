package com.babyorm.db;

import com.wix.mysql.EmbeddedMysql;

import java.util.Arrays;
import java.util.List;

import static com.wix.mysql.EmbeddedMysql.anEmbeddedMysql;
import static com.wix.mysql.distribution.Version.v5_6_latest;

public class MysqlTestDB extends TestDB {
    private static EmbeddedMysql mysql;

    public MysqlTestDB() {
        super("com.mysql.cj.jdbc.Driver", initMysql());
    }

    private static String initMysql(){
        try {
            mysql = anEmbeddedMysql(v5_6_latest)
                    .addSchema("babyOrm")
                    .start();
            return "jdbc:mysql://"+mysql.getConfig().getUsername()+":"+mysql.getConfig().getPassword()+"@localhost:" + mysql.getConfig().getPort() + "/babyOrm?createDatabaseIfNotExist=true";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void tearDown() {
        mysql.stop();
    }

    @Override
    protected List<String> initSql() {
        return Arrays.asList(
                "drop table if exists baby",
                "drop table if exists no_autogen",
                "drop table if exists parent",
                "create table baby (pk int auto_increment primary key, name text, hair_color text, numberOfToes INT, parent text)",
                "create table parent (pk text, name text)",
                "create table no_autogen (pk text, name text )");
    }
}
