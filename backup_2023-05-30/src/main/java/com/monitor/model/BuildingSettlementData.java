package com.monitor.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 建筑物沉降数据模型类
 * 用于保存建筑物沉降测量数据
 */
public class BuildingSettlementData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String pointCode;         // 测点编号
    private double initialElevation;  // 初始高程
    private double previousElevation; // 前期高程
    private double currentElevation;  // 本期高程
    private double currentChange;     // 本期变化
    private double cumulativeChange;  // 累计变化
    private double changeRate;        // 变化速率
    private String mileage;           // 里程
    private double historicalCumulative; // 历史累计
    private LocalDate measurementDate;   // 测量日期

    /**
     * 默认构造函数
     */
    public BuildingSettlementData() {
    }

    /**
     * 带参数的构造函数
     */
    public BuildingSettlementData(String pointCode, double initialElevation, double previousElevation,
                         double currentElevation, LocalDate measurementDate) {
        this.pointCode = pointCode;
        this.initialElevation = initialElevation;
        this.previousElevation = previousElevation;
        this.currentElevation = currentElevation;
        this.measurementDate = measurementDate;
        this.historicalCumulative = 0.0;
        calculateDerivedValues();
    }

    /**
     * 计算派生值（使用标准计算方法）
     */
    public void calculateDerivedValues() {
        // 本期变化量（mm）：前期高程 - 本期高程，毫米为单位
        // 注意：建筑物沉降以地面下沉为正值，所以是前期高程减去本期高程
        this.currentChange = (previousElevation - currentElevation) * 1000.0;

        // 累计变化量（mm）：初始高程 - 本期高程，毫米为单位
        this.cumulativeChange = (initialElevation - currentElevation) * 1000.0;

        // 变化速率（mm/d）：本期变化量 / 30（默认天数），毫米每天
        this.changeRate = currentChange / 30.0;
    }

    /**
     * 计算派生值（考虑实际测量日期）
     */
    public void calculateDerivedValues(LocalDate previousDate, LocalDate currentDate, int customDays) {
        // 本期变化量（mm）：前期高程 - 本期高程，毫米为单位
        // 注意：建筑物沉降以地面下沉为正值，所以是前期高程减去本期高程
        this.currentChange = (previousElevation - currentElevation) * 1000.0;

        // 累计变化量（mm）：初始高程 - 本期高程，毫米为单位
        this.cumulativeChange = (initialElevation - currentElevation) * 1000.0;

        // 变化速率（mm/d）
        // 如果自定义天数为0，使用实际测量日期间隔
        if (customDays == 0 && previousDate != null && currentDate != null) {
            long daysBetween = ChronoUnit.DAYS.between(previousDate, currentDate);
            if (daysBetween > 0) {
                this.changeRate = currentChange / daysBetween;
            } else {
                this.changeRate = 0.0; // 同一天的测量，速率为0
            }
        } else if (customDays > 0) {
            // 使用自定义天数
            this.changeRate = currentChange / customDays;
        } else {
            // 默认使用30天
            this.changeRate = currentChange / 30.0;
        }
    }

    // Getters and Setters
    public String getPointCode() {
        return pointCode;
    }

    public void setPointCode(String pointCode) {
        this.pointCode = pointCode;
    }

    public double getInitialElevation() {
        return initialElevation;
    }

    public void setInitialElevation(double initialElevation) {
        this.initialElevation = initialElevation;
    }

    public double getPreviousElevation() {
        return previousElevation;
    }

    public void setPreviousElevation(double previousElevation) {
        this.previousElevation = previousElevation;
    }

    public double getCurrentElevation() {
        return currentElevation;
    }

    public void setCurrentElevation(double currentElevation) {
        this.currentElevation = currentElevation;
    }

    public double getCurrentChange() {
        return currentChange;
    }

    public void setCurrentChange(double currentChange) {
        this.currentChange = currentChange;
    }

    public double getCumulativeChange() {
        return cumulativeChange;
    }

    public void setCumulativeChange(double cumulativeChange) {
        this.cumulativeChange = cumulativeChange;
    }

    public double getChangeRate() {
        return changeRate;
    }

    public void setChangeRate(double changeRate) {
        this.changeRate = changeRate;
    }

    public String getMileage() {
        return mileage;
    }

    public void setMileage(String mileage) {
        this.mileage = mileage;
    }

    public double getHistoricalCumulative() {
        return historicalCumulative;
    }

    public void setHistoricalCumulative(double historicalCumulative) {
        this.historicalCumulative = historicalCumulative;
    }

    public LocalDate getMeasurementDate() {
        return measurementDate;
    }

    public void setMeasurementDate(LocalDate measurementDate) {
        this.measurementDate = measurementDate;
    }

    @Override
    public String toString() {
        return String.format("%s: %.3fm (变化: %.2fmm)", pointCode, currentElevation, currentChange);
    }
} 