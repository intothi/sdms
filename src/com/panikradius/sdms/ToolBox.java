package com.panikradius.sdms;

import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class ToolBox {

    public static long getCRC32Checksum(byte[] bytes) {
        Checksum crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);
        return crc32.getValue();
    }

    public static long getCRC32Checksum(String string) {
        Checksum crc32 = new CRC32();
        byte[] bytes = string.getBytes();
        crc32.update(bytes, 0, bytes.length);
        return crc32.getValue();
    }

    public static int getRndIntBetweenIncl(int low, int high){
        return new Random().nextInt((high + 1) -low) + low;
    }
}
