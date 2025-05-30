package com.monitor.controller;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.monitor.model.BuildingSettlementData;
import com.monitor.model.BuildingSettlementDataStorage;
import com.monitor.model.BuildingSettlementPoint;
import com.monitor.model.MeasurementRecord;
import com.monitor.util.AlertUtil;
import com.monitor.util.ExcelUtil;
import com.monitor.view.BuildingSettlementPointSettingsController;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * 建筑物沉降数据管理控制器
 */
public class BuildingSettlementController {

    @FXML private Label titleLabel;
    @FXML private Button uploadDataButton;
    @FXML private DatePicker datePicker;
    @FXML private Button exportButton;
    @FXML private Button monitoringPointSettingsButton;

    @FXML private FlowPane dataBlocksFlowPane;

    @FXML private TabPane analysisTabPane;
    @FXML private Tab tableAnalysisTab;
    @FXML private Tab chartAnalysisTab;

    @FXML private TableView<BuildingSettlementData> dataTableView;
    @FXML private TableColumn<BuildingSettlementData, Number> serialNumberColumn;
    @FXML private TableColumn<BuildingSettlementData, String> pointCodeColumn;
    @FXML private TableColumn<BuildingSettlementData, Number> initialElevationColumn;
    @FXML private TableColumn<BuildingSettlementData, Number> previousElevationColumn;
    @FXML private TableColumn<BuildingSettlementData, Number> currentElevationColumn;
    @FXML private TableColumn<BuildingSettlementData, Number> currentChangeColumn;
    @FXML private TableColumn<BuildingSettlementData, Number> cumulativeChangeColumn;
    @FXML private TableColumn<BuildingSettlementData, Number> changeRateColumn;
    @FXML private TableColumn<BuildingSettlementData, String> mileageColumn;
    @FXML private TableColumn<BuildingSettlementData, Number> historicalCumulativeColumn;

    @FXML private BorderPane chartContainer;

    private LineChart<String, Number> displacementChart;
    private LineChart<String, Number> rateChart;
    private NumberAxis displacementYAxis;
    private CategoryAxis displacementXAxis;
    private NumberAxis rateYAxis;
    private CategoryAxis rateXAxis;

    @FXML private ToggleButton displacementChartButton;
    @FXML private ToggleButton rateChartButton;
    private ToggleGroup chartToggleGroup;

    @FXML private Label pointCountLabel;
    @FXML private Label uploadDateLabel;

    private ObservableList<BuildingSettlementData> settlementDataList = FXCollections.observableArrayList();
    private Stage stage;

    // 按测点ID和日期存储的所有数据
    private Map<String, Map<LocalDate, BuildingSettlementData>> allPointDataMap = new HashMap<>();

    // 按上传日期存储的数据块
    private Map<LocalDateTime, List<BuildingSettlementData>> dataBlocksMap = new HashMap<>();

    // 存储数据块复选框，方便后续更新选中状态
    private Map<LocalDateTime, CheckBox> dataBlockCheckBoxMap = new HashMap<>();

    // 当前选中的数据块时间戳列表
    private List<LocalDateTime> selectedDataBlocks = new ArrayList<>();

    // 已配置的测点列表
    private List<BuildingSettlementPoint> configuredPoints = new ArrayList<>();
    
    // 自定义速率计算天数
    private int customDaysForRateCalculation = 0;

    /**
     * 设置舞台
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * 初始化控制器
     */
    @FXML
    public void initialize() {
        // 初始化图表
        initializeCharts();
        
        // 设置表格列
        setupTableColumns();
        
        // 配置数值列的格式
        configureNumberColumns();
        
        // 设置表格数据源
        dataTableView.setItems(settlementDataList);
        
        // 初始化日期选择器
        datePicker.setValue(LocalDate.now());
        
        // 加载测点数据
        loadSettlementPoints();
        
        // 更新表格初始数据
        updateTableWithInitialData();
        
        // 添加表格上下文菜单
        setupTableContextMenu();
    }
    
    /**
     * 设置表格上下文菜单
     */
    private void setupTableContextMenu() {
        // 创建上下文菜单项
        javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
        
        // 添加设置自定义天数菜单项
        javafx.scene.control.MenuItem customDaysItem = new javafx.scene.control.MenuItem("设置自定义天数...");
        customDaysItem.setOnAction(e -> showSetCustomDaysDialog());
        
        // 添加重置为默认天数菜单项
        javafx.scene.control.MenuItem resetDaysItem = new javafx.scene.control.MenuItem("使用实际日期间隔");
        resetDaysItem.setOnAction(e -> {
            customDaysForRateCalculation = 0;
            updateTableBasedOnSelection();
            AlertUtil.showInformation("设置成功", "将使用实际测量日期间隔计算变化速率");
        });
        
        // 将菜单项添加到上下文菜单
        contextMenu.getItems().addAll(customDaysItem, resetDaysItem);
        
        // 设置表格的上下文菜单
        dataTableView.setContextMenu(contextMenu);
    }
    
    /**
     * 处理上传数据按钮点击事件
     */
    @FXML
    private void handleUploadButtonAction(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择建筑物沉降数据文件");
        fileChooser.getExtensionFilters().addAll(
                new ExtensionFilter("Excel文件", "*.xlsx"),
                new ExtensionFilter("所有文件", "*.*"));
        
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            processSelectedExcelFile(selectedFile);
        }
    }
    
    /**
     * 处理导出按钮点击事件
     */
    @FXML
    private void handleExportButtonAction(ActionEvent event) {
        // 确保有数据可导出
        if (settlementDataList.isEmpty()) {
            AlertUtil.showWarning("导出失败", "没有数据可导出");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出建筑物沉降数据");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("Excel文件", "*.xlsx"));
        fileChooser.setInitialFileName("建筑物沉降数据.xlsx");
        
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                // 准备导出数据
                List<String> headers = new ArrayList<>();
                headers.add("测点编号");
                headers.add("初始高程(m)");
                headers.add("上次高程(m)");
                headers.add("本次高程(m)");
                headers.add("本次变化量(mm)");
                headers.add("累计变化量(mm)");
                headers.add("变化速率(mm/d)");
                headers.add("里程");
                headers.add("历史累计值(mm)");
                
                List<List<String>> data = new ArrayList<>();
                for (BuildingSettlementData rowData : settlementDataList) {
                    List<String> row = new ArrayList<>();
                    row.add(rowData.getPointCode());
                    row.add(String.format("%.4f", rowData.getInitialElevation()));
                    row.add(String.format("%.4f", rowData.getPreviousElevation()));
                    row.add(String.format("%.4f", rowData.getCurrentElevation()));
                    row.add(String.format("%.2f", rowData.getCurrentChange()));
                    row.add(String.format("%.2f", rowData.getCumulativeChange()));
                    row.add(String.format("%.2f", rowData.getChangeRate()));
                    row.add(rowData.getMileage());
                    row.add(String.format("%.2f", rowData.getHistoricalCumulative()));
                    data.add(row);
                }
                
                // 导出到Excel
                ExcelUtil.exportToExcel(file, "建筑物沉降数据", headers, data);
                
                AlertUtil.showInformation("导出成功", "数据已成功导出到文件:\n" + file.getPath());
            } catch (Exception e) {
                AlertUtil.showError("导出失败", "导出数据时发生错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 处理测点设置按钮点击事件
     */
    @FXML
    private void handleSettingsButtonAction(ActionEvent event) {
        try {
            // 加载测点设置对话框
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/BuildingSettlementPointSettingsDialog.fxml"));
            Scene scene = new Scene(loader.load());
            
            // 配置对话框控制器
            BuildingSettlementPointSettingsController controller = loader.getController();
            
            // 创建对话框舞台
            Stage dialogStage = new Stage();
            dialogStage.setTitle("建筑物沉降测点设置");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(stage);
            dialogStage.setScene(scene);
            
            // 设置对话框控制器
            controller.setDialogStage(dialogStage);
            controller.setInitialData(configuredPoints);
            
            // 显示对话框并等待关闭
            dialogStage.showAndWait();
            
            // 更新测点配置
            List<BuildingSettlementPoint> updatedPoints = controller.getPoints();
            if (updatedPoints != null) {
                configuredPoints.clear();
                configuredPoints.addAll(updatedPoints);
                
                // 更新测点数量显示
                updatePointCount();
                
                // 刷新数据
                updateTableBasedOnSelection();
            }
        } catch (Exception e) {
            AlertUtil.showError("打开设置对话框失败", "无法打开测点设置对话框: " + e.getMessage());
        }
    }

    /**
     * 处理数据块选择变更
     */
    private void handleDataBlockSelection(CheckBox checkBox) {
        LocalDateTime timestamp = (LocalDateTime) checkBox.getUserData();
        
        if (checkBox.isSelected()) {
            // 如果选中了数据块，添加到选中列表
            if (!selectedDataBlocks.contains(timestamp)) {
                selectedDataBlocks.add(timestamp);
            }
        } else {
            // 如果取消选中，从列表中移除
            selectedDataBlocks.remove(timestamp);
        }
        
        // 限制最多选择两个数据块进行比较
        if (selectedDataBlocks.size() > 2) {
            LocalDateTime firstSelected = selectedDataBlocks.get(0);
            selectedDataBlocks.remove(0);
            
            // 更新UI中对应数据块的复选框状态
            CheckBox firstCheckBox = dataBlockCheckBoxMap.get(firstSelected);
            if (firstCheckBox != null) {
                firstCheckBox.setSelected(false);
            }
        }
        
        // 更新表格显示
        updateTableBasedOnSelection();
    }
    
    /**
     * 添加数据块
     */
    private void addDataBlock(String fileName, LocalDateTime dateTime) {
        // 创建数据块HBox容器
        HBox dataBlockBox = new HBox(5);
        dataBlockBox.getStyleClass().add("data-block");
        dataBlockBox.setPadding(new Insets(5));
        dataBlockBox.setUserData(dateTime); // 存储时间戳为用户数据，便于后续操作
        
        // 创建数据块复选框，显示完整的日期时间，包含秒
        CheckBox checkBox = new CheckBox(dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        checkBox.setUserData(dateTime);
        checkBox.setOnAction(e -> handleDataBlockSelection(checkBox));
        
        dataBlockBox.getChildren().add(checkBox);
        
        // 添加复选框到UI
        dataBlocksFlowPane.getChildren().add(dataBlockBox);
        dataBlockCheckBoxMap.put(dateTime, checkBox);
    }
    
    /**
     * 根据测点ID获取关联的测点配置
     */
    private BuildingSettlementPoint getSettlementPointById(String pointId) {
        for (BuildingSettlementPoint point : configuredPoints) {
            if (point.getPointId().equals(pointId)) {
                return point;
            }
        }
        return null;
    }
    
    /**
     * 处理选择的Excel文件
     */
    private void processSelectedExcelFile(File file) {
        try {
            // 检查Excel文件是否包含"建筑物沉降"工作表
            boolean hasRequiredSheet = false;
            try (FileInputStream fis = new FileInputStream(file);
                 Workbook workbook = WorkbookFactory.create(fis)) {
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    if (workbook.getSheetName(i).equals("建筑物沉降")) {
                        hasRequiredSheet = true;
                        break;
                    }
                }
            }
            
            if (!hasRequiredSheet) {
                AlertUtil.showWarning("导入警告", "Excel文件中未找到名为\"建筑物沉降\"的工作表，请检查文件格式！");
                return;
            }
            
            // 解析Excel文件，使用"建筑物沉降"工作表
            Map<String, Double> pointElevationMap = ExcelUtil.importFromExcel(file, "建筑物沉降");
            
            if (pointElevationMap == null || pointElevationMap.isEmpty()) {
                AlertUtil.showError("导入失败", "无法从文件中读取数据或文件格式不正确");
                return;
            }
            
            // 使用日期选择器中的日期
            LocalDate measureDate = datePicker.getValue();
            if (measureDate == null) {
                AlertUtil.showError("导入失败", "请选择测量日期");
                return;
            }
            
            // 处理Excel数据
            List<BuildingSettlementData> processedData = processExcelPointData(pointElevationMap, measureDate);
            
            if (processedData.isEmpty()) {
                AlertUtil.showWarning("导入警告", "没有任何有效数据被导入，请检查Excel文件格式或测点配置");
                return;
            }
            
            // 移除同一天数据冲突的检查，允许同一天多次上传数据
            // 创建新数据块
            LocalDateTime uploadTime = LocalDateTime.now();
            dataBlocksMap.put(uploadTime, processedData);
            
            // 添加数据到UI
            addDataBlock(file.getName(), uploadTime);
            
            // 自动选中新导入的数据块
            CheckBox newCheckBox = dataBlockCheckBoxMap.get(uploadTime);
            if (newCheckBox != null) {
                newCheckBox.setSelected(true);
                handleDataBlockSelection(newCheckBox);
            }
            
            // 更新上传日期显示
            uploadDateLabel.setText(measureDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            
            // 更新测点数量显示
            updatePointCount();
            
            AlertUtil.showInformation("导入成功", String.format("成功导入%d个测点的建筑物沉降数据", processedData.size()));
        } catch (Exception e) {
            AlertUtil.showError("导入失败", "处理Excel文件时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 根据选择的数据块更新表格
     */
    private void updateTableBasedOnSelection() {
        // 清空当前表格数据
        settlementDataList.clear();
        
        if (selectedDataBlocks.isEmpty()) {
            return;
        }
        
        if (selectedDataBlocks.size() == 1) {
            // 只选择了一个数据块
            LocalDateTime timestamp = selectedDataBlocks.get(0);
            List<BuildingSettlementData> dataList = dataBlocksMap.get(timestamp);
            
            if (dataList != null) {
                // 创建一个副本进行处理
                List<BuildingSettlementData> processedDataList = new ArrayList<>();
                
                for (BuildingSettlementData data : dataList) {
                    if (data != null) {
                        // 克隆数据以避免影响原始数据
                        BuildingSettlementData processedData = new BuildingSettlementData();
                        processedData.setPointCode(data.getPointCode());
                        processedData.setInitialElevation(data.getInitialElevation());
                        processedData.setCurrentElevation(data.getCurrentElevation());
                        processedData.setPreviousElevation(data.getInitialElevation()); // 单个数据块时使用初始高程作为前期高程
                        processedData.setMileage(data.getMileage());
                        processedData.setHistoricalCumulative(data.getHistoricalCumulative());
                        processedData.setMeasurementDate(data.getMeasurementDate());
                        
                        // 计算派生值
                        processedData.calculateDerivedValues();
                        
                        processedDataList.add(processedData);
                    }
                }
                
                // 按测点配置的顺序排序数据
                processedDataList.sort((d1, d2) -> {
                    BuildingSettlementPoint p1 = getSettlementPointById(d1.getPointCode());
                    BuildingSettlementPoint p2 = getSettlementPointById(d2.getPointCode());
                    if (p1 != null && p2 != null) {
                        return Integer.compare(p1.getOrderIndex(), p2.getOrderIndex());
                    } else if (p1 != null) {
                        return -1;
                    } else if (p2 != null) {
                        return 1;
                    } else {
                        return d1.getPointCode().compareTo(d2.getPointCode());
                    }
                });
                
                // 添加到表格数据列表
                settlementDataList.addAll(processedDataList);
            }
        } else if (selectedDataBlocks.size() == 2) {
            // 选择了两个数据块，需要比较
            // 按时间排序，确保前期在前，当前在后
            Collections.sort(selectedDataBlocks);
            
            LocalDateTime previousTime = selectedDataBlocks.get(0);
            LocalDateTime currentTime = selectedDataBlocks.get(1);
            
            List<BuildingSettlementData> previousDataList = dataBlocksMap.get(previousTime);
            List<BuildingSettlementData> currentDataList = dataBlocksMap.get(currentTime);
            
            if (previousDataList != null && currentDataList != null) {
                // 创建一个映射，便于快速查找前期数据
                Map<String, BuildingSettlementData> previousDataMap = new HashMap<>();
                for (BuildingSettlementData data : previousDataList) {
                    if (data != null) {
                        previousDataMap.put(data.getPointCode(), data);
                    }
                }
                
                // 处理当前数据
                List<BuildingSettlementData> processedDataList = new ArrayList<>();
                
                for (BuildingSettlementData currentData : currentDataList) {
                    if (currentData == null) continue;
                    
                    String pointCode = currentData.getPointCode();
                    BuildingSettlementData previousData = previousDataMap.get(pointCode);
                    
                    // 克隆当前数据以避免影响原始数据
                    BuildingSettlementData processedData = new BuildingSettlementData();
                    processedData.setPointCode(currentData.getPointCode());
                    processedData.setInitialElevation(currentData.getInitialElevation());
                    processedData.setCurrentElevation(currentData.getCurrentElevation());
                    processedData.setMileage(currentData.getMileage());
                    processedData.setHistoricalCumulative(currentData.getHistoricalCumulative());
                    processedData.setMeasurementDate(currentData.getMeasurementDate());
                    
                    if (previousData != null) {
                        // 设置前期高程
                        processedData.setPreviousElevation(previousData.getCurrentElevation());
                        
                        // 计算派生值，考虑实际日期
                        processedData.calculateDerivedValues(
                                previousData.getMeasurementDate(),
                                currentData.getMeasurementDate(),
                                customDaysForRateCalculation);
                    } else {
                        // 如果没有对应的前期数据，使用初始高程作为前期高程
                        processedData.setPreviousElevation(processedData.getInitialElevation());
                        processedData.calculateDerivedValues();
                    }
                    
                    processedDataList.add(processedData);
                }
                
                // 按测点配置的顺序排序数据
                processedDataList.sort((d1, d2) -> {
                    BuildingSettlementPoint p1 = getSettlementPointById(d1.getPointCode());
                    BuildingSettlementPoint p2 = getSettlementPointById(d2.getPointCode());
                    if (p1 != null && p2 != null) {
                        return Integer.compare(p1.getOrderIndex(), p2.getOrderIndex());
                    } else if (p1 != null) {
                        return -1;
                    } else if (p2 != null) {
                        return 1;
                    } else {
                        return d1.getPointCode().compareTo(d2.getPointCode());
                    }
                });
                
                // 添加到表格数据列表
                settlementDataList.addAll(processedDataList);
            }
        }
        
        // 更新图表
        updateChart();
    }
    
    /**
     * 获取当前存储的数据用于保存
     */
    public List<MeasurementRecord> getMeasurementRecordsForSaving() {
        List<MeasurementRecord> records = new ArrayList<>();
        
        // 遍历所有数据块
        for (Map.Entry<LocalDateTime, List<BuildingSettlementData>> entry : dataBlocksMap.entrySet()) {
            LocalDateTime timestamp = entry.getKey();
            List<BuildingSettlementData> dataList = entry.getValue();
            
            for (BuildingSettlementData data : dataList) {
                MeasurementRecord record = new MeasurementRecord();
                
                // 使用测点编号和时间戳创建唯一ID
                record.setId(data.getPointCode() + "_" + timestamp.toString());
                record.setValue(data.getCurrentElevation());
                record.setMeasureTime(timestamp);
                
                // 存储其他必要信息
                StringBuilder comments = new StringBuilder();
                comments.append("initialElevation=").append(data.getInitialElevation()).append(";");
                comments.append("previousElevation=").append(data.getPreviousElevation()).append(";");
                comments.append("mileage=").append(data.getMileage()).append(";");
                comments.append("historicalCumulative=").append(data.getHistoricalCumulative()).append(";");
                record.setComments(comments.toString());
                
                records.add(record);
            }
        }
        
        return records;
    }
    
    /**
     * 获取数据存储对象，用于保存
     */
    public BuildingSettlementDataStorage getBuildingSettlementDataStorage() {
        BuildingSettlementDataStorage storage = new BuildingSettlementDataStorage();
        
        // 保存测点配置
        storage.setPoints(new ArrayList<>(configuredPoints));
        
        // 保存数据块
        for (Map.Entry<LocalDateTime, List<BuildingSettlementData>> entry : dataBlocksMap.entrySet()) {
            LocalDateTime timestamp = entry.getKey();
            List<BuildingSettlementData> dataList = entry.getValue();
            storage.addDataBlock(timestamp, new ArrayList<>(dataList), timestamp.toString());
        }
        
        // 保存选中的数据块
        storage.setSelectedDataBlocks(new ArrayList<>(selectedDataBlocks));
        
        // 保存自定义天数设置
        storage.setCustomDaysForRateCalculation(customDaysForRateCalculation);
        
        return storage;
    }

    /**
     * 设置表格列
     */
    private void setupTableColumns() {
        // 设置列数据工厂
        serialNumberColumn.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<Number>(dataTableView.getItems().indexOf(cellData.getValue()) + 1));
        
        pointCodeColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getPointCode()));
        
        initialElevationColumn.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<Number>(cellData.getValue().getInitialElevation()));
        
        previousElevationColumn.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<Number>(cellData.getValue().getPreviousElevation()));
        
        currentElevationColumn.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<Number>(cellData.getValue().getCurrentElevation()));
        
        currentChangeColumn.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<Number>(cellData.getValue().getCurrentChange()));
        
        cumulativeChangeColumn.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<Number>(cellData.getValue().getCumulativeChange()));
        
        changeRateColumn.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<Number>(cellData.getValue().getChangeRate()));
        
        mileageColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getMileage()));
        
        historicalCumulativeColumn.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<Number>(cellData.getValue().getHistoricalCumulative()));
    }
    
    /**
     * 配置数值列的格式
     */
    private void configureNumberColumns() {
        // 设置数值列的格式化显示
        initialElevationColumn.setCellFactory(column -> new TableCell<BuildingSettlementData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.4f", item.doubleValue()));
                }
            }
        });
        
        previousElevationColumn.setCellFactory(column -> new TableCell<BuildingSettlementData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.4f", item.doubleValue()));
                }
            }
        });
        
        currentElevationColumn.setCellFactory(column -> new TableCell<BuildingSettlementData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.4f", item.doubleValue()));
                }
            }
        });
        
        currentChangeColumn.setCellFactory(column -> new TableCell<BuildingSettlementData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item.doubleValue()));
                    
                    // 根据变化值的正负设置颜色
                    double value = item.doubleValue();
                    if (value < 0) {
                        setStyle("-fx-text-fill: red;"); // 负值显示为红色
                    } else if (value > 0) {
                        setStyle("-fx-text-fill: green;"); // 正值显示为绿色
                    } else {
                        setStyle(""); // 零值使用默认颜色
                    }
                }
            }
        });
        
        cumulativeChangeColumn.setCellFactory(column -> new TableCell<BuildingSettlementData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item.doubleValue()));
                    
                    // 根据变化值的正负设置颜色
                    double value = item.doubleValue();
                    if (value < 0) {
                        setStyle("-fx-text-fill: red;"); // 负值显示为红色
                    } else if (value > 0) {
                        setStyle("-fx-text-fill: green;"); // 正值显示为绿色
                    } else {
                        setStyle(""); // 零值使用默认颜色
                    }
                }
            }
        });
        
        changeRateColumn.setCellFactory(column -> new TableCell<BuildingSettlementData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item.doubleValue()));
                }
            }
        });
        
        historicalCumulativeColumn.setCellFactory(column -> new TableCell<BuildingSettlementData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item.doubleValue()));
                }
            }
        });
    }
    
    /**
     * 初始化图表
     */
    private void initializeCharts() {
        // 初始化沉降变化量图表
        displacementXAxis = new CategoryAxis();
        displacementYAxis = new NumberAxis();
        displacementChart = new LineChart<>(displacementXAxis, displacementYAxis);
        
        displacementXAxis.setLabel("测点");
        displacementYAxis.setLabel("沉降变化量(mm)");
        displacementChart.setTitle("建筑物沉降变化量图");
        displacementChart.setAnimated(false);
        displacementChart.setCreateSymbols(true);
        displacementChart.setLegendVisible(true);
        
        // 初始化变化速率图表
        rateXAxis = new CategoryAxis();
        rateYAxis = new NumberAxis();
        rateChart = new LineChart<>(rateXAxis, rateYAxis);
        
        rateXAxis.setLabel("测点");
        rateYAxis.setLabel("变化速率(mm/d)");
        rateChart.setTitle("建筑物沉降变化速率图");
        rateChart.setAnimated(false);
        rateChart.setCreateSymbols(true);
        rateChart.setLegendVisible(true);
        
        // 默认显示沉降变化量图表
        showDisplacementChart();
    }
    
    /**
     * 显示沉降变化量图表
     */
    private void showDisplacementChart() {
        chartContainer.setCenter(displacementChart);
        updateChart();
    }
    
    /**
     * 显示变化速率图表
     */
    private void showRateChart() {
        chartContainer.setCenter(rateChart);
        updateChart();
    }
    
    /**
     * 更新图表数据
     */
    private void updateChart() {
        if (displacementChart == null || rateChart == null) {
            return;
        }
        
        // 清空现有数据
        displacementChart.getData().clear();
        rateChart.getData().clear();
        
        if (settlementDataList.isEmpty()) {
            return;
        }
        
        // 创建本期变化数据系列
        XYChart.Series<String, Number> currentChangeSeries = new XYChart.Series<>();
        currentChangeSeries.setName("本期变化量");
        
        // 创建累计变化数据系列
        XYChart.Series<String, Number> cumulativeChangeSeries = new XYChart.Series<>();
        cumulativeChangeSeries.setName("累计变化量");
        
        // 创建变化速率数据系列
        XYChart.Series<String, Number> changeRateSeries = new XYChart.Series<>();
        changeRateSeries.setName("变化速率");
        
        // 添加数据点
        for (BuildingSettlementData data : settlementDataList) {
            String pointId = data.getPointCode();
            
            // 添加沉降变化量数据点
            currentChangeSeries.getData().add(new XYChart.Data<>(pointId, data.getCurrentChange()));
            cumulativeChangeSeries.getData().add(new XYChart.Data<>(pointId, data.getCumulativeChange()));
            
            // 添加变化速率数据点
            changeRateSeries.getData().add(new XYChart.Data<>(pointId, data.getChangeRate()));
        }
        
        // 添加数据系列到图表
        displacementChart.getData().add(currentChangeSeries);
        displacementChart.getData().add(cumulativeChangeSeries);
        rateChart.getData().add(changeRateSeries);
    }
    
    /**
     * 加载测点数据
     */
    private void loadSettlementPoints() {
        // 这里可以从数据库或配置文件加载测点信息
        // 当前使用空列表，测点将通过设置对话框添加
        updatePointCount();
    }
    
    /**
     * 更新表格初始数据
     */
    private void updateTableWithInitialData() {
        // 清空表格
        settlementDataList.clear();
        
        // 如果有已配置的测点，可以显示初始状态
        if (!configuredPoints.isEmpty()) {
            for (BuildingSettlementPoint point : configuredPoints) {
                BuildingSettlementData data = new BuildingSettlementData();
                data.setPointCode(point.getPointId());
                data.setInitialElevation(point.getInitialElevation());
                data.setCurrentElevation(point.getInitialElevation());
                data.setPreviousElevation(point.getInitialElevation());
                data.setMileage(point.getMileage());
                data.setHistoricalCumulative(point.getHistoricalCumulative());
                data.setMeasurementDate(LocalDate.now());
                
                // 计算派生值
                data.calculateDerivedValues();
                
                settlementDataList.add(data);
            }
        }
    }
    
    /**
     * 更新测点数量显示
     */
    private void updatePointCount() {
        pointCountLabel.setText(String.valueOf(configuredPoints.size()));
    }
    
    /**
     * 处理Excel数据，转换为建筑物沉降数据
     */
    private List<BuildingSettlementData> processExcelPointData(Map<String, Double> pointElevationMap, LocalDate measureDate) {
        List<BuildingSettlementData> dataList = new ArrayList<>();
        
        for (Map.Entry<String, Double> entry : pointElevationMap.entrySet()) {
            String pointId = entry.getKey();
            Double currentElevation = entry.getValue();
            
            if (currentElevation == null) {
                continue;
            }
            
            // 查找对应的测点配置
            BuildingSettlementPoint pointConfig = getSettlementPointById(pointId);
            
            // 如果测点未配置，跳过
            if (pointConfig == null) {
                continue;
            }
            
            // 创建数据对象
            BuildingSettlementData data = new BuildingSettlementData();
            data.setPointCode(pointId);
            data.setInitialElevation(pointConfig.getInitialElevation());
            data.setCurrentElevation(currentElevation);
            data.setPreviousElevation(pointConfig.getInitialElevation()); // 默认使用初始高程作为前期高程
            data.setMileage(pointConfig.getMileage());
            data.setHistoricalCumulative(pointConfig.getHistoricalCumulative());
            data.setMeasurementDate(measureDate);
            
            // 计算派生值
            data.calculateDerivedValues();
            
            // 确保当前变化量正确计算（沉降为负值，抬升为正值）
            // 本期变化量（mm）：本期高程 - 前期高程，毫米为单位
            double currentChange = (data.getPreviousElevation() - data.getCurrentElevation()) * 1000.0;
            data.setCurrentChange(currentChange);
            
            // 累计变化量（mm）：初始高程 - 本期高程，毫米为单位
            double cumulativeChange = (data.getInitialElevation() - data.getCurrentElevation()) * 1000.0;
            data.setCumulativeChange(cumulativeChange);
            
            dataList.add(data);
        }
        
        return dataList;
    }
    
    /**
     * 从测量记录中加载数据
     */
    public void loadFromMeasurementRecords(List<MeasurementRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        
        // 清空现有数据
        dataBlocksMap.clear();
        dataBlockCheckBoxMap.clear();
        dataBlocksFlowPane.getChildren().clear();
        selectedDataBlocks.clear();
        
        // 按测量日期分组
        Map<LocalDate, List<MeasurementRecord>> recordsByDate = new HashMap<>();
        for (MeasurementRecord record : records) {
            if (record.getMeasureTime() != null) {
                LocalDate date = record.getMeasureTime().toLocalDate();
                recordsByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(record);
            }
        }
        
        // 处理每个日期的记录
        for (Map.Entry<LocalDate, List<MeasurementRecord>> entry : recordsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<MeasurementRecord> dateRecords = entry.getValue();
            
            // 为每个日期创建一个时间戳
            LocalDateTime timestamp = dateRecords.get(0).getMeasureTime();
            
            // 创建建筑物沉降数据列表
            List<BuildingSettlementData> dataList = new ArrayList<>();
            
            for (MeasurementRecord record : dateRecords) {
                // 从记录中解析数据
                String pointId = record.getId().split("_")[0]; // 假设ID格式为 "pointId_timestamp"
                
                // 查找对应的测点配置
                BuildingSettlementPoint pointConfig = getSettlementPointById(pointId);
                if (pointConfig == null) {
                    continue;
                }
                
                BuildingSettlementData data = new BuildingSettlementData();
                data.setPointCode(pointId);
                data.setCurrentElevation(record.getValue());
                data.setInitialElevation(pointConfig.getInitialElevation());
                data.setPreviousElevation(pointConfig.getInitialElevation()); // 默认使用初始高程作为前期高程
                data.setMileage(pointConfig.getMileage());
                data.setHistoricalCumulative(pointConfig.getHistoricalCumulative());
                data.setMeasurementDate(date);
                
                // 从注释中解析其他信息
                if (record.getComments() != null) {
                    String[] commentParts = record.getComments().split(";");
                    for (String part : commentParts) {
                        String[] keyValue = part.split("=");
                        if (keyValue.length == 2) {
                            String key = keyValue[0];
                            String value = keyValue[1];
                            try {
                                switch (key) {
                                    case "initialElevation":
                                        data.setInitialElevation(Double.parseDouble(value));
                                        break;
                                    case "previousElevation":
                                        data.setPreviousElevation(Double.parseDouble(value));
                                        break;
                                    case "mileage":
                                        data.setMileage(value);
                                        break;
                                    case "historicalCumulative":
                                        data.setHistoricalCumulative(Double.parseDouble(value));
                                        break;
                                }
                            } catch (NumberFormatException e) {
                                // 忽略解析错误
                            }
                        }
                    }
                }
                
                // 计算派生值
                data.calculateDerivedValues();
                
                dataList.add(data);
            }
            
            // 添加数据块
            if (!dataList.isEmpty()) {
                dataBlocksMap.put(timestamp, dataList);
                addDataBlock("导入数据", timestamp);
            }
        }
        
        // 更新测点数量显示
        updatePointCount();
    }
    
    /**
     * 从数据存储对象加载数据
     */
    public void loadFromBuildingSettlementDataStorage(BuildingSettlementDataStorage storage) {
        if (storage == null) {
            return;
        }
        
        // 清空现有数据
        configuredPoints.clear();
        dataBlocksMap.clear();
        dataBlockCheckBoxMap.clear();
        dataBlocksFlowPane.getChildren().clear();
        selectedDataBlocks.clear();
        
        // 加载测点配置
        configuredPoints.addAll(storage.getPoints());
        
        // 加载数据块
        for (LocalDateTime timestamp : storage.getDataBlockTimestamps()) {
            List<BuildingSettlementData> dataList = storage.getDataBlock(timestamp);
            if (dataList != null && !dataList.isEmpty()) {
                // 确保数据类型正确
                List<BuildingSettlementData> verifiedDataList = new ArrayList<>();
                for (BuildingSettlementData data : dataList) {
                    if (data != null) {
                        // 验证并重新计算派生值
                        BuildingSettlementData verifiedData = new BuildingSettlementData();
                        verifiedData.setPointCode(data.getPointCode());
                        verifiedData.setInitialElevation(data.getInitialElevation());
                        verifiedData.setCurrentElevation(data.getCurrentElevation());
                        verifiedData.setPreviousElevation(data.getPreviousElevation());
                        verifiedData.setMileage(data.getMileage());
                        verifiedData.setHistoricalCumulative(data.getHistoricalCumulative());
                        verifiedData.setMeasurementDate(data.getMeasurementDate());
                        
                        // 重新计算派生值
                        verifiedData.calculateDerivedValues();
                        
                        verifiedDataList.add(verifiedData);
                    }
                }
                
                dataBlocksMap.put(timestamp, verifiedDataList);
                addDataBlock(storage.getDataBlockDescription(timestamp), timestamp);
            }
        }
        
        // 加载选中的数据块
        for (LocalDateTime timestamp : storage.getSelectedDataBlocks()) {
            CheckBox checkBox = dataBlockCheckBoxMap.get(timestamp);
            if (checkBox != null) {
                checkBox.setSelected(true);
                selectedDataBlocks.add(timestamp);
            }
        }
        
        // 加载自定义天数
        customDaysForRateCalculation = storage.getCustomDaysForRateCalculation();
        
        // 更新UI
        updatePointCount();
        updateTableBasedOnSelection();
        
        // 更新上传日期显示
        if (!dataBlocksMap.isEmpty()) {
            LocalDateTime latestTime = dataBlocksMap.keySet().stream().max(LocalDateTime::compareTo).orElse(null);
            if (latestTime != null) {
                uploadDateLabel.setText(latestTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }
        }
    }

    /**
     * 显示设置自定义天数对话框
     */
    private void showSetCustomDaysDialog() {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(customDaysForRateCalculation));
        dialog.setTitle("设置自定义天数");
        dialog.setHeaderText("请输入用于计算变化速率的自定义天数");
        dialog.setContentText("天数(0表示使用实际日期间隔):");

        // 设置对话框的所有者
        if (stage != null) {
            dialog.initOwner(stage);
        }

        // 添加验证
        final Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        final TextField inputField = dialog.getEditor();
        
        inputField.textProperty().addListener((observable, oldValue, newValue) -> {
            boolean valid = false;
            try {
                if (!newValue.isEmpty()) {
                    int value = Integer.parseInt(newValue);
                    valid = value >= 0; // 确保非负值
                }
            } catch (NumberFormatException e) {
                valid = false;
            }
            okButton.setDisable(!valid);
        });

        // 显示对话框并处理结果
        dialog.showAndWait().ifPresent(result -> {
            try {
                int days = Integer.parseInt(result);
                if (days >= 0) {
                    customDaysForRateCalculation = days;
                    
                    // 更新表格数据
                    updateTableBasedOnSelection();
                    
                    AlertUtil.showInformation("设置成功", 
                        days == 0 ? "将使用实际测量日期间隔计算变化速率" : 
                        "自定义天数已设为 " + days + " 天");
                }
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("输入错误", "请输入有效的数字");
            }
        });
    }
} 