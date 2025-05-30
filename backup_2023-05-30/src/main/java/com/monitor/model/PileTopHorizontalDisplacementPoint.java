package com.monitor.model;

import java.io.Serializable;

/**
 * 桩顶水平位移测点类
 */
public class PileTopHorizontalDisplacementPoint implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String pointId;              // 测点编号
    private double initialElevation;     // 初始高程(m)
    private String mileage;              // 里程
    private double rateWarning;          // 速率报警值(mm)
    private double accumulatedWarning;   // 累计报警值(mm)
    private double historicalCumulative; // 历史累计量(mm)
    private int orderIndex;              // 排序索引，用于保持导入顺序
    
    /**
     * 默认构造函数
     */
    public PileTopHorizontalDisplacementPoint() {
        this("", 0.0, "", 0.0, 0.0);
        this.orderIndex = 0;
    }
    
    /**
     * 构造函数
     * 
     * @param pointId 测点编号
     * @param initialElevation 初始高程(m)
     * @param mileage 里程
     * @param rateWarning 速率报警值(mm)
     * @param accumulatedWarning 累计报警值(mm)
     */
    public PileTopHorizontalDisplacementPoint(String pointId, double initialElevation, String mileage, 
            double rateWarning, double accumulatedWarning) {
        this.pointId = pointId;
        this.initialElevation = initialElevation;
        this.mileage = mileage;
        this.rateWarning = rateWarning;
        this.accumulatedWarning = accumulatedWarning;
        this.historicalCumulative = 0.0;
        this.orderIndex = 0;
    }
    
    /**
     * 获取测点编号
     */
    public String getPointId() {
        return pointId;
    }
    
    /**
     * 设置测点编号
     */
    public void setPointId(String pointId) {
        this.pointId = pointId;
    }
    
    /**
     * 获取初始高程(m)
     */
    public double getInitialElevation() {
        return initialElevation;
    }
    
    /**
     * 设置初始高程(m)
     */
    public void setInitialElevation(double initialElevation) {
        this.initialElevation = initialElevation;
    }
    
    /**
     * 获取里程
     */
    public String getMileage() {
        return mileage;
    }
    
    /**
     * 设置里程
     */
    public void setMileage(String mileage) {
        this.mileage = mileage;
    }
    
    /**
     * 获取速率报警值(mm)
     */
    public double getRateWarning() {
        return rateWarning;
    }
    
    /**
     * 设置速率报警值(mm)
     */
    public void setRateWarning(double rateWarning) {
        this.rateWarning = rateWarning;
    }
    
    /**
     * 获取累计报警值(mm)
     */
    public double getAccumulatedWarning() {
        return accumulatedWarning;
    }
    
    /**
     * 设置累计报警值(mm)
     */
    public void setAccumulatedWarning(double accumulatedWarning) {
        this.accumulatedWarning = accumulatedWarning;
    }
    
    /**
     * 获取历史累计量(mm)
     */
    public double getHistoricalCumulative() {
        return historicalCumulative;
    }
    
    /**
     * 设置历史累计量(mm)
     */
    public void setHistoricalCumulative(double historicalCumulative) {
        this.historicalCumulative = historicalCumulative;
    }
    
    /**
     * 获取排序索引
     */
    public int getOrderIndex() {
        return orderIndex;
    }
    
    /**
     * 设置排序索引
     */
    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }
    
    @Override
    public String toString() {
        return "PileTopHorizontalDisplacementPoint [pointId=" + pointId + ", initialElevation=" + initialElevation
                + ", mileage=" + mileage + ", rateWarning=" + rateWarning + ", accumulatedWarning=" + accumulatedWarning
                + ", historicalCumulative=" + historicalCumulative + ", orderIndex=" + orderIndex + "]";
    }
} 