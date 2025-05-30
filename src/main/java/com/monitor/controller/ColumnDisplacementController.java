package com.monitor.controller;

import java.io.File;
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

import com.monitor.model.ColumnDisplacementData;
import com.monitor.model.ColumnDisplacementPoint;
import com.monitor.model.MeasurementRecord;
import com.monitor.model.PileDisplacementData;
import com.monitor.model.PileDisplacementPoint;
import com.monitor.model.PileDisplacementDataStorage;
import com.monitor.model.ColumnDisplacementDataStorage;
import com.monitor.model.ColumnDisplacementDataStorage.ColumnDisplacementDataWrapper;
import com.monitor.util.AlertUtil;
import com.monitor.util.ExcelUtil;
import com.monitor.view.ColumnDisplacementPointSettingsController;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
import javafx.scene.paint.Color;

/**
 * 立柱竖向位移数据管理控制器
 */
public class ColumnDisplacementController {

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
    
    @FXML private TableView<ColumnDisplacementData> dataTableView;
    @FXML private TableColumn<ColumnDisplacementData, Number> serialNumberColumn;
    @FXML private TableColumn<ColumnDisplacementData, String> pointCodeColumn;
    @FXML private TableColumn<ColumnDisplacementData, Number> initialElevationColumn;
    @FXML private TableColumn<ColumnDisplacementData, Number> previousElevationColumn;
    @FXML private TableColumn<ColumnDisplacementData, Number> currentElevationColumn;
    @FXML private TableColumn<ColumnDisplacementData, Number> currentChangeColumn;
    @FXML private TableColumn<ColumnDisplacementData, Number> cumulativeChangeColumn;
    @FXML private TableColumn<ColumnDisplacementData, Number> changeRateColumn;
    @FXML private TableColumn<ColumnDisplacementData, String> mileageColumn;
    @FXML private TableColumn<ColumnDisplacementData, Number> historicalCumulativeColumn;
    
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
    private ToggleGroup chartToggleGroup;
    
    @FXML private Label pointCountLabel;
    @FXML private Label uploadDateLabel;
    
    private ObservableList<ColumnDisplacementData> settlementDataList = FXCollections.observableArrayList();
    private Stage stage;
    
    // 所有测点的所有历史数据，结构为：<测点编号, <测量日期, 数据>>
    private Map<String, Map<LocalDate, ColumnDisplacementData>> allPointDataMap = new HashMap<>();
    
    // 数据块映射，每个数据块对应一次上传的数据，结构为：<时间戳, 数据列表>
    private Map<LocalDateTime, List<ColumnDisplacementData>> dataBlocksMap = new HashMap<>();
    
    // 数据块复选框映射
    private Map<LocalDateTime, CheckBox> dataBlockCheckBoxMap = new HashMap<>();
    
    // 当前选中的数据块
    private List<LocalDateTime> selectedDataBlocks = new ArrayList<>();
    
    // 配置的测点列表
    private List<ColumnDisplacementPoint> configuredPoints = new ArrayList<>();
    
    // 自定义速率计算天数
    private int customDaysForRateCalculation = 0;
    
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
        
        // 配置数据块流布局面板
        dataBlocksFlowPane.setHgap(5); // 减小水平间距
        dataBlocksFlowPane.setVgap(5); // 减小垂直间距
        dataBlocksFlowPane.setPrefWrapLength(1200); // 增加默认宽度以便容纳更多数据块
        
        // 为了确保更紧凑的显示，设置更小的ScrollPane高度
        ScrollPane scrollPane = (ScrollPane) dataBlocksFlowPane.getParent();
        if (scrollPane != null) {
            scrollPane.setPrefViewportHeight(100); // 减小滚动面板高度
            scrollPane.setPrefHeight(100);
        }
        
        // 初始化表格
        dataTableView.setItems(settlementDataList);
        setupTableColumns();
        configureNumberColumns();
        
        // 初始化图表
        initializeCharts();
        showDisplacementChart(); // 默认显示位移图表
        
        // 初始化日期选择器为当前日期
        datePicker.setValue(LocalDate.now());
        
        // 添加表格数据变化监听器，当数据变化时自动更新图表
        settlementDataList.addListener((javafx.collections.ListChangeListener.Change<? extends ColumnDisplacementData> c) -> {
            updateChart();
        });
        
        // 创建表格上下文菜单
        ContextMenu contextMenu = new ContextMenu();
        MenuItem setCustomDaysItem = new MenuItem("设置变化速率计算天数");
        setCustomDaysItem.setOnAction(e -> showSetCustomDaysDialog());
        contextMenu.getItems().add(setCustomDaysItem);
        dataTableView.setContextMenu(contextMenu);
        
        // 加载测点配置
        loadSettlementPoints();
    }
    
    /**
     * 设置表格列
     */
    private void setupTableColumns() {
        // 设置表格列
        serialNumberColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(settlementDataList.indexOf(cellData.getValue()) + 1));
        pointCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPointCode()));
        // 其他列将在configureNumberColumns中设置
    }
    
    /**
     * 设置数值列的格式化
     */
    private void configureNumberColumns() {
        // 设置编号列
        serialNumberColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                }
            }
        });

        // 设置初始高程列 (单位: m)
        initialElevationColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getInitialElevation()));
        initialElevationColumn.setCellFactory(col -> new TableCell<>() {
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
        
        // 设置前期高程列 (单位: m)
        previousElevationColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getPreviousElevation()));
        previousElevationColumn.setCellFactory(col -> new TableCell<>() {
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
        
        // 设置本期高程列 (单位: m)
        currentElevationColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getCurrentElevation()));
        currentElevationColumn.setCellFactory(col -> new TableCell<>() {
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
        
        // 设置本期变化列 (单位: mm)
        currentChangeColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getCurrentChange()));
        currentChangeColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    double value = item.doubleValue();
                    setText(String.format("%.2f", value));
                    if (Math.abs(value) > 5.0) { 
                        // 根据数值设置不同颜色 - 沉降观测中，正值表示隆起，负值表示沉降
                        if (value > 0) {
                            setTextFill(javafx.scene.paint.Color.RED);
                    } else {
                            setTextFill(javafx.scene.paint.Color.GREEN);
                        }
                    } else {
                        setTextFill(javafx.scene.paint.Color.BLACK);
                    }
                }
            }
        });
        
        // 设置累计变化列 (单位: mm)
        cumulativeChangeColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getCumulativeChange()));
        cumulativeChangeColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    double value = item.doubleValue();
                    setText(String.format("%.2f", value));
                    if (Math.abs(value) > 10.0) { 
                        // 根据数值设置不同颜色
                        if (value > 0) {
                            setTextFill(javafx.scene.paint.Color.RED);
                    } else {
                            setTextFill(javafx.scene.paint.Color.GREEN);
                        }
                    } else {
                        setTextFill(javafx.scene.paint.Color.BLACK);
                    }
                }
            }
        });
        
        // 设置变化速率列 (单位: mm/天)
        changeRateColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getChangeRate()));
        changeRateColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    double value = item.doubleValue();
                    setText(String.format("%.2f", value));
                    if (Math.abs(value) > 0.1) {
                        if (value > 0) {
                            setTextFill(javafx.scene.paint.Color.RED);
                    } else {
                            setTextFill(javafx.scene.paint.Color.GREEN);
                        }
                    } else {
                        setTextFill(javafx.scene.paint.Color.BLACK);
                    }
                }
            }
        });
        
        // 设置里程列
        mileageColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getMileage()));
        
        // 设置历史累计列 (单位: mm)
        historicalCumulativeColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getHistoricalCumulative()));
        historicalCumulativeColumn.setCellFactory(col -> new TableCell<>() {
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
        // 位移图表
        displacementXAxis = new CategoryAxis();
        displacementYAxis = new NumberAxis();
        displacementXAxis.setLabel("测点编号");
        displacementYAxis.setLabel("位移变化量(mm)");
        displacementChart = new LineChart<>(displacementXAxis, displacementYAxis);
        displacementChart.setTitle("立柱竖向位移变化量");
        displacementChart.setCreateSymbols(true);
        displacementChart.setAnimated(false);
        
        // 速率图表
        rateXAxis = new CategoryAxis();
        rateYAxis = new NumberAxis();
        rateXAxis.setLabel("测点编号");
        rateYAxis.setLabel("变化速率(mm/天)");
        rateChart = new LineChart<>(rateXAxis, rateYAxis);
        rateChart.setTitle("立柱竖向位移变化速率");
        rateChart.setCreateSymbols(true);
        rateChart.setAnimated(false);
    }
    
    /**
     * 显示位移图表
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
        // 确定当前应该显示哪个图表
        boolean showDisplacement = (chartToggleGroup == null) || 
                                  (displacementChartButton.isSelected());
        
        // 获取要显示的数据
        List<ColumnDisplacementData> dataToShow = new ArrayList<>(settlementDataList);
        
        // 清空现有数据
        displacementChart.getData().clear();
        rateChart.getData().clear();
        
        if (dataToShow.isEmpty()) {
            return;
        }
        
        // 按日期对数据进行分组
        Map<LocalDate, List<ColumnDisplacementData>> dataByDate = new HashMap<>();
        for (ColumnDisplacementData data : dataToShow) {
            LocalDate date = data.getMeasurementDate();
            if (!dataByDate.containsKey(date)) {
                dataByDate.put(date, new ArrayList<>());
            }
            dataByDate.get(date).add(data);
        }
        
        // 按照日期排序
        List<LocalDate> sortedDates = new ArrayList<>(dataByDate.keySet());
        sortedDates.sort((d1, d2) -> d1.compareTo(d2));
        
        // 创建数据系列
        if (showDisplacement) {
            // 立柱竖向位移累计变化曲线
            XYChart.Series<String, Number> cumulativeSeries = new XYChart.Series<>();
            cumulativeSeries.setName("累计变化量(mm)");
            
            // 立柱竖向位移本期变化曲线
            XYChart.Series<String, Number> currentSeries = new XYChart.Series<>();
            currentSeries.setName("本期变化量(mm)");
            
            // 为每个测点添加数据点
            for (ColumnDisplacementData data : dataToShow) {
                String pointId = data.getPointCode();
                cumulativeSeries.getData().add(new XYChart.Data<>(pointId, data.getCumulativeChange()));
                currentSeries.getData().add(new XYChart.Data<>(pointId, data.getCurrentChange()));
            }
            
            displacementChart.getData().add(cumulativeSeries);
            displacementChart.getData().add(currentSeries);
        } else {
            // 立柱竖向位移变化速率曲线
            XYChart.Series<String, Number> rateSeries = new XYChart.Series<>();
            rateSeries.setName("变化速率(mm/天)");
            
            // 为每个测点添加数据点
            for (ColumnDisplacementData data : dataToShow) {
                String pointId = data.getPointCode();
                rateSeries.getData().add(new XYChart.Data<>(pointId, data.getChangeRate()));
            }
            
            rateChart.getData().add(rateSeries);
        }
        
        // 应用图表样式
        applyChartStyling();
    }
    
    /**
     * 应用图表样式
     */
    private void applyChartStyling() {
        // 设置图表标题和轴标签的样式
        String titleStyle = "-fx-font-size: 16px; -fx-font-weight: bold;";
        String axisStyle = "-fx-font-size: 12px;";
        
        displacementChart.setStyle(titleStyle);
        displacementXAxis.setStyle(axisStyle);
        displacementYAxis.setStyle(axisStyle);
        
        rateChart.setStyle(titleStyle);
        rateXAxis.setStyle(axisStyle);
        rateYAxis.setStyle(axisStyle);
        
        // 应用样式类
        displacementChart.getStyleClass().add("chart");
        rateChart.getStyleClass().add("chart");
    }
    
    /**
     * 处理上传数据按钮事件
     */
    @FXML
    private void handleUploadButtonAction(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择Excel数据文件");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel文件", "*.xlsx", "*.xls"));
        
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            processSelectedExcelFile(selectedFile);
        }
    }
    
    /**
     * 处理Excel文件
     */
    private void processSelectedExcelFile(File file) {
        try {
            // 显示处理中状态
            if (uploadDateLabel != null) {
                uploadDateLabel.setText("正在处理文件，请稍候...");
            }

            String sheetName = "立柱竖向位移";

            // 验证Excel文件格式
            if (!ExcelUtil.validateExcelFormat(file, sheetName)) {
                AlertUtil.showWarning("文件格式错误",
                    "所选文件格式不符合要求。\n请确保文件包含名为\"" + sheetName + "\"的工作表，且包含测点编号和高程列。");

                if (uploadDateLabel != null) {
                    uploadDateLabel.setText("上传失败: 格式错误");
                }
                return;
            }

            // 读取Excel数据
            Map<String, Double> pointElevationMap = ExcelUtil.importFromExcel(file, sheetName);

            if (pointElevationMap.isEmpty()) {
                AlertUtil.showWarning("数据为空", "所选文件不包含有效数据。");

                if (uploadDateLabel != null) {
                    uploadDateLabel.setText("上传失败: 无有效数据");
                }
                return;
            }

            // 获取当前选择的日期
            LocalDate uploadDate = datePicker.getValue();

            // 将数据转换为ColumnDisplacementData对象
            List<ColumnDisplacementData> uploadedData = processExcelPointData(pointElevationMap, uploadDate);

            // 处理日期冲突
            handleDataDateConflict(uploadDate, uploadedData, file.getName(), pointElevationMap.size());

            // 显示成功消息
            AlertUtil.showInformation("数据上传成功", "成功导入测点数据: " + pointElevationMap.size() + "个");

        } catch (IOException e) {
            AlertUtil.showError("文件读取错误", "无法读取所选文件: " + e.getMessage());
            if (uploadDateLabel != null) {
                uploadDateLabel.setText("上传失败: 读取错误");
            }
            e.printStackTrace();
        } catch (Exception e) {
            AlertUtil.showError("处理错误", "处理数据时发生错误: " + e.getMessage());
            if (uploadDateLabel != null) {
                uploadDateLabel.setText("上传失败: 处理错误");
            }
            e.printStackTrace();
        }
    }
    
    /**
     * 处理Excel中读取的测点数据
     */
    private List<ColumnDisplacementData> processExcelPointData(Map<String, Double> pointElevationMap, LocalDate measureDate) {
        List<ColumnDisplacementData> result = new ArrayList<>();
        
        // 获取已配置的测点信息，建立映射表
        Map<String, ColumnDisplacementPoint> configuredPointsMap = new HashMap<>();
        for (ColumnDisplacementPoint point : configuredPoints) {
            configuredPointsMap.put(point.getPointId(), point);
        }
        
        // 获取已有的数据，按测点和日期分组
        Map<String, Map<LocalDate, ColumnDisplacementData>> existingDataMap = new HashMap<>(allPointDataMap);
        
        // 处理每个测点的高程数据
        for (Map.Entry<String, Double> entry : pointElevationMap.entrySet()) {
            String pointId = entry.getKey();
            double currentElevation = entry.getValue();
            
            // 创建新的数据对象
            ColumnDisplacementData data = new ColumnDisplacementData();
            data.setPointCode(pointId);
            data.setCurrentElevation(currentElevation);
            data.setMeasurementDate(measureDate);
            
            // 如果是已配置的测点，使用配置的初始高程
            ColumnDisplacementPoint configPoint = configuredPointsMap.get(pointId);
            if (configPoint != null) {
                data.setInitialElevation(configPoint.getInitialElevation());
                data.setMileage(configPoint.getMileage());
                data.setHistoricalCumulative(configPoint.getHistoricalCumulative());
            } else {
                // 如果是新测点，使用当前高程作为初始高程
                data.setInitialElevation(currentElevation);
                data.setMileage("");
                data.setHistoricalCumulative(0.0);
                
                // 自动添加到配置中
                ColumnDisplacementPoint newPoint = new ColumnDisplacementPoint(
                    pointId, currentElevation, "", 2.0, 20.0, 0.0
                );
                configuredPoints.add(newPoint);
                configuredPointsMap.put(pointId, newPoint);
            }
            
            // 查找该测点的前期数据
            Map<LocalDate, ColumnDisplacementData> pointHistory = existingDataMap.get(pointId);
            if (pointHistory != null && !pointHistory.isEmpty()) {
                // 找到最近的一次前期数据
                LocalDate latestDate = null;
                for (LocalDate date : pointHistory.keySet()) {
                    if (date.isBefore(measureDate) && (latestDate == null || date.isAfter(latestDate))) {
                        latestDate = date;
                    }
                }
                
                if (latestDate != null) {
                    ColumnDisplacementData previousData = pointHistory.get(latestDate);
                    data.setPreviousElevation(previousData.getCurrentElevation());
                    
                    // 计算变化量
                    if (customDaysForRateCalculation > 0) {
                        data.calculateDerivedValues(previousData.getMeasurementDate(), measureDate, customDaysForRateCalculation);
                    } else {
                        data.calculateDerivedValues(previousData.getMeasurementDate(), measureDate, 0);
                    }
                } else {
                    // 没有前期数据，使用初始高程作为前期高程
                    data.setPreviousElevation(data.getInitialElevation());
                    data.calculateDerivedValues();
                }
            } else {
                // 没有历史数据，使用初始高程作为前期高程
                data.setPreviousElevation(data.getInitialElevation());
                data.calculateDerivedValues();
            }
            
            result.add(data);
        }
        
        return result;
    }
    
    /**
     * 处理数据的日期冲突
     */
    private void handleDataDateConflict(LocalDate date, List<ColumnDisplacementData> data, String fileName, int dataCount) {
        // 使用当前系统时间创建唯一的数据块ID (LocalDateTime)
        LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.now());
        
        // 更新数据的测量日期
        for (ColumnDisplacementData item : data) {
            item.setMeasurementDate(date);
            
            // 将数据添加到映射表中，便于后续查询
            String pointId = item.getPointCode();
            
            if (!allPointDataMap.containsKey(pointId)) {
                allPointDataMap.put(pointId, new HashMap<>());
            }
            
            // 添加或更新数据
            allPointDataMap.get(pointId).put(date, item);
        }
        
        // 保存数据到模型
        dataBlocksMap.put(dateTime, data);
        
        // 更新UI
        addDataBlock(fileName, dateTime);
        updatePointCount();
        
        // 更新上传日期标签
        if (uploadDateLabel != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            uploadDateLabel.setText("上传日期: " + dateTime.format(formatter) + " (共" + dataCount + "个测点)");
        }
        
        // 自动选择最新上传的数据块
        CheckBox newDataCheckBox = dataBlockCheckBoxMap.get(dateTime);
        if (newDataCheckBox != null && !newDataCheckBox.isSelected()) {
            newDataCheckBox.setSelected(true);
            handleDataBlockSelection(newDataCheckBox);
        }
    }
    
    /**
     * 添加数据块到UI
     */
    private void addDataBlock(String fileName, LocalDateTime dateTime) {
        HBox blockBox = new HBox(2); // 减少内部间距
        blockBox.getStyleClass().addAll("data-block", "data-block-compact"); // 使用紧凑样式
        blockBox.setPadding(new Insets(2, 4, 2, 4)); // 进一步减小内边距
        
        CheckBox checkBox = new CheckBox();
        checkBox.setPrefWidth(16); // 减小宽度
        
        // 使用更紧凑的日期时间格式: MM-dd HH:mm
        Label label = new Label(dateTime.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")));
        label.setPrefWidth(70); // 减小宽度
        label.setStyle("-fx-font-size: 11px;"); // 进一步减小字体大小
        
        blockBox.getChildren().addAll(checkBox, label);
        
        // 添加到UI
        dataBlocksFlowPane.getChildren().add(blockBox);
        
        // 设置选择框提示 - 显示完整日期时间
        String tooltipText = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(tooltipText);
        checkBox.setTooltip(tooltip);
        label.setTooltip(tooltip);
        
        // 保存复选框引用
        dataBlockCheckBoxMap.put(dateTime, checkBox);
        
        // 设置复选框事件
        checkBox.setOnAction(e -> handleDataBlockSelection(checkBox));
        
        // 为数据块添加右键删除菜单
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("删除");
        deleteItem.setOnAction(e -> {
            removeDataBlock(dateTime);
        });
        contextMenu.getItems().add(deleteItem);
        
        // 将右键菜单绑定到数据块
        blockBox.setOnContextMenuRequested(e -> {
            contextMenu.show(blockBox, e.getScreenX(), e.getScreenY());
        });
        
        // 保存用户数据，方便后续操作
        checkBox.setUserData(dateTime);
    }
    
    /**
     * 删除数据块
     */
    private void removeDataBlock(LocalDateTime dateTime) {
        // 从UI中移除
        for (int i = 0; i < dataBlocksFlowPane.getChildren().size(); i++) {
            if (dataBlocksFlowPane.getChildren().get(i) instanceof HBox) {
                HBox blockBox = (HBox) dataBlocksFlowPane.getChildren().get(i);
                for (int j = 0; j < blockBox.getChildren().size(); j++) {
                    if (blockBox.getChildren().get(j) instanceof CheckBox) {
                        CheckBox cb = (CheckBox) blockBox.getChildren().get(j);
                        if (cb.getUserData() != null && cb.getUserData().equals(dateTime)) {
                            dataBlocksFlowPane.getChildren().remove(i);
                            break;
                        }
                    }
                }
            }
        }
        
        // 从数据结构中移除
        dataBlocksMap.remove(dateTime);
        dataBlockCheckBoxMap.remove(dateTime);
        selectedDataBlocks.remove(dateTime);
        
        // 更新UI
        updateTableBasedOnSelection();
        
        // 更新显示项
        updatePointCount();
    }
    
    /**
     * 处理数据块选择事件
     */
    private void handleDataBlockSelection(CheckBox checkBox) {
        LocalDateTime blockDateTime = (LocalDateTime) checkBox.getUserData();
        
        if (blockDateTime == null) {
            return;
        }
        
        if (checkBox.isSelected()) {
            // 如果已经选择了两个数据块，取消最早选择的那个
            if (selectedDataBlocks.size() >= 2) {
                LocalDateTime oldestDateTime = selectedDataBlocks.get(0);
                CheckBox oldestCheckBox = dataBlockCheckBoxMap.get(oldestDateTime);
                if (oldestCheckBox != null) {
                    oldestCheckBox.setSelected(false);
                }
                selectedDataBlocks.remove(0);
            }
            
            // 添加新选择的数据块
            selectedDataBlocks.add(blockDateTime);
        } else {
            // 移除取消选择的数据块
            selectedDataBlocks.remove(blockDateTime);
        }
        
        // 更新UI显示
        updateTableBasedOnSelection();
    }
    
    /**
     * 根据选择条件更新表格数据
     */
    private void updateTableBasedOnSelection() {
        // 清空数据列表
        settlementDataList.clear();
        
        // 如果没有选中任何数据块，则不显示数据
        if (selectedDataBlocks.isEmpty()) {
            updateChart();
            return;
        }
        
        // 根据选择的数据块数量处理
        if (selectedDataBlocks.size() == 1) {
            // 只选择了一个数据块，将其作为当前高程
            LocalDateTime currentDateTime = selectedDataBlocks.get(0);
            List<ColumnDisplacementData> currentData = dataBlocksMap.get(currentDateTime);
            
            // 创建按测点排序索引排序的数据映射
            Map<String, ColumnDisplacementData> dataMap = new HashMap<>();
            for (ColumnDisplacementData data : currentData) {
                dataMap.put(data.getPointCode(), data);
            }
            
            // 按照配置顺序添加测点
            List<ColumnDisplacementPoint> sortedPoints = new ArrayList<>(configuredPoints);
            sortedPoints.sort((p1, p2) -> Integer.compare(p1.getOrderIndex(), p2.getOrderIndex()));
            
            // 按排序后的测点顺序添加数据
            for (ColumnDisplacementPoint pointConfig : sortedPoints) {
                String pointCode = pointConfig.getPointId();
                ColumnDisplacementData data = dataMap.get(pointCode);
                
                if (data != null) {
                    // 使用配置中的初始高程，而不是数据中的
                    data.setInitialElevation(pointConfig.getInitialElevation());
                    // 重新计算派生值
                    data.calculateDerivedValues();
                    settlementDataList.add(data);
                }
            }
        } else if (selectedDataBlocks.size() == 2) {
            // 选择了两个数据块，按时间顺序设置为上一次和当前高程
            LocalDateTime dateTime1 = selectedDataBlocks.get(0);
            LocalDateTime dateTime2 = selectedDataBlocks.get(1);
            
            LocalDateTime previousDateTime = dateTime1.isBefore(dateTime2) ? dateTime1 : dateTime2;
            LocalDateTime currentDateTime = dateTime1.isBefore(dateTime2) ? dateTime2 : dateTime1;
            
            List<ColumnDisplacementData> previousData = dataBlocksMap.get(previousDateTime);
            List<ColumnDisplacementData> currentData = dataBlocksMap.get(currentDateTime);
            
            // 创建测点映射以便快速查找
            Map<String, ColumnDisplacementData> previousDataMap = previousData.stream()
                    .collect(Collectors.toMap(ColumnDisplacementData::getPointCode, data -> data));
            
            // 创建按测点排序索引排序的数据列表
            Map<String, ColumnDisplacementData> currentDataMap = new HashMap<>();
            for (ColumnDisplacementData data : currentData) {
                currentDataMap.put(data.getPointCode(), data);
            }
            
            // 按照orderIndex排序测点，确保与Excel导入顺序一致
            List<ColumnDisplacementPoint> sortedPoints = new ArrayList<>(configuredPoints);
            sortedPoints.sort((p1, p2) -> Integer.compare(p1.getOrderIndex(), p2.getOrderIndex()));
            
            // 按排序后的测点顺序添加数据
            for (ColumnDisplacementPoint pointConfig : sortedPoints) {
                String pointCode = pointConfig.getPointId();
                ColumnDisplacementData currentItem = currentDataMap.get(pointCode);
                
                if (currentItem != null) {
                    // 使用配置中的初始高程，而不是数据中的
                    currentItem.setInitialElevation(pointConfig.getInitialElevation());
                    
                    // 查找该测点的上一次记录
                    ColumnDisplacementData previousItem = previousDataMap.get(pointCode);
                    
                    if (previousItem != null) {
                        // 使用上一次记录的高程作为前期高程
                        currentItem.setPreviousElevation(previousItem.getCurrentElevation());
                        
                        // 获取自定义天数设置
                        int customDays = getCustomDaysForRateCalculation();
                        
                        // 计算派生值，使用实际测量日期或自定义天数
                        LocalDate previousDate = previousItem.getMeasurementDate();
                        LocalDate currentDate = currentItem.getMeasurementDate();
                        currentItem.calculateDerivedValues(previousDate, currentDate, customDays);
                    }
                    
                    settlementDataList.add(currentItem);
                }
            }
        }
        
        // 刷新表格
        dataTableView.refresh();
        
        // 更新图表
        updateChart();
    }
    
    /**
     * 处理导出按钮事件
     */
    @FXML
    private void handleExportButtonAction(ActionEvent event) {
        // 检查是否有数据可导出
        if (settlementDataList.isEmpty()) {
            AlertUtil.showWarning("无数据", "当前没有可导出的数据。");
            return;
        }
        
        // 创建文件选择器
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出数据到Excel");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel文件", "*.xlsx"));
        
        // 设置默认文件名
        LocalDate currentDate = LocalDate.now();
        String defaultFileName = "立柱竖向位移数据_" + 
                currentDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        fileChooser.setInitialFileName(defaultFileName);
        
        // 显示保存对话框
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                // 准备表头
                List<String> headers = new ArrayList<>();
                headers.add("测点编号");
                headers.add("初始高程(m)");
                headers.add("前期高程(m)");
                headers.add("本期高程(m)");
                headers.add("本期变化(mm)");
                headers.add("累计变化(mm)");
                headers.add("变化速率(mm/天)");
                headers.add("里程");
                headers.add("历史累计(mm)");
                headers.add("测量日期");
                
                // 准备数据
                List<List<String>> data = new ArrayList<>();
                
                for (ColumnDisplacementData item : settlementDataList) {
                    List<String> row = new ArrayList<>();
                    row.add(item.getPointCode());
                    row.add(String.format("%.4f", item.getInitialElevation()));
                    row.add(String.format("%.4f", item.getPreviousElevation()));
                    row.add(String.format("%.4f", item.getCurrentElevation()));
                    row.add(String.format("%.2f", item.getCurrentChange()));
                    row.add(String.format("%.2f", item.getCumulativeChange()));
                    row.add(String.format("%.2f", item.getChangeRate()));
                    row.add(item.getMileage());
                    row.add(String.format("%.2f", item.getHistoricalCumulative()));
                    row.add(item.getMeasurementDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    
                    data.add(row);
                }
                
                // 导出数据
                ExcelUtil.exportToExcel(file, "立柱竖向位移", headers, data);
                
                // 显示成功消息
                AlertUtil.showInformation("导出成功", "数据已成功导出到: " + file.getAbsolutePath());
                
            } catch (IOException e) {
                AlertUtil.showError("导出错误", "导出数据时发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 处理测点设置按钮事件
     */
    @FXML
    private void handleSettingsButtonAction(ActionEvent event) {
        try {
            // 加载FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/ColumnDisplacementPointSettingsDialog.fxml"));
            Parent root = loader.load();
            
            // 创建对话框舞台
            Stage dialogStage = new Stage();
            dialogStage.setTitle("立柱竖向位移测点设置");
            dialogStage.initModality(Modality.WINDOW_MODAL);  // 设置为模态对话框
            dialogStage.initOwner(stage);  // 设置父窗口
            
            // 设置场景
            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            
            // 获取控制器
            ColumnDisplacementPointSettingsController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            
            // 设置初始数据
            List<ColumnDisplacementPoint> initialPoints = new ArrayList<>(configuredPoints);
            controller.setInitialData(initialPoints);
            
            // 显示对话框并等待直到关闭
            dialogStage.showAndWait();
            
            // 获取用户设置的数据
            List<ColumnDisplacementPoint> newPoints = controller.getPoints();
            
            // 更新测点配置
            configuredPoints.clear();
            configuredPoints.addAll(newPoints);
            
            // 更新测点数量显示
            updatePointCount();
            
            // 重新加载数据
            updateTableBasedOnSelection();
            
            System.out.println("立柱竖向位移测点设置已更新，共 " + configuredPoints.size() + " 个测点");
        } catch (IOException e) {
            System.err.println("无法加载测点设置对话框: " + e.getMessage());
            e.printStackTrace();
            
            // 显示错误信息
            AlertUtil.showError("错误", "无法打开测点设置对话框: " + e.getMessage());
        }
    }
    
    /**
     * 更新测点数量显示
     */
    private void updatePointCount() {
        if (pointCountLabel != null) {
            pointCountLabel.setText("测点数量: " + configuredPoints.size());
        }
    }
    
    /**
     * 加载测点配置
     */
    private void loadSettlementPoints() {
        // 测点配置将在从项目文件加载时设置，不再添加测试数据
        if (configuredPoints.isEmpty()) {
            // 不添加示例测点，只有空列表
            System.out.println("立柱竖向位移无测点配置，请先添加测点");
        }
        
        // 更新测点计数显示
        updatePointCount();
        
        // 初始化表格
        updateTableWithInitialData();
    }
    
    /**
     * 初始化表格数据
     */
    private void updateTableWithInitialData() {
        if (configuredPoints.isEmpty()) {
            settlementDataList.clear();
            dataTableView.setItems(settlementDataList);
            return;
        }
        
        // 清空现有数据
        settlementDataList.clear();
        
        // 获取当前选择的日期
        LocalDate currentDate = LocalDate.now();
        if (datePicker != null && datePicker.getValue() != null) {
            currentDate = datePicker.getValue();
        }
        
        // 为每个配置的测点创建一个初始数据行
        for (ColumnDisplacementPoint point : configuredPoints) {
            ColumnDisplacementData data = new ColumnDisplacementData();
            data.setPointCode(point.getPointId());
            data.setInitialElevation(point.getInitialElevation());
            data.setPreviousElevation(point.getInitialElevation());
            data.setCurrentElevation(point.getInitialElevation());
            data.setMileage(point.getMileage());
            data.setHistoricalCumulative(point.getHistoricalCumulative());
            data.setMeasurementDate(currentDate);
            
            // 设置默认值
            data.setCurrentChange(0);
            data.setCumulativeChange(0);
            data.setChangeRate(0);
            
            settlementDataList.add(data);
        }
        
        // 更新表格
        dataTableView.setItems(settlementDataList);
        
        // 更新图表
        updateChart();
    }
    
    /**
     * 显示设置自定义天数对话框
     */
    private void showSetCustomDaysDialog() {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(customDaysForRateCalculation));
        dialog.setTitle("设置变化速率计算天数");
        dialog.setHeaderText("请输入计算变化速率的天数");
        dialog.setContentText("天数（0表示使用实际间隔天数）:");
        
        // 应用主题样式
        if (stage != null) {
            dialog.initOwner(stage);
        }
        
        // 显示对话框并处理结果
        dialog.showAndWait().ifPresent(result -> {
            try {
                int days = Integer.parseInt(result);
                if (days >= 0) {
                    customDaysForRateCalculation = days;
                    // 重新计算所有数据的变化速率
                    for (ColumnDisplacementData data : settlementDataList) {
                        // 如果有历史数据，重新计算速率
                        String pointId = data.getPointCode();
                        Map<LocalDate, ColumnDisplacementData> pointHistory = allPointDataMap.get(pointId);
                        
                        if (pointHistory != null && pointHistory.size() > 1) {
                            LocalDate measureDate = data.getMeasurementDate();
                            
                            // 找到前一个日期的数据
                            LocalDate previousDate = null;
                            for (LocalDate date : pointHistory.keySet()) {
                                if (date.isBefore(measureDate) && (previousDate == null || date.isAfter(previousDate))) {
                                    previousDate = date;
                                }
                            }
                            
                            if (previousDate != null) {
                                ColumnDisplacementData previousData = pointHistory.get(previousDate);
                                // 使用自定义天数重新计算
                                data.calculateDerivedValues(previousDate, measureDate, customDaysForRateCalculation);
                            }
                        }
                    }
                    
                    // 更新表格和图表
                    dataTableView.refresh();
                    updateChart();
                    
                    AlertUtil.showInformation("设置成功", 
                        customDaysForRateCalculation == 0 ? 
                        "将使用实际间隔天数计算变化速率。" : 
                        "将使用 " + customDaysForRateCalculation + " 天作为计算变化速率的天数。");
                } else {
                    AlertUtil.showWarning("输入错误", "天数必须大于或等于0。");
                }
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("输入错误", "请输入有效的数字。");
            }
        });
    }
    
    /**
     * 获取当前的自定义天数
     */
    public int getCustomDaysForRateCalculation() {
        return customDaysForRateCalculation;
    }
    
    /**
     * 设置自定义天数
     */
    public void setCustomDaysForRateCalculation(int days) {
        if (days >= 0) {
            this.customDaysForRateCalculation = days;
        }
    }
    
    /**
     * 获取测点数据存储
     * 用于在主控制器中保存数据
     */
    public ColumnDisplacementDataStorage getColumnDisplacementDataStorage() {
        ColumnDisplacementDataStorage storage = new ColumnDisplacementDataStorage();
        
        // 设置测点
        storage.setConfiguredPoints(new ArrayList<>(configuredPoints));
        
        // 设置数据块
        for (Map.Entry<LocalDateTime, List<ColumnDisplacementData>> entry : dataBlocksMap.entrySet()) {
            storage.addDataBlock(entry.getKey(), entry.getValue(), 
                    "导入于 " + entry.getKey().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        // 设置选中的数据块
        storage.setSelectedDataBlocks(new ArrayList<>(selectedDataBlocks));
        
        // 设置自定义天数
        storage.setCustomDaysForRateCalculation(customDaysForRateCalculation);
        
        return storage;
    }
    
    /**
     * 从立柱位移数据存储对象加载数据 (符合规范的新方法名)
     * 该方法是loadFromDataStorage的包装器，提供更符合命名规范的接口
     * @param storage 立柱位移数据存储对象
     */
    public void loadFromColumnDisplacementDataStorage(ColumnDisplacementDataStorage storage) {
        // 调用现有方法以保持向后兼容性
        loadFromDataStorage(storage);
    }
    
    /**
     * 从数据存储对象加载测点和数据
     */
    public void loadFromDataStorage(ColumnDisplacementDataStorage storage) {
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
        configuredPoints.addAll(storage.getConfiguredPoints());
        
        // 加载数据块
        Map<LocalDateTime, List<ColumnDisplacementDataWrapper>> dataBlocksFromStorage = storage.getDataBlocksMap();
        for (Map.Entry<LocalDateTime, List<ColumnDisplacementDataWrapper>> entry : dataBlocksFromStorage.entrySet()) {
            LocalDateTime timestamp = entry.getKey();
            List<ColumnDisplacementDataWrapper> wrappers = storage.getDataBlocksMap().get(timestamp);
            
            if (wrappers != null && !wrappers.isEmpty()) {
                // 提取数据并添加到本地存储
                List<ColumnDisplacementData> dataList = new ArrayList<>();
                for (ColumnDisplacementDataWrapper wrapper : wrappers) {
                    dataList.addAll(wrapper.getData());
                }
                
                // 添加到UI和本地映射
                dataBlocksMap.put(timestamp, dataList);
                
                // 显示在UI上
                addDataBlock(storage.getDataBlockDescription(timestamp), timestamp);
            }
        }
        
        // 加载选中的数据块
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
            updateTableBasedOnSelection();
            updateChartBasedOnSelection();
        }
    }
    
    /**
     * 更新图表以显示选定的数据块
     */
    private void updateChartBasedOnSelection() {
        // 确保有选中的数据
        if (selectedDataBlocks.isEmpty()) {
            displacementChart.getData().clear();
            rateChart.getData().clear();
            return;
        }

        // 更新图表
        updateChart();
    }
    
    /**
     * 从保存的测量记录加载数据
     * 用于从项目文件恢复之前的数据
     */
    public void loadFromMeasurementRecords(List<MeasurementRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        
        System.out.println("正在加载 " + records.size() + " 条立柱竖向位移测量记录");
        
        // 按日期分组
        Map<LocalDate, List<MeasurementRecord>> recordsByDate = new HashMap<>();
        for (MeasurementRecord record : records) {
            LocalDate date = record.getMeasureTime().toLocalDate();
            
            recordsByDate.computeIfAbsent(date, k -> new ArrayList<>())
                         .add(record);
        }
        
        // 清空现有数据
        allPointDataMap.clear();
        dataBlocksMap.clear();
        dataBlockCheckBoxMap.clear();
        dataBlocksFlowPane.getChildren().clear();
        selectedDataBlocks.clear();
        
        // 处理每个日期的数据
        for (Map.Entry<LocalDate, List<MeasurementRecord>> entry : recordsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<MeasurementRecord> dateRecords = entry.getValue();
            
            // 创建一个数据块
            LocalDateTime blockTime = LocalDateTime.of(date, LocalTime.now());
            List<ColumnDisplacementData> blockData = new ArrayList<>();
            
            // 处理每条记录
            for (MeasurementRecord record : dateRecords) {
                ColumnDisplacementPoint point = null;
                
                // 查找对应的测点
                for (ColumnDisplacementPoint configPoint : configuredPoints) {
                    if (configPoint.getPointId().equals(record.getId())) {
                        point = configPoint;
                        break;
                    }
                }
                
                // 如果找不到对应的测点，创建一个新的
                if (point == null) {
                    point = new ColumnDisplacementPoint();
                    point.setPointId(record.getId());
                    point.setInitialElevation(record.getValue());
                    point.setMileage("");
                    point.setRateWarningValue(10.0); // 默认值
                    point.setAccumulatedWarningValue(30.0); // 默认值
                    configuredPoints.add(point);
                }
                
                // 创建数据对象
                ColumnDisplacementData data = new ColumnDisplacementData();
                data.setPointCode(record.getId());
                data.setInitialElevation(point.getInitialElevation());
                data.setPreviousElevation(point.getInitialElevation()); // 这里简化处理，后面会更新
                data.setCurrentElevation(record.getValue());
                data.setMileage(point.getMileage());
                data.setHistoricalCumulative(point.getHistoricalCumulative());
                data.setMeasurementDate(date);
                
                // 添加到数据块
                blockData.add(data);
                
                // 更新测点的数据历史
                Map<LocalDate, ColumnDisplacementData> pointHistory = 
                    allPointDataMap.computeIfAbsent(record.getId(), k -> new HashMap<>());
                pointHistory.put(date, data);
            }
            
            // 计算派生值（变化量、累计变化等）
            for (ColumnDisplacementData data : blockData) {
                String pointId = data.getPointCode();
                Map<LocalDate, ColumnDisplacementData> pointHistory = allPointDataMap.get(pointId);
                
                if (pointHistory != null && pointHistory.size() > 1) {
                    // 找到前一个日期的数据
                    LocalDate currentDate = data.getMeasurementDate();
                    LocalDate previousDate = null;
                    
                    for (LocalDate historyDate : pointHistory.keySet()) {
                        if (historyDate.isBefore(currentDate) && 
                            (previousDate == null || historyDate.isAfter(previousDate))) {
                            previousDate = historyDate;
                        }
                    }
                    
                    // 如果找到前一个日期的数据，更新前期高程并计算变化
                    if (previousDate != null) {
                        ColumnDisplacementData previousData = pointHistory.get(previousDate);
                        data.setPreviousElevation(previousData.getCurrentElevation());
                        data.calculateDerivedValues(previousDate, currentDate, customDaysForRateCalculation);
                    } else {
                        data.calculateDerivedValues();
                    }
                } else {
                    data.calculateDerivedValues();
                }
            }
            
            // 添加数据块
            if (!blockData.isEmpty()) {
                dataBlocksMap.put(blockTime, blockData);
                
                // 在UI中显示数据块
                addDataBlock("导入于 " + blockTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), blockTime);
            }
        }
        
        // 更新表格和图表
        updatePointCount();
        updateTableWithInitialData();
    }
    
    /**
     * 获取当前显示的数据列表
     * 主要用于收集数据进行保存
     * @return 当前表格中显示的数据列表
     */
    public List<ColumnDisplacementData> getDisplayData() {
        // 返回当前表格中的数据
        if (dataTableView != null && dataTableView.getItems() != null) {
            return new ArrayList<>(dataTableView.getItems());
        }
        return new ArrayList<>();
    }
    
    /**
     * 获取用于保存的测量记录列表
     * 将当前所有数据块中的ColumnDisplacementData转换为MeasurementRecord对象
     * @return 用于保存的MeasurementRecord列表
     */
    public List<MeasurementRecord> getMeasurementRecordsForSaving() {
        List<MeasurementRecord> records = new ArrayList<>();
        
        // 遍历所有数据块
        for (Map.Entry<LocalDateTime, List<ColumnDisplacementData>> entry : dataBlocksMap.entrySet()) {
            LocalDateTime blockTime = entry.getKey();
            List<ColumnDisplacementData> dataList = entry.getValue();
            
            for (ColumnDisplacementData data : dataList) {
                MeasurementRecord record = new MeasurementRecord();
                record.setId(data.getPointCode());
                record.setValue(data.getCurrentElevation());
                
                // 设置测量时间
                if (data.getMeasurementDate() != null) {
                    record.setMeasureTime(LocalDateTime.of(data.getMeasurementDate(), LocalTime.NOON));
                } else {
                    record.setMeasureTime(blockTime);
                }
                
                // 设置单位
                record.setUnit("m");  // 高程单位默认为米
                
                // 设置预警级别
                int warningLevel = 0;
                if (Math.abs(data.getChangeRate()) > 0.5) {
                    warningLevel = 3;  // 高预警
                } else if (Math.abs(data.getChangeRate()) > 0.2) {
                    warningLevel = 2;  // 中预警
                } else if (Math.abs(data.getChangeRate()) > 0.1) {
                    warningLevel = 1;  // 低预警
                }
                record.setWarningLevel(warningLevel);
                
                // 添加到记录列表
                records.add(record);
            }
        }
        
        System.out.println("已准备 " + records.size() + " 条立柱竖向位移测量记录用于保存");
        return records;
    }
} 