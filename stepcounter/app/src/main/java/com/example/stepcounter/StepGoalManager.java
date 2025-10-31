package com.example.stepcounter;// StepGoalManager.java
import android.content.Context;
import android.content.SharedPreferences;

public class StepGoalManager {
    private static final String PREF_NAME = "StepGoalPrefs";
    private static final String KEY_DAILY_GOAL = "daily_goal";
    private static final int DEFAULT_GOAL = 10000; // 默认目标10000步

    private SharedPreferences sharedPreferences;

    public StepGoalManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // 保存步数目标
    public void saveDailyGoal(int goal) {
        sharedPreferences.edit().putInt(KEY_DAILY_GOAL, goal).apply();
    }

    // 获取当前步数目标
    public int getDailyGoal() {
        return sharedPreferences.getInt(KEY_DAILY_GOAL, DEFAULT_GOAL);
    }

    // 计算目标完成百分比
    public int calculateProgress(int currentSteps) {
        int goal = getDailyGoal();
        if (goal <= 0) return 0;
        int progress = (int) (((float) currentSteps / goal) * 100);
        return Math.min(progress, 100); // 最多100%
    }
}