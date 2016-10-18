package admu.raintransmitter.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RainTransmitterActivity extends Activity {

    Messenger mService = null;
    public boolean mIsBound;
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    // GUIs
    private static TextView test = null;
    private static TextView status = null;
    private static TextView mode = null;
    private static EditText etMonitorNumber = null;
    private static EditText etServerNumber = null;
    private static Button bSave = null;
    
    // Handler gets created on the UI-thread
    private Handler mHandler = new Handler();
    
    private PowerManager pm;
    private PowerManager.WakeLock wakeLock;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    //TODO: Add button to save the edit text items for monitor and server number
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rain_transmitter);

        sharedPref = this.getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        // check or create the folder
        boolean wasCreated = Constants.SDLINK.mkdirs();
        try {
            if (wasCreated) {
                new FileWriter(new File(Constants.SDLINK , Constants.logFile), true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
         
        test = (TextView)findViewById(R.id.id_test);
        status = (TextView)findViewById(R.id.status_textview);
        mode = (TextView)findViewById(R.id.mode_textview);
        etMonitorNumber = (EditText)findViewById(R.id.etMonitorNumber);
        etServerNumber = (EditText)findViewById(R.id.etServerNumber);
        bSave = (Button)findViewById(R.id.bSave);

        //Number to be saved should be in the format +639059716422
        etMonitorNumber.setText(sharedPref.getString(Constants.MONITOR_NUM_KEY, "Please enter monitor num."));
        etServerNumber.setText(sharedPref.getString(Constants.SERVER_NUM_KEY, "Please enter server num."));

        bSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putString(Constants.MONITOR_NUM_KEY, etMonitorNumber.getText().toString()).apply();
                editor.putString(Constants.SERVER_NUM_KEY, etServerNumber.getText().toString()).apply();
                Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
            }
        });

        Log.d("Get string", "" + sharedPref.getString(Constants.MONITOR_NUM_KEY, ""));

        startService(new Intent(RainTransmitterActivity.this, RainTransmitterService.class));
        doBindService();

        pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Constants.WAKELOCK);
        wakeLock.acquire();
    }

    @Override
    public void onBackPressed() {
        // new alertDialog
        AlertDialog.Builder alertDialogRecord = new AlertDialog.Builder(RainTransmitterActivity.this);
        alertDialogRecord
            .setTitle("EXIT APPLICATION")
            .setIcon(android.R.drawable.ic_delete)
            .setMessage("Do you really want to exit the application ?")
            .setCancelable(false)
            .setNegativeButton("NO",new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog,int id) {
                    // if this button is clicked, just close the dialog box and do nothing
                    dialog.cancel();
                }
            })
            .setPositiveButton("YES",new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog,int id) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            onDestroy();
                            RainTransmitterActivity.super.onBackPressed();
                        }
                    });
                }
            });
            // create alert dialog
            AlertDialog alertDialog = alertDialogRecord.create();
            // show it
            alertDialog.show();
    }

    @Override
     public void onDestroy() {
        doUnbindService();
        if (wakeLock.isHeld())
            wakeLock.release();
        finish();
        super.onDestroy();
    }

    /**
     * Receives the message from sendMessageToUI in RainTransmitterService
     */
    public static class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String str = "";
            switch (msg.what) {
                case Constants.MSG_SET_TEST:
                    str = msg.getData().getString("str");
                    test.setText(str);
                    break;
                case Constants.MSG_SET_STATUS_ON:
                    status.setText("MODULE OPERATING");
                    status.setTextColor(Color.RED);
                    break;
                case Constants.MSG_SET_STATUS_OFF:
                    status.setText("MODULE OFF");
                    status.setTextColor(Color.BLACK);
                    mode.setText("Transmission Mode: OFF");
                    mode.setTextColor(Color.BLACK);
                    break;
                case Constants.MSG_SET_MODE_GSM:
                    mode.setText("Transmission Mode: GSM");
                    mode.setTextColor(Color.BLUE);
                    break;
                case Constants.MSG_SET_MODE_WIFI:
                    mode.setText("Transmission Mode: WIFI");
                    mode.setTextColor(Color.BLUE);
                    break;
                case Constants.MSG_SET_MODE_TEST:
                    mode.setText("Transmission Mode: TEST");
                    mode.setTextColor(Color.BLUE);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    public void doBindService() {
        bindService(new Intent(this, RainTransmitterService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        Toast.makeText(RainTransmitterActivity.this, "Binding.", Toast.LENGTH_SHORT).show();
    }
    
    public void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, Constants.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            Toast.makeText(RainTransmitterActivity.this, "Unbinding.", Toast.LENGTH_SHORT).show();
        }
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder myService) {
            mService = new Messenger(myService);
            Toast.makeText(RainTransmitterActivity.this, "Service : attached", Toast.LENGTH_SHORT).show();
            try {
                Message msg = Message.obtain(null, Constants.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
            Toast.makeText(RainTransmitterActivity.this, "Service : disconnected.", Toast.LENGTH_SHORT).show();
        }
    };
    
    private void writeError(Throwable data) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            data.printStackTrace(pw);
            FileWriter out = new FileWriter(new File(Constants.SDLINK , "log.txt"), true);
            SimpleDateFormat ft =  new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            out.write("on " + ft.format(new Date()) + ", \n");
            out.write(sw.toString());
            out.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        } 
    }

}
