package com.panikradius.sdms.models;

import java.sql.Timestamp;

public class Tag {

    public int id;
    public String name;
    public int colorId;
    public int categoryId;
    public String color;
    public Timestamp dateTimeCreated;

    // empty constructor is for Jackson deserialization
    public Tag(){}

    public Tag(
            int id,
            String name,
            int colorId,
            int categoryId,
            String color,
            Timestamp dateTimeCreated) {

        this.id = id;
        this.name = name;
        this.colorId = colorId;
        this.categoryId = categoryId;
        this.color = color;
        this.dateTimeCreated = dateTimeCreated;
    }
}

