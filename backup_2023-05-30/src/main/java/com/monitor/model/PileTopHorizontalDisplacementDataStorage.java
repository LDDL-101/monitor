package com.monitor.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 桩顶水平位移数据存储类
 */
public class PileTopHorizontalDisplacementDataStorage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // 测点配置列表
    private List<PileTopHorizontalDisplacementPoint> points = new ArrayList<>();
    
    // 数据块映射，每个数据块对应一次上传的数据，结构为：<时间戳, 数据列表>
    private Map<LocalDateTime, List<PileTopHorizontalDisplacementData>> dataBlocksMap = new HashMap<>();
    
    // 数据块描述映射，结构为：<时间戳, 描述>
    private Map<LocalDateTime, String> dataBlockDescriptions = new HashMap<>();
    
    // 当前选中的数据块
    private List<LocalDateTime> selectedDataBlocks = new ArrayList<>();
    
    // 自定义速率计算天数
    private int customDaysForRateCalculation = 0;
    
    /**
     * 默认构造函数
     */
    public PileTopHorizontalDisplacementDataStorage() {
        // 初始化为空
    }
    
    /**
     * 添加测点
     */
    public void addPoint(PileTopHorizontalDisplacementPoint point) {
        if (point != null) {
            points.add(point);
        }
    }
    
    /**
     * 添加数据块
     */
    public void addDataBlock(LocalDateTime timestamp, List<PileTopHorizontalDisplacementData> dataBlock, String description) {
        if (timestamp != null && dataBlock != null) {
            dataBlocksMap.put(timestamp, new ArrayList<>(dataBlock));
            if (description != null) {
                dataBlockDescriptions.put(timestamp, description);
            }
        }
    }
    
    /**
     * 获取数据块
     */
    public List<PileTopHorizontalDisplacementData> getDataBlock(LocalDateTime timestamp) {
        return dataBlocksMap.getOrDefault(timestamp, new ArrayList<>());
    }
    
    /**
     * 获取数据块描述
     */
    public String getDataBlockDescription(LocalDateTime timestamp) {
        return dataBlockDescriptions.get(timestamp);
    }
    
    /**
     * 获取所有数据块时间戳
     */
    public List<LocalDateTime> getDataBlockTimestamps() {
        return new ArrayList<>(dataBlocksMap.keySet());
    }
    
    /**
     * 移除数据块
     */
    public void removeDataBlock(LocalDateTime timestamp) {
        dataBlocksMap.remove(timestamp);
        dataBlockDescriptions.remove(timestamp);
        selectedDataBlocks.remove(timestamp);
    }
    
    /**
     * 获取测点列表
     */
    public List<PileTopHorizontalDisplacementPoint> getPoints() {
        return points;
    }
    
    /**
     * 设置测点列表
     */
    public void setPoints(List<PileTopHorizontalDisplacementPoint> points) {
        this.points = new ArrayList<>(points);
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
    public void setCustomDaysForRateCalculation(int customDaysForRateCalculation) {
        this.customDaysForRateCalculation = customDaysForRateCalculation;
    }
    
    /**
     * 获取选中的数据块
     */
    public List<LocalDateTime> getSelectedDataBlocks() {
        return selectedDataBlocks;
    }
    
    /**
     * 设置选中的数据块
     */
    public void setSelectedDataBlocks(List<LocalDateTime> selectedDataBlocks) {
        this.selectedDataBlocks = new ArrayList<>(selectedDataBlocks);
    }
    
    /**
     * 获取最新的测量日期
     */
    public LocalDate getLatestMeasurementDate() {
        LocalDate latestDate = null;
        
        for (List<PileTopHorizontalDisplacementData> dataList : dataBlocksMap.values()) {
            if (dataList != null && !dataList.isEmpty()) {
                LocalDate measurementDate = dataList.get(0).getMeasurementDate();
                if (latestDate == null || measurementDate.isAfter(latestDate)) {
                    latestDate = measurementDate;
                }
            }
        }
        
        return latestDate != null ? latestDate : LocalDate.now();
    }
    
    /**
     * 查找指定测点编号的测点
     */
    public PileTopHorizontalDisplacementPoint findPointById(String pointId) {
        for (PileTopHorizontalDisplacementPoint point : points) {
            if (point.getPointId().equals(pointId)) {
                return point;
            }
        }
        return null;
    }
    
    /**
     * 获取指定测点的历史数据
     */
    public List<PileTopHorizontalDisplacementData> getHistoricalDataForPoint(String pointId) {
        List<PileTopHorizontalDisplacementData> result = new ArrayList<>();
        
        for (List<PileTopHorizontalDisplacementData> dataList : dataBlocksMap.values()) {
            for (PileTopHorizontalDisplacementData data : dataList) {
                if (data.getPointCode().equals(pointId)) {
                    result.add(data);
                    break; // 每个数据块只取一条该测点的数据
                }
            }
        }
        
        return result;
    }
    
    @Override
    public String toString() {
        return "PileTopHorizontalDisplacementDataStorage [points=" + points.size()
                + ", dataBlocks=" + dataBlocksMap.size()
                + ", selectedDataBlocks=" + selectedDataBlocks.size()
                + ", customDaysForRateCalculation=" + customDaysForRateCalculation + "]";
    }
} 