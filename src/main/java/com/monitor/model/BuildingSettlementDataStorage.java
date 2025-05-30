package com.monitor.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 建筑物沉降数据存储类
 * 用于保存与建筑物沉降相关的所有数据
 */
public class BuildingSettlementDataStorage implements Serializable {
    private static final long serialVersionUID = 1L;

    // 测点配置列表
    private List<BuildingSettlementPoint> points = new ArrayList<>();

    // 数据块映射：测量时间戳 -> 数据列表
    private Map<LocalDateTime, List<BuildingSettlementData>> dataBlocks = new HashMap<>();

    // 数据块描述：测量时间戳 -> 描述文本
    private Map<LocalDateTime, String> dataBlockDescriptions = new HashMap<>();

    // 当前选择的数据块
    private List<LocalDateTime> selectedDataBlocks = new ArrayList<>();

    // 自定义速率计算天数
    private int customDaysForRateCalculation = 0;

    /**
     * 获取测点列表
     */
    public List<BuildingSettlementPoint> getPoints() {
        return points;
    }

    /**
     * 设置测点列表
     */
    public void setPoints(List<BuildingSettlementPoint> points) {
        this.points = points;
    }

    /**
     * 添加数据块
     */
    public void addDataBlock(LocalDateTime timestamp, List<BuildingSettlementData> data, String description) {
        dataBlocks.put(timestamp, data);
        dataBlockDescriptions.put(timestamp, description);
    }

    /**
     * 获取所有数据块时间戳
     */
    public List<LocalDateTime> getDataBlockTimestamps() {
        List<LocalDateTime> timestamps = new ArrayList<>(dataBlocks.keySet());
        Collections.sort(timestamps);
        return timestamps;
    }

    /**
     * 获取数据块描述
     */
    public String getDataBlockDescription(LocalDateTime timestamp) {
        return dataBlockDescriptions.get(timestamp);
    }

    /**
     * 获取数据块
     */
    public List<BuildingSettlementData> getDataBlock(LocalDateTime timestamp) {
        return dataBlocks.get(timestamp);
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
    public void setSelectedDataBlocks(List<LocalDateTime> selected) {
        this.selectedDataBlocks = selected;
    }

    /**
     * 生成所有测点的数据映射
     */
    public Map<String, Map<LocalDate, BuildingSettlementData>> getAllPointDataMap() {
        Map<String, Map<LocalDate, BuildingSettlementData>> allPointDataMap = new HashMap<>();

        // 遍历所有数据块
        for (List<BuildingSettlementData> dataList : dataBlocks.values()) {
            for (BuildingSettlementData data : dataList) {
                String pointCode = data.getPointCode();
                LocalDate measureDate = data.getMeasurementDate();

                // 确保该测点的映射存在
                if (!allPointDataMap.containsKey(pointCode)) {
                    allPointDataMap.put(pointCode, new HashMap<>());
                }

                // 添加或更新测点在特定日期的数据
                allPointDataMap.get(pointCode).put(measureDate, data);
            }
        }

        return allPointDataMap;
    }

    /**
     * 生成表格显示数据
     * 根据选择的数据块生成显示数据
     */
    public List<BuildingSettlementData> generateDisplayData() {
        if (selectedDataBlocks.isEmpty()) {
            return Collections.emptyList();
        }

        List<BuildingSettlementData> displayData = new ArrayList<>();

        if (selectedDataBlocks.size() == 1) {
            // 只选择了一个数据块
            LocalDateTime timestamp = selectedDataBlocks.get(0);
            List<BuildingSettlementData> dataList = dataBlocks.get(timestamp);
            if (dataList != null) {
                // 按测点ID排序
                List<BuildingSettlementData> sortedData = new ArrayList<>(dataList);
                sortedData.sort(Comparator.comparing(BuildingSettlementData::getPointCode));
                displayData.addAll(sortedData);
            }
        } else if (selectedDataBlocks.size() == 2) {
            // 选择了两个数据块，需要比较
            selectedDataBlocks.sort(Comparator.naturalOrder());
            LocalDateTime previousTime = selectedDataBlocks.get(0);
            LocalDateTime currentTime = selectedDataBlocks.get(1);

            List<BuildingSettlementData> previousDataList = dataBlocks.get(previousTime);
            List<BuildingSettlementData> currentDataList = dataBlocks.get(currentTime);

            if (previousDataList != null && currentDataList != null) {
                // 将前期数据转为Map便于查找
                Map<String, BuildingSettlementData> previousDataMap = new HashMap<>();
                for (BuildingSettlementData data : previousDataList) {
                    previousDataMap.put(data.getPointCode(), data);
                }

                // 处理当前数据
                for (BuildingSettlementData currentData : currentDataList) {
                    String pointCode = currentData.getPointCode();
                    BuildingSettlementData previousData = previousDataMap.get(pointCode);

                    if (previousData != null) {
                        // 设置前期高程
                        currentData.setPreviousElevation(previousData.getCurrentElevation());

                        // 计算派生值，考虑实际日期
                        currentData.calculateDerivedValues(
                                previousData.getMeasurementDate(),
                                currentData.getMeasurementDate(),
                                customDaysForRateCalculation);
                    }

                    displayData.add(currentData);
                }

                // 按测点ID排序
                displayData.sort(Comparator.comparing(BuildingSettlementData::getPointCode));
            }
        }

        return displayData;
    }

    /**
     * 获取自定义天数
     */
    public int getCustomDaysForRateCalculation() {
        return customDaysForRateCalculation;
    }

    /**
     * 设置自定义天数
     */
    public void setCustomDaysForRateCalculation(int days) {
        this.customDaysForRateCalculation = days;
    }
} 