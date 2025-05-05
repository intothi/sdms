import db.TableDocument;

import java.io.File;
import java.sql.SQLException;
import java.util.TimeZone;

public class App {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        createEnvironment();
    }

    private static void createEnvironment(){
        System.out.println("create Environment...");

        if (!db.DbHelper.testDbConnections()) {
            System.exit(-1);
        }
        try { buildTables(); } catch (Exception e) {System.out.println(e);}

        String pathToDms = System.getProperty("user.home") + "/dms";

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

    private static void buildTables() throws SQLException {
        System.out.println("creating tables");

        TableDocument.buildTable();

        System.out.println("finished creating tables");
    }


}
