package com.panikradius.sdms.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panikradius.sdms.ResultTableData;
import com.panikradius.sdms.models.Document;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;

public class TableDocument {

    public static final TableConnectionInfo tableConnectionInfo = new TableConnectionInfo(
            DbConnection.DATABASE_NAME,
            "document",
            DbConnection.USER,
            DbConnection.PW
    );

    private static final String QUERY_CREATE =  "CREATE TABLE " + tableConnectionInfo.tableName + " ("
            + "id INT UNSIGNED NOT NULL AUTO_INCREMENT, "
            + "name VARCHAR (255) NOT NULL, "
            + "comment VARCHAR (4095) NOT NULL, "
            + "dateDocument DATE, "
            + "dateTimeArchived DATETIME, "
            + "PRIMARY KEY (id)"
            + ")";

    public static void buildTable() {
        DbHelper.buildTable(tableConnectionInfo, QUERY_CREATE);
    }

    public static String get(int id) throws JsonProcessingException {
        return DbHelper.getById(tableConnectionInfo, id);
    }

    public static void deleteById(int id) {
        DbHelper.deleteById(tableConnectionInfo, id);
    }

    public static int post(Connection connection, Document document) {

        PreparedStatement preparedStatement = null;
        int documentID = 0;
        try {

            String query = "INSERT INTO " + tableConnectionInfo.tableName +
                    " (name, comment, dateDocument, dateTimeArchived) " +
                    " VALUES (?,?,?,?)";

            preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, document.name);
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

    public static String getTable(int skip, int top) throws JsonProcessingException, SQLException {

        ResultTableData resultTableData = DbHelper.getTableResultSet(tableConnectionInfo, skip, top, "");
        if (resultTableData == null) {return "";}
        ResultSet resultSet = resultTableData.resultSet;
        if (resultSet == null) { return ""; }

        ArrayList<Document> documents = new ArrayList<Document>();
        while (resultSet.next()) {
            documents.add(new Document(
                    Integer.parseInt(resultSet.getString(1)),
                    resultSet.getString(2),
                    resultSet.getString(3),
                    Date.valueOf(resultSet.getString(5)),
                    Timestamp.valueOf(resultSet.getString(6)))
            );
        }

        String items = new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(documents);

        String result = "{ " +
                "\"items\": " + items + "," +
                "\"totalCount\": " + resultTableData.count +
                "}";
        return result;
    }
}
