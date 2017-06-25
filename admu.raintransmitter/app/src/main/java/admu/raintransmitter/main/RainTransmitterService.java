package admu.raintransmitter.main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class RainTransmitterService extends Service {

    ArrayList<Messenger> mClients = new ArrayList<>(); // Keeps track of all current registered clients.
    final Messenger mMessenger = new Messenger(new IncomingHandler()); // Target we publish for clients to send messages to IncomingHandler.

    private PowerManager pm;
    private PowerManager.WakeLock wakeLock;

    private int signalStrength = -1;
    private boolean isRecording, isWaitingToStart, isPowerProblem;
    private String start_time = null;
    private double[] sound = new double[Constants.SAMPLES];
    private double[] signal = new double[Constants.SAMPLES];
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
    private String monitorNumber = "";
    private String serverNumber = "";

    private static final DecimalFormat ftRmsDb = new DecimalFormat("0.00");
    private static final SimpleDateFormat hms = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
    private static final SimpleDateFormat hm = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
    private static final SimpleDateFormat m = new SimpleDateFormat("mm", Locale.ENGLISH);

    @Override
    public void onCreate() {
        super.onCreate();

//        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE);
//        monitorNumber = sharedPref.getString(Constants.MONITOR_NUM_KEY, "");
//        serverNumber = sharedPref.getString(Constants.SERVER_NUM_KEY, "");

        pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Constants.WAKELOCK);

        // create a folder for the record
        File bufferfile = new File(Constants.SDLINK + "/buffer.sqlite");
        try {
            buffer = new SQLiteBuffer(bufferfile.getPath());
        } catch (Exception ex) {
            buffer.db = SQLiteDatabase.openDatabase("/storage/sdcard0/rainsensorproject/buffer.sqlite", null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        }
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
        if (samplerTimer != null) stopSamplerTimer();
        if (loggerTimer != null) stopLoggerTimer();
        telephonyManager.listen(mySSListener, PhoneStateListener.LISTEN_NONE);
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
        String SENT = "SMS_SENT";
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);
        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        //Toast.makeText(getBaseContext(), "SMS sent", Toast.LENGTH_SHORT).show();
                        buffer.deleteRow(Integer.parseInt(row[0]));
                        break;
                }
                unregisterReceiver(this);
            }
        }, new IntentFilter(SENT));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(row[1], null, row[2], sentPI, null);
    }

    private class SignalStrengthListener extends PhoneStateListener {
        @Override
        public void onSignalStrengthsChanged(android.telephony.SignalStrength signalSt) {
            // get the signal strength (a value between 0 and 31) & convert it in dBm
            signalStrength = -113 + 2*signalSt.getGsmSignalStrength();
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
        if (Integer.parseInt(m.format(new Date()).substring(1,2)) < 2)
            start_time = hm.format(new Date()).substring(0,4) + "2:00";
        else if (Integer.parseInt(m.format(new Date()).substring(1,2)) < 4)
            start_time = hm.format(new Date()).substring(0,4) + "4:00";
        else if (Integer.parseInt(m.format(new Date()).substring(1,2)) < 6)
            start_time = hm.format(new Date()).substring(0,4) + "6:00";
        else if (Integer.parseInt(m.format(new Date()).substring(1,2)) < 8)
            start_time = hm.format(new Date()).substring(0,4) + "8:00";
        else {
            int test = Integer.parseInt(m.format(new Date()).substring(0,1));
            if (test == 5)
                start_time = String.valueOf(Integer.parseInt(hm.format(new Date()).substring(0,2)) + 1) + ":00:00";
            else
                start_time = hm.format(new Date()).substring(0,3) + String.valueOf(test + 1) + "0:00";
        }
        Toast.makeText(getApplicationContext(), start_time, Toast.LENGTH_LONG).show();
        isWaitingToStart = true;
    }

    private String setupName() {
        String fileName;
        fileName = String.valueOf(System.currentTimeMillis());
        return fileName;
    }

    private String setupDate () {
        String sFileNameTemp;
        DateFormat dfMyTime = DateFormat.getTimeInstance(DateFormat.MEDIUM);
        DateFormat dfMyDate= DateFormat.getDateInstance(DateFormat.SHORT);
        Date dMyDate = new Date(System.currentTimeMillis());
        sFileNameTemp = " " + dfMyDate.format(dMyDate) + " , " + dfMyTime.format(dMyDate);
        return sFileNameTemp;
    }

    /**
     * Analyses the received data from the SMS and provides the needed process. When GSM, sampler timer is
     * selected startSamplerTimer(), when WIFI, a continuous timer is selected startModeTimer().
     * @param data In the following format [Start, Stop, Truncate]-[GSM, WIFI, Buffer, Backup, Test]
     */
    @SuppressLint("UnlocalizedSms")
    public void messageAnalysis(String[] data) {
        if (data[1].equals(Constants.gsm)) {
            startLoggerTimer();
            startSamplerTimer();
            InitializeTime();
            sendMessageToUI(Constants.MSG_SET_MODE_GSM, "");

            sound = new double[Constants.SAMPLES];
            signal = new double[Constants.SAMPLES];
            position = 0;
            buffer.insertRow(monitorNumber, (Constants.SENSOR + " here, I'll start recording at : " + start_time), "1");
        }

        if (data[1].equals(Constants.wifi)) {
            startLoggerTimer();
            startModeTimer();
            InitializeTime();
            sendMessageToUI(Constants.MSG_SET_MODE_WIFI, "");
            buffer.insertRow(monitorNumber, (Constants.SENSOR + " here, I'll start recording at : " + start_time), "1");
        }
        if (data[1].equals(Constants.test)) {
            startLoggerTimer();
            startModeTimer();
            InitializeTime();
            sendMessageToUI(Constants.MSG_SET_MODE_TEST, "");
            buffer.insertRow(monitorNumber, (Constants.SENSOR + " here, I'll start recording at : " + start_time), "1");
        }
    }

    /**
     * Starts a logger timer that lasts for Constants.LOGGER_INTERVAL that starts immediately.
     * This sends rows on the buffer to the monitor mobile device (depending on priority)
     * Also checks battery (should be a different timer i think)
     */
    public void startLoggerTimer() {
        loggerTimer = null;
        loggerTimer = new Timer();
        loggerTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // check if they are some sms to send
                if (buffer.getNumberRows("0") > 0)
                    sendSMS(buffer.getFirstRow("0"));
                else if (buffer.getNumberRows("1") > 0)
                    sendSMS(buffer.getFirstRow("1"));
                else if (buffer.getNumberRows("2") > 0)
                    sendSMS(buffer.getFirstRow("2"));
                // check battery
                int bat = getBatteryLevel();
                if (bat == 90)
                    isPowerProblem = false;
                if (bat == 50)
                    isPowerProblem = true;
                if (bat == 10 && isPowerProblem) {
                    buffer.insertRow(monitorNumber, Constants.SENSOR + " here, my battery is 10% so there is a problem with my power system. Please come check on me! Thanks :)", "0");
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
        samplerTimer = null;
        samplerTimer = new Timer();
        audioFileName = setupName() + "Audio";
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
                            snd = recorderThread.getPower();
                            int sig = signalStrength;
                            audioData = setupDate() + "," + Double.toString(snd) + ",dB," + Integer.toString(sig) + ",dBm";
                            Log.w("rain316", "Signal Level:" + Integer.toString(sig));
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
                        Log.e("AudioRecord", "Recording Failed");
                    }
                }

                if (isWaitingToStart) {
                    if (start_time.equals(hms.format(new Date()))) {
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
     * Why is this the only one with process and Send? Why not WIFI?
     * This saves data received to both buffer and backup
     */
    public void startSamplerTimer(){
        samplerTimer = null;
        samplerTimer = new Timer();
        samplerTimer.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run() {
                if (isRecording) {
                    if (position < Constants.SAMPLES) {
                        sound[position] = recorderThread.getPower();
                        signal[position] = signalStrength;
                        position++;
                    }
                    if (position == Constants.SAMPLES) {
                        final double[] snd = sound;
                        final double[] sig = signal;
                        position = 0;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                processAndSend(snd, sig);
                            }
                        }).start();
                    }
                }
                if (isWaitingToStart) {
                    if (start_time.equals(hms.format(new Date()))) {
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

    /**
     * Saves the sound and signal arguments into both backup and buffer sqliteDBs
     * This doesn't really send anything, only saves to DB
     * TODO: Should be in it's own class
     * TODO: Figure out what to is being done with the server number
     * buffer receives the ff msg format: servernumber, msg (concatenated), priority (2)
     * backup receives the ff msg format: # + SENSOR.NUM + sound[5] + signal[5] + distribution of sound[10]
     * @param sound
     * @param signal
     */
    public void processAndSend(double[] sound, double[] signal) {
        // PROCESS
        double[] snd = new double[6];
        double[] sig = new double[6];
        int[] dis = new int[10];

        // sound and signal
        int pos = 0;
        int POS = 0;
        while (POS < 6) {
            double snd_sum = 0;
            double sig_sum = 0;
            for (int i = 0; i < 20; i++) {
                snd_sum += sound[pos];
                sig_sum += signal[pos];
                pos++;
            }
            snd[POS] = snd_sum / 20;
            sig[POS] = sig_sum / 20;
            POS++;
        }

        // distribution of sound
        for (int i = 0; i < 10; i++) {
            double k = Constants.THRESHOLD_START + i * Constants.THRESHOLD_STEP;
            for (int j = 0; j < Constants.SAMPLES; j++) {
                if (sound[j] >=  k) {
                    dis[i]++;
                }
            }
        }

        // SEND //
        SimpleDateFormat ft =  new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        String msg = "#" + Constants.SENSOR + ";";
        msg += ft.format(new Date()) + ";";
        for (int i = 0; i < 5; i++) {
            msg += (ftRmsDb.format(snd[i]) + ",");
        }

        msg += ftRmsDb.format(snd[5]) + ";";

        for (int i = 0; i < 5; i++) {
            msg += (ftRmsDb.format(sig[i]) + ",");
        }

        msg += ftRmsDb.format(sig[5]) + ";";

        for (int i = 0; i < 9; i++) {
            msg += (String.valueOf(dis[i]) + ",");
        }
        msg += (String.valueOf(dis[9]) + ";#");

        Log.d("RainSensor", msg);
        buffer.insertRow(serverNumber, msg, "2");
        backup.insertRow(msg);
    }

    /**
     * Display msg on UI
     * @param msg Message to display in UI thread when SMS is received.
     */
    private void display(String msg) {
        final String msgStr = msg;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                Toast.makeText(RainTransmitterService.this, msgStr, Toast.LENGTH_SHORT).show();
                super.onPostExecute(aVoid);
            }
        }.execute();

        return;
    }

    /*
     * SMSBroadCastReceiver
     * Must register a broadcast receiver for SMS since this is the trigger for turning
     * on the data gathering of the device.
     * TODO: Right now number for "monitor" device is hard coded, must add an edit text instead
     * then save it to a shared prefs for future use and just display it there for verification.
     */
    public class SMSBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            monitorNumber = getApplicationContext()
                    .getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE)
                    .getString(Constants.MONITOR_NUM_KEY, "");
            serverNumber = getApplicationContext()
                    .getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE)
                    .getString(Constants.SERVER_NUM_KEY, "");
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                // here is what I need, just combine them all  :-)
                final SmsMessage[] messages = new SmsMessage[pdus.length];
                for (int i = 0; i < pdus.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    String body = messages[i].getMessageBody();
                    String number = messages[i].getOriginatingAddress();
                    Log.d("Received SMS", number + ":" + body);
                    Log.d("Monitor No." , monitorNumber);
                    // check the msg
//                    //DEBUGGING
//                    body = "Start-WIFI";
//                    number = monitorNumber;
//                    //END DEBUG
                    String[] data = body.split("-");
                    if (data.length <= 0) {
                        this.abortBroadcast();
                    }
                    if (number.equals(monitorNumber) && data[0].toLowerCase().equals("start")) {
                        display("Processing SMS...");
                        buffer.truncateTable();
                        SimpleDateFormat ft =  new SimpleDateFormat ("HH:mm:ss", Locale.ENGLISH);
                        buffer.insertRow(monitorNumber, (Constants.SENSOR + " here, time is " + ft.format(new Date()) + ", my transmitter application is now running :)"), "1");
                        messageAnalysis(data);
                        this.abortBroadcast();
                    }
                    if (number.equals(monitorNumber) && data[0].toLowerCase().equals("stop")) {
                        display("Processing SMS...");
                        buffer.insertRow(monitorNumber, (Constants.SENSOR + " here, I don't record anymore :)"), "1");
                        isRecording = false;
                        isWaitingToStart = false;
                        recorderThread.stopRecording();
                        sendMessageToUI(Constants.MSG_SET_STATUS_OFF, "");
                        this.abortBroadcast();
                    }
                    if (data[0].equals("TRUNCATE") || data[0].toLowerCase().equals("truncate")) {
                        if (data[1].equals("BUFFER") || data[1].toLowerCase().equals("buffer")) {
                            buffer.truncateTable();
                            buffer.insertRow(monitorNumber, (Constants.SENSOR + " here, I'll reset the buffer table :)"), "1");
                            this.abortBroadcast();
                        }
                        if (data[1].equals("BACKUP") || data[1].toLowerCase().equals("backup")) {
                            backup.truncateTable();
                            buffer.insertRow(monitorNumber, (Constants.SENSOR + " here, I'll reset the backup table :)"), "1");
                            this.abortBroadcast();
                        }
                    }
                    else {
                        this.abortBroadcast();
                    }
                }
            }
        }
    }
}