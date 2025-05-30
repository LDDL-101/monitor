package com.monitor.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 桩顶水平位移数据类
 */
public class PileTopHorizontalDisplacementData implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String pointCode;            // 测点编号
    private double initialElevation;     // 初始高程(m)
    private double previousElevation;    // 上次高程(m)
    private double currentElevation;     // 本次高程(m)
    private double currentChange;        // 本次变化量(mm)
    private double cumulativeChange;     // 累计变化量(mm)
    private double changeRate;           // 变化速率(mm/d)
    private String mileage;              // 里程
    private double historicalCumulative; // 历史累计量(mm)
    private LocalDate measurementDate;   // 测量日期
    
    /**
     * 默认构造函数
     */
    public PileTopHorizontalDisplacementData() {
        this.pointCode = "";
        this.initialElevation = 0.0;
        this.previousElevation = 0.0;
        this.currentElevation = 0.0;
        this.currentChange = 0.0;
        this.cumulativeChange = 0.0;
        this.changeRate = 0.0;
        this.mileage = "";
        this.historicalCumulative = 0.0;
        this.measurementDate = LocalDate.now();
    }
    
    /**
     * 构造函数
     * 
     * @param pointCode 测点编号
     * @param initialElevation 初始高程(m)
     * @param previousElevation 上次高程(m)
     * @param currentElevation 本次高程(m)
     * @param measurementDate 测量日期
     */
    public PileTopHorizontalDisplacementData(String pointCode, double initialElevation, 
            double previousElevation, double currentElevation, LocalDate measurementDate) {
        this.pointCode = pointCode;
        this.initialElevation = initialElevation;
        this.previousElevation = previousElevation;
        this.currentElevation = currentElevation;
        this.measurementDate = measurementDate;
        this.mileage = "";
        this.historicalCumulative = 0.0;
        
        // 计算本次变化量和累计变化量 (mm)
        this.currentChange = (currentElevation - previousElevation) * 1000;
        this.cumulativeChange = (currentElevation - initialElevation) * 1000;
        
        // 默认变化速率为0
        this.changeRate = 0.0;
    }
    
    /**
     * 计算派生值
     */
    public void calculateDerivedValues() {
        // 计算本次变化量和累计变化量 (mm)
        this.currentChange = (currentElevation - previousElevation) * 1000;
        this.cumulativeChange = (currentElevation - initialElevation) * 1000;
        
        // 默认变化速率为0
        this.changeRate = 0.0;
    }
    
    /**
     * 计算派生值，包含速率计算
     * 
     * @param previousDate 上次测量日期
     * @param currentDate 当前测量日期
     * @param customDays 自定义天数 (0表示使用实际天数)
     */
    public void calculateDerivedValues(LocalDate previousDate, LocalDate currentDate, int customDays) {
        // 计算本次变化量和累计变化量 (mm)
        this.currentChange = (currentElevation - previousElevation) * 1000;
        this.cumulativeChange = (currentElevation - initialElevation) * 1000;
        
        // 计算变化速率 (mm/d)
        if (customDays > 0) {
            // 使用自定义天数
            this.changeRate = currentChange / customDays;
        } else {
            // 使用实际天数
            long days = ChronoUnit.DAYS.between(previousDate, currentDate);
            // 如果是同一天上传的数据，使用1天作为除数
            if (days <= 0) {
                days = 1;
            }
            this.changeRate = currentChange / days;
        }
    }
    
    /**
     * 获取测点编号
     */
    public String getPointCode() {
        return pointCode;
    }
    
    /**
     * 设置测点编号
     */
    public void setPointCode(String pointCode) {
        this.pointCode = pointCode;
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
     * 获取上次高程(m)
     */
    public double getPreviousElevation() {
        return previousElevation;
    }
    
    /**
     * 设置上次高程(m)
     */
    public void setPreviousElevation(double previousElevation) {
        this.previousElevation = previousElevation;
    }
    
    /**
     * 获取本次高程(m)
     */
    public double getCurrentElevation() {
        return currentElevation;
    }
    
    /**
     * 设置本次高程(m)
     */
    public void setCurrentElevation(double currentElevation) {
        this.currentElevation = currentElevation;
    }
    
    /**
     * 获取本次变化量(mm)
     */
    public double getCurrentChange() {
        return currentChange;
    }
    
    /**
     * 设置本次变化量(mm)
     */
    public void setCurrentChange(double currentChange) {
        this.currentChange = currentChange;
    }
    
    /**
     * 获取累计变化量(mm)
     */
    public double getCumulativeChange() {
        return cumulativeChange;
    }
    
    /**
     * 设置累计变化量(mm)
     */
    public void setCumulativeChange(double cumulativeChange) {
        this.cumulativeChange = cumulativeChange;
    }
    
    /**
     * 获取变化速率(mm/d)
     */
    public double getChangeRate() {
        return changeRate;
    }
    
    /**
     * 设置变化速率(mm/d)
     */
    public void setChangeRate(double changeRate) {
        this.changeRate = changeRate;
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
     * 获取测量日期
     */
    public LocalDate getMeasurementDate() {
        return measurementDate;
    }
    
    /**
     * 设置测量日期
     */
    public void setMeasurementDate(LocalDate measurementDate) {
        this.measurementDate = measurementDate;
    }
    
    @Override
    public String toString() {
        return "PileTopHorizontalDisplacementData [pointCode=" + pointCode + ", initialElevation=" + initialElevation
                + ", previousElevation=" + previousElevation + ", currentElevation=" + currentElevation
                + ", currentChange=" + currentChange + ", cumulativeChange=" + cumulativeChange + ", changeRate="
                + changeRate + ", mileage=" + mileage + ", historicalCumulative=" + historicalCumulative
                + ", measurementDate=" + measurementDate + "]";
    }
} 