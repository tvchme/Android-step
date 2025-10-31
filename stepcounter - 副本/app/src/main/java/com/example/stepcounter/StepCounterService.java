// StepCounterService.java
package com.example.stepcounter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class StepCounterService extends Service implements SensorEventListener {

    private static final String TAG = "StepCounterService";

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private StepDetectionAlgorithm stepDetector;
    private PowerManager.WakeLock wakeLock;
    private int totalSteps = 0;

    private static final String CHANNEL_ID = "StepCounterChannel";
    private static final int NOTIFICATION_ID = 1;

    public static final String ACTION_STEP_UPDATE = "STEP_UPDATE";
    public static final String EXTRA_STEP_COUNT = "step_count";

    // 传感器数据统计
    private int sensorEventCount = 0;
    private long lastLogTime = 0;

    private final IBinder binder = new StepCounterBinder();

    public class StepCounterBinder extends Binder {
        StepCounterService getService() {
            return StepCounterService.this;
        }
    }

    private BroadcastReceiver resetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("RESET_STEPS".equals(intent.getAction())) {
                Log.d(TAG, "收到重置步数指令");
                if (stepDetector != null) {
                    stepDetector.reset();
                    totalSteps = 0;
                    broadcastStepUpdate(0);
                    updateNotification(0);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "服务创建");

        createNotificationChannel();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Log.d(TAG, "加速度传感器: " + (accelerometer != null ? "可用" : "不可用"));
        }

        stepDetector = new StepDetectionAlgorithm();

        // 获取唤醒锁
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StepCounter:WakeLock");
            wakeLock.acquire();
            Log.d(TAG, "唤醒锁获取");
        }

        // 注册重置接收器
        IntentFilter filter = new IntentFilter("RESET_STEPS");
        LocalBroadcastManager.getInstance(this).registerReceiver(resetReceiver, filter);

        startForeground(NOTIFICATION_ID, createNotification());
        Log.d(TAG, "前台服务启动");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "服务启动命令");
        startStepCounting();
        return START_STICKY;
    }

    private void startStepCounting() {
        if (sensorManager != null && accelerometer != null) {
            // 使用最快的采样率获取更多数据
            boolean success = sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_GAME); // 使用游戏级延迟，约50Hz

            Log.d(TAG, "传感器注册: " + (success ? "成功" : "失败"));

            if (success) {
                updateNotification("传感器已启动");
            } else {
                updateNotification("传感器启动失败");
            }
        } else {
            Log.e(TAG, "传感器不可用");
            updateNotification("传感器不可用");
            stopSelf();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        sensorEventCount++;
        long currentTime = System.currentTimeMillis();

        // 每100个事件或每5秒记录一次日志
        if (sensorEventCount % 100 == 0 || currentTime - lastLogTime > 5000) {
            Log.d(TAG, "传感器事件: " + sensorEventCount +
                    ", 数据: " + event.values[0] + ", " + event.values[1] + ", " + event.values[2]);
            lastLogTime = currentTime;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            boolean stepDetected = stepDetector.detectStep(event.values, currentTime);

            if (stepDetected) {
                totalSteps = stepDetector.getStepCount();
                Log.i(TAG, "检测到步伐! 总步数: " + totalSteps);
                broadcastStepUpdate(totalSteps);
                updateNotification(totalSteps);
            }
        }
    }

    private void broadcastStepUpdate(int steps) {
        Intent intent = new Intent(ACTION_STEP_UPDATE);
        intent.putExtra(EXTRA_STEP_COUNT, steps);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "计步器服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("计步器后台运行服务");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("计步器运行中")
                .setContentText("正在记录您的步数")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(int steps) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("智能计步器")
                .setContentText("已记录 " + steps + " 步")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void updateNotification(String message) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("智能计步器")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "传感器精度变化: " + accuracy);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "服务销毁");

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "唤醒锁释放");
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(resetReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}