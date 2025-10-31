// MainActivity.java
package com.example.stepcounter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextView stepCountText;
    private TextView statusText;
    private Button startButton;
    private Button resetButton;
    private Button btnSettings;
    // 在MainActivity中添加
    private StepGoalManager stepGoalManager;
    private ProgressBar stepProgressBar;
    private TextView goalProgressText;

    private boolean isCounting = false;
    private int currentSteps = 0;

    private static final int PERMISSION_REQUEST_CODE = 1001;

    // 添加共享偏好设置
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "PedometerSettings";

    private final BroadcastReceiver stepReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (StepCounterService.ACTION_STEP_UPDATE.equals(intent.getAction())) {    //只处理 "步数更新" 类型的广播
                int newSteps = intent.getIntExtra(StepCounterService.EXTRA_STEP_COUNT, 0);
                if (newSteps != currentSteps) {
                    currentSteps = newSteps;
                    updateStepDisplay(currentSteps);
                    statusText.setText("实时步数: " + currentSteps);
                    Log.d(TAG, "步数更新: " + currentSteps);

                    // 更新设置活动中的今日步数
                    updateTodaySteps(currentSteps);

                    // 前几步显示Toast提示
                    if (currentSteps <= 5) {
                        Toast.makeText(MainActivity.this,
                                "步数: " + currentSteps, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "Activity创建");

        // 初始化共享偏好设置
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        initializeViews();
        setupClickListeners();

        checkPermissions();
        
    }

    private void initializeViews() {
        stepCountText = findViewById(R.id.stepCountText);
        statusText = findViewById(R.id.statusText);
        startButton = findViewById(R.id.startButton);
        resetButton = findViewById(R.id.resetButton);
        btnSettings = findViewById(R.id.btnSettings);

        updateStepDisplay(0);
        statusText.setText("点击开始进行计步");
    }

    private void setupClickListeners() {
        startButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                toggleStepCounting();
            }
        });

        resetButton.setOnClickListener(v -> resetStepCount());

        // 设置按钮点击事件
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 在跳转前保存当前步数
                updateTodaySteps(currentSteps);

                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                        PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "计步权限已授予", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "权限授予成功");
            } else {
                Toast.makeText(this, "需要计步权限才能使用完整功能", Toast.LENGTH_LONG).show();
                Log.w(TAG, "权限被拒绝");
            }
        }
    }

    private void toggleStepCounting() {
        if (!isCounting) {
            startStepCounting();
        } else {
            stopStepCounting();
        }
    }

    private void startStepCounting() {
        try {
            Log.d(TAG, "启动计步服务");
            Intent serviceIntent = new Intent(this, StepCounterService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            isCounting = true;
            startButton.setText("停止计步");
            statusText.setText("计步器运行中...请正常行走测试");
            Toast.makeText(this, "计步器已启动，请手持手机正常行走", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e(TAG, "启动服务失败: " + e.getMessage(), e);
            Toast.makeText(this, "启动失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopStepCounting() {
        try {
            Log.d(TAG, "停止计步服务");
            Intent serviceIntent = new Intent(this, StepCounterService.class);
            stopService(serviceIntent);

            isCounting = false;
            startButton.setText("开始计步");
            statusText.setText("计步器已停止");
            Toast.makeText(this, "计步器已停止", Toast.LENGTH_SHORT).show();

            // 停止计步时保存记录
            saveDailyRecord();

        } catch (Exception e) {
            Log.e(TAG, "停止服务失败: " + e.getMessage(), e);
            Toast.makeText(this, "停止失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void resetStepCount() {
        Log.d(TAG, "重置步数");
        currentSteps = 0;
        updateStepDisplay(0);
        statusText.setText("步数已重置");
        Toast.makeText(this, "步数已重置", Toast.LENGTH_SHORT).show();

        // 更新设置中的今日步数
        updateTodaySteps(0);

        // 发送重置广播
        Intent resetIntent = new Intent("RESET_STEPS");
        LocalBroadcastManager.getInstance(this).sendBroadcast(resetIntent);
    }

    private void updateStepDisplay(int steps) {
        stepCountText.setText(String.valueOf(steps));
    }

    // 新增方法：更新今日步数到共享偏好设置
    private void updateTodaySteps(int steps) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("today_steps", steps);
        editor.apply();
        Log.d(TAG, "更新今日步数: " + steps);
    }

    // 新增方法：保存每日记录
    private void saveDailyRecord() {
        // 获取当前日期
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String today = sdf.format(new java.util.Date());

        // 保存今天的步数
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("today_steps", currentSteps);
        editor.putString("last_update_date", today);
        editor.apply();

        Log.d(TAG, "保存每日记录: " + currentSteps + " 步, 日期: " + today);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity恢复");
        // 注册广播接收器
        IntentFilter filter = new IntentFilter(StepCounterService.ACTION_STEP_UPDATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(stepReceiver, filter);

        // 恢复时从共享偏好设置加载今日步数
        int savedSteps = sharedPreferences.getInt("today_steps", 0);
        if (savedSteps > 0) {
            currentSteps = savedSteps;
            updateStepDisplay(currentSteps);
            statusText.setText("恢复步数: " + currentSteps);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Activity暂停");
        // 解注册广播接收器
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stepReceiver);

        // 暂停时保存当前步数
        updateTodaySteps(currentSteps);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity销毁");
        // 停止服务
        if (isCounting) {
            stopStepCounting();
        }

        // 销毁时保存记录
        saveDailyRecord();
    }
}