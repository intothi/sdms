package com.panikradius.sdms.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panikradius.sdms.models.Tag;
import com.panikradius.sdms.models.TagKeyword;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TableTagKeyword {

    public static TableConnectionInfo tableConnectionInfo =
            new TableConnectionInfo(
                    DbConnection.DATABASE_NAME,
                    "tag_keyword",
                    DbConnection.USER,
                    DbConnection.PW
            );

    public static String QUERY_CREATE =  "CREATE TABLE " + tableConnectionInfo.tableName + " ("
            + "id INT UNSIGNED NOT NULL AUTO_INCREMENT, "
            + "tagId INT UNSIGNED NOT NULL, "
            + "keyword VARCHAR(128) NOT NULL, "
            + "dateTimeCreated DATETIME, "
            + "PRIMARY KEY (id), "
            + "FOREIGN KEY (tagId) REFERENCES tag(id)"
            + ")";

    public static void buildTable() {
        DbHelper.buildTable(tableConnectionInfo, QUERY_CREATE);
    }

    public static void postPreparedStatement(TagKeyword tagKeyword) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

            String query = "INSERT INTO " + tableConnectionInfo.tableName +
                    " (tagId, keyword, dateTimeCreated) " +
                    " VALUES (?,?,NOW())";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, tagKeyword.tagId);
            preparedStatement.setString(2, tagKeyword.keyword);
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

    public static void Edit(TagKeyword tagKeyword) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw
            );

            String query = "UPDATE " + tableConnectionInfo.tableName +
                    " SET tagId = ?, keyword = ? WHERE id = ?";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, tagKeyword.tagId);
            preparedStatement.setString(2, tagKeyword.keyword);
            preparedStatement.setInt(3, tagKeyword.id);
            preparedStatement.executeUpdate();

        } catch (Exception e) {
        } finally {
            try { preparedStatement.close(); } catch (Exception e) { /* Ignored */ }
            try { connection.close(); } catch (Exception e) { /* Ignored */ }
        }
    }

    public static String getTable() throws JsonProcessingException, SQLException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

            String query = "SELECT id, tagId, keyword, dateTimeCreated" +
                    " FROM " + tableConnectionInfo.tableName +
                    " ORDER BY tagId";

            preparedStatement = connection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();

            ArrayList<TagKeyword> fetchResult = new ArrayList<>();
            while (resultSet.next()) {
                TagKeyword tagKeyword = new TagKeyword();
                tagKeyword.id              = resultSet.getInt("id");
                tagKeyword.tagId           = resultSet.getInt("tagId");
                tagKeyword.keyword         = resultSet.getString("keyword");
                tagKeyword.dateTimeCreated = resultSet.getTimestamp("dateTimeCreated");
                fetchResult.add(tagKeyword);
            }

            String items = new ObjectMapper().writer().withDefaultPrettyPrinter()
                    .writeValueAsString(fetchResult);

            return "{ "
                    + "\"items\": " + items
                    + ", \"totalCount\": " + fetchResult.size()
                    + "}";

        } finally {
            try { if (preparedStatement != null) preparedStatement.close(); } catch (Exception e) { }
            try { if (connection != null) connection.close(); } catch (Exception e) { }
        }
    }

    public static Map<String, Integer> getKeywordToTagIdMap() throws SQLException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

            String query = "SELECT keyword, tagId " +
                    "FROM " + tableConnectionInfo.tableName;

            preparedStatement = connection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();

            Map<String, Integer> keywordToTagId = new HashMap<>();
            while (resultSet.next()) {
                keywordToTagId.put(
                        resultSet.getString("keyword").toLowerCase(),
                        resultSet.getInt("tagId")
                );
            }

            return keywordToTagId;

        } finally {
            try { if (preparedStatement != null) preparedStatement.close(); } catch (Exception e) { }
            try { if (connection != null) connection.close(); } catch (Exception e) { }
        }
    }

    public static boolean isAlreadyExisting(TagKeyword tagKeyword) {

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

            // Maria DB default is case insensitiv --> utf8_general_ci
            String query = "SELECT COUNT(*) FROM " + tableConnectionInfo.tableName
                    + " WHERE keyword = ?";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, tagKeyword.keyword);

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

}
