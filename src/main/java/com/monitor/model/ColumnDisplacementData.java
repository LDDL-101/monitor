package com.monitor.model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 立柱竖向位移数据模型类
 */
public class ColumnDisplacementData implements Serializable {
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
    public ColumnDisplacementData() {
    }

    /**
     * 带参数的构造函数
     *
     * @param pointCode 测点编号
     * @param initialElevation 初始高程
     * @param previousElevation 前期高程
     * @param currentElevation 本期高程
     * @param measurementDate 测量日期
     */
    public ColumnDisplacementData(String pointCode, double initialElevation, double previousElevation,
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
     * 计算派生值（本期变化、累计变化、变化速率）
     */
    public void calculateDerivedValues() {
        calculateDerivedValues(null, null, 0);
    }

    /**
     * 计算派生值（本期变化、累计变化、变化速率）
     * @param previousDate 前期测量日期，如果为null则不计算时间间隔
     * @param currentDate 本期测量日期，如果为null则不计算时间间隔
     * @param customDays 自定义天数，如果大于0则使用该值作为分母
     */
    public void calculateDerivedValues(LocalDate previousDate, LocalDate currentDate, int customDays) {
        // 本期变化 = (本期高程 - 前期高程) * 1000 (转换为毫米)
        this.currentChange = (this.currentElevation - this.previousElevation) * 1000;

        // 累计变化 = (本期高程 - 初始高程) * 1000 (转换为毫米)
        this.cumulativeChange = (this.currentElevation - this.initialElevation) * 1000;

        // 变化速率计算 (结果单位为毫米/天)
        if (this.previousElevation != 0) {
            // 如果指定了自定义天数
            if (customDays > 0) {
                this.changeRate = this.currentChange / customDays;
            }
            // 如果指定了日期，计算实际时间间隔
            else if (previousDate != null && currentDate != null) {
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(previousDate, currentDate);
                // 如果时间间隔小于1天，按1天计算
                if (daysBetween < 1) {
                    daysBetween = 1;
                }
                this.changeRate = this.currentChange / daysBetween;
            }
            // 如果没有日期信息，使用默认值
            else {
                this.changeRate = this.currentChange / 1.0; // 默认以mm/天为单位，按一天计算
            }
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
        return "ColumnDisplacementData{" +
                "pointCode='" + pointCode + '\'' +
                ", initialElevation=" + initialElevation +
                ", currentElevation=" + currentElevation +
                ", cumulativeChange=" + cumulativeChange +
                ", measurementDate=" + measurementDate +
                '}';
    }
} 