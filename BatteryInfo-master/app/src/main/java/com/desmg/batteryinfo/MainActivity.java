package com.desmg.batteryinfo;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {
    Handler handler1 = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Context ctx = getApplicationContext();
        final NotificationManager notificationManager = getSystemService(NotificationManager.class);
        final NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(ctx);
        final NotificationChannel channel = new NotificationChannel("com.desmg.BatteryInfo", "BatteryInfo", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("BatteryInfo");
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);// 测试发现MIUI10在手动开放权限后将会显示锁屏通知但默认权限为禁止
        channel.setShowBadge(false);// 测试发现MIUI10将不会显示角标但权限依然开放
        channel.enableLights(false);// 测试发现MIUI10默认权限为禁止
        channel.enableVibration(false);// 测试发现MIUI10将不会震动标但权限依然开放
        notificationManager.createNotificationChannel(channel);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, "com.desmg.BatteryInfo");
        builder.setSmallIcon(R.drawable.ic_launcher_foreground);
        builder.setOnlyAlertOnce(true);// 测试MIUI10正常
        builder.setBadgeIconType(NotificationCompat.BADGE_ICON_NONE);
        // setBadgeIconType与channel重复，双重保险
        // 包括上述“测试发现MIUI10将不会显示角标但权限依然开放”也建立在此处前提下
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);

        handler1.post(new Runnable() {
            @Override
            public void run() {
                //region Init
                TextView tv1 = findViewById(R.id.status);
                TextView tv2 = findViewById(R.id.nowt);
                TextView tv3 = findViewById(R.id.nowv);
                TextView tv4 = findViewById(R.id.nowp);
                TextView tv5 = findViewById(R.id.nowa);
                TextView tv6 = findViewById(R.id.willF);
                //endregion Init
                IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = ctx.registerReceiver(null, ifilter);
                if (batteryStatus != null) {
                    //region var
                    boolean isCharging;
                    boolean disCharging;
                    boolean notCharging;
                    boolean fullCharging;
                    boolean usbCharge;
                    boolean acCharge;
                    boolean wirelessCharge;
                    int batteryTemperature;
                    int chargeV;
                    int batteryPercent;
                    int chargeA;
                    long willFullTime;
                    //endregion var
                    int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                    if (status != -1) {
                        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;
                        disCharging = status == BatteryManager.BATTERY_STATUS_DISCHARGING;
                        notCharging = status == BatteryManager.BATTERY_STATUS_NOT_CHARGING;
                        fullCharging = status == BatteryManager.BATTERY_STATUS_FULL;
                        if (isCharging) {
                            int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                            if (chargePlug != -1) {
                                acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
                                usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
                                wirelessCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS;
                                if (acCharge) {
                                    tv1.setText("正在通过AC充电");
                                }
                                if (usbCharge) {
                                    tv1.setText("正在通过USB充电");
                                }
                                if (wirelessCharge) {
                                    tv1.setText("正在通过Wireless充电");
                                }
                            }
                        }
                        if (disCharging) {
                            tv1.setText("已断开充电器");
                        }
                        if (notCharging) {
                            tv1.setText("未在充电");
                        }
                        if (fullCharging) {
                            tv1.setText("已充满");
                        }
                    }
                    batteryTemperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                    if (batteryTemperature != -1) {
                        tv2.setText("当前电池温度：" + batteryTemperature / 10 + "摄氏度");
                    }
                    chargeV = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
                    if (chargeV != -1) {
                        tv3.setText("当前电池电压：" + chargeV + "mV");
                    }
                    BatteryManager mBatteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
                    if (mBatteryManager != null) {
                        batteryPercent = mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                        tv4.setText("当前电池百分比：" + batteryPercent + "%");
                        chargeA = mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                        tv5.setText("当前充电电流：" + chargeA / -1000 + "mA");
                        willFullTime = mBatteryManager.computeChargeTimeRemaining();
                        tv6.setText("预计充满时间：" + willFullTime / 1000 / 60 + "分钟");

                        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
                        bigTextStyle.setBigContentTitle("电池信息");
                        bigTextStyle.setSummaryText(tv1.getText());
                        bigTextStyle.bigText(tv2.getText() + "\r\n" + tv3.getText() + "\r\n" + tv4.getText() + "\r\n" + tv5.getText() + "\r\n" + tv6.getText());

                        builder.setStyle(bigTextStyle);
                        // 测试发现MIUI10中如果使用经典样式通知可能无法正常展开列表
                        // 遇到这种情况需要使用原生样式
                        // 或使用bak文件中的同时发6条通知模式

                        notificationManagerCompat.notify(0, builder.build());

                    }
                }
                handler1.postDelayed(this, 1000);
            }
        });
    }
}