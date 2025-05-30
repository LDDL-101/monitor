package com.monitor.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 沉降数据存储类
 * 用于保存完整的测点设置和上传数据
 */
public class SettlementDataStorage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // 配置的测点列表
    private List<SettlementPoint> configuredPoints = new ArrayList<>();
    
    // 所有测点数据的映射：测点ID -> (日期 -> 数据)
    private Map<String, Map<LocalDate, SettlementDataWrapper>> allPointDataMap = new HashMap<>();
    
    // 数据块映射：上传时间 -> 数据列表
    private Map<LocalDateTime, List<SettlementDataWrapper>> dataBlocksMap = new HashMap<>();
    
    /**
     * 默认构造函数
     */
    public SettlementDataStorage() {
    }
    
    /**
     * 获取配置的测点列表
     */
    public List<SettlementPoint> getConfiguredPoints() {
        return configuredPoints;
    }
    
    /**
     * 设置配置的测点列表
     */
    public void setConfiguredPoints(List<SettlementPoint> configuredPoints) {
        this.configuredPoints = configuredPoints;
    }
    
    /**
     * 获取所有测点数据的映射
     */
    public Map<String, Map<LocalDate, SettlementDataWrapper>> getAllPointDataMap() {
        return allPointDataMap;
    }
    
    /**
     * 设置所有测点数据的映射
     */
    public void setAllPointDataMap(Map<String, Map<LocalDate, SettlementDataWrapper>> allPointDataMap) {
        this.allPointDataMap = allPointDataMap;
    }
    
    /**
     * 获取数据块映射
     */
    public Map<LocalDateTime, List<SettlementDataWrapper>> getDataBlocksMap() {
        return dataBlocksMap;
    }
    
    /**
     * 设置数据块映射
     */
    public void setDataBlocksMap(Map<LocalDateTime, List<SettlementDataWrapper>> dataBlocksMap) {
        this.dataBlocksMap = dataBlocksMap;
    }
    
    /**
     * 添加测点配置
     */
    public void addConfiguredPoint(SettlementPoint point) {
        if (point != null) {
            // 检查是否已存在相同ID的测点
            boolean exists = configuredPoints.stream()
                .anyMatch(p -> p.getPointId().equals(point.getPointId()));
                
            if (!exists) {
                configuredPoints.add(point);
            }
        }
    }
    
    /**
     * 添加测点数据
     */
    public void addPointData(String pointId, LocalDate date, SettlementDataWrapper data, LocalDateTime uploadTime) {
        // 添加到测点数据映射
        if (!allPointDataMap.containsKey(pointId)) {
            allPointDataMap.put(pointId, new HashMap<>());
        }
        allPointDataMap.get(pointId).put(date, data);
        
        // 添加到数据块映射
        if (!dataBlocksMap.containsKey(uploadTime)) {
            dataBlocksMap.put(uploadTime, new ArrayList<>());
        }
        dataBlocksMap.get(uploadTime).add(data);
    }
    
    /**
     * 沉降数据包装类
     * 用于序列化SettlementData对象
     */
    public static class SettlementDataWrapper implements Serializable {
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
        public SettlementDataWrapper() {
        }
        
        /**
         * 从SettlementData创建包装对象
         */
        public SettlementDataWrapper(SettlementData data) {
            if (data != null) {
                this.pointCode = data.getPointCode();
                this.initialElevation = data.getInitialElevation();
                this.previousElevation = data.getPreviousElevation();
                this.currentElevation = data.getCurrentElevation();
                this.currentChange = data.getCurrentChange();
                this.cumulativeChange = data.getCumulativeChange();
                this.changeRate = data.getChangeRate();
                this.mileage = data.getMileage();
                this.historicalCumulative = data.getHistoricalCumulative();
                this.measurementDate = data.getMeasurementDate();
            }
        }
        
        /**
         * 转换为SettlementData对象
         */
        public SettlementData toSettlementData() {
            SettlementData data = new SettlementData();
            data.setPointCode(this.pointCode);
            data.setInitialElevation(this.initialElevation);
            data.setPreviousElevation(this.previousElevation);
            data.setCurrentElevation(this.currentElevation);
            data.setCurrentChange(this.currentChange);
            data.setCumulativeChange(this.cumulativeChange);
            data.setChangeRate(this.changeRate);
            data.setMileage(this.mileage);
            data.setHistoricalCumulative(this.historicalCumulative);
            data.setMeasurementDate(this.measurementDate);
            return data;
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
    }
}
