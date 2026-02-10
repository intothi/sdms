package com.panikradius.sdms.db;

public class TableConnectionInfo {

    public String databaseName;
    public String tableName;
    public String user;
    public String pw;
    public String dbConnectionURL;

    public TableConnectionInfo(String databaseName, String tableName, String user, String pw){
        this.databaseName = databaseName;
        this.tableName = tableName;
        dbConnectionURL = "jdbc:mariadb://localhost/" + databaseName;
        this.user = user;
        this.pw = pw;
    }
}
