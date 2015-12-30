package com.silicongo.george.reboottool;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
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

        if (rebootDownCountJob != null) {
            if (rebootDownCountJob.getStatus() != AsyncTask.Status.FINISHED) {
                rebootDownCountJob.cancel(true);
            }
            rebootDownCountJob = null;
        }

        totalRebootTimes = Integer.valueOf(etRebootTimes.getText().toString());
        if (totalRebootTimes < 5) {
            totalRebootTimes = 5;
        }
        currentRebootTimes = 0;

        saveAll();

        if (isRebootOn) {
            rebootDownCountJob = new RebootDownCountJob(5);
            rebootDownCountJob.execute();
        }
        setUI(isRebootOn);
    }

    private RebootDownCountJob rebootDownCountJob;
    Object countDownLock = new Object();

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
    }

    @Override
    public void onResume() {
        isRebootOn = sharedPref.getBoolean(StringRebootOn, isRebootOn);
        totalRebootTimes = sharedPref.getInt(StringTotalRebootTimes, totalRebootTimes);
        currentRebootTimes = sharedPref.getInt(StringCurrentRebootTimes, currentRebootTimes);

        setUI(isRebootOn);

        if (isRebootOn) {
            if (currentRebootTimes < totalRebootTimes) {
                rebootDownCountJob = new RebootDownCountJob(5);
                rebootDownCountJob.execute();
            } else {
                isRebootOn = false;
                setUI(isRebootOn);
                saveAll();
            }
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        if (rebootDownCountJob != null) {
            if (rebootDownCountJob.getStatus() != AsyncTask.Status.FINISHED) {
                rebootDownCountJob.cancel(true);
            }
            rebootDownCountJob = null;
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private class RebootDownCountJob extends AsyncTask<String, String, Void> {
        private static final String TAG = "executeBackgroundCmd";
        private int downCount;

        public RebootDownCountJob(int val) {
            downCount = val;
            if (downCount <= 5) {
                downCount = 5;
            }
            Log.d(TAG, "After " + downCount + " will reboot");

            tvCurrentRebootCountdown.setVisibility(View.VISIBLE);
        }

        /**
         * The system calls this to perform work in a worker thread and
         * delivers it the parameters given to AsyncTask.execute()
         */
        protected Void doInBackground(String... urls) {
            String val[] = new String[1];
            do {
                val[0] = Integer.toString(downCount);
                publishProgress(val);
                try {
                    synchronized (countDownLock) {
                        countDownLock.wait(1000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (isCancelled() == true) {
                    break;
                }
            } while (downCount-- > 0);
            return null;
        }

        protected void onPostExecute(Void result) {
            if (currentRebootTimes < totalRebootTimes) {
                currentRebootTimes++;

                saveCurrentRebootTimes();

                Log.d(TAG, "Going to reboot...");
                if (RootUtil.isDeviceRooted() == false) {
                    Toast.makeText(getBaseContext(), R.string.device_is_not_root, Toast.LENGTH_LONG);
                } else {
                    CommandLine.execShell(new String[]{"su", "-c", "reboot"});
                }
                //finish();
            }
        }

        protected void onCancelled() {
            tvCurrentRebootCountdown.setVisibility(View.GONE);
        }

        protected void onProgressUpdate(String... progress) {
            tvCurrentRebootCountdown.setText(getResources().getText(R.string.reboot_count_down).toString() + ": " + downCount + " s");
        }
    }
}
