package com.panikradius.sdms.db;

public class TableDocument {

    public static final TableConnectionInfo tableConnectionInfo = new TableConnectionInfo(
            DbConnection.DATABASE_NAME,
            "document",
            DbConnection.USER,
            DbConnection.PW
    );

    private static final String QUERY_CREATE =  "CREATE TABLE " + tableConnectionInfo.tableName + " ("
            + "id INT NOT NULL AUTO_INCREMENT, "
            + "name VARCHAR (255) NOT NULL, "
            + "comment VARCHAR (4095) NOT NULL, "
            + "dateDocument DATE, "
            + "dateArchived DATE, "
            + "PRIMARY KEY (id)"
            + ")";

    public static void buildTable() {
        DbHelper.buildTable(tableConnectionInfo, QUERY_CREATE);
    }
}
