package com.monitor.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 地下水位数据存储类
 * 用于保存完整的测点设置和上传数据
 */
public class GroundwaterLevelDataStorage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // 配置的测点列表
    private List<GroundwaterLevelPoint> points = new ArrayList<>();
    
    // 数据块映射，每个数据块对应一次上传的数据，结构为：<时间戳, 数据列表>
    private Map<LocalDateTime, List<GroundwaterLevelData>> dataBlocks = new HashMap<>();
    
    // 数据块描述，记录每个数据块的文件名等信息
    private Map<LocalDateTime, String> dataBlockDescriptions = new HashMap<>();
    
    // 当前选中的数据块
    private List<LocalDateTime> selectedDataBlocks = new ArrayList<>();
    
    // 自定义速率计算天数
    private int customDaysForRateCalculation = 0;
    
    /**
     * 获取所有配置的测点
     */
    public List<GroundwaterLevelPoint> getPoints() {
        return points;
    }
    
    /**
     * 设置测点列表
     */
    public void setPoints(List<GroundwaterLevelPoint> points) {
        this.points = points;
    }
    
    /**
     * 添加一个数据块
     */
    public void addDataBlock(LocalDateTime timestamp, List<GroundwaterLevelData> data, String description) {
        dataBlocks.put(timestamp, data);
        dataBlockDescriptions.put(timestamp, description);
    }
    
    /**
     * 获取所有数据块的时间戳
     */
    public List<LocalDateTime> getDataBlockTimestamps() {
        return new ArrayList<>(dataBlocks.keySet());
    }
    
    /**
     * 获取数据块的描述信息
     */
    public String getDataBlockDescription(LocalDateTime timestamp) {
        return dataBlockDescriptions.get(timestamp);
    }
    
    /**
     * 获取指定时间戳对应的数据块
     */
    public List<GroundwaterLevelData> getDataBlock(LocalDateTime timestamp) {
        return dataBlocks.get(timestamp);
    }
    
    /**
     * 获取当前选中的数据块
     */
    public List<LocalDateTime> getSelectedDataBlocks() {
        return selectedDataBlocks;
    }
    
    /**
     * 设置当前选中的数据块
     */
    public void setSelectedDataBlocks(List<LocalDateTime> selected) {
        this.selectedDataBlocks = selected;
    }
    
    /**
     * 构建所有测点的历史数据映射
     * 结构为：<测点编号, <测量日期, 数据>>
     */
    public Map<String, Map<LocalDate, GroundwaterLevelData>> getAllPointDataMap() {
        Map<String, Map<LocalDate, GroundwaterLevelData>> allPointDataMap = new HashMap<>();
        
        // 遍历所有选中的数据块
        for (LocalDateTime blockTime : selectedDataBlocks) {
            List<GroundwaterLevelData> blockData = dataBlocks.get(blockTime);
            if (blockData != null) {
                for (GroundwaterLevelData data : blockData) {
                    String pointId = data.getPointCode();
                    LocalDate date = data.getMeasurementDate();
                    
                    // 获取或创建该测点的数据映射
                    Map<LocalDate, GroundwaterLevelData> pointDataMap = allPointDataMap.computeIfAbsent(pointId, k -> new HashMap<>());
                    
                    // 添加该测点在当前日期的数据
                    pointDataMap.put(date, data);
                }
            }
        }
        
        return allPointDataMap;
    }
    
    /**
     * 根据当前选中的数据块生成显示数据
     */
    public List<GroundwaterLevelData> generateDisplayData() {
        if (selectedDataBlocks.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 构建测点历史数据映射
        Map<String, Map<LocalDate, GroundwaterLevelData>> allPointDataMap = getAllPointDataMap();
        
        // 获取最新的数据块
        LocalDateTime latestTimestamp = selectedDataBlocks.stream()
                .max(LocalDateTime::compareTo)
                .orElse(null);
        
        if (latestTimestamp == null) {
            return new ArrayList<>();
        }
        
        List<GroundwaterLevelData> latestData = dataBlocks.get(latestTimestamp);
        if (latestData == null || latestData.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 为每个测点生成显示数据
        List<GroundwaterLevelData> displayData = new ArrayList<>();
        for (GroundwaterLevelData data : latestData) {
            String pointId = data.getPointCode();
            LocalDate currentDate = data.getMeasurementDate();
            
            // 查找该测点的历史数据
            Map<LocalDate, GroundwaterLevelData> pointHistory = allPointDataMap.get(pointId);
            if (pointHistory != null) {
                // 查找最近一次的前期数据（当前日期之前的最新数据）
                LocalDate previousDate = pointHistory.keySet().stream()
                        .filter(date -> date.isBefore(currentDate))
                        .max(LocalDate::compareTo)
                        .orElse(null);
                
                if (previousDate != null) {
                    // 重新计算变化速率，考虑实际间隔天数
                    data.calculateDerivedValues(previousDate, currentDate, customDaysForRateCalculation);
                }
            }
            
            displayData.add(data);
        }
        
        return displayData;
    }
    
    /**
     * 获取自定义速率计算天数
     */
    public int getCustomDaysForRateCalculation() {
        return customDaysForRateCalculation;
    }
    
    /**
     * 设置自定义速率计算天数
     */
    public void setCustomDaysForRateCalculation(int days) {
        this.customDaysForRateCalculation = days;
    }
    
    /**
     * 地下水位数据包装类，用于内部数据组织
     */
    public static class GroundwaterLevelDataWrapper implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private GroundwaterLevelPoint point;
        private List<GroundwaterLevelData> data = new ArrayList<>();
        
        public GroundwaterLevelDataWrapper() {
        }
        
        public GroundwaterLevelDataWrapper(GroundwaterLevelPoint point) {
            this.point = point;
        }
        
        public GroundwaterLevelPoint getPoint() {
            return point;
        }
        
        public void setPoint(GroundwaterLevelPoint point) {
            this.point = point;
        }
        
        public List<GroundwaterLevelData> getData() {
            return data;
        }
        
        public void setData(List<GroundwaterLevelData> data) {
            this.data = data;
        }
        
        public void addData(GroundwaterLevelData dataItem) {
            this.data.add(dataItem);
        }
    }
} 