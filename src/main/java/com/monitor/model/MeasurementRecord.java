package com.monitor.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 测量记录模型类
 */
public class MeasurementRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private double value;
    private String unit;
    private LocalDateTime measureTime;
    private String operator;
    private String comments;
    private int warningLevel; // 0=正常, 1-3表示不同警告级别
    
    public MeasurementRecord() {
    }
    
    public MeasurementRecord(String id, double value, LocalDateTime measureTime) {
        this.id = id;
        this.value = value;
        this.measureTime = measureTime;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public LocalDateTime getMeasureTime() {
        return measureTime;
    }

    public void setMeasureTime(LocalDateTime measureTime) {
        this.measureTime = measureTime;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public int getWarningLevel() {
        return warningLevel;
    }

    public void setWarningLevel(int warningLevel) {
        this.warningLevel = warningLevel;
    }
    
    @Override
    public String toString() {
        return String.format("%.2f %s [%s]", value, unit, measureTime);
    }
} 