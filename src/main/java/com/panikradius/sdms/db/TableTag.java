package com.panikradius.sdms.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panikradius.sdms.models.Tag;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TableTag {

    public static TableConnectionInfo tableConnectionInfo =
            new TableConnectionInfo(
                    DbConnection.DATABASE_NAME,
                    "tag",
                    DbConnection.USER,
                    DbConnection.PW
    );

    public static String QUERY_CREATE = "CREATE TABLE " + tableConnectionInfo.tableName + " ("
            + "id INT UNSIGNED NOT NULL AUTO_INCREMENT, "
            + "name VARCHAR(32) NOT NULL, "
            + "colorId INT UNSIGNED NOT NULL, "
            + "categoryId INT UNSIGNED, "
            + "dateTimeCreated DATETIME, "
            + "PRIMARY KEY (id), "
            + "FOREIGN KEY (colorId) REFERENCES color(id), "
            + "FOREIGN KEY (categoryId) REFERENCES category(id)"
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
                    " (name, colorId, categoryId, dateTimeCreated) " +
                    " VALUES (?,?,?,NOW())";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, tag.name);
            preparedStatement.setInt(2, tag.colorId);
            preparedStatement.setInt(3, tag.categoryId);
            preparedStatement.executeUpdate();

        } catch (Exception e) {
        } finally {
            try { preparedStatement.close(); } catch (Exception e) { /* Ignored */ }
            try { connection.close(); } catch (Exception e) { /* Ignored */ }
        }
    }

    public static boolean isAlreadyExisting(com.panikradius.sdms.models.Tag tag) {

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

            // Maria DB default is case insensitiv --> utf8_general_ci
            String query = "SELECT COUNT(*) FROM " + tableConnectionInfo.tableName
                    + " tag WHERE name = ?";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, tag.name);

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
                    " SET name = ?, colorId = ?, categoryId = ? WHERE id = ?";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, tag.name);
            preparedStatement.setInt(2, tag.colorId);
            preparedStatement.setInt(3, tag.categoryId);
            preparedStatement.setInt(4, id);
            preparedStatement.executeUpdate();

        } catch (Exception e) {
        } finally {
            try { preparedStatement.close(); } catch (Exception e) { /* Ignored */ }
            try { connection.close(); } catch (Exception e) { /* Ignored */ }
        }
    }

    public static String getTable(int skip, int top)
            throws JsonProcessingException, SQLException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

            String countQuery = "SELECT COUNT(*) FROM " + tableConnectionInfo.tableName;
            ResultSet countResult = connection.createStatement().executeQuery(countQuery);
            int totalCount = 0;
            if (countResult.next()) {
                totalCount = countResult.getInt(1);
            }

            if (totalCount == 0) {
                return "{ \"items\": [], \"totalCount\": 0 }";
            }

            StringBuilder query = new StringBuilder();
            query.append("SELECT t.id, t.name, t.colorId, t.categoryId, t.dateTimeCreated, c.color AS colorHex ");
            query.append("FROM " + tableConnectionInfo.tableName + " t ");
            query.append("LEFT JOIN color c ON c.id = t.colorId ");
            query.append("ORDER BY t.id ");

            if (top != 0) {
                query.append("LIMIT ? OFFSET ?");
                preparedStatement = connection.prepareStatement(query.toString());
                preparedStatement.setInt(1, top);
                preparedStatement.setInt(2, skip);
            } else {
                preparedStatement = connection.prepareStatement(query.toString());
            }

            ResultSet resultSet = preparedStatement.executeQuery();

            ArrayList<Tag> fetchResult = new ArrayList<>();
            while (resultSet.next()) {
                Tag tag = new Tag();
                tag.id = resultSet.getInt("id");
                tag.name = resultSet.getString("name");
                tag.categoryId = resultSet.getInt("categoryId");
                tag.dateTimeCreated = resultSet.getTimestamp("dateTimeCreated");
                tag.colorId = resultSet.getInt("colorId");
                tag.color = resultSet.getString("colorHex");
                fetchResult.add(tag);
            }

            String items = new ObjectMapper().writer().withDefaultPrettyPrinter()
                    .writeValueAsString(fetchResult);

            return "{ \"items\": " + items + ", \"totalCount\": " + totalCount + "}";

        } finally {
            try { preparedStatement.close(); } catch (Exception e) { }
            try { connection.close(); } catch (Exception e) { }
        }
    }

    public static List<String> getNamesByIds(Integer[] ids) throws SQLException {

        if (ids.length == 0) { return new ArrayList<>(); }

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < ids.length; i++) {
                if (i > 0) { placeholders.append(", "); }
                placeholders.append("?");
            }

            String query = "SELECT name FROM " + tableConnectionInfo.tableName +
                    " WHERE id IN (" + placeholders + ") ORDER BY name";

            preparedStatement = connection.prepareStatement(query);
            for (int i = 0; i < ids.length; i++) {
                preparedStatement.setInt(i + 1, ids[i]);
            }

            ResultSet resultSet = preparedStatement.executeQuery();

            List<String> names = new ArrayList<>();
            while (resultSet.next()) {
                names.add(resultSet.getString("name"));
            }

            return names;

        } finally {
            try { if (preparedStatement != null) preparedStatement.close(); } catch (Exception e) { }
            try { if (connection != null) connection.close(); } catch (Exception e) { }
        }
    }
}

