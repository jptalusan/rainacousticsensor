package admu.raintransmitter.main;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RainTransmitterActivity extends AppCompatActivity {
    private static final String TAG = "RainTxAct";
    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 1000;
    Messenger mService = null;
    public boolean mIsBound;
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    // GUIs
    private static TextView test = null;
    private static TextView status = null;
    private static TextView mode = null;
    private static EditText etMonitorNumber = null;
    private static EditText etServerNumber = null;
    private static EditText etThreshold = null;
    private static EditText etServerUrl = null;
    private static Button bSave = null;
    private static Spinner transmitterSpinner = null;

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
        etThreshold = (EditText)findViewById(R.id.etThreshold);
        etServerUrl = (EditText)findViewById(R.id.etServerUrl);
        bSave = (Button)findViewById(R.id.bSave);
        transmitterSpinner = (Spinner)findViewById(R.id.transmittersSpinner);

        //Number to be saved should be in the format +639059716422
        etMonitorNumber.setText(sharedPref.getString(Constants.MONITOR_NUM_KEY, ""));
        etServerNumber.setText(sharedPref.getString(Constants.SERVER_NUM_KEY, ""));
        etThreshold.setText(Float.toString(sharedPref.getFloat(Constants.THRESHOLD_KEY, 0.0f)));
        etServerUrl.setText(sharedPref.getString(Constants.SERVER_URL_KEY, ""));

        bSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putString(Constants.MONITOR_NUM_KEY, etMonitorNumber.getText().toString()).apply();
                editor.putString(Constants.SERVER_NUM_KEY, etServerNumber.getText().toString()).apply();
                editor.putFloat(Constants.THRESHOLD_KEY, Float.parseFloat(etThreshold.getText().toString())).apply();
                editor.putString(Constants.SERVER_URL_KEY, etServerUrl.getText().toString()).apply();
                Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
            }
        });

        Log.d("Get string", "" + sharedPref.getString(Constants.MONITOR_NUM_KEY, ""));

        checkAndRequestPermissions();

        String transmitterName = sharedPref.getString(Constants.TRANSMITTER_ID_KEY, "");
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.transmitters, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        transmitterSpinner.setAdapter(adapter);

        if (!transmitterName.equals("")) {
            int spinnerPosition = adapter.getPosition(transmitterName);
            transmitterSpinner.setSelection(spinnerPosition);
        }

        transmitterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String transmitterName = (String)parent.getItemAtPosition(position);
                Log.d(TAG, transmitterName);
                editor.putString(Constants.TRANSMITTER_ID_KEY, transmitterName).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void startService() {
        // this will restart the app in case of a unexcepted crash
        final PendingIntent intent = PendingIntent.getActivity(RainTransmitterActivity.this, 0,
                new Intent(getIntent()), getIntent().getFlags());

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, final Throwable ex) {
                writeError(ex);	// a trace of the error
                AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, intent);
                System.exit(2);
            }
        });
        //end of restart code

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
            String str = msg.getData().getString("str");
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
                case Constants.MSG_SET_UPDATE_THRESHOLD:
                    etThreshold.setText(str);
//                    etThreshold.setText(Float.toString(sharedPref.getFloat(Constants.THRESHOLD_KEY, 0.0f)));
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


    private  boolean checkAndRequestPermissions() {
        int permissionRecordAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int permissionSendSMS = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
        int permissionReadSMS = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS);
        int permissionReceiveSMS = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS);
        int permissionReadPhoneState = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
        int permissionWriteStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (permissionWriteStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permissionSendSMS != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.SEND_SMS);
        }
        if (permissionReadSMS != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_SMS);
        }
        if (permissionReceiveSMS != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECEIVE_SMS);
        }
        if (permissionReadPhoneState != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (permissionRecordAudio != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            return false;
        }

        startService();
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.d(TAG, "Permission callback called-------");
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {

                Map<String, Integer> perms = new HashMap<>();
                // Initialize the map with both permissions
                perms.put(Manifest.permission.RECORD_AUDIO, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.SEND_SMS, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.READ_SMS, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.RECEIVE_SMS, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.READ_PHONE_STATE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);

                // Fill with actual results from user
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);
                    // Check for both permissions
                    if (perms.get(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        // process the normal flow
                        startService();

                        //else any one or both the permissions are not granted
                    } else {
                        Log.d(TAG, "Some permissions are not granted ask again ");
                        //permission is denied (this is the first time, when "never ask again" is not checked) so ask again explaining the usage of permission
//                        // shouldShowRequestPermissionRationale will return true
                        //show the dialog or snackbar saying its necessary and try again otherwise proceed with setup.
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)
                                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)
                                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_SMS)
                                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECEIVE_SMS)
                                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)
                                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            showDialogOK("Storage and Audio Record Services Permission required for this app",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            switch (which) {
                                                case DialogInterface.BUTTON_POSITIVE:
                                                    checkAndRequestPermissions();
                                                    break;
                                                case DialogInterface.BUTTON_NEGATIVE:
                                                    // proceed with logic by disabling the related features or quit the app.
                                                    finish();
                                                    break;
                                            }
                                        }
                                    });
                        }
                        //permission is denied (and never ask again is  checked)
                        //shouldShowRequestPermissionRationale will return false
                        else {
                            Toast.makeText(this, "Go to settings and enable permissions", Toast.LENGTH_LONG)
                                    .show();
                            //                            //proceed with logic by disabling the related features or quit the app.
                        }
                    }
                }
            }
        }

    }

    private void showDialogOK(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", okListener)
                .create()
                .show();
    }
}
