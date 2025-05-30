package com.monitor.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 深部水平位移数据存储类
 */
public class DeepHorizontalDisplacementDataStorage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // 测点列表
    private List<DeepHorizontalDisplacementPoint> points;
    
    // 数据块映射：<时间戳, 数据列表>
    private Map<LocalDateTime, List<DeepHorizontalDisplacementData>> dataBlocks;
    
    // 数据块描述映射：<时间戳, 描述>
    private Map<LocalDateTime, String> dataBlockDescriptions;
    
    // 选中的数据块
    private List<LocalDateTime> selectedDataBlocks;
    
    // 自定义速率计算天数
    private int customDaysForRateCalculation;
    
    /**
     * 默认构造方法
     */
    public DeepHorizontalDisplacementDataStorage() {
        points = new ArrayList<>();
        dataBlocks = new HashMap<>();
        dataBlockDescriptions = new HashMap<>();
        selectedDataBlocks = new ArrayList<>();
        customDaysForRateCalculation = 0;
    }
    
    /**
     * 添加测点
     * @param point 深部水平位移测点
     */
    public void addPoint(DeepHorizontalDisplacementPoint point) {
        points.add(point);
    }
    
    /**
     * 移除测点
     * @param point 深部水平位移测点
     */
    public void removePoint(DeepHorizontalDisplacementPoint point) {
        points.remove(point);
    }
    
    /**
     * 设置测点列表
     * @param points 测点列表
     */
    public void setPoints(List<DeepHorizontalDisplacementPoint> points) {
        this.points = new ArrayList<>(points);
    }
    
    /**
     * 获取测点列表
     * @return 测点列表
     */
    public List<DeepHorizontalDisplacementPoint> getPoints() {
        return Collections.unmodifiableList(points);
    }
    
    /**
     * 添加数据块
     * @param timestamp 时间戳
     * @param data 数据列表
     * @param description 描述
     */
    public void addDataBlock(LocalDateTime timestamp, List<DeepHorizontalDisplacementData> data, String description) {
        dataBlocks.put(timestamp, new ArrayList<>(data));
        dataBlockDescriptions.put(timestamp, description);
    }
    
    /**
     * 移除数据块
     * @param timestamp 时间戳
     */
    public void removeDataBlock(LocalDateTime timestamp) {
        dataBlocks.remove(timestamp);
        dataBlockDescriptions.remove(timestamp);
        selectedDataBlocks.remove(timestamp);
    }
    
    /**
     * 获取数据块
     * @param timestamp 时间戳
     * @return 数据列表
     */
    public List<DeepHorizontalDisplacementData> getDataBlock(LocalDateTime timestamp) {
        List<DeepHorizontalDisplacementData> data = dataBlocks.get(timestamp);
        if (data == null) {
            return new ArrayList<>();
        }
        return Collections.unmodifiableList(data);
    }
    
    /**
     * 获取数据块时间戳列表
     * @return 时间戳列表
     */
    public Set<LocalDateTime> getDataBlockTimestamps() {
        return Collections.unmodifiableSet(dataBlocks.keySet());
    }
    
    /**
     * 获取数据块描述
     * @param timestamp 时间戳
     * @return 描述
     */
    public String getDataBlockDescription(LocalDateTime timestamp) {
        return dataBlockDescriptions.get(timestamp);
    }
    
    /**
     * 设置数据块描述
     * @param timestamp 时间戳
     * @param description 描述
     */
    public void setDataBlockDescription(LocalDateTime timestamp, String description) {
        dataBlockDescriptions.put(timestamp, description);
    }
    
    /**
     * 获取选中的数据块
     * @return 选中的数据块时间戳列表
     */
    public List<LocalDateTime> getSelectedDataBlocks() {
        return new ArrayList<>(selectedDataBlocks);
    }
    
    /**
     * 设置选中的数据块
     * @param selectedDataBlocks 选中的数据块时间戳列表
     */
    public void setSelectedDataBlocks(List<LocalDateTime> selectedDataBlocks) {
        this.selectedDataBlocks = new ArrayList<>(selectedDataBlocks);
    }
    
    /**
     * 获取自定义速率计算天数
     * @return 自定义速率计算天数
     */
    public int getCustomDaysForRateCalculation() {
        return customDaysForRateCalculation;
    }
    
    /**
     * 设置自定义速率计算天数
     * @param days 天数
     */
    public void setCustomDaysForRateCalculation(int days) {
        this.customDaysForRateCalculation = days;
    }
    
    /**
     * 获取指定测点在指定日期的数据
     * @param pointId 测点ID
     * @param date 日期
     * @return 数据对象，如果不存在则返回null
     */
    public DeepHorizontalDisplacementData getDataByPointAndDate(String pointId, LocalDate date) {
        for (List<DeepHorizontalDisplacementData> dataList : dataBlocks.values()) {
            for (DeepHorizontalDisplacementData data : dataList) {
                if (data.getPointCode().equals(pointId) && data.getMeasurementDate().equals(date)) {
                    return data;
                }
            }
        }
        return null;
    }
    
    /**
     * 获取指定测点的所有历史数据
     * @param pointId 测点ID
     * @return 该测点的所有历史数据，按日期排序
     */
    public List<DeepHorizontalDisplacementData> getHistoricalDataForPoint(String pointId) {
        List<DeepHorizontalDisplacementData> result = new ArrayList<>();
        
        // 收集该测点的所有数据
        for (List<DeepHorizontalDisplacementData> dataList : dataBlocks.values()) {
            for (DeepHorizontalDisplacementData data : dataList) {
                if (data.getPointCode().equals(pointId)) {
                    result.add(data);
                }
            }
        }
        
        // 按日期排序
        result.sort(Comparator.comparing(DeepHorizontalDisplacementData::getMeasurementDate));
        
        return result;
    }
    
    /**
     * 获取指定测点在指定日期区间的数据
     * @param pointId 测点ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 符合条件的数据列表
     */
    public List<DeepHorizontalDisplacementData> getDataByPointAndDateRange(String pointId, LocalDate startDate, LocalDate endDate) {
        List<DeepHorizontalDisplacementData> result = new ArrayList<>();
        
        for (List<DeepHorizontalDisplacementData> dataList : dataBlocks.values()) {
            for (DeepHorizontalDisplacementData data : dataList) {
                LocalDate date = data.getMeasurementDate();
                if (data.getPointCode().equals(pointId) && 
                        (date.isEqual(startDate) || date.isAfter(startDate)) &&
                        (date.isEqual(endDate) || date.isBefore(endDate))) {
                    result.add(data);
                }
            }
        }
        
        // 按日期排序
        result.sort(Comparator.comparing(DeepHorizontalDisplacementData::getMeasurementDate));
        
        return result;
    }
    
    /**
     * 获取测点对象
     * @param pointId 测点ID
     * @return 测点对象，如果不存在则返回null
     */
    public DeepHorizontalDisplacementPoint getPointById(String pointId) {
        for (DeepHorizontalDisplacementPoint point : points) {
            if (point.getPointId().equals(pointId)) {
                return point;
            }
        }
        return null;
    }
    
    /**
     * 获取数据块数量
     * @return 数据块数量
     */
    public int getDataBlockCount() {
        return dataBlocks.size();
    }
} 