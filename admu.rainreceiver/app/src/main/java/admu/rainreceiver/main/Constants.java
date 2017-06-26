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
    public static final int LOG_INTERVAL = 5 * 1000; // in milliseconds
    public static final String SMS_EXTRA_NAME = "pdus";
    public static String directory = "/rainsensorproject/";
    public static File sdLink = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + Constants.directory);

    public static String INSERT_PHP = "insert.php";
    public static String SHARED_PREFS = "receiver";

    public static String SENSOR1 = "sensor1";
    public static String SENSOR2 = "sensor2";
    public static String SENSOR3 = "sensor3";
    public static String SERVER1 = "server1";
    public static String MONITOR = "monitor";

    public static String TRUNCATEBUFFER = "truncatebuffer";

}
