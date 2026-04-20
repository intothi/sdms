package com.panikradius.sdms.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomeStats {

    public int totalDocuments;
    public long totalSize;
    public List<Map<String, Object>> dueSoon;
    public long diskTotal;
    public long diskUsed;
    public long diskFree;
    public long ramTotal;
    public long ramUsed;
    public long ramFree;
    public long jvmUsed;
    public long jvmTotal;
    public String lastBackup;

    public HomeStats(){
        dueSoon = new ArrayList<>();
    }
}
