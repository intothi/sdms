package com.panikradius.sdms;

import com.panikradius.sdms.db.TableLog;

public class Logger {

    public static void log(String msg, LogLevel level) {
        Log log = new Log(
                0,
                msg,
                level,
                new java.sql.Timestamp(System.currentTimeMillis())
        );

        TableLog.postPreparedStatement(log);
    }
}
