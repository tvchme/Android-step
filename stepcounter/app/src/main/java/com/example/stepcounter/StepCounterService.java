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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// 建议添加数据模型类和数据库工具类
// 参考 DylanStepCount 中的 StepData 和 DbUtils 实现

public class StepCounterService extends Service implements SensorEventListener {
    private static final String TAG = "StepCounterService";
    private static final String CHANNEL_ID = "StepCounterChannel";
    private static final int NOTIFICATION_ID = 1;
    public static final String ACTION_STEP_UPDATE = "STEP_UPDATE";
    public static final String EXTRA_STEP_COUNT = "step_count";

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private StepDetectionAlgorithm stepDetector;
    private PowerManager.WakeLock wakeLock;
    private int totalSteps = 0;
    private String currentDate;

    // 数据存储相关
    private static final int SAVE_INTERVAL = 30 * 1000; // 30秒保存一次
    private SaveTimer saveTimer;

    private final IBinder binder = new StepCounterBinder();


    private BroadcastReceiver systemReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                // 屏幕关闭时延长保存间隔
                if (saveTimer != null) {
                    saveTimer.setInterval(60 * 1000);
                }
            } else if (Intent.ACTION_SCREEN_ON.equals(action) ||
                    Intent.ACTION_USER_PRESENT.equals(action)) {
                // 屏幕开启或解锁时恢复保存间隔
                if (saveTimer != null) {
                    saveTimer.setInterval(SAVE_INTERVAL);
                }
            } else if (Intent.ACTION_DATE_CHANGED.equals(action) ||
                    Intent.ACTION_TIME_TICK.equals(action)) {
                // 日期变更时保存并重置步数
                saveStepData();
                if (!currentDate.equals(getTodayDate())) {
                    resetSteps();
                    currentDate = getTodayDate();
                }
            } else if ("RESET_STEPS".equals(action)) {
                resetSteps();
            }
        }
    };

    public class StepCounterBinder extends Binder {
        StepCounterService getService() {
            return StepCounterService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "服务创建");
        currentDate = getTodayDate();
        stepDetector = new StepDetectionAlgorithm();
        initSensor();
        initWakeLock();
        initNotification();
        initBroadcastReceiver();
        initSaveTimer();
        loadTodayData(); // 加载今日数据

        startForeground(NOTIFICATION_ID, createNotification());
    }

    private void initSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            // 优先使用计步传感器，没有则使用加速度传感器
            Sensor stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (stepCounter != null) {
                accelerometer = stepCounter;
                Log.d(TAG, "使用计步传感器");
            } else {
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                Log.d(TAG, "使用加速度传感器");
            }
        }
    }

    private void initWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "StepCounter:WakeLock"
            );
            wakeLock.acquire(10 * 60 * 1000L); // 10分钟超时
        }
    }

    private void initNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "计步服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("后台计步服务");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void initBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction("RESET_STEPS");
        registerReceiver(systemReceiver, filter);
    }

    private void initSaveTimer() {
        saveTimer = new SaveTimer(SAVE_INTERVAL);
        saveTimer.start();
    }

    private void loadTodayData() {
        // 参考 DylanStepCount 中的 DbUtils 实现
        // 从数据库加载今日步数
        /*
        List<StepData> dataList = DbUtils.getQueryByWhere(StepData.class,
                "today", new String[]{currentDate});
        if (dataList.size() > 0) {
            totalSteps = Integer.parseInt(dataList.get(0).getStep());
            stepDetector.reset();
            // 同步检测器步数
            for (int i = 0; i < totalSteps; i++) {
                stepDetector.detectStep(new float[3], System.currentTimeMillis());
            }
        }
        */
    }

    private void saveStepData() {
        // 保存今日步数到数据库
        /*
        List<StepData> dataList = DbUtils.getQueryByWhere(StepData.class,
                "today", new String[]{currentDate});
        StepData stepData = new StepData();
        stepData.setToday(currentDate);
        stepData.setStep(String.valueOf(totalSteps));

        if (dataList.size() > 0) {
            stepData.setId(dataList.get(0).getId());
            DbUtils.update(stepData);
        } else {
            DbUtils.insert(stepData);
        }
        */
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startStepDetection();
        return START_STICKY;
    }

    private void startStepDetection() {
        if (sensorManager != null && accelerometer != null) {
            int delay = accelerometer.getType() == Sensor.TYPE_STEP_COUNTER ?
                    SensorManager.SENSOR_DELAY_NORMAL :
                    SensorManager.SENSOR_DELAY_GAME;
            sensorManager.registerListener(this, accelerometer, delay);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            // 系统计步传感器处理
            if (totalSteps == 0) {
                totalSteps = (int) event.values[0];
            } else {
                totalSteps = (int) event.values[0] - totalSteps;
            }
            broadcastStepUpdate(totalSteps);
            updateNotification(totalSteps);
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // 自定义算法处理
            boolean stepDetected = stepDetector.detectStep(
                    event.values, System.currentTimeMillis());
            if (stepDetected) {
                totalSteps = stepDetector.getStepCount();
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

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("计步器运行中")
                .setContentText("已记录 " + totalSteps + " 步")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(int steps) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("智能计步器")
                .setContentText("今日步数: " + steps + " 步")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());
    }

    public void resetSteps() {
        totalSteps = 0;
        stepDetector.reset();
        broadcastStepUpdate(0);
        updateNotification(0);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        unregisterReceiver(systemReceiver);
        if (saveTimer != null) {
            saveTimer.cancel();
        }
        saveStepData(); // 销毁前保存数据
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    // 定时保存数据的计时器
    private class SaveTimer extends Thread {
        private long interval;
        private volatile boolean running = true;

        SaveTimer(long interval) {
            this.interval = interval;
        }

        void setInterval(long interval) {
            this.interval = interval;
        }

        @Override
        public void run() {
            while (running) {
                try {
                    Thread.sleep(interval);
                    saveStepData();
                    Log.d(TAG, "自动保存步数数据");
                } catch (InterruptedException e) {
                    Log.e(TAG, "保存线程中断", e);
                    running = false;
                }
            }
        }

        void cancel() {
            running = false;
            interrupt();
        }
    }
}