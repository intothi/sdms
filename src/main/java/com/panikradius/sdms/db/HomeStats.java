package com.panikradius.sdms.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.panikradius.sdms.App;
import com.panikradius.sdms.Logger;
import com.panikradius.sdms.models.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
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
        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);
        } catch (Exception e) {
            Logger.log(e.getMessage(), Log.LogLevel.ERROR);
            return "";
        }

        com.panikradius.sdms.models.HomeStats homeStats = new com.panikradius.sdms.models.HomeStats();

        fillDocInfo(connection, homeStats);
        fillDocDueInfo(connection, homeStats);
        fillLastBackupInfo(connection,homeStats);
        try { connection.close(); } catch (Exception e) {}
        fillFileSystemInfo(homeStats);
        fillRAMInfo(homeStats);
        fillJVMINFO(homeStats);

        return new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(homeStats);
    }

    private static void fillDocInfo(
            Connection connection,
            com.panikradius.sdms.models.HomeStats homeStats)
    {
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(
                    "SELECT COUNT(*) AS totalDocuments, COALESCE(SUM(fileSize), 0) AS totalSize FROM document"
            );

            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            if (resultSet.next()) {
                homeStats.totalDocuments = resultSet.getInt("totalDocuments");
                homeStats.totalSize = resultSet.getLong("totalSize");
            }

        } catch (Exception e) { Logger.log(e.getMessage(), Log.LogLevel.ERROR); }
    }

    private static void fillDocDueInfo(
            Connection connection,
            com.panikradius.sdms.models.HomeStats homeStats)
    {

        int dueDays = 14;
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(
                    "SELECT id, fileName, dueDate FROM document " +
                            "WHERE dueDate IS NOT NULL AND dueDate <= DATE_ADD(CURDATE(), INTERVAL " + dueDays + " DAY) " +
                            "AND done = FALSE " +
                            "ORDER BY dueDate ASC"
            );
            ResultSet resultSet = preparedStatement.executeQuery();
            preparedStatement.close();

            while (resultSet.next()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id",       resultSet.getInt("id"));
                entry.put("fileName", resultSet.getString("fileName"));
                entry.put("dueDate",  resultSet.getString("dueDate"));
                homeStats.dueSoon.add(entry);
            }

        } catch (Exception e) { Logger.log(e.getMessage(), Log.LogLevel.ERROR); }
    }

    private static void fillFileSystemInfo(
            com.panikradius.sdms.models.HomeStats homeStats
    ) {

        try {
            File dmsDir = new File(App.pathToDms);
            long diskTotal = dmsDir.getTotalSpace();
            long diskFree  = dmsDir.getUsableSpace();
            long diskUsed  = diskTotal - diskFree;

            homeStats.diskTotal = diskTotal;
            homeStats.diskFree = diskFree;
            homeStats.diskUsed = diskUsed;

        } catch (Exception e) {
            Logger.log(e.getMessage(), Log.LogLevel.ERROR);
        }
    }

    private static void fillRAMInfo(com.panikradius.sdms.models.HomeStats homeStats){

        ProcessBuilder processBuilder = new ProcessBuilder("free", "-k");
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String ramLine;
            int ramLineCount = 0;
            while ((ramLine = bufferedReader.readLine()) != null) {
                ramLineCount++;
                if (ramLineCount == 2) {
                    String[] parts = ramLine.trim().split("\\s+");
                    homeStats.ramTotal = Long.parseLong(parts[1]) * 1024;
                    homeStats.ramUsed  = Long.parseLong(parts[2]) * 1024;
                    homeStats.ramFree  = Long.parseLong(parts[3]) * 1024;
                }
            }
            process.waitFor();

        } catch (Exception e) {
            Logger.log(e.getMessage(), Log.LogLevel.ERROR);
        }
    }

    private static void fillJVMINFO(
            com.panikradius.sdms.models.HomeStats homeStats
    )
    {
        Runtime runtime = Runtime.getRuntime();
        homeStats.jvmUsed  = runtime.totalMemory() - runtime.freeMemory();
        homeStats.jvmTotal = runtime.maxMemory();
    }

    private static void fillLastBackupInfo(
            Connection connection,
            com.panikradius.sdms.models.HomeStats homeStats
    )
    {
        try {
            PreparedStatement prepareStatement = connection.prepareStatement(
                    "SELECT dateTimeCreated FROM backup ORDER BY dateTimeCreated DESC LIMIT 1"
            );
            ResultSet resultSet = prepareStatement.executeQuery();
            if (resultSet.next()) {
                homeStats.lastBackup = resultSet.getString("dateTimeCreated");
            }
            prepareStatement.close();

        } catch (Exception e) {
            Logger.log(e.getMessage(), Log.LogLevel.ERROR);
        }
    }

}
