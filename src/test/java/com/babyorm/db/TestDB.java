package com.babyorm.db;

import com.babyorm.BabyRepo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class TestDB {
    private static boolean initialized = false;
    public static Connection connectionSupplier(){
        try {
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
            return DriverManager.getConnection("jdbc:derby:memory:babyorm;create=true");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized static void init(){
        if(initialized) return;
        initialized = true;

        BabyRepo.setGlobalConnectionSupplier(TestDB::connectionSupplier);

        try(Connection conn = connectionSupplier()){
            Statement st = conn.createStatement();
            st.executeUpdate("create table baby (pk INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), name VARCHAR(20), hair_color VARCHAR(20), numberOfToes INT )");
            st.executeUpdate("create table no_autogen (pk VARCHAR(20), name VARCHAR(20) )");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}