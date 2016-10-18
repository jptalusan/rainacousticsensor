package admu.rainreceiver.main;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.telephony.SmsMessage;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class RainReceiverService extends Service {

    ArrayList<Messenger> mClients = new ArrayList<>(); // Keeps track of all current registered clients.
    private final Messenger mMessenger = new Messenger(new IncomingHandler()); // Target we publish for clients to send messages to IncomingHandler.

	private PowerManager pm;
    private PowerManager.WakeLock wakeLock;
    
    private String server1 = "";
    private String server2 = "http://192.168.1.170/settings/";
	static File sdLink = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Constants.directory);

    private SQLiteBuffer buffer = null;

    private Timer logTimer = null ;

    private String rain1number = "";
    private String rain2number = "";
    private String rain3number = "";
    private String monitorNumber = "";
    
    @Override
     public void onCreate() {
        super.onCreate();

        // start the receiver for the texts
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        this.registerReceiver(this.SmsReceiver, filter);

        pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My wakelock");
        
        sdLink.mkdirs();
        File dbfile = new File(sdLink + "/buffer.sqlite");
        buffer = new SQLiteBuffer(dbfile.getPath());
        buffer.createTable();
        
        startTimer();
    }

     @Override
     public int onStartCommand(Intent intent, int flags, int startId) {
         wakeLock.acquire();
         return START_STICKY;
     }

     @Override
     public void onDestroy() {
         super.onDestroy();
         if (logTimer != null) stopTimer();
         wakeLock.release();
         buffer.closeDatabase();
     }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
    
    // Handler of incoming messages from clients.
    @SuppressLint("HandlerLeak")
    class IncomingHandler extends Handler {
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
    
    private void sendMessageToUI(int object, String message) {
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
                //Send data as a String
                Bundle b = new Bundle();
                b.putString("str", message);
                Message msg = Message.obtain(null, object);
                msg.setData(b);
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    /**
     * Second parameter of buffer.insertRow() is for which server to send to
     * TODO: Again, dynamic would be better.
     * TODO: Might be better to be able to allow more than 3 sensors (dynamically)
     */
    private BroadcastReceiver SmsReceiver = new BroadcastReceiver(){
		@Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE);
            monitorNumber = sharedPref.getString(Constants.MONITOR, "");
            server1 = sharedPref.getString(Constants.SERVER1, "");
            rain1number = sharedPref.getString(Constants.SENSOR1, "");
            rain2number = sharedPref.getString(Constants.SENSOR2, "");
            rain3number = sharedPref.getString(Constants.SENSOR3, "");
            // Get SMS map from Intent
            Bundle extras = intent.getExtras();
            if (extras != null) {
                // Get received SMS array
                Object[] smsExtra = (Object[]) extras.get(Constants.SMS_EXTRA_NAME);
                if (null != smsExtra) {
                    for (int i = 0; i < smsExtra.length; ++i) {
                        SmsMessage sms = SmsMessage.createFromPdu((byte[])smsExtra[i]);
                        String body = sms.getMessageBody();
                        String number = sms.getOriginatingAddress();
                        // check the msg
                        if (body.substring(0,1).equals("#") && body.substring(body.length() - 1).equals("#")) {
                            String[] data = body.split(";");
                            // truncate the buffer table
                            if (data.length == 3) {
                                if (data[0].equals("#RR") && data[1].equals("TRUNCATEBUFFER") && number.equals(monitorNumber)) {
                                    buffer.truncateTable();
                                    sendMessageToUI(Constants.MSG_SET_ROWS_BUFFER, "Rows in buffer : " + String.valueOf(buffer.getNumberRows()));
                                }
                            }
                            // calling wave update function
                            if (data.length == 6) {
                                if (data[0].equals("#RT1") && number.equals(rain1number)) {
                                    buffer.insertRow(1, 1, body);
//                                    buffer.insertRow(1, 2, body);
                                    sendMessageToUI(Constants.MSG_SET_ROWS_BUFFER, "Rows in buffer : " + String.valueOf(buffer.getNumberRows()));
                                    sendMessageToUI(Constants.MSG_SET_RECEIVED_RAIN1, data[1]);
                                }
                                if (data[0].equals("#RT2") && number.equals(rain2number)) {
                                    buffer.insertRow(2, 1, body);
//                                    buffer.insertRow(2, 2, body);
                                    sendMessageToUI(Constants.MSG_SET_ROWS_BUFFER, "Rows in buffer : " + String.valueOf(buffer.getNumberRows()));
                                    sendMessageToUI(Constants.MSG_SET_RECEIVED_RAIN2, data[1]);
                                }
                                if (data[0].equals("#RT3") && number.equals(rain3number)) {
                                    buffer.insertRow(3, 1, body);
//                                    buffer.insertRow(3, 2, body);
                                    sendMessageToUI(Constants.MSG_SET_ROWS_BUFFER, "Rows in buffer : " + String.valueOf(buffer.getNumberRows()));
                                    sendMessageToUI(Constants.MSG_SET_RECEIVED_RAIN3, data[1]);
                                }
                            }
                            this.abortBroadcast();
                        }
                    }
                }
            }
        }
    };

    /**
     * Sends postParameters to HttpClient via CustomHttpClient.executeHttpPost
     * @param row
     */
    public void updateWaveValues(String[] row) {
        // create an array list to put all the data
        ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
        ContentValues postParameters1 = new ContentValues();
        String sensorName = row[1];
        String message = row[3];
        postParameters.add(new BasicNameValuePair("key", "3TkxeKi"));
        postParameters.add(new BasicNameValuePair("table", "rain" + sensorName));
        postParameters1.put("key", "3TkxeKi");
        postParameters1.put("table", "rain" + sensorName);

        String[] text = message.split(";");
        postParameters.add(new BasicNameValuePair("date_time", text[1]));
        postParameters1.put("date_time", text[1]);

        String[] snd = text[2].split(",");
        for (int i = 0; i < 6; i++) {
            postParameters.add(new BasicNameValuePair("snd" + String.valueOf(i+1), snd[i]));
            postParameters1.put("snd" + String.valueOf(i + 1), snd[1]);
        }

        String[] sig = text[3].split(",");
        for (int i = 0; i < 6; i++) {
            postParameters.add(new BasicNameValuePair("sig" + String.valueOf(i+1), sig[i]));
            postParameters1.put("sig" + String.valueOf(i + 1), sig[1]);
        }

        String[] dis = text[4].split(",");
        for (int i = 0; i < 10; i++) {
            postParameters.add(new BasicNameValuePair("dis" + String.valueOf(i+1), dis[i]));
            postParameters1.put("dis" + String.valueOf(i + 1), dis[1]);
        }

        // http request
        String result = "";
        String address = "";
        if (Integer.parseInt(row[2]) == 1) {
            address = server1;
        }
        if (Integer.parseInt(row[2]) == 2) {
            address = server2;
        }
        try {
            result = CustomHttpClient.executeHttpPost(address + '/' + Constants.INSERT_PHP, postParameters);
        } catch (final Exception e) {
            result = "FAIL";
        }
        
        if (result.equals("SUCCESS")) {
            buffer.deleteRow(Integer.parseInt(row[0]));
            sendMessageToUI(Constants.MSG_SET_ROWS_BUFFER, "Rows in buffer : " + String.valueOf(buffer.getNumberRows()));
            if (row[1].equals("1")) {
                if (row[2].equals("1"))
                    sendMessageToUI(Constants.MSG_SET_SAVED_RAIN1_SERVER1, "Server 1 : " + text[1]);
                else
                    sendMessageToUI(Constants.MSG_SET_SAVED_RAIN1_SERVER2, "Server 2 : " + text[1]);
            }
            if (row[1].equals("2")) {
                if (row[2].equals("1"))
                    sendMessageToUI(Constants.MSG_SET_SAVED_RAIN2_SERVER1, "Server 1 : " + text[1]);
                else
                    sendMessageToUI(Constants.MSG_SET_SAVED_RAIN2_SERVER2, "Server 2 : " + text[1]);
            }
            if (row[1].equals("3")) {
                if (row[2].equals("1"))
                    sendMessageToUI(Constants.MSG_SET_SAVED_RAIN3_SERVER1, "Server 1 : " + text[1]);
                else
                    sendMessageToUI(Constants.MSG_SET_SAVED_RAIN3_SERVER2, "Server 2 : " + text[1]);
            }
        }
    }

    public void startTimer(){
        // create the timer & reset the array & its position
        logTimer = null;
        logTimer = new Timer();
        logTimer.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run() {
                if (buffer.getNumberRows() > 0) {
                    updateWaveValues(buffer.getFirstRow());
                }
            }
        }
        , 0, Constants.LOG_INTERVAL);
    }

    public void stopTimer() {
        logTimer.cancel();
        logTimer.purge();
        logTimer = null;
    }
}