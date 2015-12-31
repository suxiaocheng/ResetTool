package com.silicongo.george.reboottool;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    public static final String ACTION_REBOOT = "android.intent.action.REBOOT";
    public static final String ACTION_REQUEST_SHUTDOWN = "android.intent.action.ACTION_REQUEST_SHUTDOWN";

    /* Reboot service */
    RebootService mService;
    boolean mBound = false;
    Intent mRebootIntent;


    @Bind(R.id.tvRebootTimesInfo)
    TextView tvRebootTimesInfo;

    @Bind(R.id.etRebootTimes)
    EditText etRebootTimes;

    @Bind(R.id.tvRebootInfo)
    TextView tvRebootInfo;

    @Bind(R.id.tvCurrentRebootCountdown)
    TextView tvCurrentRebootCountdown;

    @Bind(R.id.btStartTest)
    Button btStartTest;

    @OnClick(R.id.btStartTest)
    void startReboot() {
        isRebootOn = !isRebootOn;

        if (mBound == true) {
            mService.isCancel = true;
            unbindService(mConnection);
        }

        totalRebootTimes = Integer.valueOf(etRebootTimes.getText().toString());
        if (totalRebootTimes < 3) {
            totalRebootTimes = 3;
        }
        currentRebootTimes = 0;

        saveAll();

        if (isRebootOn) {
            mRebootIntent = new Intent(this, RebootService.class);
            startService(mRebootIntent);
            bindService(mRebootIntent, mConnection, Context.BIND_AUTO_CREATE);
        }
        setUI(isRebootOn);
    }

    /* Global Var to store reboot status */
    private boolean isRebootOn = false;
    private int totalRebootTimes = 5000;
    private int currentRebootTimes = 0x0;

    SharedPreferences sharedPref;

    private final static String StringRebootOn = "com.silicongo.george.RebootOn";
    private final static String StringTotalRebootTimes = "com.silicongo.george.TotalRebootTimes";
    private final static String StringCurrentRebootTimes = "com.silicongo.george.CurrentRebootTimes";

    private void saveAll() {
        saveRebootOn();
        saveCurrentRebootTimes();
        saveTotalRebootTimes();
    }

    private void saveRebootOn() {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(StringRebootOn, isRebootOn);
        editor.commit();
    }

    private void saveTotalRebootTimes() {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(StringTotalRebootTimes, totalRebootTimes);
        editor.commit();
    }

    private void saveCurrentRebootTimes() {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(StringCurrentRebootTimes, currentRebootTimes);
        editor.commit();
    }

    private void setUI(boolean isBootOn) {
        etRebootTimes.setText(Integer.toString(totalRebootTimes));

        if (isBootOn == false) {
            if (currentRebootTimes >= totalRebootTimes) {
                tvRebootInfo.setVisibility(View.VISIBLE);
                tvRebootInfo.setText(getResources().getText(R.string.reboot_info).toString() +
                        totalRebootTimes + "/" + currentRebootTimes + ", Test PASS");
            } else {
                tvRebootInfo.setVisibility(View.GONE);
            }
            tvCurrentRebootCountdown.setVisibility(View.GONE);

            btStartTest.setText(getResources().getText(R.string.start_test));
        } else {
            tvRebootInfo.setText(getResources().getText(R.string.reboot_info).toString() +
                    totalRebootTimes + "/" + currentRebootTimes);

            tvRebootInfo.setVisibility(View.VISIBLE);
            tvCurrentRebootCountdown.setVisibility(View.VISIBLE);

            btStartTest.setText(getResources().getText(R.string.stop_test));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        sharedPref = getPreferences(Context.MODE_PRIVATE);

        isRebootOn = sharedPref.getBoolean(StringRebootOn, isRebootOn);
        totalRebootTimes = sharedPref.getInt(StringTotalRebootTimes, totalRebootTimes);
        currentRebootTimes = sharedPref.getInt(StringCurrentRebootTimes, currentRebootTimes);

        registerReceiver(broadcastUpdateUIReceiver, new IntentFilter(RebootService.BROADCAST_UPDATE_UI));

        if (isRebootOn == true) {
            if (currentRebootTimes < totalRebootTimes) {
                currentRebootTimes++;
                saveCurrentRebootTimes();

                mRebootIntent = new Intent(this, RebootService.class);
                startService(mRebootIntent);
            } else {
                isRebootOn = false;
                setUI(isRebootOn);
                saveAll();
            }
        }
    }

    @Override
    public void onResume() {
        setUI(isRebootOn);

        if (mRebootIntent != null) {
            bindService(mRebootIntent, mConnection, Context.BIND_AUTO_CREATE);
        }

        super.onResume();
    }

    @Override
    public void onPause() {
        if (mRebootIntent != null) {
            unbindService(mConnection);
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public  void onDestroy(){
        unregisterReceiver(broadcastUpdateUIReceiver);
        super.onDestroy();
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            RebootService.LocalBinder binder = (RebootService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private BroadcastReceiver broadcastUpdateUIReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Update Your UI here..
            int count = (int)(intent.getExtras().get(RebootService.DOWNCOUNT));
            if (tvCurrentRebootCountdown != null) {
                tvCurrentRebootCountdown.setText(
                        getResources().getText(R.string.reboot_count_down).toString() + ": " +
                                count + " s");
            }
            if(count == 0) {
                if (RootUtil.isDeviceRooted() == false) {
                    Toast.makeText(getBaseContext(), R.string.device_is_not_root, Toast.LENGTH_LONG);
                } else {
                    CommandLine.execShell(new String[]{"su", "-c", "reboot"});
                    Log.d(TAG, "Main Reboot Ok, Remain: " + currentRebootTimes + " Times");
                    finish();
                }
            }
        }
    };

}
