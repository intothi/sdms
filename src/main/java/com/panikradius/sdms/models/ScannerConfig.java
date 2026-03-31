package com.panikradius.sdms.models;


public class ScannerConfig {

    public int id;
    public int resolution;
    public String colorMode;
    public String scanMode;
    public int swSkip;
    public float pageWidth;
    public float pageHeight;


    public ScannerConfig(){}

    public ScannerConfig(
            int id,
            int resolution,
            String colorMode,
            String scanMode,
            int swSkip,
            float pageWidth,
            float pageHeight
    ) {
        this.id = id;
        this.resolution = resolution;
        this.colorMode = colorMode;
        this.scanMode = scanMode;
        this.swSkip = swSkip;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;

    }
}
