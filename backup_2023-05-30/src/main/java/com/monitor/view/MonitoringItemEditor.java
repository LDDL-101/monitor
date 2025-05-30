package com.monitor.view;

import com.monitor.model.MeasurementRecord;
import com.monitor.model.MonitoringItem;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 监测测项编辑组件
 * 用于显示和编辑监测测项的详细信息
 */
public class MonitoringItemEditor extends BorderPane {
    
    // 数据模型
    private MonitoringItem item;
    
    // UI组件
    private Label titleLabel;
    private TextField idField;
    private TextField nameField;
    private ComboBox<String> typeComboBox;
    private TextField locationField;
    private TextArea descriptionArea;
    private TextField unitField;
    private TextField warningLevel1Field;
    private TextField warningLevel2Field;
    private TextField warningLevel3Field;
    private DatePicker installDatePicker;
    private TableView<MeasurementRecord> recordsTable;
    
    // 按钮
    private Button saveButton;
    private Button cancelButton;
    private Button deleteButton;
    private Button addRecordButton;
    
    /**
     * 创建一个新的监测测项编辑器
     * @param item 要编辑的监测测项
     */
    public MonitoringItemEditor(MonitoringItem item) {
        this.item = item;
        
        // 创建UI组件
        setupUI();
        
        // 绑定数据
        if (item != null) {
            bindData();
        }
    }
    
    /**
     * 设置UI组件
     */
    private void setupUI() {
        // 主标题
        titleLabel = new Label("监测测项详情");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLabel.setPadding(new Insets(10, 0, 10, 0));
        
        // 创建表单
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        formGrid.setPadding(new Insets(10));
        
        // 添加表单字段
        int row = 0;
        
        formGrid.add(new Label("测项ID:"), 0, row);
        idField = new TextField();
        idField.setPromptText("输入测项唯一ID");
        formGrid.add(idField, 1, row);
        
        formGrid.add(new Label("测项名称:"), 2, row);
        nameField = new TextField();
        nameField.setPromptText("输入测项名称");
        formGrid.add(nameField, 3, row);
        row++;
        
        formGrid.add(new Label("测项类型:"), 0, row);
        typeComboBox = new ComboBox<>();
        typeComboBox.getItems().addAll("位移", "沉降", "倾斜", "应力", "水位", "裂缝", "其他");
        typeComboBox.setPromptText("选择测项类型");
        formGrid.add(typeComboBox, 1, row);
        
        formGrid.add(new Label("测项位置:"), 2, row);
        locationField = new TextField();
        locationField.setPromptText("输入测项位置");
        formGrid.add(locationField, 3, row);
        row++;
        
        formGrid.add(new Label("单位:"), 0, row);
        unitField = new TextField();
        unitField.setPromptText("测量单位");
        formGrid.add(unitField, 1, row);
        
        formGrid.add(new Label("安装日期:"), 2, row);
        installDatePicker = new DatePicker();
        formGrid.add(installDatePicker, 3, row);
        row++;
        
        formGrid.add(new Label("一级预警值:"), 0, row);
        warningLevel1Field = new TextField();
        warningLevel1Field.setPromptText("一级预警阈值");
        formGrid.add(warningLevel1Field, 1, row);
        
        formGrid.add(new Label("二级预警值:"), 2, row);
        warningLevel2Field = new TextField();
        warningLevel2Field.setPromptText("二级预警阈值");
        formGrid.add(warningLevel2Field, 3, row);
        row++;
        
        formGrid.add(new Label("三级预警值:"), 0, row);
        warningLevel3Field = new TextField();
        warningLevel3Field.setPromptText("三级预警阈值");
        formGrid.add(warningLevel3Field, 1, row);
        row++;
        
        formGrid.add(new Label("测项描述:"), 0, row);
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("输入测项详细描述");
        descriptionArea.setPrefRowCount(4);
        GridPane.setColumnSpan(descriptionArea, 4);
        formGrid.add(descriptionArea, 0, row + 1, 4, 1);
        row += 2;
        
        // 测量记录表格
        formGrid.add(new Label("测量记录:"), 0, row);
        GridPane.setColumnSpan(new Label("测量记录:"), 4);
        row++;
        
        recordsTable = new TableView<>();
        
        // 添加表格列
        TableColumn<MeasurementRecord, String> idColumn = new TableColumn<>("记录ID");
        idColumn.setPrefWidth(100);
        idColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getId()));
        
        TableColumn<MeasurementRecord, String> valueColumn = new TableColumn<>("测量值");
        valueColumn.setPrefWidth(100);
        valueColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.format("%.2f %s", data.getValue().getValue(), data.getValue().getUnit())));
        
        TableColumn<MeasurementRecord, String> timeColumn = new TableColumn<>("测量时间");
        timeColumn.setPrefWidth(150);
        timeColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getMeasureTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        
        TableColumn<MeasurementRecord, String> operatorColumn = new TableColumn<>("操作员");
        operatorColumn.setPrefWidth(100);
        operatorColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getOperator()));
        
        TableColumn<MeasurementRecord, String> warningColumn = new TableColumn<>("预警级别");
        warningColumn.setPrefWidth(80);
        warningColumn.setCellValueFactory(data -> {
            int level = data.getValue().getWarningLevel();
            String warning = level == 0 ? "正常" : "级别" + level;
            return new javafx.beans.property.SimpleStringProperty(warning);
        });
        
        recordsTable.getColumns().addAll(idColumn, valueColumn, timeColumn, operatorColumn, warningColumn);
        recordsTable.setPrefHeight(200);
        GridPane.setColumnSpan(recordsTable, 4);
        formGrid.add(recordsTable, 0, row, 4, 1);
        row++;
        
        // 按钮区域
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        
        saveButton = new Button("保存测项");
        saveButton.getStyleClass().add("primary-button");
        
        cancelButton = new Button("取消");
        cancelButton.getStyleClass().add("secondary-button");
        
        deleteButton = new Button("删除测项");
        deleteButton.getStyleClass().add("danger-button");
        
        addRecordButton = new Button("添加记录");
        addRecordButton.getStyleClass().add("primary-button");
        
        buttonBox.getChildren().addAll(saveButton, cancelButton, deleteButton, addRecordButton);
        GridPane.setColumnSpan(buttonBox, 4);
        formGrid.add(buttonBox, 0, row, 4, 1);
        
        // 设置布局
        VBox contentBox = new VBox();
        contentBox.getChildren().addAll(titleLabel, formGrid);
        contentBox.setPadding(new Insets(20));
        contentBox.setSpacing(10);
        
        setCenter(contentBox);
        
        // 设置按钮事件
        setupActions();
    }
    
    /**
     * 绑定数据到UI
     */
    private void bindData() {
        if (item == null) return;
        
        idField.setText(item.getId());
        nameField.setText(item.getName());
        typeComboBox.setValue(item.getType());
        locationField.setText(item.getLocation());
        descriptionArea.setText(item.getDescription());
        unitField.setText(item.getUnit());
        
        warningLevel1Field.setText(String.valueOf(item.getWarningLevel1()));
        warningLevel2Field.setText(String.valueOf(item.getWarningLevel2()));
        warningLevel3Field.setText(String.valueOf(item.getWarningLevel3()));
        
        if (item.getInstallTime() != null) {
            installDatePicker.setValue(item.getInstallTime().toLocalDate());
        }
        
        // 更新测量记录表
        recordsTable.getItems().clear();
        if (item.getRecords() != null) {
            recordsTable.getItems().addAll(item.getRecords());
        }
    }
    
    /**
     * 从UI获取数据更新到模型
     */
    private MonitoringItem updateModelFromUI() {
        if (item == null) {
            item = new MonitoringItem();
        }
        
        item.setId(idField.getText());
        item.setName(nameField.getText());
        item.setType(typeComboBox.getValue());
        item.setLocation(locationField.getText());
        item.setDescription(descriptionArea.getText());
        item.setUnit(unitField.getText());
        
        try {
            item.setWarningLevel1(Double.parseDouble(warningLevel1Field.getText()));
        } catch (NumberFormatException e) {
            // 使用默认值或提示错误
        }
        
        try {
            item.setWarningLevel2(Double.parseDouble(warningLevel2Field.getText()));
        } catch (NumberFormatException e) {
            // 使用默认值或提示错误
        }
        
        try {
            item.setWarningLevel3(Double.parseDouble(warningLevel3Field.getText()));
        } catch (NumberFormatException e) {
            // 使用默认值或提示错误
        }
        
        if (installDatePicker.getValue() != null) {
            item.setInstallTime(LocalDateTime.of(installDatePicker.getValue(), 
                    java.time.LocalTime.of(0, 0)));
        }
        
        return item;
    }
    
    /**
     * 设置按钮动作
     */
    private void setupActions() {
        saveButton.setOnAction(event -> {
            updateModelFromUI();
            System.out.println("保存测项: " + item.getName());
            // 这里可以添加保存到数据库的代码
        });
        
        cancelButton.setOnAction(event -> {
            // 重新加载数据，取消编辑
            bindData();
            System.out.println("取消编辑");
        });
        
        deleteButton.setOnAction(event -> {
            System.out.println("删除测项: " + item.getId());
            // 这里可以添加删除测项的代码
        });
        
        addRecordButton.setOnAction(event -> {
            System.out.println("添加测量记录");
            // 这里可以添加创建新测量记录的代码
            
            // 创建一个示例记录
            MeasurementRecord record = new MeasurementRecord(
                    "REC" + System.currentTimeMillis(),
                    Math.random() * 100,
                    LocalDateTime.now()
            );
            record.setUnit(item.getUnit());
            record.setOperator("当前用户");
            
            // 添加到列表和表格
            item.addRecord(record);
            recordsTable.getItems().add(record);
        });
    }
    
    /**
     * 获取当前编辑的测项
     */
    public MonitoringItem getItem() {
        return updateModelFromUI();
    }
} 