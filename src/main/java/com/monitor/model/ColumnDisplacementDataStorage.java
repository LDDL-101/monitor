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
 * 立柱竖向位移数据存储类
 * 用于保存完整的测点设置和上传数据
 */
public class ColumnDisplacementDataStorage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // 配置的测点列表
    private List<ColumnDisplacementPoint> configuredPoints = new ArrayList<>();
    
    // 所有测点数据的映射：测点ID -> (日期 -> 数据)
    private Map<String, Map<LocalDate, ColumnDisplacementDataWrapper>> allPointDataMap = new HashMap<>();
    
    // 数据块映射：上传时间 -> 数据列表
    private Map<LocalDateTime, List<ColumnDisplacementDataWrapper>> dataBlocksMap = new HashMap<>();
    
    // 数据块描述映射：上传时间 -> 描述
    private Map<LocalDateTime, String> dataBlockDescriptions = new HashMap<>();
    
    // 当前选中的数据块
    private List<LocalDateTime> selectedDataBlocks = new ArrayList<>();
    
    // 自定义速率计算天数
    private int customDaysForRateCalculation = 0;
    
    /**
     * 默认构造函数
     */
    public ColumnDisplacementDataStorage() {
    }
    
    /**
     * 获取配置的测点列表
     */
    public List<ColumnDisplacementPoint> getConfiguredPoints() {
        return new ArrayList<>(configuredPoints);
    }
    
    /**
     * 设置配置的测点列表
     */
    public void setConfiguredPoints(List<ColumnDisplacementPoint> points) {
        this.configuredPoints = new ArrayList<>(points);
    }
    
    /**
     * 获取所有测点数据的映射
     */
    public Map<String, Map<LocalDate, ColumnDisplacementDataWrapper>> getAllPointDataMap() {
        return allPointDataMap;
    }
    
    /**
     * 设置所有测点数据的映射
     */
    public void setAllPointDataMap(Map<String, Map<LocalDate, ColumnDisplacementDataWrapper>> allPointDataMap) {
        this.allPointDataMap = allPointDataMap;
    }
    
    /**
     * 获取数据块映射
     */
    public Map<LocalDateTime, List<ColumnDisplacementDataWrapper>> getDataBlocksMap() {
        return dataBlocksMap;
    }
    
    /**
     * 获取数据块描述
     */
    public String getDataBlockDescription(LocalDateTime timestamp) {
        return dataBlockDescriptions.get(timestamp);
    }
    
    /**
     * 获取选定的数据块
     */
    public List<LocalDateTime> getSelectedDataBlocks() {
        return new ArrayList<>(selectedDataBlocks);
    }
    
    /**
     * 设置选定的数据块
     */
    public void setSelectedDataBlocks(List<LocalDateTime> selectedBlocks) {
        this.selectedDataBlocks = new ArrayList<>(selectedBlocks);
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
     * 添加测点配置
     */
    public void addConfiguredPoint(ColumnDisplacementPoint point) {
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
     * 添加测点数据到映射中
     */
    public void addPointData(String pointId, LocalDate date, ColumnDisplacementDataWrapper data, LocalDateTime uploadTime) {
        if (!allPointDataMap.containsKey(pointId)) {
            allPointDataMap.put(pointId, new HashMap<>());
        }
        
        allPointDataMap.get(pointId).put(date, data);
    }
    
    /**
     * 添加一个数据块
     * @param timestamp 数据块上传时间
     * @param dataList 数据列表
     * @param description 数据块描述
     */
    public void addDataBlock(LocalDateTime timestamp, List<ColumnDisplacementData> dataList, String description) {
        // 创建数据包装列表
        List<ColumnDisplacementDataWrapper> wrappers = new ArrayList<>();
        
        // 按测点分组数据
        Map<String, List<ColumnDisplacementData>> pointDataMap = dataList.stream()
                .collect(Collectors.groupingBy(ColumnDisplacementData::getPointCode));
        
        // 为每个测点创建一个包装器
        for (Map.Entry<String, List<ColumnDisplacementData>> entry : pointDataMap.entrySet()) {
            String pointCode = entry.getKey();
            List<ColumnDisplacementData> pointDataList = entry.getValue();
            
            // 查找对应测点配置
            ColumnDisplacementPoint point = findPointByCode(pointCode);
            if (point == null) {
                // 如果没有找到配置，创建一个基本的测点配置
                point = new ColumnDisplacementPoint();
                point.setPointId(pointCode);
                configuredPoints.add(point);
            }
            
            // 创建数据包装器
            ColumnDisplacementDataWrapper wrapper = new ColumnDisplacementDataWrapper();
            wrapper.setPoint(point);
            wrapper.setData(new ArrayList<>(pointDataList));
            
            // 添加到包装器列表
            wrappers.add(wrapper);
            
            // 更新总数据映射
            for (ColumnDisplacementData data : pointDataList) {
                addPointData(pointCode, data.getMeasurementDate(), wrapper, timestamp);
            }
        }
        
        // 保存数据块
        dataBlocksMap.put(timestamp, wrappers);
        dataBlockDescriptions.put(timestamp, description);
    }
    
    /**
     * 根据编码查找测点配置
     */
    private ColumnDisplacementPoint findPointByCode(String pointCode) {
        for (ColumnDisplacementPoint point : configuredPoints) {
            if (point.getPointId().equals(pointCode)) {
                return point;
            }
        }
        return null;
    }
    
    /**
     * 获取所有数据块的时间戳
     */
    public List<LocalDateTime> getDataBlockTimestamps() {
        return new ArrayList<>(dataBlocksMap.keySet());
    }
    
    /**
     * 生成用于显示的数据
     */
    public List<ColumnDisplacementData> generateDisplayData() {
        List<ColumnDisplacementData> result = new ArrayList<>();
        
        if (selectedDataBlocks.isEmpty()) {
            return result;
        }
        
        if (selectedDataBlocks.size() == 1) {
            // 单个数据块处理
            LocalDateTime timestamp = selectedDataBlocks.get(0);
            List<ColumnDisplacementDataWrapper> wrappers = dataBlocksMap.get(timestamp);
            
            if (wrappers != null) {
                for (ColumnDisplacementDataWrapper wrapper : wrappers) {
                    result.add(wrapper.toColumnDisplacementData());
                }
            }
        } else if (selectedDataBlocks.size() == 2) {
            // 两个数据块比较处理
            LocalDateTime time1 = selectedDataBlocks.get(0);
            LocalDateTime time2 = selectedDataBlocks.get(1);
            
            LocalDateTime previousTime = time1.isBefore(time2) ? time1 : time2;
            LocalDateTime currentTime = time1.isBefore(time2) ? time2 : time1;
            
            List<ColumnDisplacementDataWrapper> previousWrappers = dataBlocksMap.get(previousTime);
            List<ColumnDisplacementDataWrapper> currentWrappers = dataBlocksMap.get(currentTime);
            
            // 创建前期数据映射
            Map<String, ColumnDisplacementDataWrapper> previousMap = new HashMap<>();
            if (previousWrappers != null) {
                for (ColumnDisplacementDataWrapper wrapper : previousWrappers) {
                    // 从包装器的数据中获取测点编号
                    if (!wrapper.getData().isEmpty()) {
                        String pointId = wrapper.getData().get(0).getPointCode();
                        previousMap.put(pointId, wrapper);
                    }
                }
            }
            
            // 处理当前数据
            if (currentWrappers != null) {
                for (ColumnDisplacementDataWrapper currentWrapper : currentWrappers) {
                    ColumnDisplacementData data = new ColumnDisplacementData();
                    
                    // 从包装器的数据中获取测点编号和其他信息
                    if (!currentWrapper.getData().isEmpty()) {
                        ColumnDisplacementData sourceData = currentWrapper.getData().get(0);
                        String pointId = sourceData.getPointCode();
                        data = new ColumnDisplacementData(
                            sourceData.getPointCode(),
                            sourceData.getInitialElevation(),
                            0, // 前期高程先设为0
                            sourceData.getCurrentElevation(),
                            sourceData.getMeasurementDate()
                        );
                        
                        // 查找前期数据
                        ColumnDisplacementDataWrapper previousWrapper = previousMap.get(pointId);
                        if (previousWrapper != null && !previousWrapper.getData().isEmpty()) {
                            // 设置前期高程
                            ColumnDisplacementData prevData = previousWrapper.getData().get(0);
                            data.setPreviousElevation(prevData.getCurrentElevation());
                            
                            // 计算派生值
                            data.calculateDerivedValues(prevData.getMeasurementDate(), 
                                                        data.getMeasurementDate(),
                                                        customDaysForRateCalculation);
                        }
                        
                        result.add(data);
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * 立柱竖向位移数据包装类
     * 用于序列化ColumnDisplacementData对象
     */
    public static class ColumnDisplacementDataWrapper implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private ColumnDisplacementPoint point;
        private List<ColumnDisplacementData> data = new ArrayList<>();
        
        /**
         * 默认构造函数
         */
        public ColumnDisplacementDataWrapper() {
        }
        
        public ColumnDisplacementPoint getPoint() {
            return point;
        }
        
        public void setPoint(ColumnDisplacementPoint point) {
            this.point = point;
        }
        
        public List<ColumnDisplacementData> getData() {
            return data;
        }
        
        public void setData(List<ColumnDisplacementData> data) {
            this.data = data;
        }
        
        public void addData(ColumnDisplacementData dataItem) {
            this.data.add(dataItem);
        }
        
        /**
         * 转换为ColumnDisplacementData对象
         */
        public ColumnDisplacementData toColumnDisplacementData() {
            if (data.isEmpty()) {
                return new ColumnDisplacementData();
            }
            
            // 直接使用第一条数据
            ColumnDisplacementData sourceData = data.get(0);
            ColumnDisplacementData result = new ColumnDisplacementData();
            
            // 从原始数据复制属性
            result.setPointCode(sourceData.getPointCode());
            result.setInitialElevation(sourceData.getInitialElevation());
            result.setPreviousElevation(sourceData.getPreviousElevation());
            result.setCurrentElevation(sourceData.getCurrentElevation());
            result.setCurrentChange(sourceData.getCurrentChange());
            result.setCumulativeChange(sourceData.getCumulativeChange());
            result.setChangeRate(sourceData.getChangeRate());
            result.setMileage(sourceData.getMileage());
            result.setHistoricalCumulative(sourceData.getHistoricalCumulative());
            result.setMeasurementDate(sourceData.getMeasurementDate());
            
            return result;
        }
        
        /**
         * 为兼容性转换为PileDisplacementData对象
         */
        public PileDisplacementData toPileDisplacementData() {
            if (data.isEmpty()) {
                return new PileDisplacementData();
            }
            
            // 直接使用第一条数据
            ColumnDisplacementData sourceData = data.get(0);
            PileDisplacementData result = new PileDisplacementData();
            
            // 从原始数据复制属性
            result.setPointCode(sourceData.getPointCode());
            result.setInitialElevation(sourceData.getInitialElevation());
            result.setPreviousElevation(sourceData.getPreviousElevation());
            result.setCurrentElevation(sourceData.getCurrentElevation());
            result.setCurrentChange(sourceData.getCurrentChange());
            result.setCumulativeChange(sourceData.getCumulativeChange());
            result.setChangeRate(sourceData.getChangeRate());
            result.setMileage(sourceData.getMileage());
            result.setHistoricalCumulative(sourceData.getHistoricalCumulative());
            result.setMeasurementDate(sourceData.getMeasurementDate());
            
            return result;
        }
    }
} 