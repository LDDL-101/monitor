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

import com.monitor.model.PileDisplacementPoint;
import com.monitor.util.AlertUtil;
import com.monitor.util.ExcelUtil;
import com.monitor.util.WindowManager;

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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * 桩顶竖向位移点设置对话框控制器
 */
public class PileTopDisplacementPointSettingsController {

    @FXML private TableView<PileDisplacementPoint> pointsTableView;
    @FXML private TableColumn<PileDisplacementPoint, String> pointIdColumn;
    @FXML private TableColumn<PileDisplacementPoint, Number> initialElevationColumn;
    @FXML private TableColumn<PileDisplacementPoint, String> mileageColumn;
    @FXML private TableColumn<PileDisplacementPoint, Number> rateWarningColumn;
    @FXML private TableColumn<PileDisplacementPoint, Number> accumulatedWarningColumn;
    @FXML private TableColumn<PileDisplacementPoint, Number> historicalCumulativeColumn;
    
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
    
    private ObservableList<PileDisplacementPoint> points = FXCollections.observableArrayList();
    private Stage dialogStage;
    
    /**
     * 初始化控制器
     * 自动被JavaFX调用
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
        initialElevationColumn.setCellFactory(column -> new TableCell<PileDisplacementPoint, Number>() {
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
        
        // 添加表格选择监听器，以便在选择行时更新输入字段
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
     * @param dialogStage 对话框Stage
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
        
        // 设置对话框标题
        dialogStage.setTitle("桩顶竖向位移测点设置");
    }
    
    /**
     * 设置初始数据
     * @param initialPoints 初始测点列表
     */
    public void setInitialData(List<PileDisplacementPoint> initialPoints) {
        if (initialPoints != null) {
            // 复制列表，以避免修改原始数据
            for (PileDisplacementPoint point : initialPoints) {
                // 创建新实例以避免引用问题
                PileDisplacementPoint newPoint = new PileDisplacementPoint(
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
            
            // 更新数据显示
            pointsTableView.refresh();
        }
    }
    
    /**
     * 获取设置的测点数据
     * @return 测点列表
     */
    public List<PileDisplacementPoint> getPoints() {
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
            
            PileDisplacementPoint selectedPoint = pointsTableView.getSelectionModel().getSelectedItem();
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
                for (PileDisplacementPoint point : points) {
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
                PileDisplacementPoint newPoint = new PileDisplacementPoint(
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
     * 处理批量导入按钮事件
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
                List<PileDisplacementPoint> importedPoints = importPointsFromExcel(file);
                
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
                        for (PileDisplacementPoint newPoint : importedPoints) {
                            newPoint.setOrderIndex(points.size()); // 设置排序索引
                            points.add(newPoint);
                        }
                    } else {
                        // 只合并新测点 - 判断哪些是新加的测点
                        for (PileDisplacementPoint importedPoint : importedPoints) {
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
     * 处理删除测点事件
     */
    @FXML
    private void handleDeletePoint(ActionEvent event) {
        PileDisplacementPoint selectedPoint = pointsTableView.getSelectionModel().getSelectedItem();
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
        fileChooser.setInitialFileName("桩顶竖向位移测点档案.xlsx");
        
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
     * 处理关闭按钮事件，使用标准窗口关闭
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
     * @return 如果所有必填字段有效则返回true
     */
    private boolean validateInputs() {
        String errorMessage = "";
        
        if (pointIdField.getText() == null || pointIdField.getText().trim().isEmpty()) {
            errorMessage += "测点编号不能为空\n";
        }
        
        try {
            if (initialElevationField.getText() == null || initialElevationField.getText().trim().isEmpty()) {
                errorMessage += "初始高程不能为空\n";
            } else {
                Double.parseDouble(initialElevationField.getText().trim());
            }
        } catch (NumberFormatException e) {
            errorMessage += "初始高程必须是有效的数字\n";
        }
        
        try {
            if (rateWarningField.getText() == null || rateWarningField.getText().trim().isEmpty()) {
                errorMessage += "速率报警值不能为空\n";
            } else {
                Double.parseDouble(rateWarningField.getText().trim());
            }
        } catch (NumberFormatException e) {
            errorMessage += "速率报警值必须是有效的数字\n";
        }
        
        try {
            if (accumulatedWarningField.getText() == null || accumulatedWarningField.getText().trim().isEmpty()) {
                errorMessage += "累计报警值不能为空\n";
            } else {
                Double.parseDouble(accumulatedWarningField.getText().trim());
            }
        } catch (NumberFormatException e) {
            errorMessage += "累计报警值必须是有效的数字\n";
        }
        
        try {
            if (historicalCumulativeField.getText() != null && !historicalCumulativeField.getText().trim().isEmpty()) {
                Double.parseDouble(historicalCumulativeField.getText().trim());
            }
        } catch (NumberFormatException e) {
            errorMessage += "历史累计量必须是有效的数字\n";
        }
        
        if (errorMessage.isEmpty()) {
            return true;
        } else {
            AlertUtil.showError("输入验证", errorMessage);
            return false;
        }
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
     * 从Excel导入测点数据
     */
    private List<PileDisplacementPoint> importPointsFromExcel(File file) throws IOException {
        List<PileDisplacementPoint> importedPoints = new ArrayList<>();
        
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
                PileDisplacementPoint point = new PileDisplacementPoint(
                    pointId, initialElevation, mileage, rateWarningValue, accumulatedWarningValue, historicalCumulative
                );
                point.setOrderIndex(i - 1); // 保持Excel中的顺序
                importedPoints.add(point);
            }
        }
        
        return importedPoints;
    }
    
    /**
     * 导出测点数据到Excel
     */
    private void exportPointsToExcel(File file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("桩顶竖向位移测点");
            
            // 创建标题行
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("测点编号");
            headerRow.createCell(1).setCellValue("初始高程(m)");
            headerRow.createCell(2).setCellValue("里程");
            headerRow.createCell(3).setCellValue("速率报警值(mm)");
            headerRow.createCell(4).setCellValue("累计报警值(mm)");
            headerRow.createCell(5).setCellValue("历史累计量(mm)");
            
            // 填充数据
            for (int i = 0; i < points.size(); i++) {
                PileDisplacementPoint point = points.get(i);
                Row row = sheet.createRow(i + 1);
                
                row.createCell(0).setCellValue(point.getPointId());
                row.createCell(1).setCellValue(point.getInitialElevation());
                row.createCell(2).setCellValue(point.getMileage());
                row.createCell(3).setCellValue(point.getRateWarningValue());
                row.createCell(4).setCellValue(point.getAccumulatedWarningValue());
                row.createCell(5).setCellValue(point.getHistoricalCumulative());
            }
            
            // 自动调整列宽
            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 写入文件
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }
        }
    }
    
    /**
     * 获取单元格的字符串值
     */
    private String getCellValueAsString(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long)cell.getNumericCellValue());
            default:
                return "";
        }
    }
} 