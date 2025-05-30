package com.monitor.model;

import java.io.Serializable;

/**
 * 钢支撑轴力测点模型类
 * 用于保存钢支撑轴力测点的属性
 */
public class SteelSupportAxialForcePoint implements Serializable {
    private static final long serialVersionUID = 1L;

    private String pointId;                 // 测点编号
    private String mileage;                 // 里程
    private double minValue;                // 最小值(KN)
    private double controlValue;            // 控制值(KN)
    private double historicalCumulative;    // 历史累计量(mm)
    private int orderIndex;                 // 排序索引，用于保持导入顺序

    /**
     * 默认构造函数
     */
    public SteelSupportAxialForcePoint() {
    }

    /**
     * 带参数的构造函数
     */
    public SteelSupportAxialForcePoint(String pointId, String mileage,
                          double minValue, double controlValue,
                          double historicalCumulative) {
        this.pointId = pointId;
        this.mileage = mileage;
        this.minValue = minValue;
        this.controlValue = controlValue;
        this.historicalCumulative = historicalCumulative;
    }

    /**
     * 带参数的构造函数（无历史累计）
     */
    public SteelSupportAxialForcePoint(String pointId, String mileage,
                          double minValue, double controlValue) {
        this(pointId, mileage, minValue, controlValue, 0.0);
    }

    // Getters and Setters
    public String getPointId() {
        return pointId;
    }

    public void setPointId(String pointId) {
        this.pointId = pointId;
    }

    public String getMileage() {
        return mileage;
    }

    public void setMileage(String mileage) {
        this.mileage = mileage;
    }

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    public double getControlValue() {
        return controlValue;
    }

    public void setControlValue(double controlValue) {
        this.controlValue = controlValue;
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
        return pointId + " (" + mileage + ")";
    }
} 