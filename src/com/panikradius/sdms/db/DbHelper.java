package com.panikradius.sdms.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panikradius.sdms.ResultTableData;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class DbHelper {

    private DbHelper(){}

    public static boolean testDbConnection() {
        System.out.print("testing connecting to maria-com.panikradius.sdms.db ...");

        try {
            Class.forName(DbConnection.JDBC_DRIVER);
        } catch (Exception e) {
            System.out.print(" --> failed to load driver");
            return false;
        }

        System.out.println(" --> success");
        return true;
    }

    public static boolean testCredentials() {
        System.out.print("testing credentials to DB ...");

        Connection connection = null;

        try {
            connection = DriverManager.getConnection(
                    DbConnection.DB_URL,
                    DbConnection.USER,
                    DbConnection.PW);

        } catch (Exception e) {
            System.out.print(" --> failed to login with DB credentials" + DbConnection.DB_URL);
            System.out.println(e);
            return false;
        } finally {
            try { connection.close(); } catch (Exception e) { /* Ignored */ }
        }

        return true;
    }

    public static boolean tableExistsSQL(TableConnectionInfo tableConnectionInfo) throws SQLException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

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
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

            String query = "SELECT count(*) FROM " + tableConnectionInfo.tableName + " WHERE " + whereStatement + " LIMIT 1;";

            statement = connection.createStatement();
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
            connection = DriverManager.getConnection(
                    tableConnectionInfo.dbConnectionURL,
                    tableConnectionInfo.user,
                    tableConnectionInfo.pw);

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

    public static String getById(TableConnectionInfo tableConnectionInfo, int id) throws JsonProcessingException {
        Connection connection = null;
        Statement statement = null;
        //Product product = null;
        // TODO(CT) It makes no sense to me, to create an instance of product, in an general helper class

        try {
            connection = DriverManager.getConnection(tableConnectionInfo.dbConnectionURL, tableConnectionInfo.user, tableConnectionInfo.pw);
            statement = connection.createStatement();
            String query = "SELECT * FROM " + tableConnectionInfo.tableName + " WHERE id = " + id + " LIMIT 1";
            ResultSet resultSet = statement.executeQuery(query);

            if (resultSet.first()){
//                product = new Product(
//                        Integer.parseInt(resultSet.getString(1)),
//                        resultSet.getString(2),
//                        resultSet.getString(3),
//                        Timestamp.valueOf(resultSet.getString(4)),
//                        Date.valueOf(resultSet.getString(5))
//                );
            }

        } catch (Exception e) {
            System.out.println(e);
        } finally {
            try { statement.close(); } catch (Exception e) { /* Ignored */ }
            try { connection.close(); } catch (Exception e) { /* Ignored */ }
        }
        //return new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(product);
        return "";
    }

    public static void deleteById(TableConnectionInfo tableConnectionInfo, int id) {
        Connection connection = null;
        Statement statement = null;

        try {
            connection = DriverManager.getConnection(tableConnectionInfo.dbConnectionURL, tableConnectionInfo.user, tableConnectionInfo.pw);
            statement = connection.createStatement();
            String query = "DELETE FROM " + tableConnectionInfo.tableName + " WHERE id = " + id;
            statement.executeQuery(query);
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            try { statement.close(); } catch (Exception e) { /* Ignored */ }
            try { connection.close(); } catch (Exception e) { /* Ignored */ }
        }
    }

    public static ResultTableData getTableResultSet(TableConnectionInfo tableConnectionInfo, int skip, int top, String orderby) throws JsonProcessingException {
        Connection connection = null;
        Statement statement = null;


        int totalCount = 0;
        try {
            connection = DriverManager.getConnection(tableConnectionInfo.dbConnectionURL, tableConnectionInfo.user, tableConnectionInfo.pw);
            statement = connection.createStatement();

            String query = "SELECT COUNT(*) FROM " + tableConnectionInfo.tableName;
            ResultSet resultSet = statement.executeQuery(query);

            if (resultSet.first()) {
                totalCount = Integer.parseInt(resultSet.getString(1));
            }

            if (totalCount == 0) {
                return null;
            }

            if (!orderby.isEmpty()){
                StringBuilder str = new StringBuilder();
                str.append(" ");
                str.append(orderby);
                orderby = str.toString();
            }

            query = "SELECT * FROM " + tableConnectionInfo.tableName + orderby + " LIMIT " + skip + "," + top;

            return new ResultTableData(statement.executeQuery(query), totalCount);

        } catch (Exception e) {
            System.out.println(e);
        } finally {
            try { statement.close(); } catch (Exception e) { /* Ignored */ }
            try { connection.close(); } catch (Exception e) { /* Ignored */ }
        }
        return null;
    }





}
