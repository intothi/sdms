package com.panikradius.sdms.models;

import java.sql.Timestamp;

public class Log {

    public int id;
    public String msg;
    public LogLevel level;
    public Timestamp logDate;

    public Log(int id, String msg, LogLevel level, Timestamp logDate){
        this.id = id;
        this.msg = msg;
        this.level = level;
        this.logDate = logDate;
    }

    public enum LogLevel {
        FATAL,
        ERROR,
        WARN,
        INFO
    }
}