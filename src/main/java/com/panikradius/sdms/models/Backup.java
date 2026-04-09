package com.panikradius.sdms.models;

import java.sql.Timestamp;

public class Backup {

    public int id;
    public String filePath;
    public long fileSize;
    public Timestamp dateTimeCreated;
    public long timeCreation;
    public String createdBy;

    public Backup() {}

    public Backup(int id, String filePath, long fileSize,
                  Timestamp dateTimeCreated, long timeCreation, String createdBy) {

        this.id = id;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.dateTimeCreated = dateTimeCreated;
        this.timeCreation = timeCreation;
        this.createdBy = createdBy;
    }
}