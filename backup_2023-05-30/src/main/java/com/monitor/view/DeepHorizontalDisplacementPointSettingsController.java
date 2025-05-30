package com.monitor.view;

import com.monitor.model.DeepHorizontalDisplacementPoint;
import com.monitor.model.DeepHorizontalDisplacementPoint.Measurement;
import com.monitor.util.AlertUtil;
import com.monitor.util.ExcelUtil;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * 深部水平位移测点设置对话框控制器
 */
public class DeepHorizontalDisplacementPointSettingsController {

    @FXML private ComboBox<DeepHorizontalDisplacementPoint> pointSelector;
    @FXML private TextField pointIdField;
    @FXML private TextField mileageField;
    @FXML private Button addPointButton;
    @FXML private Button removePointButton;

    @FXML private TableView<Measurement> measurementTableView;
    @FXML private TableColumn<Measurement, Double> depthColumn;
    @FXML private TableColumn<Measurement, Double> initialValueColumn;
    @FXML private TableColumn<Measurement, Double> rateAlarmColumn;
    @FXML private TableColumn<Measurement, Double> cumulativeAlarmColumn;
    @FXML private TableColumn<Measurement, Double> historicalCumulativeColumn;

    @FXML private TextField depthField;
    @FXML private TextField initialValueField;
    @FXML private TextField rateAlarmField;
    @FXML private TextField cumulativeAlarmField;
    @FXML private TextField historicalCumulativeField;

    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button batchImportButton;
    @FXML private Button exportButton;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private Stage dialogStage;
    private ObservableList<DeepHorizontalDisplacementPoint> pointsList = FXCollections.observableArrayList();
    private ObservableList<Measurement> measurementsList = FXCollections.observableArrayList();
    private boolean saveClicked = false;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 设置测点选择下拉框
        pointSelector.setItems(pointsList);
        pointSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // 更新测点信息
                pointIdField.setText(newVal.getPointId());
                mileageField.setText(newVal.getMileage());
                
                // 更新测量列表
                measurementsList.clear();
                measurementsList.addAll(newVal.getMeasurements());
            } else {
                pointIdField.clear();
                mileageField.clear();
                measurementsList.clear();
            }
        });
        
        // 设置表格列的单元格值工厂
        depthColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getDepth()).asObject());
            
        initialValueColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getInitialValue()).asObject());
        
        rateAlarmColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getRateAlarmValue()).asObject());
            
        cumulativeAlarmColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getCumulativeAlarmValue()).asObject());
            
        historicalCumulativeColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getHistoricalCumulative()).asObject());
        
        // 设置数值列的单元格工厂，以便格式化显示
        setupNumberColumnCellFactory(depthColumn);
        setupNumberColumnCellFactory(initialValueColumn);
        setupNumberColumnCellFactory(rateAlarmColumn);
        setupNumberColumnCellFactory(cumulativeAlarmColumn);
        setupNumberColumnCellFactory(historicalCumulativeColumn);

        // 设置表格数据源
        measurementTableView.setItems(measurementsList);

        // 设置表格选择监听
        measurementTableView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showMeasurementDetails(newValue));
    }

    /**
     * 设置数值列的单元格工厂
     * @param column 数值列
     */
    private void setupNumberColumnCellFactory(TableColumn<Measurement, Double> column) {
        column.setCellFactory(col -> new TableCell<Measurement, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // 如果是深度列，确保显示为负值
                    if (column == depthColumn) {
                        double depth = item;
                        // 确保深度为负值
                        if (depth > 0) {
                            depth = -depth;
                        }
                        setText(String.format("%.2f", depth));
                    } else {
                        setText(String.format("%.2f", item));
                    }
                }
            }
        });
    }

    /**
     * 在控件中显示测量点详细信息
     * @param measurement 测量点对象
     */
    private void showMeasurementDetails(Measurement measurement) {
        if (measurement != null) {
            // 填充表单字段
            double depth = measurement.getDepth();
            // 确保深度为负值显示
            if (depth > 0) {
                depth = -depth;
            }
            depthField.setText(String.format("%.2f", depth));
            initialValueField.setText(String.format("%.2f", measurement.getInitialValue()));
            rateAlarmField.setText(String.format("%.2f", measurement.getRateAlarmValue()));
            cumulativeAlarmField.setText(String.format("%.2f", measurement.getCumulativeAlarmValue()));
            historicalCumulativeField.setText(String.format("%.2f", measurement.getHistoricalCumulative()));
        } else {
            // 清空表单字段
            clearMeasurementFields();
        }
    }

    /**
     * 清空测量点表单字段
     */
    private void clearMeasurementFields() {
        depthField.clear();
        initialValueField.clear();
        rateAlarmField.clear();
        cumulativeAlarmField.clear();
        historicalCumulativeField.clear();
    }

    /**
     * 设置对话框窗口
     * @param dialogStage 对话框窗口
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * 设置初始数据
     * @param points 测点列表
     */
    public void setInitialData(List<DeepHorizontalDisplacementPoint> points) {
        if (points != null) {
            pointsList.addAll(points);
            
            // 如果有测点，选择第一个
            if (!pointsList.isEmpty()) {
                pointSelector.getSelectionModel().select(0);
            }
        }
    }

    /**
     * 获取是否点击了保存按钮
     * @return 是否点击了保存按钮
     */
    public boolean isSaveClicked() {
        return saveClicked;
    }

    /**
     * 获取测点列表
     * @return 测点列表
     */
    public List<DeepHorizontalDisplacementPoint> getPoints() {
        return new ArrayList<>(pointsList);
    }

    /**
     * 处理添加测点按钮点击事件
     */
    @FXML
    private void handleAddPointButton(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("添加测点");
        dialog.setHeaderText("请输入新测点的编号和里程");
        dialog.setContentText("测点编号:");
        
        Optional<String> pointIdResult = dialog.showAndWait();
        if (pointIdResult.isPresent() && !pointIdResult.get().trim().isEmpty()) {
            String pointId = pointIdResult.get().trim();
            
            // 检查测点ID是否已存在
            if (isPointIdExists(pointId)) {
                AlertUtil.showWarning("添加失败", "测点编号已存在，请使用不同的编号。");
                return;
            }
            
            // 询问里程
            TextInputDialog mileageDialog = new TextInputDialog("");
            mileageDialog.setTitle("添加测点");
            mileageDialog.setHeaderText("请输入新测点的里程");
            mileageDialog.setContentText("里程:");
            
            Optional<String> mileageResult = mileageDialog.showAndWait();
            String mileage = mileageResult.orElse("").trim();
            
            // 创建新测点
            DeepHorizontalDisplacementPoint newPoint = new DeepHorizontalDisplacementPoint(pointId, mileage);
            
            // 添加到列表
            pointsList.add(newPoint);
            
            // 选择新测点
            pointSelector.getSelectionModel().select(newPoint);
            
            AlertUtil.showInformation("添加成功", "成功添加新测点。");
        }
    }

    /**
     * 处理移除测点按钮点击事件
     */
    @FXML
    private void handleRemovePointButton(ActionEvent event) {
        DeepHorizontalDisplacementPoint selectedPoint = pointSelector.getSelectionModel().getSelectedItem();
        
        if (selectedPoint == null) {
            AlertUtil.showWarning("移除失败", "请先选择一个测点。");
            return;
        }
        
        ButtonType result = AlertUtil.showConfirmation(
                "确认移除", "确定要移除测点 " + selectedPoint.getPointId() + " 吗？此操作不可撤销。");
        
        if (result == ButtonType.OK) {
            // 从列表中移除测点
            pointsList.remove(selectedPoint);
            
            // 如果有其他测点，选择第一个
            if (!pointsList.isEmpty()) {
                pointSelector.getSelectionModel().select(0);
            } else {
                // 清空表单字段
                clearPointFields();
                clearMeasurementFields();
                measurementsList.clear();
            }
            
            AlertUtil.showInformation("移除成功", "测点已成功移除。");
        }
    }

    /**
     * 处理添加测量点按钮点击事件
     */
    @FXML
    private void handleAddButton(ActionEvent event) {
        try {
            if (validateMeasurementInput()) {
                DeepHorizontalDisplacementPoint selectedPoint = pointSelector.getValue();
                if (selectedPoint != null) {
                    Measurement measurement = createMeasurementFromInput();
                    
                    // 检查该深度是否已存在
                    boolean depthExists = false;
                    double inputDepth = measurement.getDepth();
                    // 确保深度为负值进行比较
                    if (inputDepth > 0) {
                        inputDepth = -inputDepth;
                    }
                    
                    for (Measurement m : measurementsList) {
                        double existingDepth = m.getDepth();
                        // 确保比较时都是负值
                        if (existingDepth > 0) {
                            existingDepth = -existingDepth;
                        }
                        
                        if (Math.abs(existingDepth - inputDepth) < 0.001) {
                            depthExists = true;
                            break;
                        }
                    }
                    
                    if (depthExists) {
                        AlertUtil.showWarning("输入错误", "该深度的测量点已存在。");
                        return;
                    }
                    
                    // 添加到列表和模型
                    selectedPoint.addMeasurement(measurement);
                    measurementsList.add(measurement);
                    
                    // 清空输入字段
                    clearMeasurementFields();
                }
            }
        } catch (NumberFormatException e) {
            AlertUtil.showError("输入错误", "请输入有效的数值。");
        }
    }

    /**
     * 处理更新测量点按钮点击事件
     */
    @FXML
    private void handleUpdateButton(ActionEvent event) {
        DeepHorizontalDisplacementPoint selectedPoint = pointSelector.getSelectionModel().getSelectedItem();
        Measurement selectedMeasurement = measurementTableView.getSelectionModel().getSelectedItem();
        
        if (selectedPoint == null) {
            AlertUtil.showWarning("更新失败", "请先选择一个测点。");
            return;
        }
        
        if (selectedMeasurement == null) {
            AlertUtil.showWarning("更新失败", "请先选择一个测量点。");
            return;
        }
        
        if (validateMeasurementInput()) {
            // 获取新数据
            double depth = Double.parseDouble(depthField.getText().trim());
            
            // 如果深度已更改，检查新深度是否已存在
            if (depth != selectedMeasurement.getDepth() && isMeasurementDepthExists(depth)) {
                AlertUtil.showWarning("更新失败", "此深度的测量点已存在，请使用不同的深度。");
                return;
            }
            
            // 更新数据
            selectedMeasurement.setDepth(depth);
            selectedMeasurement.setInitialValue(Double.parseDouble(initialValueField.getText().trim()));
            selectedMeasurement.setRateAlarmValue(Double.parseDouble(rateAlarmField.getText().trim()));
            selectedMeasurement.setCumulativeAlarmValue(Double.parseDouble(cumulativeAlarmField.getText().trim()));
            selectedMeasurement.setHistoricalCumulative(Double.parseDouble(historicalCumulativeField.getText().trim()));
            
            // 刷新表格
            measurementTableView.refresh();
            
            AlertUtil.showInformation("更新成功", "成功更新测量点信息。");
        }
    }

    /**
     * 处理删除测量点按钮点击事件
     */
    @FXML
    private void handleDeleteButton(ActionEvent event) {
        DeepHorizontalDisplacementPoint selectedPoint = pointSelector.getSelectionModel().getSelectedItem();
        Measurement selectedMeasurement = measurementTableView.getSelectionModel().getSelectedItem();
        
        if (selectedPoint == null) {
            AlertUtil.showWarning("删除失败", "请先选择一个测点。");
            return;
        }
        
        if (selectedMeasurement == null) {
            AlertUtil.showWarning("删除失败", "请先选择一个测量点。");
            return;
        }
        
        ButtonType result = AlertUtil.showConfirmation(
                "确认删除", "确定要删除深度为 " + selectedMeasurement.getDepth() + "m 的测量点吗？此操作不可撤销。");
        
        if (result == ButtonType.OK) {
            // 从测点和列表中移除
            selectedPoint.removeMeasurement(selectedMeasurement);
            measurementsList.remove(selectedMeasurement);
            
            // 清空输入字段
            clearMeasurementFields();
            
            AlertUtil.showInformation("删除成功", "测量点已成功删除。");
        }
    }

    /**
     * 处理批量录入按钮点击事件
     */
    @FXML
    private void handleBatchImportButton(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("批量导入测量点数据");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel文件", "*.xlsx"));
        
        File selectedFile = fileChooser.showOpenDialog(dialogStage);
        if (selectedFile != null) {
            try {
                // 从Excel导入数据
                Map<String, List<Measurement>> importedData = importMeasurementsFromExcel(selectedFile);
                
                // 处理导入的数据
                // processImportedData(importedData); // 注释掉这行，因为在importMeasurementsFromExcel中已经调用了processImportedData
                
                AlertUtil.showInformation("导入成功", "成功导入测量点数据。");
            } catch (Exception e) {
                AlertUtil.showError("导入错误", "导入数据时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 从Excel文件导入测量点数据
     * @param file Excel文件
     * @return 测点数据映射 <测点ID, 测量点列表>
     * @throws Exception 导入过程中的错误
     */
    private Map<String, List<Measurement>> importMeasurementsFromExcel(File file) throws Exception {
        Map<String, List<Measurement>> result = new HashMap<>();
        Map<String, String> mileageMap = new HashMap<>();
        
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            // 遍历所有Sheet
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = workbook.getSheetName(i);
                
                // 检查Sheet名称是否为有效的测点ID
                String pointId = sheetName.trim();
                if (pointId.isEmpty()) continue;
                
                // 准备存储该测点的测量点列表
                List<Measurement> measurements = new ArrayList<>();
                
                // 查找表头行
                int headerRowNum = -1;
                for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    
                    Cell firstCell = row.getCell(0);
                    if (firstCell != null && getCellValueAsString(firstCell).contains("深度")) {
                        headerRowNum = r;
                        break;
                    }
                }
                
                if (headerRowNum == -1) continue; // 没有找到表头
                
                // 查找表头各列的索引
                Row headerRow = sheet.getRow(headerRowNum);
                int depthColIndex = -1;
                int valueColIndex = -1;
                int rateAlarmColIndex = -1;
                int cumulativeAlarmColIndex = -1;
                int historicalCumColIndex = -1;
                int mileageColIndex = -1;
                
                for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                    Cell cell = headerRow.getCell(c);
                    if (cell == null) continue;
                    
                    String headerText = getCellValueAsString(cell).trim();
                    if (headerText.contains("深度")) {
                        depthColIndex = c;
                    } else if (headerText.contains("初始值") || headerText.contains("初值")) {
                        valueColIndex = c;
                    } else if (headerText.contains("速率报警") || headerText.contains("速率预警") || headerText.contains("速率警戒值")) {
                        rateAlarmColIndex = c;
                    } else if (headerText.contains("累计报警") || headerText.contains("累计预警") || headerText.contains("累计警戒值")) {
                        cumulativeAlarmColIndex = c;
                    } else if (headerText.contains("历史累计") || headerText.contains("历史变化")) {
                        historicalCumColIndex = c;
                    } else if (headerText.contains("里程")) {
                        mileageColIndex = c;
                    }
                }
                
                // 需要至少有深度和初始值列
                if (depthColIndex == -1 || valueColIndex == -1) continue;
                
                // 读取数据行
                for (int r = headerRowNum + 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    
                    Cell depthCell = row.getCell(depthColIndex);
                    Cell valueCell = row.getCell(valueColIndex);
                    
                    // 跳过没有深度或初始值的行
                    if (depthCell == null || valueCell == null) continue;
                    
                    try {
                        // 读取深度值
                        double depth = getCellValueAsDouble(depthCell);
                        // 确保深度值为负值（表示地下深度）
                        if (depth > 0) {
                            depth = -depth;
                        }
                        
                        // 读取初始值
                        double initialValue = getCellValueAsDouble(valueCell);
                        
                        // 读取其他可选值
                        double rateAlarmValue = 5.0; // 默认值
                        if (rateAlarmColIndex != -1) {
                            Cell cell = row.getCell(rateAlarmColIndex);
                            if (cell != null) {
                                rateAlarmValue = getCellValueAsDouble(cell);
                            }
                        }
                        
                        double cumulativeAlarmValue = 10.0; // 默认值
                        if (cumulativeAlarmColIndex != -1) {
                            Cell cell = row.getCell(cumulativeAlarmColIndex);
                            if (cell != null) {
                                cumulativeAlarmValue = getCellValueAsDouble(cell);
                            }
                        }
                        
                        double historicalCumulative = 0.0; // 默认值
                        if (historicalCumColIndex != -1) {
                            Cell cell = row.getCell(historicalCumColIndex);
                            if (cell != null) {
                                historicalCumulative = getCellValueAsDouble(cell);
                            }
                        }
                        
                        // 读取里程（若有）
                        if (mileageColIndex != -1) {
                            Cell cell = row.getCell(mileageColIndex);
                            if (cell != null) {
                                String mileage = getCellValueAsString(cell);
                                if (!mileage.isEmpty()) {
                                    mileageMap.put(pointId, mileage);
                                }
                            }
                        }
                        
                        // 创建测量点对象
                        Measurement measurement = new Measurement(
                                depth, initialValue, rateAlarmValue, cumulativeAlarmValue, historicalCumulative);
                        
                        // 添加到列表
                        measurements.add(measurement);
                        
                    } catch (NumberFormatException e) {
                        // 忽略无法解析为数字的行
                        continue;
                    }
                }
                
                // 添加到结果
                if (!measurements.isEmpty()) {
                    result.put(pointId, measurements);
                }
            }
        }
        
        // 处理导入的数据
        processImportedData(result, mileageMap);
        
        return result;
    }
    
    /**
     * 获取单元格的数值
     */
    private double getCellValueAsDouble(Cell cell) {
        if (cell == null) {
            return 0.0;
        }
        
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            case FORMULA:
                try {
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    return 0.0;
                }
            default:
                return 0.0;
        }
    }
    
    /**
     * 获取单元格字符串值
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case NUMERIC:
                double value = cell.getNumericCellValue();
                // 检查是否为整数
                if (value == Math.floor(value)) {
                    return String.format("%.0f", value);
                }
                return String.format("%.6f", value);
            case STRING:
                return cell.getStringCellValue().trim();
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        return "";
                    }
                }
            default:
                return "";
        }
    }
    
    /**
     * 处理从Excel导入的数据
     */
    private void processImportedData(Map<String, List<Measurement>> importedData, Map<String, String> mileageMap) {
        for (Map.Entry<String, List<Measurement>> entry : importedData.entrySet()) {
            String pointId = entry.getKey();
            List<Measurement> measurements = entry.getValue();
            
            // 查找现有测点
            DeepHorizontalDisplacementPoint existingPoint = findPointById(pointId);
            
            if (existingPoint != null) {
                // 如果测点已存在，询问是否替换其测量点数据
                ButtonType result = AlertUtil.showConfirmation(
                        "测点已存在", 
                        "测点 " + pointId + " 已存在。是否替换其现有测量点数据？");
                
                if (result == ButtonType.OK) {
                    // 清除现有测量点并添加新的
                    existingPoint.getMeasurements().clear();
                    existingPoint.getMeasurements().addAll(measurements);
                    
                    // 如果当前选中的是该测点，更新表格
                    if (pointSelector.getSelectionModel().getSelectedItem() == existingPoint) {
                        measurementsList.clear();
                        measurementsList.addAll(measurements);
                    }
                }
            } else {
                // 如果测点不存在，创建新测点
                // 先尝试使用从Excel获取的里程信息
                String mileage = mileageMap.getOrDefault(pointId, "");
                
                // 如果没有从Excel获取到里程信息，则需要用户输入
                if (mileage.isEmpty()) {
                    TextInputDialog mileageDialog = new TextInputDialog("");
                    mileageDialog.setTitle("添加测点");
                    mileageDialog.setHeaderText("请为测点 " + pointId + " 输入里程");
                    mileageDialog.setContentText("里程:");
                    
                    Optional<String> mileageResult = mileageDialog.showAndWait();
                    mileage = mileageResult.orElse("").trim();
                }
                
                DeepHorizontalDisplacementPoint newPoint = new DeepHorizontalDisplacementPoint(pointId, mileage);
                newPoint.getMeasurements().addAll(measurements);
                
                // 添加到测点列表
                pointsList.add(newPoint);
                
                // 选择新添加的测点
                pointSelector.getSelectionModel().select(newPoint);
            }
        }
    }

    /**
     * 根据ID查找测点
     */
    private DeepHorizontalDisplacementPoint findPointById(String pointId) {
        for (DeepHorizontalDisplacementPoint point : pointsList) {
            if (point.getPointId().equals(pointId)) {
                return point;
            }
        }
        return null;
    }

    /**
     * 处理导出按钮点击事件
     */
    @FXML
    private void handleExportButton(ActionEvent event) {
        DeepHorizontalDisplacementPoint selectedPoint = pointSelector.getSelectionModel().getSelectedItem();
        
        if (selectedPoint == null) {
            AlertUtil.showWarning("导出失败", "请先选择一个测点。");
            return;
        }
        
        if (selectedPoint.getMeasurements().isEmpty()) {
            AlertUtil.showWarning("导出失败", "当前测点没有测量点数据可供导出。");
            return;
        }
        
        // 创建文件选择器
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出到Excel");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel文件", "*.xlsx"));
        fileChooser.setInitialFileName(selectedPoint.getPointId() + "_测量点数据.xlsx");
        
        // 显示文件选择对话框
        File selectedFile = fileChooser.showSaveDialog(dialogStage);
        
        if (selectedFile != null) {
            try {
                // 导出数据到Excel（这里需要实现导出功能）
                // ...
                
                AlertUtil.showInformation("导出成功", "测量点数据已成功导出到Excel文件。");
            } catch (Exception e) {
                AlertUtil.showError("导出错误", "导出数据时出错：" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理保存按钮点击事件
     */
    @FXML
    private void handleSaveButton(ActionEvent event) {
        // 保存点ID和里程
        DeepHorizontalDisplacementPoint selectedPoint = pointSelector.getSelectionModel().getSelectedItem();
        if (selectedPoint != null && validatePointFields()) {
            selectedPoint.setPointId(pointIdField.getText().trim());
            selectedPoint.setMileage(mileageField.getText().trim());
        }
        
        saveClicked = true;
        dialogStage.close();
    }

    /**
     * 处理取消按钮点击事件
     */
    @FXML
    private void handleCancelButton(ActionEvent event) {
        saveClicked = false;
        dialogStage.close();
    }

    /**
     * 验证测点输入是否有效
     */
    private boolean validatePointFields() {
        StringBuilder errorMessage = new StringBuilder();
        
        if (pointIdField.getText() == null || pointIdField.getText().trim().isEmpty()) {
            errorMessage.append("测点编号不能为空\n");
        }
        
        if (errorMessage.length() > 0) {
            AlertUtil.showWarning("输入错误", errorMessage.toString());
            return false;
        }
        
        return true;
    }

    /**
     * 验证测量点输入是否有效
     */
    private boolean validateMeasurementInput() {
        StringBuilder errorMessage = new StringBuilder();
        
        try {
            Double.parseDouble(depthField.getText().trim());
            // 深度可正可负，不需要检查是否为非负数
        } catch (NumberFormatException e) {
            errorMessage.append("深度必须是有效的数字\n");
        }
        
        try {
            Double.parseDouble(initialValueField.getText().trim());
        } catch (NumberFormatException e) {
            errorMessage.append("初始值必须是有效的数字\n");
        }
        
        try {
            Double.parseDouble(rateAlarmField.getText().trim());
        } catch (NumberFormatException e) {
            errorMessage.append("速率报警值必须是有效的数字\n");
        }
        
        try {
            Double.parseDouble(cumulativeAlarmField.getText().trim());
        } catch (NumberFormatException e) {
            errorMessage.append("累计报警值必须是有效的数字\n");
        }
        
        try {
            Double.parseDouble(historicalCumulativeField.getText().trim());
        } catch (NumberFormatException e) {
            errorMessage.append("历史累积值必须是有效的数字\n");
        }
        
        if (errorMessage.length() > 0) {
            AlertUtil.showWarning("输入错误", errorMessage.toString());
            return false;
        }
        
        return true;
    }

    /**
     * 创建测量点对象
     */
    private Measurement createMeasurementFromInput() {
        double depth = Double.parseDouble(depthField.getText());
        // 确保深度为负值保存
        if (depth > 0) {
            depth = -depth;
        }
        
        double initialValue = Double.parseDouble(initialValueField.getText());
        double rateAlarm = Double.parseDouble(rateAlarmField.getText());
        double cumulativeAlarm = Double.parseDouble(cumulativeAlarmField.getText());
        double historicalCumulative = Double.parseDouble(historicalCumulativeField.getText());
        
        return new Measurement(depth, initialValue, rateAlarm, cumulativeAlarm, historicalCumulative);
    }

    /**
     * 检查测点ID是否已存在
     */
    private boolean isPointIdExists(String pointId) {
        for (DeepHorizontalDisplacementPoint point : pointsList) {
            if (point.getPointId().equals(pointId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查当前测点中是否已存在指定深度的测量点
     */
    private boolean isMeasurementDepthExists(double depth) {
        for (Measurement measurement : measurementsList) {
            if (Math.abs(measurement.getDepth() - depth) < 0.001) {
                return true;
            }
        }
        return false;
    }

    /**
     * 清空测点输入字段
     */
    private void clearPointFields() {
        pointIdField.clear();
        mileageField.clear();
    }
} 