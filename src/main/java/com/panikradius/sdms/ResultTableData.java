package com.panikradius.sdms;

import java.sql.ResultSet;

public class ResultTableData {

    public ResultSet resultSet;
    public int count;

    public ResultTableData(ResultSet resultSet, int count) {
        this.resultSet = resultSet;
        this.count = count;
    }
}
