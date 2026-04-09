package com.panikradius.sdms.models;

public class BackupConfig {

    public int id;
    public int backupIntervalDays;
    public String backupPath;
    public String backupTime;

    public BackupConfig() {}

    public BackupConfig(int id, int backupIntervalDays, String backupPath, String backupTime) {
        this.id = id;
        this.backupIntervalDays = backupIntervalDays;
        this.backupPath = backupPath;
        this.backupTime = backupTime;
    }
}