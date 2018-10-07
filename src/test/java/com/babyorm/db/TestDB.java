package com.babyorm.db;

import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public abstract class TestDB {
    private String connectString;
    private HikariDataSource ds;
    private SessionFactory sessionFactory;

    protected TestDB(String driverClass, String connectString, String hibernateDialect) {
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        this.connectString = connectString;
        ds = new HikariDataSource();
        ds.setJdbcUrl(connectString);
        ds.setMaximumPoolSize(50);

        if(hibernateDialect != null){
            Configuration config = new Configuration();
            config.addAnnotatedClass(Baby.class);
            config.addAnnotatedClass(Parent.class);
            config.setProperty("hibernate.dialect", hibernateDialect);
            config.setProperty("hibernate.connection.driver_class", driverClass);
            config.setProperty("hibernate.connection.url", connectString);
            sessionFactory = config.buildSessionFactory();
        }

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
            return ds.getConnection();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public SessionFactory getSessionFactory(){
        return sessionFactory;
    }
    public void tearDown(){}

    protected abstract List<String> initSql();
}
