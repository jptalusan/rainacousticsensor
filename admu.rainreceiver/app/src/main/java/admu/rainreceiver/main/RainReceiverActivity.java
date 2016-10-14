package admu.rainreceiver.main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.widget.TextView;
import android.widget.Toast;

public class RainReceiverActivity extends Activity {

	private TextView received_rain1, saved_rain1_server1 = null, saved_rain1_server2 = null;
	private TextView received_rain2, saved_rain2_server1 = null, saved_rain2_server2 = null;
	private TextView received_rain3, saved_rain3_server1 = null, saved_rain3_server2 = null;
	private TextView rows_buffer = null;
	
	private PowerManager pm;
    private PowerManager.WakeLock wakeLock;
	
	Messenger mService = null;
    public boolean mIsBound;
    final Messenger mMessenger = new Messenger(new IncomingHandler());

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rain_receiver);
		
		// this will restart the app in case of a unexcepted crash
		final PendingIntent intent = PendingIntent.getActivity(RainReceiverActivity.this, 0,
	            new Intent(getIntent()), getIntent().getFlags());
		
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, final Throwable ex) {
            	AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            	mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, intent);
            	System.exit(2);
            }
        });
		
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
		
		startService(new Intent(RainReceiverActivity.this, RainReceiverService.class));
		doBindService();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
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
            	case RainReceiverService.MSG_SET_ROWS_BUFFER:
	            	str = msg.getData().getString("str");
	            	rows_buffer.setText(str);
	                break;
	            case RainReceiverService.MSG_SET_RECEIVED_RAIN1:
	            	str = msg.getData().getString("str");
	            	received_rain1.setText(str);
	                break;
	            case RainReceiverService.MSG_SET_SAVED_RAIN1_SERVER1:
	            	str = msg.getData().getString("str");
	            	saved_rain1_server1.setText(str);
	                break;
	            case RainReceiverService.MSG_SET_SAVED_RAIN1_SERVER2:
	            	str = msg.getData().getString("str");
	            	saved_rain1_server2.setText(str);
	                break;
	            case RainReceiverService.MSG_SET_RECEIVED_RAIN2:
	            	str = msg.getData().getString("str");
	            	received_rain2.setText(str);
	                break;
	            case RainReceiverService.MSG_SET_SAVED_RAIN2_SERVER1:
	            	str = msg.getData().getString("str");
	            	saved_rain2_server1.setText(str);
	                break;
	            case RainReceiverService.MSG_SET_SAVED_RAIN2_SERVER2:
	            	str = msg.getData().getString("str");
	            	saved_rain2_server2.setText(str);
	                break;
	            case RainReceiverService.MSG_SET_RECEIVED_RAIN3:
	            	str = msg.getData().getString("str");
	            	received_rain3.setText(str);
	                break;
	            case RainReceiverService.MSG_SET_SAVED_RAIN3_SERVER1:
	            	str = msg.getData().getString("str");
	            	saved_rain3_server1.setText(str);
	                break;
	            case RainReceiverService.MSG_SET_SAVED_RAIN3_SERVER2:
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
                    Message msg = Message.obtain(null, RainReceiverService.MSG_UNREGISTER_CLIENT);
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
                Message msg = Message.obtain(null, RainReceiverService.MSG_REGISTER_CLIENT);
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

}