package com.example.stepcounter.util;

import android.content.Context;
import android.content.SharedPreferences;

public class GoalUtils {
    // 存储键名
    private static final String PREF_NAME = "StepGoalPref";
    private static final String KEY_GOAL = "daily_goal";
    private static final int DEFAULT_GOAL = 8000; // 默认目标

    // 保存目标步数
    public static void saveStepGoal(Context context, int goal) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_GOAL, goal).apply();
    }

    // 获取目标步数（默认返回8000）
    public static int getStepGoal(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_GOAL, DEFAULT_GOAL);
    }
}