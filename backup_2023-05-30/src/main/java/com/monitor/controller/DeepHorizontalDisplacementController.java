package com.monitor.controller;

import com.monitor.model.DeepHorizontalDisplacementData;
import com.monitor.model.DeepHorizontalDisplacementDataStorage;
import com.monitor.model.DeepHorizontalDisplacementPoint;
import com.monitor.model.MeasurementRecord;
import com.monitor.util.AlertUtil;
import com.monitor.view.DeepHorizontalDisplacementPointSettingsController;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.scene.Node;
import javafx.application.Platform;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

/**
 * 深部水平位移数据管理控制器
 */
public class DeepHorizontalDisplacementController {

    @FXML private Button uploadDataButton;
    @FXML private DatePicker datePicker;
    @FXML private Button exportButton;
    @FXML private Button monitoringPointSettingsButton;
    @FXML private ComboBox<String> pointSelector;

    @FXML private FlowPane dataBlocksFlowPane;

    @FXML private TabPane analysisTabPane;
    @FXML private Tab tableAnalysisTab;
    @FXML private Tab chartAnalysisTab;

    @FXML private TableView<DeepHorizontalDisplacementData> dataTableView;
    @FXML private TableColumn<DeepHorizontalDisplacementData, Number> depthColumn;
    @FXML private TableColumn<DeepHorizontalDisplacementData, Number> initialValueColumn;
    @FXML private TableColumn<DeepHorizontalDisplacementData, Number> previousValueColumn;
    @FXML private TableColumn<DeepHorizontalDisplacementData, Number> currentValueColumn;
    @FXML private TableColumn<DeepHorizontalDisplacementData, Number> currentChangeColumn;
    @FXML private TableColumn<DeepHorizontalDisplacementData, Number> cumulativeChangeColumn;
    @FXML private TableColumn<DeepHorizontalDisplacementData, Number> changeRateColumn;
    @FXML private TableColumn<DeepHorizontalDisplacementData, Number> historicalCumulativeColumn;

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

    private ObservableList<DeepHorizontalDisplacementData> displacementDataList = FXCollections.observableArrayList();
    private Stage stage;

    // 测点数据映射 <测点编号, <日期, 数据>>
    private Map<String, Map<LocalDate, DeepHorizontalDisplacementData>> allPointDataMap = new HashMap<>();

    // 数据块映射 <时间戳, 数据列表>
    private Map<LocalDateTime, List<DeepHorizontalDisplacementData>> dataBlocksMap = new HashMap<>();

    // 数据块复选框映射 <时间戳, 复选框>
    private Map<LocalDateTime, CheckBox> dataBlockCheckBoxMap = new HashMap<>();

    // 选中的数据块列表
    private List<LocalDateTime> selectedDataBlocks = new ArrayList<>();

    // 已配置的测点列表
    private List<DeepHorizontalDisplacementPoint> configuredPoints = new ArrayList<>();

    // 自定义计算天数
    private int customDaysForRateCalculation = 0;

    // 当前选中的测点
    private String selectedPoint;

    // 按测点分组的数据块映射 <测点编号, <时间戳, 数据列表>>
    private Map<String, Map<LocalDateTime, List<DeepHorizontalDisplacementData>>> pointDataBlocksMap = new HashMap<>();

    // 垂直方向图表相关变量
    private NumberAxis verticalDisplacementXAxis;
    private NumberAxis verticalDisplacementDepthAxis;
    private NumberAxis verticalRateXAxis;
    private NumberAxis verticalRateDepthAxis;
    private LineChart<Number, Number> verticalDisplacementChart;
    private LineChart<Number, Number> verticalRateChart;

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
        // 初始化切换按钮组
        chartToggleGroup = new ToggleGroup();
        displacementChartButton.setToggleGroup(chartToggleGroup);
        rateChartButton.setToggleGroup(chartToggleGroup);
        
        // 设置切换事件
        displacementChartButton.setOnAction(e -> showDisplacementChart());
        rateChartButton.setOnAction(e -> showRateChart());
        
        // 初始化表格
        setupTableColumns();
        dataTableView.setItems(displacementDataList);
        
        // 设置表格右键菜单
        setupTableContextMenu();
        
        // 初始化图表
        initializeCharts();
        
        // 初始化日期选择器为当前日期
        datePicker.setValue(LocalDate.now());
        
        // 设置测点选择器监听器
        pointSelector.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedPoint = newValue;
            updateDataBlocksForSelectedPoint();
            updateDataDisplay();
        });
        
        // 添加表格数据变化监听器，当数据变化时自动更新图表
        displacementDataList.addListener((javafx.collections.ListChangeListener.Change<? extends DeepHorizontalDisplacementData> c) -> {
            // 如果当前显示的是垂直图表，则更新垂直图表
            if (selectedPoint != null && !selectedPoint.isEmpty()) {
                updateVerticalChart();
            } else {
                updateChart();
            }
        });
        
        // 加载测点配置
        loadDeepHorizontalDisplacementPoints();
        
        // 初始化数据
        updateTableWithInitialData();
        
        // 更新测点数量显示
        updatePointCount();
    }

    /**
     * 设置表格列
     */
    @FXML
    private void setupTableColumns() {
        // Depth column
        depthColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getDepth()));
        depthColumn.setCellFactory(column -> new TableCell<DeepHorizontalDisplacementData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // 确保深度显示为负值，表示地下深度
                    double depth = item.doubleValue();
                    // 如果深度为正值，则转为负值
                    if (depth > 0) {
                        depth = -depth;
                    }
                    setText(String.format("%.1f", depth));
                }
            }
        });
        
        // 重新启用排序，并设置自定义比较器，确保按照深度从浅到深排序（负值从大到小）
        dataTableView.setSortPolicy(tableView -> true);
        depthColumn.setComparator((depth1, depth2) -> {
            // 确保转换为负值进行比较
            double d1 = depth1.doubleValue();
            double d2 = depth2.doubleValue();
            
            // 如果是正值，转为负值
            if (d1 > 0) d1 = -d1;
            if (d2 > 0) d2 = -d2;
            
            // 降序排列（从大到小，即从浅到深 -0.5, -1.0, -1.5...）
            return Double.compare(d2, d1);
        });
        
        // 设置初始排序为深度列，从浅到深排序（即从-0.5到更大的负值）
        depthColumn.setSortType(TableColumn.SortType.ASCENDING);
        dataTableView.getSortOrder().add(depthColumn);
        dataTableView.sort();
        
        // Initial value column
        initialValueColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getInitialValue()));
        initialValueColumn.setCellFactory(column -> new TableCell<DeepHorizontalDisplacementData, Number>() {
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
        
        // Previous value column
        previousValueColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getPreviousValue()));
        previousValueColumn.setCellFactory(column -> new TableCell<DeepHorizontalDisplacementData, Number>() {
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
        
        // Current value column
        currentValueColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getCurrentValue()));
        currentValueColumn.setCellFactory(column -> new TableCell<DeepHorizontalDisplacementData, Number>() {
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
        
        // Current change column
        currentChangeColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getCurrentChange()));
        currentChangeColumn.setCellFactory(column -> new TableCell<DeepHorizontalDisplacementData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    // Display with two decimal places
                    setText(String.format("%.2f", item.doubleValue()));
                    
                    // Set color: positive values in red, negative values in green
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
        
        // Cumulative change column
        cumulativeChangeColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getCumulativeChange()));
        cumulativeChangeColumn.setCellFactory(column -> new TableCell<DeepHorizontalDisplacementData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    // Display with two decimal places
                    setText(String.format("%.2f", item.doubleValue()));
                    
                    // Set color: positive values in red, negative values in green
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
        
        // Change rate column
        changeRateColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getChangeRate()));
        changeRateColumn.setCellFactory(column -> new TableCell<DeepHorizontalDisplacementData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    // Display with two decimal places
                    setText(String.format("%.2f", item.doubleValue()));
                    
                    // Set color: positive values in red, negative values in green
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
        
        // Historical cumulative column
        historicalCumulativeColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getHistoricalCumulative()));
        historicalCumulativeColumn.setCellFactory(column -> new TableCell<DeepHorizontalDisplacementData, Number>() {
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
        
        // Set table data source
        dataTableView.setItems(displacementDataList);
        
        // 启用表格排序并设置默认排序列
        dataTableView.getSortOrder().add(depthColumn);
    }
    
    /**
     * 初始化图表
     */
    private void initializeCharts() {
        // 创建旧的水平图表
        initializeOldCharts();
        
        // 创建新的垂直方向变化量图和变化速率图
        initializeVerticalCharts();
        
        // 默认显示位移图表
        displacementChartButton.setSelected(true);
        showDisplacementChart();
    }
    
    /**
     * 初始化旧的水平图表
     */
    private void initializeOldCharts() {
        // 位移图表
        displacementXAxis = new CategoryAxis();
        displacementYAxis = new NumberAxis();
        displacementXAxis.setLabel("测点编号");
        displacementYAxis.setLabel("位移量(mm)");
        
        // 设置Y轴格式为两位小数
        displacementYAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return String.format("%.2f", object.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                return Double.parseDouble(string);
            }
        });
        
        displacementChart = new LineChart<>(displacementXAxis, displacementYAxis);
        displacementChart.setTitle("深部水平位移变化图");
        displacementChart.setCreateSymbols(true); // 显示数据点符号
        displacementChart.setAnimated(false);
        displacementChart.setLegendVisible(true);
        
        // 速率图表
        rateXAxis = new CategoryAxis();
        rateYAxis = new NumberAxis();
        rateXAxis.setLabel("测点编号");
        rateYAxis.setLabel("变化速率(mm/d)");
        
        // 设置Y轴格式为两位小数
        rateYAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return String.format("%.2f", object.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                return Double.parseDouble(string);
            }
        });
        
        rateChart = new LineChart<>(rateXAxis, rateYAxis);
        rateChart.setTitle("深部水平位移变化速率图");
        rateChart.setCreateSymbols(true); // 显示数据点符号
        rateChart.setAnimated(false);
        rateChart.setLegendVisible(true);
    }
    
    /**
     * 初始化垂直方向图表
     */
    private void initializeVerticalCharts() {
        // 垂直方向变化量图表
        verticalDisplacementXAxis = new NumberAxis();
        verticalDisplacementDepthAxis = new NumberAxis();
        verticalDisplacementXAxis.setLabel("位移量(mm)");
        verticalDisplacementDepthAxis.setLabel("深度(m)");
        verticalDisplacementDepthAxis.setAutoRanging(false); // Y轴不自动调整，将在更新图表时根据数据设置
        
        // 设置X轴默认范围，居中在0
        verticalDisplacementXAxis.setAutoRanging(false); // 不自动调整，但在updateVerticalChart中会根据数据动态调整
        verticalDisplacementXAxis.setLowerBound(-50);
        verticalDisplacementXAxis.setUpperBound(50);
        verticalDisplacementXAxis.setTickUnit(20);
        
        // 设置X轴格式为两位小数
        verticalDisplacementXAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return String.format("%.0f", object.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                return Double.parseDouble(string);
            }
        });
        
        // 创建垂直方向变化量图表
        verticalDisplacementChart = new LineChart<>(verticalDisplacementXAxis, verticalDisplacementDepthAxis);
        verticalDisplacementChart.setTitle("深部水平位移变化量-深度关系图");
        verticalDisplacementChart.setCreateSymbols(true);
        verticalDisplacementChart.setAnimated(false);
        verticalDisplacementChart.setLegendVisible(true);
        verticalDisplacementChart.setHorizontalGridLinesVisible(true);
        verticalDisplacementChart.setVerticalGridLinesVisible(true);
        
        // 强制设置固定尺寸为300x600
        verticalDisplacementChart.setPrefWidth(300);
        verticalDisplacementChart.setPrefHeight(600);
        verticalDisplacementChart.setMinWidth(300);
        verticalDisplacementChart.setMaxWidth(300);
        verticalDisplacementChart.setMinHeight(600);
        
        // 初始Y轴设置，确保从上到下为深度递增（从浅到深）
        verticalDisplacementDepthAxis.setUpperBound(-0.5); // 顶部从-0.5米开始
        verticalDisplacementDepthAxis.setLowerBound(-40); // 到-40米深度
        verticalDisplacementDepthAxis.setTickUnit(5);
        
        // 确保Y轴的刻度标签正确显示负值
        verticalDisplacementDepthAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return String.format("%.1f", object.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                return Double.parseDouble(string);
            }
        });
        
        // 设置X轴位置在顶部
        verticalDisplacementChart.setAxisSortingPolicy(LineChart.SortingPolicy.NONE);
        
        // 垂直方向变化速率图表 - 类似设置
        verticalRateXAxis = new NumberAxis();
        verticalRateDepthAxis = new NumberAxis();
        verticalRateXAxis.setLabel("变化速率(mm/d)");
        verticalRateDepthAxis.setLabel("深度(m)");
        verticalRateDepthAxis.setAutoRanging(false);
        
        // 设置X轴默认范围，居中在0
        verticalRateXAxis.setAutoRanging(false);
        verticalRateXAxis.setLowerBound(-2);
        verticalRateXAxis.setUpperBound(2);
        verticalRateXAxis.setTickUnit(0.5);
        
        // 设置X轴格式为一位小数
        verticalRateXAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return String.format("%.1f", object.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                return Double.parseDouble(string);
            }
        });
        
        // 创建垂直方向变化速率图表
        verticalRateChart = new LineChart<>(verticalRateXAxis, verticalRateDepthAxis);
        verticalRateChart.setTitle("深部水平位移变化速率-深度关系图");
        verticalRateChart.setCreateSymbols(true);
        verticalRateChart.setAnimated(false);
        verticalRateChart.setLegendVisible(true);
        verticalRateChart.setHorizontalGridLinesVisible(true);
        verticalRateChart.setVerticalGridLinesVisible(true);
        
        // 同样强制设置固定尺寸
        verticalRateChart.setPrefWidth(300);
        verticalRateChart.setPrefHeight(600);
        verticalRateChart.setMinWidth(300);
        verticalRateChart.setMaxWidth(300);
        verticalRateChart.setMinHeight(600);
        
        // 设置X轴位置在顶部
        verticalRateChart.setAxisSortingPolicy(LineChart.SortingPolicy.NONE);
        
        // 设置Y轴刻度标签格式，确保正确显示负值深度
        verticalRateDepthAxis.setTickLabelFormatter(verticalDisplacementDepthAxis.getTickLabelFormatter());
        
        // 初始Y轴设置，确保从上到下深度递增（浅到深）
        verticalRateDepthAxis.setUpperBound(-0.5); // 顶部从-0.5米开始
        verticalRateDepthAxis.setLowerBound(-40); // 到-40米深度
        verticalRateDepthAxis.setTickUnit(5);
        
        // 设置图表样式 - 减小数据点大小
        String chartStyle = 
                ".chart-line-symbol {" +
                "    -fx-background-radius: 2px;" +  // 减小数据点大小
                "    -fx-padding: 2px;" +           // 减小内边距
                "}" +
                ".chart-series-line {" +
                "    -fx-stroke-width: 2px;" +     // 保持线条粗细
                "}" +
                ".chart-horizontal-grid-lines {" +
                "    -fx-stroke: #dddddd;" +
                "    -fx-stroke-width: 0.5px;" +
                "}" +
                ".chart-vertical-grid-lines {" +
                "    -fx-stroke: #dddddd;" +
                "    -fx-stroke-width: 0.5px;" +
                "}" +
                // 强调中心线 (X=0处)
                ".chart-vertical-zero-line {" +
                "    -fx-stroke: #000000;" +
                "    -fx-stroke-width: 2px;" +
                "}" +
                // 轴线样式
                ".axis {" +
                "    -fx-font-size: 12px;" +
                "    -fx-tick-label-font-size: 12px;" +
                "}" +
                ".axis-label {" +
                "    -fx-font-size: 14px;" +
                "}" +
                ".chart-title {" +
                "    -fx-font-size: 16px;" +
                "    -fx-font-weight: bold;" +
                "}";
        
        verticalDisplacementChart.setStyle(chartStyle);
        verticalRateChart.setStyle(chartStyle);
        displacementChart.setStyle(chartStyle);
        rateChart.setStyle(chartStyle);
    }
    
    /**
     * 显示位移图表
     */
    private void showDisplacementChart() {
        if (selectedPoint != null && !selectedPoint.isEmpty()) {
            // 使用垂直方向图表
            // 确保尺寸设置正确
            verticalDisplacementChart.setPrefWidth(300);
            verticalDisplacementChart.setMinWidth(300);
            verticalDisplacementChart.setMaxWidth(300);
            verticalDisplacementChart.setPrefHeight(600);
            verticalDisplacementChart.setMinHeight(600);
            
            chartContainer.setCenter(verticalDisplacementChart);
            updateVerticalChart();
        } else {
            // 使用旧的水平图表
            chartContainer.setCenter(displacementChart);
            updateChart();
        }
    }
    
    /**
     * 显示速率图表
     */
    private void showRateChart() {
        if (selectedPoint != null && !selectedPoint.isEmpty()) {
            // 使用垂直方向图表
            // 确保尺寸设置正确
            verticalRateChart.setPrefWidth(300);
            verticalRateChart.setMinWidth(300);
            verticalRateChart.setMaxWidth(300);
            verticalRateChart.setPrefHeight(600);
            verticalRateChart.setMinHeight(600);
            
            chartContainer.setCenter(verticalRateChart);
            updateVerticalChart();
        } else {
            // 使用旧的水平图表
            chartContainer.setCenter(rateChart);
            updateChart();
        }
    }
    
    /**
     * 更新图表显示
     */
    private void updateChart() {
        if (displacementDataList.isEmpty()) {
            return;
        }
        
        // 清除现有数据
        displacementChart.getData().clear();
        rateChart.getData().clear();
        
        // 创建数据系列
        XYChart.Series<String, Number> cumulativeChangeSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> currentChangeSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> rateSeries = new XYChart.Series<>();
        
        cumulativeChangeSeries.setName("累计变化量(mm)");
        currentChangeSeries.setName("本次变化量(mm)");
        rateSeries.setName("变化速率(mm/d)");
        
        // 添加数据点
        for (DeepHorizontalDisplacementData data : displacementDataList) {
            String pointId = data.getPointCode();
            
            // 累计变化量
            cumulativeChangeSeries.getData().add(new XYChart.Data<>(pointId, data.getCumulativeChange()));
            
            // 本次变化量
            currentChangeSeries.getData().add(new XYChart.Data<>(pointId, data.getCurrentChange()));
            
            // 变化速率
            rateSeries.getData().add(new XYChart.Data<>(pointId, data.getChangeRate()));
        }
        
        // 将数据添加到图表
        displacementChart.getData().add(cumulativeChangeSeries);
        displacementChart.getData().add(currentChangeSeries);
        rateChart.getData().add(rateSeries);
        
        // 应用自定义样式
        applyChartStyling();
    }

    /**
     * 应用图表样式
     */
    private void applyChartStyling() {
        // 为累计变化量线条设置样式（蓝色实线）
        if (!displacementChart.getData().isEmpty()) {
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
        
        // 为本次变化量线条设置样式（红色虚线）
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
        
        // 为变化速率线条设置样式（绿色实线）
        if (!rateChart.getData().isEmpty()) {
            XYChart.Series<String, Number> rateSeries = rateChart.getData().get(0);
            String rateStyle = "-fx-stroke: #008800; -fx-stroke-width: 2px;";
            
            rateSeries.getNode().setStyle(rateStyle);
            
            // 设置数据点样式
            for (XYChart.Data<String, Number> data : rateSeries.getData()) {
                if (data.getNode() != null) {
                    data.getNode().setStyle("-fx-background-color: #008800, white;");
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
        
        MenuItem customRateItem = new MenuItem("设置变化速率计算天数");
        customRateItem.setOnAction(e -> showSetCustomDaysDialog());
        
        contextMenu.getItems().addAll(refreshItem, exportItem, customRateItem);
        dataTableView.setContextMenu(contextMenu);
    }
    
    /**
     * 使用初始数据更新表格
     */
    private void updateTableWithInitialData() {
        displacementDataList.clear();
        
        // 如果有已配置的测点，为每个测点创建初始数据
        if (!configuredPoints.isEmpty()) {
            for (DeepHorizontalDisplacementPoint point : configuredPoints) {
                // 只有当测点有测量数据时才创建
                if (!point.getMeasurements().isEmpty()) {
                    // 获取第一个测量点的数据
                    DeepHorizontalDisplacementPoint.Measurement firstMeasurement = point.getMeasurements().get(0);
                    
                    DeepHorizontalDisplacementData data = new DeepHorizontalDisplacementData();
                    data.setPointCode(point.getPointId());
                    data.setDepth(firstMeasurement.getDepth());
                    data.setInitialValue(firstMeasurement.getInitialValue());
                    data.setPreviousValue(firstMeasurement.getInitialValue());
                    data.setCurrentValue(firstMeasurement.getInitialValue());
                    data.setMeasurementDate(LocalDate.now());
                    data.setHistoricalCumulative(firstMeasurement.getHistoricalCumulative());
                    
                    // 计算衍生值
                    data.calculateDerivedValues();
                    
                    displacementDataList.add(data);
                }
            }
        }
        
        // 更新图表
        updateChart();
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
     * 加载深部水平位移测点配置
     */
    private void loadDeepHorizontalDisplacementPoints() {
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
     * 处理导出数据按钮点击事件
     */
    @FXML
    private void handleExportButtonAction(ActionEvent event) {
        if (displacementDataList.isEmpty()) {
            AlertUtil.showInformation("导出", "没有数据可供导出。");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出数据");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel文件", "*.xlsx"));
        fileChooser.setInitialFileName("深部水平位移数据.xlsx");
        
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                // TODO: 实现导出逻辑
                AlertUtil.showInformation("导出成功", "数据已导出到: " + file.getAbsolutePath());
            } catch (Exception e) {
                AlertUtil.showError("导出错误", "导出数据时出错: " + e.getMessage());
            }
        }
    }

    /**
     * 处理测点设置按钮点击事件
     */
    @FXML
    private void handleSettingsButtonAction(ActionEvent event) {
        try {
            // 加载设置对话框FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/DeepHorizontalDisplacementPointSettingsDialog.fxml"));
            Parent root = loader.load();
            
            // 获取控制器
            DeepHorizontalDisplacementPointSettingsController controller = loader.getController();
            
            // 创建对话框
            Stage dialogStage = new Stage();
            dialogStage.setTitle("深部水平位移测点设置");
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
            if (controller.isSaveClicked()) {
                List<DeepHorizontalDisplacementPoint> updatedPoints = controller.getPoints();
                configuredPoints = updatedPoints;
                
                // 更新测点计数
                updatePointCount();
                
                // 更新表格数据
                updateTableWithInitialData();
            }
            
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
                    
                    // 存储所有测点数据 <测点ID, <深度, 测值>>
                    Map<String, Map<Double, Double>> pointDepthValueMap = new HashMap<>();
                    
                    // 获取已配置的测点名称列表
                    List<String> configuredPointIds = configuredPoints.stream()
                        .map(DeepHorizontalDisplacementPoint::getPointId)
                        .collect(Collectors.toList());
                    
                    int validSheetCount = 0;
                    int totalDataCount = 0;
                    
                    // 遍历所有sheet，查找与配置测点名称匹配的sheet
                    for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                        Sheet sheet = workbook.getSheetAt(i);
                        if (sheet == null) continue;
                        
                        String sheetName = workbook.getSheetName(i);
                        
                        // 检查sheet名称是否与任何配置的测点名称匹配
                        boolean matchFound = false;
                        String matchedPointId = null;
                        
                        for (String pointId : configuredPointIds) {
                            if (sheetName.contains(pointId)) {
                                matchFound = true;
                                matchedPointId = pointId;
                                break;
                            }
                        }
                        
                        if (!matchFound) {
                            // 如果没有匹配的测点名称，跳过此sheet
                            continue;
                        }
                        
                        // 获取第一个非空行，判断表头
                        Row headerRow = null;
                        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                            if (sheet.getRow(r) != null && sheet.getRow(r).getCell(0) != null) {
                                headerRow = sheet.getRow(r);
                                break;
                            }
                        }
                        
                        if (headerRow == null) continue;
                        
                        // 获取表头内容
                        String firstColumnHeader = getCellValueAsString(headerRow.getCell(0)).trim();
                        String secondColumnHeader = getCellValueAsString(headerRow.getCell(1)).trim();
                        
                        // 判断是否是深度和测值列
                        boolean isDepthValueSheet = firstColumnHeader.contains("深度") && secondColumnHeader.contains("测值");
                        
                        if (isDepthValueSheet) {
                            // 处理深部水平位移数据
                            Map<Double, Double> depthValueMap = new HashMap<>();
                            int dataCount = 0;
                            
                            // 遍历数据行
                            for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                                Row dataRow = sheet.getRow(rowIndex);
                                if (dataRow == null) continue;
                                
                                Cell depthCell = dataRow.getCell(0);
                                Cell valueCell = dataRow.getCell(1);
                                
                                if (depthCell == null || valueCell == null) continue;
                                
                                try {
                                    double depth = getNumericCellValue(depthCell);
                                    double value = getNumericCellValue(valueCell);
                                    
                                    depthValueMap.put(depth, value);
                                    dataCount++;
                                } catch (NumberFormatException e) {
                                    // 忽略无法解析为数字的行
                                    continue;
                                }
                            }
                            
                            if (dataCount > 0) {
                                pointDepthValueMap.put(matchedPointId, depthValueMap);
                                validSheetCount++;
                                totalDataCount += dataCount;
                            }
                        }
                    }
                    
                    if (validSheetCount == 0 || totalDataCount == 0) {
                        AlertUtil.showWarning("上传失败", "未找到有效的深部水平位移数据。请确保Excel中的工作表名称与测点名称匹配，并且表格包含深度和测值列。");
                        return;
                    }
                    
                    // 处理数据
                    LocalDate measureDate = datePicker.getValue();
                    if (measureDate == null) {
                        measureDate = LocalDate.now();
                        datePicker.setValue(measureDate);
                    }
                    
                    // 创建数据块时间戳
                    LocalDateTime dataBlockTimestamp = LocalDateTime.now();
                    
                    // 将测点ID添加到下拉框中
                    Set<String> allPointIds = new HashSet<>(pointSelector.getItems());
                    
                    // 处理每个测点的数据
                    boolean hasValidData = false;
                    List<DeepHorizontalDisplacementData> allPointsDataBlock = new ArrayList<>();
                    
                    for (String pointId : pointDepthValueMap.keySet()) {
                        Map<Double, Double> depthValueMap = pointDepthValueMap.get(pointId);
                        
                        List<DeepHorizontalDisplacementData> pointDataList = processPointDepthValueData(
                                pointId, depthValueMap, measureDate);
                        
                        if (pointDataList != null && !pointDataList.isEmpty()) {
                            hasValidData = true;
                            
                            // 添加到所有测点的数据块
                            allPointsDataBlock.addAll(pointDataList);
                            
                            // 添加测点到下拉框
                            if (!allPointIds.contains(pointId)) {
                                pointSelector.getItems().add(pointId);
                                allPointIds.add(pointId);
                            }
                            
                            // 添加到按测点分组的数据块映射
                            pointDataBlocksMap.computeIfAbsent(pointId, k -> new HashMap<>())
                                    .put(dataBlockTimestamp, pointDataList);
                        }
                    }
                    
                    // 添加到全局数据块映射，确保所有测点的数据都保存在一起
                    if (!allPointsDataBlock.isEmpty()) {
                        dataBlocksMap.put(dataBlockTimestamp, allPointsDataBlock);
                        System.out.println("已添加 " + allPointsDataBlock.size() +
                                " 条数据到全局数据块，时间戳: " + dataBlockTimestamp);
                    }
                    
                    if (!hasValidData) {
                        AlertUtil.showWarning("上传警告", "没有匹配的测点数据，请先配置测点。");
                        return;
                    }
                    
                    // 添加数据块到UI，使其在界面上显示
                    if (hasValidData) {
                        String blockLabel = measureDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) +
                                " " + dataBlockTimestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                        addDataBlock(blockLabel, dataBlockTimestamp);
                        
                        // 默认选中新添加的数据块
                        CheckBox checkBox = dataBlockCheckBoxMap.get(dataBlockTimestamp);
                        if (checkBox != null) {
                            checkBox.setSelected(true);
                            if (!selectedDataBlocks.contains(dataBlockTimestamp)) {
                                selectedDataBlocks.add(dataBlockTimestamp);
                            }
                        }
                    }
                    
                    // 如果下拉框为空，自动选择第一个测点
                    if (selectedPoint == null && !pointSelector.getItems().isEmpty()) {
                        pointSelector.getSelectionModel().selectFirst();
                    } else if (selectedPoint != null) {
                        // 如果已选中测点，刷新数据块显示
                        updateDataBlocksForSelectedPoint();
                    }
                    
                    // 更新数据显示
                    updateDataDisplay();
                    
                    // 显示成功消息
                    AlertUtil.showInformation("上传成功", "成功从 " + validSheetCount +
                            " 个工作表中导入 " + totalDataCount + " 条深部水平位移数据。");
                }
            } catch (Exception e) {
                AlertUtil.showError("数据处理错误", "处理Excel数据时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 处理测点深度测值数据并生成深部水平位移数据列表
     */
    private List<DeepHorizontalDisplacementData> processPointDepthValueData(
            String pointId, Map<Double, Double> depthValueMap, LocalDate measureDate) {
        
        List<DeepHorizontalDisplacementData> result = new ArrayList<>();
        
        // 构建所有点的历史数据映射
        Map<String, Map<LocalDate, DeepHorizontalDisplacementData>> allPointDataMap = getAllPointDataMap();
        
        // 尝试查找匹配的测点配置
        DeepHorizontalDisplacementPoint matchedPoint = null;
        for (DeepHorizontalDisplacementPoint point : configuredPoints) {
            if (point.getPointId().equals(pointId)) {
                matchedPoint = point;
                break;
            }
        }
        
        // 如果没有找到测点配置，自动创建一个
        if (matchedPoint == null) {
            matchedPoint = new DeepHorizontalDisplacementPoint(pointId, "");
            
            // 根据深度值创建测量点
            for (Double depth : depthValueMap.keySet()) {
                // 确保深度值为负值（表示地下深度）
                double negativeDepth = depth;
                if (negativeDepth > 0) {
                    negativeDepth = -negativeDepth;
                }
                
                double value = depthValueMap.get(depth);
                DeepHorizontalDisplacementPoint.Measurement measurement = 
                        new DeepHorizontalDisplacementPoint.Measurement(negativeDepth, value, 5.0, 10.0, 0.0);
                matchedPoint.addMeasurement(measurement);
            }
            
            // 添加到配置列表
            configuredPoints.add(matchedPoint);
            
            // 更新测点计数
            updatePointCount();
        }
        
        // 遍历深度测值数据
        for (Map.Entry<Double, Double> entry : depthValueMap.entrySet()) {
            double originalDepth = entry.getKey();
            double currentValue = entry.getValue();
            
            // 确保深度值为负值（表示地下深度）
            double depth = originalDepth;
            if (depth > 0) {
                depth = -depth;
            }
            
            // 查找对应深度的测量配置
            DeepHorizontalDisplacementPoint.Measurement depthMeasurement = null;
            for (DeepHorizontalDisplacementPoint.Measurement m : matchedPoint.getMeasurements()) {
                // 使用负值深度进行比较
                double measurementDepth = m.getDepth();
                if (measurementDepth > 0) {
                    measurementDepth = -measurementDepth;
                }
                
                if (Math.abs(measurementDepth - depth) < 0.001) {
                    depthMeasurement = m;
                    break;
                }
            }
            
            // 如果没有找到对应深度的配置，跳过
            if (depthMeasurement == null) {
                continue;
            }
            
            // 获取初始值
            double initialValue = depthMeasurement.getInitialValue();
            
            // 查找前期测值
            double previousValue = initialValue;  // 默认与初始值相同
            LocalDate previousDate = null;
            
            // 从历史数据中查找最近一次的前期数据
            Map<LocalDate, DeepHorizontalDisplacementData> pointHistory = allPointDataMap.get(pointId);
            if (pointHistory != null && !pointHistory.isEmpty()) {
                // 查找该深度的历史数据
                for (Map.Entry<LocalDate, DeepHorizontalDisplacementData> historyEntry : pointHistory.entrySet()) {
                    DeepHorizontalDisplacementData historyData = historyEntry.getValue();
                    
                    // 检查是否是同一深度的数据点
                    double historyDepth = historyData.getDepth();
                    if (historyDepth > 0) {
                        historyDepth = -historyDepth; // 确保用负值比较
                    }
                    
                    if (Math.abs(historyDepth - depth) < 0.001) {
                        LocalDate historyDate = historyEntry.getKey();
                        // 找出测量日期之前的最新数据
                        if (historyDate.isBefore(measureDate) && (previousDate == null || historyDate.isAfter(previousDate))) {
                            previousDate = historyDate;
                            previousValue = historyData.getCurrentValue();
                        }
                    }
                }
            }
            
            // 创建深部水平位移数据对象
            DeepHorizontalDisplacementData displacementData = new DeepHorizontalDisplacementData();
            displacementData.setPointCode(pointId);
            displacementData.setInitialValue(initialValue);
            displacementData.setPreviousValue(previousValue);
            displacementData.setCurrentValue(currentValue);
            displacementData.setMeasurementDate(measureDate);
            displacementData.setMileage(matchedPoint.getMileage());
            displacementData.setDepth(depth); // 使用负值深度
            displacementData.setHistoricalCumulative(depthMeasurement.getHistoricalCumulative());
            
            // 计算衍生值
            if (previousDate != null) {
                displacementData.calculateDerivedValues(previousDate, measureDate, customDaysForRateCalculation);
            } else {
                displacementData.calculateDerivedValues();
            }
            
            // 添加到结果列表
            result.add(displacementData);
        }
        
        return result;
    }

    /**
     * 获取所有点的历史数据映射
     * @return 所有点的历史数据映射 <测点编号, <测量日期, 数据>>
     */
    private Map<String, Map<LocalDate, DeepHorizontalDisplacementData>> getAllPointDataMap() {
        Map<String, Map<LocalDate, DeepHorizontalDisplacementData>> result = new HashMap<>();
        
        // 遍历按测点分组的所有数据块
        for (String pointId : pointDataBlocksMap.keySet()) {
            Map<LocalDateTime, List<DeepHorizontalDisplacementData>> blocks = pointDataBlocksMap.get(pointId);
            
            // 获取或创建该测点的数据映射
            Map<LocalDate, DeepHorizontalDisplacementData> pointDataMap = 
                    result.computeIfAbsent(pointId, k -> new HashMap<>());
            
            // 遍历该测点的所有数据块
            for (List<DeepHorizontalDisplacementData> dataList : blocks.values()) {
                for (DeepHorizontalDisplacementData data : dataList) {
                    LocalDate date = data.getMeasurementDate();
                    
                    // 存储最新的一条同一测点同一深度的数据
                    DeepHorizontalDisplacementData existingData = pointDataMap.get(date);
                    if (existingData == null || 
                            Math.abs(existingData.getDepth() - data.getDepth()) < 0.001) {
                        pointDataMap.put(date, data);
                    }
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
    
    /**
     * 添加数据块到UI
     */
    private void addDataBlock(String blockLabel, LocalDateTime dateTime) {
        // 创建数据块HBox容器
        HBox dataBlockBox = new HBox(5);
        dataBlockBox.getStyleClass().addAll("data-block", "deep-horizontal-data-block");
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
        
        // 设置工具提示，显示完整的日期时间
        Tooltip tooltip = new Tooltip("上传时间: " + dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        Tooltip.install(dataBlockBox, tooltip);
        
        // 选中新添加的数据块
        checkBox.setSelected(true);
        selectedDataBlocks.add(dateTime); // 确保添加到选中列表
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
        // 如果没有选择任何数据块或没有选中测点，清空表格
        if (selectedDataBlocks.isEmpty() || (selectedPoint == null && !pointSelector.getItems().isEmpty())) {
            displacementDataList.clear();
            return;
        }

        // 清空当前表格数据
        displacementDataList.clear();

        // 获取所有数据块的所有数据
        List<DeepHorizontalDisplacementData> allData = new ArrayList<>();
        
        for (LocalDateTime timestamp : selectedDataBlocks) {
            List<DeepHorizontalDisplacementData> dataBlock = dataBlocksMap.get(timestamp);
            if (dataBlock != null) {
                allData.addAll(dataBlock);
            }
        }
        
        // 如果有选中的测点，只显示该测点的数据
        if (selectedPoint != null && !selectedPoint.isEmpty()) {
            // 过滤出当前选中测点的数据
            List<DeepHorizontalDisplacementData> pointData = allData.stream()
                .filter(data -> data.getPointCode().equals(selectedPoint))
                .collect(Collectors.toList());
            
            // 按深度排序 - 从浅到深（负值从大到小）
            pointData.sort((data1, data2) -> {
                double depth1 = data1.getDepth();
                double depth2 = data2.getDepth();
                
                // 确保深度为负值
                if (depth1 > 0) depth1 = -depth1;
                if (depth2 > 0) depth2 = -depth2;
                
                // 降序排列（从大到小，即从浅到深 -0.5, -1.0, -1.5...）
                return Double.compare(depth2, depth1);
            });
            
            displacementDataList.addAll(pointData);
            
            // 更新垂直图表
            updateVerticalChart();
        } else {
            // 如果没有选中测点但有数据块，显示最后一个数据块的所有测点数据
            LocalDateTime latestTimestamp = selectedDataBlocks.get(selectedDataBlocks.size() - 1);
            List<DeepHorizontalDisplacementData> latestData = dataBlocksMap.get(latestTimestamp);
            
            if (latestData != null) {
                displacementDataList.addAll(latestData);
            }
        }
        
        // 更新上传日期标签
        if (!selectedDataBlocks.isEmpty()) {
            LocalDateTime latestTimestamp = Collections.max(selectedDataBlocks);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            uploadDateLabel.setText(latestTimestamp.format(formatter));
        }
        
        // 更新水平图表
        updateChart();
        
        // 如果当前显示的是位移图表，确保更新后的图表正确显示
        if (displacementChartButton.isSelected()) {
            showDisplacementChart();
        } else {
            showRateChart();
        }
    }
    
    /**
     * 获取数据存储对象
     */
    public DeepHorizontalDisplacementDataStorage getDeepHorizontalDisplacementDataStorage() {
        DeepHorizontalDisplacementDataStorage storage = new DeepHorizontalDisplacementDataStorage();
        
        // 设置测点列表
        storage.setPoints(configuredPoints);
        
        // 设置自定义速率计算天数
        storage.setCustomDaysForRateCalculation(customDaysForRateCalculation);
        
        // 设置选中的数据块
        storage.setSelectedDataBlocks(selectedDataBlocks);
        
        // 确保用于界面显示的数据块检查框与UI匹配
        if (selectedPoint != null && !selectedPoint.isEmpty()) {
            // 记录当前界面上显示的数据块
            Map<LocalDateTime, Boolean> visibleDataBlocks = new HashMap<>();
            for (Map.Entry<LocalDateTime, CheckBox> entry : dataBlockCheckBoxMap.entrySet()) {
                visibleDataBlocks.put(entry.getKey(), entry.getValue().isSelected());
            }
        }
        
        // 添加所有测点的所有数据块，而不仅限于当前选中测点的数据
        System.out.println("准备保存数据块: 总数=" + dataBlocksMap.size());
        
        for (Map.Entry<LocalDateTime, List<DeepHorizontalDisplacementData>> entry : dataBlocksMap.entrySet()) {
            LocalDateTime timestamp = entry.getKey();
            List<DeepHorizontalDisplacementData> dataBlock = entry.getValue();
            
            // 创建描述，使用第一条数据的测量日期
            String description = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            if (!dataBlock.isEmpty()) {
                LocalDate measureDate = dataBlock.get(0).getMeasurementDate();
                description = measureDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) +
                        " " + timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                
                // 加入测点信息到描述
                String pointCode = dataBlock.get(0).getPointCode();
                if (pointCode != null && !pointCode.isEmpty()) {
                    description = "测点:" + pointCode + " " + description;
                }
            }
            
            storage.addDataBlock(timestamp, dataBlock, description);
        }
        
        System.out.println("已保存 " + storage.getDataBlockTimestamps().size() + " 个数据块到存储对象");
        
        return storage;
    }
    
    /**
     * 从深部水平位移数据存储对象加载数据
     * @param storage 数据存储对象
     */
    public void loadFromDeepHorizontalDisplacementDataStorage(DeepHorizontalDisplacementDataStorage storage) {
        if (storage == null) {
            return;
        }
        
        // 清空现有数据
        configuredPoints.clear();
        dataBlocksMap.clear();
        dataBlockCheckBoxMap.clear();
        dataBlocksFlowPane.getChildren().clear();
        selectedDataBlocks.clear();
        pointDataBlocksMap.clear();
        pointSelector.getItems().clear();
        
        // 加载测点配置
        configuredPoints.addAll(storage.getPoints());
        
        // 加载数据块
        Set<String> uniquePointIds = new HashSet<>();
        
        for (LocalDateTime timestamp : storage.getDataBlockTimestamps()) {
            List<DeepHorizontalDisplacementData> dataList = storage.getDataBlock(timestamp);
            String description = storage.getDataBlockDescription(timestamp);
            
            // 将数据块添加到全局映射中
            dataBlocksMap.put(timestamp, dataList);
            
            // 按测点分组数据
            Map<String, List<DeepHorizontalDisplacementData>> pointDataMap = new HashMap<>();
            for (DeepHorizontalDisplacementData data : dataList) {
                String pointId = data.getPointCode();
                uniquePointIds.add(pointId);
                pointDataMap.computeIfAbsent(pointId, k -> new ArrayList<>()).add(data);
            }
            
            // 将分组后的数据添加到按测点分组的数据块映射中
            for (Map.Entry<String, List<DeepHorizontalDisplacementData>> entry : pointDataMap.entrySet()) {
                String pointId = entry.getKey();
                List<DeepHorizontalDisplacementData> pointDataList = entry.getValue();
                
                pointDataBlocksMap.computeIfAbsent(pointId, k -> new HashMap<>())
                        .put(timestamp, pointDataList);
            }
        }
        
        // 添加测点到下拉框
        pointSelector.getItems().addAll(uniquePointIds);
        
        // 设置选定的数据块
        List<LocalDateTime> selected = storage.getSelectedDataBlocks();
        if (selected != null && !selected.isEmpty()) {
            selectedDataBlocks.addAll(selected);
        }
        
        // 设置自定义天数
        customDaysForRateCalculation = storage.getCustomDaysForRateCalculation();
        
        // 更新测点计数
        updatePointCount();
        
        // 如果有测点，选择第一个
        if (!pointSelector.getItems().isEmpty()) {
            pointSelector.getSelectionModel().selectFirst();
            selectedPoint = pointSelector.getValue();
            // 自动更新数据块显示，这会触发数据显示
            updateDataBlocksForSelectedPoint();
        } else {
            // 如果没有测点，清空表格
            displacementDataList.clear();
        }
    }
    
    /**
     * 从测量记录加载数据
     * @param records 测量记录列表
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
        pointDataBlocksMap.clear();
        pointSelector.getItems().clear();
        
        // 按测点和测量日期分组记录
        Map<String, Map<LocalDate, Map<Double, MeasurementRecord>>> recordsByPointAndDate = new HashMap<>();
        
        // 识别深部水平位移测点（字母-数字格式）
        Pattern pointPattern = Pattern.compile("[A-Za-z]-\\d+");
        
        for (MeasurementRecord record : records) {
            if (record.getMeasureTime() != null) {
                String recordId = record.getId();
                String pointId = null;
                double depth = 0.0;
                
                // 从记录ID提取测点编号和深度
                if (recordId.contains("_")) {
                    String[] parts = recordId.split("_");
                    if (parts.length >= 2) {
                        pointId = parts[0];
                        try {
                            // 尝试将第二部分解析为深度
                            depth = Double.parseDouble(parts[1]);
                        } catch (NumberFormatException e) {
                            // 忽略无法解析的深度
                            continue;
                        }
                    }
                } else {
                    // 直接尝试使用ID作为测点编号
                    pointId = recordId;
                }
                
                // 验证是否为深部水平位移测点
                if (pointId != null && pointPattern.matcher(pointId).find()) {
                    LocalDate date = record.getMeasureTime().toLocalDate();
                    
                    // 按测点、日期和深度存储
                    recordsByPointAndDate
                        .computeIfAbsent(pointId, k -> new HashMap<>())
                        .computeIfAbsent(date, k -> new HashMap<>())
                        .put(depth, record);
                }
            }
        }
        
        // 处理每个测点的记录
        for (String pointId : recordsByPointAndDate.keySet()) {
            Map<LocalDate, Map<Double, MeasurementRecord>> pointDates = recordsByPointAndDate.get(pointId);
            
            // 查找或创建测点配置
            DeepHorizontalDisplacementPoint point = null;
            for (DeepHorizontalDisplacementPoint p : configuredPoints) {
                if (p.getPointId().equals(pointId)) {
                    point = p;
                    break;
                }
            }
            
            if (point == null) {
                // 创建新测点配置
                point = new DeepHorizontalDisplacementPoint(pointId, "");
                configuredPoints.add(point);
            }
            
            // 处理每个日期的数据
            for (Map.Entry<LocalDate, Map<Double, MeasurementRecord>> dateEntry : pointDates.entrySet()) {
                LocalDate date = dateEntry.getKey();
                Map<Double, MeasurementRecord> depthRecords = dateEntry.getValue();
                
                // 创建数据块
                LocalDateTime blockTimestamp = LocalDateTime.of(date, LocalTime.NOON);
                List<DeepHorizontalDisplacementData> dataBlock = new ArrayList<>();
                
                for (Map.Entry<Double, MeasurementRecord> depthEntry : depthRecords.entrySet()) {
                    double depth = depthEntry.getKey();
                    MeasurementRecord record = depthEntry.getValue();
                    
                    // 查找或创建测量点配置
                    DeepHorizontalDisplacementPoint.Measurement measurement = null;
                    for (DeepHorizontalDisplacementPoint.Measurement m : point.getMeasurements()) {
                        if (Math.abs(m.getDepth() - depth) < 0.001) {
                            measurement = m;
                            break;
                        }
                    }
                    
                    if (measurement == null) {
                        // 创建新测量点配置
                        measurement = new DeepHorizontalDisplacementPoint.Measurement(
                                depth, record.getValue(), 5.0, 10.0, 0.0);
                        point.addMeasurement(measurement);
                    }
                    
                    // 创建数据
                    DeepHorizontalDisplacementData data = new DeepHorizontalDisplacementData();
                    data.setPointCode(pointId);
                    data.setDepth(depth);
                    data.setInitialValue(measurement.getInitialValue());
                    data.setPreviousValue(measurement.getInitialValue()); // 默认与初始值相同
                    data.setCurrentValue(record.getValue());
                    data.setMeasurementDate(date);
                    data.setMileage(point.getMileage());
                    data.setHistoricalCumulative(measurement.getHistoricalCumulative());
                    
                    // 计算变化量
                    data.calculateDerivedValues();
                    
                    dataBlock.add(data);
                }
                
                if (!dataBlock.isEmpty()) {
                    // 添加到全局数据块映射
                    dataBlocksMap.put(blockTimestamp, dataBlock);
                    
                    // 添加到按测点分组的数据块映射
                    pointDataBlocksMap.computeIfAbsent(pointId, k -> new HashMap<>())
                            .put(blockTimestamp, dataBlock);
                    
                    // 自动添加到测点下拉框
                    if (!pointSelector.getItems().contains(pointId)) {
                        pointSelector.getItems().add(pointId);
                    }
                }
            }
        }
        
        // 更新测点计数
        updatePointCount();
        
        // 如果有测点，选择第一个
        if (!pointSelector.getItems().isEmpty()) {
            pointSelector.getSelectionModel().selectFirst();
            selectedPoint = pointSelector.getValue();
            
            // 自动更新数据块显示
            updateDataBlocksForSelectedPoint();
            
            // 显示成功消息
            AlertUtil.showInformation("数据加载成功", 
                String.format("已加载 %d 个深部水平位移测点的数据。", pointSelector.getItems().size()));
        } else {
            // 清空表格
            displacementDataList.clear();
            
            if (!dataBlocksMap.isEmpty()) {
                AlertUtil.showInformation("数据加载成功", 
                    "已加载深部水平位移数据，但未找到有效的测点信息。");
            }
        }
    }
    
    /**
     * 判断数据是否变化，需要保存
     * @return 如果有数据块，表示数据已变化
     */
    public boolean hasDataChanged() {
        return !dataBlocksMap.isEmpty();
    }
    
    /**
     * 获取用于保存的测量记录列表
     */
    public List<MeasurementRecord> getMeasurementRecordsForSaving() {
        List<MeasurementRecord> records = new ArrayList<>();
        
        // 遍历所有数据块
        for (Map.Entry<LocalDateTime, List<DeepHorizontalDisplacementData>> entry : dataBlocksMap.entrySet()) {
            for (DeepHorizontalDisplacementData data : entry.getValue()) {
                MeasurementRecord record = new MeasurementRecord();
                record.setId(data.getPointCode() + "_" + data.getMeasurementDate().toString());
                record.setValue(data.getCurrentValue());
                record.setMeasureTime(LocalDateTime.of(data.getMeasurementDate(), LocalTime.NOON));
                record.setUnit("mm");
                records.add(record);
            }
        }
        
        return records;
    }

    /**
     * 更新选中测点的数据块显示
     */
    private void updateDataBlocksForSelectedPoint() {
        // 清空现有数据块显示
        dataBlocksFlowPane.getChildren().clear();
        dataBlockCheckBoxMap.clear();
        selectedDataBlocks.clear();
        
        if (selectedPoint == null || selectedPoint.isEmpty()) {
            updateDataDisplay();
            return;
        }
        
        // 获取选中测点的数据块
        Map<LocalDateTime, List<DeepHorizontalDisplacementData>> pointBlocks = 
                pointDataBlocksMap.getOrDefault(selectedPoint, new HashMap<>());
        
        // 如果没有数据块，直接返回
        if (pointBlocks.isEmpty()) {
            updateDataDisplay();
            return;
        }
        
        // 添加数据块到界面
        for (Map.Entry<LocalDateTime, List<DeepHorizontalDisplacementData>> entry : pointBlocks.entrySet()) {
            LocalDateTime timestamp = entry.getKey();
            
            // 为数据块创建标签，只显示日期和时间
            String blockLabel = entry.getValue().get(0).getMeasurementDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + 
                    " " + timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            
            addDataBlock(blockLabel, timestamp);
        }
        
        // 如果只有一个数据块，默认选中
        if (pointBlocks.size() == 1) {
            LocalDateTime singleTimestamp = pointBlocks.keySet().iterator().next();
            if (!selectedDataBlocks.contains(singleTimestamp)) {
                selectedDataBlocks.add(singleTimestamp);
                CheckBox checkBox = dataBlockCheckBoxMap.get(singleTimestamp);
                if (checkBox != null) {
                    checkBox.setSelected(true);
                }
            }
        }
        
        // 更新数据显示
        updateDataDisplay();
    }

    /**
     * 更新垂直方向图表
     */
    private void updateVerticalChart() {
        if (displacementDataList.isEmpty() || selectedPoint == null || selectedPoint.isEmpty()) {
            return;
        }
        
        // 确保图表尺寸设置正确
        verticalDisplacementChart.setPrefWidth(300);
        verticalDisplacementChart.setMinWidth(300);
        verticalDisplacementChart.setMaxWidth(300);
        verticalDisplacementChart.setPrefHeight(600);
        verticalDisplacementChart.setMinHeight(600);
        
        verticalRateChart.setPrefWidth(300);
        verticalRateChart.setMinWidth(300);
        verticalRateChart.setMaxWidth(300);
        verticalRateChart.setPrefHeight(600);
        verticalRateChart.setMinHeight(600);
        
        // 清除现有数据
        verticalDisplacementChart.getData().clear();
        verticalRateChart.getData().clear();
        
        // 创建数据系列
        XYChart.Series<Number, Number> cumulativeChangeSeries = new XYChart.Series<>();
        XYChart.Series<Number, Number> currentChangeSeries = new XYChart.Series<>();
        XYChart.Series<Number, Number> rateSeries = new XYChart.Series<>();
        
        cumulativeChangeSeries.setName("累计变化量(mm)");
        currentChangeSeries.setName("本次变化量(mm)");
        rateSeries.setName("变化速率(mm/d)");
        
        // 确定最大深度和位移值范围，用于设置轴范围
        double minDepth = -0.5; // 默认最小深度为-0.5米
        double maxDepth = -40.0; // 默认最大深度为-40米
        double minDisplacement = 0;
        double maxDisplacement = 0;
        double minRate = 0;
        double maxRate = 0;
        
        // 将数据列表按深度排序 - 从浅到深（负值从大到小）
        List<DeepHorizontalDisplacementData> sortedDataList = new ArrayList<>(displacementDataList);
        sortedDataList.sort((data1, data2) -> {
            double depth1 = data1.getDepth();
            double depth2 = data2.getDepth();
            
            // 确保深度为负值
            if (depth1 > 0) depth1 = -depth1;
            if (depth2 > 0) depth2 = -depth2;
            
            // 降序排列（从大到小，即从浅到深 -0.5, -1.0, -1.5...）
            return Double.compare(depth2, depth1);
        });
        
        // 添加数据点
        boolean hasSelectedPointData = false;
        for (DeepHorizontalDisplacementData data : sortedDataList) {
            // 只添加选中测点的数据
            if (!data.getPointCode().equals(selectedPoint)) {
                continue;
            }
            
            hasSelectedPointData = true;
            // 确保深度为负值
            double depth = data.getDepth();
            if (depth > 0) {
                depth = -depth; // 将正深度转为负深度
            }
            
            double cumulativeChange = data.getCumulativeChange();
            double currentChange = data.getCurrentChange();
            double rate = data.getChangeRate();
            
            // 更新深度范围
            if (depth > minDepth) {
                minDepth = depth;
            }
            if (depth < maxDepth) {
                maxDepth = depth;
            }
            
            // 更新位移值范围
            if (cumulativeChange < minDisplacement) minDisplacement = cumulativeChange;
            if (cumulativeChange > maxDisplacement) maxDisplacement = cumulativeChange;
            if (currentChange < minDisplacement) minDisplacement = currentChange;
            if (currentChange > maxDisplacement) maxDisplacement = currentChange;
            
            // 更新变化速率范围
            if (rate < minRate) minRate = rate;
            if (rate > maxRate) maxRate = rate;
            
            // 添加累计变化量数据点 - 使用负深度值
            cumulativeChangeSeries.getData().add(new XYChart.Data<>(cumulativeChange, depth));
            
            // 添加本次变化量数据点 - 使用负深度值
            currentChangeSeries.getData().add(new XYChart.Data<>(currentChange, depth));
            
            // 添加变化速率数据点 - 使用负深度值
            rateSeries.getData().add(new XYChart.Data<>(rate, depth));
        }
        
        if (!hasSelectedPointData) {
            return; // 如果没有选中测点的数据，直接返回
        }
        
        // 获取所有实际深度值
        List<Double> depthValues = sortedDataList.stream()
            .filter(data -> data.getPointCode().equals(selectedPoint))
            .map(DeepHorizontalDisplacementData::getDepth)
            .map(depth -> depth > 0 ? -depth : depth) // 确保所有深度为负值
            .distinct()
            .sorted((d1, d2) -> Double.compare(d2, d1)) // 从浅到深排序（负值从大到小）
            .collect(Collectors.toList());
            
        if (depthValues.isEmpty()) {
            return; // 如果没有深度数据，直接返回
        }
        
        // 设置Y轴范围，确保从上到下是从浅到深
        if (!depthValues.isEmpty()) {
            // 上边界设置为最浅深度再往上一点（确保最浅深度可见）
            double upperBound = depthValues.get(0) + 0.5;
            if (upperBound > -0.5) upperBound = -0.5; // 确保上限不超过-0.5m
            
            // 下边界设置为最深深度再往下一点（确保最深深度可见）
            double lowerBound = depthValues.get(depthValues.size() - 1) - 0.5;
            
            // 设置深度轴范围
            verticalDisplacementDepthAxis.setUpperBound(upperBound);
            verticalDisplacementDepthAxis.setLowerBound(lowerBound);
            verticalRateDepthAxis.setUpperBound(upperBound);
            verticalRateDepthAxis.setLowerBound(lowerBound);
            
            // 完全自定义Y轴刻度 - 使用表中的所有深度值
            // 禁用自动刻度单位
            verticalDisplacementDepthAxis.setTickUnit(0);
            verticalRateDepthAxis.setTickUnit(0);
            
            // 直接使用所有实际深度值作为刻度位置
            verticalDisplacementDepthAxis.setTickLabelFormatter(null); // 先重置格式化器
            verticalRateDepthAxis.setTickLabelFormatter(null);
            
            // 设置自定义刻度标签格式化器
            StringConverter<Number> tickLabelFormatter = new StringConverter<Number>() {
                @Override
                public String toString(Number object) {
                    double value = object.doubleValue();
                    // 显示所有深度值，保留一位小数
                    return String.format("%.1f", value);
                }

                @Override
                public Number fromString(String string) {
                    if (string == null || string.isEmpty()) return 0;
                    try {
                        return Double.parseDouble(string);
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }
            };
            
            // 应用格式化器
            verticalDisplacementDepthAxis.setTickLabelFormatter(tickLabelFormatter);
            verticalRateDepthAxis.setTickLabelFormatter(tickLabelFormatter);
            
            try {
                // 尝试直接设置刻度位置 - 基于反射，可能在某些JavaFX版本中不适用
                Method setTicksMethod = NumberAxis.class.getDeclaredMethod("setTickValues", List.class);
                setTicksMethod.setAccessible(true);
                setTicksMethod.invoke(verticalDisplacementDepthAxis, depthValues);
                setTicksMethod.invoke(verticalRateDepthAxis, depthValues);
            } catch (Exception e) {
                // 如果直接设置刻度位置失败，则使用另一种方法
                System.out.println("无法直接设置刻度位置: " + e.getMessage());
                
                // 手动增加可见的刻度线数量
                verticalDisplacementDepthAxis.setMinorTickVisible(true);
                verticalRateDepthAxis.setMinorTickVisible(true);
                verticalDisplacementDepthAxis.setMinorTickCount(depthValues.size());
                verticalRateDepthAxis.setMinorTickCount(depthValues.size());
                
                // 设置很小的刻度单位，强制显示更多的刻度
                double range = Math.abs(upperBound - lowerBound);
                double smallTickUnit = range / (depthValues.size() * 2);
                verticalDisplacementDepthAxis.setTickUnit(smallTickUnit);
                verticalRateDepthAxis.setTickUnit(smallTickUnit);
            }
            
            // 添加深度参考线
            verticalDisplacementChart.getData().add(createReferenceLines(depthValues));
            verticalRateChart.getData().add(createReferenceLines(depthValues));
        }
        
        // 禁用自动刻度，使用自定义刻度
        verticalDisplacementDepthAxis.setAutoRanging(false);
        verticalDisplacementDepthAxis.setTickLabelsVisible(true);
        verticalRateDepthAxis.setAutoRanging(false);
        verticalRateDepthAxis.setTickLabelsVisible(true);
        
        // 为速率图表设置相同的刻度标签格式化器
        verticalRateDepthAxis.setTickLabelFormatter(verticalDisplacementDepthAxis.getTickLabelFormatter());
        
        // 调整X轴位移范围
        // 计算数据范围的中心点
        double displacementCenter = (minDisplacement + maxDisplacement) / 2;
        
        // 计算合适的最大范围
        double absMax = Math.max(Math.abs(minDisplacement - displacementCenter), 
                               Math.abs(maxDisplacement - displacementCenter));
        // 确保至少有±25的视图范围
        absMax = Math.max(absMax * 1.5, 25);
        
        // 向上取整到最接近的10的倍数
        double roundedMax = Math.ceil(absMax / 10) * 10;
        
        // 根据中心点设置对称的范围
        double xLowerBound = displacementCenter - roundedMax;
        double xUpperBound = displacementCenter + roundedMax;
        
        // 当数据位于原点附近时，使用对称的范围
        if (Math.abs(displacementCenter) < roundedMax * 0.2) {
            xLowerBound = -roundedMax;
            xUpperBound = roundedMax;
        }
        
        // 设置X轴范围
        verticalDisplacementXAxis.setLowerBound(xLowerBound);
        verticalDisplacementXAxis.setUpperBound(xUpperBound);
        
        // 调整X轴刻度间隔 - 确保有合理数量的刻度线
        double xRange = xUpperBound - xLowerBound;
        double xTickUnit = 10.0; // 默认10mm一个刻度
        
        if (xRange > 200) xTickUnit = 50.0;
        else if (xRange > 100) xTickUnit = 20.0;
        else if (xRange > 50) xTickUnit = 10.0;
        else xTickUnit = 5.0;
        
        verticalDisplacementXAxis.setTickUnit(xTickUnit);
        
        // 调整变化速率X轴范围 - 类似方法
        double rateAbsMax = Math.max(Math.abs(minRate), Math.abs(maxRate));
        // 确保至少有±1的范围
        rateAbsMax = Math.max(rateAbsMax * 1.2, 1);
        
        // 设置为-rateAbsMax到+rateAbsMax的对称范围，并取整到最接近的0.5的倍数
        double roundedRateMax = Math.ceil(rateAbsMax / 0.5) * 0.5;
        verticalRateXAxis.setLowerBound(-roundedRateMax);
        verticalRateXAxis.setUpperBound(roundedRateMax);
        
        // 调整速率图表刻度间隔
        if (roundedRateMax > 5) {
            verticalRateXAxis.setTickUnit(1.0);
        } else if (roundedRateMax > 2) {
            verticalRateXAxis.setTickUnit(0.5);
        } else {
            verticalRateXAxis.setTickUnit(0.2);
        }
        
        // 清除现有数据并添加数据系列
        verticalDisplacementChart.getData().clear();
        verticalRateChart.getData().clear();
        
        // 添加数据到图表前，将深度值转换为负值
        // 根据测点ID和深度组织数据，确保为每个深度添加数据点
        Map<Double, Double> depthToCumulativeChangeMap = new HashMap<>();
        Map<Double, Double> depthToCurrentChangeMap = new HashMap<>();
        Map<Double, Double> depthToRateMap = new HashMap<>();
        
        for (DeepHorizontalDisplacementData data : sortedDataList) {
            if (data.getPointCode().equals(selectedPoint)) {
                double depth = data.getDepth();
                // 确保深度为负值
                if (depth > 0) depth = -depth;
                
                depthToCumulativeChangeMap.put(depth, data.getCumulativeChange());
                depthToCurrentChangeMap.put(depth, data.getCurrentChange());
                depthToRateMap.put(depth, data.getChangeRate());
            }
        }
        
        // 创建有序的深度列表，确保从浅到深排序（负值从大到小）
        List<Double> orderedDepths = new ArrayList<>(depthToCumulativeChangeMap.keySet());
        orderedDepths.sort((d1, d2) -> Double.compare(d2, d1)); // 从浅到深排序
        
        // 创建新的数据系列，确保按照从浅到深顺序添加数据点
        XYChart.Series<Number, Number> orderedCumulativeSeries = new XYChart.Series<>();
        XYChart.Series<Number, Number> orderedCurrentSeries = new XYChart.Series<>();
        XYChart.Series<Number, Number> orderedRateSeries = new XYChart.Series<>();
        
        orderedCumulativeSeries.setName("累计变化量(mm)");
        orderedCurrentSeries.setName("本次变化量(mm)");
        orderedRateSeries.setName("变化速率(mm/d)");
        
        // 按顺序添加数据点
        for (Double depth : orderedDepths) {
            if (depthToCumulativeChangeMap.containsKey(depth)) {
                orderedCumulativeSeries.getData().add(
                    new XYChart.Data<>(depthToCumulativeChangeMap.get(depth), depth));
            }
            
            if (depthToCurrentChangeMap.containsKey(depth)) {
                orderedCurrentSeries.getData().add(
                    new XYChart.Data<>(depthToCurrentChangeMap.get(depth), depth));
            }
            
            if (depthToRateMap.containsKey(depth)) {
                orderedRateSeries.getData().add(
                    new XYChart.Data<>(depthToRateMap.get(depth), depth));
            }
        }
        
        // 将数据添加到位移图表
        if (!orderedCurrentSeries.getData().isEmpty()) {
            verticalDisplacementChart.getData().add(orderedCurrentSeries);
        }
        
        if (!orderedCumulativeSeries.getData().isEmpty()) {
            verticalDisplacementChart.getData().add(orderedCumulativeSeries);
        }
        
        // 添加变化速率数据
        if (!orderedRateSeries.getData().isEmpty()) {
            verticalRateChart.getData().add(orderedRateSeries);
        }
        
        // 应用自定义样式
        applyVerticalChartStyling();
        
        // 更新标题以包含测点信息
        verticalDisplacementChart.setTitle("深部水平位移变化量-深度关系图 (测点:" + selectedPoint + ")");
        verticalRateChart.setTitle("深部水平位移变化速率-深度关系图 (测点:" + selectedPoint + ")");
    }
    
    /**
     * 应用垂直方向图表样式
     */
    private void applyVerticalChartStyling() {
        // 设置排序策略为NONE，确保数据按原始顺序显示
        verticalDisplacementChart.setAxisSortingPolicy(LineChart.SortingPolicy.NONE);
        verticalRateChart.setAxisSortingPolicy(LineChart.SortingPolicy.NONE);

        // 确保图表保持正确的尺寸
        verticalDisplacementChart.setPrefWidth(300);
        verticalDisplacementChart.setMinWidth(300);
        verticalDisplacementChart.setMaxWidth(300);
        
        verticalRateChart.setPrefWidth(300);
        verticalRateChart.setMinWidth(300);
        verticalRateChart.setMaxWidth(300);

        // 为位移图表中的数据系列设置样式
        if (!verticalDisplacementChart.getData().isEmpty()) {
            // 本次位移变化量 - 如果是第一个系列，设置为蓝色粗实线
            XYChart.Series<Number, Number> currentSeries = verticalDisplacementChart.getData().get(0);
            String currentStyle = "-fx-stroke: #0066cc; -fx-stroke-width: 2.5px;";
            
            currentSeries.getNode().setStyle(currentStyle);
            
            // 设置数据点样式 - 蓝色实心圆点，更小
            for (XYChart.Data<Number, Number> data : currentSeries.getData()) {
                if (data.getNode() != null) {
                    data.getNode().setStyle("-fx-background-color: #0066cc; -fx-background-radius: 2px; -fx-padding: 2px;");
                }
            }
        }
        
        // 如果有第二个系列（通常是累计变化量），设置为红色虚线
        if (verticalDisplacementChart.getData().size() >= 2) {
            XYChart.Series<Number, Number> cumulativeSeries = verticalDisplacementChart.getData().get(1);
            String cumulativeStyle = "-fx-stroke: #cc0000; -fx-stroke-width: 2px; -fx-stroke-dash-array: 5 3;";
            
            cumulativeSeries.getNode().setStyle(cumulativeStyle);
            
            // 设置数据点样式 - 红色更小的方形点
            for (XYChart.Data<Number, Number> data : cumulativeSeries.getData()) {
                if (data.getNode() != null) {
                    data.getNode().setStyle("-fx-background-color: #cc0000; -fx-background-radius: 0px; -fx-padding: 2px;");
                }
            }
        }
        
        // 为变化速率图表设置样式 - 绿色实线
        if (!verticalRateChart.getData().isEmpty()) {
            XYChart.Series<Number, Number> rateSeries = verticalRateChart.getData().get(0);
            String rateStyle = "-fx-stroke: #009900; -fx-stroke-width: 2px;";
            
            rateSeries.getNode().setStyle(rateStyle);
            
            // 设置数据点样式 - 绿色实心圆点，更小
            for (XYChart.Data<Number, Number> data : rateSeries.getData()) {
                if (data.getNode() != null) {
                    data.getNode().setStyle("-fx-background-color: #009900; -fx-background-radius: 2px; -fx-padding: 2px;");
                }
            }
        }
        
        // 为垂直中心线(X=0)添加突出显示
        Node verticalZeroLine = verticalDisplacementChart.lookup(".chart-vertical-zero-line");
        if (verticalZeroLine != null) {
            verticalZeroLine.setStyle("-fx-stroke: #000000; -fx-stroke-width: 1.5px;");
        }
        
        // 增强水平网格线和Y轴刻度可见性
        String enhancedGridStyle = 
                ".chart-horizontal-grid-lines {\n" +
                "    -fx-stroke: #aaaaaa;\n" +
                "    -fx-stroke-width: 0.8px;\n" +
                "}\n" +
                ".axis:left {\n" +
                "    -fx-tick-label-font-weight: bold;\n" +
                "    -fx-tick-label-fill: #333333;\n" +
                "}\n" +
                ".axis:left .axis-tick-mark {\n" +
                "    -fx-stroke: #333333;\n" +
                "    -fx-stroke-width: 1.0px;\n" +
                "}\n" +
                ".axis:left .axis-minor-tick-mark {\n" +
                "    -fx-stroke: #666666;\n" +
                "    -fx-stroke-width: 0.5px;\n" +
                "}\n";
        
        verticalDisplacementChart.setStyle(verticalDisplacementChart.getStyle() + enhancedGridStyle);
        verticalRateChart.setStyle(verticalRateChart.getStyle() + enhancedGridStyle);
        
        // 为垂直方向图表设置额外的样式类，便于CSS自定义
        verticalDisplacementChart.getStyleClass().add("vertical-displacement-chart");
        verticalRateChart.getStyleClass().add("vertical-rate-chart");
    }

    /**
     * 创建深度参考线
     * @param depthValues 深度值列表
     * @return XYChart.Series 包含参考线的数据系列
     */
    private XYChart.Series<Number, Number> createReferenceLines(List<Double> depthValues) {
        XYChart.Series<Number, Number> referenceLines = new XYChart.Series<>();
        referenceLines.setName("深度参考线");
        
        // 隐藏系列名，我们不希望它显示在图例中
        Platform.runLater(() -> {
            if (referenceLines.getNode() != null) {
                referenceLines.getNode().setVisible(false);
            }
        });
        
        return referenceLines;
    }
} 