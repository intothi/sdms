package com.panikradius.sdms.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panikradius.sdms.Log;
import com.panikradius.sdms.LogLevel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

public class TableLog {

    public static TableConnectionInfo tableConnectionInfo = new TableConnectionInfo(
            DbConnection.DATABASE_NAME,
            "log",
            DbConnection.USER,
            DbConnection.PW
    );

    public static String QUERY_CREATE =  "CREATE TABLE " + tableConnectionInfo.tableName + " ("
            + "logID INT UNSIGNED NOT NULL AUTO_INCREMENT, "
            + "msg VARCHAR (4095) NOT NULL, "
            + "logLevel CHAR (7) NOT NULL, "
            + "logDate DATETIME, "
            + "PRIMARY KEY (logID)"
            + ")";

    public static void postPreparedStatement(Log log) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DriverManager.getConnection(tableConnectionInfo.dbConnectionURL, tableConnectionInfo.user, tableConnectionInfo.pw);

            String query = "INSERT INTO " + tableConnectionInfo.tableName +
                    " (logID, msg, logLevel, logDate) " +
                    " VALUES (?,?,?,?)";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, 0);
            preparedStatement.setString(2, log.msg);
            preparedStatement.setString(3, log.level.toString());
            preparedStatement.setTimestamp(4, log.logDate);
            preparedStatement.executeUpdate();

        } catch (Exception e) {
        } finally {
            try { preparedStatement.close(); } catch (Exception e) { /* Ignored */ }
            try { connection.close(); } catch (Exception e) { /* Ignored */ }
        }
    }

    public static void buildTable() {
        DbHelper.buildTable(tableConnectionInfo, QUERY_CREATE);
    }

    public static String get(int id) throws JsonProcessingException {
        return DbHelper.getById(tableConnectionInfo, id);
    }

    public static void deleteById(int id) {
        DbHelper.deleteById(tableConnectionInfo, id);
    }

    public static String getTable(int skip, int top) throws JsonProcessingException, SQLException {

        String orderBy = "ORDER BY logDate DESC";

        com.panikradius.sdms.ResultTableData resultTableData = DbHelper.getTableResultSet(tableConnectionInfo, skip, top, orderBy);
        if (resultTableData == null) {return "";}
        ResultSet resultSet = resultTableData.resultSet;
        if (resultSet == null) { return ""; }

        ArrayList<Log> fetchResult = new ArrayList<Log>();
        while (resultSet.next()) {
            fetchResult.add(new Log(
                    Integer.parseInt(resultSet.getString(1)),
                    resultSet.getString(2),
                    LogLevel.valueOf(resultSet.getString(3)),
                    Timestamp.valueOf(resultSet.getString(4))
            ));
        }

        String items = new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(fetchResult);

        String result = "{ " +
                "\"items\": " + items + "," +
                "\"totalCount\": " + resultTableData.count +
                "}";
        return result;
    }


}
