package com.silicongo.george.reboottool;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by suxch on 2015/12/30.
 */
public class CommandLine {
    public static final String TAG = "CommandLine";

    public static String[] execShell(String cmd[]) {
        String cmdInfo[] = new String[500];
        int valid_cmd_line = 0x0;
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(cmd);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            p.getInputStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
                if (valid_cmd_line < cmdInfo.length) {
                    cmdInfo[valid_cmd_line] = line;
                    valid_cmd_line++;
                } else {
                    Log.e(TAG, "output buffer not enough to hold string");
                }
                Log.i("execShell", line);
            }
        } catch (IOException t) {
            t.printStackTrace();
            if (valid_cmd_line < cmdInfo.length) {
                cmdInfo[valid_cmd_line] = t.getMessage();
                valid_cmd_line++;
            }
        } finally {
            if (p != null) p.destroy();
        }

        cmdInfo[valid_cmd_line] = null;
        return cmdInfo;
    }
}
