package com.monitor.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.monitor.model.SettlementDataStorage;
import com.monitor.model.PileDisplacementDataStorage;
import com.monitor.model.ColumnDisplacementDataStorage;
import com.monitor.model.GroundwaterLevelDataStorage;
import com.monitor.model.BuildingSettlementDataStorage;
import com.monitor.model.SteelSupportAxialForceDataStorage;
import com.monitor.model.ConcreteSupportAxialForceDataStorage;
import com.monitor.model.DeepHorizontalDisplacementDataStorage;
import com.monitor.model.PileTopHorizontalDisplacementDataStorage;

/**
 * 监测测项模型类
 * 用于表示单个监测测点的属性和数据
 */
public class MonitoringItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String type;        // 测项类型：沉降、位移、倾斜、应力、水位等
    private String location;    // 测点位置描述
    private String description; // 测点详细描述
    private String unit;        // 测量单位
    private double warningLevel1; // 一级预警阈值
    private double warningLevel2; // 二级预警阈值
    private double warningLevel3; // 三级预警阈值
    private LocalDateTime installTime; // 安装时间
    private LocalDateTime lastMeasureTime; // 最后测量时间
    private List<MeasurementRecord> records; // 测量记录
    private SettlementDataStorage settlementDataStorage; // 沉降数据存储
    private PileDisplacementDataStorage pileDisplacementDataStorage; // 桩顶竖向位移数据存储
    private ColumnDisplacementDataStorage columnDisplacementDataStorage; // 立柱竖向位移数据存储
    private GroundwaterLevelDataStorage groundwaterLevelDataStorage; // 地下水位数据存储
    private BuildingSettlementDataStorage buildingSettlementDataStorage; // 建筑物沉降数据存储
    private SteelSupportAxialForceDataStorage steelSupportAxialForceDataStorage; // 钢支撑轴力数据存储
    private ConcreteSupportAxialForceDataStorage concreteSupportAxialForceDataStorage; // 砼支撑轴力数据存储
    private DeepHorizontalDisplacementDataStorage deepHorizontalDisplacementDataStorage; // 深部水平位移数据存储
    private PileTopHorizontalDisplacementDataStorage pileTopHorizontalDisplacementDataStorage; // 桩顶水平位移数据存储

    /**
     * 默认构造函数
     */
    public MonitoringItem() {
        this.records = new ArrayList<>();
    }

    /**
     * 带参数的构造函数
     */
    public MonitoringItem(String id, String name, String type, String location) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.location = location;
        this.records = new ArrayList<>();
    }

    /**
     * 添加测量记录
     */
    public void addRecord(MeasurementRecord record) {
        if (records == null) {
            records = new ArrayList<>();
        }
        records.add(record);
        // 更新最后测量时间
        if (record.getMeasureTime() != null) {
            if (lastMeasureTime == null || record.getMeasureTime().isAfter(lastMeasureTime)) {
                lastMeasureTime = record.getMeasureTime();
            }
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getWarningLevel1() {
        return warningLevel1;
    }

    public void setWarningLevel1(double warningLevel1) {
        this.warningLevel1 = warningLevel1;
    }

    public double getWarningLevel2() {
        return warningLevel2;
    }

    public void setWarningLevel2(double warningLevel2) {
        this.warningLevel2 = warningLevel2;
    }

    public double getWarningLevel3() {
        return warningLevel3;
    }

    public void setWarningLevel3(double warningLevel3) {
        this.warningLevel3 = warningLevel3;
    }

    public LocalDateTime getInstallTime() {
        return installTime;
    }

    public void setInstallTime(LocalDateTime installTime) {
        this.installTime = installTime;
    }

    public LocalDateTime getLastMeasureTime() {
        return lastMeasureTime;
    }

    public void setLastMeasureTime(LocalDateTime lastMeasureTime) {
        this.lastMeasureTime = lastMeasureTime;
    }

    public List<MeasurementRecord> getRecords() {
        return records;
    }

    public void setRecords(List<MeasurementRecord> records) {
        this.records = records;
    }

    public SettlementDataStorage getSettlementDataStorage() {
        return settlementDataStorage;
    }

    public void setSettlementDataStorage(SettlementDataStorage settlementDataStorage) {
        this.settlementDataStorage = settlementDataStorage;
    }
    
    public PileDisplacementDataStorage getPileDisplacementDataStorage() {
        return pileDisplacementDataStorage;
    }

    public void setPileDisplacementDataStorage(PileDisplacementDataStorage pileDisplacementDataStorage) {
        this.pileDisplacementDataStorage = pileDisplacementDataStorage;
    }
    
    public ColumnDisplacementDataStorage getColumnDisplacementDataStorage() {
        return columnDisplacementDataStorage;
    }

    public void setColumnDisplacementDataStorage(ColumnDisplacementDataStorage columnDisplacementDataStorage) {
        this.columnDisplacementDataStorage = columnDisplacementDataStorage;
    }

    /**
     * 获取或创建立柱竖向位移数据存储对象
     * 如果存储对象已存在，直接返回；否则创建一个新的存储对象
     * @return 立柱竖向位移数据存储对象
     */
    public ColumnDisplacementDataStorage getOrCreateColumnDisplacementDataStorage() {
        if (this.columnDisplacementDataStorage == null) {
            this.columnDisplacementDataStorage = new ColumnDisplacementDataStorage();
        }
        return this.columnDisplacementDataStorage;
    }

    /**
     * 获取最新的测量值
     */
    public double getLatestValue() {
        if (records == null || records.isEmpty()) {
            return 0.0;
        }

        MeasurementRecord latest = null;
        for (MeasurementRecord record : records) {
            if (latest == null ||
                (record.getMeasureTime() != null &&
                latest.getMeasureTime() != null &&
                record.getMeasureTime().isAfter(latest.getMeasureTime()))) {
                latest = record;
            }
        }

        return latest != null ? latest.getValue() : 0.0;
    }

    /**
     * 获取当前预警级别
     * @return 0=正常, 1-3表示不同警告级别
     */
    public int getCurrentWarningLevel() {
        double latestValue = getLatestValue();

        if (latestValue >= warningLevel3) {
            return 3;
        } else if (latestValue >= warningLevel2) {
            return 2;
        } else if (latestValue >= warningLevel1) {
            return 1;
        } else {
            return 0;
        }
    }

    public GroundwaterLevelDataStorage getGroundwaterLevelDataStorage() {
        return groundwaterLevelDataStorage;
    }

    public void setGroundwaterLevelDataStorage(GroundwaterLevelDataStorage groundwaterLevelDataStorage) {
        this.groundwaterLevelDataStorage = groundwaterLevelDataStorage;
    }

    /**
     * 获取或创建地下水位数据存储对象
     * 如果存储对象已存在，直接返回；否则创建一个新的存储对象
     * @return 地下水位数据存储对象
     */
    public GroundwaterLevelDataStorage getOrCreateGroundwaterLevelDataStorage() {
        if (this.groundwaterLevelDataStorage == null) {
            this.groundwaterLevelDataStorage = new GroundwaterLevelDataStorage();
        }
        return this.groundwaterLevelDataStorage;
    }
    
    public BuildingSettlementDataStorage getBuildingSettlementDataStorage() {
        return buildingSettlementDataStorage;
    }

    public void setBuildingSettlementDataStorage(BuildingSettlementDataStorage buildingSettlementDataStorage) {
        this.buildingSettlementDataStorage = buildingSettlementDataStorage;
    }

    /**
     * 获取或创建建筑物沉降数据存储对象
     * 如果存储对象已存在，直接返回；否则创建一个新的存储对象
     * @return 建筑物沉降数据存储对象
     */
    public BuildingSettlementDataStorage getOrCreateBuildingSettlementDataStorage() {
        if (this.buildingSettlementDataStorage == null) {
            this.buildingSettlementDataStorage = new BuildingSettlementDataStorage();
        }
        return this.buildingSettlementDataStorage;
    }

    /**
     * 获取钢支撑轴力数据存储对象
     */
    public SteelSupportAxialForceDataStorage getSteelSupportAxialForceDataStorage() {
        return steelSupportAxialForceDataStorage;
    }

    /**
     * 设置钢支撑轴力数据存储对象
     */
    public void setSteelSupportAxialForceDataStorage(SteelSupportAxialForceDataStorage steelSupportAxialForceDataStorage) {
        this.steelSupportAxialForceDataStorage = steelSupportAxialForceDataStorage;
    }

    /**
     * 获取或创建钢支撑轴力数据存储对象
     */
    public SteelSupportAxialForceDataStorage getOrCreateSteelSupportAxialForceDataStorage() {
        if (this.steelSupportAxialForceDataStorage == null) {
            this.steelSupportAxialForceDataStorage = new SteelSupportAxialForceDataStorage();
        }
        return this.steelSupportAxialForceDataStorage;
    }

    /**
     * 获取砼支撑轴力数据存储对象
     */
    public ConcreteSupportAxialForceDataStorage getConcreteSupportAxialForceDataStorage() {
        return concreteSupportAxialForceDataStorage;
    }

    /**
     * 设置砼支撑轴力数据存储对象
     */
    public void setConcreteSupportAxialForceDataStorage(ConcreteSupportAxialForceDataStorage concreteSupportAxialForceDataStorage) {
        this.concreteSupportAxialForceDataStorage = concreteSupportAxialForceDataStorage;
    }

    /**
     * 获取或创建砼支撑轴力数据存储对象
     */
    public ConcreteSupportAxialForceDataStorage getOrCreateConcreteSupportAxialForceDataStorage() {
        if (this.concreteSupportAxialForceDataStorage == null) {
            this.concreteSupportAxialForceDataStorage = new ConcreteSupportAxialForceDataStorage();
        }
        return this.concreteSupportAxialForceDataStorage;
    }

    /**
     * 获取深部水平位移数据存储对象
     */
    public DeepHorizontalDisplacementDataStorage getDeepHorizontalDisplacementDataStorage() {
        return deepHorizontalDisplacementDataStorage;
    }

    /**
     * 设置深部水平位移数据存储对象
     */
    public void setDeepHorizontalDisplacementDataStorage(DeepHorizontalDisplacementDataStorage deepHorizontalDisplacementDataStorage) {
        this.deepHorizontalDisplacementDataStorage = deepHorizontalDisplacementDataStorage;
    }

    /**
     * 获取或创建深部水平位移数据存储对象
     * 如果存储对象已存在，直接返回；否则创建一个新的存储对象
     * @return 深部水平位移数据存储对象
     */
    public DeepHorizontalDisplacementDataStorage getOrCreateDeepHorizontalDisplacementDataStorage() {
        if (this.deepHorizontalDisplacementDataStorage == null) {
            this.deepHorizontalDisplacementDataStorage = new DeepHorizontalDisplacementDataStorage();
        }
        return this.deepHorizontalDisplacementDataStorage;
    }
    
    /**
     * 获取桩顶水平位移数据存储
     */
    public PileTopHorizontalDisplacementDataStorage getPileTopHorizontalDisplacementDataStorage() {
        return pileTopHorizontalDisplacementDataStorage;
    }
    
    /**
     * 设置桩顶水平位移数据存储
     */
    public void setPileTopHorizontalDisplacementDataStorage(PileTopHorizontalDisplacementDataStorage pileTopHorizontalDisplacementDataStorage) {
        this.pileTopHorizontalDisplacementDataStorage = pileTopHorizontalDisplacementDataStorage;
    }
    
    /**
     * 获取或创建桩顶水平位移数据存储对象
     * 如果存储对象已存在，直接返回；否则创建一个新的存储对象
     * @return 桩顶水平位移数据存储对象
     */
    public PileTopHorizontalDisplacementDataStorage getOrCreatePileTopHorizontalDisplacementDataStorage() {
        if (this.pileTopHorizontalDisplacementDataStorage == null) {
            this.pileTopHorizontalDisplacementDataStorage = new PileTopHorizontalDisplacementDataStorage();
        }
        return this.pileTopHorizontalDisplacementDataStorage;
    }
}