package com.silicongo.george.reboottool;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;

/**
 * Created by Administrator on 2015/12/31.
 */
public class RebootService extends IntentService {
    public static final String TAG = "RebootService";

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private Object lock = new Object();

    public int iRebootCount;
    public boolean isCancel = false;

    public static final String BROADCAST_UPDATE_UI = "com.silicongo.george.updateui";
    public static final String DOWNCOUNT = "com.silicongo.george.downcount";

    /**
     * A constructor is required, and must call the super IntentService(String)
     * constructor with a name for the worker thread.
     */
    public RebootService() {
        super("RebootService");
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        RebootService getService() {
            // Return this instance of LocalService so clients can call public methods
            return RebootService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns, IntentService
     * stops the service, as appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        // Normally we would do some work here, like download a file.
        // For our sample, we just sleep for 5 seconds.
        iRebootCount = 5;
        Log.d(TAG, "HandleIntent");

        do {
            Intent i = new Intent(BROADCAST_UPDATE_UI);
            i.putExtra(DOWNCOUNT, iRebootCount);
            sendBroadcast(i);
            if (isCancel == true) {
                break;
            }
            try {
                synchronized (lock) {
                    lock.wait(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (iRebootCount-- > 0);

        if (isCancel == false) {
            Intent i = new Intent(BROADCAST_UPDATE_UI);
            i.putExtra(DOWNCOUNT, iRebootCount);
            sendBroadcast(i);
            Log.d(TAG, "Going to reboot...");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "service starting");
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "service done");
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }
}
