package com.monitor.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 深部水平位移数据类
 */
public class DeepHorizontalDisplacementData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String pointCode;
    private double initialValue;
    private double previousValue;
    private double currentValue;
    private double currentChange;
    private double cumulativeChange;
    private double changeRate;
    private String mileage;
    private double depth;
    private LocalDate measurementDate;
    private double historicalCumulative;
    
    /**
     * 默认构造方法
     */
    public DeepHorizontalDisplacementData() {
        pointCode = "";
        initialValue = 0.0;
        previousValue = 0.0;
        currentValue = 0.0;
        currentChange = 0.0;
        cumulativeChange = 0.0;
        changeRate = 0.0;
        mileage = "";
        depth = 0.0;
        measurementDate = LocalDate.now();
        historicalCumulative = 0.0;
    }
    
    /**
     * 带参数的构造方法
     */
    public DeepHorizontalDisplacementData(String pointCode, double initialValue, double previousValue, 
                                   double currentValue, LocalDate measurementDate) {
        this.pointCode = pointCode;
        this.initialValue = initialValue;
        this.previousValue = previousValue;
        this.currentValue = currentValue;
        this.measurementDate = measurementDate;
        this.mileage = "";
        this.depth = 0.0;
        this.historicalCumulative = 0.0;
        
        // 计算衍生值
        calculateDerivedValues();
    }
    
    /**
     * 计算衍生值：本次变化量、累计变化量和变化速率
     */
    public void calculateDerivedValues() {
        // 本次变化量 = 当前测值 - 上次测值
        currentChange = currentValue - previousValue;
        
        // 累计变化量 = 当前测值 - 初始测值
        cumulativeChange = currentValue - initialValue;
        
        // 变化速率不设置，因为没有时间间隔
        changeRate = currentChange;
    }
    
    /**
     * 计算衍生值，包括基于天数的变化速率
     * @param previousDate 上一次测量日期
     * @param currentDate 当前测量日期
     * @param customDays 自定义天数(0表示使用实际间隔天数)
     */
    public void calculateDerivedValues(LocalDate previousDate, LocalDate currentDate, int customDays) {
        // 本次变化量 = 当前测值 - 上次测值
        currentChange = currentValue - previousValue;
        
        // 累计变化量 = 当前测值 - 初始测值
        cumulativeChange = currentValue - initialValue;
        
        // 计算天数差
        long daysBetween = ChronoUnit.DAYS.between(previousDate, currentDate);
        if (daysBetween <= 0) daysBetween = 1; // 防止除以零错误
        
        // 如果指定了自定义天数，且不为0，则使用自定义天数
        if (customDays > 0) {
            changeRate = currentChange / customDays;
        } else {
            // 否则使用实际天数
            changeRate = currentChange / (double) daysBetween;
        }
    }
    
    // Getter和Setter方法
    public String getPointCode() {
        return pointCode;
    }
    
    public void setPointCode(String pointCode) {
        this.pointCode = pointCode;
    }
    
    public double getInitialValue() {
        return initialValue;
    }
    
    public void setInitialValue(double initialValue) {
        this.initialValue = initialValue;
    }
    
    public double getPreviousValue() {
        return previousValue;
    }
    
    public void setPreviousValue(double previousValue) {
        this.previousValue = previousValue;
    }
    
    public double getCurrentValue() {
        return currentValue;
    }
    
    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
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
    
    public double getDepth() {
        return depth;
    }
    
    public void setDepth(double depth) {
        this.depth = depth;
    }
    
    public LocalDate getMeasurementDate() {
        return measurementDate;
    }
    
    public void setMeasurementDate(LocalDate measurementDate) {
        this.measurementDate = measurementDate;
    }
    
    public double getHistoricalCumulative() {
        return historicalCumulative;
    }
    
    public void setHistoricalCumulative(double historicalCumulative) {
        this.historicalCumulative = historicalCumulative;
    }
} 