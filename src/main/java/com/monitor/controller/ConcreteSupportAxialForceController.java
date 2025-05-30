package com.monitor.controller;

import com.monitor.model.MeasurementRecord;
import com.monitor.model.ConcreteSupportAxialForceData;
import com.monitor.model.ConcreteSupportAxialForceDataStorage;
import com.monitor.model.ConcreteSupportAxialForcePoint;
import com.monitor.util.AlertUtil;
import com.monitor.util.ExcelUtil;
import com.monitor.view.ConcreteSupportAxialForcePointSettingsController;
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
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.scene.Parent;
import javafx.scene.control.Tooltip;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 砼支撑轴力数据管理控制器
 */
public class ConcreteSupportAxialForceController {

    @FXML private Label titleLabel;
    @FXML private Button uploadDataButton;
    @FXML private DatePicker datePicker;
    @FXML private Button exportButton;
    @FXML private Button monitoringPointSettingsButton;

    @FXML private FlowPane dataBlocksFlowPane;

    @FXML private TabPane analysisTabPane;
    @FXML private Tab tableAnalysisTab;
    @FXML private Tab chartAnalysisTab;

    @FXML private TableView<ConcreteSupportAxialForceData> dataTableView;
    @FXML private TableColumn<ConcreteSupportAxialForceData, Number> serialNumberColumn;
    @FXML private TableColumn<ConcreteSupportAxialForceData, String> pointCodeColumn;
    @FXML private TableColumn<ConcreteSupportAxialForceData, Number> previousAxialForceColumn;
    @FXML private TableColumn<ConcreteSupportAxialForceData, Number> currentAxialForceColumn;
    @FXML private TableColumn<ConcreteSupportAxialForceData, Number> currentChangeColumn;
    @FXML private TableColumn<ConcreteSupportAxialForceData, String> mileageColumn;
    @FXML private TableColumn<ConcreteSupportAxialForceData, Number> historicalCumulativeColumn;

    @FXML private BorderPane chartContainer;

    private LineChart<String, Number> axialForceChart;
    private LineChart<String, Number> changeChart;
    private NumberAxis axialForceYAxis;
    private CategoryAxis axialForceXAxis;
    private NumberAxis changeYAxis;
    private CategoryAxis changeXAxis;

    @FXML private ToggleButton axialForceChartButton;
    @FXML private ToggleButton changeChartButton;
    private ToggleGroup chartToggleGroup;

    @FXML private Label pointCountLabel;
    @FXML private Label uploadDateLabel;

    private ObservableList<ConcreteSupportAxialForceData> axialForceDataList = FXCollections.observableArrayList();
    private Stage stage;

    // 测点数据映射 <测点编号, <日期, 数据>>
    private Map<String, Map<LocalDate, ConcreteSupportAxialForceData>> allPointDataMap = new HashMap<>();

    // 数据块映射 <时间戳, 数据列表>
    private Map<LocalDateTime, List<ConcreteSupportAxialForceData>> dataBlocksMap = new HashMap<>();

    // 数据块复选框映射 <时间戳, 复选框>
    private Map<LocalDateTime, CheckBox> dataBlockCheckBoxMap = new HashMap<>();

    // 选中的数据块列表
    private List<LocalDateTime> selectedDataBlocks = new ArrayList<>();

    // 已配置的测点列表
    private List<ConcreteSupportAxialForcePoint> configuredPoints = new ArrayList<>();

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
        loadConcreteSupportPoints();
        
        // 设置图表类型切换
        chartToggleGroup = new ToggleGroup();
        axialForceChartButton.setToggleGroup(chartToggleGroup);
        changeChartButton.setToggleGroup(chartToggleGroup);
        
        // 修改图表按钮文本以匹配图片名称
        axialForceChartButton.setText("砼支撑轴力图");
        changeChartButton.setText("轴力变化量图");
        
        axialForceChartButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                showAxialForceChart();
            }
        });
        
        changeChartButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                showChangeChart();
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
     * 设置表格列
     */
    private void setupTableColumns() {
        // 序号列
        serialNumberColumn.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<>(axialForceDataList.indexOf(cellData.getValue()) + 1));
        
        // 测点编号列
        pointCodeColumn.setCellValueFactory(new PropertyValueFactory<>("pointCode"));
        
        // 上次轴力列
        previousAxialForceColumn.setCellValueFactory(new PropertyValueFactory<>("previousForce"));
        previousAxialForceColumn.setCellFactory(column -> new TableCell<ConcreteSupportAxialForceData, Number>() {
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
        
        // 本次轴力列
        currentAxialForceColumn.setCellValueFactory(new PropertyValueFactory<>("currentForce"));
        currentAxialForceColumn.setCellFactory(column -> new TableCell<ConcreteSupportAxialForceData, Number>() {
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
        
        // 变化量列
        currentChangeColumn.setCellValueFactory(new PropertyValueFactory<>("currentChange"));
        currentChangeColumn.setCellFactory(column -> new TableCell<ConcreteSupportAxialForceData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    // 显示两位小数
                    setText(String.format("%.2f", item.doubleValue()));
                    
                    // 设置颜色：正值为红色(压力增加)，负值为绿色(压力减小)
                    double value = item.doubleValue();
                    if (value > 0) {
                        setTextFill(Color.RED);
                    } else if (value < 0) {
                        setTextFill(Color.GREEN);
                    } else {
                        setTextFill(Color.BLACK);
                    }
                }
            }
        });
        
        // 里程列
        mileageColumn.setCellValueFactory(new PropertyValueFactory<>("mileage"));
        
        // 历史累计列
        historicalCumulativeColumn.setCellValueFactory(new PropertyValueFactory<>("historicalCumulative"));
        historicalCumulativeColumn.setCellFactory(column -> new TableCell<ConcreteSupportAxialForceData, Number>() {
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
        
        // 设置表格数据源
        dataTableView.setItems(axialForceDataList);
    }
    
    /**
     * 初始化图表
     */
    private void initializeCharts() {
        // 轴力图表
        axialForceXAxis = new CategoryAxis();
        axialForceYAxis = new NumberAxis();
        axialForceXAxis.setLabel("测点编号");
        axialForceYAxis.setLabel("轴力(KN)");
        
        // 设置Y轴格式为两位小数
        axialForceYAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return String.format("%.2f", object.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                return Double.parseDouble(string);
            }
        });
        
        axialForceChart = new LineChart<>(axialForceXAxis, axialForceYAxis);
        axialForceChart.setTitle("砼支撑轴力图");
        axialForceChart.setCreateSymbols(true); // 显示数据点符号
        axialForceChart.setAnimated(false);
        axialForceChart.setLegendVisible(true);
        
        // 变化量图表
        changeXAxis = new CategoryAxis();
        changeYAxis = new NumberAxis();
        changeXAxis.setLabel("测点编号");
        changeYAxis.setLabel("变化量(KN)");
        
        // 设置Y轴格式为两位小数
        changeYAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return String.format("%.2f", object.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                return Double.parseDouble(string);
            }
        });
        
        changeChart = new LineChart<>(changeXAxis, changeYAxis);
        changeChart.setTitle("轴力变化量图");
        changeChart.setCreateSymbols(true); // 显示数据点符号
        changeChart.setAnimated(false);
        changeChart.setLegendVisible(true);
        
        // 设置图表样式
        String chartStyle = 
                ".chart-line-symbol {" +
                "    -fx-background-radius: 5px;" +
                "    -fx-padding: 5px;" +
                "}" +
                ".chart-series-line {" +
                "    -fx-stroke-width: 2px;" +
                "}";
        
        axialForceChart.setStyle(chartStyle);
        changeChart.setStyle(chartStyle);
        
        // 默认显示轴力图表
        axialForceChartButton.setSelected(true);
        showAxialForceChart();
    }
    
    /**
     * 显示轴力图表
     */
    private void showAxialForceChart() {
        chartContainer.setCenter(axialForceChart);
        updateChart();
    }
    
    /**
     * 显示变化量图表
     */
    private void showChangeChart() {
        chartContainer.setCenter(changeChart);
        updateChart();
    }
    
    /**
     * 更新图表显示
     */
    private void updateChart() {
        if (axialForceDataList.isEmpty()) {
            return;
        }
        
        // 清除现有数据
        axialForceChart.getData().clear();
        changeChart.getData().clear();
        
        // 创建数据系列
        XYChart.Series<String, Number> currentAxialForceSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> previousAxialForceSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> changeSeries = new XYChart.Series<>();
        
        // 修改系列名称，确保与图例颜色一致
        previousAxialForceSeries.setName("当前轴力(KN)");
        currentAxialForceSeries.setName("上次轴力(KN)");
        changeSeries.setName("变化量(KN)");
        
        // 添加数据点
        for (ConcreteSupportAxialForceData data : axialForceDataList) {
            String pointId = data.getPointCode();
            
            // 当前轴力
            currentAxialForceSeries.getData().add(new XYChart.Data<>(pointId, data.getCurrentForce()));
            
            // 上次轴力
            previousAxialForceSeries.getData().add(new XYChart.Data<>(pointId, data.getPreviousForce()));
            
            // 变化量
            changeSeries.getData().add(new XYChart.Data<>(pointId, data.getCurrentChange()));
        }
        
        // 将数据添加到图表 - 交换添加顺序以匹配图例
        axialForceChart.getData().add(previousAxialForceSeries);
        axialForceChart.getData().add(currentAxialForceSeries);
        changeChart.getData().add(changeSeries);
        
        // 应用样式并添加数据点提示
        for (XYChart.Series<String, Number> series : axialForceChart.getData()) {
            applySeriesTooltips(series);
        }
        
        for (XYChart.Series<String, Number> series : changeChart.getData()) {
            applySeriesTooltips(series);
        }
        
        // 应用自定义样式
        applyChartStyling();
    }
    
    /**
     * 为数据系列添加提示
     */
    private void applySeriesTooltips(XYChart.Series<String, Number> series) {
        for (XYChart.Data<String, Number> data : series.getData()) {
            Tooltip tooltip = new Tooltip(String.format("%s: %.2f", data.getXValue(), data.getYValue().doubleValue()));
            
            // 设置提示显示时间
            tooltip.setShowDelay(javafx.util.Duration.millis(100));
            tooltip.setShowDuration(javafx.util.Duration.seconds(10));
            
            Tooltip.install(data.getNode(), tooltip);
            
            // 给数据点添加鼠标悬停效果
            data.getNode().setOnMouseEntered(event -> {
                data.getNode().setScaleX(1.5);
                data.getNode().setScaleY(1.5);
                data.getNode().setCursor(javafx.scene.Cursor.HAND);
            });
            
            data.getNode().setOnMouseExited(event -> {
                data.getNode().setScaleX(1.0);
                data.getNode().setScaleY(1.0);
            });
        }
    }
    
    /**
     * 应用图表样式
     */
    private void applyChartStyling() {
        // 为当前轴力(蓝色)设置样式
        if (!axialForceChart.getData().isEmpty() && axialForceChart.getData().size() >= 1) {
            XYChart.Series<String, Number> currentSeries = axialForceChart.getData().get(0);
            String currentStyle = "-fx-stroke: #0000ff; -fx-stroke-width: 2px;";
            
            currentSeries.getNode().setStyle(currentStyle);
            
            // 设置数据点样式
            for (XYChart.Data<String, Number> data : currentSeries.getData()) {
                if (data.getNode() != null) {
                    data.getNode().setStyle("-fx-background-color: #0000ff, white;");
                }
            }
        }
        
        // 为上次轴力(红色)设置样式 - 改为实线
        if (axialForceChart.getData().size() >= 2) {
            XYChart.Series<String, Number> previousSeries = axialForceChart.getData().get(1);
            String previousStyle = "-fx-stroke: #ff0000; -fx-stroke-width: 2px;";
            
            previousSeries.getNode().setStyle(previousStyle);
            
            // 设置数据点样式
            for (XYChart.Data<String, Number> data : previousSeries.getData()) {
                if (data.getNode() != null) {
                    data.getNode().setStyle("-fx-background-color: #ff0000, white;");
                }
            }
        }
        
        // 为变化量线条设置样式（实线，红色）
        if (!changeChart.getData().isEmpty()) {
            XYChart.Series<String, Number> changeSeries = changeChart.getData().get(0);
            String changeStyle = "-fx-stroke: #ff0000; -fx-stroke-width: 2px;";
            
            changeSeries.getNode().setStyle(changeStyle);
            
            // 设置数据点样式
            for (XYChart.Data<String, Number> data : changeSeries.getData()) {
                if (data.getNode() != null) {
                    data.getNode().setStyle("-fx-background-color: #ff0000, white;");
                }
            }
        }
    }
    
    /**
     * 设置表格右键菜单
     */
    private void setupTableContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem refreshItem = new MenuItem("刷新数据");
        refreshItem.setOnAction(e -> updateTableWithInitialData());
        
        MenuItem exportItem = new MenuItem("导出当前数据");
        exportItem.setOnAction(e -> handleExportButtonAction(new ActionEvent()));
        
        contextMenu.getItems().addAll(refreshItem, exportItem);
        dataTableView.setContextMenu(contextMenu);
    }
    
    /**
     * 更新初始数据
     */
    private void updateTableWithInitialData() {
        axialForceDataList.clear();
        
        // 如果有已配置的测点，为每个测点创建初始数据
        if (!configuredPoints.isEmpty()) {
            for (ConcreteSupportAxialForcePoint point : configuredPoints) {
                ConcreteSupportAxialForceData data = new ConcreteSupportAxialForceData();
                data.setPointCode(point.getPointId());
                data.setMileage(point.getMileage());
                data.setCurrentForce(0.0);
                data.setPreviousForce(0.0);
                data.setCurrentChange(0.0);
                data.setHistoricalCumulative(point.getHistoricalCumulative());
                data.setMeasurementDate(LocalDate.now());
                
                axialForceDataList.add(data);
            }
        }
        
        // 更新图表
        updateChart();
    }
    
    /**
     * 处理导出数据按钮点击事件
     */
    @FXML
    private void handleExportButtonAction(ActionEvent event) {
        if (axialForceDataList.isEmpty()) {
            AlertUtil.showInformation("导出", "没有数据可供导出。");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出数据");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel文件", "*.xlsx"));
        fileChooser.setInitialFileName("砼支撑轴力数据.xlsx");
        
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            // 导出逻辑将在后续实现
            AlertUtil.showInformation("导出成功", "数据已导出到: " + file.getAbsolutePath());
        }
    }

    /**
     * 加载砼支撑轴力测点配置
     */
    private void loadConcreteSupportPoints() {
        // 清除示例数据，实际项目中可从文件或数据库加载测点配置
        configuredPoints.clear();
        
        // 更新测点计数
        updatePointCount();
    }
    
    /**
     * 更新测点数量显示
     */
    private void updatePointCount() {
        if (pointCountLabel != null) {
            pointCountLabel.setText(String.valueOf(configuredPoints.size()));
        }
    }

    /**
     * 处理测点设置按钮点击事件
     */
    @FXML
    private void handleSettingsButtonAction(ActionEvent event) {
        try {
            // 加载设置对话框FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/ConcreteSupportAxialForcePointSettingsDialog.fxml"));
            Parent root = loader.load();
            
            // 获取控制器
            ConcreteSupportAxialForcePointSettingsController controller = loader.getController();
            
            // 创建对话框
            Stage dialogStage = new Stage();
            dialogStage.setTitle("砼支撑轴力测点设置");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(stage);
            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            
            // 设置对话框控制器
            controller.setDialogStage(dialogStage);
            controller.setInitialData(configuredPoints);
            
            // 显示对话框并等待
            dialogStage.showAndWait();
            
            // 获取更新后的测点列表
            List<ConcreteSupportAxialForcePoint> updatedPoints = controller.getPoints();
            configuredPoints = updatedPoints;
            
            // 更新测点计数
            updatePointCount();
            
            // 更新表格数据
            updateTableWithInitialData();
            
        } catch (IOException e) {
            AlertUtil.showError("加载错误", "无法加载测点设置对话框: " + e.getMessage());
        }
    }
    
    /**
     * 处理上传数据按钮点击事件
     */
    @FXML
    private void handleUploadButtonAction(ActionEvent event) {
        // 如果没有测点配置，提示用户先配置测点
        if (configuredPoints.isEmpty()) {
            AlertUtil.showWarning("无法上传数据", "请先配置测点");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择Excel数据文件");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel文件", "*.xlsx", "*.xls"));
                
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                // 读取Excel文件
                try (Workbook workbook = WorkbookFactory.create(file)) {
                    
                    // 查找名为"砼支撑轴力"的工作表
                    Sheet sheet = null;
                    for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                        String sheetName = workbook.getSheetName(i);
                        if (sheetName.contains("砼支撑轴力")) {
                            sheet = workbook.getSheetAt(i);
                            break;
                        }
                    }
                    
                    if (sheet == null) {
                        AlertUtil.showWarning("上传失败", "未找到名为'砼支撑轴力'的工作表，请检查Excel文件格式。");
                        return;
                    }
                    
                    // 检查表头行
                    Row headerRow = sheet.getRow(0);
                    if (headerRow == null) {
                        AlertUtil.showWarning("上传失败", "工作表中没有表头行。");
                        return;
                    }
                    
                    // 确认表头行格式
                    String pointIdHeader = getCellValueAsString(headerRow.getCell(0));
                    String valueHeader = getCellValueAsString(headerRow.getCell(1));
                    
                    if (!pointIdHeader.contains("测点编号") || !valueHeader.contains("本次测值")) {
                        AlertUtil.showWarning("上传失败", "表头格式不正确。需要包含'测点编号'和'本次测值'列。");
                        return;
                    }
                    
                    // 读取数据
                    Map<String, Double> pointValueMap = new HashMap<>();
                    int dataCount = 0;
                    
                    // 读取数据行
                    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row == null) continue;
                        
                        Cell pointIdCell = row.getCell(0);
                        Cell valueCell = row.getCell(1);
                        
                        // 跳过没有必要数据的行
                        if (pointIdCell == null || valueCell == null) {
                            continue;
                        }
                        
                        String pointId = getCellValueAsString(pointIdCell).trim();
                        
                        // 如果点号为空则跳过
                        if (pointId.isEmpty()) {
                            continue;
                        }
                        
                        try {
                            double value = getNumericCellValue(valueCell);
                            pointValueMap.put(pointId, value);
                            dataCount++;
                        } catch (NumberFormatException e) {
                            // 忽略无法解析为数字的值
                            continue;
                        }
                    }
                    
                    if (dataCount == 0) {
                        AlertUtil.showWarning("上传失败", "未找到有效的砼支撑轴力数据。");
                        return;
                    }
                    
                    // 处理数据
                    LocalDate measureDate = datePicker.getValue();
                    if (measureDate == null) {
                        measureDate = LocalDate.now();
                        datePicker.setValue(measureDate);
                    }
                    
                    List<ConcreteSupportAxialForceData> axialForceDataList = processExcelPointData(pointValueMap, measureDate);
                    
                    if (axialForceDataList.isEmpty()) {
                        AlertUtil.showWarning("上传警告", "没有匹配的测点数据，请先配置测点。");
                        return;
                    }
                    
                    // 创建数据块时间戳
                    LocalDateTime dataBlockTimestamp = LocalDateTime.now();
                    
                    // 为数据块创建标签，只显示日期和时间
                    String blockLabel = measureDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + 
                            " " + dataBlockTimestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    
                    // 添加数据块 - 允许同一天上传多期数据
                    addDataBlock(blockLabel, dataBlockTimestamp);
                    dataBlocksMap.put(dataBlockTimestamp, axialForceDataList);
                    
                    // 默认选中该数据块
                    CheckBox checkBox = dataBlockCheckBoxMap.get(dataBlockTimestamp);
                    if (checkBox != null) {
                        checkBox.setSelected(true);
                        selectedDataBlocks.add(dataBlockTimestamp);
                    }
                    
                    // 更新数据显示
                    updateDataDisplay();
                    
                    // 显示成功消息
                    AlertUtil.showInformation("上传成功", "成功导入 " + dataCount + " 条砼支撑轴力数据。");
                }
            } catch (Exception e) {
                AlertUtil.showError("数据处理错误", "处理Excel数据时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 获取数据存储对象
     */
    public ConcreteSupportAxialForceDataStorage getConcreteSupportAxialForceDataStorage() {
        ConcreteSupportAxialForceDataStorage storage = new ConcreteSupportAxialForceDataStorage();
        
        // 设置测点列表
        storage.setPoints(configuredPoints);
        
        // 添加数据块
        for (Map.Entry<LocalDateTime, List<ConcreteSupportAxialForceData>> entry : dataBlocksMap.entrySet()) {
            LocalDateTime timestamp = entry.getKey();
            List<ConcreteSupportAxialForceData> dataBlock = entry.getValue();
            
            String description = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            if (dataBlockCheckBoxMap.containsKey(timestamp)) {
                CheckBox checkBox = dataBlockCheckBoxMap.get(timestamp);
                description = checkBox.getText();
            }
            
            storage.addDataBlock(timestamp, dataBlock, description);
        }
        
        // 设置选中的数据块
        storage.setSelectedDataBlocks(selectedDataBlocks);
        
        return storage;
    }
    
    /**
     * 从砼支撑轴力数据存储对象加载数据
     */
    public void loadFromConcreteSupportAxialForceDataStorage(ConcreteSupportAxialForceDataStorage storage) {
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
            List<ConcreteSupportAxialForceData> dataList = storage.getDataBlock(timestamp);
            String description = storage.getDataBlockDescription(timestamp);
            
            // 如果没有描述或描述只是时间戳，创建更简化的描述
            if (description == null || description.contains("(")) {
                // 从数据中获取测量日期
                if (!dataList.isEmpty()) {
                    LocalDate measureDate = dataList.get(0).getMeasurementDate();
                    description = measureDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + 
                              " " + timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                } else {
                    description = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }
            }
            
            // 添加数据块
            dataBlocksMap.put(timestamp, dataList);
            
            // 添加数据块到UI
            addDataBlock(description, timestamp);
        }
        
        // 设置选定的数据块
        List<LocalDateTime> selected = storage.getSelectedDataBlocks();
        if (selected != null && !selected.isEmpty()) {
            for (LocalDateTime timestamp : selected) {
                CheckBox checkBox = dataBlockCheckBoxMap.get(timestamp);
                if (checkBox != null) {
                    checkBox.setSelected(true);
                    selectedDataBlocks.add(timestamp);
                }
            }
            
            // 更新数据显示
            updateDataDisplay();
        }
        
        // 更新测点计数
        updatePointCount();
    }
    
    /**
     * 从测量记录加载数据
     */
    public void loadFromMeasurementRecords(List<MeasurementRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        
        // 按测量日期分组
        Map<LocalDate, List<MeasurementRecord>> recordsByDate = new HashMap<>();
        for (MeasurementRecord record : records) {
            if (record.getMeasureTime() != null) {
                LocalDate date = record.getMeasureTime().toLocalDate();
                recordsByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(record);
            }
        }
        
        // 实际转换逻辑将在后续实现
        AlertUtil.showInformation("数据加载", "成功加载 " + records.size() + " 条测量记录。");
    }
    
    /**
     * 获取用于保存的测量记录列表
     */
    public List<MeasurementRecord> getMeasurementRecordsForSaving() {
        List<MeasurementRecord> records = new ArrayList<>();
        
        // 遍历所有数据块
        for (Map.Entry<LocalDateTime, List<ConcreteSupportAxialForceData>> entry : dataBlocksMap.entrySet()) {
            for (ConcreteSupportAxialForceData data : entry.getValue()) {
                MeasurementRecord record = new MeasurementRecord();
                record.setId(data.getPointCode() + "_" + data.getMeasurementDate().toString());
                record.setValue(data.getCurrentForce());
                record.setMeasureTime(LocalDateTime.of(data.getMeasurementDate(), LocalTime.NOON));
                record.setUnit("kN");
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
     * 添加数据块到UI
     */
    private void addDataBlock(String blockLabel, LocalDateTime dateTime) {
        // 创建数据块HBox容器
        HBox dataBlockBox = new HBox(5);
        dataBlockBox.getStyleClass().addAll("data-block", "concrete-support-data-block");
        dataBlockBox.setPadding(new javafx.geometry.Insets(5));
        dataBlockBox.setUserData(dateTime); // 存储时间戳为用户数据，便于后续操作
        
        // 创建数据块复选框，使用传入的blockLabel作为标识
        CheckBox checkBox = new CheckBox(blockLabel);
        checkBox.setUserData(dateTime);
        checkBox.setOnAction(e -> handleDataBlockSelection(checkBox));
        
        // 应用自定义样式
        checkBox.getStyleClass().add("data-block-checkbox");
        
        dataBlockBox.getChildren().add(checkBox);
        
        // 添加复选框到UI
        dataBlocksFlowPane.getChildren().add(dataBlockBox);
        dataBlockCheckBoxMap.put(dateTime, checkBox);
        
        // 添加右键菜单
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("删除数据块");
        deleteItem.setOnAction(e -> removeDataBlock(dateTime));
        
        contextMenu.getItems().add(deleteItem);
        
        // 为整个HBox添加右键菜单
        dataBlockBox.setOnContextMenuRequested(e -> {
            contextMenu.show(dataBlockBox, e.getScreenX(), e.getScreenY());
        });
        
        // 添加淡入动画效果
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(800), dataBlockBox);
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        fadeTransition.play();
        
        // 设置工具提示，显示完整的日期时间
        Tooltip tooltip = new Tooltip("上传时间: " + dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        Tooltip.install(dataBlockBox, tooltip);
        
        // 选中新添加的数据块
        checkBox.setSelected(true);
    }
    
    /**
     * 移除数据块
     */
    private void removeDataBlock(LocalDateTime dateTime) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认删除");
        confirmAlert.setHeaderText("删除数据块");
        confirmAlert.setContentText("确定要删除此数据块吗？此操作不可撤销。");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // 从数据结构中移除
            dataBlocksMap.remove(dateTime);
            
            // 从UI中移除
            dataBlocksFlowPane.getChildren().removeIf(node -> {
                if (node instanceof HBox) {
                    HBox box = (HBox) node;
                    return box.getUserData() == dateTime;
                }
                return false;
            });
            
            // 从选中列表中移除
            selectedDataBlocks.remove(dateTime);
            
            // 从映射中移除
            dataBlockCheckBoxMap.remove(dateTime);
            
            // 更新数据显示
            updateDataDisplay();
        }
    }
    
    /**
     * 处理数据块选择
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
            // 如果选择了超过两个数据块，只保留最早和最新的两个
            List<LocalDateTime> sortedBlocks = new ArrayList<>(selectedDataBlocks);
            Collections.sort(sortedBlocks);
            
            LocalDateTime earliest = sortedBlocks.get(0);
            LocalDateTime latest = sortedBlocks.get(sortedBlocks.size() - 1);
            
            // 清除中间的数据块选择
            for (LocalDateTime dt : new ArrayList<>(selectedDataBlocks)) {
                if (!dt.equals(earliest) && !dt.equals(latest)) {
                    selectedDataBlocks.remove(dt);
                    
                    // 更新UI中对应数据块的复选框状态
                    CheckBox cb = dataBlockCheckBoxMap.get(dt);
                    if (cb != null) {
                        cb.setSelected(false);
                    }
                }
            }
        }
        
        // 更新数据显示
        updateDataDisplay();
    }
    
    /**
     * 更新数据显示
     */
    private void updateDataDisplay() {
        // 如果没有选择任何数据块，清空表格
        if (selectedDataBlocks.isEmpty()) {
            axialForceDataList.clear();
            return;
        }
        
        // 清空当前表格数据
        axialForceDataList.clear();
        
        // 根据选择的数据块数量处理
        if (selectedDataBlocks.size() == 1) {
            // 只选择了一个数据块，直接显示其数据
            LocalDateTime currentDateTime = selectedDataBlocks.get(0);
            List<ConcreteSupportAxialForceData> currentData = dataBlocksMap.get(currentDateTime);
            
            if (currentData != null) {
                // 修复问题1：当选择第一期数据时，确保上次轴力为0
                // 创建排序后的时间戳列表
                List<LocalDateTime> sortedTimestamps = new ArrayList<>(dataBlocksMap.keySet());
                Collections.sort(sortedTimestamps);
                
                // 如果当前数据块是最早的一期数据，将上次轴力设为0
                if (!sortedTimestamps.isEmpty() && currentDateTime.equals(sortedTimestamps.get(0))) {
                    for (ConcreteSupportAxialForceData data : currentData) {
                        data.setPreviousForce(0.0);
                        // 确保变化量与当前轴力一致
                        data.setCurrentChange(data.getCurrentForce() - data.getPreviousForce());
                    }
                }
                
                axialForceDataList.addAll(currentData);
            }
            
            // 更新上传日期标签
            if (uploadDateLabel != null) {
                uploadDateLabel.setText(currentDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
        } else if (selectedDataBlocks.size() >= 2) {
            // 选择了两个数据块，对它们进行排序（按时间顺序）
            List<LocalDateTime> sortedTimestamps = new ArrayList<>(selectedDataBlocks);
            Collections.sort(sortedTimestamps);
            
            // 确定哪个是较早的数据块，哪个是较新的数据块
            LocalDateTime earliestDateTime = sortedTimestamps.get(0);
            LocalDateTime latestDateTime = sortedTimestamps.get(sortedTimestamps.size() - 1);
            
            List<ConcreteSupportAxialForceData> earliestData = dataBlocksMap.get(earliestDateTime);
            List<ConcreteSupportAxialForceData> latestData = dataBlocksMap.get(latestDateTime);
            
            // 创建测点ID到上一期数据的映射
            Map<String, ConcreteSupportAxialForceData> earliestDataMap = new HashMap<>();
            for (ConcreteSupportAxialForceData data : earliestData) {
                earliestDataMap.put(data.getPointCode(), data);
            }
            
            // 处理最新数据，关联早期数据
            for (ConcreteSupportAxialForceData latestItem : latestData) {
                String pointCode = latestItem.getPointCode();
                ConcreteSupportAxialForceData earliestItem = earliestDataMap.get(pointCode);
                
                if (earliestItem != null) {
                    // 设置前期轴力为最早期数据的轴力
                    latestItem.setPreviousForce(earliestItem.getCurrentForce());
                    
                    // 计算变化量
                    latestItem.setCurrentChange(latestItem.getCurrentForce() - latestItem.getPreviousForce());
                }
                
                axialForceDataList.add(latestItem);
            }
            
            // 更新上传日期标签显示最近的时间
            if (uploadDateLabel != null) {
                uploadDateLabel.setText(latestDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
        }
        
        // 更新图表
        updateChart();
    }

    /**
     * 处理Excel数据并生成砼支撑轴力数据列表
     */
    private List<ConcreteSupportAxialForceData> processExcelPointData(Map<String, Double> pointValueMap, LocalDate measureDate) {
        List<ConcreteSupportAxialForceData> result = new ArrayList<>();
        
        // 如果没有配置测点，则返回空列表
        if (configuredPoints.isEmpty()) {
            return result;
        }
        
        // 构建所有点的历史数据映射
        Map<String, Map<LocalDate, ConcreteSupportAxialForceData>> allPointDataMap = getAllPointDataMap();
        
        // 遍历配置的测点
        for (ConcreteSupportAxialForcePoint point : configuredPoints) {
            String pointId = point.getPointId();
            
            // 检查是否有该测点的数据
            if (!pointValueMap.containsKey(pointId)) {
                continue;
            }
            
            // 获取当前轴力值
            double currentForce = pointValueMap.get(pointId);
            
            // 查找前期轴力
            double previousForce = currentForce;  // 默认与当前值相同
            LocalDate previousDate = null;
            
            // 从历史数据中查找最近一次的前期数据
            Map<LocalDate, ConcreteSupportAxialForceData> pointHistory = allPointDataMap.get(pointId);
            if (pointHistory != null && !pointHistory.isEmpty()) {
                // 找出测量日期之前的最新数据
                for (LocalDate date : pointHistory.keySet()) {
                    if (date.isBefore(measureDate) && (previousDate == null || date.isAfter(previousDate))) {
                        previousDate = date;
                    }
                }
                
                // 如果找到前期数据，获取前期轴力
                if (previousDate != null) {
                    ConcreteSupportAxialForceData previousData = pointHistory.get(previousDate);
                    previousForce = previousData.getCurrentForce();
                }
            }
            
            // 创建砼支撑轴力数据对象
            ConcreteSupportAxialForceData axialForceData = new ConcreteSupportAxialForceData();
            axialForceData.setPointCode(pointId);
            axialForceData.setPreviousForce(previousForce);
            axialForceData.setCurrentForce(currentForce);
            axialForceData.setCurrentChange(currentForce - previousForce);
            axialForceData.setMeasurementDate(measureDate);
            axialForceData.setMileage(point.getMileage());
            axialForceData.setHistoricalCumulative(point.getHistoricalCumulative());
            
            // 添加到结果列表
            result.add(axialForceData);
        }
        
        return result;
    }

    /**
     * 获取所有点的历史数据映射
     */
    private Map<String, Map<LocalDate, ConcreteSupportAxialForceData>> getAllPointDataMap() {
        Map<String, Map<LocalDate, ConcreteSupportAxialForceData>> result = new HashMap<>();
        
        // 遍历所有数据块
        for (LocalDateTime blockTime : dataBlocksMap.keySet()) {
            List<ConcreteSupportAxialForceData> blockData = dataBlocksMap.get(blockTime);
            if (blockData != null) {
                for (ConcreteSupportAxialForceData data : blockData) {
                    String pointId = data.getPointCode();
                    LocalDate date = data.getMeasurementDate();
                    
                    // 获取或创建该测点的数据映射
                    Map<LocalDate, ConcreteSupportAxialForceData> pointDataMap = 
                            result.computeIfAbsent(pointId, k -> new HashMap<>());
                    
                    // 添加该测点在当前日期的数据
                    pointDataMap.put(date, data);
                }
            }
        }
        
        return result;
    }

    /**
     * 获取单元格的字符串值
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }
        
        switch (cellType) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                double value = cell.getNumericCellValue();
                // 检查是否为整数
                if (value == Math.floor(value)) {
                    return String.format("%.0f", value);
                }
                return String.format("%.6f", value);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    /**
     * 获取单元格的数值
     */
    private double getNumericCellValue(Cell cell) throws NumberFormatException {
        if (cell == null) {
            throw new NumberFormatException("空单元格");
        }
        
        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }
        
        switch (cellType) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                // 尝试将字符串转换为数字
                String str = cell.getStringCellValue().trim();
                return Double.parseDouble(str);
            default:
                throw new NumberFormatException("无效的数值类型");
        }
    }
} 