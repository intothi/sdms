package com.panikradius.sdms.models;

import java.sql.Date;
import java.sql.Timestamp;

public class Tag {

    public int id;
    public String name;
    public String color;
    public Timestamp dateTimeCreated;

    // empty constructor is for Jackson deserialization
    public Tag(){}

    public Tag(int id, String name, String color, Timestamp dateTimeCreated) {

        this.id = id;
        this.name = name;
        this.color = color;
        this.dateTimeCreated = dateTimeCreated;
    }
}
