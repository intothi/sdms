package com.panikradius.sdms.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.panikradius.sdms.App;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HomeStats {

    public static TableConnectionInfo tableConnectionInfo =
            new TableConnectionInfo(
                    DbConnection.DATABASE_NAME,
                    "",
                    DbConnection.USER,
                    DbConnection.PW
            );

    public static String getStats() throws Exception {
        Connection connection = null;
        PreparedStatement preparedStatementStats = null;
        PreparedStatement preparedStatementDue = null;

        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

            // Gesamtanzahl + Gesamtgröße
            preparedStatementStats = connection.prepareStatement(
                    "SELECT COUNT(*) AS totalDocuments, COALESCE(SUM(fileSize), 0) AS totalSize FROM document"
            );
            ResultSet resultSetStats = preparedStatementStats.executeQuery();
            int totalDocuments = 0;
            long totalSize = 0;
            if (resultSetStats.next()) {
                totalDocuments = resultSetStats.getInt("totalDocuments");
                totalSize      = resultSetStats.getLong("totalSize");
            }

            // Fällige Dokumente in den nächsten 14 Tagen
            preparedStatementDue = connection.prepareStatement(
                    "SELECT id, fileName, dueDate FROM document " +
                            "WHERE dueDate IS NOT NULL AND dueDate >= CURDATE() AND dueDate <= DATE_ADD(CURDATE(), INTERVAL 14 DAY) " +
                            "ORDER BY dueDate ASC"
            );
            ResultSet resultSetDue = preparedStatementDue.executeQuery();
            List<Map<String, Object>> dueSoon = new ArrayList<>();
            while (resultSetDue.next()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id",       resultSetDue.getInt("id"));
                entry.put("fileName", resultSetDue.getString("fileName"));
                entry.put("dueDate",  resultSetDue.getString("dueDate"));
                dueSoon.add(entry);
            }

            // Speicherplatz Pi
            File dmsDir = new File(App.pathToDms);
            long diskTotal = dmsDir.getTotalSpace();
            long diskFree  = dmsDir.getUsableSpace();
            long diskUsed  = diskTotal - diskFree;

            // Letztes Backup
            // TODO: implementieren sobald backup Tabelle existiert
            String lastBackup = null;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalDocuments", totalDocuments);
            result.put("totalSize",      totalSize);
            result.put("dueSoon",        dueSoon);
            result.put("diskTotal",      diskTotal);
            result.put("diskUsed",       diskUsed);
            result.put("diskFree",       diskFree);
            result.put("lastBackup",     lastBackup);

            return new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(result);

        } finally {
            try { if (preparedStatementStats != null) preparedStatementStats.close(); } catch (Exception e) {}
            try { if (preparedStatementDue  != null) preparedStatementDue.close();  } catch (Exception e) {}
            try { if (connection != null) connection.close(); } catch (Exception e) {}
        }
    }
}
