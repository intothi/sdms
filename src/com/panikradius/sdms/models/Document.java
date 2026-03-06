package com.panikradius.sdms.models;

import java.sql.Date;
import java.sql.Timestamp;

public class Document {

    public int id;
    public String name;
    public String comment;

    //TODO Filesize
    public Date dateDocument;
    public Timestamp dateTimeArchived;

    // empty constructor is for Jackson deserialization
    public Document(){}

    public Document(
            int id,
            String name,
            String comment,
            Date dateDocument,
            Timestamp dateTimeArchived) {

        this.id = id;
        this.name = name;
        this.comment = comment;
        this.dateDocument = dateDocument;
        this.dateTimeArchived = dateTimeArchived;
    }

}
