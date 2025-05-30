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

import com.monitor.model.SettlementData;
import com.monitor.model.SettlementPoint;
import com.monitor.model.MeasurementRecord;
import com.monitor.model.SettlementDataStorage;
import com.monitor.model.SettlementDataStorage.SettlementDataWrapper;
import com.monitor.util.AlertUtil;
import com.monitor.util.ExcelUtil;
import com.monitor.view.SettlementPointSettingsController;

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
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
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
import javafx.scene.control.ButtonType;
import javafx.scene.control.ToggleGroup;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public class SettlementDataController {

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

    @FXML private TableView<SettlementData> dataTableView;
    @FXML private TableColumn<SettlementData, Number> serialNumberColumn;
    @FXML private TableColumn<SettlementData, String> pointCodeColumn;
    @FXML private TableColumn<SettlementData, Number> initialElevationColumn;
    @FXML private TableColumn<SettlementData, Number> previousElevationColumn;
    @FXML private TableColumn<SettlementData, Number> currentElevationColumn;
    @FXML private TableColumn<SettlementData, Number> currentChangeColumn;
    @FXML private TableColumn<SettlementData, Number> cumulativeChangeColumn;
    @FXML private TableColumn<SettlementData, Number> changeRateColumn;
    @FXML private TableColumn<SettlementData, String> mileageColumn;
    @FXML private TableColumn<SettlementData, Number> historicalCumulativeColumn;

    @FXML private BorderPane chartContainer;

    // 添加chart相关变量
    private LineChart<String, Number> displacementChart;
    private LineChart<String, Number> rateChart;
    private NumberAxis displacementYAxis;
    private CategoryAxis displacementXAxis;
    private NumberAxis rateYAxis;
    private CategoryAxis rateXAxis;

    // 添加切换按钮
    @FXML private ToggleButton displacementChartButton;
    @FXML private ToggleButton rateChartButton;
    private ToggleGroup chartToggleGroup;

    @FXML private Label pointCountLabel;
    @FXML private Label uploadDateLabel;

    private ObservableList<SettlementData> settlementDataList = FXCollections.observableArrayList();
    private Stage stage;

    // 存储所有已上传的数据块，按点位ID分组
    private Map<String, Map<LocalDate, SettlementData>> allPointDataMap = new HashMap<>();

    // 存储所有数据块，按上传时间索引
    private Map<LocalDateTime, List<SettlementData>> dataBlocksMap = new HashMap<>();

    // 存储数据块与选择框的映射
    private Map<LocalDateTime, CheckBox> dataBlockCheckBoxMap = new HashMap<>();

    // 当前选中的数据块
    private List<LocalDateTime> selectedDataBlocks = new ArrayList<>();

    // 已设置的测点
    private List<SettlementPoint> configuredPoints = new ArrayList<>();

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        // 初始化日期选择器为当前日期
        datePicker.setValue(LocalDate.now());

        // 添加表格数据变化监听器，当数据变化时自动更新图表
        settlementDataList.addListener((javafx.collections.ListChangeListener.Change<? extends SettlementData> c) -> {
            // 当表格数据变化时更新图表
            updateChart();
        });

        // 为表格添加右键菜单
        ContextMenu contextMenu = new ContextMenu();
        MenuItem setCustomDaysItem = new MenuItem("设置变化速率计算天数");
        setCustomDaysItem.setOnAction(e -> showSetCustomDaysDialog());
        contextMenu.getItems().add(setCustomDaysItem);
        dataTableView.setContextMenu(contextMenu);

        // 配置表格列
        serialNumberColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(settlementDataList.indexOf(cellData.getValue()) + 1));
        pointCodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPointCode()));

        // 高程数据保留4位小数
        initialElevationColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getInitialElevation()));
        initialElevationColumn.setCellFactory(column -> new TableCell<SettlementData, Number>() {
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
        previousElevationColumn.setCellFactory(column -> new TableCell<SettlementData, Number>() {
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
        currentElevationColumn.setCellFactory(column -> new TableCell<SettlementData, Number>() {
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

        // 变化量保留1位小数
        currentChangeColumn.setCellValueFactory(cellData ->
            // 显示毫米单位的变化量
            new SimpleDoubleProperty(cellData.getValue().getCurrentChange() * 1000));
        currentChangeColumn.setCellFactory(column -> new TableCell<SettlementData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.1f", item.doubleValue()));
                }
            }
        });

        // 累计变化量保留1位小数
        cumulativeChangeColumn.setCellValueFactory(cellData ->
            // 显示毫米单位的累计变化量
            new SimpleDoubleProperty(cellData.getValue().getCumulativeChange() * 1000));
        cumulativeChangeColumn.setCellFactory(column -> new TableCell<SettlementData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.1f", item.doubleValue()));
                }
            }
        });

        // 变化速率保留2位小数
        changeRateColumn.setCellValueFactory(cellData ->
            // 显示毫米单位的变化速率
            new SimpleDoubleProperty(cellData.getValue().getChangeRate() * 1000));
        changeRateColumn.setCellFactory(column -> new TableCell<SettlementData, Number>() {
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

        mileageColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMileage()));
        historicalCumulativeColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getHistoricalCumulative()));
        historicalCumulativeColumn.setCellFactory(column -> new TableCell<SettlementData, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.1f", item.doubleValue()));
                }
            }
        });

        // 设置数据源
        dataTableView.setItems(settlementDataList);

        // 禁用表格列排序，确保按照测点的导入顺序显示
        serialNumberColumn.setSortable(false);
        pointCodeColumn.setSortable(false);
        initialElevationColumn.setSortable(false);
        previousElevationColumn.setSortable(false);
        currentElevationColumn.setSortable(false);
        currentChangeColumn.setSortable(false);
        cumulativeChangeColumn.setSortable(false);
        changeRateColumn.setSortable(false);
        mileageColumn.setSortable(false);
        historicalCumulativeColumn.setSortable(false);

        // 初始化图表
        initializeCharts();

        // 给dataBlocksFlowPane设置样式
        dataBlocksFlowPane.setHgap(10);
        dataBlocksFlowPane.setVgap(10);

        // 加载初始测点数据（如果有）
        loadSettlementPoints();
    }

    /**
     * 初始化两个图表
     */
    private void initializeCharts() {
        // 位移变化量图表
        displacementXAxis = new CategoryAxis();
        displacementYAxis = new NumberAxis(-50, 50, 10);
        displacementChart = new LineChart<>(displacementXAxis, displacementYAxis);

        displacementChart.setTitle("地表沉降曲线图-位移变化量");
        displacementXAxis.setLabel("测点编号");
        displacementYAxis.setLabel("变化量(mm)");
        displacementChart.setLegendVisible(true);
        displacementChart.setAnimated(false);

        // 设置X轴标签旋转
        displacementChart.getXAxis().setTickLabelRotation(90);

        // 变化速率图表
        rateXAxis = new CategoryAxis();
        rateYAxis = new NumberAxis(-5, 5, 1);
        rateChart = new LineChart<>(rateXAxis, rateYAxis);

        rateChart.setTitle("地表沉降曲线-变化速率(mm/d)");
        rateXAxis.setLabel("测点编号");
        rateYAxis.setLabel("变化速率(mm/d)");
        rateChart.setLegendVisible(true);
        rateChart.setAnimated(false);

        // 设置X轴标签旋转
        rateChart.getXAxis().setTickLabelRotation(90);

        // 默认显示位移变化量图表
        showDisplacementChart();
    }

    /**
     * 显示位移变化量图表
     */
    private void showDisplacementChart() {
        chartContainer.setCenter(displacementChart);
        if (displacementChartButton != null) {
            displacementChartButton.setSelected(true);
        }
    }

    /**
     * 显示变化速率图表
     */
    private void showRateChart() {
        chartContainer.setCenter(rateChart);
        if (rateChartButton != null) {
            rateChartButton.setSelected(true);
        }
    }

    /**
     * 更新图表数据
     */
    private void updateChart() {
        // 清空图表数据
        displacementChart.getData().clear();
        rateChart.getData().clear();

        if (settlementDataList.isEmpty()) {
            return;
        }

        // 创建位移变化量数据系列
        XYChart.Series<String, Number> currentChangeSeries = new XYChart.Series<>();
        currentChangeSeries.setName("本次变化量");

        XYChart.Series<String, Number> cumulativeChangeSeries = new XYChart.Series<>();
        cumulativeChangeSeries.setName("累计变化量");

        // 创建变化速率数据系列
        XYChart.Series<String, Number> rateSeries = new XYChart.Series<>();
        rateSeries.setName("变化速率");

        // 填充数据
        for (SettlementData data : settlementDataList) {
            // 位移变化量数据
            currentChangeSeries.getData().add(
                new XYChart.Data<>(data.getPointCode(), data.getCurrentChange() * 1000));

            cumulativeChangeSeries.getData().add(
                new XYChart.Data<>(data.getPointCode(), data.getCumulativeChange() * 1000));

            // 变化速率数据
            rateSeries.getData().add(
                new XYChart.Data<>(data.getPointCode(), data.getChangeRate() * 1000));
        }

        // 添加系列到图表
        displacementChart.getData().addAll(currentChangeSeries, cumulativeChangeSeries);
        rateChart.getData().add(rateSeries);
    }

    /**
     * 加载已配置的测点数据，默认仅显示测点信息
     */
    private void loadSettlementPoints() {
        // 初始化为空列表，不再添加测试数据
        configuredPoints = new ArrayList<>();

        // 根据配置的测点显示初始数据
        updateTableWithInitialData();
    }

    /**
     * 根据配置的测点显示初始数据
     */
    private void updateTableWithInitialData() {
        settlementDataList.clear();

        // 按照orderIndex排序测点，确保与Excel导入顺序一致
        List<SettlementPoint> sortedPoints = new ArrayList<>(configuredPoints);
        sortedPoints.sort((p1, p2) -> Integer.compare(p1.getOrderIndex(), p2.getOrderIndex()));

        for (SettlementPoint point : sortedPoints) {
            SettlementData data = new SettlementData();
            data.setPointCode(point.getPointId());
            data.setInitialElevation(point.getInitialElevation());
            data.setPreviousElevation(point.getInitialElevation());
            data.setCurrentElevation(point.getInitialElevation());
            data.setMileage(point.getMileage());
            data.setMeasurementDate(LocalDate.now());
            data.calculateDerivedValues();
            settlementDataList.add(data);
        }

        updatePointCount();

        // 更新图表
        updateChart();
    }

    @FXML
    private void handleUploadButtonAction(ActionEvent event) {
        // 创建文件选择器
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择沉降数据文件");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Excel 文件", "*.xlsx", "*.xls")
        );

        // 设置初始目录为示例数据目录（如果存在）
        File samplesDir = new File("src/main/resources/samples");
        if (samplesDir.exists() && samplesDir.isDirectory()) {
            fileChooser.setInitialDirectory(samplesDir);
        }

        // 打开文件选择对话框
        File selectedFile = fileChooser.showOpenDialog(stage);

        // 如果用户取消了选择，则直接返回
        if (selectedFile == null) {
            return;
        }

        // 处理选中的文件
        processSelectedExcelFile(selectedFile);
    }

    /**
     * 处理选中的Excel文件
     *
     * @param file 用户选择的Excel文件
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

            // 将数据转换为SettlementData对象
            List<SettlementData> uploadedData = processExcelPointData(pointElevationMap, uploadDate);

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
    private void handleDataDateConflict(LocalDate date, List<SettlementData> data, String fileName, int dataCount) {
        // 使用当前系统时间创建唯一的数据块ID (LocalDateTime)
        LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.now());

        // 更新数据的测量日期
        for (SettlementData item : data) {
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
     * 处理从Excel导入的点位高程数据并转换为SettlementData对象
     */
    private List<SettlementData> processExcelPointData(Map<String, Double> pointElevationMap, LocalDate measureDate) {
        List<SettlementData> result = new ArrayList<>();

        // 处理Excel数据
        for (Map.Entry<String, Double> entry : pointElevationMap.entrySet()) {
            String pointCode = entry.getKey();
            double currentElevation = entry.getValue();

            // 查找该测点的配置信息
            SettlementPoint pointConfig = configuredPoints.stream()
                    .filter(p -> p.getPointId().equals(pointCode))
                    .findFirst()
                    .orElse(null);

            if (pointConfig == null) {
                // 如果没有找到测点配置，创建一个新的配置
                pointConfig = new SettlementPoint();
                pointConfig.setPointId(pointCode);
                pointConfig.setInitialElevation(currentElevation);
                configuredPoints.add(pointConfig);
            }

            // 查找该测点的历史数据
            Map<LocalDate, SettlementData> pointHistory = allPointDataMap.getOrDefault(pointCode, new HashMap<>());

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

            // 创建SettlementData对象
            SettlementData data = new SettlementData(pointCode, initialElevation, previousElevation, currentElevation, measureDate);
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

        // 创建日期和文件名标签，显示到秒
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDate = dateTime.format(formatter);
        Label dateLabel = new Label(formattedDate);

        // 将组件添加到数据块
        dataBlock.getChildren().addAll(checkBox, dateLabel);

        // 创建右键菜单
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("删除");
        deleteItem.setOnAction(e -> {
            removeDataBlock(dateTime);
            dataBlocksFlowPane.getChildren().remove(dataBlock);

            // 更新表格
            if (selectedDataBlocks.contains(dateTime)) {
                selectedDataBlocks.remove(dateTime);
                updateTableBasedOnSelection();
            }
        });
        contextMenu.getItems().add(deleteItem);

        // 将右键菜单绑定到数据块
        dataBlock.setOnContextMenuRequested(e -> {
            contextMenu.show(dataBlock, e.getScreenX(), e.getScreenY());
        });

        // 添加到FlowPane
        dataBlocksFlowPane.getChildren().add(dataBlock);

        // 保存选择框映射
        dataBlockCheckBoxMap.put(dateTime, checkBox);
    }

    /**
     * 处理数据块选择事件
     */
    private void handleDataBlockSelection(CheckBox checkBox) {
        LocalDateTime blockDateTime = (LocalDateTime) checkBox.getUserData();

        if (checkBox.isSelected()) {
            // 如果已经选择了两个数据块，取消最早选择的那个
            if (selectedDataBlocks.size() >= 2) {
                LocalDateTime oldestDateTime = selectedDataBlocks.get(0);
                CheckBox oldestCheckBox = dataBlockCheckBoxMap.get(oldestDateTime);
                oldestCheckBox.setSelected(false);
                selectedDataBlocks.remove(0);
            }

            // 添加新选择的数据块
            selectedDataBlocks.add(blockDateTime);
        } else {
            // 移除取消选择的数据块
            selectedDataBlocks.remove(blockDateTime);
        }

        // 基于选择的数据块更新表格
        updateTableBasedOnSelection();
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
            List<SettlementData> currentData = dataBlocksMap.get(currentDateTime);

            // 创建按测点排序索引排序的数据列表
            Map<String, SettlementData> dataMap = new HashMap<>();
            for (SettlementData data : currentData) {
                dataMap.put(data.getPointCode(), data);
            }

            // 按照orderIndex排序测点，确保与Excel导入顺序一致
            List<SettlementPoint> sortedPoints = new ArrayList<>(configuredPoints);
            sortedPoints.sort((p1, p2) -> Integer.compare(p1.getOrderIndex(), p2.getOrderIndex()));

            // 按排序后的测点顺序添加数据
            for (SettlementPoint pointConfig : sortedPoints) {
                String pointCode = pointConfig.getPointId();
                SettlementData data = dataMap.get(pointCode);

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

            List<SettlementData> previousData = dataBlocksMap.get(previousDateTime);
            List<SettlementData> currentData = dataBlocksMap.get(currentDateTime);

            // 创建测点映射以便快速查找
            Map<String, SettlementData> previousDataMap = previousData.stream()
                    .collect(Collectors.toMap(SettlementData::getPointCode, data -> data));

            // 创建按测点排序索引排序的数据列表
            Map<String, SettlementData> currentDataMap = new HashMap<>();
            for (SettlementData data : currentData) {
                currentDataMap.put(data.getPointCode(), data);
            }

            // 按照orderIndex排序测点，确保与Excel导入顺序一致
            List<SettlementPoint> sortedPoints = new ArrayList<>(configuredPoints);
            sortedPoints.sort((p1, p2) -> Integer.compare(p1.getOrderIndex(), p2.getOrderIndex()));

            // 按排序后的测点顺序添加数据
            for (SettlementPoint pointConfig : sortedPoints) {
                String pointCode = pointConfig.getPointId();
                SettlementData currentItem = currentDataMap.get(pointCode);

                if (currentItem != null) {
                    // 使用配置中的初始高程，而不是数据中的
                    currentItem.setInitialElevation(pointConfig.getInitialElevation());

                    // 查找该测点的上一次记录
                    SettlementData previousItem = previousDataMap.get(pointCode);

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

    @FXML
    private void handleExportButtonAction(ActionEvent event) {
        if (settlementDataList.isEmpty()) {
            AlertUtil.showWarning("导出失败", "没有数据可导出");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出沉降数据");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Excel 文件", "*.xlsx")
        );
        fileChooser.setInitialFileName("沉降数据_" + datePicker.getValue().toString() + ".xlsx");
        File saveFile = fileChooser.showSaveDialog(stage);

        if (saveFile != null) {
            try {
                // 准备表头和数据
                List<String> headers = new ArrayList<>();
                headers.add("序号");
                headers.add("测点编号");
                headers.add("初始高程");
                headers.add("前期高程");
                headers.add("本期高程");
                headers.add("本期变化 (mm)");
                headers.add("累计变化 (mm)");
                headers.add("变化速率 (mm/天)");
                headers.add("里程");

                List<List<String>> data = new ArrayList<>();
                for (int i = 0; i < settlementDataList.size(); i++) {
                    SettlementData item = settlementDataList.get(i);
                    List<String> row = new ArrayList<>();
                    row.add(String.valueOf(i + 1));
                    row.add(item.getPointCode());
                    row.add(String.valueOf(item.getInitialElevation()));
                    row.add(String.valueOf(item.getPreviousElevation()));
                    row.add(String.valueOf(item.getCurrentElevation()));
                    row.add(String.format("%.2f", item.getCurrentChange() * 1000));
                    row.add(String.format("%.2f", item.getCumulativeChange() * 1000));
                    row.add(String.format("%.2f", item.getChangeRate() * 1000));
                    row.add(item.getMileage() != null ? item.getMileage() : "");
                    data.add(row);
                }

                // 导出Excel
                ExcelUtil.exportToExcel(saveFile, "地表点沉降", headers, data);
                AlertUtil.showInformation("导出成功", "数据已成功导出到: " + saveFile.getAbsolutePath());

            } catch (IOException e) {
                AlertUtil.showError("导出失败", "无法导出数据: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleSettingsButtonAction(ActionEvent event) {
        try {
            // 加载测点设置对话框FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/SettlementPointSettingsDialog.fxml"));
            Parent root = loader.load();

            // 创建对话框Stage
            Stage dialogStage = new Stage();
            dialogStage.setTitle("地表点沉降设置");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(((Node)event.getSource()).getScene().getWindow());
            dialogStage.setScene(new Scene(root));

            // 获取控制器并设置对话框Stage
            SettlementPointSettingsController controller = loader.getController();
            controller.setDialogStage(dialogStage);

            // 加载现有的测点数据
            controller.setInitialData(configuredPoints);

            // 显示对话框并等待用户关闭
            dialogStage.showAndWait();

            // 对话框关闭后，获取更新后的测点数据
            List<SettlementPoint> updatedPoints = controller.getPoints();
            if (updatedPoints != null) {
                configuredPoints = updatedPoints;

                // 更新表格显示
                updateTableWithInitialData();

                // 清空选择
                for (CheckBox checkBox : dataBlockCheckBoxMap.values()) {
                    checkBox.setSelected(false);
                }
                selectedDataBlocks.clear();
            }

        } catch (IOException e) {
            AlertUtil.showError("错误", "无法打开测点设置对话框: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updatePointCount() {
        if (pointCountLabel != null) {
            pointCountLabel.setText("总测点数: " + settlementDataList.size());
        }
    }

    /**
     * 获取当前的测量记录数据，用于保存到项目文件
     *
     * @return 当前所有测量记录列表
     */
    public List<MeasurementRecord> getMeasurementRecordsForSaving() {
        List<MeasurementRecord> recordsToSave = new ArrayList<>();

        // 遍历所有点位数据
        for (Map<LocalDate, SettlementData> dateMap : allPointDataMap.values()) {
            for (SettlementData settlementData : dateMap.values()) {
                // 创建对应的MeasurementRecord
                MeasurementRecord record = new MeasurementRecord();
                record.setId(settlementData.getPointCode());
                record.setValue(settlementData.getCurrentElevation());

                // 转换LocalDate到LocalDateTime
                if (settlementData.getMeasurementDate() != null) {
                    record.setMeasureTime(LocalDateTime.of(settlementData.getMeasurementDate(), LocalTime.NOON));
                }

                record.setUnit("m"); // 高程单位为米

                // 设置预警等级（基于变化率和阈值计算）
                int warningLevel = calculateWarningLevel(settlementData);
                record.setWarningLevel(warningLevel);

                recordsToSave.add(record);
            }
        }

        return recordsToSave;
    }

    /**
     * 获取完整的沉降数据存储对象，包含所有测点设置和上传数据
     *
     * @return 沉降数据存储对象
     */
    public SettlementDataStorage getSettlementDataStorage() {
        SettlementDataStorage storage = new SettlementDataStorage();

        // 保存测点配置
        storage.setConfiguredPoints(new ArrayList<>(configuredPoints));

        // 保存所有测点数据
        Map<String, Map<LocalDate, SettlementDataWrapper>> wrappedPointDataMap = new HashMap<>();
        for (Map.Entry<String, Map<LocalDate, SettlementData>> entry : allPointDataMap.entrySet()) {
            String pointId = entry.getKey();
            Map<LocalDate, SettlementData> dateMap = entry.getValue();

            Map<LocalDate, SettlementDataWrapper> wrappedDateMap = new HashMap<>();
            for (Map.Entry<LocalDate, SettlementData> dateEntry : dateMap.entrySet()) {
                LocalDate date = dateEntry.getKey();
                SettlementData data = dateEntry.getValue();
                wrappedDateMap.put(date, new SettlementDataWrapper(data));
            }

            wrappedPointDataMap.put(pointId, wrappedDateMap);
        }
        storage.setAllPointDataMap(wrappedPointDataMap);

        // 保存数据块映射
        Map<LocalDateTime, List<SettlementDataWrapper>> wrappedDataBlocksMap = new HashMap<>();
        for (Map.Entry<LocalDateTime, List<SettlementData>> entry : dataBlocksMap.entrySet()) {
            LocalDateTime dateTime = entry.getKey();
            List<SettlementData> dataList = entry.getValue();

            List<SettlementDataWrapper> wrappedDataList = new ArrayList<>();
            for (SettlementData data : dataList) {
                wrappedDataList.add(new SettlementDataWrapper(data));
            }

            wrappedDataBlocksMap.put(dateTime, wrappedDataList);
        }
        storage.setDataBlocksMap(wrappedDataBlocksMap);

        return storage;
    }

    /**
     * 从测量记录列表加载数据
     *
     * @param records 测量记录列表
     */
    public void loadFromMeasurementRecords(List<MeasurementRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        // 清空现有数据
        settlementDataList.clear();
        allPointDataMap.clear();
        dataBlocksMap.clear();
        dataBlockCheckBoxMap.clear();
        selectedDataBlocks.clear();

        // 创建测点配置列表（如果还没有初始化）
        if (configuredPoints.isEmpty()) {
            for (MeasurementRecord record : records) {
                boolean exists = configuredPoints.stream()
                    .anyMatch(p -> p.getPointId().equals(record.getId()));

                if (!exists) {
                    SettlementPoint point = new SettlementPoint();
                    point.setPointId(record.getId());
                    point.setInitialElevation(record.getValue());
                    configuredPoints.add(point);
                }
            }
        }

        // 按日期分组记录
        Map<LocalDate, List<MeasurementRecord>> recordsByDate = new HashMap<>();

        for (MeasurementRecord record : records) {
            if (record.getMeasureTime() != null) {
                LocalDate date = record.getMeasureTime().toLocalDate();
                recordsByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(record);
            }
        }

        // 处理每个日期组
        for (Map.Entry<LocalDate, List<MeasurementRecord>> entry : recordsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<MeasurementRecord> dateRecords = entry.getValue();

            // 转换为SettlementData列表
            List<SettlementData> dataList = new ArrayList<>();

            for (MeasurementRecord record : dateRecords) {
                // 查找该测点的配置信息
                SettlementPoint pointConfig = configuredPoints.stream()
                    .filter(p -> p.getPointId().equals(record.getId()))
                    .findFirst()
                    .orElse(null);

                if (pointConfig == null) {
                    // 如果没有找到测点配置，创建一个新的配置
                    pointConfig = new SettlementPoint();
                    pointConfig.setPointId(record.getId());
                    pointConfig.setInitialElevation(record.getValue());
                    configuredPoints.add(pointConfig);
                }

                // 查找该测点的历史数据
                Map<LocalDate, SettlementData> pointHistory = allPointDataMap.getOrDefault(record.getId(), new HashMap<>());

                // 获取初始高程和前期高程
                double initialElevation = pointConfig.getInitialElevation();
                double previousElevation = initialElevation;

                if (!pointHistory.isEmpty()) {
                    // 查找最近的记录作为前期高程
                    LocalDate latestDate = null;
                    for (LocalDate histDate : pointHistory.keySet()) {
                        if (histDate.isBefore(date) && (latestDate == null || histDate.isAfter(latestDate))) {
                            latestDate = histDate;
                        }
                    }

                    if (latestDate != null) {
                        previousElevation = pointHistory.get(latestDate).getCurrentElevation();
                    }
                }

                // 创建SettlementData对象
                SettlementData data = new SettlementData(
                    record.getId(),
                    initialElevation,
                    previousElevation,
                    record.getValue(),
                    date
                );

                if (pointConfig.getMileage() != null) {
                    data.setMileage(pointConfig.getMileage());
                }

                // 计算派生值
                data.calculateDerivedValues();

                // 添加到结果列表和历史数据映射
                dataList.add(data);

                if (!allPointDataMap.containsKey(record.getId())) {
                    allPointDataMap.put(record.getId(), new HashMap<>());
                }
                allPointDataMap.get(record.getId()).put(date, data);
            }

            // 存储该批次数据
            dataBlocksMap.put(LocalDateTime.of(date, LocalTime.now()), dataList);

            // 创建并添加数据块
            addDataBlock("加载的数据", LocalDateTime.of(date, LocalTime.now()));
        }

        // 更新UI
        updatePointCount();
        if (!selectedDataBlocks.isEmpty()) {
            updateTableBasedOnSelection();
        } else {
            updateTableWithInitialData();
        }
    }

    /**
     * 从沉降数据存储对象加载数据
     *
     * @param storage 沉降数据存储对象
     */
    public void loadFromSettlementDataStorage(SettlementDataStorage storage) {
        if (storage == null) {
            return;
        }

        // 清空现有数据
        settlementDataList.clear();
        allPointDataMap.clear();
        dataBlocksMap.clear();
        dataBlockCheckBoxMap.clear();
        selectedDataBlocks.clear();

        // 加载测点配置
        configuredPoints = new ArrayList<>(storage.getConfiguredPoints());

        // 加载所有测点数据
        Map<String, Map<LocalDate, SettlementDataWrapper>> wrappedPointDataMap = storage.getAllPointDataMap();
        for (Map.Entry<String, Map<LocalDate, SettlementDataWrapper>> entry : wrappedPointDataMap.entrySet()) {
            String pointId = entry.getKey();
            Map<LocalDate, SettlementDataWrapper> wrappedDateMap = entry.getValue();

            Map<LocalDate, SettlementData> dateMap = new HashMap<>();
            for (Map.Entry<LocalDate, SettlementDataWrapper> dateEntry : wrappedDateMap.entrySet()) {
                LocalDate date = dateEntry.getKey();
                SettlementDataWrapper wrapper = dateEntry.getValue();
                dateMap.put(date, wrapper.toSettlementData());
            }

            allPointDataMap.put(pointId, dateMap);
        }

        // 加载数据块映射
        Map<LocalDateTime, List<SettlementDataWrapper>> wrappedDataBlocksMap = storage.getDataBlocksMap();
        for (Map.Entry<LocalDateTime, List<SettlementDataWrapper>> entry : wrappedDataBlocksMap.entrySet()) {
            LocalDateTime dateTime = entry.getKey();
            List<SettlementDataWrapper> wrappedDataList = entry.getValue();

            List<SettlementData> dataList = new ArrayList<>();
            for (SettlementDataWrapper wrapper : wrappedDataList) {
                dataList.add(wrapper.toSettlementData());
            }

            dataBlocksMap.put(dateTime, dataList);

            // 创建并添加数据块
            addDataBlock("\u52a0\u8f7d\u7684\u6570\u636e", dateTime);
        }

        // 更新UI
        updatePointCount();
        updateTableWithInitialData();
    }

    /**
     * 根据沉降数据计算预警等级
     *
     * @param data 沉降数据
     * @return 预警等级（0=正常, 1-3表示不同警告级别）
     */
    private int calculateWarningLevel(SettlementData data) {
        // 查找该测点的配置
        SettlementPoint pointConfig = configuredPoints.stream()
            .filter(p -> p.getPointId().equals(data.getPointCode()))
            .findFirst()
            .orElse(null);

        if (pointConfig == null) {
            return 0; // 没有配置，返回正常
        }

        // 累计变化量（毫米）
        double cumulativeChange = Math.abs(data.getCumulativeChange() * 1000);
        // 变化速率（毫米/天）
        double changeRate = Math.abs(data.getChangeRate() * 1000);

        // 根据阈值判断预警等级
        if (cumulativeChange >= pointConfig.getAccumulatedWarningValue() * 1.5 ||
            changeRate >= pointConfig.getRateWarningValue() * 1.5) {
            return 3; // 三级预警
        } else if (cumulativeChange >= pointConfig.getAccumulatedWarningValue() * 1.2 ||
                   changeRate >= pointConfig.getRateWarningValue() * 1.2) {
            return 2; // 二级预警
        } else if (cumulativeChange >= pointConfig.getAccumulatedWarningValue() ||
                   changeRate >= pointConfig.getRateWarningValue()) {
            return 1; // 一级预警
        }

        return 0; // 正常
    }

    /**
     * 项目相关数据是否已更改（用于决定是否需要保存）
     */
    public boolean hasDataChanged() {
        return !dataBlocksMap.isEmpty(); // 如果有上传的数据块，则认为数据已更改
    }

    // 自定义天数设置，用于计算变化速率
    private int customDaysForRateCalculation = 0;

    /**
     * 获取用于计算变化速率的自定义天数
     * @return 自定义天数，如果为0则使用实际测量日期间隔
     */
    public int getCustomDaysForRateCalculation() {
        return customDaysForRateCalculation;
    }

    /**
     * 设置用于计算变化速率的自定义天数
     * @param days 自定义天数，如果为0则使用实际测量日期间隔
     */
    public void setCustomDaysForRateCalculation(int days) {
        this.customDaysForRateCalculation = days;
        // 如果已经有数据，重新计算变化速率
        if (!settlementDataList.isEmpty()) {
            updateTableBasedOnSelection();
        }
    }

    /**
     * 显示设置自定义天数的对话框
     */
    private void showSetCustomDaysDialog() {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(customDaysForRateCalculation));
        dialog.setTitle("设置变化速率计算天数");
        dialog.setHeaderText("请输入用于计算变化速率的天数");
        dialog.setContentText("天数 (0 = 使用实际测量日期间隔):");

        dialog.showAndWait().ifPresent(result -> {
            try {
                int days = Integer.parseInt(result);
                if (days < 0) {
                    AlertUtil.showError("输入错误", "天数不能为负数");
                    return;
                }

                setCustomDaysForRateCalculation(days);

                // 显示当前设置
                String message = days > 0 ?
                    "已设置变化速率计算天数为 " + days + " 天" :
                    "已设置使用实际测量日期间隔计算变化速率";

                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("设置成功");
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.showAndWait();

            } catch (NumberFormatException e) {
                AlertUtil.showError("输入错误", "请输入有效的数字");
            }
        });
    }

    /**
     * 删除数据块及其关联数据
     */
    private void removeDataBlock(LocalDateTime dateTime) {
        // 获取该日期的数据
        List<SettlementData> dataToRemove = dataBlocksMap.get(dateTime);
        if (dataToRemove == null) {
            return;
        }

        // 从点位历史数据中移除
        for (SettlementData data : dataToRemove) {
            String pointCode = data.getPointCode();
            Map<LocalDate, SettlementData> pointHistory = allPointDataMap.get(pointCode);
            if (pointHistory != null) {
                pointHistory.remove(data.getMeasurementDate());
            }
        }

        // 从数据结构中移除
        dataBlocksMap.remove(dateTime);
        dataBlockCheckBoxMap.remove(dateTime);

        // 从选中列表中移除
        selectedDataBlocks.remove(dateTime);

        // 如果删除后没有选中的数据块，显示初始数据
        if (selectedDataBlocks.isEmpty()) {
            updateTableWithInitialData();
        }

        // 更新测点计数
        updatePointCount();
    }
}