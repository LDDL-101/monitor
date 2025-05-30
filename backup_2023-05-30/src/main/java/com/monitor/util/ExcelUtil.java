package com.monitor.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.monitor.model.SettlementPoint;

/**
 * Excel文件处理工具类
 */
public class ExcelUtil {

    /**
     * 导出数据到Excel文件
     *
     * @param file 导出的文件
     * @param sheetName 工作表名称
     * @param headers 表头
     * @param data 数据行
     * @throws IOException 如果发生I/O错误
     */
    public static void exportToExcel(File file, String sheetName, List<String> headers, List<List<String>> data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            // 创建表头
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
            }

            // 填充数据
            for (int i = 0; i < data.size(); i++) {
                Row row = sheet.createRow(i + 1);
                List<String> rowData = data.get(i);

                for (int j = 0; j < rowData.size(); j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(rowData.get(j));
                }
            }

            // 自动调整列宽
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            // 写入文件
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                workbook.write(outputStream);
            }
        }
    }

    /**
     * 从Excel文件导入地表沉降数据
     *
     * @param file 要导入的Excel文件
     * @return 导入的数据，格式为[{"测点编号", "本次高程"}]的列表
     * @throws IOException 如果发生I/O错误
     */
    public static List<List<String>> importFromExcel(File file) throws IOException {
        List<List<String>> result = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            // 查找名为"地表点沉降"的工作表
            Sheet sheet = null;
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                if ("地表点沉降".equals(workbook.getSheetName(i))) {
                    sheet = workbook.getSheetAt(i);
                    break;
                }
            }

            if (sheet == null) {
                throw new IOException("未找到名为'地表点沉降'的工作表");
            }

            // 检查表头行是否存在
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IOException("工作表中没有表头行");
            }

            // 确认表头行格式
            String pointHeader = getCellValueAsString(headerRow.getCell(0));
            String elevationHeader = getCellValueAsString(headerRow.getCell(1));

            if (!pointHeader.contains("测点") || !elevationHeader.contains("高程")) {
                throw new IOException("表头格式不正确。需要包含'测点'和'高程'列");
            }

            // 读取数据行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell codeCell = row.getCell(0);
                Cell elevationCell = row.getCell(1);

                // 跳过没有必要数据的行
                if (codeCell == null || elevationCell == null) {
                    continue;
                }

                String pointCode = getCellValueAsString(codeCell).trim();
                String elevation = getCellValueAsString(elevationCell).trim();

                // 如果点号或高程为空，则跳过
                if (pointCode.isEmpty() || elevation.isEmpty()) {
                    continue;
                }

                List<String> rowData = new ArrayList<>();
                rowData.add(pointCode);
                rowData.add(elevation);
                result.add(rowData);
            }
        } catch (Exception e) {
            throw new IOException("处理Excel文件时出错: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * 将单元格的值转换为字符串
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }

        switch (cellType) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                double value = cell.getNumericCellValue();
                // 检查是否为整数
                if (value == Math.floor(value)) {
                    return String.format("%.0f", value);
                }
                // 避免显示科学计数法，同时保留足够的精度
                return String.format("%.6f", value);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    /**
     * 验证Excel文件格式是否符合地表沉降数据要求
     *
     * @param file 要验证的文件
     * @return 是否符合预期格式
     */
    public static boolean validateExcelFormat(File file) {
        if (file == null || !file.exists() || !file.canRead()) {
            return false;
        }

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            // 检查是否存在名为"地表点沉降"的工作表
            boolean foundSheet = false;
            Sheet sheet = null;

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                if ("地表点沉降".equals(workbook.getSheetName(i))) {
                    foundSheet = true;
                    sheet = workbook.getSheetAt(i);
                    break;
                }
            }

            if (!foundSheet || sheet == null) {
                return false;
            }

            // 检查表头是否包含必要的列
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return false;
            }

            Cell firstCell = headerRow.getCell(0);
            Cell secondCell = headerRow.getCell(1);

            if (firstCell == null || secondCell == null) {
                return false;
            }

            String firstHeader = getCellValueAsString(firstCell);
            String secondHeader = getCellValueAsString(secondCell);

            return firstHeader.contains("测点") && secondHeader.contains("高程");

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 导入Excel数据
     * @param file Excel文件
     * @param sheetName 需要读取的工作表名称
     * @return 包含点位编号和高程数据的Map
     * @throws IOException 当文件读取出错时
     */
    public static Map<String, Double> importFromExcel(File file, String sheetName) throws IOException {
        Map<String, Double> pointElevationMap = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            // 查找指定的sheet
            Sheet sheet = null;
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                if (workbook.getSheetName(i).equals(sheetName)) {
                    sheet = workbook.getSheetAt(i);
                    break;
                }
            }

            if (sheet == null) {
                throw new IOException("未找到名为'" + sheetName + "'的工作表");
            }

            // 读取数据（从第2行开始，第1行为标题）
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    // 第一列为测点编号
                    Cell pointCell = row.getCell(0);
                    // 第二列为本次高程
                    Cell elevationCell = row.getCell(1);

                    if (pointCell != null && elevationCell != null) {
                        String pointCode = getStringCellValue(pointCell);
                        double elevation = getNumericCellValue(elevationCell);

                        if (!pointCode.isEmpty()) {
                            pointElevationMap.put(pointCode, elevation);
                        }
                    }
                }
            }
        }

        return pointElevationMap;
    }

    /**
     * 验证Excel文件格式是否符合要求
     * @param file Excel文件
     * @param sheetName 需要校验的工作表名称
     * @return 验证结果，成功返回true，否则返回false
     */
    public static boolean validateExcelFormat(File file, String sheetName) {
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            // 检查是否存在指定的sheet
            boolean sheetExists = false;
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                if (workbook.getSheetName(i).equals(sheetName)) {
                    sheetExists = true;
                    Sheet sheet = workbook.getSheetAt(i);

                    // 检查第一行（标题行）
                    Row headerRow = sheet.getRow(0);
                    if (headerRow == null) {
                        return false;
                    }

                    // 检查是否有至少两列
                    if (headerRow.getLastCellNum() < 2) {
                        return false;
                    }

                    // 检查表头
                    Cell pointHeaderCell = headerRow.getCell(0);
                    Cell elevationHeaderCell = headerRow.getCell(1);

                    if (pointHeaderCell == null || elevationHeaderCell == null) {
                        return false;
                    }

                    String pointHeader = getStringCellValue(pointHeaderCell);
                    String elevationHeader = getStringCellValue(elevationHeaderCell);

                    // 验证表头是否为"测点编号"和"本次高程"
                    if (!pointHeader.contains("测点") || !elevationHeader.contains("高程")) {
                        return false;
                    }

                    break;
                }
            }

            return sheetExists;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取单元格的字符串值
     */
    private static String getStringCellValue(Cell cell) {
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((int)cell.getNumericCellValue());
        } else {
            return "";
        }
    }

    /**
     * 获取单元格的数值
     */
    private static double getNumericCellValue(Cell cell) {
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Double.parseDouble(cell.getStringCellValue().trim());
            } catch (NumberFormatException e) {
                return 0.0;
            }
        } else {
            return 0.0;
        }
    }

    /**
     * 从 Excel 文件导入沉降测点数据
     *
     * @param file Excel文件
     * @param existingPoints 现有的测点列表
     * @param createMissingPoints 是否创建不存在的测点
     * @return 导入的测点列表
     * @throws IOException 如果发生I/O错误
     */
    public static List<SettlementPoint> importSettlementPointsFromExcel(
            File file,
            List<SettlementPoint> existingPoints,
            boolean createMissingPoints) throws IOException {

        List<SettlementPoint> importedPoints = new ArrayList<>();
        Map<String, SettlementPoint> existingPointsMap = new HashMap<>();
        int orderIndex = 0; // 用于记录导入顺序

        // 创建现有测点的映射
        if (existingPoints != null) {
            for (SettlementPoint point : existingPoints) {
                existingPointsMap.put(point.getPointId(), point);
            }
        }

        try (Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = workbook.getSheetAt(0); // 使用第一个工作表

            // 查找表头行
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IOException("无法读取Excel文件的表头");
            }

            // 查找测点编号和高程列
            int pointIdColumn = -1;
            int elevationColumn = -1;
            int mileageColumn = -1;
            int rateWarningColumn = -1;
            int accumulatedWarningColumn = -1;

            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String headerValue = getStringCellValue(cell).toLowerCase();
                    if (headerValue.contains("测点") || headerValue.contains("编号")) {
                        pointIdColumn = i;
                    } else if (headerValue.contains("高程")) {
                        elevationColumn = i;
                    } else if (headerValue.contains("里程")) {
                        mileageColumn = i;
                    } else if (headerValue.contains("速率") && headerValue.contains("警戒")) {
                        rateWarningColumn = i;
                    } else if (headerValue.contains("累计") && headerValue.contains("警戒")) {
                        accumulatedWarningColumn = i;
                    }
                }
            }

            if (pointIdColumn == -1 || elevationColumn == -1) {
                throw new IOException("无法在Excel文件中找到测点编号或高程列");
            }

            // 处理数据行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell pointIdCell = row.getCell(pointIdColumn);
                Cell elevationCell = row.getCell(elevationColumn);

                if (pointIdCell == null) continue;

                String pointId = getStringCellValue(pointIdCell);
                if (pointId.isEmpty()) continue;

                double elevation = 0.0;
                if (elevationCell != null) {
                    elevation = getNumericCellValue(elevationCell);
                }

                // 检查测点是否已存在
                SettlementPoint point = existingPointsMap.get(pointId);

                if (point == null && createMissingPoints) {
                    // 创建新测点
                    point = new SettlementPoint();
                    point.setPointId(pointId);
                    point.setInitialElevation(elevation);
                    point.setOrderIndex(orderIndex++); // 设置排序索引

                    // 设置可选字段
                    if (mileageColumn != -1) {
                        Cell mileageCell = row.getCell(mileageColumn);
                        if (mileageCell != null) {
                            point.setMileage(getStringCellValue(mileageCell));
                        }
                    }

                    if (rateWarningColumn != -1) {
                        Cell rateWarningCell = row.getCell(rateWarningColumn);
                        if (rateWarningCell != null) {
                            point.setRateWarningValue(getNumericCellValue(rateWarningCell));
                        }
                    }

                    if (accumulatedWarningColumn != -1) {
                        Cell accWarningCell = row.getCell(accumulatedWarningColumn);
                        if (accWarningCell != null) {
                            point.setAccumulatedWarningValue(getNumericCellValue(accWarningCell));
                        }
                    }

                    importedPoints.add(point);
                } else if (point != null) {
                    // 更新现有测点
                    point.setInitialElevation(elevation);
                    // 如果没有设置过排序索引，则设置一个
                    if (point.getOrderIndex() == 0) {
                        point.setOrderIndex(orderIndex++);
                    }

                    // 更新可选字段
                    if (mileageColumn != -1) {
                        Cell mileageCell = row.getCell(mileageColumn);
                        if (mileageCell != null) {
                            point.setMileage(getStringCellValue(mileageCell));
                        }
                    }

                    if (rateWarningColumn != -1) {
                        Cell rateWarningCell = row.getCell(rateWarningColumn);
                        if (rateWarningCell != null) {
                            point.setRateWarningValue(getNumericCellValue(rateWarningCell));
                        }
                    }

                    if (accumulatedWarningColumn != -1) {
                        Cell accWarningCell = row.getCell(accumulatedWarningColumn);
                        if (accWarningCell != null) {
                            point.setAccumulatedWarningValue(getNumericCellValue(accWarningCell));
                        }
                    }

                    importedPoints.add(point);
                }
            }

            return importedPoints;

        } catch (Exception e) {
            throw new IOException("导入Excel文件时出错: " + e.getMessage(), e);
        }
    }
}