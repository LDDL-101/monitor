package com.monitor.view;

import com.monitor.model.ConcreteSupportAxialForcePoint;
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
 * 砼支撑轴力测点设置控制器
 */
public class ConcreteSupportAxialForcePointSettingsController {

    @FXML private TableView<ConcreteSupportAxialForcePoint> pointsTableView;
    @FXML private TableColumn<ConcreteSupportAxialForcePoint, String> pointIdColumn;
    @FXML private TableColumn<ConcreteSupportAxialForcePoint, String> mileageColumn;
    @FXML private TableColumn<ConcreteSupportAxialForcePoint, Number> alarmValueColumn;
    @FXML private TableColumn<ConcreteSupportAxialForcePoint, Number> historicalCumulativeColumn;

    @FXML private TextField pointIdField;
    @FXML private TextField mileageField;
    @FXML private TextField alarmValueField;
    @FXML private TextField historicalCumulativeField;

    @FXML private Button addButton;
    @FXML private Button batchImportButton;
    @FXML private Button exportButton;
    @FXML private Button saveCloseButton;

    @FXML private MenuItem deleteMenuItem;

    private ObservableList<ConcreteSupportAxialForcePoint> points = FXCollections.observableArrayList();
    private Stage dialogStage;

    /**
     * 初始化方法，设置表格列绑定等
     */
    @FXML
    private void initialize() {
        // 设置表格列的单元格值工厂
        pointIdColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getPointId()));
        mileageColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getMileage()));
        alarmValueColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getAlarmValue()));
        historicalCumulativeColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getHistoricalCumulative()));

        // 格式化数字列显示
        alarmValueColumn.setCellFactory(column -> new TableCell<ConcreteSupportAxialForcePoint, Number>() {
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

        historicalCumulativeColumn.setCellFactory(column -> new TableCell<ConcreteSupportAxialForcePoint, Number>() {
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
                        alarmValueField.setText(String.format("%.2f", newValue.getAlarmValue()));
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
        dialogStage.setTitle("砼支撑轴力测点设置");
    }

    /**
     * 设置初始数据
     */
    public void setInitialData(List<ConcreteSupportAxialForcePoint> initialPoints) {
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
    public List<ConcreteSupportAxialForcePoint> getPoints() {
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
        
        double alarmValue = 0;
        double historicalCumulative = 0;
        
        try {
            alarmValue = Double.parseDouble(alarmValueField.getText().trim());
        } catch (NumberFormatException e) {
            AlertUtil.showError("输入错误", "报警值必须是有效的数字");
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

        if (isEdit) {
            // 编辑已有测点
            ConcreteSupportAxialForcePoint point = points.get(editIndex);
            point.setMileage(mileage);
            point.setAlarmValue(alarmValue);
            point.setHistoricalCumulative(historicalCumulative);
            
            // 刷新表格
            pointsTableView.refresh();
            
            // 清空输入字段
            clearInputFields();
            
            // 显示成功消息
            AlertUtil.showInformation("编辑成功", "已更新测点 " + pointId);
        } else {
            // 添加新测点
            ConcreteSupportAxialForcePoint newPoint = new ConcreteSupportAxialForcePoint(
                    pointId, mileage, alarmValue, historicalCumulative);
            
            // 设置排序索引
            newPoint.setOrderIndex(points.size());
            
            // 添加到列表
            points.add(newPoint);
            
            // 清空输入字段
            clearInputFields();
            
            // 显示成功消息
            AlertUtil.showInformation("添加成功", "已添加测点 " + pointId);
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
                new FileChooser.ExtensionFilter("Excel 文件", "*.xlsx", "*.xls"));
        
        File file = fileChooser.showOpenDialog(dialogStage);
        if (file != null) {
            try {
                List<ConcreteSupportAxialForcePoint> importedPoints = importPointsFromExcel(file);
                
                if (importedPoints.isEmpty()) {
                    AlertUtil.showWarning("导入失败", "未找到有效的测点数据");
                    return;
                }
                
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
                    for (ConcreteSupportAxialForcePoint newPoint : importedPoints) {
                        newPoint.setOrderIndex(points.size()); // 设置排序索引
                        points.add(newPoint);
                    }
                } else {
                    // 只合并新测点 - 判断哪些是新加的测点
                    for (ConcreteSupportAxialForcePoint importedPoint : importedPoints) {
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
                
                // 刷新表格
                pointsTableView.refresh();
                
                // 显示成功消息
                AlertUtil.showInformation("导入成功", "已导入 " + importedPoints.size() + " 个测点");
                
            } catch (IOException e) {
                AlertUtil.showError("导入错误", "无法读取Excel文件: " + e.getMessage());
            }
        }
    }

    /**
     * 处理删除测点按钮事件
     */
    @FXML
    private void handleDeletePoint(ActionEvent event) {
        ConcreteSupportAxialForcePoint selectedPoint = pointsTableView.getSelectionModel().getSelectedItem();
        if (selectedPoint != null) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("确认删除");
            confirmAlert.setHeaderText("删除测点");
            confirmAlert.setContentText("确定要删除测点 " + selectedPoint.getPointId() + " 吗？");
            
            if (confirmAlert.showAndWait().get() == ButtonType.OK) {
                points.remove(selectedPoint);
                
                // 更新剩余测点的排序索引
                for (int i = 0; i < points.size(); i++) {
                    points.get(i).setOrderIndex(i);
                }
                
                // 清空输入字段
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
        if (points.isEmpty()) {
            AlertUtil.showWarning("导出失败", "没有测点数据可供导出");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出测点配置");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel 文件", "*.xlsx"));
        fileChooser.setInitialFileName("砼支撑轴力测点.xlsx");
        
        File file = fileChooser.showSaveDialog(dialogStage);
        if (file != null) {
            try {
                exportPointsToExcel(file);
                AlertUtil.showInformation("导出成功", "测点数据已导出到: " + file.getAbsolutePath());
            } catch (IOException e) {
                AlertUtil.showError("导出错误", "无法导出Excel文件: " + e.getMessage());
            }
        }
    }

    /**
     * 关闭按钮事件
     */
    @FXML
    private void handleClose(ActionEvent event) {
        dialogStage.close();
    }

    /**
     * 保存并关闭按钮事件
     */
    @FXML
    private void handleSaveClose(ActionEvent event) {
        dialogStage.close();
    }

    /**
     * 验证输入字段
     */
    private boolean validateInputs() {
        // 验证测点编号
        String pointId = pointIdField.getText().trim();
        if (pointId.isEmpty()) {
            AlertUtil.showWarning("输入错误", "测点编号不能为空");
            pointIdField.requestFocus();
            return false;
        }
        
        // 验证里程
        String mileage = mileageField.getText().trim();
        if (mileage.isEmpty()) {
            AlertUtil.showWarning("输入错误", "里程不能为空");
            mileageField.requestFocus();
            return false;
        }
        
        // 验证报警值
        String alarmValueText = alarmValueField.getText().trim();
        if (alarmValueText.isEmpty()) {
            AlertUtil.showWarning("输入错误", "报警值不能为空");
            alarmValueField.requestFocus();
            return false;
        }
        
        try {
            double alarmValue = Double.parseDouble(alarmValueText);
            if (alarmValue < 0) {
                AlertUtil.showWarning("输入错误", "报警值必须是非负数");
                alarmValueField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            AlertUtil.showWarning("输入错误", "报警值必须是有效的数字");
            alarmValueField.requestFocus();
            return false;
        }
        
        // 验证历史累计值
        String historicalText = historicalCumulativeField.getText().trim();
        if (historicalText.isEmpty()) {
            historicalCumulativeField.setText("0.0");
        } else {
            try {
                Double.parseDouble(historicalText);
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("输入错误", "历史累计值必须是有效的数字");
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
        mileageField.clear();
        alarmValueField.clear();
        historicalCumulativeField.clear();
        pointIdField.requestFocus();
    }

    /**
     * 从Excel文件导入测点数据
     */
    private List<ConcreteSupportAxialForcePoint> importPointsFromExcel(File file) throws IOException {
        List<ConcreteSupportAxialForcePoint> importedPoints = new ArrayList<>();
        
        try (Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // 标志位：是否找到表头行
            boolean foundHeader = false;
            
            // 查找表头行
            for (Row row : sheet) {
                if (row.getCell(0) == null) continue;
                
                String headerText = getCellValueAsString(row.getCell(0)).trim();
                
                if (headerText.contains("测点") || headerText.contains("编号")) {
                    foundHeader = true;
                    break;
                }
            }
            
            if (!foundHeader) {
                throw new IOException("未找到有效的表头行");
            }
            
            // 读取数据行
            boolean isFirst = true;
            int pointIdCol = -1;
            int mileageCol = -1;
            int alarmValueCol = -1;
            int historicalCumulativeCol = -1;
            
            for (Row row : sheet) {
                // 跳过空行
                if (row.getCell(0) == null) continue;
                
                if (isFirst) {
                    // 处理表头行，查找列索引
                    for (int i = 0; i < row.getLastCellNum(); i++) {
                        String cellText = getCellValueAsString(row.getCell(i)).trim();
                        
                        if (cellText.contains("测点") || cellText.contains("编号")) {
                            pointIdCol = i;
                        } else if (cellText.contains("里程")) {
                            mileageCol = i;
                        } else if (cellText.contains("报警")) {
                            alarmValueCol = i;
                        } else if (cellText.contains("历史") || cellText.contains("累计")) {
                            historicalCumulativeCol = i;
                        }
                    }
                    
                    // 检查必要的列是否找到
                    if (pointIdCol == -1 || mileageCol == -1 || alarmValueCol == -1) {
                        throw new IOException("表格缺少必要的列（测点编号、里程、报警值）");
                    }
                    
                    isFirst = false;
                    continue;
                }
                
                // 处理数据行
                String pointId = getCellValueAsString(row.getCell(pointIdCol)).trim();
                if (pointId.isEmpty()) continue;
                
                String mileage = getCellValueAsString(row.getCell(mileageCol)).trim();
                
                // 读取报警值
                double alarmValue = 0;
                try {
                    org.apache.poi.ss.usermodel.Cell alarmCell = row.getCell(alarmValueCol);
                    if (alarmCell != null) {
                        if (alarmCell.getCellType() == CellType.NUMERIC) {
                            alarmValue = alarmCell.getNumericCellValue();
                        } else {
                            alarmValue = Double.parseDouble(getCellValueAsString(alarmCell));
                        }
                    }
                } catch (NumberFormatException e) {
                    // 忽略无法解析的值
                }
                
                // 读取历史累计值（如果有）
                double historicalCumulative = 0;
                if (historicalCumulativeCol != -1) {
                    try {
                        org.apache.poi.ss.usermodel.Cell historicalCell = row.getCell(historicalCumulativeCol);
                        if (historicalCell != null) {
                            if (historicalCell.getCellType() == CellType.NUMERIC) {
                                historicalCumulative = historicalCell.getNumericCellValue();
                            } else {
                                historicalCumulative = Double.parseDouble(getCellValueAsString(historicalCell));
                            }
                        }
                    } catch (NumberFormatException e) {
                        // 忽略无法解析的值
                    }
                }
                
                // 创建测点对象并添加到列表
                ConcreteSupportAxialForcePoint point = new ConcreteSupportAxialForcePoint(
                        pointId, mileage, alarmValue, historicalCumulative);
                importedPoints.add(point);
            }
        }
        
        return importedPoints;
    }

    /**
     * 导出测点数据到Excel文件
     */
    private void exportPointsToExcel(File file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("砼支撑轴力测点");
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("测点编号");
            headerRow.createCell(1).setCellValue("里程");
            headerRow.createCell(2).setCellValue("报警值(KN)");
            headerRow.createCell(3).setCellValue("历史累计值(KN)");
            
            // 创建数字列的小数格式
            org.apache.poi.ss.usermodel.CellStyle decimalStyle = createDecimalStyle(workbook, 2);
            
            // 添加数据行
            for (int i = 0; i < points.size(); i++) {
                ConcreteSupportAxialForcePoint point = points.get(i);
                Row row = sheet.createRow(i + 1);
                
                row.createCell(0).setCellValue(point.getPointId());
                row.createCell(1).setCellValue(point.getMileage());
                
                org.apache.poi.ss.usermodel.Cell alarmCell = row.createCell(2);
                alarmCell.setCellValue(point.getAlarmValue());
                alarmCell.setCellStyle(decimalStyle);
                
                org.apache.poi.ss.usermodel.Cell historicalCell = row.createCell(3);
                historicalCell.setCellValue(point.getHistoricalCumulative());
                historicalCell.setCellStyle(decimalStyle);
            }
            
            // 自动调整列宽
            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 保存文件
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }
        }
    }

    /**
     * 创建带有指定小数位数的单元格样式
     */
    private org.apache.poi.ss.usermodel.CellStyle createDecimalStyle(Workbook workbook, int decimalPlaces) {
        org.apache.poi.ss.usermodel.CellStyle style = workbook.createCellStyle();
        String formatPattern = "0.";
        for (int i = 0; i < decimalPlaces; i++) {
            formatPattern += "0";
        }
        style.setDataFormat(workbook.createDataFormat().getFormat(formatPattern));
        return style;
    }

    /**
     * 获取单元格的字符串值
     */
    private String getCellValueAsString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                } else {
                    // 检查是否为整数
                    double value = cell.getNumericCellValue();
                    if (value == (long) value) {
                        return String.format("%d", (long) value);
                    } else {
                        return String.format("%f", value);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception e2) {
                        return "";
                    }
                }
            default:
                return "";
        }
    }
} 