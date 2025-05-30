package com.monitor.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 钢支撑轴力数据存储类
 * 用于保存测点配置和数据块信息
 */
public class SteelSupportAxialForceDataStorage implements Serializable {
    private static final long serialVersionUID = 1L;

    // 测点配置列表
    private List<SteelSupportAxialForcePoint> points = new ArrayList<>();

    // 数据块映射表 <时间戳, 数据列表>
    private Map<LocalDateTime, List<SteelSupportAxialForceData>> dataBlocks = new HashMap<>();

    // 数据块描述映射表 <时间戳, 描述>
    private Map<LocalDateTime, String> dataBlockDescriptions = new HashMap<>();

    // 选中的数据块列表
    private List<LocalDateTime> selectedDataBlocks = new ArrayList<>();

    // 自定义计算天数
    private int customDaysForCalculation = 0;

    /**
     * 获取测点列表
     */
    public List<SteelSupportAxialForcePoint> getPoints() {
        return points;
    }

    /**
     * 设置测点列表
     */
    public void setPoints(List<SteelSupportAxialForcePoint> points) {
        this.points = points;
    }

    /**
     * 添加数据块
     */
    public void addDataBlock(LocalDateTime timestamp, List<SteelSupportAxialForceData> data, String description) {
        dataBlocks.put(timestamp, data);
        dataBlockDescriptions.put(timestamp, description);
    }

    /**
     * 获取数据块时间戳列表
     */
    public List<LocalDateTime> getDataBlockTimestamps() {
        return new ArrayList<>(dataBlocks.keySet());
    }

    /**
     * 获取数据块描述
     */
    public String getDataBlockDescription(LocalDateTime timestamp) {
        return dataBlockDescriptions.getOrDefault(timestamp, "");
    }

    /**
     * 获取数据块
     */
    public List<SteelSupportAxialForceData> getDataBlock(LocalDateTime timestamp) {
        return dataBlocks.getOrDefault(timestamp, new ArrayList<>());
    }

    /**
     * 获取选中的数据块列表
     */
    public List<LocalDateTime> getSelectedDataBlocks() {
        return selectedDataBlocks;
    }

    /**
     * 设置选中的数据块列表
     */
    public void setSelectedDataBlocks(List<LocalDateTime> selected) {
        this.selectedDataBlocks = selected;
    }

    /**
     * 获取所有测点的数据映射
     */
    public Map<String, Map<LocalDate, SteelSupportAxialForceData>> getAllPointDataMap() {
        Map<String, Map<LocalDate, SteelSupportAxialForceData>> result = new HashMap<>();
        
        // 遍历所有数据块
        for (Map.Entry<LocalDateTime, List<SteelSupportAxialForceData>> entry : dataBlocks.entrySet()) {
            for (SteelSupportAxialForceData data : entry.getValue()) {
                String pointId = data.getPointCode();
                LocalDate date = data.getMeasurementDate();
                
                // 获取或创建测点映射
                Map<LocalDate, SteelSupportAxialForceData> pointMap = result.computeIfAbsent(pointId, k -> new HashMap<>());
                
                // 添加数据
                pointMap.put(date, data);
            }
        }
        
        return result;
    }

    /**
     * 生成用于显示的数据
     */
    public List<SteelSupportAxialForceData> generateDisplayData() {
        List<SteelSupportAxialForceData> displayData = new ArrayList<>();
        
        if (selectedDataBlocks.isEmpty()) {
            return displayData;
        }
        
        // 获取选中数据块的数据
        List<SteelSupportAxialForceData> currentBlock = dataBlocks.get(selectedDataBlocks.get(0));
        
        if (currentBlock == null) {
            return displayData;
        }
        
        // 如果只选中一个数据块，直接返回
        if (selectedDataBlocks.size() == 1) {
            return new ArrayList<>(currentBlock);
        }
        
        // 如果选中两个数据块，进行比较
        List<SteelSupportAxialForceData> previousBlock = dataBlocks.get(selectedDataBlocks.get(1));
        
        if (previousBlock == null) {
            return new ArrayList<>(currentBlock);
        }
        
        // 确定哪个是较早的数据块
        LocalDateTime currentTime = selectedDataBlocks.get(0);
        LocalDateTime previousTime = selectedDataBlocks.get(1);
        
        if (currentTime.isBefore(previousTime)) {
            // 交换，保证当前块是较新的
            List<SteelSupportAxialForceData> temp = currentBlock;
            currentBlock = previousBlock;
            previousBlock = temp;
        }
        
        // 创建测点ID到数据的映射
        Map<String, SteelSupportAxialForceData> previousDataMap = new HashMap<>();
        for (SteelSupportAxialForceData data : previousBlock) {
            previousDataMap.put(data.getPointCode(), data);
        }
        
        // 处理当前数据块
        for (SteelSupportAxialForceData currentData : currentBlock) {
            SteelSupportAxialForceData previousData = previousDataMap.get(currentData.getPointCode());
            
            if (previousData != null) {
                // 设置上次轴力
                currentData.setPreviousForce(previousData.getCurrentForce());
                
                // 重新计算变化量
                currentData.calculateDerivedValues();
            }
            
            displayData.add(currentData);
        }
        
        return displayData;
    }

    /**
     * 获取自定义计算天数
     */
    public int getCustomDaysForCalculation() {
        return customDaysForCalculation;
    }

    /**
     * 设置自定义计算天数
     */
    public void setCustomDaysForCalculation(int days) {
        this.customDaysForCalculation = days;
    }
} 