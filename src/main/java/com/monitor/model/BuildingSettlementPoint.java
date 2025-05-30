package com.monitor.model;

import java.io.Serializable;

/**
 * 建筑物沉降测点模型类
 * 用于保存建筑物沉降测点的属性
 */
public class BuildingSettlementPoint implements Serializable {
    private static final long serialVersionUID = 1L;

    private String pointId;                 // 测点编号
    private double initialElevation;        // 初始高程(m)
    private String mileage;                 // 里程
    private double rateWarningValue;        // 速率报警值(mm)
    private double accumulatedWarningValue; // 累计报警值(mm)
    private double historicalCumulative;    // 历史累计量(mm)
    private int orderIndex;                 // 排序索引，用于保持导入顺序

    /**
     * 默认构造函数
     */
    public BuildingSettlementPoint() {
    }

    /**
     * 带参数的构造函数
     */
    public BuildingSettlementPoint(String pointId, double initialElevation, String mileage,
                          double rateWarningValue, double accumulatedWarningValue,
                          double historicalCumulative) {
        this.pointId = pointId;
        this.initialElevation = initialElevation;
        this.mileage = mileage;
        this.rateWarningValue = rateWarningValue;
        this.accumulatedWarningValue = accumulatedWarningValue;
        this.historicalCumulative = historicalCumulative;
    }

    /**
     * 带参数的构造函数（无历史累计）
     */
    public BuildingSettlementPoint(String pointId, double initialElevation, String mileage,
                          double rateWarningValue, double accumulatedWarningValue) {
        this(pointId, initialElevation, mileage, rateWarningValue, accumulatedWarningValue, 0.0);
    }

    // Getters and Setters
    public String getPointId() {
        return pointId;
    }

    public void setPointId(String pointId) {
        this.pointId = pointId;
    }

    public double getInitialElevation() {
        return initialElevation;
    }

    public void setInitialElevation(double initialElevation) {
        this.initialElevation = initialElevation;
    }

    public String getMileage() {
        return mileage;
    }

    public void setMileage(String mileage) {
        this.mileage = mileage;
    }

    public double getRateWarningValue() {
        return rateWarningValue;
    }

    public void setRateWarningValue(double rateWarningValue) {
        this.rateWarningValue = rateWarningValue;
    }

    public double getAccumulatedWarningValue() {
        return accumulatedWarningValue;
    }

    public void setAccumulatedWarningValue(double accumulatedWarningValue) {
        this.accumulatedWarningValue = accumulatedWarningValue;
    }

    public double getHistoricalCumulative() {
        return historicalCumulative;
    }

    public void setHistoricalCumulative(double historicalCumulative) {
        this.historicalCumulative = historicalCumulative;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    @Override
    public String toString() {
        return pointId + " (" + initialElevation + "m)";
    }
} 