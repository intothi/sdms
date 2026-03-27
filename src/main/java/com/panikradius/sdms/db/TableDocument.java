package com.panikradius.sdms.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panikradius.sdms.App;
import com.panikradius.sdms.ResultTableData;
import com.panikradius.sdms.models.Document;
import com.panikradius.sdms.models.Tag;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TableDocument {

    public static final TableConnectionInfo tableConnectionInfo = new TableConnectionInfo(
            DbConnection.DATABASE_NAME,
            "document",
            DbConnection.USER,
            DbConnection.PW
    );

    private static final String QUERY_CREATE =  "CREATE TABLE " + tableConnectionInfo.tableName + " ("
            + "id INT UNSIGNED NOT NULL AUTO_INCREMENT, "
            + "fileName VARCHAR (255) NOT NULL, "
            + "comment VARCHAR (4095) NOT NULL, "
            + "dateDocument DATE, "
            + "dateTimeArchived DATETIME, "
            + "PRIMARY KEY (id)"
            + ")";

    public static void buildTable() {
        DbHelper.buildTable(tableConnectionInfo, QUERY_CREATE);
    }

    public static Response get(String filename) {
        String path = App.pathToDms + filename;

        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get(path));
            return Response.ok(fileBytes)
                    .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                    .type("application/pdf")
                    .build();
        } catch (IOException e) {
            return Response.serverError().build();
        }
    }

    public static void deleteById(Connection connection, int id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM " + tableConnectionInfo.tableName + " WHERE id = ?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public static int post(Connection connection, Document document) {

        PreparedStatement preparedStatement = null;
        int documentID = 0;
        try {

            String query = "INSERT INTO " + tableConnectionInfo.tableName +
                    " (fileName, comment, dateDocument, dateTimeArchived) " +
                    " VALUES (?,?,?,?)";

            preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, document.fileName);
            preparedStatement.setString(2, document.comment);
            preparedStatement.setDate(3, document.dateDocument);
            preparedStatement.setTimestamp(4, new java.sql.Timestamp(System.currentTimeMillis()));
            preparedStatement.executeUpdate();
            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            if (resultSet.next()) {
                documentID = resultSet.getInt(1);
            }

        } catch (Exception e) {
        } finally {
            try { preparedStatement.close(); } catch (Exception e) { /* Ignored */ }
        }

        return documentID;
    }

    public static String getTable(
            int skip,
            int top,
            String name,
            String[] tagIds ) throws JsonProcessingException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        PreparedStatement countStatement = null;
        Map<Integer, Document> documentMap = new LinkedHashMap<>();
        int totalCount = 0;

        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

            StringBuilder whereClause = new StringBuilder();
            List<Object> params = new ArrayList<>();

            if (!name.isEmpty()) {
                whereClause.append(" WHERE LOWER(d.fileName) LIKE LOWER(?) ");
                params.add("%" + name + "%");
            }

            if (tagIds.length > 0) {
                whereClause.append((whereClause.length() == 0) ? " WHERE " : " AND ");
                whereClause.append("dt.tagID IN (");
                for (int i = 0; i < tagIds.length; i++) {
                    whereClause.append(i == 0 ? "?" : ",?");
                    params.add(tagIds[i]);
                }
                whereClause.append(")");
            }

            String countQuery = "SELECT COUNT(DISTINCT d.id) " +
                    "FROM document d " +
                    "LEFT JOIN document_tag dt ON dt.documentID = d.id " +
                    whereClause;

            countStatement = connection.prepareStatement(countQuery);
            for (int i = 0; i < params.size(); i++) {
                countStatement.setObject(i + 1, params.get(i));
            }

            ResultSet countResult = countStatement.executeQuery();
            if (countResult.next()) {
                totalCount = countResult.getInt(1);
            }

            // ── Haupt Query mit Subquery für korrektes LIMIT ─────
            List<Object> mainParams = new ArrayList<>(params);

            StringBuilder mainQuery = new StringBuilder();
            mainQuery.append("SELECT d.id, d.fileName, d.comment, d.dateDocument, d.dateTimeArchived, ");
            mainQuery.append("t.id AS tagId, t.name AS tagName, c.color AS tagColor ");
            mainQuery.append("FROM (SELECT * FROM document d ");

            if (whereClause.length() == 0) {
                mainQuery.append("LEFT JOIN document_tag dt ON dt.documentID = d.id ");
                mainQuery.append(whereClause);
                mainQuery.append(" GROUP BY d.id ");
            }

            mainQuery.append("ORDER BY id ");

            if (top != 0) {
                mainQuery.append("LIMIT ? OFFSET ?");
                mainParams.add(top);
                mainParams.add(skip);
            }

            mainQuery.append(") d ");
            mainQuery.append("LEFT JOIN document_tag dt ON dt.documentID = d.id ");
            mainQuery.append("LEFT JOIN tag t ON t.id = dt.tagID ");
            mainQuery.append("LEFT JOIN color c ON c.id = t.colorId ");
            mainQuery.append("ORDER BY d.id");

            preparedStatement = connection.prepareStatement(mainQuery.toString());
            for (int i = 0; i < mainParams.size(); i++) {
                preparedStatement.setObject(i + 1, mainParams.get(i));
            }

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                int docId = resultSet.getInt("id");

                if (!documentMap.containsKey(docId)) {
                    Document document = new Document();
                    document.id = docId;
                    document.fileName = resultSet.getString("fileName");
                    document.comment = resultSet.getString("comment");
                    document.dateDocument = resultSet.getDate("dateDocument");
                    document.dateTimeArchived = resultSet.getTimestamp("dateTimeArchived");
                    document.tags = new ArrayList<Tag>();
                    documentMap.put(docId, document);
                }

                int tagId = resultSet.getInt("tagId");
                if (tagId != 0) {
                    Tag tag = new Tag();
                    tag.id    = tagId;
                    tag.name  = resultSet.getString("tagName");
                    tag.color = resultSet.getString("tagColor");
                    documentMap.get(docId).tags.add(tag);
                }
            }

        } catch (Exception e) {
        } finally {
            try { if (countStatement != null) countStatement.close(); } catch (Exception e) { }
            try { if (preparedStatement != null) preparedStatement.close(); } catch (Exception e) { }
            try { if (connection != null) connection.close(); } catch (Exception e) { }
        }

        int totalPages = top != 0 ? (int) Math.ceil((double) totalCount / top) : 1;

        String items = new ObjectMapper().writer().withDefaultPrettyPrinter()
                .writeValueAsString(documentMap.values());

        return "{ " +
                "\"items\": " + items + "," +
                "\"totalCount\": " + totalCount + "," +
                "\"totalPages\": " + totalPages +
                "}";
    }

    public static boolean isAlreadyExisting(String fileName) {

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

            // Maria DB default is case insensitiv --> utf8_general_ci
            String query = "SELECT COUNT(*) FROM document WHERE fileName = ?";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, fileName);

            ResultSet resultSet = preparedStatement.executeQuery();

            resultSet.first();
            return resultSet.getInt(1) != 0;

        } catch (Exception e) {
            System.out.println(e);
        } finally {
            try { preparedStatement.close(); } catch (Exception e) { /* Ignored */ }
            try { connection.close(); } catch (Exception e) { /* Ignored */ }
        }

        return false;
    }

    public static String getFileNameById(Connection connection, int id) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT fileName" + " FROM " + tableConnectionInfo.tableName
                        + " WHERE id = ?");

        preparedStatement.setInt(1, id);
        ResultSet resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) { return resultSet.getString("fileName"); }
        return null;
    }
}
