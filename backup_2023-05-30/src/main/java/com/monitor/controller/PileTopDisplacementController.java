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

import com.monitor.model.PileDisplacementData;
import com.monitor.model.PileDisplacementPoint;
import com.monitor.model.MeasurementRecord;
import com.monitor.model.PileDisplacementDataStorage;
import com.monitor.model.PileDisplacementDataStorage.PileDisplacementDataWrapper;
import com.monitor.util.AlertUtil;
import com.monitor.util.ExcelUtil;
import com.monitor.view.PileTopDisplacementPointSettingsController;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
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
import javafx.scene.control.MenuItem;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ToggleGroup;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.control.ContextMenu;

/**
 * 桩顶竖向位移数据管理控制器
 */
public class PileTopDisplacementController {

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
    
    @FXML private TableView<PileDisplacementData> dataTableView;
    @FXML private TableColumn<PileDisplacementData, Number> serialNumberColumn;
    @FXML private TableColumn<PileDisplacementData, String> pointCodeColumn;
    @FXML private TableColumn<PileDisplacementData, Number> initialElevationColumn;
    @FXML private TableColumn<PileDisplacementData, Number> previousElevationColumn;
    @FXML private TableColumn<PileDisplacementData, Number> currentElevationColumn;
    @FXML private TableColumn<PileDisplacementData, Number> currentChangeColumn;
    @FXML private TableColumn<PileDisplacementData, Number> cumulativeChangeColumn;
    @FXML private TableColumn<PileDisplacementData, Number> changeRateColumn;
    @FXML private TableColumn<PileDisplacementData, String> mileageColumn;
    @FXML private TableColumn<PileDisplacementData, Number> historicalCumulativeColumn;
    
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
    
    private ObservableList<PileDisplacementData> settlementDataList = FXCollections.observableArrayList();
    private Stage stage;
    
    // 所有测点的所有历史数据，结构为：<测点编号, <测量日期, 数据>>
    private Map<String, Map<LocalDate, PileDisplacementData>> allPointDataMap = new HashMap<>();
    
    // 数据块映射，每个数据块对应一次上传的数据，结构为：<时间戳, 数据列表>
    private Map<LocalDateTime, List<PileDisplacementData>> dataBlocksMap = new HashMap<>();
    
    // 数据块复选框映射
    private Map<LocalDateTime, CheckBox> dataBlockCheckBoxMap = new HashMap<>();
    
    // 当前选中的数据块
    private List<LocalDateTime> selectedDataBlocks = new ArrayList<>();
    
    // 配置的测点列表
    private List<PileDisplacementPoint> configuredPoints = new ArrayList<>();
    
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
        settlementDataList.addListener((javafx.collections.ListChangeListener.Change<? extends PileDisplacementData> c) -> {
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
    }
    
    private void configureNumberColumns() {
        // 高程数据保留4位小数
        initialElevationColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getInitialElevation()));
        initialElevationColumn.setCellFactory(column -> new TableCell<PileDisplacementData, Number>() {
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
        
        previousElevationColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getPreviousElevation()));
        previousElevationColumn.setCellFactory(column -> new TableCell<PileDisplacementData, Number>() {
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
        
        currentElevationColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getCurrentElevation()));
        currentElevationColumn.setCellFactory(column -> new TableCell<PileDisplacementData, Number>() {
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
        
        // 变化量数据保留2位小数，并根据正负值显示不同颜色
        currentChangeColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getCurrentChange()));
        currentChangeColumn.setCellFactory(column -> new TableCell<PileDisplacementData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    // 将米转换为毫米显示
                    double value = item.doubleValue() * 1000;
                    setText(String.format("%.2f", value));
                    
                    // 根据数值设置颜色：负值(上升)为绿色，正值(下沉)为红色
                    if (value < 0) {
                        setTextFill(Color.GREEN);
                    } else if (value > 0) {
                        setTextFill(Color.RED);
                    } else {
                        setTextFill(Color.BLACK);
                    }
                }
            }
        });
        
        cumulativeChangeColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getCumulativeChange()));
        cumulativeChangeColumn.setCellFactory(column -> new TableCell<PileDisplacementData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    // 将米转换为毫米显示
                    double value = item.doubleValue() * 1000;
                    setText(String.format("%.2f", value));
                    
                    // 根据数值设置颜色：负值(上升)为绿色，正值(下沉)为红色
                    if (value < 0) {
                        setTextFill(Color.GREEN);
                    } else if (value > 0) {
                        setTextFill(Color.RED);
                    } else {
                        setTextFill(Color.BLACK);
                    }
                }
            }
        });
        
        changeRateColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getChangeRate()));
        changeRateColumn.setCellFactory(column -> new TableCell<PileDisplacementData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    // 将米/天转换为毫米/天显示
                    double value = item.doubleValue() * 1000;
                    setText(String.format("%.2f", value));
                    
                    // 根据数值设置颜色：负值(上升)为绿色，正值(下沉)为红色
                    if (value < 0) {
                        setTextFill(Color.GREEN);
                    } else if (value > 0) {
                        setTextFill(Color.RED);
                    } else {
                        setTextFill(Color.BLACK);
                    }
                }
            }
        });
        
        mileageColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMileage()));
        
        historicalCumulativeColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getHistoricalCumulative()));
        historicalCumulativeColumn.setCellFactory(column -> new TableCell<PileDisplacementData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // 将米转换为毫米显示
                    setText(String.format("%.2f", item.doubleValue() * 1000));
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
        displacementChart.setTitle("桩顶竖向位移变化量");
        displacementChart.setCreateSymbols(true);
        displacementChart.setAnimated(false);
        
        // 速率图表
        rateXAxis = new CategoryAxis();
        rateYAxis = new NumberAxis();
        rateXAxis.setLabel("测点编号");
        rateYAxis.setLabel("变化速率(mm/天)");
        rateChart = new LineChart<>(rateXAxis, rateYAxis);
        rateChart.setTitle("桩顶竖向位移变化速率");
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
        if (settlementDataList.isEmpty() || displacementChart == null || rateChart == null) {
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
        for (PileDisplacementData data : settlementDataList) {
            String pointId = data.getPointCode();
            
            // 累计变化量(米转毫米)
            displacementSeries.getData().add(new XYChart.Data<>(pointId, data.getCumulativeChange() * 1000));
            
            // 本次变化量(米转毫米)
            currentChangeSeries.getData().add(new XYChart.Data<>(pointId, data.getCurrentChange() * 1000));
            
            // 变化速率(米/天转毫米/天)
            rateSeries.getData().add(new XYChart.Data<>(pointId, data.getChangeRate() * 1000));
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
     * 加载配置的测点数据并初始化表格
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
        for (PileDisplacementPoint point : configuredPoints) {
            PileDisplacementData data = new PileDisplacementData();
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
     * 加载测点配置信息
     */
    private void loadSettlementPoints() {
        // 此处可以添加从文件或数据库加载测点配置的代码
        // 现在我们只添加几个示例测点
        if (configuredPoints.isEmpty()) {
           }
        
        // 更新测点计数显示
        updatePointCount();
        
        // 初始化表格
        updateTableWithInitialData();
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

            String sheetName = "桩顶竖向位移";

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

            // 将数据转换为PileDisplacementData对象
            List<PileDisplacementData> uploadedData = processExcelPointData(pointElevationMap, uploadDate);

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
     * 处理数据的日期冲突
     */
    private void handleDataDateConflict(LocalDate date, List<PileDisplacementData> data, String fileName, int dataCount) {
        // 使用当前系统时间创建唯一的数据块ID (LocalDateTime)
        LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.now());

        // 更新数据的测量日期
        for (PileDisplacementData item : data) {
            item.setMeasurementDate(date);
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
     * 处理数据块选择事件
     */
    private void handleDataBlockSelection(CheckBox checkBox) {
        LocalDateTime dateTime = (LocalDateTime) checkBox.getUserData();
        
        if (checkBox.isSelected()) {
            // 如果选中了数据块，添加到选中列表
            if (!selectedDataBlocks.contains(dateTime)) {
                selectedDataBlocks.add(dateTime);
            }
        } else {
            // 如果取消选中，从列表中移除
            selectedDataBlocks.remove(dateTime);
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
     * 添加数据块到UI
     */
    private void addDataBlock(String fileName, LocalDateTime dateTime) {
        // 创建数据块容器
        HBox dataBlock = new HBox(10);
        dataBlock.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 5; -fx-background-color: #f8f8f8; -fx-padding: 10;");
        dataBlock.setPrefHeight(40);

        // 创建选择框
        CheckBox checkBox = new CheckBox();
        checkBox.setUserData(dateTime);

        // 设置选择框的事件处理
        checkBox.setOnAction(e -> handleDataBlockSelection(checkBox));
        
        // 创建日期标签
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        Label dateLabel = new Label(dateTime.format(formatter));
        dateLabel.setPrefWidth(150);
        
        // 将组件添加到数据块，不再显示文件名标签
        dataBlock.getChildren().addAll(checkBox, dateLabel);
        
        // 添加到流式面板
        dataBlocksFlowPane.getChildren().add(dataBlock);
        
        // 保存复选框引用
        dataBlockCheckBoxMap.put(dateTime, checkBox);
    }

    /**
     * 根据选择的数据块更新表格
     */
    private void updateTableBasedOnSelection() {
        // 如果没有选择任何数据块，显示初始数据
        if (selectedDataBlocks.isEmpty()) {
            updateTableWithInitialData();
            return;
        }

        // 清空当前表格数据
        settlementDataList.clear();

        // 根据选择的数据块数量处理
        if (selectedDataBlocks.size() == 1) {
            // 只选择了一个数据块，将其作为当前高程
            LocalDateTime currentDateTime = selectedDataBlocks.get(0);
            List<PileDisplacementData> currentData = dataBlocksMap.get(currentDateTime);

            // 创建按测点排序索引排序的数据列表
            Map<String, PileDisplacementData> dataMap = new HashMap<>();
            for (PileDisplacementData data : currentData) {
                dataMap.put(data.getPointCode(), data);
            }

            // 按照orderIndex排序测点，确保与Excel导入顺序一致
            List<PileDisplacementPoint> sortedPoints = new ArrayList<>(configuredPoints);
            sortedPoints.sort((p1, p2) -> Integer.compare(p1.getOrderIndex(), p2.getOrderIndex()));

            // 按排序后的测点顺序添加数据
            for (PileDisplacementPoint pointConfig : sortedPoints) {
                String pointCode = pointConfig.getPointId();
                PileDisplacementData data = dataMap.get(pointCode);

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

            List<PileDisplacementData> previousData = dataBlocksMap.get(previousDateTime);
            List<PileDisplacementData> currentData = dataBlocksMap.get(currentDateTime);

            // 创建测点映射以便快速查找
            Map<String, PileDisplacementData> previousDataMap = previousData.stream()
                    .collect(Collectors.toMap(PileDisplacementData::getPointCode, data -> data));

            // 创建按测点排序索引排序的数据列表
            Map<String, PileDisplacementData> currentDataMap = new HashMap<>();
            for (PileDisplacementData data : currentData) {
                currentDataMap.put(data.getPointCode(), data);
            }

            // 按照orderIndex排序测点，确保与Excel导入顺序一致
            List<PileDisplacementPoint> sortedPoints = new ArrayList<>(configuredPoints);
            sortedPoints.sort((p1, p2) -> Integer.compare(p1.getOrderIndex(), p2.getOrderIndex()));

            // 按排序后的测点顺序添加数据
            for (PileDisplacementPoint pointConfig : sortedPoints) {
                String pointCode = pointConfig.getPointId();
                PileDisplacementData currentItem = currentDataMap.get(pointCode);

                if (currentItem != null) {
                    // 使用配置中的初始高程，而不是数据中的
                    currentItem.setInitialElevation(pointConfig.getInitialElevation());

                    // 查找该测点的上一次记录
                    PileDisplacementData previousItem = previousDataMap.get(pointCode);

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

        // 更新图表
        updateChart();
    }

    /**
     * 处理从Excel导入的点位高程数据并转换为PileDisplacementData对象
     */
    private List<PileDisplacementData> processExcelPointData(Map<String, Double> pointElevationMap, LocalDate measureDate) {
        List<PileDisplacementData> result = new ArrayList<>();

        // 处理Excel数据
        for (Map.Entry<String, Double> entry : pointElevationMap.entrySet()) {
            String pointCode = entry.getKey();
            double currentElevation = entry.getValue();

            // 查找该测点的配置信息
            PileDisplacementPoint pointConfig = configuredPoints.stream()
                    .filter(p -> p.getPointId().equals(pointCode))
                    .findFirst()
                    .orElse(null);

            if (pointConfig == null) {
                // 如果没有找到测点配置，创建一个新的配置
                pointConfig = new PileDisplacementPoint();
                pointConfig.setPointId(pointCode);
                pointConfig.setInitialElevation(currentElevation);
                configuredPoints.add(pointConfig);
            }

            // 查找该测点的历史数据
            Map<LocalDate, PileDisplacementData> pointHistory = allPointDataMap.getOrDefault(pointCode, new HashMap<>());

            // 获取初始高程和前期高程
            double initialElevation = pointConfig.getInitialElevation();
            double previousElevation = initialElevation;

            if (!pointHistory.isEmpty()) {
                // 查找最近的记录作为前期高程
                LocalDate latestDate = null;
                for (LocalDate date : pointHistory.keySet()) {
                    if (date.isBefore(measureDate) && (latestDate == null || date.isAfter(latestDate))) {
                        latestDate = date;
                    }
                }

                if (latestDate != null) {
                    previousElevation = pointHistory.get(latestDate).getCurrentElevation();
                }
            }

            // 创建PileDisplacementData对象
            PileDisplacementData data = new PileDisplacementData(pointCode, initialElevation, previousElevation, currentElevation, measureDate);
            if (pointConfig.getMileage() != null) {
                data.setMileage(pointConfig.getMileage());
            }

            // 使用改进的方法计算变化速率，考虑实际测量日期
            LocalDate previousDate = null;
            if (!pointHistory.isEmpty()) {
                // 查找最近的记录作为前期测量日期
                LocalDate latestDate = null;
                for (LocalDate date : pointHistory.keySet()) {
                    if (date.isBefore(measureDate) && (latestDate == null || date.isAfter(latestDate))) {
                        latestDate = date;
                    }
                }
                previousDate = latestDate;
            }

            // 获取自定义天数设置（如果有的话）
            int customDays = getCustomDaysForRateCalculation();

            // 计算派生值，使用实际测量日期或自定义天数
            data.calculateDerivedValues(previousDate, measureDate, customDays);

            // 添加到结果列表和历史数据映射
            result.add(data);

            if (!allPointDataMap.containsKey(pointCode)) {
                allPointDataMap.put(pointCode, new HashMap<>());
            }
            allPointDataMap.get(pointCode).put(measureDate, data);
        }

        return result;
    }

    /**
     * 处理导出按钮事件
     */
    @FXML
    private void handleExportButtonAction(ActionEvent event) {
        if (settlementDataList.isEmpty()) {
            AlertUtil.showWarning("导出失败", "没有数据可导出");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出桩顶竖向位移数据");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Excel 文件", "*.xlsx")
        );
        fileChooser.setInitialFileName("桩顶竖向位移数据导出");

        File selectedFile = fileChooser.showSaveDialog(stage);
        if (selectedFile != null) {
            try {
                // 准备表头
                List<String> headers = new ArrayList<>();
                headers.add("测点编号");
                headers.add("里程");
                headers.add("初始高程(m)");
                headers.add("前期高程(m)");
                headers.add("本期高程(m)");
                headers.add("本期变化量(mm)");
                headers.add("累计变化量(mm)");
                headers.add("变化速率(mm/d)");
                headers.add("历史累计(mm)");
                headers.add("测量日期");

                // 准备数据
                List<List<String>> data = new ArrayList<>();
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                for (PileDisplacementData item : settlementDataList) {
                    List<String> row = new ArrayList<>();
                    row.add(item.getPointCode());
                    row.add(item.getMileage() != null ? item.getMileage() : "");
                    row.add(String.format("%.4f", item.getInitialElevation()));
                    row.add(String.format("%.4f", item.getPreviousElevation()));
                    row.add(String.format("%.4f", item.getCurrentElevation()));
                    row.add(String.format("%.2f", item.getCurrentChange()));
                    row.add(String.format("%.2f", item.getCumulativeChange()));
                    row.add(String.format("%.2f", item.getChangeRate()));
                    row.add(String.format("%.2f", item.getHistoricalCumulative()));
                    row.add(item.getMeasurementDate() != null ? item.getMeasurementDate().format(dateFormatter) : "");
                    
                    data.add(row);
                }

                // 导出到Excel
                ExcelUtil.exportToExcel(selectedFile, "桩顶竖向位移", headers, data);
                
                AlertUtil.showInformation("导出成功", "成功导出数据到 " + selectedFile.getName());
            } catch (IOException e) {
                AlertUtil.showError("导出失败", "无法导出数据: " + e.getMessage());
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
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/dialogs/PileTopDisplacementPointSettingsDialog.fxml"));
            Parent root = loader.load();

            // 创建对话框
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.WINDOW_MODAL);
            if (stage != null) {
                dialogStage.initOwner(stage);
            }
            dialogStage.setTitle("桩顶竖向位移测点设置");
            dialogStage.setResizable(true);
            
            // 创建场景
            Scene scene = new Scene(root);
            dialogStage.setScene(scene);

            // 获取控制器
            PileTopDisplacementPointSettingsController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            
            // 传入当前测点数据
            controller.setInitialData(configuredPoints);

            // 显示对话框并等待用户响应
            dialogStage.showAndWait();
            
            // 获取更新后的测点数据
            List<PileDisplacementPoint> updatedPoints = controller.getPoints();
            if (updatedPoints != null) {
                configuredPoints.clear();
                configuredPoints.addAll(updatedPoints);
                
                // 更新测点数量显示
                updatePointCount();
                
                // 更新表格数据
                updateTableWithInitialData();
            }
        } catch (IOException e) {
            AlertUtil.showError("错误", "无法打开测点设置对话框: " + e.getMessage());
            e.printStackTrace();
        }
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
     * 获取要保存的测量记录
     */
    public List<MeasurementRecord> getMeasurementRecordsForSaving() {
        List<MeasurementRecord> records = new ArrayList<>();
        // 实现获取测量记录的逻辑
        return records;
    }

    /**
     * 获取数据存储对象
     */
    public PileDisplacementDataStorage getSettlementDataStorage() {
        PileDisplacementDataStorage storage = new PileDisplacementDataStorage();
        
        // 设置测点配置
        storage.setPoints(new ArrayList<>(configuredPoints));
        
        // 设置数据块
        for (Map.Entry<LocalDateTime, List<PileDisplacementData>> entry : dataBlocksMap.entrySet()) {
            LocalDateTime timestamp = entry.getKey();
            List<PileDisplacementData> dataList = entry.getValue();
            
            // 使用时间戳的格式化字符串作为描述
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String description = timestamp.format(formatter);
            
            storage.addDataBlock(timestamp, dataList, description);
        }
        
        // 设置选定的数据块
        storage.setSelectedDataBlocks(new ArrayList<>(selectedDataBlocks));
        
        // 设置自定义速率计算天数
        storage.setCustomDaysForRateCalculation(customDaysForRateCalculation);
        
        return storage;
    }

    /**
     * 从测量记录加载数据
     */
    public void loadFromMeasurementRecords(List<MeasurementRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        
        // 创建测点数据映射
        Map<String, Map<LocalDate, PileDisplacementData>> pointDataMap = new HashMap<>();
        
        // 整理测量记录
        for (MeasurementRecord record : records) {
            String pointId = record.getId();
            LocalDate measureDate = record.getMeasureTime().toLocalDate();
            double elevation = record.getValue();
            
            // 查找或创建测点配置
            PileDisplacementPoint pointConfig = configuredPoints.stream()
                .filter(p -> p.getPointId().equals(pointId))
                .findFirst()
                .orElse(null);
                
            if (pointConfig == null) {
                pointConfig = new PileDisplacementPoint();
                pointConfig.setPointId(pointId);
                pointConfig.setInitialElevation(elevation);
                configuredPoints.add(pointConfig);
            }
            
            // 创建测点数据
            PileDisplacementData data = new PileDisplacementData();
            data.setPointCode(pointId);
            data.setInitialElevation(pointConfig.getInitialElevation());
            data.setCurrentElevation(elevation);
            data.setMeasurementDate(measureDate);
            
            // 添加到映射
            if (!pointDataMap.containsKey(pointId)) {
                pointDataMap.put(pointId, new HashMap<>());
            }
            pointDataMap.get(pointId).put(measureDate, data);
        }
        
        // 按日期分组创建数据块
        Map<LocalDate, List<PileDisplacementData>> dateGroupedData = new HashMap<>();
        
        // 遍历所有测点数据
        for (Map.Entry<String, Map<LocalDate, PileDisplacementData>> pointEntry : pointDataMap.entrySet()) {
            for (Map.Entry<LocalDate, PileDisplacementData> dataEntry : pointEntry.getValue().entrySet()) {
                LocalDate date = dataEntry.getKey();
                PileDisplacementData data = dataEntry.getValue();
                
                if (!dateGroupedData.containsKey(date)) {
                    dateGroupedData.put(date, new ArrayList<>());
                }
                
                dateGroupedData.get(date).add(data);
            }
        }
        
        // 创建数据块
        for (Map.Entry<LocalDate, List<PileDisplacementData>> entry : dateGroupedData.entrySet()) {
            LocalDate date = entry.getKey();
            List<PileDisplacementData> dataList = entry.getValue();
            
            // 创建时间戳
            LocalDateTime timestamp = LocalDateTime.of(date, LocalTime.of(0, 0));
            
            // 保存数据块
            dataBlocksMap.put(timestamp, dataList);
            
            // 更新UI
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            addDataBlock("从测量记录导入 " + date.format(formatter), timestamp);
        }
        
        // 更新测点计数
        updatePointCount();
        
        // 如果有数据块，选择最新的一个
        if (!dataBlocksMap.isEmpty()) {
            LocalDateTime latestTimestamp = dataBlocksMap.keySet().stream()
                .max(LocalDateTime::compareTo)
                .orElse(null);
                
            if (latestTimestamp != null) {
                selectedDataBlocks.clear();
                selectedDataBlocks.add(latestTimestamp);
                
                // 更新UI选择状态
                CheckBox checkBox = dataBlockCheckBoxMap.get(latestTimestamp);
                if (checkBox != null) {
                    checkBox.setSelected(true);
                }
                
                // 更新表格
                updateTableBasedOnSelection();
            }
        }
    }

    /**
     * 从桩顶位移数据存储对象加载数据 (符合规范的新方法名)
     * 该方法是loadFromSettlementDataStorage的包装器，提供更符合命名规范的接口
     * @param storage 桩顶位移数据存储对象
     */
    public void loadFromPileDisplacementDataStorage(PileDisplacementDataStorage storage) {
        // 调用现有方法以保持向后兼容性
        loadFromSettlementDataStorage(storage);
    }

    /**
     * 从数据存储对象加载数据
     */
    public void loadFromSettlementDataStorage(PileDisplacementDataStorage storage) {
        if (storage == null) {
            return;
        }
        
        // 清空现有数据
        dataBlocksMap.clear();
        allPointDataMap.clear();
        selectedDataBlocks.clear();
        configuredPoints.clear();
        dataBlocksFlowPane.getChildren().clear();
        dataBlockCheckBoxMap.clear();
        
        // 加载测点配置
        configuredPoints.addAll(storage.getPoints());
        
        // 加载数据块
        for (LocalDateTime timestamp : storage.getDataBlockTimestamps()) {
            List<PileDisplacementData> dataList = storage.getDataBlock(timestamp);
            String description = storage.getDataBlockDescription(timestamp);
            
            // 添加数据块
            dataBlocksMap.put(timestamp, dataList);
            addDataBlock(description != null ? description : "导入数据", timestamp);
            
            // 更新测点历史数据映射
            for (PileDisplacementData data : dataList) {
                String pointCode = data.getPointCode();
                LocalDate date = data.getMeasurementDate();
                
                if (!allPointDataMap.containsKey(pointCode)) {
                    allPointDataMap.put(pointCode, new HashMap<>());
                }
                
                allPointDataMap.get(pointCode).put(date, data);
            }
        }
        
        // 设置选定的数据块
        List<LocalDateTime> selected = storage.getSelectedDataBlocks();
        for (LocalDateTime timestamp : selected) {
            if (dataBlocksMap.containsKey(timestamp)) {
                selectedDataBlocks.add(timestamp);
                
                // 更新UI选择状态
                CheckBox checkBox = dataBlockCheckBoxMap.get(timestamp);
                if (checkBox != null) {
                    checkBox.setSelected(true);
                }
            }
        }
        
        // 设置自定义速率计算天数
        customDaysForRateCalculation = storage.getCustomDaysForRateCalculation();
        
        // 更新测点计数
        updatePointCount();
        
        // 更新表格
        if (!selectedDataBlocks.isEmpty()) {
            updateTableBasedOnSelection();
        } else {
            updateTableWithInitialData();
        }
    }

    /**
     * 判断数据是否变化
     */
    public boolean hasDataChanged() {
        return !dataBlocksMap.isEmpty();
    }

    /**
     * 获取速率计算天数
     */
    public int getCustomDaysForRateCalculation() {
        return customDaysForRateCalculation;
    }

    /**
     * 设置速率计算天数
     */
    public void setCustomDaysForRateCalculation(int days) {
        if (days >= 0) {
            this.customDaysForRateCalculation = days;
            // 重新计算速率
            updateTableBasedOnSelection();
        }
    }

    /**
     * 显示设置速率计算天数对话框
     */
    private void showSetCustomDaysDialog() {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(customDaysForRateCalculation));
        dialog.setTitle("设置变化速率计算天数");
        dialog.setHeaderText("请输入计算变化速率的天数");
        dialog.setContentText("天数 (0表示使用实际间隔天数):");
        
        // 设置样式
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        
        // 添加确认按钮事件处理
        dialog.showAndWait().ifPresent(result -> {
            try {
                int days = Integer.parseInt(result);
                if (days >= 0) {
                    setCustomDaysForRateCalculation(days);
                    AlertUtil.showInformation("设置成功", 
                            days > 0 ? "变化速率将按 " + days + " 天计算" : "变化速率将按实际间隔天数计算");
                } else {
                    AlertUtil.showWarning("输入错误", "请输入不小于0的整数");
                }
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("输入错误", "请输入有效的整数");
            }
        });
    }
} 