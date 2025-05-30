package com.monitor.view;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.monitor.model.SettlementPoint;
import com.monitor.util.AlertUtil;
import com.monitor.util.ExcelUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * 数据导入向导控制器
 */
public class ImportWizardController {

    @FXML private VBox step1Box;
    @FXML private VBox step2Box;
    @FXML private VBox step3Box;

    @FXML private Button selectFileButton;
    @FXML private Label selectedFileLabel;
    @FXML private Button nextButton1;

    @FXML private TextField defaultInitialElevationField;
    @FXML private TextField defaultRateWarningField;
    @FXML private TextField defaultAccumulatedWarningField;
    @FXML private Button nextButton2;
    @FXML private Button backButton2;

    @FXML private ProgressBar importProgressBar;
    @FXML private Label importStatusLabel;
    @FXML private Button finishButton;
    @FXML private Button backButton3;

    private Stage dialogStage;
    private File selectedFile;
    private List<SettlementPoint> importedPoints = new ArrayList<>();
    private boolean importSuccess = false;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 初始化显示第一步
        showStep(1);

        // 设置默认值
        defaultInitialElevationField.setText("0.0");
        defaultRateWarningField.setText("2.0");
        defaultAccumulatedWarningField.setText("20.0");

        // 禁用下一步按钮，直到选择文件
        nextButton1.setDisable(true);

        // 设置按钮事件
        selectFileButton.setOnAction(e -> handleSelectFile());
        nextButton1.setOnAction(e -> showStep(2));
        backButton2.setOnAction(e -> showStep(1));
        nextButton2.setOnAction(e -> {
            if (validateInputs()) {
                showStep(3);
                importData();
            }
        });
        backButton3.setOnAction(e -> showStep(2));
        finishButton.setOnAction(e -> {
            if (importSuccess) {
                dialogStage.close();
            } else {
                AlertUtil.showWarning("导入未完成", "请等待导入完成或重试");
            }
        });
    }

    /**
     * 设置对话框的Stage
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * 获取导入的测点
     */
    public List<SettlementPoint> getImportedPoints() {
        return importedPoints;
    }

    /**
     * 显示指定步骤
     */
    private void showStep(int step) {
        step1Box.setVisible(step == 1);
        step2Box.setVisible(step == 2);
        step3Box.setVisible(step == 3);
    }

    /**
     * 处理选择文件按钮事件
     */
    private void handleSelectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择Excel文件");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel文件", "*.xlsx")
        );

        File file = fileChooser.showOpenDialog(dialogStage);
        if (file != null) {
            selectedFile = file;
            selectedFileLabel.setText("已选择: " + file.getName());
            nextButton1.setDisable(false);
        }
    }

    /**
     * 验证输入
     */
    private boolean validateInputs() {
        StringBuilder errorMessage = new StringBuilder();

        try {
            if (!defaultInitialElevationField.getText().trim().isEmpty()) {
                Double.parseDouble(defaultInitialElevationField.getText().trim());
            } else {
                errorMessage.append("默认初始高程不能为空！\n");
            }
        } catch (NumberFormatException e) {
            errorMessage.append("默认初始高程必须是有效的数字！\n");
        }

        try {
            if (!defaultRateWarningField.getText().trim().isEmpty()) {
                Double.parseDouble(defaultRateWarningField.getText().trim());
            } else {
                errorMessage.append("默认速率报警值不能为空！\n");
            }
        } catch (NumberFormatException e) {
            errorMessage.append("默认速率报警值必须是有效的数字！\n");
        }

        try {
            if (!defaultAccumulatedWarningField.getText().trim().isEmpty()) {
                Double.parseDouble(defaultAccumulatedWarningField.getText().trim());
            } else {
                errorMessage.append("默认累计报警值不能为空！\n");
            }
        } catch (NumberFormatException e) {
            errorMessage.append("默认累计报警值必须是有效的数字！\n");
        }

        if (errorMessage.length() > 0) {
            AlertUtil.showError("输入错误", errorMessage.toString());
            return false;
        }

        return true;
    }

    /**
     * 导入数据
     */
    private void importData() {
        // 禁用返回按钮
        backButton3.setDisable(true);
        finishButton.setDisable(true);

        // 获取默认值
        double defaultInitialElevation = Double.parseDouble(defaultInitialElevationField.getText().trim());
        double defaultRateWarning = Double.parseDouble(defaultRateWarningField.getText().trim());
        double defaultAccumulatedWarning = Double.parseDouble(defaultAccumulatedWarningField.getText().trim());

        // 创建后台任务
        Thread importThread = new Thread(() -> {
            try {
                // 更新UI状态
                updateStatus(0.1, "正在读取Excel文件...");

                // 读取Excel数据
                Map<String, Double> pointElevationMap = ExcelUtil.importFromExcel(selectedFile, "地表点沉降");

                if (pointElevationMap.isEmpty()) {
                    updateStatus(0, "导入失败: 文件中没有有效数据");
                    return;
                }

                updateStatus(0.4, "正在处理测点数据...");

                // 创建测点对象
                importedPoints.clear();
                int index = 0;
                for (Map.Entry<String, Double> entry : pointElevationMap.entrySet()) {
                    String pointId = entry.getKey();
                    double elevation = entry.getValue();

                    SettlementPoint point = new SettlementPoint();
                    point.setPointId(pointId);
                    point.setInitialElevation(elevation);
                    point.setRateWarningValue(defaultRateWarning);
                    point.setAccumulatedWarningValue(defaultAccumulatedWarning);
                    // 不再设置OrderIndex
                    index++;

                    importedPoints.add(point);

                    // 更新进度
                    double progress = 0.4 + (0.5 * index / pointElevationMap.size());
                    updateStatus(progress, "已处理 " + index + "/" + pointElevationMap.size() + " 个测点");
                }

                updateStatus(1.0, "导入完成，共导入 " + importedPoints.size() + " 个测点");
                importSuccess = true;

                // 启用完成按钮
                javafx.application.Platform.runLater(() -> {
                    finishButton.setDisable(false);
                });

            } catch (IOException e) {
                updateStatus(0, "导入失败: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                updateStatus(0, "导入失败: " + e.getMessage());
                e.printStackTrace();
            }
        });

        importThread.setDaemon(true);
        importThread.start();
    }

    /**
     * 更新状态
     */
    private void updateStatus(double progress, String message) {
        javafx.application.Platform.runLater(() -> {
            importProgressBar.setProgress(progress);
            importStatusLabel.setText(message);
        });
    }
}
