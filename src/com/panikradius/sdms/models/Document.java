package com.panikradius.sdms.models;

import java.sql.Date;
import java.sql.Timestamp;

public class Document {

    public int id;
    public String name;
    public String comment;
    public String filePath;

    //TODO Filesize
    public Date dateDocument;
    public Timestamp dateTimeCreated;

    // empty constructor is for Jackson deserialization
    public Document(){}

    public Document(int id, String name, String comment, String filePath,
                    Date dateDocument, Timestamp dateTimeCreated) {

        this.id = id;
        this.name = name;
        this.comment = comment;
        this.filePath = filePath;
        this.dateDocument = dateDocument;
        this.dateTimeCreated = dateTimeCreated;
    }

}
