package com.monitor.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 桩顶竖向位移数据模型类
 */
public class PileDisplacementData implements Serializable {
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

    public PileDisplacementData() {
    }

    public PileDisplacementData(String pointCode, double initialElevation, double previousElevation,
                         double currentElevation, LocalDate measurementDate) {
        this.pointCode = pointCode;
        this.initialElevation = initialElevation;
        this.previousElevation = previousElevation;
        this.currentElevation = currentElevation;
        this.measurementDate = measurementDate;
        
        // 计算派生值
        calculateDerivedValues();
    }

    /**
     * 计算派生值：本期变化、累计变化、变化速率
     */
    public void calculateDerivedValues() {
        // 本期变化(mm) = 本期高程 - 前期高程，数据单位是m，乘以1000转换为mm，向下移动为负值，向上移动为正值
        this.currentChange = (currentElevation - previousElevation) * 1000;
        
        // 累计变化(mm) = 本期高程 - 初始高程，数据单位是m，乘以1000转换为mm，向下移动为负值，向上移动为正值
        this.cumulativeChange = (currentElevation - initialElevation) * 1000;
        
        // 变化速率默认为每天速率(mm/day)，可通过自定义天数计算
        this.changeRate = currentChange;
    }

    /**
     * 使用自定义天数计算变化速率
     * @param previousDate 前期测量日期
     * @param currentDate 本期测量日期
     * @param customDays 自定义天数，如果为0，则使用实际间隔天数
     */
    public void calculateDerivedValues(LocalDate previousDate, LocalDate currentDate, int customDays) {
        // 首先计算本期变化、累计变化
        calculateDerivedValues();
        
        // 然后计算速率
        if (previousDate != null && currentDate != null) {
            long daysBetween;
            
            if (customDays > 0) {
                // 使用自定义天数
                daysBetween = customDays;
            } else {
                // 计算实际间隔天数
                daysBetween = ChronoUnit.DAYS.between(previousDate, currentDate);
                if (daysBetween <= 0) {
                    daysBetween = 1; // 至少为1天，避免除以零
                }
            }
            
            // 变化速率(mm/day) = 本期变化(mm) / 天数
            this.changeRate = currentChange / daysBetween;
        } else {
            // 如果没有日期信息，速率等于本期变化
            this.changeRate = currentChange;
        }
    }

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
        return "PileDisplacementData{" +
                "pointCode='" + pointCode + '\'' +
                ", currentElevation=" + currentElevation +
                ", measurementDate=" + measurementDate +
                ", currentChange=" + currentChange +
                ", cumulativeChange=" + cumulativeChange +
                '}';
    }
} 