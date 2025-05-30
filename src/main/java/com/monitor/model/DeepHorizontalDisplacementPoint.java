package com.monitor.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 深部水平位移测点配置类
 */
public class DeepHorizontalDisplacementPoint implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String pointId;
    private String mileage;
    private List<Measurement> measurements = new ArrayList<>();
    
    /**
     * 深部水平位移测量点（每个深度的测量）
     */
    public static class Measurement implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private double depth;
        private double initialValue;
        private double rateAlarmValue;
        private double cumulativeAlarmValue; 
        private double historicalCumulative;
        
        public Measurement() {
            depth = 0.0;
            initialValue = 0.0;
            rateAlarmValue = 0.0;
            cumulativeAlarmValue = 0.0;
            historicalCumulative = 0.0;
        }
        
        public Measurement(double depth, double initialValue, double rateAlarmValue, 
                           double cumulativeAlarmValue, double historicalCumulative) {
            this.depth = depth;
            this.initialValue = initialValue;
            this.rateAlarmValue = rateAlarmValue;
            this.cumulativeAlarmValue = cumulativeAlarmValue;
            this.historicalCumulative = historicalCumulative;
        }
        
        public double getDepth() {
            return depth;
        }
        
        public void setDepth(double depth) {
            this.depth = depth;
        }
        
        public double getInitialValue() {
            return initialValue;
        }
        
        public void setInitialValue(double initialValue) {
            this.initialValue = initialValue;
        }
        
        public double getRateAlarmValue() {
            return rateAlarmValue;
        }
        
        public void setRateAlarmValue(double rateAlarmValue) {
            this.rateAlarmValue = rateAlarmValue;
        }
        
        public double getCumulativeAlarmValue() {
            return cumulativeAlarmValue;
        }
        
        public void setCumulativeAlarmValue(double cumulativeAlarmValue) {
            this.cumulativeAlarmValue = cumulativeAlarmValue;
        }
        
        public double getHistoricalCumulative() {
            return historicalCumulative;
        }
        
        public void setHistoricalCumulative(double historicalCumulative) {
            this.historicalCumulative = historicalCumulative;
        }
    }
    
    /**
     * 默认构造方法
     */
    public DeepHorizontalDisplacementPoint() {
        pointId = "";
        mileage = "";
    }
    
    /**
     * 带参数的构造方法
     */
    public DeepHorizontalDisplacementPoint(String pointId, String mileage) {
        this.pointId = pointId;
        this.mileage = mileage;
    }
    
    // Getter和Setter方法
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
    
    public List<Measurement> getMeasurements() {
        return measurements;
    }
    
    public void setMeasurements(List<Measurement> measurements) {
        this.measurements = measurements;
    }
    
    /**
     * 添加一个测量点
     * @param measurement 测量点
     */
    public void addMeasurement(Measurement measurement) {
        if (measurements == null) {
            measurements = new ArrayList<>();
        }
        measurements.add(measurement);
    }
    
    /**
     * 移除一个测量点
     * @param measurement 测量点
     * @return 是否成功移除
     */
    public boolean removeMeasurement(Measurement measurement) {
        if (measurements != null) {
            return measurements.remove(measurement);
        }
        return false;
    }
    
    @Override
    public String toString() {
        return pointId;
    }
} 