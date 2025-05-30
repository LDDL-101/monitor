package com.monitor.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.monitor.model.SettlementPoint;
import com.monitor.util.AlertUtil;
import com.monitor.util.ExcelUtil;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * 沉降点设置对话框控制器
 */
public class SettlementPointSettingsController {

    @FXML private TableView<SettlementPoint> pointsTableView;
    @FXML private TableColumn<SettlementPoint, String> pointIdColumn;
    @FXML private TableColumn<SettlementPoint, Number> initialElevationColumn;
    @FXML private TableColumn<SettlementPoint, String> mileageColumn;
    @FXML private TableColumn<SettlementPoint, Number> rateWarningColumn;
    @FXML private TableColumn<SettlementPoint, Number> accumulatedWarningColumn;
    @FXML private TableColumn<SettlementPoint, Number> historicalCumulativeColumn;

    @FXML private TextField pointIdField;
    @FXML private TextField initialElevationField;
    @FXML private TextField mileageField;
    @FXML private TextField rateWarningField;
    @FXML private TextField accumulatedWarningField;
    @FXML private TextField historicalCumulativeField;

    @FXML private Button addButton;
    @FXML private Button batchImportButton;
    @FXML private Button exportButton;
    @FXML private Button closeButton;

    @FXML private MenuItem deleteMenuItem;

    private ObservableList<SettlementPoint> points = FXCollections.observableArrayList();
    private Stage dialogStage;

    /**
     * 初始化方法，由JavaFX自动调用
     */
    @FXML
    private void initialize() {
        // 设置表格列的工厂
        pointIdColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getPointId()));

        initialElevationColumn.setCellValueFactory(cellData ->
            new SimpleDoubleProperty(cellData.getValue().getInitialElevation()));

        mileageColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getMileage()));

        rateWarningColumn.setCellValueFactory(cellData ->
            new SimpleDoubleProperty(cellData.getValue().getRateWarningValue()));

        accumulatedWarningColumn.setCellValueFactory(cellData ->
            new SimpleDoubleProperty(cellData.getValue().getAccumulatedWarningValue()));

        historicalCumulativeColumn.setCellValueFactory(cellData ->
            new SimpleDoubleProperty(cellData.getValue().getHistoricalCumulative()));

        // 设置表格数据源
        pointsTableView.setItems(points);

        // 禁用表格列排序，确保按照导入顺序显示
        pointIdColumn.setSortable(false);
        initialElevationColumn.setSortable(false);
        mileageColumn.setSortable(false);
        rateWarningColumn.setSortable(false);
        accumulatedWarningColumn.setSortable(false);
        historicalCumulativeColumn.setSortable(false);
    }

    /**
     * 设置对话框的Stage
     * @param dialogStage 对话框的Stage
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * 设置初始数据
     * @param initialPoints 初始测点数据
     */
    public void setInitialData(List<SettlementPoint> initialPoints) {
        if (initialPoints != null) {
            points.clear();

            // 设置排序索引（如果没有的话）
            int index = 0;
            for (SettlementPoint point : initialPoints) {
                // 如果没有设置排序索引，则设置一个
                if (point.getOrderIndex() == 0) {
                    point.setOrderIndex(index);
                }
                index++;
            }

            // 按照orderIndex排序添加测点
            List<SettlementPoint> sortedPoints = new ArrayList<>(initialPoints);
            sortedPoints.sort((p1, p2) -> Integer.compare(p1.getOrderIndex(), p2.getOrderIndex()));

            points.addAll(sortedPoints);
        }
    }

    /**
     * 获取当前测点数据
     * @return 测点数据列表
     */
    public List<SettlementPoint> getPoints() {
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
            String pointId = pointIdField.getText().trim();
            double initialElevation = Double.parseDouble(initialElevationField.getText().trim());
            String mileage = mileageField.getText().trim();
            double rateWarning = Double.parseDouble(rateWarningField.getText().trim());
            double accumulatedWarning = Double.parseDouble(accumulatedWarningField.getText().trim());
            double historicalCumulative = 0.0;

            if (historicalCumulativeField.getText() != null && !historicalCumulativeField.getText().trim().isEmpty()) {
                historicalCumulative = Double.parseDouble(historicalCumulativeField.getText().trim());
            }

            // 检查是否已存在相同ID的测点
            Optional<SettlementPoint> existingPoint = points.stream()
                .filter(p -> p.getPointId().equals(pointId))
                .findFirst();

            if (existingPoint.isPresent()) {
                // 如果存在，则更新现有测点
                SettlementPoint point = existingPoint.get();
                point.setInitialElevation(initialElevation);
                point.setMileage(mileage);
                point.setRateWarningValue(rateWarning);
                point.setAccumulatedWarningValue(accumulatedWarning);
                point.setHistoricalCumulative(historicalCumulative);
                pointsTableView.refresh();
            } else {
                // 如果不存在，则添加新测点
                SettlementPoint newPoint = new SettlementPoint(
                    pointId, initialElevation, mileage, rateWarning, accumulatedWarning, historicalCumulative);
                // 设置排序索引，保持添加顺序
                newPoint.setOrderIndex(points.size());
                points.add(newPoint);
            }

            // 清空输入字段
            clearInputFields();

        } catch (NumberFormatException e) {
            AlertUtil.showError("输入错误", "请确保所有数值字段包含有效的数字。");
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
            new FileChooser.ExtensionFilter("Excel文件", "*.xlsx")
        );

        File file = fileChooser.showOpenDialog(dialogStage);
        if (file != null) {
            try {
                List<SettlementPoint> importedPoints = importPointsFromExcel(file);
                
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
                        for (SettlementPoint newPoint : importedPoints) {
                            newPoint.setOrderIndex(points.size()); // 设置排序索引
                            points.add(newPoint);
                        }
                    } else {
                        // 只合并新测点 - 判断哪些是新加的测点
                        for (SettlementPoint newPoint : importedPoints) {
                            // 检查是否已存在相同ID的测点
                            Optional<SettlementPoint> existingPoint = points.stream()
                                .filter(p -> p.getPointId().equals(newPoint.getPointId()))
                                .findFirst();

                            if (existingPoint.isPresent()) {
                                // 如果存在，则更新现有测点
                                SettlementPoint point = existingPoint.get();
                                point.setInitialElevation(newPoint.getInitialElevation());
                                point.setMileage(newPoint.getMileage());
                                point.setRateWarningValue(newPoint.getRateWarningValue());
                                point.setAccumulatedWarningValue(newPoint.getAccumulatedWarningValue());
                                // 保留原来的排序索引
                            } else {
                                // 如果不存在，则添加新测点
                                // 保持导入的排序索引
                                newPoint.setOrderIndex(points.size()); // 设置排序索引
                                points.add(newPoint);
                            }
                        }
                    }

                    pointsTableView.refresh();
                    AlertUtil.showInformation("导入成功", "成功导入 " + importedPoints.size() + " 个测点。");
                }
            } catch (Exception e) {
                AlertUtil.showError("导入失败", "导入Excel文件时发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理删除测点菜单项事件
     */
    @FXML
    private void handleDeletePoint(ActionEvent event) {
        SettlementPoint selectedPoint = pointsTableView.getSelectionModel().getSelectedItem();
        if (selectedPoint != null) {
            points.remove(selectedPoint);
        } else {
            AlertUtil.showWarning("未选择测点", "请先选择要删除的测点。");
        }
    }

    /**
     * 处理导出档案按钮事件
     */
    @FXML
    private void handleExport(ActionEvent event) {
        if (points.isEmpty()) {
            AlertUtil.showWarning("导出失败", "没有测点数据可导出。");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出测点档案");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel文件", "*.xlsx")
        );
        fileChooser.setInitialFileName("测点设置.xlsx");

        File file = fileChooser.showSaveDialog(dialogStage);
        if (file != null) {
            try {
                exportPointsToExcel(file);
                AlertUtil.showInformation("导出成功", "成功导出测点档案到: " + file.getAbsolutePath());
            } catch (Exception e) {
                AlertUtil.showError("导出失败", "导出Excel文件时发生错误: " + e.getMessage());
                e.printStackTrace();
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
     * 验证输入字段
     * @return 是否验证通过
     */
    private boolean validateInputs() {
        StringBuilder errorMessage = new StringBuilder();

        if (pointIdField.getText().trim().isEmpty()) {
            errorMessage.append("测点编号不能为空！\n");
        }

        try {
            if (!initialElevationField.getText().trim().isEmpty()) {
                Double.parseDouble(initialElevationField.getText().trim());
            } else {
                errorMessage.append("初始高程不能为空！\n");
            }
        } catch (NumberFormatException e) {
            errorMessage.append("初始高程必须是有效的数字！\n");
        }

        if (mileageField.getText().trim().isEmpty()) {
            errorMessage.append("里程不能为空！\n");
        }

        try {
            if (!rateWarningField.getText().trim().isEmpty()) {
                Double.parseDouble(rateWarningField.getText().trim());
            } else {
                errorMessage.append("速率报警值不能为空！\n");
            }
        } catch (NumberFormatException e) {
            errorMessage.append("速率报警值必须是有效的数字！\n");
        }

        try {
            if (!accumulatedWarningField.getText().trim().isEmpty()) {
                Double.parseDouble(accumulatedWarningField.getText().trim());
            } else {
                errorMessage.append("累计报警值不能为空！\n");
            }
        } catch (NumberFormatException e) {
            errorMessage.append("累计报警值必须是有效的数字！\n");
        }

        if (errorMessage.length() > 0) {
            AlertUtil.showError("输入错误", errorMessage.toString());
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
     * 从Excel文件导入测点数据
     * @param file Excel文件
     * @return 导入的测点列表
     */
    private List<SettlementPoint> importPointsFromExcel(File file) throws IOException {
        List<SettlementPoint> importedPoints = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            // 跳过标题行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // 读取单元格数据
                String pointId = getCellValueAsString(row.getCell(0));
                if (pointId == null || pointId.trim().isEmpty()) continue;

                double initialElevation = getCellValueAsDouble(row.getCell(1));
                String mileage = getCellValueAsString(row.getCell(2));
                double rateWarning = getCellValueAsDouble(row.getCell(3));
                double accumulatedWarning = getCellValueAsDouble(row.getCell(4));

                SettlementPoint point = new SettlementPoint(
                    pointId, initialElevation, mileage, rateWarning, accumulatedWarning);
                importedPoints.add(point);
            }
        }

        return importedPoints;
    }

    /**
     * 导出测点数据到Excel文件
     * @param file 导出的文件
     */
    private void exportPointsToExcel(File file) throws IOException {
        List<String> headers = List.of(
            "测点编号", "初始高程(m)", "里程", "速率报警值(mm)", "累计报警值(mm)");

        List<List<String>> data = new ArrayList<>();

        for (SettlementPoint point : points) {
            List<String> row = List.of(
                point.getPointId(),
                String.valueOf(point.getInitialElevation()),
                point.getMileage(),
                String.valueOf(point.getRateWarningValue()),
                String.valueOf(point.getAccumulatedWarningValue())
            );
            data.add(row);
        }

        ExcelUtil.exportToExcel(file, "测点设置", headers, data);
    }

    /**
     * 获取单元格的字符串值
     * @param cell 单元格
     * @return 字符串值
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            default:
                return "";
        }
    }

    /**
     * 获取单元格的数值
     * @param cell 单元格
     * @return 数值
     */
    private double getCellValueAsDouble(Cell cell) {
        if (cell == null) return 0.0;

        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            default:
                return 0.0;
        }
    }
}