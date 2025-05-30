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
 * 桩顶竖向位移数据存储类，用于存储和管理桩顶竖向位移监测数据
 */
public class PileDisplacementDataStorage implements Serializable {
    private static final long serialVersionUID = 1L;

    // 测点配置信息
    private List<PileDisplacementPoint> points = new ArrayList<>();
    
    // 每个数据块的数据集合，按上传时间戳分组
    private Map<LocalDateTime, List<PileDisplacementData>> dataBlocks = new HashMap<>();
    
    // 每个数据块的描述信息
    private Map<LocalDateTime, String> dataBlockDescriptions = new HashMap<>();
    
    // 当前选定的数据块时间戳列表
    private List<LocalDateTime> selectedDataBlocks = new ArrayList<>();
    
    // 自定义速率计算天数
    private int customDaysForRateCalculation = 0;
    
    /**
     * 获取所有配置的测点
     */
    public List<PileDisplacementPoint> getPoints() {
        return points;
    }
    
    /**
     * 设置测点配置
     */
    public void setPoints(List<PileDisplacementPoint> points) {
        this.points = new ArrayList<>(points);
    }
    
    /**
     * 添加一个数据块
     */
    public void addDataBlock(LocalDateTime timestamp, List<PileDisplacementData> data, String description) {
        dataBlocks.put(timestamp, new ArrayList<>(data));
        dataBlockDescriptions.put(timestamp, description);
    }
    
    /**
     * 获取所有数据块的时间戳
     */
    public List<LocalDateTime> getDataBlockTimestamps() {
        return new ArrayList<>(dataBlocks.keySet());
    }
    
    /**
     * 获取数据块描述信息
     */
    public String getDataBlockDescription(LocalDateTime timestamp) {
        return dataBlockDescriptions.get(timestamp);
    }
    
    /**
     * 获取指定数据块的数据
     */
    public List<PileDisplacementData> getDataBlock(LocalDateTime timestamp) {
        List<PileDisplacementData> data = dataBlocks.get(timestamp);
        return data != null ? new ArrayList<>(data) : new ArrayList<>();
    }
    
    /**
     * 获取选定的数据块时间戳列表
     */
    public List<LocalDateTime> getSelectedDataBlocks() {
        return new ArrayList<>(selectedDataBlocks);
    }
    
    /**
     * 设置选定的数据块
     */
    public void setSelectedDataBlocks(List<LocalDateTime> selected) {
        this.selectedDataBlocks = new ArrayList<>(selected);
    }
    
    /**
     * 获取所有测点的所有日期的数据映射
     * 结构为：<测点编号, <测量日期, 数据>>
     */
    public Map<String, Map<LocalDate, PileDisplacementData>> getAllPointDataMap() {
        Map<String, Map<LocalDate, PileDisplacementData>> result = new HashMap<>();
        
        // 遍历所有数据块
        for (Map.Entry<LocalDateTime, List<PileDisplacementData>> entry : dataBlocks.entrySet()) {
            for (PileDisplacementData data : entry.getValue()) {
                String pointCode = data.getPointCode();
                LocalDate date = data.getMeasurementDate();
                
                if (!result.containsKey(pointCode)) {
                    result.put(pointCode, new HashMap<>());
                }
                
                result.get(pointCode).put(date, data);
            }
        }
        
        return result;
    }
    
    /**
     * 根据当前选择的数据块生成用于显示的数据
     */
    public List<PileDisplacementData> generateDisplayData() {
        if (selectedDataBlocks.isEmpty()) {
            return new ArrayList<>();
        }
        
        if (selectedDataBlocks.size() == 1) {
            // 单个数据块显示
            return getDataBlock(selectedDataBlocks.get(0));
        } else if (selectedDataBlocks.size() == 2) {
            // 两个数据块比较显示
            LocalDateTime time1 = selectedDataBlocks.get(0);
            LocalDateTime time2 = selectedDataBlocks.get(1);
            
            // 确定哪个是前期，哪个是本期
            LocalDateTime previousTime = time1.isBefore(time2) ? time1 : time2;
            LocalDateTime currentTime = time1.isBefore(time2) ? time2 : time1;
            
            List<PileDisplacementData> previousData = getDataBlock(previousTime);
            List<PileDisplacementData> currentData = getDataBlock(currentTime);
            
            // 创建测点映射以便快速查找
            Map<String, PileDisplacementData> previousDataMap = previousData.stream()
                    .collect(Collectors.toMap(PileDisplacementData::getPointCode, data -> data));
            
            // 处理当前数据，结合前期数据计算变化值
            List<PileDisplacementData> result = new ArrayList<>();
            
            for (PileDisplacementData current : currentData) {
                PileDisplacementData previous = previousDataMap.get(current.getPointCode());
                
                if (previous != null) {
                    // 将前期数据的高程设为前期高程
                    current.setPreviousElevation(previous.getCurrentElevation());
                    
                    // 重新计算变化量和速率
                    if (customDaysForRateCalculation > 0) {
                        current.calculateDerivedValues(previous.getMeasurementDate(), 
                                current.getMeasurementDate(), customDaysForRateCalculation);
                    } else {
                        current.calculateDerivedValues(previous.getMeasurementDate(), 
                                current.getMeasurementDate(), 0);
                    }
                }
                
                result.add(current);
            }
            
            return result;
        }
        
        return new ArrayList<>();
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
     * 用于序列化的数据包装类
     */
    public static class PileDisplacementDataWrapper implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private PileDisplacementPoint point;
        private List<PileDisplacementData> data = new ArrayList<>();
        
        public PileDisplacementDataWrapper() {
        }
        
        public PileDisplacementDataWrapper(PileDisplacementPoint point) {
            this.point = point;
        }
        
        public PileDisplacementPoint getPoint() {
            return point;
        }
        
        public void setPoint(PileDisplacementPoint point) {
            this.point = point;
        }
        
        public List<PileDisplacementData> getData() {
            return data;
        }
        
        public void setData(List<PileDisplacementData> data) {
            this.data = data;
        }
        
        public void addData(PileDisplacementData dataItem) {
            this.data.add(dataItem);
        }
    }
} 