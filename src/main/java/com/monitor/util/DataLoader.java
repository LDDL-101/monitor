package com.monitor.util;

import com.monitor.model.MeasurementRecord;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据加载和处理工具类
 */
public class DataLoader {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * 从CSV文件加载沉降测量数据
     * @param file CSV文件
     * @return 测量记录列表
     * @throws IOException 如果文件读取失败
     */
    public static ObservableList<MeasurementRecord> loadMeasurementData(File file) throws IOException {
        List<MeasurementRecord> records = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            // 跳过标题行
            String line = reader.readLine();
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 5) continue;
                
                try {
                    String id = parts[0].trim();
                    LocalDate date = LocalDate.parse(parts[1].trim(), DATE_FORMATTER);
                    double value = Double.parseDouble(parts[2].trim());
                    String operator = parts.length > 3 ? parts[3].trim() : "";
                    String comments = parts.length > 4 ? parts[4].trim() : "";
                    
                    MeasurementRecord record = new MeasurementRecord();
                    record.setId(id);
                    record.setMeasureTime(LocalDateTime.of(date, LocalTime.NOON));
                    record.setValue(value);
                    record.setOperator(operator);
                    record.setComments(comments);
                    record.setUnit("mm");        // 默认单位
                    
                    records.add(record);
                } catch (Exception e) {
                    System.err.println("错误解析行: " + line + " - " + e.getMessage());
                }
            }
        }
        
        return FXCollections.observableArrayList(records);
    }
    
    /**
     * 生成测试数据
     * @param pointCount 测点数量
     * @param daysCount 天数
     * @return 测量记录列表
     */
    public static ObservableList<MeasurementRecord> generateSampleData(int pointCount, int daysCount) {
        List<MeasurementRecord> records = new ArrayList<>();
        LocalDate startDate = LocalDate.now().minusDays(daysCount);
        
        for (int point = 1; point <= pointCount; point++) {
            double baseValue = Math.random() * 10;
            double trend = Math.random() * 0.5;
            
            for (int day = 0; day < daysCount; day++) {
                LocalDate date = startDate.plusDays(day);
                // 添加一些随机波动以及整体下沉趋势
                double noise = (Math.random() - 0.5) * 0.3;
                double value = baseValue + day * trend + noise;
                
                MeasurementRecord record = new MeasurementRecord();
                record.setId("P" + String.format("%03d", point));
                record.setMeasureTime(LocalDateTime.of(date, LocalTime.NOON));
                record.setValue(value);
                record.setOperator("系统");
                record.setUnit("mm");
                record.setComments("模拟数据");
                record.setWarningLevel(value > 5 ? 1 : 0); // 简单预警
                
                records.add(record);
            }
        }
        
        return FXCollections.observableArrayList(records);
    }
    
    /**
     * 按测点ID获取唯一测点列表
     * @param records 所有测量记录
     * @return 唯一测点ID列表
     */
    public static List<String> getUniquePointIds(ObservableList<MeasurementRecord> records) {
        return records.stream()
                .map(MeasurementRecord::getId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
    
    /**
     * 按日期获取唯一日期列表
     * @param records 所有测量记录
     * @return 唯一日期列表
     */
    public static List<LocalDate> getUniqueDates(ObservableList<MeasurementRecord> records) {
        return records.stream()
                .map(r -> r.getMeasureTime().toLocalDate())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
    
    /**
     * 筛选特定测点的记录
     * @param records 所有测量记录
     * @param pointId 测点ID
     * @return 该测点的记录
     */
    public static ObservableList<MeasurementRecord> filterByPointId(ObservableList<MeasurementRecord> records, String pointId) {
        return records.stream()
                .filter(r -> r.getId().equals(pointId))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
    }
    
    /**
     * 筛选特定日期的记录
     * @param records 所有测量记录
     * @param date 日期
     * @return 该日期的记录
     */
    public static ObservableList<MeasurementRecord> filterByDate(ObservableList<MeasurementRecord> records, LocalDate date) {
        return records.stream()
                .filter(r -> r.getMeasureTime().toLocalDate().equals(date))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
    }
    
    /**
     * 为特定测点创建图表数据系列
     * @param records 该测点的所有记录（应已排序）
     * @return 图表数据系列
     */
    public static XYChart.Series<Number, Number> createChartSeries(List<MeasurementRecord> records, String seriesName) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(seriesName);
        
        for (int i = 0; i < records.size(); i++) {
            MeasurementRecord record = records.get(i);
            series.getData().add(new XYChart.Data<>(i+1, record.getValue()));
        }
        
        return series;
    }
    
    /**
     * 计算累计沉降值
     * 注意：此方法会在每个记录中储存额外的数据，但不会持久化
     * @param records 测量记录列表
     * @return 按测点ID分组的记录
     */
    public static Map<String, List<MeasurementRecord>> calculateDerivedValues(ObservableList<MeasurementRecord> records) {
        Map<String, List<MeasurementRecord>> pointGroups = new HashMap<>();
        
        // 按测点分组
        for (MeasurementRecord record : records) {
            pointGroups.computeIfAbsent(record.getId(), k -> new ArrayList<>()).add(record);
        }
        
        // 对每个测点，计算累计沉降
        for (List<MeasurementRecord> pointRecords : pointGroups.values()) {
            // 按时间排序
            pointRecords.sort((r1, r2) -> r1.getMeasureTime().compareTo(r2.getMeasureTime()));
            
            // 这里我们跟踪额外数据，因为MeasurementRecord没有累计值和速率字段
            double initialValue = pointRecords.get(0).getValue();
            Map<MeasurementRecord, Double> accumulatedValues = new HashMap<>();
            Map<MeasurementRecord, Double> rates = new HashMap<>();
            
            MeasurementRecord prevRecord = null;
            
            for (MeasurementRecord record : pointRecords) {
                // 累计值 = 初始值 - 当前值
                double accumulatedValue = initialValue - record.getValue();
                accumulatedValues.put(record, accumulatedValue);
                
                // 计算速率
                if (prevRecord != null) {
                    long hoursBetween = java.time.temporal.ChronoUnit.HOURS.between(
                            prevRecord.getMeasureTime(), record.getMeasureTime());
                    if (hoursBetween > 0) {
                        double valueDiff = record.getValue() - prevRecord.getValue();
                        double dailyRate = (Math.abs(valueDiff) / hoursBetween) * 24; // 转换为每天
                        rates.put(record, dailyRate);
                    }
                }
                
                prevRecord = record;
            }
            
            // 为首个记录设置速率为0
            if (!pointRecords.isEmpty() && !rates.containsKey(pointRecords.get(0))) {
                rates.put(pointRecords.get(0), 0.0);
            }
        }
        
        return pointGroups;
    }
} 