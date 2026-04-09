package com.panikradius.sdms.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.panikradius.sdms.models.BackupConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TableBackupConfig {

    public static TableConnectionInfo tableConnectionInfo = new TableConnectionInfo(
            DbConnection.DATABASE_NAME,
            "backup_config",
            DbConnection.USER,
            DbConnection.PW
    );

    public static String QUERY_CREATE = "CREATE TABLE " + tableConnectionInfo.tableName + " ("
            + "id INT UNSIGNED NOT NULL AUTO_INCREMENT, "
            + "backupIntervalDays INT NOT NULL, "
            + "backupPath VARCHAR(1023) NOT NULL, "
            + "backupTime TIME NOT NULL, "
            + "PRIMARY KEY (id)"
            + ")";

    public static void buildTable() {
        DbHelper.buildTable(tableConnectionInfo, QUERY_CREATE);
    }

    public static void insertDefaultValues(Connection connection) throws Exception {

        if (DbHelper.hasEntry(tableConnectionInfo)){ return;}

        String query = "INSERT INTO " + tableConnectionInfo.tableName
                + " (backupIntervalDays, backupPath, backupTime) "
                + "VALUES (?, ?, ?)";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, 1);
        preparedStatement.setString(2, "/home/pi");
        preparedStatement.setString(3, "03:00:00");
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public static BackupConfig get() throws Exception {
        Connection connection = DriverManager.getConnection(
                tableConnectionInfo.dbConnectionURL,
                tableConnectionInfo.user,
                tableConnectionInfo.pw);

        String query = "SELECT id, backupIntervalDays, backupPath, backupTime "
                + "FROM " + tableConnectionInfo.tableName
                + " LIMIT 1";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        ResultSet resultSet = preparedStatement.executeQuery();

        BackupConfig config = null;
        if (resultSet.next()) {
            config = new BackupConfig(
                    resultSet.getInt(1),
                    resultSet.getInt(2),
                    resultSet.getString(3),
                    resultSet.getString(4)
            );
        }

        try { resultSet.close(); } catch (Exception e) { /* Ignored */ }
        try { preparedStatement.close(); } catch (Exception e) { /* Ignored */ }
        try { connection.close(); } catch (Exception e) { /* Ignored */ }

        return config;
    }

    public static void update(BackupConfig config) throws Exception {
        Connection connection = DriverManager.getConnection(
                tableConnectionInfo.dbConnectionURL,
                tableConnectionInfo.user,
                tableConnectionInfo.pw);

        String query = "UPDATE " + tableConnectionInfo.tableName
                + " SET backupIntervalDays=?, backupPath=?, backupTime=? "
                + "WHERE id=?";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, config.backupIntervalDays);
        preparedStatement.setString(2, config.backupPath);
        preparedStatement.setString(3, config.backupTime);
        preparedStatement.setInt(4, config.id);
        preparedStatement.executeUpdate();

        try { preparedStatement.close(); } catch (Exception e) { /* Ignored */ }
        try { connection.close(); } catch (Exception e) { /* Ignored */ }
    }
}