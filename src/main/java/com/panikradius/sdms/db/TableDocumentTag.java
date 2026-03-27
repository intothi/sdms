package com.panikradius.sdms.db;

import com.panikradius.sdms.models.DocumentTag;
import com.panikradius.sdms.models.Tag;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TableDocumentTag {

    public static TableConnectionInfo tableConnectionInfo =
            new TableConnectionInfo(
                    DbConnection.DATABASE_NAME,
                    "document_tag",
                    DbConnection.USER,
                    DbConnection.PW
            );

    public static String QUERY_CREATE =
            "CREATE TABLE " + tableConnectionInfo.tableName + " ("
                    + "documentID INT UNSIGNED NOT NULL, "
                    + "tagID INT UNSIGNED NOT NULL, "
                    + "PRIMARY KEY (documentID, tagID), "
                    + "FOREIGN KEY (documentID) REFERENCES document(id), "
                    + "FOREIGN KEY (tagID) REFERENCES tag(id)"
                    + ")";

    public static void buildTable() {
        DbHelper.buildTable(tableConnectionInfo, QUERY_CREATE);
    }

    public static void postPreparedStatement(
            Connection connection,
            DocumentTag documentTag) {

        PreparedStatement preparedStatement = null;
        try {

            String query = "INSERT INTO " + tableConnectionInfo.tableName +
                    " (documentID, tagID) " +
                    " VALUES (?,?)";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, documentTag.documentID);
            preparedStatement.setInt(2, documentTag.tagID);
            preparedStatement.executeUpdate();

        } catch (Exception e) {
        } finally {
            try { preparedStatement.close(); } catch (Exception e) { /* Ignored */ }
        }
    }

    public static void deleteByDocumentId(Connection connection, int documentId) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(
                "DELETE FROM " + tableConnectionInfo.tableName
                        + " WHERE documentID = ?");

        preparedStatement.setInt(1, documentId);
        preparedStatement.executeUpdate();
    }
}
