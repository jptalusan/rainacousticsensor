package admu.raintransmitter.main;

import android.os.Environment;

import java.io.File;

/**
 * Created by talusan on 10/14/2016.
 */
public class Constants {
    public static final int MSG_REGISTER_CLIENT = 0;
    public static final int MSG_UNREGISTER_CLIENT = 1;
    public static final int MSG_SET_TEST = 2;
    public static final int MSG_SET_STATUS_ON = 3;
    public static final int MSG_SET_STATUS_OFF = 4;
    public static final int MSG_SET_MODE_GSM = 5;
    public static final int MSG_SET_MODE_WIFI = 6;
    public static final int MSG_SET_MODE_TEST = 7;
    public static final int SAMPLER_INTERVAL = 1000; // in milliseconds
    public static final int LOGGER_INTERVAL = 25000; // in milliseconds

    // create storage elements
    public static String directory = "/rainsensorproject/";
    public static File SDLINK = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + directory);
    public static String WAKELOCK = "My wakelock";

    public static String SHARED_PREFS = "SHARED_PREFS";
    // for SMART private String serverNumber ="", monitorNumber = "";
    // for GLOBE
    public static String MONITOR_NUM_KEY = "MONITOR_NUM";
    public static String serverNumber ="+639059716422";
    public static String monitorNumber = "+639059716422";
    public static double THRESHOLD_START = -20.0f;
    public static double THRESHOLD_STEP = 1.0f;
    public static int SAMPLES = 120;
    // to be changed for every phone
    public static String SENSOR = "RT1";
    public static String wifi = "WIFI";
    public static String gsm = "GSM";
    public static String test = "TEST";
    static String logFile = "log.txt";
}
