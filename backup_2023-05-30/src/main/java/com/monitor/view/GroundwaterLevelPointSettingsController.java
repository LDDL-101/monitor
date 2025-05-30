package com.monitor.view;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.monitor.model.GroundwaterLevelPoint;
import com.monitor.util.AlertUtil;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

/**
 * 地下水位测点设置对话框控制器
 */
public class GroundwaterLevelPointSettingsController {
    
    @FXML private TableView<GroundwaterLevelPoint> pointsTableView;
    @FXML private TableColumn<GroundwaterLevelPoint, String> pointIdColumn;
    @FXML private TableColumn<GroundwaterLevelPoint, Number> initialElevationColumn;
    @FXML private TableColumn<GroundwaterLevelPoint, String> mileageColumn;
    @FXML private TableColumn<GroundwaterLevelPoint, Number> rateWarningColumn;
    @FXML private TableColumn<GroundwaterLevelPoint, Number> accumulatedWarningColumn;
    @FXML private TableColumn<GroundwaterLevelPoint, Number> historicalCumulativeColumn;
    
    @FXML private TextField pointIdField;
    @FXML private TextField initialElevationField;
    @FXML private TextField mileageField;
    @FXML private TextField rateWarningField;
    @FXML private TextField accumulatedWarningField;
    @FXML private TextField historicalCumulativeField;
    
    @FXML private Button addButton;
    @FXML private Button batchImportButton;
    @FXML private Button exportButton;
    @FXML private Button saveCloseButton;
    
    @FXML private MenuItem deleteMenuItem;
    
    private ObservableList<GroundwaterLevelPoint> points = FXCollections.observableArrayList();
    private Stage dialogStage;
    
    /**
     * 初始化控制器
     */
    @FXML
    private void initialize() {
        // 设置表格列
        pointIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPointId()));
        initialElevationColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getInitialElevation()));
        initialElevationColumn.setCellFactory(column -> new TableCell<GroundwaterLevelPoint, Number>() {
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
        
        mileageColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMileage()));
        rateWarningColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getRateWarningValue()));
        accumulatedWarningColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getAccumulatedWarningValue()));
        historicalCumulativeColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getHistoricalCumulative()));
        
        // 设置表格数据源
        pointsTableView.setItems(points);
        
        // 添加表格选择事件处理
        pointsTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                // 显示选中测点的详细信息
                pointIdField.setText(newValue.getPointId());
                initialElevationField.setText(String.format("%.4f", newValue.getInitialElevation()));
                mileageField.setText(newValue.getMileage());
                rateWarningField.setText(String.format("%.2f", newValue.getRateWarningValue()));
                accumulatedWarningField.setText(String.format("%.2f", newValue.getAccumulatedWarningValue()));
                historicalCumulativeField.setText(String.format("%.2f", newValue.getHistoricalCumulative()));
            }
        });
    }
    
    /**
     * 设置对话框舞台
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
        dialogStage.setTitle("地下水位测点设置");
    }
    
    /**
     * 设置初始数据
     */
    public void setInitialData(List<GroundwaterLevelPoint> initialPoints) {
        if (initialPoints != null) {
            points.clear();
            points.addAll(initialPoints);
            
            // 按测点编号排序
            Collections.sort(points, (p1, p2) -> {
                int idx1 = p1.getOrderIndex();
                int idx2 = p2.getOrderIndex();
                if (idx1 != idx2) {
                    return Integer.compare(idx1, idx2);
                }
                return p1.getPointId().compareTo(p2.getPointId());
            });
        }
    }
    
    /**
     * 获取配置的测点列表
     */
    public List<GroundwaterLevelPoint> getPoints() {
        return new ArrayList<>(points);
    }
    
    /**
     * 处理添加/更新测点按钮点击事件
     */
    @FXML
    private void handleAddPoint(ActionEvent event) {
        if (!validateInputs()) {
            return;
        }
        
        // 获取输入值
        String pointId = pointIdField.getText().trim();
        double initialElevation = Double.parseDouble(initialElevationField.getText().trim());
        String mileage = mileageField.getText().trim();
        double rateWarningValue = Double.parseDouble(rateWarningField.getText().trim());
        double accumulatedWarningValue = Double.parseDouble(accumulatedWarningField.getText().trim());
        
        // 处理历史累计值，如果为空则默认为0
        double historicalCumulative = 0;
        if (!historicalCumulativeField.getText().trim().isEmpty()) {
            try {
                historicalCumulative = Double.parseDouble(historicalCumulativeField.getText().trim());
            } catch (NumberFormatException e) {
                // 使用默认值
            }
        }
        
        // 检查是否已存在该测点
        boolean exists = false;
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).getPointId().equals(pointId)) {
                // 更新现有测点
                GroundwaterLevelPoint existingPoint = points.get(i);
                existingPoint.setInitialElevation(initialElevation);
                existingPoint.setMileage(mileage);
                existingPoint.setRateWarningValue(rateWarningValue);
                existingPoint.setAccumulatedWarningValue(accumulatedWarningValue);
                existingPoint.setHistoricalCumulative(historicalCumulative);
                
                // 刷新表格
                pointsTableView.refresh();
                exists = true;
                break;
            }
        }
        
        if (!exists) {
            // 添加新测点
            GroundwaterLevelPoint newPoint = new GroundwaterLevelPoint(
                    pointId, initialElevation, mileage, 
                    rateWarningValue, accumulatedWarningValue, historicalCumulative);
            
            // 设置排序索引为当前列表大小
            newPoint.setOrderIndex(points.size());
            
            // 添加到列表
            points.add(newPoint);
        }
        
        // 清除输入字段
        clearInputFields();
    }
    
    /**
     * 处理批量导入按钮点击事件
     */
    @FXML
    private void handleBatchImport(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择Excel文件");
        fileChooser.getExtensionFilters().add(
                new ExtensionFilter("Excel文件", "*.xlsx", "*.xls"));
        
        // 显示打开文件对话框
        File file = fileChooser.showOpenDialog(dialogStage);
        if (file != null) {
            try {
                List<GroundwaterLevelPoint> importedPoints = importPointsFromExcel(file);
                
                if (importedPoints.isEmpty()) {
                    AlertUtil.showInformation("导入结果", "没有找到有效的测点数据。");
                    return;
                }
                
                // 确认提示
                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setTitle("确认导入");
                alert.setHeaderText("发现 " + importedPoints.size() + " 个测点");
                alert.setContentText("点击确定导入这些测点，将覆盖已有的同名测点。");
                
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    // 创建测点ID到测点的映射
                    Map<String, GroundwaterLevelPoint> existingPointsMap = new HashMap<>();
                    for (GroundwaterLevelPoint point : points) {
                        existingPointsMap.put(point.getPointId(), point);
                    }
                    
                    // 合并新旧测点
                    for (GroundwaterLevelPoint importedPoint : importedPoints) {
                        if (existingPointsMap.containsKey(importedPoint.getPointId())) {
                            // 更新已有测点
                            GroundwaterLevelPoint existingPoint = existingPointsMap.get(importedPoint.getPointId());
                            existingPoint.setInitialElevation(importedPoint.getInitialElevation());
                            existingPoint.setMileage(importedPoint.getMileage());
                            existingPoint.setRateWarningValue(importedPoint.getRateWarningValue());
                            existingPoint.setAccumulatedWarningValue(importedPoint.getAccumulatedWarningValue());
                            existingPoint.setHistoricalCumulative(importedPoint.getHistoricalCumulative());
                        } else {
                            // 添加新测点，设置排序索引为当前列表大小
                            importedPoint.setOrderIndex(points.size());
                            points.add(importedPoint);
                        }
                    }
                    
                    // 刷新表格
                    pointsTableView.refresh();
                    
                    // 显示成功消息
                    AlertUtil.showInformation("导入成功", "成功导入 " + importedPoints.size() + " 个测点。");
                }
                
            } catch (IOException e) {
                AlertUtil.showError("导入错误", "无法读取Excel文件: " + e.getMessage());
            }
        }
    }
    
    /**
     * 处理删除测点菜单项点击事件
     */
    @FXML
    private void handleDeletePoint(ActionEvent event) {
        GroundwaterLevelPoint selectedPoint = pointsTableView.getSelectionModel().getSelectedItem();
        if (selectedPoint != null) {
            // 确认删除
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle("确认删除");
            alert.setHeaderText("删除测点");
            alert.setContentText("确定要删除测点 " + selectedPoint.getPointId() + " 吗？");
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                points.remove(selectedPoint);
                clearInputFields();
            }
        }
    }
    
    /**
     * 处理导出测点数据按钮点击事件
     */
    @FXML
    private void handleExport(ActionEvent event) {
        if (points.isEmpty()) {
            AlertUtil.showInformation("导出", "没有测点数据可供导出。");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出测点数据");
        fileChooser.getExtensionFilters().add(
                new ExtensionFilter("Excel文件", "*.xlsx"));
        fileChooser.setInitialFileName("地下水位测点配置.xlsx");
        
        // 显示保存文件对话框
        File file = fileChooser.showSaveDialog(dialogStage);
        if (file != null) {
            try {
                exportPointsToExcel(file);
                AlertUtil.showInformation("导出成功", "测点数据已成功导出到:\n" + file.getAbsolutePath());
            } catch (IOException e) {
                AlertUtil.showError("导出错误", "导出测点数据时发生错误:\n" + e.getMessage());
            }
        }
    }
    
    /**
     * 处理关闭按钮点击事件
     */
    @FXML
    private void handleClose(ActionEvent event) {
        dialogStage.close();
    }
    
    /**
     * 处理保存并关闭按钮点击事件
     */
    @FXML
    private void handleSaveClose(ActionEvent event) {
        dialogStage.close();
    }
    
    /**
     * 验证输入字段
     */
    private boolean validateInputs() {
        String errorMessage = "";
        
        // 验证测点编号
        if (pointIdField.getText() == null || pointIdField.getText().trim().isEmpty()) {
            errorMessage += "测点编号不能为空\n";
        }
        
        // 验证初始高程
        try {
            if (initialElevationField.getText() == null || initialElevationField.getText().trim().isEmpty()) {
                errorMessage += "初始高程不能为空\n";
            } else {
                Double.parseDouble(initialElevationField.getText().trim());
            }
        } catch (NumberFormatException e) {
            errorMessage += "初始高程必须是有效的数字\n";
        }
        
        // 验证速率报警值
        try {
            if (rateWarningField.getText() == null || rateWarningField.getText().trim().isEmpty()) {
                errorMessage += "速率报警值不能为空\n";
            } else {
                Double.parseDouble(rateWarningField.getText().trim());
            }
        } catch (NumberFormatException e) {
            errorMessage += "速率报警值必须是有效的数字\n";
        }
        
        // 验证累计报警值
        try {
            if (accumulatedWarningField.getText() == null || accumulatedWarningField.getText().trim().isEmpty()) {
                errorMessage += "累计报警值不能为空\n";
            } else {
                Double.parseDouble(accumulatedWarningField.getText().trim());
            }
        } catch (NumberFormatException e) {
            errorMessage += "累计报警值必须是有效的数字\n";
        }
        
        // 验证历史累计值（可选）
        if (historicalCumulativeField.getText() != null && !historicalCumulativeField.getText().trim().isEmpty()) {
            try {
                Double.parseDouble(historicalCumulativeField.getText().trim());
            } catch (NumberFormatException e) {
                errorMessage += "历史累计值必须是有效的数字\n";
            }
        }
        
        // 如果有错误，显示错误消息
        if (!errorMessage.isEmpty()) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("输入错误");
            alert.setHeaderText("请修正以下输入错误");
            alert.setContentText(errorMessage);
            alert.showAndWait();
            return false;
        }
        
        return true;
    }
    
    /**
     * 清除输入字段
     */
    private void clearInputFields() {
        pointIdField.clear();
        initialElevationField.clear();
        mileageField.clear();
        rateWarningField.clear();
        accumulatedWarningField.clear();
        historicalCumulativeField.clear();
        pointsTableView.getSelectionModel().clearSelection();
    }
    
    /**
     * 从Excel文件导入测点
     */
    private List<GroundwaterLevelPoint> importPointsFromExcel(File file) throws IOException {
        List<GroundwaterLevelPoint> importedPoints = new ArrayList<>();
        
        try (Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            
            boolean headerFound = false;
            int rowIndex = 0;
            
            for (Row row : sheet) {
                rowIndex++;
                
                // 跳过前几行，直到找到表头
                if (!headerFound) {
                    boolean hasPointIdHeader = false;
                    boolean hasElevationHeader = false;
                    
                    for (Cell cell : row) {
                        if (cell.getCellType() == CellType.STRING) {
                            String value = cell.getStringCellValue().trim();
                            if (value.contains("测点编号") || value.contains("点位编号")) {
                                hasPointIdHeader = true;
                            } else if (value.contains("初始高程") || value.contains("高程")) {
                                hasElevationHeader = true;
                            }
                        }
                    }
                    
                    if (hasPointIdHeader && hasElevationHeader) {
                        headerFound = true;
                    }
                    
                    continue;
                }
                
                // 处理数据行
                Cell pointIdCell = row.getCell(0);
                Cell elevationCell = row.getCell(1);
                Cell mileageCell = row.getCell(2);
                Cell rateWarningCell = row.getCell(3);
                Cell accumulatedWarningCell = row.getCell(4);
                Cell historicalCumulativeCell = row.getCell(5);
                
                // 如果测点编号和初始高程单元格为空，则跳过该行
                if (pointIdCell == null || elevationCell == null ||
                    getCellValueAsString(pointIdCell).isEmpty()) {
                    continue;
                }
                
                String pointId = getCellValueAsString(pointIdCell);
                double elevation = 0;
                
                try {
                    elevation = getCellValueAsDouble(elevationCell);
                } catch (NumberFormatException e) {
                    continue; // 如果高程无效，跳过该行
                }
                
                // 获取里程
                String mileage = "";
                if (mileageCell != null) {
                    mileage = getCellValueAsString(mileageCell);
                }
                
                // 获取报警值
                double rateWarning = 10.0; // 默认值
                double accumulatedWarning = 30.0; // 默认值
                double historicalCumulative = 0.0; // 默认值
                
                if (rateWarningCell != null) {
                    try {
                        rateWarning = getCellValueAsDouble(rateWarningCell);
                    } catch (NumberFormatException e) {
                        // 使用默认值
                    }
                }
                
                if (accumulatedWarningCell != null) {
                    try {
                        accumulatedWarning = getCellValueAsDouble(accumulatedWarningCell);
                    } catch (NumberFormatException e) {
                        // 使用默认值
                    }
                }
                
                if (historicalCumulativeCell != null) {
                    try {
                        historicalCumulative = getCellValueAsDouble(historicalCumulativeCell);
                    } catch (NumberFormatException e) {
                        // 使用默认值
                    }
                }
                
                // 创建测点并添加到列表
                GroundwaterLevelPoint point = new GroundwaterLevelPoint(pointId, elevation, mileage, 
                        rateWarning, accumulatedWarning, historicalCumulative);
                point.setOrderIndex(importedPoints.size());
                importedPoints.add(point);
            }
        }
        
        return importedPoints;
    }
    
    /**
     * 导出测点到Excel文件
     */
    private void exportPointsToExcel(File file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("地下水位测点配置");
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("测点编号");
            headerRow.createCell(1).setCellValue("初始高程(m)");
            headerRow.createCell(2).setCellValue("里程");
            headerRow.createCell(3).setCellValue("速率报警值(mm)");
            headerRow.createCell(4).setCellValue("累计报警值(mm)");
            headerRow.createCell(5).setCellValue("历史累计量(mm)");
            
            // 填充数据
            int rowNum = 1;
            for (GroundwaterLevelPoint point : points) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(point.getPointId());
                row.createCell(1).setCellValue(point.getInitialElevation());
                row.createCell(2).setCellValue(point.getMileage());
                row.createCell(3).setCellValue(point.getRateWarningValue());
                row.createCell(4).setCellValue(point.getAccumulatedWarningValue());
                row.createCell(5).setCellValue(point.getHistoricalCumulative());
            }
            
            // 调整列宽
            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 写入文件
            try (java.io.FileOutputStream outputStream = new java.io.FileOutputStream(file)) {
                workbook.write(outputStream);
            }
        }
    }
    
    /**
     * 获取单元格的字符串值
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // 对于数字，将其转换为字符串
                return String.valueOf((int)cell.getNumericCellValue());
            default:
                return "";
        }
    }
    
    /**
     * 获取单元格的数值
     */
    private double getCellValueAsDouble(Cell cell) throws NumberFormatException {
        if (cell == null) throw new NumberFormatException("空单元格");
        
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                // 尝试将字符串转换为数字
                return Double.parseDouble(cell.getStringCellValue().trim());
            default:
                throw new NumberFormatException("无效的数值类型");
        }
    }
} 