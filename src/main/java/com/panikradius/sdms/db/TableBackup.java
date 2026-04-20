package com.panikradius.sdms.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.panikradius.sdms.models.Backup;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;

public class TableBackup {

    public static TableConnectionInfo tableConnectionInfo = new TableConnectionInfo(
            DbConnection.DATABASE_NAME,
            "backup",
            DbConnection.USER,
            DbConnection.PW
    );

    public static String QUERY_CREATE = "CREATE TABLE " + tableConnectionInfo.tableName + " ("
            + "id INT UNSIGNED NOT NULL AUTO_INCREMENT, "
            + "filePath VARCHAR(1023) NOT NULL, "
            + "fileSize BIGINT NOT NULL, "
            + "dateTimeCreated DATETIME NOT NULL, "
            + "timeCreation BIGINT NOT NULL, "
            + "createdBy VARCHAR(16) NOT NULL, "
            + "PRIMARY KEY (id)"
            + ")";

    public static void buildTable() {
        DbHelper.buildTable(tableConnectionInfo, QUERY_CREATE);
    }

    public static void post(Connection connection, Backup backup) throws Exception {
        String query = "INSERT INTO " + tableConnectionInfo.tableName
                + " (filePath, fileSize, dateTimeCreated, timeCreation, createdBy) " +
                "VALUES (?,?,?,?,?)";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, backup.filePath);
        preparedStatement.setLong(2, backup.fileSize);
        preparedStatement.setTimestamp(3, backup.dateTimeCreated);
        preparedStatement.setLong(4, backup.timeCreation);
        preparedStatement.setString(5, backup.createdBy);
        preparedStatement.executeUpdate();
        try { preparedStatement.close(); } catch (Exception e) { /* Ignored */ }
    }

    public static String getTable(int skip, int top) throws Exception {
        String orderBy = "ORDER BY dateTimeCreated DESC";

        com.panikradius.sdms.ResultTableData resultTableData =
                DbHelper.getTableResultSet(tableConnectionInfo, skip, top, orderBy);

        String emptyResult = "{\"items\":[],\"totalCount\":0}";

        if (resultTableData == null) { return emptyResult; }
        ResultSet resultSet = resultTableData.resultSet;
        if (resultSet == null) { return emptyResult; }

        ArrayList<Backup> fetchResult = new ArrayList<Backup>();
        while (resultSet.next()) {
            fetchResult.add(new Backup(
                    resultSet.getInt(1),
                    resultSet.getString(2),
                    resultSet.getLong(3),
                    Timestamp.valueOf(resultSet.getString(4)),
                    resultSet.getLong(5),
                    resultSet.getString(6)
            ));
        }

        String items = new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(fetchResult);
        return "{ \"items\": " + items + ", \"totalCount\": " + resultTableData.count + "}";
    }
}