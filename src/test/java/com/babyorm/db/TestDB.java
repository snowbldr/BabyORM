package com.babyorm.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public abstract class TestDB {
    private String connectString;

    protected TestDB(String driverClass, String connectString) {
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        this.connectString = connectString;

        try(Connection conn = connectionSupplier()){
            Statement st = conn.createStatement();
            initSql().forEach(s->{
                try {
                    st.executeUpdate(s);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Connection connectionSupplier(){
        try {
            return DriverManager.getConnection(connectString);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public void tearDown(){}

    protected abstract List<String> initSql();
}
