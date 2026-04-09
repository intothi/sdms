package com.panikradius.sdms.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panikradius.sdms.App;
import com.panikradius.sdms.models.Document;
import com.panikradius.sdms.models.DocumentMeta;
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

    private static final String QUERY_CREATE = "CREATE TABLE " + tableConnectionInfo.tableName + " ("
            + "id INT UNSIGNED NOT NULL AUTO_INCREMENT, "
            + "fileName VARCHAR(255) NOT NULL, "
            + "comment VARCHAR(4095) NOT NULL, "
            + "dateDocument DATE, "
            + "dueDate DATE, "
            + "parentId INT UNSIGNED NULL, "
            + "fileSize BIGINT UNSIGNED NULL, "
            + "countPages INT UNSIGNED NULL, "
            + "countWords INT UNSIGNED NULL, "
            + "mimeType VARCHAR(127) NULL, "
            + "ocrText MEDIUMTEXT NULL, "
            + "language VARCHAR(31) NULL, "
            + "checksum CHAR(64) NULL, "
            + "deskewDone BOOLEAN NOT NULL DEFAULT FALSE, "
            + "done BOOLEAN NOT NULL DEFAULT FALSE, "
            + "dateTimeArchived DATETIME, "
            + "PRIMARY KEY (id), "
            + "CONSTRAINT fk_document_parent FOREIGN KEY (parentId) REFERENCES " + tableConnectionInfo.tableName + " (id) ON DELETE SET NULL, "
            + "FULLTEXT INDEX idx_ocr_text (ocrText)"
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
                    " (fileName, comment, dateDocument, dueDate, parentId, fileSize, " +
                    " countPages, countWords, mimeType, ocrText, language, checksum, deskewDone, dateTimeArchived) " +
                    " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())";

            preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, document.fileName);
            preparedStatement.setString(2, document.comment);
            preparedStatement.setDate(3, document.dateDocument);
            preparedStatement.setDate(4, document.dueDate);

            if (document.parentId == null) {
                preparedStatement.setNull(5, java.sql.Types.INTEGER);
            } else {
                preparedStatement.setInt(5, document.parentId);
            }

            preparedStatement.setLong(6, document.fileSize);
            preparedStatement.setInt(7, document.countPages);
            preparedStatement.setInt(8, document.countWords);
            preparedStatement.setString(9, document.mimeType);
            preparedStatement.setString(10, document.ocrText);
            preparedStatement.setString(11, document.language);
            preparedStatement.setString(12, document.checksum);
            preparedStatement.setBoolean(13,document.deskewDone);

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

    public static String getThread(int id) throws Exception {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        List<com.panikradius.sdms.models.Document> documentArrayList = new ArrayList<>();

        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

            String query =
                    "WITH RECURSIVE thread AS (" +
                            "  SELECT * FROM " + tableConnectionInfo.tableName + " WHERE id = ? " +
                            "  UNION ALL " +
                            "  SELECT d.* FROM " + tableConnectionInfo.tableName + " d " +
                            "  INNER JOIN thread t ON d.parentId = t.id " +
                            ") " +
                            "SELECT * FROM thread WHERE id != ?";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, id);
            preparedStatement.setInt(2, id);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                com.panikradius.sdms.models.Document document = new com.panikradius.sdms.models.Document(
                        resultSet.getInt("id"),
                        resultSet.getString("fileName"),
                        resultSet.getString("comment"),
                        resultSet.getDate("dateDocument"),
                        resultSet.getDate("dueDate"),
                        resultSet.getTimestamp("dateTimeArchived")
                );
                document.parentId = resultSet.getInt("parentId");
                document.countPages = resultSet.getInt("countPages");
                document.fileSize = resultSet.getLong("fileSize");
                documentArrayList.add(document);
            }

        } finally {
            try { if (preparedStatement != null) preparedStatement.close(); } catch (Exception e) {}
            try { if (connection != null) connection.close(); } catch (Exception e) {}
        }

        return new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(documentArrayList);
    }

    public static String getTable(
            int skip,
            int top,
            String name,
            String[] tagIds,
            String sortBy,
            String sortDir) throws JsonProcessingException {

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

            // ── WHERE Clause nur auf document-Ebene ──────────────
            StringBuilder whereClause = new StringBuilder();
            List<Object> params = new ArrayList<>();

            if (!name.isEmpty()) {
                whereClause.append(" WHERE LOWER(fileName) LIKE LOWER(?) ");
                params.add("%" + name + "%");
            }

            // Tag-Filter als IN-Subquery auf document_tag
            if (tagIds.length > 0) {
                whereClause.append(whereClause.length() == 0 ? " WHERE " : " AND ");
                whereClause.append("id IN (SELECT documentID FROM document_tag WHERE tagID IN (");
                for (int i = 0; i < tagIds.length; i++) {
                    whereClause.append(i == 0 ? "?" : ",?");
                    params.add(tagIds[i]);
                }
                whereClause.append("))");
            }

            StringBuilder sortClause = new StringBuilder();
            if (!sortBy.isEmpty()) {
                sortClause.append(" ORDER BY " + sortBy + " " + sortDir);
            }

            // ── COUNT Query ───────────────────────────────────────
            String countQuery = "SELECT COUNT(*) FROM document" + whereClause;

            countStatement = connection.prepareStatement(countQuery);
            for (int i = 0; i < params.size(); i++) {
                countStatement.setObject(i + 1, params.get(i));
            }

            ResultSet countResult = countStatement.executeQuery();
            if (countResult.next()) {
                totalCount = countResult.getInt(1);
            }


            List<Object> mainParams = new ArrayList<>(params);

            StringBuilder mainQuery = new StringBuilder();
            mainQuery.append("SELECT d.id, d.fileName, d.comment, d.dateDocument, d.dueDate, d.dateTimeArchived, d.parentId, d.fileSize, d.countPages, d.done, ");
            mainQuery.append("t.id AS tagId, t.name AS tagName, c.color AS tagColor ");
            mainQuery.append("FROM (SELECT * FROM document");
            mainQuery.append(whereClause);
            //mainQuery.append(" ORDER BY id ");
            mainQuery.append(sortClause);
            mainQuery.append(" ");

            if (top != 0) {
                mainQuery.append("LIMIT ? OFFSET ?");
                mainParams.add(top);
                mainParams.add(skip);
            }

            mainQuery.append(") d ");
            mainQuery.append("LEFT JOIN document_tag dt ON dt.documentID = d.id ");
            mainQuery.append("LEFT JOIN tag t ON t.id = dt.tagID ");
            mainQuery.append("LEFT JOIN color c ON c.id = t.colorId ");
            //mainQuery.append("ORDER BY d.id");
            mainQuery.append(sortClause);

            preparedStatement = connection.prepareStatement(mainQuery.toString());
            for (int i = 0; i < mainParams.size(); i++) {
                preparedStatement.setObject(i + 1, mainParams.get(i));
            }

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                int docId = resultSet.getInt("id");

                if (!documentMap.containsKey(docId)) {
                    Document document = new Document(
                            docId,
                            resultSet.getString("fileName"),
                            resultSet.getString("comment"),
                            resultSet.getDate("dateDocument"),
                            resultSet.getDate("dueDate"),
                            resultSet.getTimestamp("dateTimeArchived")
                    );

                    document.parentId = resultSet.getInt("parentId");
                    document.fileSize = resultSet.getLong("fileSize");
                    document.countPages = resultSet.getInt("countPages");
                    document.done = resultSet.getBoolean("done");
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

    public static void updateMeta(int id, DocumentMeta meta) throws Exception {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

            String query = "UPDATE " + tableConnectionInfo.tableName +
                    " SET comment = ?, dateDocument = ?, dueDate = ?, parentId = ? WHERE id = ?";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, meta.comment);
            preparedStatement.setDate(2, meta.dateDocument != null ? Date.valueOf(meta.dateDocument) : null);
            preparedStatement.setDate(3, meta.dueDate != null ? Date.valueOf(meta.dueDate) : null);
            if (meta.parentId == null) {
                preparedStatement.setNull(4, java.sql.Types.INTEGER);
            } else {
                preparedStatement.setInt(4, meta.parentId);
            }
            preparedStatement.setInt(5, id);
            preparedStatement.executeUpdate();

        } finally {
            try { if (preparedStatement != null) preparedStatement.close(); } catch (Exception e) {}
            try { if (connection != null) connection.close(); } catch (Exception e) {}
        }
    }

    public static void updateDone(int id, boolean done) throws Exception {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

            preparedStatement = connection.prepareStatement(
                    "UPDATE " + tableConnectionInfo.tableName + " SET done = ? WHERE id = ?"
            );
            preparedStatement.setBoolean(1, done);
            preparedStatement.setInt(2, id);
            preparedStatement.executeUpdate();

        } finally {
            try { if (preparedStatement != null) preparedStatement.close(); } catch (Exception e) {}
            try { if (connection != null) connection.close(); } catch (Exception e) {}
        }
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

    public static void updateFileName(Connection connection, int id, String newFileName) throws Exception {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(
                    "UPDATE " + tableConnectionInfo.tableName + " SET fileName = ? WHERE id = ?"
            );
            preparedStatement.setString(1, newFileName);
            preparedStatement.setInt(2, id);
            preparedStatement.executeUpdate();
        } finally {
            try { if (preparedStatement != null) preparedStatement.close(); } catch (Exception e) {}
        }
    }

    public static List<com.panikradius.sdms.models.Document> getAll() throws Exception {
        Connection connection = DriverManager.getConnection(
                DbConnection.DB_URL,
                DbConnection.USER,
                DbConnection.PW);

        String query = "SELECT fileName, checksum FROM document";
        PreparedStatement stmt = connection.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();

        List<com.panikradius.sdms.models.Document> result = new ArrayList<>();
        while (rs.next()) {
            com.panikradius.sdms.models.Document doc = new com.panikradius.sdms.models.Document();
            doc.fileName = rs.getString(1);
            doc.checksum = rs.getString(2);
            result.add(doc);
        }

        try { rs.close(); } catch (Exception e) { /* Ignored */ }
        try { stmt.close(); } catch (Exception e) { /* Ignored */ }
        try { connection.close(); } catch (Exception e) { /* Ignored */ }

        return result;
    }
}
