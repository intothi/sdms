package com.panikradius.sdms.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panikradius.sdms.models.Log;
import com.panikradius.sdms.models.Tag;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

public class TableTag {

    public static TableConnectionInfo tableConnectionInfo = new TableConnectionInfo(
            DbConnection.DATABASE_NAME,
            "tag",
            DbConnection.USER,
            DbConnection.PW
    );

    public static String QUERY_CREATE =  "CREATE TABLE " + tableConnectionInfo.tableName + " ("
            + "id INT UNSIGNED NOT NULL AUTO_INCREMENT, "
            + "name VARCHAR (32) NOT NULL, "
            + "color CHAR (7) NOT NULL, "
            + "dateTimeCreated DATETIME, "
            + "PRIMARY KEY (id)"
            + ")";

    public static void buildTable() {
        DbHelper.buildTable(tableConnectionInfo, QUERY_CREATE);
    }

    public static void postPreparedStatement(Tag tag) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

            String query = "INSERT INTO " + tableConnectionInfo.tableName +
                    " (name, color, dateTimeCreated) " +
                    " VALUES (?,?,?)";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, tag.name);
            preparedStatement.setString(2, tag.color);
            preparedStatement.setTimestamp(3, tag.dateTimeCreated);
            preparedStatement.executeUpdate();

        } catch (Exception e) {
        } finally {
            try { preparedStatement.close(); } catch (Exception e) { /* Ignored */ }
            try { connection.close(); } catch (Exception e) { /* Ignored */ }
        }
    }

    public static String get(int id) throws JsonProcessingException {
        return DbHelper.getById(tableConnectionInfo, id);
    }

    public static void deleteById(int id) {
        DbHelper.deleteById(tableConnectionInfo, id);
    }

    public static void EditById(int id, com.panikradius.sdms.models.Tag tag) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL, tableConnectionInfo.user, tableConnectionInfo.pw);

            String query = "UPDATE " + tableConnectionInfo.tableName +
                    " SET name = ?, color = ? WHERE id = ?";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, tag.name);
            preparedStatement.setString(2, tag.color);
            preparedStatement.setInt(3, id);
            preparedStatement.executeUpdate();


        } catch (Exception e) {
        } finally {
            try { preparedStatement.close(); } catch (Exception e) { /* Ignored */ }
            try { connection.close(); } catch (Exception e) { /* Ignored */ }
        }
    }

    public static String getTable(int skip, int top)
            throws JsonProcessingException, SQLException {

        String orderBy = "ORDER BY name DESC";

        com.panikradius.sdms.ResultTableData resultTableData =
                DbHelper.getTableResultSet(tableConnectionInfo, skip, top, orderBy);

        if (resultTableData == null) {return "";}
        ResultSet resultSet = resultTableData.resultSet;
        if (resultSet == null) { return ""; }

        ArrayList<Tag> fetchResult = new ArrayList<Tag>();
        while (resultSet.next()) {
            fetchResult.add(
                    new Tag(
                            Integer.parseInt(resultSet.getString(1)),
                            resultSet.getString(2),
                            resultSet.getString(3),
                            Timestamp.valueOf(resultSet.getString(4))
            ));
        }

        String items = new ObjectMapper().writer().withDefaultPrettyPrinter().
                writeValueAsString(fetchResult);

        String result = "{ " +
                "\"items\": " + items + "," +
                "\"totalCount\": " + resultTableData.count +
                "}";
        return result;
    }
}

