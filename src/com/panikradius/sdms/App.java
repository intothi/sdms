
package com.panikradius.sdms;

import com.panikradius.sdms.db.TableDocument;
import com.panikradius.sdms.db.TableLog;
import com.panikradius.sdms.db.TableTag;
import com.sun.net.httpserver.HttpServer;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.File;
import java.net.URI;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class App {

    public static final String projectName = "dms";
    private static String pathToDms = System.getProperty("user.home") + "/dms";

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        dbTest();
        createEnvironment();

        System.out.println("init backend");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        ResourceConfig rc = new ResourceConfig().packages("com.panikradius.sdms.routes");
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

    private static void createEnvironment(){
        System.out.println("create Environment...");
        try { buildTables(); } catch (Exception e) {System.out.println(e);}
        createDMSPath();
    }

    private static void buildTables() throws SQLException {
        System.out.println("creating tables");
        TableDocument.buildTable();
        TableLog.buildTable();
        TableTag.buildTable();
        System.out.println("finished creating tables");
    }

    private static void createDMSPath() {
        File dir = new File(pathToDms);
        if (!dir.exists()){
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
