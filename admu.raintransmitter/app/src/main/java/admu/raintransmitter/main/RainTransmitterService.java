package admu.raintransmitter.main;
//Rain|Reco|SQL
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;

public class RainTransmitterService extends Service {
    private static final String TAG = "Rain Tx Service";
    ArrayList<Messenger> mClients = new ArrayList<>(); // Keeps track of all current registered clients.
    final Messenger mMessenger = new Messenger(new IncomingHandler()); // Target we publish for clients to send messages to IncomingHandler.

    private PowerManager pm;
    private PowerManager.WakeLock wakeLock;

    private int signalStrength = -1;
    private boolean isRecording = false;
    private boolean isWaitingToStart = false;
    private boolean isPowerProblem = false;
    private String start_time = null;
    private double[] sound = null;
    private int position = 0;
    // timer for logging and sampling
    private Timer samplerTimer = null ;
    private Timer loggerTimer = null ;

    private SignalStrengthListener mySSListener = new SignalStrengthListener();
    private TelephonyManager telephonyManager;
    private Intent batteryStatus = null;
    private SMSBroadcastReceiver mySMS = new SMSBroadcastReceiver();

    private SQLiteBackup backup = null;
    private SQLiteBuffer buffer = null;

    // detection parameters
    private RecorderThread recorderThread;

    //Acoustic file parameters
    private String audioFileName;
    private String audioData;
    private File audioLogFile;
    private FileWriter audioLog;
    private long currentTime;
    private long stopTime;
    private String controllerNumber = "";
    private String serverReceiverNumber = "";
    private String serverUrl = "";

    private static final DecimalFormat ftRmsDb = new DecimalFormat("0.00");
    private static final SimpleDateFormat hms = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
    private static final SimpleDateFormat hm = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
    private static final SimpleDateFormat m = new SimpleDateFormat("mm", Locale.ENGLISH);

    private Thread recordingThread = null;
    private File file = null;
    private FileOutputStream os = null;
    private Timer iterateLoggers = null;
    private long timeToStart = 0;
    private int numberOfSamples = 0;

    private boolean isAt50Percent = false;
    private boolean isAt25Percent = false;

    private String transmitterId = "";

    private boolean isFirstTaskRunning = false;
    private boolean isSecondTaskRunning = false;

    private ArrayList<Double> ambientSoundArr = null;
    private Timer ambientLevelTimer = null;

    private Timer threeHourTextIfRecordingTimer = null;

    private long currentDataTimeStamp = 0;

    private float threshold = 0.0f;

    private boolean isServiceStarted = false;

    @Override
    public void onCreate() {
        super.onCreate();

        File dir =new File(android.os.Environment.getExternalStorageDirectory(),Constants.directoryName);
        if(!dir.exists())
        {
            dir.mkdirs();
        }
        String filename= "logger.txt";
        File f = new File(dir+File.separator+filename);
        try
        {
            FileOutputStream fOut = new FileOutputStream(f);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(
                    fOut);
            Calendar c = Calendar.getInstance();
            System.out.println("Current time => "+c.getTime());

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedDate = df.format(c.getTime());

            myOutWriter.append("Data for Current Date and Time : " +formattedDate);
            myOutWriter.append("\n");
            myOutWriter.close();
            fOut.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Constants.WAKELOCK);

        // create a folder for the record
        Log.d(TAG, "Filepath: " + Constants.SDLINK);
        File bufferfile = new File(Constants.SDLINK + "/buffer.sqlite");
        buffer = new SQLiteBuffer(bufferfile.getPath());
        File backupfile = new File(Constants.SDLINK + "/backup.sqlite");
        backup = new SQLiteBackup(backupfile.getPath());

        // register SMS receiver
        IntentFilter filter = new IntentFilter( "android.provider.Telephony.SMS_RECEIVED" );
        filter.setPriority( IntentFilter.SYSTEM_HIGH_PRIORITY);
        registerReceiver(mySMS, filter);

        // register signal strength & call listener
        telephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        telephonyManager.listen(mySSListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        wakeLock.acquire();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        wakeLock.release();
        if (samplerTimer != null)
            stopSamplerTimer();
        if (loggerTimer != null)
            stopLoggerTimer();
        stopIterateLoggerTimer();
        telephonyManager.listen(mySSListener, PhoneStateListener.LISTEN_NONE);
        recorderThread.stopRecording();
        recorderThread.stop();
        backup.closeDatabase();
        buffer.closeDatabase();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @SuppressLint("HandlerLeak")
    class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case Constants.MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Updates the UI textview in the main activity
     * @param object
     * @param message
     */
    private void sendMessageToUI(int object, String message) {
        for (int i = mClients.size() - 1; i >= 0; --i) {
            try {
                //Send data as a String
                Bundle b = new Bundle();
                b.putString("str", message);
                Message msg = Message.obtain(null, object);
                msg.setData(b);
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                // The client is dead. Remove it from the list.
                mClients.remove(i);
            }
        }
    }

    /**
     * Sends SMS to number in buffer row: String number, String message, String priority
     * deletes the row from the buffer once sent.
     * This only sends information regarding sound and signal when GSM is set.
     * @param row Buffer row: [0] id, [1] String number, [2] String message, [3] String priority
     */
    private void sendSMS(final String[] row) {
        Log.d(TAG, "sendSMS: " + row[1] + " " + row[2]);
        String SENT = "SMS_SENT";
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);
        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Log.d("EXTRA", "Deleting row: " + row[0] + ", prio: " + row[3]);
                        buffer.deleteRow(Integer.parseInt(row[0]));
                        break;
                }
                unregisterReceiver(this);
            }
        }, new IntentFilter(SENT));

        SmsManager sms = SmsManager.getDefault();
        //Loop this for data (since many rows) get all rows for 18 values
        sms.sendTextMessage(row[1], null, row[2], sentPI, null);
    }

    //TODO: For 18 data points
    /*
    * Format of text message:
    * #RT5;1500645432;1.88;1.98;13.02;4.26;7.74;2.75;14.98;12.47;-0.87;11.67;5.59;5.55;1.93;4.35;2.66;0.62;1.48;2.49#
    */
    private void sendDataSMS(List<String[]> data) {
        Log.d(TAG, "SendDataSMS: " + data.size());
        String SENT = "SMS_SENT";
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);
        //---when the SMS has been sent---
        final List<String[]> fData = data;
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        for (String[] sArr : fData) {
                            Log.d("EXTRA", "Deleting row: " + sArr[0] + ", prio: " + sArr[3]);
                            buffer.deleteRow(Integer.parseInt(sArr[0]));
                        }
                        break;
                }
                unregisterReceiver(this);
            }
        }, new IntentFilter(SENT));

        Pair<String, String> dataToSend = assembleRows(data);
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(dataToSend.first, null, dataToSend.second, sentPI, null);
    }

    private Pair<String, String> assembleRows(List<String[]> data) {
        //Loop this for data (since many rows) get all rows for 18 values
        //Init data is the first row's message in the form of: #RT5;1500645432;1.88;#
        String initData = data.get(0)[2];
        StringBuilder newMsg = new StringBuilder();
        newMsg.append(initData.substring(0, initData.length() - 1));

        String prefix = "";
        //Just get the sound data from the row
        for (int i = 1; i < data.size(); ++i) {
            newMsg.append(prefix);
            prefix = ";";
            String[] parsedTemp = data.get(i)[2].split(";");
            newMsg.append(parsedTemp[2]);
        }
        newMsg.append("#");

        Log.d(TAG, "NewTxt:" + newMsg);
        String destinationAddress = data.get(0)[1];
        Log.d("EXTRA", "Start:" + data.get(0)[0] + "," + destinationAddress + "," + data.get(0)[2]);

        return new Pair<>(destinationAddress, newMsg.toString());
    }

    private class SignalStrengthListener extends PhoneStateListener {
        @Override
        public void onSignalStrengthsChanged(android.telephony.SignalStrength signalSt) {
            // get the signal strength (a value between 0 and 31) & convert it in dBm
            signalStrength = -113 + (2 * signalSt.getGsmSignalStrength());
            super.onSignalStrengthsChanged(signalSt);
        }
    }

    /**
     * Returns battery level
     * TODO: Must handle the possible null pointer exceptions
     * @return
     */
    private int getBatteryLevel() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryStatus = this.registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return (int) (((float)level / (float)scale) * 100);
    }

    /**
     * Ensures that the start_time will only be 2 minutes away from initialization
     */
    public void InitializeTime() {
        timeToStart = System.currentTimeMillis() + Constants.THIRTY_SECONDS;
        Log.d(TAG, "Start time: " + convertMillisToTimeFormat(timeToStart));
        isWaitingToStart = true;
        recordingThread.start();
        Toast.makeText(getApplicationContext(), convertMillisToTimeFormat(timeToStart), Toast.LENGTH_LONG).show();
    }

    private String setupName() {
        return String.valueOf(System.currentTimeMillis() / 1000);
    }

    private long setupDate() {
        return System.currentTimeMillis() / 1000;
        //return Long.toString(timeInSeconds);
    }

    private String convertMillisToTimeFormat(long timeInMillis) {

        Date date = new Date(timeInMillis);
        DateFormat formatter = DateFormat.getTimeInstance();
        return formatter.format(date);
    }

    /**
     * Analyses the received data from the SMS and provides the needed process. When GSM, sampler timer is
     * selected startSamplerTimer(), when WIFI, a continuous timer is selected startModeTimer().
     * @param data In the following format [Start, Stop, Truncate]-[GSM, WIFI, Buffer, Backup, Test]
     */
    @SuppressLint("UnlocalizedSms")
    public void messageAnalysis(String[] data) {
        Log.i (TAG, "Starting messageAnalysis()");
        if (data[1].toLowerCase().equals(Constants.gsm)) {
            startLoggerTimer();
            startSamplerTimer();
            InitializeTime();
            sendMessageToUI(Constants.MSG_SET_MODE_GSM, "");

            sound = new double[Constants.SAMPLES];
//            signal = new double[Constants.SAMPLES];
            position = 0;
            buffer.insertRow(controllerNumber, (transmitterId + " here, I'll start recording at : " + convertMillisToTimeFormat(timeToStart)), "1");
        }

        if (data[1].equals(Constants.wifi)) {
            startLoggerTimer();
            startModeTimer();
            InitializeTime();
            sendMessageToUI(Constants.MSG_SET_MODE_WIFI, "");
            buffer.insertRow(controllerNumber, (transmitterId + " here, I'll start recording at : " + convertMillisToTimeFormat(timeToStart)), "1");
        }
        if (data[1].equals(Constants.test)) {
            startLoggerTimer();
            startModeTimer();
            InitializeTime();
            sendMessageToUI(Constants.MSG_SET_MODE_TEST, "");
            buffer.insertRow(controllerNumber, (transmitterId + " here, I'll start recording at : " + convertMillisToTimeFormat(timeToStart)), "1");
        }
    }

    /**
     * Starts a logger timer that lasts for Constants.LOGGER_INTERVAL that starts immediately.
     * This sends rows on the buffer to the monitor mobile device (depending on priority)
     * Also checks battery (should be a different timer i think)
     */
    //TODO: should have the data to be sent in a different priority? so that it would check if there are more than 18 of them then attempt to send all 18
    //then delete all 18 rows after
    //Else if only notification send them immediately and then delete.
    //Add capability to check if connected to net and then send via Wifi/Mobile if possible (text is last resort)
    public void startLoggerTimer() {
        loggerTimer = null;
        loggerTimer = new Timer();
        loggerTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // check if they are some sms to send
                if (buffer.getNumberRows("0") > 0) {
                    sendSMS(buffer.getFirstRow("0"));
                } else if (buffer.getNumberRows("1") > 0) {
                    sendSMS(buffer.getFirstRow("1"));
                } else if (buffer.getNumberRows("2") >= 18) {
                    //Where the data is being sent
                    //Check first if net is available
                    if (Utilities.isDeviceConnected(getApplicationContext())) {
                        try {
                            postRainData(buffer.getXNumberOfDataPoints(18));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        sendDataSMS(buffer.getXNumberOfDataPoints(18));
                    }
                }

                //TODO: Do i try and send this as text? this will only get deleted if successfully sent
                if (buffer.getNumberRows("4") > 0 &&
                        Utilities.isDeviceConnected(getApplicationContext())) { //hack for all received texts
                    Log.d(TAG, "Getting: " + buffer.getNumberRows("4") + " rows of prio: 4");
                    try {
                        postTextData(buffer.getFirstRow("4"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    //no need to send sms received texts (just to server)
                } else {
                    Log.d(TAG, "No data connection or no prio 4 rows.");
                }

                // check battery
                int bat = getBatteryLevel();
                if (bat == 90) {
                    isPowerProblem = false;
                }
                if (bat == 50 && !isAt50Percent) {
                    buffer.insertRow(controllerNumber, transmitterId + " here, battery at 50%. Please come check on me!", "0");
                    isAt50Percent = true;
                }
                if (bat == 25 && !isAt25Percent) {
                    buffer.insertRow(controllerNumber, transmitterId + " here, battery at 25%. Please come check on me!", "0");
                    isAt25Percent = true;
                    isPowerProblem = true;
                }
                if (bat == 10 && isPowerProblem) {
                    buffer.insertRow(controllerNumber, transmitterId + " here, battery at 10%. Please come check on me!", "0");
                    isPowerProblem = false;
                }
            }
        }, 0, Constants.LOGGER_INTERVAL);
    }

    /**
     * Starts the recording of RecorderThread
     * get sound and signal data and save to csv file
     * Why is this only for WIFI and not GSM?
     */
    public void  startModeTimer() {
        samplerTimer = new Timer();
        //Adding buffersize
        audioFileName = setupName() + "Audio-" + recorderThread.getRecBufSize();
        audioLogFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + audioFileName + ".csv");
        try {
            audioLogFile.createNewFile();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create " + audioLog.toString());
        }
        samplerTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isRecording) {
                    try {
                        while (currentTime < stopTime + 1) {
                            double snd = 0;
                            int sig = signalStrength;
                            audioData = setupDate() + "," + Double.toString(snd) + ",dB," + Integer.toString(sig) + ",dBm";
                            Log.w(TAG, "Signal Level:" + Integer.toString(sig));
                            audioLog = new FileWriter(audioLogFile, true);
                            audioLog.write(audioData + "\r\n");
                            currentTime = System.currentTimeMillis();
                        }
                        currentTime = System.currentTimeMillis();
                        stopTime = System.currentTimeMillis();
                        audioLog.flush();
                        audioLog.close();
                    } catch (Throwable t) {
                        // TODO: handle exception
                        Log.e(TAG, "WIFI Recording Failed " + t.toString());
                    }
                }

                if (isWaitingToStart) {
                    if (start_time.equals(hms.format(new Date()))) {
                        Log.d(TAG, "Starting");
                        recorderThread = new RecorderThread();
                        recorderThread.start();
                        isRecording = true;
                        isWaitingToStart = false;
                        sendMessageToUI(Constants.MSG_SET_STATUS_ON, "");
                    }
                }
            }
        }, 0, Constants.SAMPLER_INTERVAL);
    }

    /**
     * Why is this the only one with process and Send? Why GSM only not WIFI?
     * This saves data received to both buffer and backup
     */
    //TODO: This gets the sound level power 10 times a second (10Hz)
    public void startSamplerTimer(){
        recordingThread = new Thread(new Runnable() {
            public void run() {
                captureAudioDataThread();
            }
        }, "AudioRecorder Thread");
    }

    private void captureAudioDataThread() {
        Log.d(TAG, "captureAudioDataThread started...");
        while (isWaitingToStart) {
            if (System.currentTimeMillis() > timeToStart) {
                Log.d(TAG, "Starting capturing now...");

                //TODO: Refactor other timers to be the same as this recorder thread class
                recorderThread = new RecorderThread();
                recorderThread.start();

                //Creating audioLogFile
                audioLogFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
                        + setupName() + "-Tx-" + recorderThread.getRecBufSize() + ".csv");
                try {
                    if(audioLogFile.createNewFile())
                        audioLog = new FileWriter(audioLogFile, true);
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to create " + audioLog.toString());
                }

                //TODO: Fix this
//                int numberOfSamples = Utilities.computeNumberOfSamplesPerText(Constants.sampleRate, recorderThread.getRecBufSize());
                numberOfSamples = Constants.SAMPLES_PER_SECOND;
//                Log.d(TAG, "Number of samples per sec: " + numberOfSamples);

                isWaitingToStart = false;

                //TODO: is this still needed?
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                sendMessageToUI(Constants.MSG_SET_STATUS_ON, "");
            }
        }

        //Start threehourtimer to text every now and then.
        threeHourTextIfRecordingTimer = new Timer();
        threeHourTextIfRecordingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isServiceStarted) {
                    Log.d(TAG, "I'm still alive..., sending to: " + controllerNumber);
                    buffer.insertRow(controllerNumber, transmitterId + " here, I'm still alive don't worry.", "1");
                }
            }
        }, Constants.THREE_HOURS, Constants.THREE_HOURS);

        //Start ambient level thread here
        ambientLevelTimer = new Timer();
        ambientLevelTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "Starting ambient audio recording tasks...");
                isRecording = true;
                if (!isFirstTaskRunning && !isSecondTaskRunning) {
                    getAmbientSoundLevel();
                }
            }
        }, 0, Constants.AMBIENT_AUDIO_RECORDING_TIME + Constants.DATA_AUDIO_RECORDING_TIME + Constants.EXTRA_BUFFER_TIME);
    }

    public void stopSamplerTimer() {
        samplerTimer.cancel();
        samplerTimer.purge();
        samplerTimer = null;
    }

    public void stopLoggerTimer() {
        loggerTimer.cancel();
        loggerTimer.purge();
        loggerTimer = null;
    }

    public void stopIterateLoggerTimer() {
        if (iterateLoggers != null) {
            iterateLoggers.cancel();
            iterateLoggers.purge();
            iterateLoggers = null;
        }
    }

    public void stopAmbientLevelTimer() {
        if (ambientLevelTimer != null) {
            ambientLevelTimer.cancel();
            ambientLevelTimer.purge();
            ambientLevelTimer = null;
        }
    }

    public void stopThreeHourTimer() {
        if (threeHourTextIfRecordingTimer != null) {
            threeHourTextIfRecordingTimer.cancel();
            threeHourTextIfRecordingTimer.purge();
            threeHourTextIfRecordingTimer = null;
        }
    }

    /**
     * Saves the sound and signal arguments into both backup and buffer sqliteDBs
     * This doesn't really send anything, only saves to DB
     * TODO: Should send 18 data points
     * buffer receives the ff msg format: servernumber, msg (concatenated), priority (2)
     * backup receives the ff msg format: # + SENSOR.NUM + sound[1]
     * @param soundLevel
     */
    //Problem here is that sometimes data comes in too fast and timestamps are duplicated
    //I think better to just increment by 1 sec here just to be safe, every time this is called just store the time stamp to currentDataTimestamp on first run
    //each data has a duration of 1 sec
    public void processAndSend(double soundLevel) {
        // SEND //
        String msg = "#" + transmitterId + ";";
        msg += currentDataTimeStamp + ";";
        msg += ftRmsDb.format(soundLevel) + ";#";

        Log.d("EXTRA", "processAndSend(): Server,msg,priority: " + serverReceiverNumber + "," + msg + ",2");
        buffer.insertRow(serverReceiverNumber, msg, "2");
        backup.insertRow(msg);
    }

    /**
     * SMSBroadCastReceiver
     * Must register a broadcast receiver for SMS since this is the trigger for turning
     * on the data gathering of the device.
     * then save it to a shared prefs for future use and just display it there for verification.
     */
    //TODO: to preserve data integrity, delete buffer on start if it only has less than 18 data points? since 18 seconds is negligible when you have to "restart" the app anyway.
    public class SMSBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            controllerNumber = getApplicationContext()
                    .getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE)
                    .getString(Constants.MONITOR_NUM_KEY, "");
            serverReceiverNumber = getApplicationContext()
                    .getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE)
                    .getString(Constants.SERVER_NUM_KEY, "");
            transmitterId = getApplicationContext()
                    .getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE)
                    .getString(Constants.TRANSMITTER_ID_KEY, "");
            threshold = getApplicationContext()
                    .getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE)
                    .getFloat(Constants.THRESHOLD_KEY, 0.0f);
            serverUrl = getApplicationContext()
                    .getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE)
                    .getString(Constants.SERVER_URL_KEY, "");

            Log.d(TAG, "Threshold: " + threshold);
            Log.d(TAG, "Transmitter: " + transmitterId);
            Log.d(TAG, "Server URL:" + serverUrl);

            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                // here is what I need, just combine them all  :-)
                final SmsMessage[] messages = new SmsMessage[pdus.length];
                for (int i = 0; i < pdus.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    String body = messages[i].getMessageBody();
                    String number = messages[i].getOriginatingAddress();
                    Log.i(TAG,"Received SMS: " + number + ":" + body);
                    Log.i(TAG,"Controller No." + controllerNumber);

                    String[] data = body.split("-");
                    if (data.length <= 0) {
                        this.abortBroadcast();
                    }

                    //Saving received messages to buffer with priority 1 for sending to server (start truncates)
                    //Prio 4 is received text messages
                    if (!body.equals("start-gsm")) {
                        buffer.insertRow(number, transmitterId + ";" + setupName() + ";" + body, "4");
                    }

                    if (number.contains(controllerNumber) && data[0].toLowerCase().equals("start")) {
                        //TODO: Make sure multiple starts won't cause this to fail
                        if (!isWaitingToStart) {
                            isServiceStarted = true;

                            //TODO: Why truncate table?, so start GSM won't be saved from above, have to add hack
                            buffer.truncateTable();
                            buffer.insertRow(number, transmitterId + ";" + setupName() + ";" + body, "4"); //hack

                            SimpleDateFormat ft = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
                            buffer.insertRow(controllerNumber, (transmitterId + " here, time is " + ft.format(new Date()) + ", started recording."), "1");
                            messageAnalysis(data);
                            this.abortBroadcast();
                        } else {
                            Log.w(TAG, "Already sent start message, please wait, or send stop before starting again.");
                        }
                    }
                    if (number.contains(controllerNumber) && data[0].toLowerCase().equals("stop")) {
                        buffer.insertRow(controllerNumber, (transmitterId + " here, stopped recording."), "1");
                        Log.i(TAG, "Stopping..");

                        isServiceStarted = false;
                        isRecording = false;
                        isWaitingToStart = false;
                        if (recorderThread != null)
                            recorderThread.stopRecording();
                        if (recordingThread != null &&
                                recordingThread.isAlive()) {
                            recordingThread.interrupt();
                        }
                        stopIterateLoggerTimer();
                        stopAmbientLevelTimer();
                        stopThreeHourTimer();

                        try {
                            if (os != null) {
                                os.close();
                            }
                            if (audioLog != null) {
                                audioLog.flush();
                                audioLog.close();
                            }
                        } catch (IOException f) {
                            f.printStackTrace();
                        }

                        isFirstTaskRunning = false;
                        isSecondTaskRunning = false;
                        currentDataTimeStamp = 0;
                        sendMessageToUI(Constants.MSG_SET_STATUS_OFF, "");
                        this.abortBroadcast();
                    }
                    if (number.contains(controllerNumber) && data[0].toLowerCase().equals("truncate")) {
                        if (data[1].toLowerCase().equals("buffer")) {
                            buffer.truncateTable();
                            buffer.insertRow(controllerNumber, (transmitterId + " here, resetting buffer table."), "1");
                            this.abortBroadcast();
                        }
                        if (data[1].toLowerCase().equals("backup")) {
                            backup.truncateTable();
                            buffer.insertRow(controllerNumber, (transmitterId + " here, resetting backup table."), "1");
                            this.abortBroadcast();
                        }
                    }
                    //Promo texting "promo-CODE-NUMBER"
                    if (number.contains(controllerNumber) && data[0].toLowerCase().equals("promo")) {
                        if (data.length == 3) {
                            Log.d(TAG, "Preparing to text: " + data[1] + " to " + data[2]);
                            buffer.insertRow(data[2], data[1], "0");
                        } else {
                            buffer.insertRow(controllerNumber, (transmitterId + " here, you sent incorrect promo syntax. [promo-code-number]."), "0");
                        }
//                        messageAnalysis(data);
                    }
                    else {
                        this.abortBroadcast();
                    }
                    //Texting to modify threshold values (thresh-number)
                    if (number.contains(controllerNumber) && data[0].toLowerCase().equals("thresh")) {
                        if (data.length != 2) {
                            Log.d(TAG, "Adjusting threshold to: " + data[1]);
                            buffer.insertRow(controllerNumber, (transmitterId + " here, you sent incorrect threshold syntax. [thresh-number]."), "0");
//                            buffer.insertRow(data[2], data[1], "0");
                        } else {
                            if (data[1].matches(regExp)) {
                                float newThreshold = Float.parseFloat(data[1]);

                                getApplicationContext()
                                        .getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE)
                                        .edit()
                                        .putFloat(Constants.THRESHOLD_KEY, newThreshold)
                                        .commit();

                                threshold = getApplicationContext()
                                        .getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE)
                                        .getFloat(Constants.THRESHOLD_KEY, 0.0f);

                                buffer.insertRow(controllerNumber, (transmitterId + " here, adjusting threshold to: " + threshold), "0");

                                //Updates threshold edit view
                                sendMessageToUI(Constants.MSG_SET_UPDATE_THRESHOLD, data[1]);
                            } else {
                                buffer.insertRow(controllerNumber, (transmitterId + " here, you sent incorrect threshold syntax, please use FLOAT. [thresh-number]."), "0");
                            }
                        }
//                        messageAnalysis(data);
                    }
                    else {
                        this.abortBroadcast();
                    }
                }
            }
        }
    }

    String regExp = "[\\x00-\\x20]*[+-]?(((((\\p{Digit}+)(\\.)?((\\p{Digit}+)?)([eE][+-]?(\\p{Digit}+))?)|(\\.((\\p{Digit}+))([eE][+-]?(\\p{Digit}+))?)|(((0[xX](\\p{XDigit}+)(\\.)?)|(0[xX](\\p{XDigit}+)?(\\.)(\\p{XDigit}+)))[pP][+-]?(\\p{Digit}+)))[fFdD]?))[\\x00-\\x20]*";
//    boolean matches = yourString.matches(regExp);

    //TODO: Clarify if they want 15 samples or 15 seconds, does not seem to be possible, if 15 sec, i get 13 samples, if 15 samples, it takes 17 sec
    private void getAmbientSoundLevel() {
        long startTime = System.currentTimeMillis();
        isFirstTaskRunning = true;
        ambientSoundArr = new ArrayList<>();
        sound = new double[numberOfSamples];
        while ((
                (startTime + Constants.AMBIENT_AUDIO_RECORDING_TIME >  System.currentTimeMillis()) //either these 3
                && isRecording
                && !isSecondTaskRunning)
                || ambientSoundArr.size() < Constants.NUMBER_OF_SAMPLES_FOR_AMBIENCE) //or this one
        {
            //Data gathering, right now not recording to file
            byte sData[] = new byte[Constants.frameByteSize];
            // gets the voice output from microphone to byte format
            if (recorderThread.audioRecord == null) {
                return;
            }
            int readSize = recorderThread.audioRecord.read(sData, 0, Constants.frameByteSize);
            //TODO: Add array here to limit amount of captured data if it the sampling rate is too high
            double out5 = Utilities.getPower(sData, readSize);
            if (position < numberOfSamples) {
                sound[position] = out5;
                position++;
            } else if (position == numberOfSamples) {
                double sum = 0.0;
                for (int i = 0; i < numberOfSamples; ++i) {
                    sum += sound[i];
                }
                double ave = sum / numberOfSamples;
                ambientSoundArr.add(ave);
                position = 0;
                sound = new double[numberOfSamples];
            }
        } //End of while loop

        Log.d(TAG, "Ending recording of ambient sound...");
        isFirstTaskRunning = false;

        //TODO:move this to else once i figure out how to record ambient sound while sound recording to minimize gaps
        currentDataTimeStamp = 0;

        //Check ambient sound levels
        if (isAmbientSoundLevelHigherThanThreshold(ambientSoundArr)) {
            //Clear up data in arr
            ambientSoundArr.clear();
            ambientSoundArr = null;
            Log.d(TAG, "Threshold met...");
            if (!isSecondTaskRunning) {
                recordAudioForAnalysis();
            }
        } else {
            long time = System.currentTimeMillis() + Constants.DATA_AUDIO_RECORDING_TIME + Constants.EXTRA_BUFFER_TIME;
            Log.d(TAG, "Threshold not met, will record again in " + convertMillisToTimeFormat(time) +"...");
            isRecording = false; //if less than threshold, turn off
        }
    }

    private void recordAudioForAnalysis() {
        Log.d(TAG, "Starting recording of audio for analysis...");
        long startTime = System.currentTimeMillis();
        isSecondTaskRunning = true;

        //Logging
        audioFileName = setupName() + "-Tx";
        Log.d(TAG, "iterateLoggingPCMFiles()" + convertMillisToTimeFormat(System.currentTimeMillis()));
        file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + audioFileName + ".pcm");

        try {
            os = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        sound = new double[numberOfSamples];
        //Loop
        //TODO: Fix race condition, this may finish without the last N seconds or samples have been written to sound[pos]
        while ((startTime + Constants.DATA_AUDIO_RECORDING_TIME >  System.currentTimeMillis())
                && isRecording
                && !isFirstTaskRunning)
        {
            //Data gathering
            byte sData[] = new byte[Constants.frameByteSize];

            // gets the voice output from microphone to byte format
            int readSize = recorderThread.audioRecord.read(sData, 0, Constants.frameByteSize);
            try {
                //TODO: Add array here to limit amount of captured data if it the sampling rate is too high
                double out5 = Utilities.getPower(sData, readSize);
                String audioData = setupDate() + "," + Utilities.roundDown(out5, 3);
                if (position < numberOfSamples) {
                    sound[position] = out5;
                    position++;
                } else if (position == numberOfSamples) {
                    double sum = 0.0;
                    for (int i = 0; i < numberOfSamples; ++i) {
                        sum += sound[i];
                    }
                    final double rawAverage = sum / numberOfSamples;
                    Log.d("EXTRA", "rawAverage: " + rawAverage);
                    //On first run, currentDataTimeStamp = 0, return to 0 on stop-gsm and when ambient is lower than threshold
                    //To be more accurate with the times
                    if (currentDataTimeStamp == 0) {
                        currentDataTimeStamp = setupDate();
                    } else {
                        currentDataTimeStamp++;
                    }
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            processAndSend(Utilities.roundDown(rawAverage, 3));
                        }
                    }).start();
                    position = 0;
                    sound = new double[numberOfSamples];
                }

                //TODO: Build on this as a way to record ambience before data rec is finished
                long timeLeft = startTime + Constants.DATA_AUDIO_RECORDING_TIME - Constants.AMBIENT_AUDIO_RECORDING_TIME;
                if (System.currentTimeMillis() > timeLeft) {
                    Log.d("TRIAL", "Time left for ambience:" + System.currentTimeMillis());
                }
                audioLog.write(audioData + "\r\n");
                os.write(sData, 0, Constants.frameByteSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } //End of while loop

        //File closing PCM
        try {
            if (os != null)
                os.close();
        } catch (IOException f) {
            f.printStackTrace();
        }

        os = null;

        isRecording = false;
        isSecondTaskRunning = false;
        Log.d(TAG, "Ending recording of audio for analysis...");
    }

    //If more than 80% of ambient sound values are greater than threshold, then record data
    private boolean isAmbientSoundLevelHigherThanThreshold(ArrayList<Double> ambientSound) {
        Log.d(TAG, "isAmbientSoundLevelHigherThanThreshold: " + ambientSound.size());
        int counter = 0;
        for (double d : ambientSound) {
            Log.d(TAG, "compare: " + d + ": " + threshold);
            if (d >= threshold) {
                ++counter;
            }
        }
        Log.d(TAG, "counter: " + counter + "/" + Math.ceil(ambientSound.size() * Constants.SIXTY_FIVE_PERCENT));
        //TODO: not really exact with the number of samples, just how many where sent here.
        //Target is 10/15 thats why weird 0.65, 15 * 0.65 = 9.~ -> 10
        return counter > Math.ceil(ambientSound.size() * Constants.SIXTY_FIVE_PERCENT);
//        return false;//debug
    }

    public void postRainData(List<String[]> data) throws JSONException {
        //Assemble data here
        Pair<String, String> dataToSend = assembleRows(data);
        final List<String[]> fData = data;
        final RequestParams params = new RequestParams("data", dataToSend.second);
        Handler asyncHttpPost = new Handler(Looper.getMainLooper());
        //server url should be = http://rainsensor.excthackathon.x10host.com
        asyncHttpPost.post(new Runnable() {
            @Override
            public void run() {
                CustomHttpClient.post(serverUrl, params, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        Log.d(TAG, "postRainData:" + response.toString());
                        try {
                            String result = response.getString("result");
                            if (result.toLowerCase().equals("success")) {
                                for (String[] sArr: fData) {
                                    Log.d("EXTRA", "Deleting row: " + sArr[0]);
                                    buffer.deleteRow(Integer.parseInt(sArr[0]));
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        });
    }

    public void postTextData(String[] data) throws JSONException {
        //data[0] - id
        //data[1] - phone number
        //data[2] - message --> id;timestamp;body
        //data[3] - priority
        final String id = data[0];
        final RequestParams params = new RequestParams();

        final String[] messages = data[2].split(";");
        params.put("transmitterId", messages[0]);
        params.put("phoneNumber", data[1]);
        params.put("message", data[2]);
        Handler asyncHttpPost = new Handler(Looper.getMainLooper());
        //server url should be = http://rainsensor.excthackathon.x10host.com
        asyncHttpPost.post(new Runnable() {
            @Override
            public void run() {
                CustomHttpClientForTexts.post(serverUrl, params, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        Log.d(TAG, "postTextData:" + response.toString());
                        try {
                            String result = response.getString("result");
                            if (result.toLowerCase().equals("success")) {
                                Log.d("EXTRA", "Deleting row: " + id + ", prio: 4");
                                buffer.deleteRow(Integer.parseInt(id));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        });
    }
}