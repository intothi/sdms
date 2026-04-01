package com.panikradius.sdms.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.panikradius.sdms.App;
import com.panikradius.sdms.Logger;
import com.panikradius.sdms.models.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
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

            File dmsDir = new File(App.pathToDms);
            long diskTotal = dmsDir.getTotalSpace();
            long diskFree  = dmsDir.getUsableSpace();
            long diskUsed  = diskTotal - diskFree;

            // RAM
            long ramTotal = 0, ramUsed = 0, ramFree = 0;
            ProcessBuilder pbRam = new ProcessBuilder("free", "-k");
            pbRam.redirectErrorStream(true);
            Process processRam = pbRam.start();
            BufferedReader readerRam = new BufferedReader(new InputStreamReader(processRam.getInputStream()));
            String ramLine;
            int ramLineCount = 0;
            while ((ramLine = readerRam.readLine()) != null) {
                ramLineCount++;
                if (ramLineCount == 2) {
                    String[] parts = ramLine.trim().split("\\s+");
                    ramTotal = Long.parseLong(parts[1]) * 1024;
                    ramUsed  = Long.parseLong(parts[2]) * 1024;
                    ramFree  = Long.parseLong(parts[3]) * 1024;
                }
            }
            processRam.waitFor();

            // JVM
            Runtime runtime = Runtime.getRuntime();
            long jvmUsed  = runtime.totalMemory() - runtime.freeMemory();
            long jvmTotal = runtime.maxMemory();

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
            result.put("ramTotal",       ramTotal);
            result.put("ramUsed",        ramUsed);
            result.put("ramFree",        ramFree);
            result.put("jvmUsed",        jvmUsed);
            result.put("jvmTotal",       jvmTotal);
            result.put("lastBackup",     lastBackup);

            return new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(result);

        } finally {
            try { if (preparedStatementStats != null) preparedStatementStats.close(); } catch (Exception e) {}
            try { if (preparedStatementDue  != null) preparedStatementDue.close();  } catch (Exception e) {}
            try { if (connection != null) connection.close(); } catch (Exception e) {}
        }
    }
}
