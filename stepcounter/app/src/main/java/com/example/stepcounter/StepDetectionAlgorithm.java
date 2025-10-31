package com.example.stepcounter;

import android.util.Log;

import java.util.LinkedList;

public class StepDetectionAlgorithm {
    private static final String TAG = "StepDetector";

    // 存放三轴数据
    private float[] oriValues = new float[3];
    // 用于存放计算阈值的波峰波谷差值
    private final float[] tempValue = new float[4];
    private int tempCount = 0;
    // 步频相关参数
    private static final long MIN_STEP_INTERVAL = 300; // 最小步间间隔(ms)
    private static final long MAX_STEP_INTERVAL = 2000; // 最大步间间隔(ms)
    // 动态阈值参数
    private static final float INITIAL_THRESHOLD = 1.3f;
    private static final float MIN_THRESHOLD = 1.0f;
    private static final float MAX_THRESHOLD = 3.0f;
    // 静止检测参数
    private static final float STILLNESS_THRESHOLD = 0.2f;
    private static final int STILLNESS_WINDOW = 20;
    private static final long STILLNESS_TIME = 3000;

    // 状态变量
    private boolean isDirectionUp = false;
    private int continueUpCount = 0;
    private int continueUpFormerCount = 0;
    private boolean lastStatus = false;
    private float peakOfWave = 0;
    private float valleyOfWave = 0;
    private long timeOfThisPeak = 0;
    private long timeOfLastPeak = 0;
    private long lastStepTime = 0;
    private int stepCount = 0;
    private float gravityNew = 0;
    private float gravityOld = 0;
    private float dynamicThreshold = INITIAL_THRESHOLD;

    // 静止检测
    private final LinkedList<Float> stillnessBuffer = new LinkedList<>();
    private long lastMovementTime = 0;
    private boolean isDeviceStill = false;

    // 低通滤波参数
    private static final float ALPHA = 0.8f;
    private float[] filteredValues = new float[3];

    public boolean detectStep(float[] accelerationData, long timestamp) {
        if (accelerationData == null || accelerationData.length < 3) {
            return false;
        }

        // 应用低通滤波
        for (int i = 0; i < 3; i++) {
            filteredValues[i] = lowPassFilter(accelerationData[i], filteredValues[i]);
        }

        // 计算加速度矢量幅度
        gravityNew = (float) Math.sqrt(
                filteredValues[0] * filteredValues[0] +
                        filteredValues[1] * filteredValues[1] +
                        filteredValues[2] * filteredValues[2]
        );

        // 更新静止检测
        updateStillnessDetection(gravityNew, timestamp);
        if (isDeviceStill) {
            return false;
        }

        // 检测步伐
        boolean stepDetected = detectorNewStep(timestamp);
        gravityOld = gravityNew;
        return stepDetected;
    }

    private float lowPassFilter(float current, float last) {
        return ALPHA * last + (1 - ALPHA) * current;
    }

    private void updateStillnessDetection(float value, long timestamp) {
        stillnessBuffer.add(value);
        if (stillnessBuffer.size() > STILLNESS_WINDOW) {
            stillnessBuffer.removeFirst();
        }

        if (stillnessBuffer.size() < STILLNESS_WINDOW) {
            return;
        }

        // 计算方差
        float mean = 0;
        for (float v : stillnessBuffer) mean += v;
        mean /= stillnessBuffer.size();

        float variance = 0;
        for (float v : stillnessBuffer) {
            variance += Math.pow(v - mean, 2);
        }
        variance /= stillnessBuffer.size();

        // 判断静止状态
        if (variance < STILLNESS_THRESHOLD) {
            if (timestamp - lastMovementTime > STILLNESS_TIME) {
                isDeviceStill = true;
                resetStepState(); // 静止时重置检测状态
            }
        } else {
            lastMovementTime = timestamp;
            isDeviceStill = false;
        }
    }

    private boolean detectorNewStep(long timestamp) {
        if (gravityOld == 0) {
            return false;
        }

        boolean isPeak = detectorPeak(gravityNew, gravityOld);
        if (isPeak) {
            timeOfLastPeak = timeOfThisPeak;
            timeOfThisPeak = timestamp;

            // 检查时间间隔
            long interval = timeOfThisPeak - timeOfLastPeak;
            if (interval >= MIN_STEP_INTERVAL && interval <= MAX_STEP_INTERVAL) {
                // 检查波峰波谷差值
                if (peakOfWave - valleyOfWave >= dynamicThreshold) {
                    stepCount++;
                    lastStepTime = timestamp;
                    updateThreshold(peakOfWave - valleyOfWave);
                    Log.d(TAG, "检测到步伐! 总步数: " + stepCount + ", 阈值: " + dynamicThreshold);
                    return true;
                }
            } else if (interval > MAX_STEP_INTERVAL) {
                // 超过最大间隔，更新阈值但不计步
                updateThreshold(peakOfWave - valleyOfWave);
            }
        }
        return false;
    }

    private boolean detectorPeak(float newValue, float oldValue) {
        lastStatus = isDirectionUp;

        if (newValue >= oldValue) {
            isDirectionUp = true;
            continueUpCount++;
        } else {
            continueUpFormerCount = continueUpCount;
            continueUpCount = 0;
            isDirectionUp = false;
        }

        // 波峰判断条件
        if (!isDirectionUp && lastStatus && continueUpFormerCount >= 2) {
            peakOfWave = oldValue;
            return true;
        } else if (!lastStatus && isDirectionUp) {
            valleyOfWave = oldValue;
        }
        return false;
    }

    private void updateThreshold(float peakValleyDiff) {
        if (tempCount < tempValue.length) {
            tempValue[tempCount] = peakValleyDiff;
            tempCount++;
        } else {
            // 计算平均值并梯度化阈值
            float avg = 0;
            for (float v : tempValue) avg += v;
            avg /= tempValue.length;

            // 梯度调整阈值
            if (avg >= 8) dynamicThreshold = 4.3f;
            else if (avg >= 7) dynamicThreshold = 3.3f;
            else if (avg >= 4) dynamicThreshold = 2.3f;
            else if (avg >= 3) dynamicThreshold = 2.0f;
            else dynamicThreshold = 1.3f;

            // 阈值范围限制
            dynamicThreshold = Math.max(dynamicThreshold, MIN_THRESHOLD);
            dynamicThreshold = Math.min(dynamicThreshold, MAX_THRESHOLD);

            // 滚动更新
            System.arraycopy(tempValue, 1, tempValue, 0, tempValue.length - 1);
            tempValue[tempValue.length - 1] = peakValleyDiff;
        }
    }

    private void resetStepState() {
        continueUpCount = 0;
        continueUpFormerCount = 0;
        lastStatus = false;
        peakOfWave = 0;
        valleyOfWave = 0;
    }

    public int getStepCount() {
        return stepCount;
    }

    public void reset() {
        stepCount = 0;
        gravityOld = 0;
        lastStepTime = 0;
        dynamicThreshold = INITIAL_THRESHOLD;
        tempCount = 0;
        stillnessBuffer.clear();
        resetStepState();
        isDeviceStill = false;
        lastMovementTime = 0;
    }

    public boolean isDeviceStill() {
        return isDeviceStill;
    }

    public float getCurrentThreshold() {
        return dynamicThreshold;
    }
}