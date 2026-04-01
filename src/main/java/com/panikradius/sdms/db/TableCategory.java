package com.panikradius.sdms.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panikradius.sdms.models.Category;
import com.panikradius.sdms.models.Tag;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

public class TableCategory {

    public static TableConnectionInfo tableConnectionInfo =
            new TableConnectionInfo(
                    DbConnection.DATABASE_NAME,
                    "category",
                    DbConnection.USER,
                    DbConnection.PW
            );

    public static String QUERY_CREATE = "CREATE TABLE " + tableConnectionInfo.tableName + " ("
            + "id INT UNSIGNED NOT NULL AUTO_INCREMENT, "
            + "name VARCHAR(64) NOT NULL, "
            + "dateTimeCreated DATETIME, "
            + "PRIMARY KEY (id)"
            + ")";

    public static void insertDefaultCategories(Connection connection) throws SQLException {

        if (DbHelper.hasEntry(tableConnectionInfo, "")) {
            return;
        }

        String query = "INSERT INTO " + tableConnectionInfo.tableName
                + " (name, dateTimeCreated) VALUES (?, NOW())";
        PreparedStatement preparedStatement = connection.prepareStatement(query);

        String[] categories = {
                "Dokumentenart",
                "Absender",
                "Status",
                "Person",
                "Thema"
        };

        for (int i = 0; i < categories.length; i++) {
            preparedStatement.setString(1, categories[i]);
            preparedStatement.executeUpdate();
        }

        preparedStatement.close();
    }

    public static void buildTable() {
        DbHelper.buildTable(tableConnectionInfo, QUERY_CREATE);
    }

    public static String getTable(int skip, int top)
            throws JsonProcessingException, SQLException {

        // String orderBy = "ORDER BY name DESC";

        com.panikradius.sdms.ResultTableData resultTableData =
                DbHelper.getTableResultSet(tableConnectionInfo, skip, top, "");

        if (resultTableData == null) {return "";}
        ResultSet resultSet = resultTableData.resultSet;
        if (resultSet == null) { return ""; }

        ArrayList<Category> fetchResult = new ArrayList<Category>();
        while (resultSet.next()) {
            fetchResult.add(
                    new Category(
                            Integer.parseInt(resultSet.getString(1)),
                            resultSet.getString(2),
                            Timestamp.valueOf(resultSet.getString(3))
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

    public static void postPreparedStatement(Category category) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

            String query = "INSERT INTO " + tableConnectionInfo.tableName +
                    " (name, dateTimeCreated) " +
                    " VALUES (?,NOW())";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, category.name);
            preparedStatement.executeUpdate();

        } catch (Exception e) {
        } finally {
            try { preparedStatement.close(); } catch (Exception e) { /* Ignored */ }
            try { connection.close(); } catch (Exception e) { /* Ignored */ }
        }
    }

    public static boolean isAlreadyExisting(
            com.panikradius.sdms.models.Category category) {

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

            // Maria DB default is case insensitiv --> utf8_general_ci
            String query = "SELECT COUNT(*) FROM " + tableConnectionInfo.tableName +
                    " WHERE name = ?";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, category.name);

            ResultSet resultSet = preparedStatement.executeQuery();

            resultSet.first();
            return resultSet.getInt(1) != 0;

        } catch (Exception e) {
        } finally {
            try { preparedStatement.close(); } catch (Exception e) { /* Ignored */ }
            try { connection.close(); } catch (Exception e) { /* Ignored */ }
        }

        return false;
    }

    public static void EditById(int id, com.panikradius.sdms.models.Category category) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL, tableConnectionInfo.user, tableConnectionInfo.pw);

            String query = "UPDATE " + tableConnectionInfo.tableName +
                    " SET name = ? WHERE id = ?";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, category.name);
            preparedStatement.setInt(2, id);
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
