package com.monitor.model;

import java.io.Serializable;

/**
 * 砼支撑轴力测点模型类
 * 用于保存砼支撑轴力测点的属性
 */
public class ConcreteSupportAxialForcePoint implements Serializable {
    private static final long serialVersionUID = 1L;

    private String pointId;                 // 测点编号
    private String mileage;                 // 里程
    private double alarmValue;              // 报警值(KN)
    private double historicalCumulative;    // 历史累计值(KN)
    private int orderIndex;                 // 排序索引，用于保持导入顺序

    /**
     * 默认构造函数
     */
    public ConcreteSupportAxialForcePoint() {
    }

    /**
     * 带参数的构造函数
     */
    public ConcreteSupportAxialForcePoint(String pointId, String mileage,
                          double alarmValue, double historicalCumulative) {
        this.pointId = pointId;
        this.mileage = mileage;
        this.alarmValue = alarmValue;
        this.historicalCumulative = historicalCumulative;
    }

    /**
     * 带参数的构造函数（无历史累计）
     */
    public ConcreteSupportAxialForcePoint(String pointId, String mileage,
                          double alarmValue) {
        this(pointId, mileage, alarmValue, 0.0);
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

    public double getAlarmValue() {
        return alarmValue;
    }

    public void setAlarmValue(double alarmValue) {
        this.alarmValue = alarmValue;
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