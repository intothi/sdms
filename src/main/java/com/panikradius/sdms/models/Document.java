package com.panikradius.sdms.models;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Document {

    public int id;
    public String fileName;
    public String comment;
    public Date dateDocument;
    public Date dueDate;
    public Integer parentId;
    public long fileSize;
    public int countPages;
    public int countWords;
    public String mimeType;
    public String ocrText;
    public String language;
    public String checksum;
    public boolean deskewDone;
    public Timestamp dateTimeArchived;
    public List<Tag> tags = new ArrayList<Tag>();

    // empty constructor is for Jackson deserialization
    public Document(){}

    public Document(
            int id,
            String fileName,
            String comment,
            Date dateDocument,
            Date dueDate,
            Timestamp dateTimeArchived) {

        this.id = id;
        this.fileName = fileName;
        this.comment = comment;
        this.dateDocument = dateDocument;
        this.dueDate = dueDate;
        this.dateTimeArchived = dateTimeArchived;
    }

    public Document(
            String fileName,
            String comment,
            Date dateDocument,
            Date dueDate,
            Timestamp dateTimeArchived) {

        this.fileName = fileName;
        this.comment = comment;
        this.dateDocument = dateDocument;
        this.dueDate = dueDate;
        this.dateTimeArchived = dateTimeArchived;
    }

}
