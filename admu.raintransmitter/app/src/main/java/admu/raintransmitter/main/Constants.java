package admu.raintransmitter.main;

import android.media.AudioFormat;
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
    public static String directoryName = "rainsensorproject-tx";
    public static String directory = "/" + directoryName + "/";
    public static File SDLINK = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + directory);
    public static String WAKELOCK = "My wakelock";

    public static String SHARED_PREFS = "SHARED_PREFS";
    public static String MONITOR_NUM_KEY = "MONITOR_NUM";
    public static String SERVER_NUM_KEY = "SERVER_NUM";
    public static String TRANSMITTER_ID_KEY = "TRANSMITTER_ID";
    public static String THRESHOLD_KEY = "THRESHOLD";
    public static int SAMPLES = 120;

    // to be changed for every phone
    public static String SENSOR = "RT1";
    public static String wifi = "wifi";
    public static String gsm = "gsm";
    public static String test = "test";
    static String logFile = "log.txt";

    // Recorder Thread
    static int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    static int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    static int sampleRate = 44100;//44100 for actual; //8000 for emulator;
    static int frameByteSize = 2048; // for 1024 fft size (16bit sample size)

    static int SAMPLES_PER_SECOND = 25; //8 for emulator 8000, 25 for 44100

    static int DEFAULT_LOGGER_DURATION = 600000;
    static int THIRTY_SECONDS = 30000;

    static int AMBIENT_AUDIO_RECORDING_TIME = 15000;//15000; //15 seconds
    static int DATA_AUDIO_RECORDING_TIME = 300000;//300000; //5 minutes
    static int THREE_HOURS = 10800000; //10800000; 60000 for test

    static double EIGHTY_PERCENT = 0.8;
}
