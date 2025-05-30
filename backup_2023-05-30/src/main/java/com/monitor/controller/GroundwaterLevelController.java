package com.monitor.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.monitor.model.GroundwaterLevelData;
import com.monitor.model.GroundwaterLevelPoint;
import com.monitor.model.MeasurementRecord;
import com.monitor.model.GroundwaterLevelDataStorage;
import com.monitor.util.AlertUtil;
import com.monitor.util.ExcelUtil;
import com.monitor.view.GroundwaterLevelPointSettingsController;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.chart.CategoryAxis;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;
import javafx.scene.control.ToggleGroup;

/**
 * 地下水位数据管理控制器
 */
public class GroundwaterLevelController {

    @FXML private Label titleLabel;
    @FXML private VBox topContainer;
    @FXML private Button uploadDataButton;
    @FXML private DatePicker datePicker;
    @FXML private Button exportButton;
    @FXML private Button monitoringPointSettingsButton;
    
    @FXML private FlowPane dataBlocksFlowPane;
    
    @FXML private TabPane analysisTabPane;
    @FXML private Tab tableAnalysisTab;
    @FXML private Tab chartAnalysisTab;
    
    @FXML private TableView<GroundwaterLevelData> dataTableView;
    @FXML private TableColumn<GroundwaterLevelData, Number> serialNumberColumn;
    @FXML private TableColumn<GroundwaterLevelData, String> pointCodeColumn;
    @FXML private TableColumn<GroundwaterLevelData, Number> initialElevationColumn;
    @FXML private TableColumn<GroundwaterLevelData, Number> previousElevationColumn;
    @FXML private TableColumn<GroundwaterLevelData, Number> currentElevationColumn;
    @FXML private TableColumn<GroundwaterLevelData, Number> currentChangeColumn;
    @FXML private TableColumn<GroundwaterLevelData, Number> cumulativeChangeColumn;
    @FXML private TableColumn<GroundwaterLevelData, Number> changeRateColumn;
    @FXML private TableColumn<GroundwaterLevelData, String> mileageColumn;
    @FXML private TableColumn<GroundwaterLevelData, Number> historicalCumulativeColumn;
    
    @FXML private BorderPane chartContainer;
    
    // 图表相关控件
    private LineChart<String, Number> displacementChart;
    private LineChart<String, Number> rateChart;
    private NumberAxis displacementYAxis;
    private CategoryAxis displacementXAxis;
    private NumberAxis rateYAxis;
    private CategoryAxis rateXAxis;
    
    // 图表切换按钮
    @FXML private ToggleButton displacementChartButton;
    @FXML private ToggleButton rateChartButton;
    
    @FXML private Label pointCountLabel;
    @FXML private Label uploadDateLabel;
    
    private ObservableList<GroundwaterLevelData> dataList = FXCollections.observableArrayList();
    private Stage stage;
    
    // 所有测点的所有历史数据，结构为：<测点编号, <测量日期, 数据>>
    private Map<String, Map<LocalDate, GroundwaterLevelData>> allPointDataMap = new HashMap<>();
    
    // 数据块映射，每个数据块对应一次上传的数据，结构为：<时间戳, 数据列表>
    private Map<LocalDateTime, List<GroundwaterLevelData>> dataBlocksMap = new HashMap<>();
    
    // 数据块复选框映射
    private Map<LocalDateTime, CheckBox> dataBlockCheckBoxMap = new HashMap<>();
    
    // 当前选中的数据块
    private List<LocalDateTime> selectedDataBlocks = new ArrayList<>();
    
    // 配置的测点列表
    private List<GroundwaterLevelPoint> configuredPoints = new ArrayList<>();
    
    // 自定义速率计算天数
    private int customDaysForRateCalculation = 0;
    
    // 切换按钮组
    private ToggleGroup chartToggleGroup;
    
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    @FXML
    public void initialize() {
        // 初始化切换按钮组
        chartToggleGroup = new ToggleGroup();
        displacementChartButton.setToggleGroup(chartToggleGroup);
        rateChartButton.setToggleGroup(chartToggleGroup);
        displacementChartButton.setSelected(true);
        
        // 设置切换事件
        displacementChartButton.setOnAction(e -> showDisplacementChart());
        rateChartButton.setOnAction(e -> showRateChart());
        
        // 初始化表格
        dataTableView.setItems(dataList);
        setupTableColumns();
        
        // 初始化图表
        initializeCharts();
        showDisplacementChart(); // 默认显示水位变化图表
        
        // 初始化日期选择器为当前日期
        datePicker.setValue(LocalDate.now());
        
        // 添加表格数据变化监听器，当数据变化时自动更新图表
        dataList.addListener((javafx.collections.ListChangeListener.Change<? extends GroundwaterLevelData> c) -> {
            updateChart();
        });

        // 创建表格上下文菜单
        ContextMenu contextMenu = new ContextMenu();
        MenuItem setCustomDaysItem = new MenuItem("设置变化速率计算天数");
        setCustomDaysItem.setOnAction(e -> showSetCustomDaysDialog());
        contextMenu.getItems().add(setCustomDaysItem);
        dataTableView.setContextMenu(contextMenu);
        
        // 加载测点配置
        loadGroundwaterLevelPoints();
    }
    
    /**
     * 设置表格列
     */
    private void setupTableColumns() {
        // 设置表格列的单元格工厂和值工厂
        serialNumberColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(dataList.indexOf(cellData.getValue()) + 1));
        pointCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPointCode()));
        
        // 初始高程、前期高程、当前高程列 - 单位为米，保留三位小数
        initialElevationColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getInitialElevation()));
        initialElevationColumn.setCellFactory(column -> new TableCell<GroundwaterLevelData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // 单位为米，保留三位小数
                    setText(String.format("%.3f", item.doubleValue()));
                }
            }
        });
        
        previousElevationColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getPreviousElevation()));
        previousElevationColumn.setCellFactory(column -> new TableCell<GroundwaterLevelData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // 单位为米，保留三位小数
                    setText(String.format("%.3f", item.doubleValue()));
                }
            }
        });
        
        currentElevationColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getCurrentElevation()));
        currentElevationColumn.setCellFactory(column -> new TableCell<GroundwaterLevelData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // 单位为米，保留三位小数
                    setText(String.format("%.3f", item.doubleValue()));
                }
            }
        });
        
        // 本期变化列 - 单位为毫米，保留两位小数
        currentChangeColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getCurrentChange()));
        currentChangeColumn.setCellFactory(column -> new TableCell<GroundwaterLevelData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    // 单位为毫米，保留两位小数
                    setText(String.format("%.2f", item.doubleValue()));
                    
                    // 根据数值设置颜色：正值(水位上升)为蓝色，负值(水位下降)为红色
                    double value = item.doubleValue();
                    if (value > 0) {
                        setTextFill(Color.BLUE);
                    } else if (value < 0) {
                        setTextFill(Color.RED);
                    } else {
                        setTextFill(Color.BLACK);
                    }
                }
            }
        });
        
        // 累计变化列 - 单位为毫米，保留两位小数
        cumulativeChangeColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getCumulativeChange()));
        cumulativeChangeColumn.setCellFactory(column -> new TableCell<GroundwaterLevelData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    // 单位为毫米，保留两位小数
                    setText(String.format("%.2f", item.doubleValue()));
                    
                    // 根据数值设置颜色：正值(水位上升)为蓝色，负值(水位下降)为红色
                    double value = item.doubleValue();
                    if (value > 0) {
                        setTextFill(Color.BLUE);
                    } else if (value < 0) {
                        setTextFill(Color.RED);
                    } else {
                        setTextFill(Color.BLACK);
                    }
                }
            }
        });
        
        // 变化速率列 - 单位为毫米/天，保留两位小数
        changeRateColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getChangeRate()));
        changeRateColumn.setCellFactory(column -> new TableCell<GroundwaterLevelData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    // 单位为毫米/天，保留两位小数
                    setText(String.format("%.2f", item.doubleValue()));
                    
                    // 根据数值设置颜色：正值(水位上升)为蓝色，负值(水位下降)为红色
                    double value = item.doubleValue();
                    if (value > 0) {
                        setTextFill(Color.BLUE);
                    } else if (value < 0) {
                        setTextFill(Color.RED);
                    } else {
                        setTextFill(Color.BLACK);
                    }
                }
            }
        });
        
        mileageColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMileage()));
        
        // 历史累计列 - 单位为毫米，保留两位小数
        historicalCumulativeColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getHistoricalCumulative()));
        historicalCumulativeColumn.setCellFactory(column -> new TableCell<GroundwaterLevelData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // 单位为毫米，保留两位小数
                    setText(String.format("%.2f", item.doubleValue()));
                }
            }
        });
    }
    
    /**
     * 初始化图表
     */
    private void initializeCharts() {
        // 水位变化图表
        displacementXAxis = new CategoryAxis();
        displacementYAxis = new NumberAxis();
        displacementXAxis.setLabel("测点编号");
        displacementYAxis.setLabel("水位变化量(mm)");
        displacementChart = new LineChart<>(displacementXAxis, displacementYAxis);
        displacementChart.setTitle("地下水位变化量");
        displacementChart.setCreateSymbols(true);
        displacementChart.setAnimated(false);
        
        // 变化速率图表
        rateXAxis = new CategoryAxis();
        rateYAxis = new NumberAxis();
        rateXAxis.setLabel("测点编号");
        rateYAxis.setLabel("变化速率(mm/天)");
        rateChart = new LineChart<>(rateXAxis, rateYAxis);
        rateChart.setTitle("地下水位变化速率");
        rateChart.setCreateSymbols(true);
        rateChart.setAnimated(false);
    }
    
    /**
     * 显示水位变化图表
     */
    private void showDisplacementChart() {
        chartContainer.setCenter(displacementChart);
        updateChart();
    }
    
    /**
     * 显示速率图表
     */
    private void showRateChart() {
        chartContainer.setCenter(rateChart);
        updateChart();
    }
    
    /**
     * 更新图表显示
     */
    private void updateChart() {
        if (dataList.isEmpty() || displacementChart == null || rateChart == null) {
            return;
        }
        
        // 清除现有数据
        displacementChart.getData().clear();
        rateChart.getData().clear();
        
        // 创建数据系列
        XYChart.Series<String, Number> displacementSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> currentChangeSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> rateSeries = new XYChart.Series<>();
        
        displacementSeries.setName("累计变化量(mm)");
        currentChangeSeries.setName("本次变化量(mm)");
        rateSeries.setName("变化速率(mm/d)");
        
        // 添加数据点
        for (GroundwaterLevelData data : dataList) {
            String pointId = data.getPointCode();
            
            // 累计变化量
            displacementSeries.getData().add(new XYChart.Data<>(pointId, data.getCumulativeChange()));
            
            // 本次变化量
            currentChangeSeries.getData().add(new XYChart.Data<>(pointId, data.getCurrentChange()));
            
            // 变化速率
            rateSeries.getData().add(new XYChart.Data<>(pointId, data.getChangeRate()));
        }
        
        // 将数据添加到图表
        displacementChart.getData().add(displacementSeries);
        displacementChart.getData().add(currentChangeSeries);
        rateChart.getData().add(rateSeries);
        
        // 应用自定义样式
        applyChartStyling();
    }
    
    /**
     * 应用图表样式
     */
    private void applyChartStyling() {
        // 为累计变化量线条设置样式（实线，蓝色）
        if (!displacementChart.getData().isEmpty() && displacementChart.getData().size() >= 1) {
            XYChart.Series<String, Number> cumulativeSeries = displacementChart.getData().get(0);
            String cumulativeStyle = "-fx-stroke: #0000ff; -fx-stroke-width: 2px;";
            
            cumulativeSeries.getNode().setStyle(cumulativeStyle);
            
            // 设置数据点样式
            for (XYChart.Data<String, Number> data : cumulativeSeries.getData()) {
                if (data.getNode() != null) {
                    data.getNode().setStyle("-fx-background-color: #0000ff, white;");
                }
            }
        }
        
        // 为本次变化量线条设置样式（虚线，红色）
        if (displacementChart.getData().size() >= 2) {
            XYChart.Series<String, Number> currentSeries = displacementChart.getData().get(1);
            String currentStyle = "-fx-stroke: #ff0000; -fx-stroke-width: 2px; -fx-stroke-dash-array: 5 5;";
            
            currentSeries.getNode().setStyle(currentStyle);
            
            // 设置数据点样式
            for (XYChart.Data<String, Number> data : currentSeries.getData()) {
                if (data.getNode() != null) {
                    data.getNode().setStyle("-fx-background-color: #ff0000, white;");
                }
            }
        }
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
        
        // 添加右键菜单
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("删除数据块");
        deleteItem.setOnAction(e -> removeDataBlock(dateTime));
        contextMenu.getItems().add(deleteItem);
        
        // 为整个HBox添加右键菜单
        dataBlockBox.setOnContextMenuRequested(e -> {
            contextMenu.show(dataBlockBox, e.getScreenX(), e.getScreenY());
        });
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
     * 显示设置自定义天数对话框
     */
    private void showSetCustomDaysDialog() {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(customDaysForRateCalculation));
        dialog.setTitle("自定义设置");
        dialog.setHeaderText("设置变化速率计算天数");
        dialog.setContentText("请输入天数(0表示使用实际间隔天数):");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(days -> {
            try {
                int daysValue = Integer.parseInt(days);
                if (daysValue >= 0) {
                    customDaysForRateCalculation = daysValue;
                    // 更新数据显示
                    updateDataDisplay();
                } else {
                    AlertUtil.showWarning("输入错误", "天数必须是非负整数。");
                }
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("输入错误", "请输入有效的整数。");
            }
        });
    }
    
    /**
     * 加载地下水位测点信息
     */
    private void loadGroundwaterLevelPoints() {
        // 此处可以添加从文件或数据库加载测点配置的代码
        // 现在我们只添加几个示例测点
        if (configuredPoints.isEmpty()) {
            // 示例测点数据
            configuredPoints.add(new GroundwaterLevelPoint("WL01", 102.500, "K0+100", 5.0, 10.0));
            configuredPoints.add(new GroundwaterLevelPoint("WL02", 103.200, "K0+200", 5.0, 10.0));
            configuredPoints.add(new GroundwaterLevelPoint("WL03", 104.100, "K0+300", 5.0, 10.0));
        }
        
        // 更新测点计数显示
        updatePointCount();
    }
    
    /**
     * 处理上传数据按钮点击事件
     */
    @FXML
    private void handleUploadButtonAction(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择Excel数据文件");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel文件", "*.xlsx", "*.xls"));
                
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                // 打开Excel文件
                try (FileInputStream fis = new FileInputStream(file);
                     Workbook workbook = WorkbookFactory.create(fis)) {
                    
                    // 查找名为"地下水位"的工作表
                    Sheet sheet = null;
                    for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                        String sheetName = workbook.getSheetName(i);
                        if (sheetName.contains("地下水位")) {
                            sheet = workbook.getSheetAt(i);
                            break;
                        }
                    }
                    
                    if (sheet == null) {
                        AlertUtil.showInformation("上传失败", "未找到含有'地下水位'的工作表，请检查Excel文件格式。");
                        return;
                    }
                    
                    // 检查表头行
                    Row headerRow = sheet.getRow(0);
                    if (headerRow == null) {
                        AlertUtil.showInformation("上传失败", "工作表中没有表头行。");
                        return;
                    }
                    
                    // 确认表头行格式
                    String pointIdHeader = getCellValueAsString(headerRow.getCell(0));
                    String elevationHeader = getCellValueAsString(headerRow.getCell(1));
                    
                    if (!pointIdHeader.contains("测点") || !elevationHeader.contains("高程")) {
                        AlertUtil.showInformation("上传失败", "表头格式不正确。需要包含'测点'和'高程'列。");
                        return;
                    }
                    
                    // 读取数据
                    Map<String, Double> pointElevationMap = new HashMap<>();
                    int dataCount = 0;
                    
                    // 读取数据行
                    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row == null) continue;
                        
                        Cell pointIdCell = row.getCell(0);
                        Cell elevationCell = row.getCell(1);
                        
                        // 跳过没有必要数据的行
                        if (pointIdCell == null || elevationCell == null) {
                            continue;
                        }
                        
                        String pointId = getCellValueAsString(pointIdCell).trim();
                        
                        // 如果点号为空则跳过
                        if (pointId.isEmpty()) {
                            continue;
                        }
                        
                        try {
                            double elevation = getNumericCellValue(elevationCell);
                            pointElevationMap.put(pointId, elevation);
                            dataCount++;
                        } catch (NumberFormatException e) {
                            // 忽略无法解析为数字的高程值
                            continue;
                        }
                    }
                    
                    if (dataCount == 0) {
                        AlertUtil.showInformation("上传失败", "未找到有效的地下水位数据。");
                        return;
                    }
                    
                    // 处理数据
                    LocalDate measureDate = datePicker.getValue();
                    if (measureDate == null) {
                        measureDate = LocalDate.now();
                        datePicker.setValue(measureDate);
                    }
                    
                    List<GroundwaterLevelData> waterLevelDataList = processExcelPointData(pointElevationMap, measureDate);
                    
                    if (waterLevelDataList.isEmpty()) {
                        AlertUtil.showInformation("上传警告", "没有匹配的测点数据，请先配置测点。");
                        return;
                    }
                    
                    // 创建数据块时间戳
                    LocalDateTime dataBlockTimestamp = LocalDateTime.now();
                    
                    // 检查是否有相同日期的数据
                    for (LocalDateTime timestamp : dataBlocksMap.keySet()) {
                        List<GroundwaterLevelData> existingData = dataBlocksMap.get(timestamp);
                        if (!existingData.isEmpty() && existingData.get(0).getMeasurementDate().equals(measureDate)) {
                            // 有相同日期的数据，提示用户
                            handleDataDateConflict(measureDate, waterLevelDataList, file.getName(), dataCount);
                            return;
                        }
                    }
                    
                    // 添加数据块
                    addDataBlock(file.getName(), dataBlockTimestamp);
                    dataBlocksMap.put(dataBlockTimestamp, waterLevelDataList);
                    
                    // 默认选中该数据块
                    CheckBox checkBox = dataBlockCheckBoxMap.get(dataBlockTimestamp);
                    if (checkBox != null) {
                        checkBox.setSelected(true);
                        selectedDataBlocks.add(dataBlockTimestamp);
                    }
                    
                    // 更新数据显示
                    updateDataDisplay();
                    
                    // 显示成功消息
                    AlertUtil.showInformation("上传成功", "成功导入 " + dataCount + " 条地下水位数据。");
                }
            } catch (Exception e) {
                AlertUtil.showError("数据处理错误", "处理Excel数据时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 处理数据日期冲突
     */
    private void handleDataDateConflict(LocalDate date, List<GroundwaterLevelData> data, String fileName, int dataCount) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("数据日期冲突");
        alert.setHeaderText("发现相同日期(" + date + ")的数据");
        alert.setContentText("是否仍要导入这些数据？");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // 用户确认导入，创建新数据块
            LocalDateTime dataBlockTimestamp = LocalDateTime.now();
            addDataBlock(fileName, dataBlockTimestamp);
            dataBlocksMap.put(dataBlockTimestamp, data);
            
            // 选中该数据块
            CheckBox checkBox = dataBlockCheckBoxMap.get(dataBlockTimestamp);
            if (checkBox != null) {
                checkBox.setSelected(true);
                selectedDataBlocks.add(dataBlockTimestamp);
            }
            
            // 更新数据显示
            updateDataDisplay();
            
            // 显示成功消息
            AlertUtil.showInformation("上传成功", "成功导入 " + dataCount + " 条地下水位数据。");
        }
    }
    
    /**
     * 处理Excel点数据
     */
    private List<GroundwaterLevelData> processExcelPointData(Map<String, Double> pointElevationMap, LocalDate measureDate) {
        List<GroundwaterLevelData> result = new ArrayList<>();
        
        // 如果没有配置测点，则返回空列表
        if (configuredPoints.isEmpty()) {
            return result;
        }
        
        // 构建所有点的历史数据映射
        Map<String, Map<LocalDate, GroundwaterLevelData>> allPointDataMap = getAllPointDataMap();
        
        // 遍历配置的测点
        for (GroundwaterLevelPoint point : configuredPoints) {
            String pointId = point.getPointId();
            
            // 检查是否有该测点的数据
            if (!pointElevationMap.containsKey(pointId)) {
                continue;
            }
            
            // 获取当前高程
            double currentElevation = pointElevationMap.get(pointId);
            
            // 获取初始高程（从配置中获取）
            double initialElevation = point.getInitialElevation();
            
            // 查找前期高程
            double previousElevation = initialElevation;
            LocalDate previousDate = null;
            
            // 从历史数据中查找最近一次的前期数据
            Map<LocalDate, GroundwaterLevelData> pointHistory = allPointDataMap.get(pointId);
            if (pointHistory != null && !pointHistory.isEmpty()) {
                // 找出测量日期之前的最新数据
                for (LocalDate date : pointHistory.keySet()) {
                    if (date.isBefore(measureDate) && (previousDate == null || date.isAfter(previousDate))) {
                        previousDate = date;
                    }
                }
                
                // 如果找到前期数据，获取前期高程
                if (previousDate != null) {
                    GroundwaterLevelData previousData = pointHistory.get(previousDate);
                    previousElevation = previousData.getCurrentElevation();
                }
            }
            
            // 创建地下水位数据对象
            GroundwaterLevelData waterLevelData = new GroundwaterLevelData(
                    pointId, initialElevation, previousElevation, currentElevation, measureDate);
            
            // 设置里程
            waterLevelData.setMileage(point.getMileage());
            
            // 设置历史累计量
            waterLevelData.setHistoricalCumulative(point.getHistoricalCumulative());
            
            // 重新计算衍生值
            if (previousDate != null) {
                waterLevelData.calculateDerivedValues(previousDate, measureDate, customDaysForRateCalculation);
            } else {
                waterLevelData.calculateDerivedValues();
            }
            
            // 添加到结果列表
            result.add(waterLevelData);
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
    
    /**
     * 获取所有点的历史数据映射
     */
    private Map<String, Map<LocalDate, GroundwaterLevelData>> getAllPointDataMap() {
        Map<String, Map<LocalDate, GroundwaterLevelData>> result = new HashMap<>();
        
        // 遍历所有数据块
        for (LocalDateTime blockTime : dataBlocksMap.keySet()) {
            List<GroundwaterLevelData> blockData = dataBlocksMap.get(blockTime);
            if (blockData != null) {
                for (GroundwaterLevelData data : blockData) {
                    String pointId = data.getPointCode();
                    LocalDate date = data.getMeasurementDate();
                    
                    // 获取或创建该测点的数据映射
                    Map<LocalDate, GroundwaterLevelData> pointDataMap = 
                            result.computeIfAbsent(pointId, k -> new HashMap<>());
                    
                    // 添加该测点在当前日期的数据
                    pointDataMap.put(date, data);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 处理导出数据按钮点击事件
     */
    @FXML
    private void handleExportButtonAction(ActionEvent event) {
        if (dataList.isEmpty()) {
            AlertUtil.showInformation("导出", "没有数据可供导出。");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出数据");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel文件", "*.xlsx"));
        fileChooser.setInitialFileName("地下水位数据.xlsx");
        
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            // 显示导出成功消息
            AlertUtil.showInformation("导出成功", "数据已导出到: " + file.getAbsolutePath());
        }
    }
    
    /**
     * 处理测点设置按钮点击事件
     */
    @FXML
    private void handleSettingsButtonAction(ActionEvent event) {
        try {
            // 加载设置对话框FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/GroundwaterLevelPointSettingsDialog.fxml"));
            Parent root = loader.load();
            
            // 获取控制器
            GroundwaterLevelPointSettingsController controller = loader.getController();
            
            // 创建对话框
            Stage dialogStage = new Stage();
            dialogStage.setTitle("地下水位测点设置");
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
            List<GroundwaterLevelPoint> updatedPoints = controller.getPoints();
            configuredPoints = updatedPoints;
            
            // 更新测点计数
            updatePointCount();
            
        } catch (IOException e) {
            AlertUtil.showError("加载错误", "无法加载测点设置对话框: " + e.getMessage());
        }
    }
    
    /**
     * 更新测点计数显示
     */
    private void updatePointCount() {
        pointCountLabel.setText(String.valueOf(configuredPoints.size()));
    }
    
    /**
     * 获取用于保存的测量记录列表
     */
    public List<MeasurementRecord> getMeasurementRecordsForSaving() {
        List<MeasurementRecord> records = new ArrayList<>();
        
        // 遍历所有数据块
        for (Map.Entry<LocalDateTime, List<GroundwaterLevelData>> entry : dataBlocksMap.entrySet()) {
            for (GroundwaterLevelData data : entry.getValue()) {
                MeasurementRecord record = new MeasurementRecord();
                record.setId(data.getPointCode() + "_" + data.getMeasurementDate().toString());
                record.setValue(data.getCurrentElevation());
                record.setMeasureTime(LocalDateTime.of(data.getMeasurementDate(), LocalTime.NOON));
                record.setUnit("m");
                records.add(record);
            }
        }
        
        return records;
    }
    
    /**
     * 获取数据存储对象
     */
    public GroundwaterLevelDataStorage getGroundwaterLevelDataStorage() {
        GroundwaterLevelDataStorage storage = new GroundwaterLevelDataStorage();
        
        // 设置测点列表
        storage.setPoints(configuredPoints);
        
        // 设置自定义速率计算天数
        storage.setCustomDaysForRateCalculation(customDaysForRateCalculation);
        
        // 设置选中的数据块
        storage.setSelectedDataBlocks(selectedDataBlocks);
        
        // 添加数据块
        for (Map.Entry<LocalDateTime, List<GroundwaterLevelData>> entry : dataBlocksMap.entrySet()) {
            LocalDateTime timestamp = entry.getKey();
            List<GroundwaterLevelData> dataBlock = entry.getValue();
            
            String description = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            if (dataBlockCheckBoxMap.containsKey(timestamp)) {
                CheckBox checkBox = dataBlockCheckBoxMap.get(timestamp);
                description = checkBox.getText();
            }
            
            storage.addDataBlock(timestamp, dataBlock, description);
        }
        
        return storage;
    }
    
    /**
     * 从地下水位数据存储对象加载数据
     * @param storage 地下水位数据存储对象
     */
    public void loadFromGroundwaterLevelDataStorage(GroundwaterLevelDataStorage storage) {
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
            List<GroundwaterLevelData> dataList = storage.getDataBlock(timestamp);
            String description = storage.getDataBlockDescription(timestamp);
            
            // 添加数据块
            dataBlocksMap.put(timestamp, dataList);
            
            // 添加数据块到UI
            addDataBlock(description != null ? description : "导入数据", timestamp);
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
        
        // 设置自定义天数
        customDaysForRateCalculation = storage.getCustomDaysForRateCalculation();
        
        // 更新测点计数
        updatePointCount();
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
        
        updateDataDisplay();
    }
    
    /**
     * 更新数据显示
     */
    private void updateDataDisplay() {
        // 如果没有选择任何数据块，清空表格
        if (selectedDataBlocks.isEmpty()) {
            dataList.clear();
            return;
        }

        // 清空当前表格数据
        dataList.clear();

        // 根据选择的数据块数量处理
        if (selectedDataBlocks.size() == 1) {
            // 只选择了一个数据块，将其作为当前高程
            LocalDateTime currentDateTime = selectedDataBlocks.get(0);
            List<GroundwaterLevelData> currentData = dataBlocksMap.get(currentDateTime);

            // 创建按测点排序的数据列表
            Map<String, GroundwaterLevelData> dataMap = new HashMap<>();
            for (GroundwaterLevelData data : currentData) {
                dataMap.put(data.getPointCode(), data);
            }

            // 按照测点列表顺序添加数据
            List<GroundwaterLevelPoint> sortedPoints = new ArrayList<>(configuredPoints);
            
            // 按排序后的测点顺序添加数据
            for (GroundwaterLevelPoint pointConfig : sortedPoints) {
                String pointCode = pointConfig.getPointId();
                GroundwaterLevelData data = dataMap.get(pointCode);

                if (data != null) {
                    // 使用配置中的初始高程
                    data.setInitialElevation(pointConfig.getInitialElevation());
                    // 重新计算派生值
                    data.calculateDerivedValues();
                    dataList.add(data);
                }
            }
        } else if (selectedDataBlocks.size() == 2) {
            // 选择了两个数据块，按时间顺序设置为上一次和当前高程
            LocalDateTime dateTime1 = selectedDataBlocks.get(0);
            LocalDateTime dateTime2 = selectedDataBlocks.get(1);

            LocalDateTime previousDateTime = dateTime1.isBefore(dateTime2) ? dateTime1 : dateTime2;
            LocalDateTime currentDateTime = dateTime1.isBefore(dateTime2) ? dateTime2 : dateTime1;

            List<GroundwaterLevelData> previousData = dataBlocksMap.get(previousDateTime);
            List<GroundwaterLevelData> currentData = dataBlocksMap.get(currentDateTime);

            // 创建测点映射以便快速查找
            Map<String, GroundwaterLevelData> previousDataMap = previousData.stream()
                    .collect(Collectors.toMap(GroundwaterLevelData::getPointCode, data -> data));

            // 创建按测点排序的数据列表
            Map<String, GroundwaterLevelData> currentDataMap = new HashMap<>();
            for (GroundwaterLevelData data : currentData) {
                currentDataMap.put(data.getPointCode(), data);
            }

            // 按照测点列表顺序添加数据
            List<GroundwaterLevelPoint> sortedPoints = new ArrayList<>(configuredPoints);

            // 按排序后的测点顺序添加数据
            for (GroundwaterLevelPoint pointConfig : sortedPoints) {
                String pointCode = pointConfig.getPointId();
                GroundwaterLevelData currentItem = currentDataMap.get(pointCode);

                if (currentItem != null) {
                    // 使用配置中的初始高程
                    currentItem.setInitialElevation(pointConfig.getInitialElevation());

                    // 查找该测点的上一次记录
                    GroundwaterLevelData previousItem = previousDataMap.get(pointCode);

                    if (previousItem != null) {
                        // 使用上一次记录的高程作为前期高程
                        currentItem.setPreviousElevation(previousItem.getCurrentElevation());

                        // 获取自定义天数设置
                        int customDays = customDaysForRateCalculation;

                        // 计算派生值，使用实际测量日期或自定义天数
                        LocalDate previousDate = previousItem.getMeasurementDate();
                        LocalDate currentDate = currentItem.getMeasurementDate();
                        currentItem.calculateDerivedValues(previousDate, currentDate, customDays);
                    }

                    dataList.add(currentItem);
                }
            }
        }

        // 更新上传日期标签
        if (!selectedDataBlocks.isEmpty()) {
            LocalDateTime latestTimestamp = Collections.max(selectedDataBlocks);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            uploadDateLabel.setText(latestTimestamp.format(formatter));
        }
        
        // 更新图表
        updateChart();
    }
    
    /**
     * 从测量记录加载数据
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
        
        // 为每个日期创建数据块
        for (Map.Entry<LocalDate, List<MeasurementRecord>> entry : recordsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<MeasurementRecord> dateRecords = entry.getValue();
            
            LocalDateTime blockTimestamp = LocalDateTime.of(date, LocalTime.NOON);
            List<GroundwaterLevelData> dataBlock = new ArrayList<>();
            
            // 转换测量记录为地下水位数据
            for (MeasurementRecord record : dateRecords) {
                String pointCode = record.getId();
                if (pointCode.contains("_")) {
                    pointCode = pointCode.substring(0, pointCode.indexOf("_"));
                }
                
                // 检查是否在配置的测点列表中
                boolean pointFound = false;
                double initialElevation = record.getValue();
                String mileage = "";
                
                for (GroundwaterLevelPoint point : configuredPoints) {
                    if (point.getPointId().equals(pointCode)) {
                        initialElevation = point.getInitialElevation();
                        mileage = point.getMileage();
                        pointFound = true;
                        break;
                    }
                }
                
                // 如果测点不在配置列表中，可以添加到配置列表
                if (!pointFound) {
                    GroundwaterLevelPoint newPoint = new GroundwaterLevelPoint(
                            pointCode, initialElevation, mileage, 5.0, 10.0);
                    configuredPoints.add(newPoint);
                }
                
                // 创建地下水位数据
                GroundwaterLevelData data = new GroundwaterLevelData();
                data.setPointCode(pointCode);
                data.setInitialElevation(initialElevation);
                data.setCurrentElevation(record.getValue());
                data.setPreviousElevation(initialElevation); // 假设初始值为前期值
                data.setMeasurementDate(date);
                data.setMileage(mileage);
                data.setHistoricalCumulative(0.0);
                
                // 计算变化值
                data.calculateDerivedValues();
                
                dataBlock.add(data);
            }
            
            if (!dataBlock.isEmpty()) {
                // 添加数据块
                dataBlocksMap.put(blockTimestamp, dataBlock);
                
                // 添加数据块到UI
                addDataBlock("导入数据", blockTimestamp);
                
                // 选中此数据块
                CheckBox checkBox = dataBlockCheckBoxMap.get(blockTimestamp);
                if (checkBox != null) {
                    checkBox.setSelected(true);
                    selectedDataBlocks.add(blockTimestamp);
                }
            }
        }
        
        // 更新数据显示
        updateDataDisplay();
        
        // 更新测点计数
        updatePointCount();
        
        // 显示加载成功消息
        if (!dataBlocksMap.isEmpty()) {
            int totalPoints = configuredPoints.size();
            int totalBlocks = dataBlocksMap.size();
            AlertUtil.showInformation("数据加载成功", 
                String.format("已加载 %d 个数据块，共 %d 个测点。", totalBlocks, totalPoints));
        }
    }
} 