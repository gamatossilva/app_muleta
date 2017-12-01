package com.example.guilhermea.bluetooth_33_final;


public final class Constants {
    public enum Type{
        INTERNAL, EXTERNAL;
    }

    public static final String STORAGE_TYPE = "storage_type";
    public static final String FILE_NAME = "arquivo_%s.txt";
    public static final String RECORDED_FILE = "file name";

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    private Constants (){

    }
}
