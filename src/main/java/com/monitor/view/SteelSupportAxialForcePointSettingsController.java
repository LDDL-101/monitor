package com.monitor.view;

import com.monitor.model.SteelSupportAxialForcePoint;
import com.monitor.util.AlertUtil;
import com.monitor.util.ExcelUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 钢支撑轴力测点设置控制器
 */
public class SteelSupportAxialForcePointSettingsController {

    @FXML private TableView<SteelSupportAxialForcePoint> pointsTableView;
    @FXML private TableColumn<SteelSupportAxialForcePoint, String> pointIdColumn;
    @FXML private TableColumn<SteelSupportAxialForcePoint, String> mileageColumn;
    @FXML private TableColumn<SteelSupportAxialForcePoint, Number> minValueColumn;
    @FXML private TableColumn<SteelSupportAxialForcePoint, Number> controlValueColumn;
    @FXML private TableColumn<SteelSupportAxialForcePoint, Number> historicalCumulativeColumn;

    @FXML private TextField pointIdField;
    @FXML private TextField mileageField;
    @FXML private TextField minValueField;
    @FXML private TextField controlValueField;
    @FXML private TextField historicalCumulativeField;

    @FXML private Button addButton;
    @FXML private Button batchImportButton;
    @FXML private Button exportButton;
    @FXML private Button saveCloseButton;

    @FXML private MenuItem deleteMenuItem;

    private ObservableList<SteelSupportAxialForcePoint> points = FXCollections.observableArrayList();
    private Stage dialogStage;

    /**
     * 初始化方法，设置表格列绑定等
     */
    @FXML
    private void initialize() {
        // 设置表格列的单元格值工厂
        pointIdColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getPointId()));
        mileageColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getMileage()));
        minValueColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getMinValue()));
        controlValueColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getControlValue()));
        historicalCumulativeColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getHistoricalCumulative()));

        // 格式化数字列显示
        minValueColumn.setCellFactory(column -> new TableCell<SteelSupportAxialForcePoint, Number>() {
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

        controlValueColumn.setCellFactory(column -> new TableCell<SteelSupportAxialForcePoint, Number>() {
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

        historicalCumulativeColumn.setCellFactory(column -> new TableCell<SteelSupportAxialForcePoint, Number>() {
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

        // 设置表格数据
        pointsTableView.setItems(points);

        // 设置行点击事件，点击一行时填充编辑区域
        pointsTableView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        pointIdField.setText(newValue.getPointId());
                        mileageField.setText(newValue.getMileage());
                        minValueField.setText(String.format("%.2f", newValue.getMinValue()));
                        controlValueField.setText(String.format("%.2f", newValue.getControlValue()));
                        historicalCumulativeField.setText(String.format("%.2f", newValue.getHistoricalCumulative()));
                    }
                });
    }

    /**
     * 设置对话框的Stage
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
        // 设置对话框标题
        dialogStage.setTitle("钢支撑轴力测点设置");
    }

    /**
     * 设置初始数据
     */
    public void setInitialData(List<SteelSupportAxialForcePoint> initialPoints) {
        if (initialPoints != null) {
            points.clear();
            points.addAll(initialPoints);
            
            // 更新排序索引，确保顺序正确
            for (int i = 0; i < points.size(); i++) {
                points.get(i).setOrderIndex(i);
            }
            
            // 按排序索引重新排序
            points.sort((p1, p2) -> Integer.compare(p1.getOrderIndex(), p2.getOrderIndex()));
            
            // 刷新表格
            pointsTableView.refresh();
        }
    }

    /**
     * 获取测点列表
     */
    public List<SteelSupportAxialForcePoint> getPoints() {
        return new ArrayList<>(points);
    }

    /**
     * 处理添加测点按钮事件
     */
    @FXML
    private void handleAddPoint(ActionEvent event) {
        if (!validateInputs()) {
            return;
        }

        String pointId = pointIdField.getText().trim();
        String mileage = mileageField.getText().trim();
        
        double minValue = 0;
        double controlValue = 0;
        double historicalCumulative = 0;
        
        try {
            minValue = Double.parseDouble(minValueField.getText().trim());
        } catch (NumberFormatException e) {
            AlertUtil.showError("输入错误", "最小值必须是有效的数字");
            return;
        }
        
        try {
            controlValue = Double.parseDouble(controlValueField.getText().trim());
        } catch (NumberFormatException e) {
            AlertUtil.showError("输入错误", "控制值必须是有效的数字");
            return;
        }
        
        try {
            historicalCumulative = Double.parseDouble(historicalCumulativeField.getText().trim());
        } catch (NumberFormatException e) {
            AlertUtil.showError("输入错误", "历史累计值必须是有效的数字");
            return;
        }

        // 检查是否已存在相同ID的测点
        boolean isEdit = false;
        int editIndex = -1;
        
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).getPointId().equals(pointId)) {
                isEdit = true;
                editIndex = i;
                break;
            }
        }

        SteelSupportAxialForcePoint point = new SteelSupportAxialForcePoint(
                pointId, mileage, minValue, controlValue, historicalCumulative);
        
        if (isEdit) {
            // 更新现有测点
            point.setOrderIndex(points.get(editIndex).getOrderIndex());
            points.set(editIndex, point);
            AlertUtil.showInformation("更新成功", "测点 " + pointId + " 已更新");
        } else {
            // 添加新测点
            point.setOrderIndex(points.size());
            points.add(point);
            AlertUtil.showInformation("添加成功", "测点 " + pointId + " 已添加");
        }

        // 清空输入字段
        clearInputFields();
    }

    /**
     * 处理批量导入按钮事件
     */
    @FXML
    private void handleBatchImport(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择Excel文件");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel文件", "*.xlsx", "*.xls"));
        
        File file = fileChooser.showOpenDialog(dialogStage);
        
        if (file != null) {
            try {
                List<SteelSupportAxialForcePoint> importedPoints = importPointsFromExcel(file);
                
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
                        for (SteelSupportAxialForcePoint newPoint : importedPoints) {
                            newPoint.setOrderIndex(points.size()); // 设置排序索引
                            points.add(newPoint);
                        }
                    } else {
                        // 只合并新测点 - 判断哪些是新加的测点
                        for (SteelSupportAxialForcePoint importedPoint : importedPoints) {
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
        SteelSupportAxialForcePoint selectedPoint = pointsTableView.getSelectionModel().getSelectedItem();
        
        if (selectedPoint != null) {
            ButtonType result = AlertUtil.showConfirmation(
                    "确认删除",
                    "确定要删除测点 " + selectedPoint.getPointId() + " 吗？");
            
            if (result == ButtonType.OK) {
                points.remove(selectedPoint);
                clearInputFields();
            }
        } else {
            AlertUtil.showWarning("未选择测点", "请先选择要删除的测点");
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
        fileChooser.setInitialFileName("钢支撑轴力测点档案.xlsx");
        
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
        dialogStage.close();
    }

    /**
     * 处理保存并关闭按钮事件
     */
    @FXML
    private void handleSaveClose(ActionEvent event) {
        dialogStage.close();
    }

    /**
     * 验证输入字段
     */
    private boolean validateInputs() {
        String pointId = pointIdField.getText().trim();
        String mileage = mileageField.getText().trim();
        String minValueText = minValueField.getText().trim();
        String controlValueText = controlValueField.getText().trim();
        String historicalCumulativeText = historicalCumulativeField.getText().trim();
        
        StringBuilder errorMessage = new StringBuilder();
        
        if (pointId.isEmpty()) {
            errorMessage.append("测点编号不能为空\n");
        }
        
        if (mileage.isEmpty()) {
            errorMessage.append("里程不能为空\n");
        }
        
        try {
            if (!minValueText.isEmpty()) {
                Double.parseDouble(minValueText);
            } else {
                errorMessage.append("最小值不能为空\n");
            }
        } catch (NumberFormatException e) {
            errorMessage.append("最小值必须是有效的数字\n");
        }
        
        try {
            if (!controlValueText.isEmpty()) {
                Double.parseDouble(controlValueText);
            } else {
                errorMessage.append("控制值不能为空\n");
            }
        } catch (NumberFormatException e) {
            errorMessage.append("控制值必须是有效的数字\n");
        }
        
        try {
            if (!historicalCumulativeText.isEmpty()) {
                Double.parseDouble(historicalCumulativeText);
            } else {
                // 允许历史累计值为空，默认为0
                historicalCumulativeField.setText("0.0");
            }
        } catch (NumberFormatException e) {
            errorMessage.append("历史累计值必须是有效的数字\n");
        }
        
        if (errorMessage.length() > 0) {
            AlertUtil.showError("输入验证", errorMessage.toString());
            return false;
        }
        
        return true;
    }

    /**
     * 清空输入字段
     */
    private void clearInputFields() {
        pointIdField.clear();
        mileageField.clear();
        minValueField.clear();
        controlValueField.clear();
        historicalCumulativeField.clear();
        pointsTableView.getSelectionModel().clearSelection();
    }

    /**
     * 从Excel导入测点数据
     */
    private List<SteelSupportAxialForcePoint> importPointsFromExcel(File file) throws IOException {
        List<SteelSupportAxialForcePoint> importedPoints = new ArrayList<>();
        
        try (Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            
            // 按首行判断列的位置
            int pointIdCol = -1, mileageCol = -1, minValueCol = -1, 
                controlValueCol = -1, historicalCumulativeCol = -1;
            
            if (headerRow != null) {
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    org.apache.poi.ss.usermodel.Cell cell = headerRow.getCell(i);
                    if (cell != null) {
                        String cellValue = cell.getStringCellValue().trim();
                        if (cellValue.contains("测点编号")) pointIdCol = i;
                        else if (cellValue.contains("里程")) mileageCol = i;
                        else if (cellValue.contains("最小值")) minValueCol = i;
                        else if (cellValue.contains("控制值")) controlValueCol = i;
                        else if (cellValue.contains("历史累计")) historicalCumulativeCol = i;
                    }
                }
            }
            
            // 检查必要的列是否存在
            if (pointIdCol == -1 || mileageCol == -1) {
                AlertUtil.showError("格式错误", "Excel文件必须包含'测点编号'和'里程'列");
                return importedPoints;
            }
            
            // 读取数据行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                // 读取测点编号
                org.apache.poi.ss.usermodel.Cell pointIdCell = row.getCell(pointIdCol);
                if (pointIdCell == null) continue;
                
                String pointId = getCellValueAsString(pointIdCell);
                if (pointId.isEmpty()) continue;
                
                // 读取里程
                String mileage = "";
                if (mileageCol != -1) {
                    org.apache.poi.ss.usermodel.Cell mileageCell = row.getCell(mileageCol);
                    if (mileageCell != null) {
                        mileage = getCellValueAsString(mileageCell);
                    }
                }
                
                // 读取最小值
                double minValue = 0.0;
                if (minValueCol != -1) {
                    org.apache.poi.ss.usermodel.Cell minValueCell = row.getCell(minValueCol);
                    if (minValueCell != null && minValueCell.getCellType() == CellType.NUMERIC) {
                        minValue = minValueCell.getNumericCellValue();
                    } else if (minValueCell != null) {
                        try {
                            minValue = Double.parseDouble(getCellValueAsString(minValueCell));
                        } catch (NumberFormatException e) {
                            // 使用默认值
                        }
                    }
                }
                
                // 读取控制值
                double controlValue = 0.0;
                if (controlValueCol != -1) {
                    org.apache.poi.ss.usermodel.Cell controlValueCell = row.getCell(controlValueCol);
                    if (controlValueCell != null && controlValueCell.getCellType() == CellType.NUMERIC) {
                        controlValue = controlValueCell.getNumericCellValue();
                    } else if (controlValueCell != null) {
                        try {
                            controlValue = Double.parseDouble(getCellValueAsString(controlValueCell));
                        } catch (NumberFormatException e) {
                            // 使用默认值
                        }
                    }
                }
                
                // 读取历史累计量
                double historicalCumulative = 0.0;
                if (historicalCumulativeCol != -1) {
                    org.apache.poi.ss.usermodel.Cell historicalCumulativeCell = row.getCell(historicalCumulativeCol);
                    if (historicalCumulativeCell != null && historicalCumulativeCell.getCellType() == CellType.NUMERIC) {
                        historicalCumulative = historicalCumulativeCell.getNumericCellValue();
                    } else if (historicalCumulativeCell != null) {
                        try {
                            historicalCumulative = Double.parseDouble(getCellValueAsString(historicalCumulativeCell));
                        } catch (NumberFormatException e) {
                            // 使用默认值
                        }
                    }
                }
                
                // 创建测点对象并添加到列表
                SteelSupportAxialForcePoint point = new SteelSupportAxialForcePoint(
                    pointId, mileage, minValue, controlValue, historicalCumulative
                );
                point.setOrderIndex(i - 1); // 保持Excel中的顺序
                importedPoints.add(point);
            }
        } catch (Exception e) {
            throw new IOException("解析Excel文件时出错: " + e.getMessage(), e);
        }
        
        return importedPoints;
    }

    /**
     * 导出测点数据到Excel
     */
    private void exportPointsToExcel(File file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("钢支撑轴力测点");
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("测点编号");
            headerRow.createCell(1).setCellValue("里程");
            headerRow.createCell(2).setCellValue("最小值(KN)");
            headerRow.createCell(3).setCellValue("控制值(KN)");
            headerRow.createCell(4).setCellValue("历史累计值(KN)");
            
            // 填充数据
            for (int i = 0; i < points.size(); i++) {
                SteelSupportAxialForcePoint point = points.get(i);
                Row row = sheet.createRow(i + 1);
                
                row.createCell(0).setCellValue(point.getPointId());
                row.createCell(1).setCellValue(point.getMileage());
                
                // 使用2位小数格式
                org.apache.poi.ss.usermodel.Cell minValueCell = row.createCell(2);
                minValueCell.setCellValue(point.getMinValue());
                minValueCell.setCellStyle(createDecimalStyle(workbook, 2));
                
                org.apache.poi.ss.usermodel.Cell controlValueCell = row.createCell(3);
                controlValueCell.setCellValue(point.getControlValue());
                controlValueCell.setCellStyle(createDecimalStyle(workbook, 2));
                
                org.apache.poi.ss.usermodel.Cell historicalCumulativeCell = row.createCell(4);
                historicalCumulativeCell.setCellValue(point.getHistoricalCumulative());
                historicalCumulativeCell.setCellStyle(createDecimalStyle(workbook, 2));
            }
            
            // 自动调整列宽
            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 写入文件
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }
        }
    }
    
    /**
     * 创建指定小数位数的单元格样式
     */
    private org.apache.poi.ss.usermodel.CellStyle createDecimalStyle(Workbook workbook, int decimalPlaces) {
        org.apache.poi.ss.usermodel.CellStyle style = workbook.createCellStyle();
        String format = "0." + "0".repeat(decimalPlaces);
        style.setDataFormat(workbook.createDataFormat().getFormat(format));
        return style;
    }

    /**
     * 获取单元格的字符串值
     */
    private String getCellValueAsString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) {
            return "";
        }
        
        CellType cellType = cell.getCellType();
        
        if (cellType == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cellType == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toString();
            } else {
                return String.valueOf(cell.getNumericCellValue());
            }
        } else if (cellType == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        } else if (cellType == CellType.FORMULA) {
            try {
                return String.valueOf(cell.getNumericCellValue());
            } catch (Exception e) {
                return cell.getStringCellValue();
            }
        } else {
            return "";
        }
    }
} 