package com.monitor.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 地下水位数据模型类
 * 用于存储单个测点在特定日期的水位数据
 */
public class GroundwaterLevelData implements Serializable {
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
    
    public GroundwaterLevelData() {
    }
    
    public GroundwaterLevelData(String pointCode, double initialElevation, double previousElevation,
                         double currentElevation, LocalDate measurementDate) {
        this.pointCode = pointCode;
        this.initialElevation = initialElevation;
        this.previousElevation = previousElevation;
        this.currentElevation = currentElevation;
        this.measurementDate = measurementDate;
        
        // 计算衍生值
        calculateDerivedValues();
    }
    
    /**
     * 计算变化值和累计变化值（毫米单位）
     */
    public void calculateDerivedValues() {
        // 本期变化量（mm）= (前期高程 - 本期高程) * 1000
        // 注意：对于水位，向下变化（高程减小）表示水位上升
        this.currentChange = (previousElevation - currentElevation) * 1000;
        
        // 累计变化量（mm）= (初始高程 - 本期高程) * 1000 + 历史累计值
        this.cumulativeChange = (initialElevation - currentElevation) * 1000 + historicalCumulative;
        
        // 变化速率默认为每天变化量
        this.changeRate = this.currentChange;
    }
    
    /**
     * 计算变化值和累计变化值，支持指定日期计算变化速率
     * @param previousDate 前期测量日期
     * @param currentDate 本期测量日期
     * @param customDays 自定义速率计算天数，为0时按实际天数计算
     */
    public void calculateDerivedValues(LocalDate previousDate, LocalDate currentDate, int customDays) {
        // 本期变化量（mm）= (前期高程 - 本期高程) * 1000
        this.currentChange = (previousElevation - currentElevation) * 1000;
        
        // 累计变化量（mm）= (初始高程 - 本期高程) * 1000 + 历史累计值
        this.cumulativeChange = (initialElevation - currentElevation) * 1000 + historicalCumulative;
        
        // 计算天数差
        long daysBetween = 1; // 默认为1天，避免除以0
        
        if (previousDate != null && currentDate != null) {
            daysBetween = ChronoUnit.DAYS.between(previousDate, currentDate);
            if (daysBetween <= 0) {
                daysBetween = 1; // 确保至少为1天
            }
        }
        
        // 如果指定了自定义天数，使用自定义天数
        if (customDays > 0) {
            daysBetween = customDays;
        }
        
        // 变化速率（mm/天）= 本期变化量 / 天数
        this.changeRate = this.currentChange / daysBetween;
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
        return String.format("%s: %.4f m (变化: %.2f mm)", pointCode, currentElevation, currentChange);
    }
} 