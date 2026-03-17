package com.panikradius.sdms.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panikradius.sdms.models.Color;
import com.panikradius.sdms.models.Tag;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

public class TableColor {

    public static TableConnectionInfo tableConnectionInfo =
            new TableConnectionInfo(
                    DbConnection.DATABASE_NAME,
                    "color",
                    DbConnection.USER,
                    DbConnection.PW
            );

    public static String QUERY_CREATE =
            "CREATE TABLE " + tableConnectionInfo.tableName + " ("
                    + "id INT UNSIGNED NOT NULL AUTO_INCREMENT, "
                    + "name VARCHAR (32) NOT NULL, "
                    + "color CHAR (7) NOT NULL, "
                    + "dateTimeCreated DATETIME, "
                    + "PRIMARY KEY (id)"
                    + ")";

    public static void insertDefaultColors(Connection connection) throws SQLException {

        if (DbHelper.hasEntry(tableConnectionInfo, "")) {
            return;
        }

        String query = "INSERT INTO " + tableConnectionInfo.tableName +
                "(name, color, dateTimeCreated) VALUES (?, ?, ?)";
        PreparedStatement preparedStatement =
                connection.prepareStatement(query);

        String[][] colors = {
                {"Stahlblau", "#4f8ef7"},
                {"Salbeigrün", "#5aab7f"},
                {"Terrakotta", "#c4694f"},
                {"Lavendel", "#8b7ec8"},
                {"Sandbraun", "#b89a5a"},
                {"Petrol", "#3d8fa6"},
                {"Altrosa", "#c47e8b"},
                {"Schiefergrau", "#6b7f9e"},
                {"Olivgrün", "#7a9a4f"},
                {"Kupfer", "#b8734f"},
        };

        for (String[] color : colors) {
            preparedStatement.setString(1, color[0]);
            preparedStatement.setString(2, color[1]);
            java.sql.Timestamp timestamp = new java.sql.Timestamp(System.currentTimeMillis());
            preparedStatement.setTimestamp(3, timestamp);
            preparedStatement.executeUpdate();
        }

        preparedStatement.close();
    }

    public static void buildTable() {
        DbHelper.buildTable(tableConnectionInfo, QUERY_CREATE);
    }

    public static String getTable(int skip, int top)
            throws JsonProcessingException, SQLException {

        com.panikradius.sdms.ResultTableData resultTableData =
                DbHelper.getTableResultSet(tableConnectionInfo, skip, top, "");

        if (resultTableData == null) {return "";}
        ResultSet resultSet = resultTableData.resultSet;
        if (resultSet == null) {return "";}

        ArrayList<Color> fetchResult = new ArrayList<Color>();
        while (resultSet.next()) {
            fetchResult.add(
                    new Color(
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

    public static void postPreparedStatement(Color color) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

            String query = "INSERT INTO " + tableConnectionInfo.tableName +
                    " (name, color, dateTimeCreated) " +
                    " VALUES (?,?,NOW())";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, color.name);
            preparedStatement.setString(2, color.color);
            preparedStatement.executeUpdate();

        } catch (Exception e) {
        } finally {
            try { preparedStatement.close(); } catch (Exception e) { /* Ignored */ }
            try { connection.close(); } catch (Exception e) { /* Ignored */ }
        }
    }

    public static void Edit(Color color) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

            String query = "UPDATE " + tableConnectionInfo.tableName +
                    " SET name = ?, color = ? WHERE id = ?";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, color.name);
            preparedStatement.setString(2, color.color);
            preparedStatement.setInt(3, color.id);
            preparedStatement.executeUpdate();

        } catch (Exception e) {
        } finally {
            try { preparedStatement.close(); } catch (Exception e) { /* Ignored */ }
            try { connection.close(); } catch (Exception e) { /* Ignored */ }
        }
    }

    public static void deleteById(int id) {
        DbHelper.deleteById(tableConnectionInfo, id);
    }

}
