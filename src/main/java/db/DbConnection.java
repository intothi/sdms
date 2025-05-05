package db;

public class DbConnection {

    public static final String JDBC_DRIVER = "org.mariadb.jdbc.Driver";

    public static final String DATABASE_NAME_DMS = "dmsDb";
    public static final String DB_URL_DMS = "jdbc:mariadb://localhost/" + DATABASE_NAME_DMS;
    public static final String USER_DMS = "dmsUser";
    public static final String PW_DMS = "12345";

    // CREATE DATABASE dmsDb;
    // CREATE USER dmsUser IDENTIFIED BY '12345';
    // GRANT ALL PRIVILEGES ON dmsDb.* TO 'dmsUser'@'localhost' identified by '12345';
    // FLUSH PRIVILEGES;

}