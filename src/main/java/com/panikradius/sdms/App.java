
package com.panikradius.sdms;

import com.panikradius.sdms.db.DbConnection;
import com.panikradius.sdms.db.TableCategory;
import com.panikradius.sdms.db.TableColor;
import com.panikradius.sdms.db.TableDocument;
import com.panikradius.sdms.db.TableDocumentTag;
import com.panikradius.sdms.db.TableLog;
import com.panikradius.sdms.db.TableScannerConfig;
import com.panikradius.sdms.db.TableTag;
import com.panikradius.sdms.db.TableTagKeyword;
import com.sun.net.httpserver.HttpServer;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.File;
import java.net.URI;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class App {

    public static final boolean devMode = true;
    public static final String projectName = "dms";
    public static final String pathToDms = System.getProperty("user.home") + "/sdms/";

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        dbTest();
        createEnvironment();

        System.out.println("init backend");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        ResourceConfig rc = new ResourceConfig().packages("com.panikradius.sdms.routes");
        rc.register(MultiPartFeature.class);

        rc.register(new CorsFilter());
        rc.register(org.glassfish.jersey.jackson.JacksonFeature.class);
        rc.register(org.glassfish.jersey.media.multipart.MultiPartFeature.class);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("jersey.config.server.wadl.disableWadl", "true");
        rc.setProperties(properties);

        // if we dont register JacksonFeature, Jackson wont work if we build the app
        // for some reason though its no problem at all to run it in the debugger of intelij, even if its not registered

        HttpServer server = JdkHttpServerFactory.createHttpServer(URI.create("http://localhost:8080/v1"), rc);
        System.out.println("backend is listening ...");

        // Fristen + Häckchen setzen können
        // Notifikation

    }

    private static void dbTest() {
        System.out.println("initialization");
        if (!com.panikradius.sdms.db.DbHelper.testDbConnection()) {
            System.exit(-1);
        }

        if (!com.panikradius.sdms.db.DbHelper.testCredentials()) {
            System.exit(-1);
            //TODO create com.panikradius.sdms.db user,password and privileges via java
        }
    }

    private static void createEnvironment() {
        System.out.println("create Environment...");
        try {
            buildTables();
            fillTables();
        } catch (Exception e) {
            System.out.println(e);
        }
        createDMSPath();
    }

    private static void fillTables() throws SQLException {
        System.out.println("filling tables");
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(
                    DbConnection.DB_URL,
                    DbConnection.USER,
                    DbConnection.PW);
            connection.setAutoCommit(false);

            TableScannerConfig.insertDefaultValues(connection);
            TableColor.insertDefaultColors(connection);
            TableCategory.insertDefaultCategories(connection);
            TableTag.insertDefaultTags(connection);

            connection.commit();
            System.out.println("finished filling tables");
        } catch (Exception e) {
            connection.rollback();
        }
    }

    private static void buildTables() {
        System.out.println("creating tables");
        TableLog.buildTable();
        TableScannerConfig.buildTable();
        TableColor.buildTable();
        TableCategory.buildTable();
        TableTag.buildTable();
        TableDocument.buildTable();
        TableDocumentTag.buildTable();
        TableTagKeyword.buildTable();
        System.out.println("finished creating tables");
    }

    private static void createDMSPath() {
        File dir = new File(pathToDms);
        if (!dir.exists()) {
            System.out.print("creating DMS home dir ... ");
            boolean created = dir.mkdirs();
            if (!created) {
                System.out.println("failed!");
            } else {
                System.out.println("success!");
            }
        }
    }
}
