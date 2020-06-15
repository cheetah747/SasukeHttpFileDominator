package com.sibyl.HttpFileDominator.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.annotation.RequiresApi;

/**
 * @author Sasuke on 2020/6/9.
 */

public class BatteryOptiDominator {
    /**判断本应用是否在电池优化白名单里*/
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void requestIgnoreBatteryOpti(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager == null || !powerManager.isIgnoringBatteryOptimizations(context.getPackageName())){
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}