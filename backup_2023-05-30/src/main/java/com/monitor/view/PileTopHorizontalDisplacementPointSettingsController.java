package com.monitor.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.monitor.model.PileTopHorizontalDisplacementPoint;
import com.monitor.util.AlertUtil;
import com.monitor.util.ExcelUtil;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;

/**
 * 桩顶水平位移测点设置控制器
 */
public class PileTopHorizontalDisplacementPointSettingsController {

    @FXML private TableView<PileTopHorizontalDisplacementPoint> pointsTableView;
    @FXML private TableColumn<PileTopHorizontalDisplacementPoint, String> pointIdColumn;
    @FXML private TableColumn<PileTopHorizontalDisplacementPoint, Number> initialElevationColumn;
    @FXML private TableColumn<PileTopHorizontalDisplacementPoint, String> mileageColumn;
    @FXML private TableColumn<PileTopHorizontalDisplacementPoint, Number> rateWarningColumn;
    @FXML private TableColumn<PileTopHorizontalDisplacementPoint, Number> accumulatedWarningColumn;
    @FXML private TableColumn<PileTopHorizontalDisplacementPoint, Number> historicalCumulativeColumn;
    
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
    
    private ObservableList<PileTopHorizontalDisplacementPoint> points = FXCollections.observableArrayList();
    private Stage dialogStage;
    
    /**
     * 初始化控制器
     * 自动被JavaFX调用
     */
    @FXML
    private void initialize() {
        // 初始化表格列
        initializeTableColumns();
        
        // 设置表格选择监听器
        pointsTableView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    showPointDetails(newValue);
                    if (newValue != null) {
                        addButton.setText("更新");
                    } else {
                        addButton.setText("添加");
                    }
                });
        
        // 绑定数据源
        pointsTableView.setItems(points);
    }
    
    /**
     * 初始化表格列
     */
    private void initializeTableColumns() {
        // 设置单元格值工厂
        pointIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPointId()));
        initialElevationColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getInitialElevation()));
        mileageColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMileage()));
        rateWarningColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getRateWarning()));
        accumulatedWarningColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getAccumulatedWarning()));
        historicalCumulativeColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getHistoricalCumulative()));
        
        // 启用编辑
        pointsTableView.setEditable(true);
        
        // 设置文本字段单元格
        pointIdColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        mileageColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        
        // 设置数值字段单元格
        StringConverter<Number> doubleConverter = new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return object == null ? "" : String.format("%.3f", object.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                try {
                    return Double.parseDouble(string);
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }
        };
        
        initialElevationColumn.setCellFactory(TextFieldTableCell.forTableColumn(doubleConverter));
        rateWarningColumn.setCellFactory(TextFieldTableCell.forTableColumn(doubleConverter));
        accumulatedWarningColumn.setCellFactory(TextFieldTableCell.forTableColumn(doubleConverter));
        historicalCumulativeColumn.setCellFactory(TextFieldTableCell.forTableColumn(doubleConverter));
        
        // 设置编辑提交事件处理
        pointIdColumn.setOnEditCommit(event -> {
            PileTopHorizontalDisplacementPoint point = event.getRowValue();
            point.setPointId(event.getNewValue());
        });
        
        initialElevationColumn.setOnEditCommit(event -> {
            PileTopHorizontalDisplacementPoint point = event.getRowValue();
            point.setInitialElevation(event.getNewValue().doubleValue());
        });
        
        mileageColumn.setOnEditCommit(event -> {
            PileTopHorizontalDisplacementPoint point = event.getRowValue();
            point.setMileage(event.getNewValue());
        });
        
        rateWarningColumn.setOnEditCommit(event -> {
            PileTopHorizontalDisplacementPoint point = event.getRowValue();
            point.setRateWarning(event.getNewValue().doubleValue());
        });
        
        accumulatedWarningColumn.setOnEditCommit(event -> {
            PileTopHorizontalDisplacementPoint point = event.getRowValue();
            point.setAccumulatedWarning(event.getNewValue().doubleValue());
        });
        
        historicalCumulativeColumn.setOnEditCommit(event -> {
            PileTopHorizontalDisplacementPoint point = event.getRowValue();
            point.setHistoricalCumulative(event.getNewValue().doubleValue());
        });
    }
    
    /**
     * 显示测点详情
     */
    private void showPointDetails(PileTopHorizontalDisplacementPoint point) {
        if (point != null) {
            // 填充表单字段
            pointIdField.setText(point.getPointId());
            initialElevationField.setText(String.format("%.3f", point.getInitialElevation()));
            mileageField.setText(point.getMileage());
            rateWarningField.setText(String.format("%.2f", point.getRateWarning()));
            accumulatedWarningField.setText(String.format("%.2f", point.getAccumulatedWarning()));
            historicalCumulativeField.setText(String.format("%.2f", point.getHistoricalCumulative()));
        } else {
            // 清空表单字段
            clearInputFields();
        }
    }
    
    /**
     * 清空输入字段
     */
    private void clearInputFields() {
            pointIdField.setText("");
            initialElevationField.setText("");
            mileageField.setText("");
            rateWarningField.setText("");
            accumulatedWarningField.setText("");
            historicalCumulativeField.setText("");
        }
    
    /**
     * 验证输入
     */
    private boolean validateInputs() {
        String pointId = pointIdField.getText().trim();
        
        if (pointId.isEmpty()) {
            AlertUtil.showWarning("输入错误", "测点编号不能为空");
            return false;
        }
        
        try {
            if (!initialElevationField.getText().trim().isEmpty()) {
                Double.parseDouble(initialElevationField.getText().trim());
            }
            
            if (!rateWarningField.getText().trim().isEmpty()) {
                Double.parseDouble(rateWarningField.getText().trim());
            }
            
            if (!accumulatedWarningField.getText().trim().isEmpty()) {
                Double.parseDouble(accumulatedWarningField.getText().trim());
            }
            
            if (!historicalCumulativeField.getText().trim().isEmpty()) {
                Double.parseDouble(historicalCumulativeField.getText().trim());
            }
        } catch (NumberFormatException e) {
            AlertUtil.showWarning("输入错误", "请输入有效的数值");
            return false;
        }
        
        return true;
    }
    
    /**
     * 处理添加测点按钮点击事件
     */
    @FXML
    private void handleAddPoint(ActionEvent event) {
        if (!validateInputs()) {
            return;
        }
        
        try {
            // 获取输入值
            String pointId = pointIdField.getText().trim();
            double initialElevation = initialElevationField.getText().trim().isEmpty() ? 0.0 : 
                                     Double.parseDouble(initialElevationField.getText().trim());
            String mileage = mileageField.getText().trim();
            double rateWarning = rateWarningField.getText().trim().isEmpty() ? 0.0 : 
                               Double.parseDouble(rateWarningField.getText().trim());
            double accumulatedWarning = accumulatedWarningField.getText().trim().isEmpty() ? 0.0 : 
                                      Double.parseDouble(accumulatedWarningField.getText().trim());
            double historicalCumulative = historicalCumulativeField.getText().trim().isEmpty() ? 0.0 : 
                                        Double.parseDouble(historicalCumulativeField.getText().trim());
            
            PileTopHorizontalDisplacementPoint selectedPoint = pointsTableView.getSelectionModel().getSelectedItem();
            if (selectedPoint != null && addButton.getText().equals("更新")) {
                // 更新模式
                selectedPoint.setPointId(pointId);
                selectedPoint.setInitialElevation(initialElevation);
                selectedPoint.setMileage(mileage);
                selectedPoint.setRateWarning(rateWarning);
                selectedPoint.setAccumulatedWarning(accumulatedWarning);
                selectedPoint.setHistoricalCumulative(historicalCumulative);
                
                pointsTableView.refresh();
                clearInputFields();
                pointsTableView.getSelectionModel().clearSelection();
            } else {
                // 添加模式
                // 首先检查是否已存在同ID的测点
                boolean exists = false;
                for (PileTopHorizontalDisplacementPoint point : points) {
                    if (point.getPointId().equals(pointId)) {
                        exists = true;
                        break;
                    }
                }
                
                if (exists) {
                AlertUtil.showWarning("输入错误", "已存在相同编号的测点");
                return;
            }
            
            // 创建新测点
            PileTopHorizontalDisplacementPoint newPoint = new PileTopHorizontalDisplacementPoint(
                    pointId, initialElevation, mileage, rateWarning, accumulatedWarning);
            newPoint.setHistoricalCumulative(historicalCumulative);
            
            // 添加到列表
            points.add(newPoint);
            
            // 清空表单
                clearInputFields();
            
                // 取消选择
                pointsTableView.getSelectionModel().clearSelection();
            }
            
        } catch (NumberFormatException e) {
            AlertUtil.showWarning("输入错误", "请输入有效的数值");
        }
    }
    
    /**
     * 处理删除测点菜单项点击事件
     */
    @FXML
    private void handleDeletePoint(ActionEvent event) {
        PileTopHorizontalDisplacementPoint selectedPoint = pointsTableView.getSelectionModel().getSelectedItem();
        
        if (selectedPoint == null) {
            AlertUtil.showWarning("选择错误", "请先选择要删除的测点");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认删除");
        confirmAlert.setHeaderText("删除测点");
        confirmAlert.setContentText("确定要删除测点 " + selectedPoint.getPointId() + " 吗？");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            points.remove(selectedPoint);
            clearInputFields();
        }
    }
    
    /**
     * 处理批量导入按钮点击事件
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
                List<PileTopHorizontalDisplacementPoint> importedPoints = importPointsFromExcel(file);
                
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
                        for (PileTopHorizontalDisplacementPoint newPoint : importedPoints) {
                            newPoint.setOrderIndex(points.size()); // 设置排序索引
                            points.add(newPoint);
                        }
                    } else {
                        // 只合并新测点 - 判断哪些是新加的测点
                        for (PileTopHorizontalDisplacementPoint importedPoint : importedPoints) {
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
     * 从Excel文件导入测点数据
     */
    private List<PileTopHorizontalDisplacementPoint> importPointsFromExcel(File file) throws IOException {
        List<PileTopHorizontalDisplacementPoint> importedPoints = new ArrayList<>();
        
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
                    else if (cellValue.contains("初始测值") || cellValue.contains("初始高程")) initialElevationCol = i;
                    else if (cellValue.contains("里程")) mileageCol = i;
                    else if (cellValue.contains("速率报警")) rateWarningCol = i;
                    else if (cellValue.contains("累计报警")) accumulatedWarningCol = i;
                    else if (cellValue.contains("历史累计")) historicalCumulativeCol = i;
                }
            }
            
            // 检查必要的列是否存在
            if (pointIdCol == -1 || initialElevationCol == -1) {
                AlertUtil.showError("格式错误", "Excel文件必须包含'测点编号'和'初始测值'列");
                return importedPoints;
            }
            
            // 读取数据行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Cell pointIdCell = row.getCell(pointIdCol);
                if (pointIdCell == null) continue;
                
                String pointId = getCellValueAsString(pointIdCell);
                if (pointId == null || pointId.trim().isEmpty()) continue;
                
                try {
                    // 读取各列数据
                    double initialElevation = 0.0;
                    if (initialElevationCol >= 0) {
                        Cell cell = row.getCell(initialElevationCol);
                        if (cell != null) {
                            initialElevation = getNumericCellValue(cell, 0.0);
                        }
                    }
                    
                    String mileage = "";
                    if (mileageCol >= 0) {
                        Cell cell = row.getCell(mileageCol);
                        if (cell != null) {
                            mileage = getCellValueAsString(cell);
                            if (mileage == null) mileage = "";
                        }
                    }
                    
                    double rateWarningValue = 0.0;
                    if (rateWarningCol >= 0) {
                        Cell cell = row.getCell(rateWarningCol);
                        if (cell != null) {
                            rateWarningValue = getNumericCellValue(cell, 0.0);
                        }
                    }
                    
                    double accumulatedWarningValue = 0.0;
                    if (accumulatedWarningCol >= 0) {
                        Cell cell = row.getCell(accumulatedWarningCol);
                        if (cell != null) {
                            accumulatedWarningValue = getNumericCellValue(cell, 0.0);
                        }
                    }
                    
                    double historicalCumulative = 0.0;
                    if (historicalCumulativeCol >= 0) {
                        Cell cell = row.getCell(historicalCumulativeCol);
                        if (cell != null) {
                            historicalCumulative = getNumericCellValue(cell, 0.0);
                        }
                    }
                    
                    // 创建测点对象并添加到列表
                    PileTopHorizontalDisplacementPoint point = new PileTopHorizontalDisplacementPoint(
                        pointId.trim(), initialElevation, mileage.trim(), rateWarningValue, accumulatedWarningValue
                    );
                    point.setHistoricalCumulative(historicalCumulative);
                    point.setOrderIndex(i - 1); // 保持Excel中的顺序
                    importedPoints.add(point);
                } catch (Exception e) {
                    // 跳过无效行
                    System.err.println("跳过无效行: " + i + ", 错误: " + e.getMessage());
                }
            }
        }
        
        return importedPoints;
    }
    
    /**
     * 处理导出按钮点击事件
     */
    @FXML
    private void handleExport(ActionEvent event) {
        if (points.isEmpty()) {
            AlertUtil.showWarning("导出错误", "没有数据可导出。");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出测点数据");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel文件", "*.xlsx"));
        fileChooser.setInitialFileName("桩顶水平位移测点数据.xlsx");
        
        File file = fileChooser.showSaveDialog(dialogStage);
        if (file != null) {
            try {
                exportPointsToExcel(file);
                AlertUtil.showInformation("导出成功", "数据已成功导出到: " + file.getAbsolutePath());
            } catch (IOException e) {
                AlertUtil.showError("导出错误", "导出数据时发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 导出测点数据到Excel
     */
    private void exportPointsToExcel(File file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("桩顶水平位移测点");
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("测点编号");
            headerRow.createCell(1).setCellValue("初始测值(m)");
            headerRow.createCell(2).setCellValue("里程");
            headerRow.createCell(3).setCellValue("速率报警值(mm)");
            headerRow.createCell(4).setCellValue("累计报警值(mm)");
            headerRow.createCell(5).setCellValue("历史累计量(mm)");
            
            // 填充数据
            int rowNum = 1;
            for (PileTopHorizontalDisplacementPoint point : points) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(point.getPointId());
                row.createCell(1).setCellValue(point.getInitialElevation());
                row.createCell(2).setCellValue(point.getMileage());
                row.createCell(3).setCellValue(point.getRateWarning());
                row.createCell(4).setCellValue(point.getAccumulatedWarning());
                row.createCell(5).setCellValue(point.getHistoricalCumulative());
            }
            
            // 调整列宽
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
     * 获取单元格字符串值
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // 对于数值，转换为字符串
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getStringCellValue());
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
     * 从单元格获取数值，支持默认值
     */
    private double getNumericCellValue(Cell cell, double defaultValue) {
        if (cell == null) {
            return defaultValue;
        }
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                case STRING:
                    try {
                        return Double.parseDouble(cell.getStringCellValue().trim());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                case FORMULA:
                    try {
                        return cell.getNumericCellValue();
                    } catch (Exception e) {
                        return defaultValue;
                    }
                default:
                    return defaultValue;
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * 处理保存并关闭按钮点击事件
     */
    @FXML
    private void handleSaveAndClose(ActionEvent event) {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
    
    /**
     * 设置对话框舞台
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
        
        // 设置对话框标题
        dialogStage.setTitle("桩顶水平位移测点设置");
    }
    
    /**
     * 设置初始数据
     */
    public void setInitialData(List<PileTopHorizontalDisplacementPoint> initialPoints) {
        points.clear();
        if (initialPoints != null) {
            for (PileTopHorizontalDisplacementPoint point : initialPoints) {
                // 创建新实例以避免引用问题
                PileTopHorizontalDisplacementPoint newPoint = new PileTopHorizontalDisplacementPoint(
                    point.getPointId(),
                    point.getInitialElevation(),
                    point.getMileage(),
                    point.getRateWarning(),
                    point.getAccumulatedWarning()
                );
                newPoint.setHistoricalCumulative(point.getHistoricalCumulative());
                points.add(newPoint);
            }
            
            // 更新表格
            pointsTableView.refresh();
        }
    }
    
    /**
     * 获取测点列表
     */
    public List<PileTopHorizontalDisplacementPoint> getPoints() {
        return new ArrayList<>(points);
    }
} 