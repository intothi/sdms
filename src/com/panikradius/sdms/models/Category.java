package com.panikradius.sdms.models;

import java.sql.Timestamp;

public class Category {

    public int id;
    public String name;
    public Timestamp dateTimeCreated;

    // empty constructor is for Jackson deserialization
    public Category(){}

    public Category(
            int id,
            String name,
            Timestamp dateTimeCreated) {

        this.id = id;
        this.name = name;
        this.dateTimeCreated = dateTimeCreated;
    }
}
