// StepDetectionAlgorithm.java
package com.example.stepcounter;

import java.util.LinkedList;

public class StepDetectionAlgorithm {

    private static final int WINDOW_SIZE = 10;
    private static final double STEP_THRESHOLD = 1.0; // 降低阈值
    private static final long MIN_STEP_INTERVAL = 250; // 最小步间间隔(毫秒)
    private static final long MAX_STEP_INTERVAL = 2000; // 最大步间间隔(毫秒)

    private final LinkedList<Double> accelBuffer = new LinkedList<>();
    private long lastStepTime = 0L;
    private int stepCount = 0;
    private double lastValue = 0.0;
    private boolean lastPeak = false;
    private double dynamicThreshold = STEP_THRESHOLD;

    // 动态阈值调整
    private final LinkedList<Double> peakValues = new LinkedList<>();
    private static final int PEAK_HISTORY_SIZE = 20;

    public boolean detectStep(float[] accelerationData, long timestamp) {
        if (accelerationData == null || accelerationData.length < 3) {
            return false;
        }

        // 计算加速度矢量幅度
        double magnitude = Math.sqrt(
                accelerationData[0] * accelerationData[0] +
                        accelerationData[1] * accelerationData[1] +
                        accelerationData[2] * accelerationData[2]
        );

        // 应用低通滤波去除高频噪声
        double filtered = lowPassFilter(magnitude, 0.8);

        // 减去重力影响 (约9.8 m/s²)
        double gravityFree = Math.abs(filtered - 9.8);

        // 应用移动平均滤波平滑数据
        double smoothed = movingAverageFilter(gravityFree);

        // 动态调整阈值
        updateDynamicThreshold(smoothed);

        // 检测步伐
        boolean isStep = detectStepAlgorithm(smoothed, timestamp);

        lastValue = smoothed;
        return isStep;
    }

    private double lowPassFilter(double current, double alpha) {
        return alpha * lastValue + (1 - alpha) * current;
    }

    private double movingAverageFilter(double newValue) {
        accelBuffer.add(newValue);
        if (accelBuffer.size() > WINDOW_SIZE) {
            accelBuffer.removeFirst();
        }

        double sum = 0.0;
        for (Double value : accelBuffer) {
            sum += value;
        }
        return sum / accelBuffer.size();
    }

    private void updateDynamicThreshold(double currentValue) {
        // 记录峰值
        if (currentValue > dynamicThreshold && !lastPeak) {
            peakValues.add(currentValue);
            if (peakValues.size() > PEAK_HISTORY_SIZE) {
                peakValues.removeFirst();
            }

            // 基于历史峰值调整阈值
            if (peakValues.size() >= 5) {
                double sum = 0;
                for (Double peak : peakValues) {
                    sum += peak;
                }
                double averagePeak = sum / peakValues.size();
                dynamicThreshold = averagePeak * 0.6; // 使用平均峰值的60%作为阈值
                dynamicThreshold = Math.max(dynamicThreshold, STEP_THRESHOLD); // 不低于基础阈值
            }
        }
    }

    private boolean detectStepAlgorithm(double currentValue, long timestamp) {
        long timeSinceLastStep = timestamp - lastStepTime;

        // 检查时间间隔是否在合理范围内
        if (timeSinceLastStep < MIN_STEP_INTERVAL) {
            lastPeak = (currentValue > dynamicThreshold);
            return false;
        }

        // 检查是否超过最大步间间隔(重置检测状态)
        if (timeSinceLastStep > MAX_STEP_INTERVAL) {
            lastPeak = false;
        }

        // 峰值检测算法
        boolean currentPeak = (currentValue > dynamicThreshold);
        boolean stepDetected = false;

        // 检测从低到高的上升沿
        if (currentPeak && !lastPeak) {
            stepDetected = true;
        }

        lastPeak = currentPeak;

        if (stepDetected) {
            stepCount++;
            lastStepTime = timestamp;
        }

        return stepDetected;
    }

    public int getStepCount() {
        return stepCount;
    }

    public void reset() {
        stepCount = 0;
        accelBuffer.clear();
        peakValues.clear();
        lastStepTime = 0L;
        lastValue = 0.0;
        lastPeak = false;
        dynamicThreshold = STEP_THRESHOLD;
    }
}