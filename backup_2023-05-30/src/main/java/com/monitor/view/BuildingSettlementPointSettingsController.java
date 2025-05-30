package com.monitor.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.monitor.model.BuildingSettlementPoint;
import com.monitor.util.AlertUtil;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * 建筑物沉降测点设置对话框控制器
 */
public class BuildingSettlementPointSettingsController {

    @FXML private TableView<BuildingSettlementPoint> pointsTableView;
    @FXML private TableColumn<BuildingSettlementPoint, String> pointIdColumn;
    @FXML private TableColumn<BuildingSettlementPoint, Number> initialElevationColumn;
    @FXML private TableColumn<BuildingSettlementPoint, String> mileageColumn;
    @FXML private TableColumn<BuildingSettlementPoint, Number> rateWarningColumn;
    @FXML private TableColumn<BuildingSettlementPoint, Number> accumulatedWarningColumn;
    @FXML private TableColumn<BuildingSettlementPoint, Number> historicalCumulativeColumn;

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

    private ObservableList<BuildingSettlementPoint> points = FXCollections.observableArrayList();
    private Stage dialogStage;

    /**
     * 初始化
     */
    @FXML
    private void initialize() {
        // 设置表格列
        pointIdColumn.setCellValueFactory(new PropertyValueFactory<>("pointId"));
        initialElevationColumn.setCellValueFactory(new PropertyValueFactory<>("initialElevation"));
        mileageColumn.setCellValueFactory(new PropertyValueFactory<>("mileage"));
        rateWarningColumn.setCellValueFactory(new PropertyValueFactory<>("rateWarningValue"));
        accumulatedWarningColumn.setCellValueFactory(new PropertyValueFactory<>("accumulatedWarningValue"));
        historicalCumulativeColumn.setCellValueFactory(new PropertyValueFactory<>("historicalCumulative"));

        // 格式化数字列显示
        initialElevationColumn.setCellFactory(column -> new TableCell<BuildingSettlementPoint, Number>() {
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

        // 设置表格数据
        pointsTableView.setItems(points);
        
        // 添加表格选择监听器
        pointsTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                pointIdField.setText(newValue.getPointId());
                                initialElevationField.setText(String.format("%.4f", newValue.getInitialElevation()));                mileageField.setText(newValue.getMileage());                rateWarningField.setText(String.format("%.4f", newValue.getRateWarningValue()));                accumulatedWarningField.setText(String.format("%.4f", newValue.getAccumulatedWarningValue()));                historicalCumulativeField.setText(String.format("%.4f", newValue.getHistoricalCumulative()));
                
                addButton.setText("更新");
            } else {
                clearInputFields();
                addButton.setText("添加");
            }
        });
    }

    /**
     * 设置对话框舞台
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * 设置初始数据
     */
    public void setInitialData(List<BuildingSettlementPoint> initialPoints) {
        if (initialPoints != null) {
            points.clear();
            points.addAll(initialPoints);
            
            // 确保新添加的点有正确的序号
            int maxOrderIndex = -1;
            for (BuildingSettlementPoint point : points) {
                if (point.getOrderIndex() > maxOrderIndex) {
                    maxOrderIndex = point.getOrderIndex();
                }
            }
            
            int nextOrderIndex = maxOrderIndex + 1;
            for (BuildingSettlementPoint point : points) {
                if (point.getOrderIndex() < 0) {
                    point.setOrderIndex(nextOrderIndex++);
                }
            }
        }
    }

    /**
     * 获取所有测点
     */
    public List<BuildingSettlementPoint> getPoints() {
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

        try {
            // 解析输入值
            String pointId = pointIdField.getText().trim();
            double initialElevation = Double.parseDouble(initialElevationField.getText().trim());
            String mileage = mileageField.getText().trim();
            double rateWarning = Double.parseDouble(rateWarningField.getText().trim());
            double accumulatedWarning = Double.parseDouble(accumulatedWarningField.getText().trim());
            
            double historicalCumulative = 0.0;
            if (!historicalCumulativeField.getText().trim().isEmpty()) {
                historicalCumulative = Double.parseDouble(historicalCumulativeField.getText().trim());
            }

            BuildingSettlementPoint selectedPoint = pointsTableView.getSelectionModel().getSelectedItem();
            if (selectedPoint != null && addButton.getText().equals("更新")) {
                // 更新模式
                selectedPoint.setPointId(pointId);
                selectedPoint.setInitialElevation(initialElevation);
                selectedPoint.setMileage(mileage);
                selectedPoint.setRateWarningValue(rateWarning);
                selectedPoint.setAccumulatedWarningValue(accumulatedWarning);
                selectedPoint.setHistoricalCumulative(historicalCumulative);
                
                pointsTableView.refresh();
                pointsTableView.getSelectionModel().clearSelection();
            } else {
                // 添加模式
                // 检查是否已存在相同ID的测点
                boolean exists = false;
                for (BuildingSettlementPoint point : points) {
                    if (point.getPointId().equals(pointId)) {
                        exists = true;
                        break;
                    }
                }

                if (exists) {
                    AlertUtil.showWarning("添加测点失败", "已存在ID为 " + pointId + " 的测点");
                    return;
                }

                // 创建新测点
                BuildingSettlementPoint newPoint = new BuildingSettlementPoint(
                        pointId, initialElevation, mileage, rateWarning, accumulatedWarning, historicalCumulative);
                
                // 设置排序索引
                newPoint.setOrderIndex(points.size());
                
                // 添加到列表
                points.add(newPoint);
            }

            // 清空输入字段
            clearInputFields();

        } catch (NumberFormatException e) {
            AlertUtil.showError("输入错误", "请输入有效的数值");
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
                List<BuildingSettlementPoint> importedPoints = importPointsFromExcel(file);
                
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
                        for (BuildingSettlementPoint newPoint : importedPoints) {
                            newPoint.setOrderIndex(points.size()); // 设置排序索引
                            points.add(newPoint);
                        }
                    } else {
                        // 只合并新测点 - 判断哪些是新加的测点
                        for (BuildingSettlementPoint importedPoint : importedPoints) {
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
                AlertUtil.showError("导入失败", "无法从文件中导入数据: " + e.getMessage());
            }
        }
    }

    /**
     * 处理删除测点菜单项事件
     */
    @FXML
    private void handleDeletePoint(ActionEvent event) {
        BuildingSettlementPoint selectedPoint = pointsTableView.getSelectionModel().getSelectedItem();
        if (selectedPoint != null) {
            if (AlertUtil.showConfirmationDialog("删除测点", "确定要删除选中的测点吗？")) {
                points.remove(selectedPoint);
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
            AlertUtil.showWarning("导出失败", "没有测点可以导出");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出测点数据");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("Excel文件", "*.xlsx"));
        fileChooser.setInitialFileName("建筑物沉降测点数据.xlsx");

        File file = fileChooser.showSaveDialog(dialogStage);
        if (file != null) {
            try {
                exportPointsToExcel(file);
                AlertUtil.showInformation("导出成功", "测点数据已成功导出到文件:\n" + file.getPath());
            } catch (IOException e) {
                AlertUtil.showError("导出失败", "无法导出到文件: " + e.getMessage());
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
        // 关闭对话框，返回成功结果
        dialogStage.close();
    }

    /**
     * 验证输入字段
     */
    private boolean validateInputs() {
        if (pointIdField.getText().trim().isEmpty()) {
            AlertUtil.showWarning("验证失败", "测点编号不能为空");
            pointIdField.requestFocus();
            return false;
        }

        try {
            if (!initialElevationField.getText().trim().isEmpty()) {
                Double.parseDouble(initialElevationField.getText().trim());
            } else {
                AlertUtil.showWarning("验证失败", "初始高程不能为空");
                initialElevationField.requestFocus();
                return false;
            }

            if (!rateWarningField.getText().trim().isEmpty()) {
                Double.parseDouble(rateWarningField.getText().trim());
            } else {
                AlertUtil.showWarning("验证失败", "速率报警值不能为空");
                rateWarningField.requestFocus();
                return false;
            }

            if (!accumulatedWarningField.getText().trim().isEmpty()) {
                Double.parseDouble(accumulatedWarningField.getText().trim());
            } else {
                AlertUtil.showWarning("验证失败", "累计报警值不能为空");
                accumulatedWarningField.requestFocus();
                return false;
            }

            if (!historicalCumulativeField.getText().trim().isEmpty()) {
                Double.parseDouble(historicalCumulativeField.getText().trim());
            }
        } catch (NumberFormatException e) {
            AlertUtil.showWarning("验证失败", "请输入有效的数值");
            return false;
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
        pointIdField.requestFocus();
    }

    /**
     * 从Excel文件导入测点
     */
    private List<BuildingSettlementPoint> importPointsFromExcel(File file) throws IOException {
        List<BuildingSettlementPoint> importedPoints = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // 跳过标题行
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }

            // 处理数据行
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                
                // 如果第一列为空，跳过该行
                Cell pointIdCell = row.getCell(0);
                if (pointIdCell == null || pointIdCell.getCellType() == CellType.BLANK) {
                    continue;
                }

                String pointId = getCellValueAsString(pointIdCell);
                double initialElevation = 0;
                String mileage = "";
                double rateWarning = 0;
                double accumulatedWarning = 0;
                double historicalCumulative = 0;

                // 读取初始高程
                Cell initialElevationCell = row.getCell(1);
                if (initialElevationCell != null) {
                    initialElevation = Double.parseDouble(getCellValueAsString(initialElevationCell));
                }

                // 读取里程
                Cell mileageCell = row.getCell(2);
                if (mileageCell != null) {
                    mileage = getCellValueAsString(mileageCell);
                }

                // 读取速率报警值
                Cell rateWarningCell = row.getCell(3);
                if (rateWarningCell != null) {
                    rateWarning = Double.parseDouble(getCellValueAsString(rateWarningCell));
                }

                // 读取累计报警值
                Cell accumulatedWarningCell = row.getCell(4);
                if (accumulatedWarningCell != null) {
                    accumulatedWarning = Double.parseDouble(getCellValueAsString(accumulatedWarningCell));
                }

                // 读取历史累计值
                Cell historicalCumulativeCell = row.getCell(5);
                if (historicalCumulativeCell != null) {
                    historicalCumulative = Double.parseDouble(getCellValueAsString(historicalCumulativeCell));
                }

                // 创建测点对象并添加到列表
                BuildingSettlementPoint point = new BuildingSettlementPoint(
                        pointId, initialElevation, mileage, rateWarning, accumulatedWarning, historicalCumulative);
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
            Sheet sheet = workbook.createSheet("建筑物沉降测点数据");

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("测点编号");
            headerRow.createCell(1).setCellValue("初始高程(m)");
            headerRow.createCell(2).setCellValue("里程");
            headerRow.createCell(3).setCellValue("速率报警值(mm)");
            headerRow.createCell(4).setCellValue("累计报警值(mm)");
            headerRow.createCell(5).setCellValue("历史累计量(mm)");

            // 创建数据行
            int rowNum = 1;
            for (BuildingSettlementPoint point : points) {
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

            // 保存文件
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }
        }
    }

    /**
     * 获取单元格值作为字符串
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            default:
                return "";
        }
    }
} 