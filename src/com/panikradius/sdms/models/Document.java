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
    public Timestamp dateTimeArchived;

    public Document(){}

    public Document(int id, String name, String comment, String filePath,
                    Date dateDocument, Timestamp dateTimeArchived) {

        this.id = id;
        this.name = name;
        this.comment = comment;
        this.filePath = filePath;
        this.dateDocument = dateDocument;
        this.dateTimeArchived = dateTimeArchived;
    }

}
