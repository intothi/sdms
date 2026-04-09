package com.panikradius.sdms.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.panikradius.sdms.models.ScannerConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TableScannerConfig {

    public static final TableConnectionInfo tableConnectionInfo = new TableConnectionInfo(
            DbConnection.DATABASE_NAME,
            "scanner_config",
            DbConnection.USER,
            DbConnection.PW
    );

    private static final String QUERY_CREATE =  "CREATE TABLE " + tableConnectionInfo.tableName + " ("
            + "id INT UNSIGNED NOT NULL AUTO_INCREMENT, "
            + "resolution INT NOT NULL DEFAULT 300, "
            + "colorMode VARCHAR(16) NOT NULL DEFAULT 'Gray', "
            + "scanMode VARCHAR(16) NOT NULL DEFAULT 'ADF Duplex', "
            + "swSkip INT NOT NULL DEFAULT 15, "
            + "pageWidth DECIMAL(7,3) NOT NULL DEFAULT 210.000, "
            + "pageHeight DECIMAL(7,3) NOT NULL DEFAULT 297.000, "
            + "PRIMARY KEY (id)"
            + ")";

    public static void buildTable() {
        DbHelper.buildTable(tableConnectionInfo, QUERY_CREATE);
    }

    public static void insertDefaultValues(Connection connection) throws SQLException {

        if (DbHelper.hasEntry(tableConnectionInfo)) {
            return;
        }

        String query = "INSERT INTO " + tableConnectionInfo.tableName +
                "()" +
                "VALUES ()";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public static ScannerConfig get() throws SQLException, JsonProcessingException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

            String query = "SELECT * FROM " + tableConnectionInfo.tableName + " WHERE id = 1";

            preparedStatement = connection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                ScannerConfig config = new ScannerConfig(
                        resultSet.getInt("id"),
                        resultSet.getInt("resolution"),
                        resultSet.getString("colorMode"),
                        resultSet.getString("scanMode"),
                        resultSet.getInt("swSkip"),
                        resultSet.getFloat("pageWidth"),
                        resultSet.getFloat("pageHeight")
                );
                return config;
            }

            return null;

        } finally {
            try { if (preparedStatement != null) preparedStatement.close(); } catch (Exception e) { }
            try { if (connection != null) connection.close(); } catch (Exception e) { }
        }
    }

    public static void update(ScannerConfig scannerConfig) throws SQLException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

            String query = "UPDATE " + tableConnectionInfo.tableName + " SET "
                    + "resolution = ?, "
                    + "colorMode = ?, "
                    + "scanMode = ?, "
                    + "swSkip = ?, "
                    + "pageWidth = ?, "
                    + "pageHeight = ? "
                    + "WHERE id = 1";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1,    scannerConfig.resolution);
            preparedStatement.setString(2, scannerConfig.colorMode);
            preparedStatement.setString(3, scannerConfig.scanMode);
            preparedStatement.setInt(4,    scannerConfig.swSkip);
            preparedStatement.setDouble(5, scannerConfig.pageWidth);
            preparedStatement.setDouble(6, scannerConfig.pageHeight);

            preparedStatement.executeUpdate();

        } finally {
            try { if (preparedStatement != null) preparedStatement.close(); } catch (Exception e) { }
            try { if (connection != null) connection.close(); } catch (Exception e) { }
        }
    }
}
