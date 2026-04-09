package com.panikradius.sdms;

import com.panikradius.sdms.db.DbConnection;
import com.panikradius.sdms.db.TableBackup;
import com.panikradius.sdms.db.TableDocument;
import com.panikradius.sdms.models.Backup;
import com.panikradius.sdms.models.BackupConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import com.panikradius.sdms.models.Log;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

public class BackupService {

    public static final String CREATED_BY_CRONJOB = "cronjob";
    public static final String CREATED_BY_USER = "user";

    public static byte[] createBackup(BackupConfig config, String createdBy) throws Exception {
        long timeStart = System.currentTimeMillis();

        byte[] dbDump = createDbDump();
        byte[] archive = createArchive(dbDump);

        long timeCreation = System.currentTimeMillis() - timeStart;

        if (createdBy.equals(CREATED_BY_CRONJOB)) {
            saveBackupToDisk(archive, config);
        }

        logBackup(archive.length, createdBy, timeCreation, config, createdBy.equals(CREATED_BY_CRONJOB));

        return archive;
    }

    private static byte[] createDbDump() throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "mysqldump",
                "-u", DbConnection.USER,
                "-p" + DbConnection.PW,
                DbConnection.DATABASE_NAME
        );
        processBuilder.redirectErrorStream(false);
        Process process = processBuilder.start();

        //byte[] dump = process.getInputStream().readAllBytes();

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = process.getInputStream().read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        byte[] dump = buffer.toByteArray();

        process.waitFor();
        return dump;
    }

    private static byte[] createArchive(byte[] dbDump) throws Exception {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(byteArrayOutputStream);
        TarArchiveOutputStream tar = new TarArchiveOutputStream(gzip);
        tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

        // DB Dump hinzufügen
        TarArchiveEntry dumpEntry = new TarArchiveEntry("sdms_dump.sql");
        dumpEntry.setSize(dbDump.length);
        tar.putArchiveEntry(dumpEntry);
        tar.write(dbDump);
        tar.closeArchiveEntry();

        List<com.panikradius.sdms.models.Document> documents = TableDocument.getAll();
        // Dokumente hinzufügen
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        for (int i = 0; i < documents.size(); i++) {
            com.panikradius.sdms.models.Document doc = documents.get(i);
            File file = new File(App.pathToDms + doc.fileName);

            if (!file.exists()) {
                Logger.log("Backup: Datei nicht gefunden --> " + doc.fileName, com.panikradius.sdms.models.Log.LogLevel.ERROR);
                continue;
            }

            byte[] fileBytes = Files.readAllBytes(file.toPath());

            // Checksum prüfen
            digest.reset();
            byte[] hash = digest.digest(fileBytes);
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < hash.length; j++) {
                sb.append(String.format("%02x", hash[j]));
            }
            String checksum = sb.toString();

            if (!checksum.equals(doc.checksum)) {
                Logger.log("Backup: Checksum fehlgeschlagen --> " + doc.fileName, com.panikradius.sdms.models.Log.LogLevel.ERROR);
                continue;
            }

            TarArchiveEntry entry = new TarArchiveEntry("documents/" + doc.fileName);
            entry.setSize(fileBytes.length);
            tar.putArchiveEntry(entry);
            tar.write(fileBytes);
            tar.closeArchiveEntry();
        }

        tar.finish();
        tar.close();
        gzip.close();

        return byteArrayOutputStream.toByteArray();
    }

    private static void saveBackupToDisk(byte[] archive, BackupConfig config) throws Exception {
        File backupDir = new File(config.backupPath + "/sdmsBackup");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        String timestamp = new java.text.SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());
        String fileName = "sdms_backup_" + timestamp + ".tar.gz";
        Files.write(Paths.get(backupDir.getAbsolutePath() + "/" + fileName), archive);
    }

    private static void logBackup(long fileSize, String createdBy, long timeCreation, BackupConfig config, boolean savedToDisk) throws Exception {
        String filePath = savedToDisk
                ? config.backupPath + "/sdmsBackup"
                : "stream";

        Backup backup = new Backup(
                0,
                filePath,
                fileSize,
                new Timestamp(System.currentTimeMillis()),
                timeCreation,
                createdBy
        );

        Connection connection = DriverManager.getConnection(
                DbConnection.DB_URL,
                DbConnection.USER,
                DbConnection.PW);
        connection.setAutoCommit(false);
        TableBackup.post(connection, backup);
        connection.commit();
        try { connection.close(); } catch (Exception e) { /* Ignored */ }
    }

    public static void updateCronJob(BackupConfig config) throws Exception {
        String cronExpression = parseToCronExpression(config.backupTime);
        String cronLine = cronExpression + " curl -s -X POST http://localhost:8080/v1/backup/trigger";

        // Aktuellen Crontab auslesen
        ProcessBuilder pbRead = new ProcessBuilder("crontab", "-l");
        pbRead.redirectErrorStream(false);
        Process processRead = pbRead.start();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = processRead.getInputStream().read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        int readExitCode = processRead.waitFor();
        String currentCrontab = readExitCode == 0 ? buffer.toString() : "";

        // SDMS-Zeile entfernen falls vorhanden
        StringBuilder newCrontab = new StringBuilder();
        String[] lines = currentCrontab.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].contains("backup/trigger")) {
                newCrontab.append(lines[i]).append("\n");
            }
        }
        newCrontab.append(cronLine).append("\n");

        Logger.log("updateCronJob new crontab: " + newCrontab.toString(), Log.LogLevel.INFO);
        // Neuen Crontab schreiben
        ProcessBuilder pbWrite = new ProcessBuilder("crontab", "-");
        pbWrite.redirectErrorStream(true);
        Process processWrite = pbWrite.start();
        processWrite.getOutputStream().write(newCrontab.toString().getBytes());
        processWrite.getOutputStream().close();
        processWrite.waitFor();
        Logger.log("updateCronJob exit code: " + processWrite.exitValue(), Log.LogLevel.INFO);
    }

    private static String parseToCronExpression(String backupTime) {
        String[] parts = backupTime.split(":");
        String hour = parts[0];
        String minute = parts[1];
        return minute + " " + hour + " * * *";
    }
}