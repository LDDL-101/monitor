package com.monitor.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.monitor.model.ColumnDisplacementPoint;
import com.monitor.util.AlertUtil;
import com.monitor.util.ExcelUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.MenuItem;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * 立柱竖向位移点设置对话框控制器
 */
public class ColumnDisplacementPointSettingsController {

    @FXML private TableView<ColumnDisplacementPoint> pointsTableView;
    @FXML private TableColumn<ColumnDisplacementPoint, String> pointIdColumn;
    @FXML private TableColumn<ColumnDisplacementPoint, Number> initialElevationColumn;
    @FXML private TableColumn<ColumnDisplacementPoint, String> mileageColumn;
    @FXML private TableColumn<ColumnDisplacementPoint, Number> rateWarningColumn;
    @FXML private TableColumn<ColumnDisplacementPoint, Number> accumulatedWarningColumn;
    @FXML private TableColumn<ColumnDisplacementPoint, Number> historicalCumulativeColumn;
    
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
    
    private ObservableList<ColumnDisplacementPoint> points = FXCollections.observableArrayList();
    private Stage dialogStage;
    
    /**
     * 初始化控制器
     */
    @FXML
    private void initialize() {
        // 设置表格列的单元格值工厂
        pointIdColumn.setCellValueFactory(new PropertyValueFactory<>("pointId"));
        initialElevationColumn.setCellValueFactory(new PropertyValueFactory<>("initialElevation"));
        mileageColumn.setCellValueFactory(new PropertyValueFactory<>("mileage"));
        rateWarningColumn.setCellValueFactory(new PropertyValueFactory<>("rateWarningValue"));
        accumulatedWarningColumn.setCellValueFactory(new PropertyValueFactory<>("accumulatedWarningValue"));
        historicalCumulativeColumn.setCellValueFactory(new PropertyValueFactory<>("historicalCumulative"));
        
        // 设置数值列的格式
        initialElevationColumn.setCellFactory(column -> new TableCell<ColumnDisplacementPoint, Number>() {
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
        
        // 设置表格的数据源
        pointsTableView.setItems(points);
        
        // 添加表格选择监听器
        pointsTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                pointIdField.setText(newValue.getPointId());
                initialElevationField.setText(String.format("%.4f", newValue.getInitialElevation()));
                mileageField.setText(newValue.getMileage());
                rateWarningField.setText(String.format("%.2f", newValue.getRateWarningValue()));
                accumulatedWarningField.setText(String.format("%.2f", newValue.getAccumulatedWarningValue()));
                historicalCumulativeField.setText(String.format("%.2f", newValue.getHistoricalCumulative()));
                
                addButton.setText("更新");
            } else {
                clearInputFields();
                addButton.setText("添加");
            }
        });
    }
    
    /**
     * 设置对话框的Stage
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
        dialogStage.setTitle("立柱竖向位移测点设置");
    }
    
    /**
     * 设置初始数据
     */
    public void setInitialData(List<ColumnDisplacementPoint> initialPoints) {
        if (initialPoints != null) {
            points.clear();
            
            for (ColumnDisplacementPoint point : initialPoints) {
                ColumnDisplacementPoint newPoint = new ColumnDisplacementPoint(
                    point.getPointId(),
                    point.getInitialElevation(),
                    point.getMileage(),
                    point.getRateWarningValue(),
                    point.getAccumulatedWarningValue(),
                    point.getHistoricalCumulative()
                );
                newPoint.setOrderIndex(point.getOrderIndex());
                points.add(newPoint);
            }
            
            pointsTableView.refresh();
        }
    }
    
    /**
     * 获取设置的测点数据
     */
    public List<ColumnDisplacementPoint> getPoints() {
        return new ArrayList<>(points);
    }
    
    /**
     * 处理添加/更新测点按钮事件
     */
    @FXML
    private void handleAddPoint(ActionEvent event) {
        if (!validateInputs()) {
            return;
        }
        
        try {
            // 获取输入值
            String pointId = pointIdField.getText().trim();
            double initialElevation = Double.parseDouble(initialElevationField.getText().trim());
            String mileage = mileageField.getText().trim();
            double rateWarningValue = Double.parseDouble(rateWarningField.getText().trim());
            double accumulatedWarningValue = Double.parseDouble(accumulatedWarningField.getText().trim());
            double historicalCumulative = 0.0;
            
            if (!historicalCumulativeField.getText().trim().isEmpty()) {
                historicalCumulative = Double.parseDouble(historicalCumulativeField.getText().trim());
            }
            
            ColumnDisplacementPoint selectedPoint = pointsTableView.getSelectionModel().getSelectedItem();
            if (selectedPoint != null && addButton.getText().equals("更新")) {
                // 更新模式
                selectedPoint.setPointId(pointId);
                selectedPoint.setInitialElevation(initialElevation);
                selectedPoint.setMileage(mileage);
                selectedPoint.setRateWarningValue(rateWarningValue);
                selectedPoint.setAccumulatedWarningValue(accumulatedWarningValue);
                selectedPoint.setHistoricalCumulative(historicalCumulative);
                
                pointsTableView.refresh();
            } else {
                // 添加模式
                // 首先检查是否已存在同ID的测点
                boolean exists = false;
                for (ColumnDisplacementPoint point : points) {
                    if (point.getPointId().equals(pointId)) {
                        exists = true;
                        break;
                    }
                }
                
                if (exists) {
                    AlertUtil.showWarning("添加失败", "已存在测点编号为 " + pointId + " 的测点");
                    return;
                }
                
                // 创建新的测点
                ColumnDisplacementPoint newPoint = new ColumnDisplacementPoint(
                    pointId, initialElevation, mileage, rateWarningValue, accumulatedWarningValue, historicalCumulative
                    );
                newPoint.setOrderIndex(points.size()); // 保持原有顺序
                    points.add(newPoint);
            }
            
            // 清空输入字段
            clearInputFields();
            addButton.setText("添加");
            pointsTableView.getSelectionModel().clearSelection();
            
        } catch (NumberFormatException e) {
            AlertUtil.showError("输入错误", "请确保所有数值字段包含有效的数字");
        }
    }
    
    /**
     * 批量导入测点
     */
    @FXML
    private void handleBatchImport(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择Excel文件");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel文件", "*.xlsx", "*.xls")
        );
        
        File file = fileChooser.showOpenDialog(dialogStage);
        if (file != null) {
            try {
                List<ColumnDisplacementPoint> importedPoints = importPointsFromExcel(file);
                
                if (importedPoints != null && !importedPoints.isEmpty()) {
                    // 提示用户选择是否更新初始值
                    boolean clearAndReplace = AlertUtil.showConfirmationDialog(
                        "导入选项", 
                        "是否更新初始值",
                        "清除现有测点并更新",
                        "只合并新测点"
                    );
                    
                    if (clearAndReplace) {
                        // 清除现有测点并更新
                        points.clear();
                        // 添加导入的所有测点
                        for (ColumnDisplacementPoint newPoint : importedPoints) {
                            newPoint.setOrderIndex(points.size()); // 设置排序索引
                            points.add(newPoint);
                        }
                    } else {
                        // 只合并新测点 - 判断哪些是新加的测点
                        for (ColumnDisplacementPoint importedPoint : importedPoints) {
                            boolean exists = false;
                            
                            // 检查是否已存在该测点
                            for (int i = 0; i < points.size(); i++) {
                                if (points.get(i).getPointId().equals(importedPoint.getPointId())) {
                                    // 如果存在，则更新
                                    points.set(i, importedPoint);
                                    exists = true;
                                    break;
                                }
                            }
                            
                            // 如果不存在，则添加
                            if (!exists) {
                                importedPoint.setOrderIndex(points.size()); // 设置顺序索引
                                points.add(importedPoint);
                            }
                        }
                    }
                    
                    pointsTableView.refresh();
                    AlertUtil.showInformation("导入成功", "成功导入 " + importedPoints.size() + " 个测点");
                }
            } catch (IOException e) {
                AlertUtil.showError("导入失败", "无法读取Excel文件: " + e.getMessage());
            }
        }
    }
    
    /**
     * 处理删除测点菜单项事件
     */
    @FXML
    private void handleDeletePoint(ActionEvent event) {
        ColumnDisplacementPoint selectedPoint = pointsTableView.getSelectionModel().getSelectedItem();
        if (selectedPoint != null) {
            if (AlertUtil.showConfirmationDialog("确认删除", "确定要删除测点 " + selectedPoint.getPointId() + " 吗?")) {
                points.remove(selectedPoint);
                clearInputFields();
            }
        } else {
            AlertUtil.showWarning("未选择", "请先选择要删除的测点");
        }
    }
    
    /**
     * 处理导出按钮事件
     */
    @FXML
    private void handleExport(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存Excel文件");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel文件", "*.xlsx")
        );
        fileChooser.setInitialFileName("立柱竖向位移测点档案.xlsx");
        
        File file = fileChooser.showSaveDialog(dialogStage);
        if (file != null) {
            try {
                exportPointsToExcel(file);
                AlertUtil.showInformation("导出成功", "测点档案已成功导出到: " + file.getAbsolutePath());
            } catch (IOException e) {
                AlertUtil.showError("导出失败", "无法写入Excel文件: " + e.getMessage());
            }
        }
    }
    
    /**
     * 处理关闭按钮事件
     */
    @FXML
    private void handleClose(ActionEvent event) {
        if (dialogStage != null) {
        dialogStage.close();
        }
    }
    
    /**
     * 处理保存并关闭按钮事件
     */
    @FXML
    private void handleSaveClose(ActionEvent event) {
        if (dialogStage != null) {
        dialogStage.close();
        }
    }
    
    /**
     * 验证输入字段
     */
    private boolean validateInputs() {
        String pointId = pointIdField.getText().trim();
        if (pointId.isEmpty()) {
            AlertUtil.showWarning("验证错误", "测点ID不能为空");
            pointIdField.requestFocus();
            return false;
        }
        
        String initialElevationStr = initialElevationField.getText().trim();
        if (initialElevationStr.isEmpty()) {
            AlertUtil.showWarning("验证错误", "初始高程不能为空");
            initialElevationField.requestFocus();
            return false;
        }
        
        try {
            Double.parseDouble(initialElevationStr);
        } catch (NumberFormatException e) {
            AlertUtil.showWarning("验证错误", "初始高程必须是有效的数字");
            initialElevationField.requestFocus();
            return false;
        }
        
        String rateWarningStr = rateWarningField.getText().trim();
        if (rateWarningStr.isEmpty()) {
            AlertUtil.showWarning("验证错误", "速率报警值不能为空");
            rateWarningField.requestFocus();
            return false;
        }
        
        try {
            Double.parseDouble(rateWarningStr);
        } catch (NumberFormatException e) {
            AlertUtil.showWarning("验证错误", "速率报警值必须是有效的数字");
            rateWarningField.requestFocus();
            return false;
        }
        
        String accumulatedWarningStr = accumulatedWarningField.getText().trim();
        if (accumulatedWarningStr.isEmpty()) {
            AlertUtil.showWarning("验证错误", "累计报警值不能为空");
            accumulatedWarningField.requestFocus();
            return false;
        }
        
        try {
            Double.parseDouble(accumulatedWarningStr);
        } catch (NumberFormatException e) {
            AlertUtil.showWarning("验证错误", "累计报警值必须是有效的数字");
            accumulatedWarningField.requestFocus();
            return false;
        }
        
        String historicalCumulativeStr = historicalCumulativeField.getText().trim();
        if (!historicalCumulativeStr.isEmpty()) {
            try {
                Double.parseDouble(historicalCumulativeStr);
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("验证错误", "历史累计量必须是有效的数字");
                historicalCumulativeField.requestFocus();
            return false;
            }
        }
        
        return true;
    }
    
    /**
     * 清空输入字段
     */
    private void clearInputFields() {
        pointIdField.clear();
        initialElevationField.clear();
        mileageField.clear();
        rateWarningField.clear();
        accumulatedWarningField.clear();
        historicalCumulativeField.clear();
        
        addButton.setText("添加");
    }
    
    /**
     * 从Excel文件导入测点数据
     */
    private List<ColumnDisplacementPoint> importPointsFromExcel(File file) throws IOException {
        List<ColumnDisplacementPoint> importedPoints = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            
            // 按首行判断列的位置
            int pointIdCol = -1, initialElevationCol = -1, mileageCol = -1, 
                rateWarningCol = -1, accumulatedWarningCol = -1, historicalCumulativeCol = -1;
            
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String cellValue = cell.getStringCellValue().trim();
                    if (cellValue.contains("测点编号")) pointIdCol = i;
                    else if (cellValue.contains("初始高程")) initialElevationCol = i;
                    else if (cellValue.contains("里程")) mileageCol = i;
                    else if (cellValue.contains("速率报警")) rateWarningCol = i;
                    else if (cellValue.contains("累计报警")) accumulatedWarningCol = i;
                    else if (cellValue.contains("历史累计")) historicalCumulativeCol = i;
                }
            }
            
            // 检查必要的列是否存在
            if (pointIdCol == -1 || initialElevationCol == -1) {
                AlertUtil.showError("格式错误", "Excel文件必须包含'测点编号'和'初始高程'列");
                return importedPoints;
            }
            
            // 读取数据行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                // 读取测点编号
                Cell pointIdCell = row.getCell(pointIdCol);
                if (pointIdCell == null) continue;
                
                String pointId = getCellValueAsString(pointIdCell);
                if (pointId.isEmpty()) continue;
                
                // 读取初始高程
                Cell initialElevationCell = row.getCell(initialElevationCol);
                double initialElevation = 0;
                if (initialElevationCell != null) {
                    initialElevation = initialElevationCell.getNumericCellValue();
                }
                
                // 读取里程
                String mileage = "";
                if (mileageCol != -1) {
                    Cell mileageCell = row.getCell(mileageCol);
                    if (mileageCell != null) {
                        mileage = getCellValueAsString(mileageCell);
                    }
                }
                
                // 读取速率报警值
                double rateWarningValue = 5.0; // 默认值
                if (rateWarningCol != -1) {
                    Cell rateWarningCell = row.getCell(rateWarningCol);
                    if (rateWarningCell != null) {
                        rateWarningValue = rateWarningCell.getNumericCellValue();
                    }
                }
                
                // 读取累计报警值
                double accumulatedWarningValue = 20.0; // 默认值
                if (accumulatedWarningCol != -1) {
                    Cell accumulatedWarningCell = row.getCell(accumulatedWarningCol);
                    if (accumulatedWarningCell != null) {
                        accumulatedWarningValue = accumulatedWarningCell.getNumericCellValue();
                    }
                }
                
                // 读取历史累计量
                double historicalCumulative = 0.0;
                if (historicalCumulativeCol != -1) {
                    Cell historicalCumulativeCell = row.getCell(historicalCumulativeCol);
                    if (historicalCumulativeCell != null) {
                        historicalCumulative = historicalCumulativeCell.getNumericCellValue();
                    }
                    }
                    
                // 创建测点对象并添加到列表
                ColumnDisplacementPoint point = new ColumnDisplacementPoint(
                    pointId, initialElevation, mileage, rateWarningValue, accumulatedWarningValue, historicalCumulative
                    );
                point.setOrderIndex(i - 1); // 保持Excel中的顺序
                    importedPoints.add(point);
            }
        }
        
        return importedPoints;
    }
    
    /**
     * 将测点数据导出到Excel文件
     */
    private void exportPointsToExcel(File file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("立柱竖向位移测点");
            
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("测点编号");
            headerRow.createCell(1).setCellValue("初始高程(m)");
            headerRow.createCell(2).setCellValue("里程");
            headerRow.createCell(3).setCellValue("速率报警值(mm)");
            headerRow.createCell(4).setCellValue("累计报警值(mm)");
            headerRow.createCell(5).setCellValue("历史累计量(mm)");
            
            List<ColumnDisplacementPoint> sortedPoints = new ArrayList<>(points);
            sortedPoints.sort((p1, p2) -> Integer.compare(p1.getOrderIndex(), p2.getOrderIndex()));
            
            int rowNum = 1;
            for (ColumnDisplacementPoint point : sortedPoints) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(point.getPointId());
                row.createCell(1).setCellValue(point.getInitialElevation());
                row.createCell(2).setCellValue(point.getMileage());
                row.createCell(3).setCellValue(point.getRateWarningValue());
                row.createCell(4).setCellValue(point.getAccumulatedWarningValue());
                row.createCell(5).setCellValue(point.getHistoricalCumulative());
            }
            
            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }
            
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
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
                return cell.getStringCellValue();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
} 