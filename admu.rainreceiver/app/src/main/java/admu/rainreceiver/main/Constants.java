package admu.rainreceiver.main;

import android.os.Environment;

import java.io.File;

/**
 * Created by JPTalusan on 16/10/2016.
 */
public class Constants {
    public static final int MSG_REGISTER_CLIENT = 0;
    public static final int MSG_UNREGISTER_CLIENT = 1;
    public static final int MSG_SET_ROWS_BUFFER = 2;
    public static final int MSG_SET_RECEIVED_RAIN1 = 3;
    public static final int MSG_SET_SAVED_RAIN1_SERVER1 = 4;
    public static final int MSG_SET_SAVED_RAIN1_SERVER2 = 5;

    public static final int MSG_SET_RECEIVED_RAIN2 = 6;
    public static final int MSG_SET_SAVED_RAIN2_SERVER1 = 7;
    public static final int MSG_SET_SAVED_RAIN2_SERVER2 = 8;

    public static final int MSG_SET_RECEIVED_RAIN3 = 9;
    public static final int MSG_SET_SAVED_RAIN3_SERVER1 = 10;
    public static final int MSG_SET_SAVED_RAIN3_SERVER2 = 11;

    public static final int MSG_SET_RECEIVED_RAIN4 = 12;
    public static final int MSG_SET_SAVED_RAIN4_SERVER1 = 13;
    public static final int MSG_SET_SAVED_RAIN4_SERVER2 = 14;

    public static final int MSG_SET_RECEIVED_RAIN5 = 15;
    public static final int MSG_SET_SAVED_RAIN5_SERVER1 = 16;
    public static final int MSG_SET_SAVED_RAIN5_SERVER2 = 17;

    public static final int MSG_SET_RECEIVED_RAIN6 = 18;
    public static final int MSG_SET_SAVED_RAIN6_SERVER1 = 19;
    public static final int MSG_SET_SAVED_RAIN6_SERVER2 = 20;

    public static final int MSG_SET_RECEIVED_RAIN7 = 21;
    public static final int MSG_SET_SAVED_RAIN7_SERVER1 = 22;
    public static final int MSG_SET_SAVED_RAIN7_SERVER2 = 23;

    public static final int MSG_SET_RECEIVED_RAIN8 = 24;
    public static final int MSG_SET_SAVED_RAIN8_SERVER1 = 25;
    public static final int MSG_SET_SAVED_RAIN8_SERVER2 = 26;

    public static final int LOG_INTERVAL = 5 * 1000; // in milliseconds
    public static final String SMS_EXTRA_NAME = "pdus";
    public static final String directoryName = "rainsensorproject-rx";
    public static String directory = "/" + directoryName + "/";
    public static File SDLINK = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + directory);

    public static String INSERT_PHP = "insert.php";
    public static String SHARED_PREFS = "receiver";

    public static String SENSOR1 = "sensor1";
    public static String SENSOR2 = "sensor2";
    public static String SENSOR3 = "sensor3";
    public static String SENSOR4 = "sensor4";
    public static String SENSOR5 = "sensor5";
    public static String SENSOR6 = "sensor6";
    public static String SENSOR7 = "sensor7";
    public static String SENSOR8 = "sensor8";
    public static String SERVER1 = "server1";
    public static String MONITOR = "monitor";

    public static String TRUNCATEBUFFER = "truncatebuffer";

}
