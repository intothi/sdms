package com.panikradius.sdms.models;

import java.sql.Timestamp;

public class TagKeyword {

    public int id;
    public int tagId;
    public String keyword;
    public Timestamp dateTimeCreated;

    public TagKeyword(){};

    public TagKeyword(
            int id,
            int tagId,
            String keyword,
            Timestamp dateTimeCreated)
    {
        this.id = id;
        this.tagId = tagId;
        this.keyword = keyword;
        this.dateTimeCreated = dateTimeCreated;
    };

}
