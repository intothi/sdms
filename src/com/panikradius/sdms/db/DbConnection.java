package com.panikradius.sdms.db;

public class DbConnection {

    public static final String JDBC_DRIVER = "org.mariadb.jdbc.Driver";

    public static final String DATABASE_NAME = com.panikradius.sdms.App.projectName + "Db";
    public static final String DB_URL = "jdbc:mariadb://localhost/" + DATABASE_NAME;
    public static final String USER = com.panikradius.sdms.App.projectName + "User";
    public static final String PW = "12345";

    // CREATE DATABASE dmsDb;
    // CREATE USER dmsUser IDENTIFIED BY '12345';
    // GRANT ALL PRIVILEGES ON dmsDb.* TO 'dmsUser'@'localhost' identified by '12345';
    // FLUSH PRIVILEGES;

}