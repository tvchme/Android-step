package com.example.stepcounter;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private EditText etWeight, etHeight, etDailyGoal;
    private Button btnSave;
    private TextView tvTodaySteps, tvYesterdaySteps, tvDayBeforeSteps, tvStats;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "PedometerSettings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 初始化视图
        initViews();

        // 加载保存的设置
        loadSettings();

        // 加载历史记录
        loadStepHistory();

        // 设置按钮点击事件
        setupButtonListeners();
    }

    private void initViews() {
        etWeight = findViewById(R.id.etWeight);
        etHeight = findViewById(R.id.etHeight);
        etDailyGoal = findViewById(R.id.etDailyGoal);
        btnSave = findViewById(R.id.btnSave);
        tvTodaySteps = findViewById(R.id.tvTodaySteps);
        tvYesterdaySteps = findViewById(R.id.tvYesterdaySteps);
        tvDayBeforeSteps = findViewById(R.id.tvDayBeforeSteps);
        tvStats = findViewById(R.id.tvStats);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    private void setupButtonListeners() {
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });
    }

    private void loadSettings() {
        // 从SharedPreferences加载设置，如果没有则使用默认值
        float weight = sharedPreferences.getFloat("weight", 70.0f);
        float height = sharedPreferences.getFloat("height", 170.0f);
        int dailyGoal = sharedPreferences.getInt("daily_goal", 10000);

        etWeight.setText(String.valueOf(weight));
        etHeight.setText(String.valueOf(height));
        etDailyGoal.setText(String.valueOf(dailyGoal));
    }

    private void saveSettings() {
        try {
            float weight = Float.parseFloat(etWeight.getText().toString());
            float height = Float.parseFloat(etHeight.getText().toString());
            int dailyGoal = Integer.parseInt(etDailyGoal.getText().toString());

            if (weight <= 0 || height <= 0 || dailyGoal <= 0) {
                Toast.makeText(this, "请输入有效的数值", Toast.LENGTH_SHORT).show();
                return;
            }

            // 保存到SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putFloat("weight", weight);
            editor.putFloat("height", height);
            editor.putInt("daily_goal", dailyGoal);
            editor.apply();

            Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();

            // 更新健康信息显示
            updateHealthInfo(weight, height);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的数值", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateHealthInfo(float weight, float height) {
        // 计算BMI
        float heightInMeter = height / 100;
        float bmi = weight / (heightInMeter * heightInMeter);

        String bmiCategory;
        if (bmi < 18.5) {
            bmiCategory = "偏瘦";
        } else if (bmi < 24) {
            bmiCategory = "正常";
        } else if (bmi < 28) {
            bmiCategory = "偏胖";
        } else {
            bmiCategory = "肥胖";
        }

        String healthInfo = String.format(Locale.getDefault(),
                "BMI: %.1f (%s)", bmi, bmiCategory);

        // 可以在界面上显示这些信息，或者用Toast显示
        Toast.makeText(this, "健康信息: " + healthInfo, Toast.LENGTH_LONG).show();
    }

    private void loadStepHistory() {
        // 加载今天的步数（从主活动传递过来或从SharedPreferences读取）
        int todaySteps = sharedPreferences.getInt("today_steps", 0);
        int yesterdaySteps = sharedPreferences.getInt("yesterday_steps", 5240);
        int dayBeforeSteps = sharedPreferences.getInt("day_before_steps", 7890);

        tvTodaySteps.setText(todaySteps + " 步");
        tvYesterdaySteps.setText(yesterdaySteps + " 步");
        tvDayBeforeSteps.setText(dayBeforeSteps + " 步");

        // 更新统计信息
        updateStatistics(todaySteps, yesterdaySteps, dayBeforeSteps);
    }

    private void updateStatistics(int today, int yesterday, int dayBefore) {
        int total = today + yesterday + dayBefore;
        int average = total / 3;
        int dailyGoal = sharedPreferences.getInt("daily_goal", 10000);

        int goalDays = 0;
        if (today >= dailyGoal) goalDays++;
        if (yesterday >= dailyGoal) goalDays++;
        if (dayBefore >= dailyGoal) goalDays++;

        float achievementRate = (goalDays * 100f) / 3;

        String stats = String.format(Locale.getDefault(),
                "最近3天统计:\n" +
                        "总步数: %,d 步\n" +
                        "平均每日: %,d 步\n" +
                        "目标达成率: %.1f%%",
                total, average, achievementRate);

        tvStats.setText(stats);
    }

    // 从主活动调用此方法来更新今天的步数
    public static void updateTodaySteps(SharedPreferences sharedPreferences, int steps) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("today_steps", steps);
        editor.apply();
    }

    // 从主活动调用此方法来保存历史记录（每天结束时调用）
    public static void saveDailyRecord(SharedPreferences sharedPreferences, int steps) {
        // 获取当前日期
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());

        // 保存今天的步数
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("today_steps", steps);
        editor.putString("last_update_date", today);
        editor.apply();

        // 这里可以添加逻辑来保存更久的历史记录
        // 例如将昨天的数据移到前天的位置等
    }
}