package admu.rainreceiver.main;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RainReceiverActivity extends AppCompatActivity {
    private static final String TAG = "Rain Rx Act";
    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 1000;
    private TextView received_rain1, saved_rain1_server1 = null, saved_rain1_server2 = null;
    private TextView received_rain2, saved_rain2_server1 = null, saved_rain2_server2 = null;
    private TextView received_rain3, saved_rain3_server1 = null, saved_rain3_server2 = null;
    private TextView rows_buffer = null;
    private EditText etSensor1, etSensor2, etSensor3, etServer, etMonitor;
    private Button bSave;

    private PowerManager pm;
    private PowerManager.WakeLock wakeLock;

    Messenger mService = null;
    public boolean mIsBound;
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rain_receiver);
        sharedPref = this.getSharedPreferences(Constants.SHARED_PREFS, Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        // this will restart the app in case of a unexcepted crash
//        final PendingIntent intent = PendingIntent.getActivity(RainReceiverActivity.this, 0,
//                new Intent(getIntent()), PendingIntent.FLAG_CANCEL_CURRENT);
//
//        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
//            @Override
//            public void uncaughtException(Thread thread, final Throwable ex) {
//                AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, intent);
//                System.exit(2);
//            }
//        });

        pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My wakelock");
        wakeLock.acquire();
        
        received_rain1 = (TextView)findViewById(R.id.id_rain1_received);
        saved_rain1_server1 = (TextView)findViewById(R.id.id_rain1_server1_saved);
        saved_rain1_server2 = (TextView)findViewById(R.id.id_rain1_server2_saved);
        received_rain2 = (TextView)findViewById(R.id.id_rain2_received);
        saved_rain2_server1 = (TextView)findViewById(R.id.id_rain2_server1_saved);
        saved_rain2_server2 = (TextView)findViewById(R.id.id_rain2_server2_saved);
        received_rain3 = (TextView)findViewById(R.id.id_rain3_received);
        saved_rain3_server1 = (TextView)findViewById(R.id.id_rain3_server1_saved);
        saved_rain3_server2 = (TextView)findViewById(R.id.id_rain3_server2_saved);
        rows_buffer = (TextView)findViewById(R.id.id_rows_buffer);

        etSensor1 = (EditText)findViewById(R.id.sensor1);
        etSensor2 = (EditText)findViewById(R.id.sensor2);
        etSensor3 = (EditText)findViewById(R.id.sensor3);
        etServer = (EditText)findViewById(R.id.etServer);
        etMonitor = (EditText)findViewById(R.id.etMonitor);

        bSave = (Button)findViewById(R.id.bSave);

        //Number to be saved should be in the format +639059716422
        //Server address should be in: http://admurainsensor.comxa.com
        etSensor1.setText(sharedPref.getString(Constants.SENSOR1, ""));
        etSensor2.setText(sharedPref.getString(Constants.SENSOR2, ""));
        etSensor3.setText(sharedPref.getString(Constants.SENSOR3, ""));
        etMonitor.setText(sharedPref.getString(Constants.MONITOR, ""));
        etServer.setText(sharedPref.getString(Constants.SERVER1, "http://rainsensor.excthackathon.x10host.com"));

        bSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putString(Constants.SENSOR1, etSensor1.getText().toString()).apply();
                editor.putString(Constants.SENSOR2, etSensor2.getText().toString()).apply();
                editor.putString(Constants.SENSOR3, etSensor3.getText().toString()).apply();
                editor.putString(Constants.MONITOR, etMonitor.getText().toString()).apply();
                editor.putString(Constants.SERVER1, etServer.getText().toString()).apply();
                Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
            }
        });

        checkAndRequestPermissions();
    }

    private void startService() {
        startService(new Intent(RainReceiverActivity.this, RainReceiverService.class));
        doBindService();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        doUnbindService();
        if (wakeLock.isHeld())
            wakeLock.release();
    }

    @Override
    public void onBackPressed() {
        //
    }
    
    @SuppressLint("HandlerLeak")
    public class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String str = null;
            switch (msg.what) { 
                case Constants.MSG_SET_ROWS_BUFFER:
                    str = msg.getData().getString("str");
                    rows_buffer.setText(str);
                    break;
                case Constants.MSG_SET_RECEIVED_RAIN1:
                    str = msg.getData().getString("str");
                    received_rain1.setText(str);
                    break;
                case Constants.MSG_SET_SAVED_RAIN1_SERVER1:
                    str = msg.getData().getString("str");
                    saved_rain1_server1.setText(str);
                    break;
                case Constants.MSG_SET_SAVED_RAIN1_SERVER2:
                    str = msg.getData().getString("str");
                    saved_rain1_server2.setText(str);
                    break;
                case Constants.MSG_SET_RECEIVED_RAIN2:
                    str = msg.getData().getString("str");
                    received_rain2.setText(str);
                    break;
                case Constants.MSG_SET_SAVED_RAIN2_SERVER1:
                    str = msg.getData().getString("str");
                    saved_rain2_server1.setText(str);
                    break;
                case Constants.MSG_SET_SAVED_RAIN2_SERVER2:
                    str = msg.getData().getString("str");
                    saved_rain2_server2.setText(str);
                    break;
                case Constants.MSG_SET_RECEIVED_RAIN3:
                    str = msg.getData().getString("str");
                    received_rain3.setText(str);
                    break;
                case Constants.MSG_SET_SAVED_RAIN3_SERVER1:
                    str = msg.getData().getString("str");
                    saved_rain3_server1.setText(str);
                    break;
                case Constants.MSG_SET_SAVED_RAIN3_SERVER2:
                    str = msg.getData().getString("str");
                    saved_rain3_server2.setText(str);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    public void doBindService() {
        bindService(new Intent(this, RainReceiverService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        Toast.makeText(RainReceiverActivity.this, "Binding.", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(RainReceiverActivity.this, "Unbinding.", Toast.LENGTH_SHORT).show();
        }
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder myService) {
            mService = new Messenger(myService);
            Toast.makeText(RainReceiverActivity.this, "Service : attached", Toast.LENGTH_SHORT).show();
            try {
                Message msg = Message.obtain(null, Constants.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
            Toast.makeText(RainReceiverActivity.this, "Service : disconnected.", Toast.LENGTH_SHORT).show();
        }
    };

    private boolean checkAndRequestPermissions() {
        int permissionSendSMS = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
        int permissionReadSMS = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS);
        int permissionReceiveSMS = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS);
        int permissionWriteStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (permissionSendSMS != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.SEND_SMS);
        }
        if (permissionReadSMS != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_SMS);
        }
        if (permissionReceiveSMS != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECEIVE_SMS);
        }
        if (permissionWriteStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
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
                // Initialize the map with both permission
                perms.put(Manifest.permission.SEND_SMS, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.READ_SMS, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.RECEIVE_SMS, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);

                // Fill with actual results from user
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);
                    // Check for both permissions
                    if (perms.get(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        // process the normal flow
                        startService();

                        //else any one or both the permissions are not granted
                    } else {
                        Log.d(TAG, "Some permissions are not granted ask again ");
                        //permission is denied (this is the first time, when "never ask again" is not checked) so ask again explaining the usage of permission
//                        // shouldShowRequestPermissionRationale will return true
                        //show the dialog or snackbar saying its necessary and try again otherwise proceed with setup.
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS) ||
                                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_SMS) ||
                                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECEIVE_SMS) ||
                                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
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