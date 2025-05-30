package com.monitor.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 钢支撑轴力数据模型类
 */
public class SteelSupportAxialForceData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String pointCode;         // 测点编号
    private double previousForce;     // 上次轴力(KN)
    private double currentForce;      // 本次轴力(KN)
    private double currentChange;     // 本次变化量(KN)
    private String mileage;           // 里程
    private double historicalCumulative; // 历史累计值(mm)
    private LocalDate measurementDate;   // 测量日期

    /**
     * 默认构造函数
     */
    public SteelSupportAxialForceData() {
    }

    /**
     * 带参数的构造函数
     */
    public SteelSupportAxialForceData(String pointCode, double previousForce,
                         double currentForce, LocalDate measurementDate) {
        this.pointCode = pointCode;
        this.previousForce = previousForce;
        this.currentForce = currentForce;
        this.measurementDate = measurementDate;
    }

    /**
     * 计算派生值
     */
    public void calculateDerivedValues() {
        // 本次变化量 = 本次轴力 - 上次轴力
        this.currentChange = this.currentForce - this.previousForce;
    }

    /**
     * 计算派生值 - 带自定义天数
     */
    public void calculateDerivedValues(LocalDate previousDate, LocalDate currentDate, int customDays) {
        // 本次变化量 = 本次轴力 - 上次轴力
        this.currentChange = this.currentForce - this.previousForce;
    }

    // Getters and Setters
    public String getPointCode() {
        return pointCode;
    }

    public void setPointCode(String pointCode) {
        this.pointCode = pointCode;
    }

    public double getPreviousForce() {
        return previousForce;
    }

    public void setPreviousForce(double previousForce) {
        this.previousForce = previousForce;
    }

    public double getCurrentForce() {
        return currentForce;
    }

    public void setCurrentForce(double currentForce) {
        this.currentForce = currentForce;
    }

    public double getCurrentChange() {
        return currentChange;
    }

    public void setCurrentChange(double currentChange) {
        this.currentChange = currentChange;
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
        return String.format("%s: %.2fKN, 变化量: %.2fKN, 日期: %s", 
                pointCode, currentForce, currentChange, measurementDate);
    }
} 