package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DbHelper {

    private DbHelper(){}

    public static boolean testDbConnections() {
        System.out.print("testing connecting to maria-db ...");

        try {
            Class.forName(DbConnection.JDBC_DRIVER);
        } catch (Exception e) {
            System.out.print(" --> failed to load driver");
            return false;
        }

        Connection connection = null;

        try {
            connection = DriverManager.getConnection(DbConnection.DB_URL_DMS, DbConnection.USER_DMS, DbConnection.PW_DMS);
        } catch (Exception e) {
            System.out.print(" --> failed to establish database connection to: " + DbConnection.DB_URL_DMS);
            System.out.println(e);
            return false;
        } finally {
            try { connection.close(); } catch (Exception e) { /* Ignored */ }
        }

        System.out.println(" --> success");
        return true;
    }

    public static boolean tableExistsSQL(TableConnectionInfo tableConnectionInfo) throws SQLException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = DriverManager.getConnection(tableConnectionInfo.dbConnectionURL, tableConnectionInfo.user, tableConnectionInfo.pw);

            preparedStatement = connection.prepareStatement("SELECT count(*) "
                    + "FROM information_schema.tables "
                    + "WHERE table_name = ?"
                    + "LIMIT 1;");
            preparedStatement.setString(1, tableConnectionInfo.tableName);

            resultSet = preparedStatement.executeQuery();
            resultSet.next();
            return resultSet.getInt(1) != 0;

        } catch (Exception e) { /* Ignored */ return false;
        } finally {
            try { resultSet.close(); } catch (Exception e) { /* Ignored */ }
            try { preparedStatement.close(); } catch (Exception e) { /* Ignored */ }
            try { connection.close(); } catch (Exception e) { /* Ignored */ }
        }
    }

    public static boolean hasEntry(TableConnectionInfo tableConnectionInfo, String whereStatement) {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            connection = DriverManager.getConnection(tableConnectionInfo.dbConnectionURL, tableConnectionInfo.user, tableConnectionInfo.pw);
            statement = connection.createStatement();
            String query = "SELECT count(*) FROM " + tableConnectionInfo.tableName + " WHERE " + whereStatement + " LIMIT 1;";
            resultSet = statement.executeQuery(query);

            resultSet.first();
            return resultSet.getInt(1) != 0;

        } catch (Exception e) { /* Ignored */ return false;
        } finally {
            try { resultSet.close(); } catch (Exception e) { /* Ignored */ }
            try { statement.close(); } catch (Exception e) { /* Ignored */ }
            try { connection.close(); } catch (Exception e) { /* Ignored */ }
        }

    }

    public static void buildTable(TableConnectionInfo tableConnectionInfo, String queryCreate) {
        Connection connection = null;
        Statement statement = null;
        try {
            if (DbHelper.tableExistsSQL(tableConnectionInfo)) {
                System.out.println("skipping table -->" + tableConnectionInfo.tableName + "<-- reason: exists");
                return;
            }
            connection = DriverManager.getConnection(tableConnectionInfo.dbConnectionURL, tableConnectionInfo.user, tableConnectionInfo.pw);
            statement = connection.createStatement();
            statement.execute(queryCreate);
            System.out.println("table -->" + tableConnectionInfo.tableName + "<-- created");
            System.out.println(queryCreate);
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            try { statement.close(); } catch (Exception e) { /* Ignored */ }
            try { connection.close(); } catch (Exception e) { /* Ignored */ }
        }
    }
}
