
package com.panikradius.sdms;

import com.panikradius.sdms.db.TableDocument;
import com.panikradius.sdms.db.TableLog;

import java.io.File;
import java.sql.SQLException;
import java.util.TimeZone;

public class App {

    public static final String projectName = "dms";
    private static String pathToDms = System.getProperty("user.home") + "/dms";

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        dbTest();
        createEnvironment();
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
