package com.monitor.controller;

import com.monitor.model.MeasurementRecord;
import com.monitor.model.SteelSupportAxialForceData;
import com.monitor.model.SteelSupportAxialForceDataStorage;
import com.monitor.model.SteelSupportAxialForcePoint;
import com.monitor.util.AlertUtil;
import com.monitor.util.ExcelUtil;
import com.monitor.view.SteelSupportAxialForcePointSettingsController;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 钢支撑轴力数据管理控制器
 */
public class SteelSupportAxialForceController {

    @FXML private Label titleLabel;
    @FXML private Button uploadDataButton;
    @FXML private DatePicker datePicker;
    @FXML private Button exportButton;
    @FXML private Button monitoringPointSettingsButton;

    @FXML private FlowPane dataBlocksFlowPane;

    @FXML private TabPane analysisTabPane;
    @FXML private Tab tableAnalysisTab;
    @FXML private Tab chartAnalysisTab;

    @FXML private TableView<SteelSupportAxialForceData> dataTableView;
    @FXML private TableColumn<SteelSupportAxialForceData, Number> serialNumberColumn;
    @FXML private TableColumn<SteelSupportAxialForceData, String> pointCodeColumn;
    @FXML private TableColumn<SteelSupportAxialForceData, Number> previousForceColumn;
    @FXML private TableColumn<SteelSupportAxialForceData, Number> currentForceColumn;
    @FXML private TableColumn<SteelSupportAxialForceData, Number> currentChangeColumn;
    @FXML private TableColumn<SteelSupportAxialForceData, String> mileageColumn;
    @FXML private TableColumn<SteelSupportAxialForceData, Number> historicalCumulativeColumn;

    @FXML private BorderPane chartContainer;

    private LineChart<String, Number> forceChart;
    private LineChart<String, Number> variationChart;
    private NumberAxis forceYAxis;
    private CategoryAxis forceXAxis;
    private NumberAxis variationYAxis;
    private CategoryAxis variationXAxis;

    @FXML private ToggleButton forceChartButton;
    @FXML private ToggleButton variationChartButton;
    private ToggleGroup chartToggleGroup;

    @FXML private Label pointCountLabel;
    @FXML private Label uploadDateLabel;

    private ObservableList<SteelSupportAxialForceData> axialForceDataList = FXCollections.observableArrayList();
    private Stage stage;

    // 测点数据映射 <测点编号, <日期, 数据>>
    private Map<String, Map<LocalDate, SteelSupportAxialForceData>> allPointDataMap = new HashMap<>();

    // 数据块映射 <时间戳, 数据列表>
    private Map<LocalDateTime, List<SteelSupportAxialForceData>> dataBlocksMap = new HashMap<>();

    // 数据块复选框映射 <时间戳, 复选框>
    private Map<LocalDateTime, CheckBox> dataBlockCheckBoxMap = new HashMap<>();

    // 选中的数据块列表
    private List<LocalDateTime> selectedDataBlocks = new ArrayList<>();

    // 已配置的测点列表
    private List<SteelSupportAxialForcePoint> configuredPoints = new ArrayList<>();

    // 自定义计算天数
    private int customDaysForRateCalculation = 0;

    /**
     * 设置主窗口
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
        // 初始化表格列
        setupTableColumns();
        
        // 初始化图表
        initializeCharts();
        
        // 加载测点配置
        loadSteelSupportPoints();
        
        // 设置图表类型切换
        chartToggleGroup = new ToggleGroup();
        forceChartButton.setToggleGroup(chartToggleGroup);
        variationChartButton.setToggleGroup(chartToggleGroup);
        
        // 修改图表按钮文本以匹配图片名称
        forceChartButton.setText("钢支撑轴力图");
        variationChartButton.setText("轴力变化量图");
        
        forceChartButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                showForceChart();
            }
        });
        
        variationChartButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                showVariationChart();
            }
        });
        
        // 设置表格右键菜单
        setupTableContextMenu();
        
        // 初始化数据
        updateTableWithInitialData();
        
        // 更新测点数量显示
        updatePointCount();
    }

    /**
     * 设置表格右键菜单
     */
    private void setupTableContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem customRateItem = new MenuItem("设置自定义比较天数");
        customRateItem.setOnAction(e -> showSetCustomDaysDialog());
        
        contextMenu.getItems().add(customRateItem);
        dataTableView.setContextMenu(contextMenu);
    }

    /**
     * 处理上传数据按钮事件
     */
    @FXML
    private void handleUploadButtonAction(ActionEvent event) {
        if (configuredPoints.isEmpty()) {
            AlertUtil.showWarning("无测点配置", "请先配置钢支撑轴力测点");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择Excel数据文件");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel文件", "*.xlsx", "*.xls"));
        
        File file = fileChooser.showOpenDialog(stage);
        
        if (file != null) {
            processSelectedExcelFile(file);
        }
    }

    /**
     * 处理导出按钮事件
     */
    @FXML
    private void handleExportButtonAction(ActionEvent event) {
        if (axialForceDataList.isEmpty()) {
            AlertUtil.showWarning("无数据", "没有数据可供导出");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出数据到Excel");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel文件", "*.xlsx"));
        
        LocalDate date = datePicker.getValue();
        String dateStr = date != null ? date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) : "data";
        fileChooser.setInitialFileName("钢支撑轴力数据_" + dateStr + ".xlsx");
        
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            try {
                List<String> headers = Arrays.asList("测点编号", "上次轴力(KN)", "本次轴力(KN)", 
                        "本次变化量(KN)", "里程", "历史累计值(mm)");
                
                List<List<String>> data = new ArrayList<>();
                for (SteelSupportAxialForceData rowData : axialForceDataList) {
                    List<String> row = new ArrayList<>();
                    row.add(rowData.getPointCode());
                    row.add(String.format("%.2f", rowData.getPreviousForce()));
                    row.add(String.format("%.2f", rowData.getCurrentForce()));
                    row.add(String.format("%.2f", rowData.getCurrentChange()));
                    row.add(rowData.getMileage());
                    row.add(String.format("%.2f", rowData.getHistoricalCumulative()));
                    data.add(row);
                }
                
                ExcelUtil.exportToExcel(file, "钢支撑轴力数据", headers, data);
                AlertUtil.showInformation("导出成功", "数据已成功导出到文件: " + file.getName());
            } catch (IOException e) {
                AlertUtil.showError("导出错误", "导出数据到Excel时发生错误: " + e.getMessage());
            }
        }
    }

    /**
     * 处理测点设置按钮事件
     */
    @FXML
    private void handleSettingsButtonAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/dialogs/SteelSupportAxialForcePointSettingsDialog.fxml"));
            BorderPane page = loader.load();
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("钢支撑轴力测点设置");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(stage);
            
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);
            
            SteelSupportAxialForcePointSettingsController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setInitialData(configuredPoints);
            
            dialogStage.showAndWait();
            
            // 获取更新后的测点列表
            List<SteelSupportAxialForcePoint> updatedPoints = controller.getPoints();
            if (updatedPoints != null) {
                configuredPoints.clear();
                configuredPoints.addAll(updatedPoints);
                updatePointCount();
            }
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("错误", "无法打开测点设置对话框: " + e.getMessage());
        }
    }

    /**
     * 处理数据块选择
     */
    private void handleDataBlockSelection(CheckBox checkBox) {
        // 获取关联的时间戳
        LocalDateTime timestamp = null;
        for (Map.Entry<LocalDateTime, CheckBox> entry : dataBlockCheckBoxMap.entrySet()) {
            if (entry.getValue() == checkBox) {
                timestamp = entry.getKey();
                break;
            }
        }
        
        if (timestamp == null) {
            return;
        }
        
        if (checkBox.isSelected()) {
            // 添加到选中列表
            if (!selectedDataBlocks.contains(timestamp)) {
                selectedDataBlocks.add(timestamp);
            }
            
            // 如果选中数量超过2个，取消最早选中的数据块
            if (selectedDataBlocks.size() > 2) {
                LocalDateTime firstTimestamp = selectedDataBlocks.get(0);
                selectedDataBlocks.remove(0);
                CheckBox firstCheckBox = dataBlockCheckBoxMap.get(firstTimestamp);
                if (firstCheckBox != null) {
                    firstCheckBox.setSelected(false);
                }
            }
        } else {
            // 从选中列表移除
            selectedDataBlocks.remove(timestamp);
        }
        
        // 更新表格数据
        updateTableBasedOnSelection();
        
        // 更新图表
        updateChart();
    }

    /**
     * 添加数据块UI元素
     */
    private void addDataBlock(String fileName, LocalDateTime dateTime) {
        VBox dataBlock = new VBox(5);
        dataBlock.getStyleClass().add("data-block");
        
        // 创建复选框，只显示日期时间
        String dateTimeStr = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        CheckBox checkBox = new CheckBox(dateTimeStr);
        checkBox.setSelected(false);
        
        // 添加事件处理
        checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            handleDataBlockSelection(checkBox);
        });
        
        // 添加到映射表
        dataBlockCheckBoxMap.put(dateTime, checkBox);
        
        // 添加到数据块
        dataBlock.getChildren().add(checkBox);
        
        // 添加右键菜单
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("删除");
        deleteItem.setOnAction(e -> removeDataBlock(dateTime));
        contextMenu.getItems().add(deleteItem);
        
        // 为数据块添加右键菜单
        dataBlock.setOnContextMenuRequested(e -> {
            contextMenu.show(dataBlock, e.getScreenX(), e.getScreenY());
        });
        
        // 添加到流布局面板
        dataBlocksFlowPane.getChildren().add(0, dataBlock);
    }
    
    /**
     * 删除数据块
     */
    private void removeDataBlock(LocalDateTime dateTime) {
        // 确认删除
        boolean confirm = AlertUtil.showConfirmationDialog(
                "确认删除", 
                "是否确定删除 " + dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " 的数据?");
        
        if (!confirm) {
            return;
        }
        
        // 从选中列表中移除
        selectedDataBlocks.remove(dateTime);
        
        // 从数据块映射中移除
        dataBlocksMap.remove(dateTime);
        
        // 从UI中移除
        CheckBox checkBox = dataBlockCheckBoxMap.get(dateTime);
        if (checkBox != null) {
            // 找到包含该复选框的VBox并从流布局面板中移除
            dataBlocksFlowPane.getChildren().removeIf(node -> {
                if (node instanceof VBox) {
                    return ((VBox) node).getChildren().contains(checkBox);
                }
                return false;
            });
        }
        
        // 从复选框映射中移除
        dataBlockCheckBoxMap.remove(dateTime);
        
        // 更新表格
        updateTableBasedOnSelection();
        
        // 更新图表
        updateChart();
    }

    /**
     * 获取测点配置
     */
    private SteelSupportAxialForcePoint getSteelSupportPointById(String pointId) {
        for (SteelSupportAxialForcePoint point : configuredPoints) {
            if (point.getPointId().equals(pointId)) {
                return point;
            }
        }
        return null;
    }

    /**
     * 处理Excel文件
     */
    private void processSelectedExcelFile(File file) {
        try {
            // 验证Excel文件是否包含"钢支撑轴力"表格
            boolean hasValidSheet = false;
            try (org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(file)) {
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    String sheetName = workbook.getSheetName(i);
                    if (sheetName.contains("钢支撑轴力")) {
                        hasValidSheet = true;
                        break;
                    }
                }
            }
            
            if (!hasValidSheet) {
                AlertUtil.showError("格式错误", "Excel文件中没有名为'钢支撑轴力'的工作表");
                return;
            }
            
            // 设置测量日期
            LocalDate measureDate = datePicker.getValue();
            if (measureDate == null) {
                measureDate = LocalDate.now();
                datePicker.setValue(measureDate);
            }
            
            // 导入数据
            Map<String, Double> pointForceMap = ExcelUtil.importFromExcel(file, "钢支撑轴力");
            
            if (pointForceMap.isEmpty()) {
                AlertUtil.showWarning("无数据", "在'钢支撑轴力'工作表中没有找到有效的数据");
                return;
            }
            
            // 处理数据
            List<SteelSupportAxialForceData> dataList = processExcelPointData(pointForceMap, measureDate);
            
            if (dataList.isEmpty()) {
                AlertUtil.showWarning("数据错误", "无法处理Excel文件中的数据");
                return;
            }
            
            // 添加到数据块
            LocalDateTime now = LocalDateTime.now();
            dataBlocksMap.put(now, dataList);
            
            // 添加数据块UI
            addDataBlock(file.getName(), now);
            
            // 选中新添加的数据块
            CheckBox checkBox = dataBlockCheckBoxMap.get(now);
            if (checkBox != null) {
                checkBox.setSelected(true);
            }
            
            // 更新上传日期
            uploadDateLabel.setText(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            AlertUtil.showInformation("上传成功", "成功上传 " + dataList.size() + " 条测点数据");
        } catch (IOException e) {
            AlertUtil.showError("文件错误", "读取Excel文件时出错: " + e.getMessage());
        } catch (Exception e) {
            AlertUtil.showError("处理错误", "处理数据时出错: " + e.getMessage());
        }
    }

    /**
     * 更新表格数据
     */
    private void updateTableBasedOnSelection() {
        axialForceDataList.clear();
        
        if (selectedDataBlocks.isEmpty()) {
            return;
        }
        
        // 如果只选中一个数据块
        if (selectedDataBlocks.size() == 1) {
            LocalDateTime timestamp = selectedDataBlocks.get(0);
            List<SteelSupportAxialForceData> dataList = dataBlocksMap.get(timestamp);
            
            if (dataList != null) {
                axialForceDataList.addAll(dataList);
            }
        } else {
            // 如果选中两个数据块，进行比较
            LocalDateTime timestamp1 = selectedDataBlocks.get(0);
            LocalDateTime timestamp2 = selectedDataBlocks.get(1);
            
            List<SteelSupportAxialForceData> dataList1 = dataBlocksMap.get(timestamp1);
            List<SteelSupportAxialForceData> dataList2 = dataBlocksMap.get(timestamp2);
            
            if (dataList1 == null || dataList2 == null) {
                return;
            }
            
            // 确定哪个是较早的数据块
            boolean isTimestamp1Earlier = timestamp1.isBefore(timestamp2);
            List<SteelSupportAxialForceData> previousDataList = isTimestamp1Earlier ? dataList1 : dataList2;
            List<SteelSupportAxialForceData> currentDataList = isTimestamp1Earlier ? dataList2 : dataList1;
            
            // 创建测点ID到数据的映射
            Map<String, SteelSupportAxialForceData> previousDataMap = new HashMap<>();
            for (SteelSupportAxialForceData data : previousDataList) {
                previousDataMap.put(data.getPointCode(), data);
            }
            
            // 处理当前数据
            for (SteelSupportAxialForceData currentData : currentDataList) {
                SteelSupportAxialForceData previousData = previousDataMap.get(currentData.getPointCode());
                
                if (previousData != null) {
                    // 设置上次轴力
                    currentData.setPreviousForce(previousData.getCurrentForce());
                    
                    // 计算变化量
                    currentData.calculateDerivedValues();
                }
                
                axialForceDataList.add(currentData);
            }
        }
        
        // 更新序号列
        for (int i = 0; i < axialForceDataList.size(); i++) {
            final int idx = i + 1;
            serialNumberColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(
                    axialForceDataList.indexOf(data.getValue()) + 1));
        }
    }

    /**
     * 处理Excel数据
     */
    private List<SteelSupportAxialForceData> processExcelPointData(Map<String, Double> pointForceMap, LocalDate measureDate) {
        List<SteelSupportAxialForceData> dataList = new ArrayList<>();
        
        // 处理每个测点
        for (Map.Entry<String, Double> entry : pointForceMap.entrySet()) {
            String pointId = entry.getKey();
            Double force = entry.getValue();
            
            // 查找测点配置
            SteelSupportAxialForcePoint point = getSteelSupportPointById(pointId);
            
            // 如果没有找到配置，使用默认配置
            if (point == null) {
                continue;
            }
            
            // 创建数据对象
            SteelSupportAxialForceData data = new SteelSupportAxialForceData();
            data.setPointCode(pointId);
            data.setCurrentForce(force);
            data.setPreviousForce(0); // 默认值，后续比较时会更新
            data.setMileage(point.getMileage());
            data.setHistoricalCumulative(point.getHistoricalCumulative());
            data.setMeasurementDate(measureDate);
            
            // 计算派生值
            data.calculateDerivedValues();
            
            dataList.add(data);
        }
        
        return dataList;
    }

    /**
     * 设置表格列
     */
    private void setupTableColumns() {
        // 设置列值工厂
        serialNumberColumn.setCellValueFactory(cellData -> 
                new SimpleObjectProperty<>(axialForceDataList.indexOf(cellData.getValue()) + 1));
        
        pointCodeColumn.setCellValueFactory(cellData -> 
                new SimpleObjectProperty<>(cellData.getValue().getPointCode()));
        
        previousForceColumn.setCellValueFactory(cellData -> 
                new SimpleObjectProperty<>(cellData.getValue().getPreviousForce()));
        
        currentForceColumn.setCellValueFactory(cellData -> 
                new SimpleObjectProperty<>(cellData.getValue().getCurrentForce()));
        
        currentChangeColumn.setCellValueFactory(cellData -> 
                new SimpleObjectProperty<>(cellData.getValue().getCurrentChange()));
        
        mileageColumn.setCellValueFactory(cellData -> 
                new SimpleObjectProperty<>(cellData.getValue().getMileage()));
        
        historicalCumulativeColumn.setCellValueFactory(cellData -> 
                new SimpleObjectProperty<>(cellData.getValue().getHistoricalCumulative()));
        
        // 设置表格数据
        dataTableView.setItems(axialForceDataList);
        
        // 配置数字列格式化
        configureNumberColumns();
    }

    /**
     * 配置数字列格式化
     */
    private void configureNumberColumns() {
        previousForceColumn.setCellFactory(column -> new TableCell<SteelSupportAxialForceData, Number>() {
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
        
        currentForceColumn.setCellFactory(column -> new TableCell<SteelSupportAxialForceData, Number>() {
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
        
        currentChangeColumn.setCellFactory(column -> new TableCell<SteelSupportAxialForceData, Number>() {
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
        
        historicalCumulativeColumn.setCellFactory(column -> new TableCell<SteelSupportAxialForceData, Number>() {
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
        // 创建轴力图表
        forceXAxis = new CategoryAxis();
        forceYAxis = new NumberAxis();
        forceXAxis.setLabel("测点");
        forceYAxis.setLabel("轴力(KN)");
        forceChart = new LineChart<>(forceXAxis, forceYAxis);
        forceChart.setTitle("钢支撑轴力图");
        forceChart.setCreateSymbols(true);
        forceChart.setLegendVisible(true);
        forceChart.setAnimated(false);  // 禁用动画提高性能
        
        // 样式优化
        forceChart.getStyleClass().add("chart");
        forceXAxis.setTickLabelRotation(45);  // 旋转X轴标签，防止重叠
        forceXAxis.getStyleClass().add("axis-label");
        forceYAxis.getStyleClass().add("axis-label");
        
        // 设置Y轴格式
        forceYAxis.setAutoRanging(true);
        forceYAxis.setForceZeroInRange(false);  // 根据数据范围自动缩放，不强制包含零点
        forceYAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                // 显示轴力值，保留2位小数
                return String.format("%.2f", object.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                try {
                    return Double.parseDouble(string);
                } catch (Exception e) {
                    return 0;
                }
            }
        });
        
        // 创建变化趋势图表
        variationXAxis = new CategoryAxis();
        variationYAxis = new NumberAxis();
        variationXAxis.setLabel("测点");
        variationYAxis.setLabel("变化量(KN)");
        variationChart = new LineChart<>(variationXAxis, variationYAxis);
        variationChart.setTitle("轴力变化量图");
        variationChart.setCreateSymbols(true);
        variationChart.setLegendVisible(true);
        variationChart.setAnimated(false);
        
        // 样式优化
        variationChart.getStyleClass().add("chart");
        variationXAxis.setTickLabelRotation(45);  // 旋转X轴标签，防止重叠
        variationXAxis.getStyleClass().add("axis-label");
        variationYAxis.getStyleClass().add("axis-label");
        
        // 设置Y轴格式
        variationYAxis.setAutoRanging(true);
        variationYAxis.setForceZeroInRange(true);  // 强制包含零点
        variationYAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                // 显示变化量，保留2位小数
                return String.format("%.2f", object.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                try {
                    return Double.parseDouble(string);
                } catch (Exception e) {
                    return 0;
                }
            }
        });
        
        // 默认显示轴力图表
        showForceChart();
    }

    /**
     * 显示轴力图表
     */
    private void showForceChart() {
        // 设置过渡动画
        if (chartContainer.getCenter() != forceChart) {
            // 添加淡入效果
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), forceChart);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            
            chartContainer.setCenter(forceChart);
            fadeIn.play();
        }
        
        // 更新UI状态
        forceChartButton.setSelected(true);
        variationChartButton.setSelected(false);
        
        // 更新图表数据
        updateChart();
    }

    /**
     * 显示变化趋势图表
     */
    private void showVariationChart() {
        // 设置过渡动画
        if (chartContainer.getCenter() != variationChart) {
            // 添加淡入效果
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), variationChart);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            
            chartContainer.setCenter(variationChart);
            fadeIn.play();
        }
        
        // 更新UI状态
        forceChartButton.setSelected(false);
        variationChartButton.setSelected(true);
        
        // 更新图表数据
        updateChart();
    }

    /**
     * 更新图表数据
     */
    private void updateChart() {
        // 清除现有数据
        forceChart.getData().clear();
        variationChart.getData().clear();
        
        if (axialForceDataList.isEmpty()) {
            return;
        }
        
        // 创建数据系列
        XYChart.Series<String, Number> currentForceSeries = new XYChart.Series<>();
        currentForceSeries.setName("本次轴力");
        
        XYChart.Series<String, Number> previousForceSeries = new XYChart.Series<>();
        previousForceSeries.setName("上次轴力");
        
        XYChart.Series<String, Number> changeSeries = new XYChart.Series<>();
        changeSeries.setName("变化量");
        
        // 获取所有测点编号并排序，确保图表中点的顺序一致
        List<String> sortedPointCodes = axialForceDataList.stream()
                .map(SteelSupportAxialForceData::getPointCode)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        
        // 添加数据点 - 按排序后的测点添加
        for (String pointCode : sortedPointCodes) {
            // 查找该测点的数据
            SteelSupportAxialForceData data = axialForceDataList.stream()
                    .filter(d -> d.getPointCode().equals(pointCode))
                    .findFirst()
                    .orElse(null);
            
            if (data != null) {
                // 添加轴力数据
                XYChart.Data<String, Number> currentData = new XYChart.Data<>(data.getPointCode(), data.getCurrentForce());
                currentForceSeries.getData().add(currentData);
                
                XYChart.Data<String, Number> previousData = new XYChart.Data<>(data.getPointCode(), data.getPreviousForce());
                previousForceSeries.getData().add(previousData);
                
                // 添加变化量数据
                XYChart.Data<String, Number> changeData = new XYChart.Data<>(data.getPointCode(), data.getCurrentChange());
                changeSeries.getData().add(changeData);
            }
        }
        
        // 添加到图表
        forceChart.getData().addAll(currentForceSeries, previousForceSeries);
        variationChart.getData().add(changeSeries);
        
        // 应用样式以增强可读性
        applyChartStyling();
    }
    
    /**
     * 应用图表样式
     */
    private void applyChartStyling() {
        // 轴力图样式
        for (XYChart.Series<String, Number> series : forceChart.getData()) {
            if (series.getName().equals("本次轴力")) {
                // 设置本次轴力线颜色和样式
                String color = "#3498db"; // 蓝色
                series.getNode().lookup(".chart-series-line").setStyle(
                        "-fx-stroke: " + color + "; -fx-stroke-width: 2px;");
                
                // 为数据点添加数值标签和修改样式为实心圆点
                for (XYChart.Data<String, Number> data : series.getData()) {
                    Node node = data.getNode();
                    if (node != null) {
                        // 设置数据点为实心圆点，颜色与线条一致，更小的点
                        node.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 3px; -fx-padding: 3px;");
                        
                        // 添加工具提示
                        Tooltip tip = new Tooltip(String.format("测点: %s\n本次轴力: %.2f KN", 
                                data.getXValue(), data.getYValue().doubleValue()));
                        Tooltip.install(node, tip);
                        
                        // 鼠标悬浮效果
                        node.setOnMouseEntered(event -> 
                            node.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 5px; -fx-padding: 5px;"));
                        node.setOnMouseExited(event -> 
                            node.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 3px; -fx-padding: 3px;"));
                    }
                }
            } else if (series.getName().equals("上次轴力")) {
                // 设置上次轴力线颜色和样式 - 改为红色
                String color = "#e74c3c"; // 红色
                series.getNode().lookup(".chart-series-line").setStyle(
                        "-fx-stroke: " + color + "; -fx-stroke-width: 1.5px;");
                
                // 为数据点添加数值标签和修改样式为实心圆点
                for (XYChart.Data<String, Number> data : series.getData()) {
                    Node node = data.getNode();
                    if (node != null) {
                        // 设置数据点为实心圆点，颜色与线条一致，更小的点
                        node.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 3px; -fx-padding: 3px;");
                        
                        // 添加工具提示
                        Tooltip tip = new Tooltip(String.format("测点: %s\n上次轴力: %.2f KN", 
                                data.getXValue(), data.getYValue().doubleValue()));
                        Tooltip.install(node, tip);
                        
                        // 鼠标悬浮效果
                        node.setOnMouseEntered(event -> 
                            node.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 5px; -fx-padding: 5px;"));
                        node.setOnMouseExited(event -> 
                            node.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 3px; -fx-padding: 3px;"));
                    }
                }
            }
        }
        
        // 轴力变化图样式 - 全部使用蓝色
        String blueColor = "#3498db";
        
        for (XYChart.Series<String, Number> series : variationChart.getData()) {
            // 更新整体线条样式为蓝色
            series.getNode().lookup(".chart-series-line").setStyle(
                    "-fx-stroke: " + blueColor + "; -fx-stroke-width: 2px;");
            
            // 设置所有数据点为蓝色
            for (XYChart.Data<String, Number> data : series.getData()) {
                Node node = data.getNode();
                if (node != null) {
                    // 设置数据点为实心圆点，统一使用蓝色，更小的点
                    node.setStyle("-fx-background-color: " + blueColor + "; -fx-background-radius: 3px; -fx-padding: 3px;");
                    
                    // 添加工具提示
                    Tooltip tip = new Tooltip(String.format("测点: %s\n变化量: %.2f KN", 
                            data.getXValue(), data.getYValue().doubleValue()));
                    Tooltip.install(node, tip);
                    
                    // 鼠标悬浮效果
                    node.setOnMouseEntered(event -> 
                        node.setStyle("-fx-background-color: " + blueColor + "; -fx-background-radius: 5px; -fx-padding: 5px;"));
                    node.setOnMouseExited(event -> 
                        node.setStyle("-fx-background-color: " + blueColor + "; -fx-background-radius: 3px; -fx-padding: 3px;"));
                }
            }
        }
    }

    /**
     * 加载测点配置
     */
    private void loadSteelSupportPoints() {
        // 从项目文件或数据库加载测点配置
    }

    /**
     * 更新表格初始数据
     */
    private void updateTableWithInitialData() {
        // 创建示例数据
        axialForceDataList.clear();
        
        // 将表格数据添加到数据映射中
        for (SteelSupportAxialForceData data : axialForceDataList) {
            Map<LocalDate, SteelSupportAxialForceData> pointMap = allPointDataMap.computeIfAbsent(
                    data.getPointCode(), k -> new HashMap<>());
            pointMap.put(data.getMeasurementDate(), data);
        }
        
        // 刷新表格
        dataTableView.refresh();
    }

    /**
     * 更新测点数量显示
     */
    private void updatePointCount() {
        pointCountLabel.setText(String.valueOf(configuredPoints.size()));
    }

    /**
     * 显示自定义天数设置对话框
     */
    private void showSetCustomDaysDialog() {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(customDaysForRateCalculation));
        dialog.setTitle("自定义计算天数");
        dialog.setHeaderText("设置变化速率计算的天数");
        dialog.setContentText("天数:");
        
        dialog.showAndWait().ifPresent(value -> {
            try {
                int days = Integer.parseInt(value);
                if (days > 0) {
                    customDaysForRateCalculation = days;
                    updateTableBasedOnSelection();
                }
            } catch (NumberFormatException e) {
                AlertUtil.showError("输入错误", "请输入有效的天数");
            }
        });
    }

    /**
     * 获取自定义计算天数
     */
    public int getCustomDaysForRateCalculation() {
        return customDaysForRateCalculation;
    }

    /**
     * 设置自定义计算天数
     */
    public void setCustomDaysForRateCalculation(int days) {
        this.customDaysForRateCalculation = days;
        updateTableBasedOnSelection();
    }

    /**
     * 获取数据存储对象
     */
    public SteelSupportAxialForceDataStorage getSteelSupportAxialForceDataStorage() {
        SteelSupportAxialForceDataStorage storage = new SteelSupportAxialForceDataStorage();
        
        // 设置测点配置
        storage.setPoints(new ArrayList<>(configuredPoints));
        
        // 添加数据块
        for (Map.Entry<LocalDateTime, List<SteelSupportAxialForceData>> entry : dataBlocksMap.entrySet()) {
            String description = entry.getKey().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            storage.addDataBlock(entry.getKey(), entry.getValue(), description);
        }
        
        // 设置选中的数据块
        storage.setSelectedDataBlocks(new ArrayList<>(selectedDataBlocks));
        
        // 设置自定义计算天数
        storage.setCustomDaysForCalculation(customDaysForRateCalculation);
        
        return storage;
    }

    /**
     * 将钢支撑轴力数据转换为通用测量记录用于保存
     * @return 测量记录列表
     */
    public List<MeasurementRecord> getMeasurementRecordsForSaving() {
        List<MeasurementRecord> records = new ArrayList<>();
        
        // 遍历所有数据块
        for (Map.Entry<LocalDateTime, List<SteelSupportAxialForceData>> entry : dataBlocksMap.entrySet()) {
            LocalDateTime timestamp = entry.getKey();
            List<SteelSupportAxialForceData> dataList = entry.getValue();
            
            // 将每个数据点转换为测量记录
            for (SteelSupportAxialForceData data : dataList) {
                MeasurementRecord record = new MeasurementRecord();
                record.setId(data.getPointCode()); // 使用测点编号作为记录ID
                record.setValue(data.getCurrentForce()); // 使用当前轴力作为记录值
                record.setMeasureTime(timestamp); // 使用数据块的时间戳
                record.setUnit("KN"); // 轴力单位为千牛
                
                // 存储附加信息到comments字段，便于后续还原
                StringBuilder comments = new StringBuilder();
                comments.append("type:steel_support_axial_force");
                if (data.getMileage() != null && !data.getMileage().isEmpty()) {
                    comments.append(";mileage:").append(data.getMileage());
                }
                if (data.getPreviousForce() != 0) {
                    comments.append(";prev:").append(data.getPreviousForce());
                }
                if (data.getCurrentChange() != 0) {
                    comments.append(";change:").append(data.getCurrentChange());
                }
                if (data.getHistoricalCumulative() != 0) {
                    comments.append(";hist_cumul:").append(data.getHistoricalCumulative());
                }
                
                record.setComments(comments.toString());
                records.add(record);
            }
        }
        
        return records;
    }

    /**
     * 判断数据是否变化，需要保存
     * @return 如果有数据块，表示数据已变化
     */
    public boolean hasDataChanged() {
        return !dataBlocksMap.isEmpty();
    }

    /**
     * 从测量记录加载数据
     */
    public void loadFromMeasurementRecords(List<MeasurementRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        
        // 清除现有数据
        dataBlocksMap.clear();
        dataBlockCheckBoxMap.clear();
        selectedDataBlocks.clear();
        allPointDataMap.clear();
        axialForceDataList.clear();
        dataBlocksFlowPane.getChildren().clear();
        
        // 按时间分组记录
        Map<LocalDateTime, List<MeasurementRecord>> recordsByTime = new HashMap<>();
        
        for (MeasurementRecord record : records) {
            LocalDateTime time = record.getMeasureTime();
            if (time == null) {
                continue;
            }
            
            List<MeasurementRecord> timeRecords = recordsByTime.computeIfAbsent(
                    time, k -> new ArrayList<>());
            timeRecords.add(record);
        }
        
        // 处理每个时间点的记录
        for (Map.Entry<LocalDateTime, List<MeasurementRecord>> entry : recordsByTime.entrySet()) {
            LocalDateTime time = entry.getKey();
            List<MeasurementRecord> timeRecords = entry.getValue();
            
            // 创建数据列表
            List<SteelSupportAxialForceData> dataList = new ArrayList<>();
            
            for (MeasurementRecord record : timeRecords) {
                SteelSupportAxialForceData data = new SteelSupportAxialForceData();
                data.setPointCode(record.getId());
                data.setCurrentForce(record.getValue());
                data.setMeasurementDate(time.toLocalDate());
                
                // 设置其他属性（如果记录中包含）
                if (record.getComments() != null && record.getComments().contains("mileage:")) {
                    String mileage = record.getComments().split("mileage:")[1].trim();
                    data.setMileage(mileage);
                }
                
                dataList.add(data);
            }
            
            // 添加到数据块
            dataBlocksMap.put(time, dataList);
            
            // 添加数据块UI - 只使用日期时间作为标识
            addDataBlock("", time);
        }
        
        // 选中最新的数据块
        if (!dataBlocksMap.isEmpty()) {
            LocalDateTime latestTime = Collections.max(dataBlocksMap.keySet());
            CheckBox checkBox = dataBlockCheckBoxMap.get(latestTime);
            if (checkBox != null) {
                checkBox.setSelected(true);
            }
        }
    }

    /**
     * 从数据存储加载
     */
    public void loadFromSteelSupportAxialForceDataStorage(SteelSupportAxialForceDataStorage storage) {
        if (storage == null) {
            return;
        }
        
        // 加载测点配置
        configuredPoints.clear();
        configuredPoints.addAll(storage.getPoints());
        
        // 清除现有数据
        dataBlocksMap.clear();
        dataBlockCheckBoxMap.clear();
        selectedDataBlocks.clear();
        allPointDataMap.clear();
        axialForceDataList.clear();
        dataBlocksFlowPane.getChildren().clear();
        
        // 加载数据块
        for (LocalDateTime timestamp : storage.getDataBlockTimestamps()) {
            List<SteelSupportAxialForceData> dataList = storage.getDataBlock(timestamp);
            
            // 添加到数据块映射
            dataBlocksMap.put(timestamp, dataList);
            
            // 添加数据块UI - 只使用日期时间作为标识
            addDataBlock("", timestamp);
        }
        
        // 恢复选中的数据块
        for (LocalDateTime timestamp : storage.getSelectedDataBlocks()) {
            CheckBox checkBox = dataBlockCheckBoxMap.get(timestamp);
            if (checkBox != null) {
                checkBox.setSelected(true);
            }
        }
        
        // 设置自定义计算天数
        customDaysForRateCalculation = storage.getCustomDaysForCalculation();
        
        // 更新测点数量
        updatePointCount();
    }
} 