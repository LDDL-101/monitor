package com.monitor.view;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.monitor.model.SettlementPoint;
import com.monitor.util.AlertUtil;
import com.monitor.util.ExcelUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;

/**
 * 数据导入向导控制器
 */
public class DataImportWizardController {
    
    @FXML private VBox step1Box;
    @FXML private VBox step2Box;
    @FXML private VBox step3Box;
    
    @FXML private Button selectFileButton;
    @FXML private Label selectedFileLabel;
    @FXML private Button nextButton1;
    
    @FXML private TableView<Map.Entry<String, Double>> previewTableView;
    @FXML private TableColumn<Map.Entry<String, Double>, String> pointIdColumn;
    @FXML private TableColumn<Map.Entry<String, Double>, Number> elevationColumn;
    @FXML private CheckBox createMissingPointsCheckBox;
    @FXML private Button backButton2;
    @FXML private Button nextButton2;
    
    @FXML private Label importSummaryLabel;
    @FXML private ProgressIndicator importProgressIndicator;
    @FXML private Button backButton3;
    @FXML private Button finishButton;
    
    private Stage dialogStage;
    private File selectedFile;
    private Map<String, Double> pointElevationMap;
    private ObservableList<Map.Entry<String, Double>> previewData = FXCollections.observableArrayList();
    private List<SettlementPoint> existingPoints;
    private List<SettlementPoint> importedPoints;
    private boolean importSuccessful = false;
    
    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 初始化步骤显示
        showStep(1);
        
        // 设置预览表格
        pointIdColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getKey()));
        
        elevationColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getValue()));
        
        previewTableView.setItems(previewData);
        
        // 设置按钮状态
        nextButton1.setDisable(true);
        finishButton.setDisable(true);
    }
    
    /**
     * 设置对话框的Stage
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    
    /**
     * 设置已存在的测点列表
     */
    public void setExistingPoints(List<SettlementPoint> existingPoints) {
        this.existingPoints = existingPoints;
    }
    
    /**
     * 获取导入的测点列表
     */
    public List<SettlementPoint> getImportedPoints() {
        return importedPoints;
    }
    
    /**
     * 导入是否成功
     */
    public boolean isImportSuccessful() {
        return importSuccessful;
    }
    
    /**
     * 获取选择的文件
     */
    public File getSelectedFile() {
        return selectedFile;
    }
    
    /**
     * 获取点位高程映射
     */
    public Map<String, Double> getPointElevationMap() {
        return pointElevationMap;
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
    @FXML
    private void handleSelectFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择Excel数据文件");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel文件", "*.xlsx", "*.xls")
        );
        
        File file = fileChooser.showOpenDialog(dialogStage);
        if (file != null) {
            selectedFile = file;
            selectedFileLabel.setText(file.getName());
            nextButton1.setDisable(false);
        }
    }
    
    /**
     * 处理第一步下一步按钮事件
     */
    @FXML
    private void handleNext1(ActionEvent event) {
        try {
            // 读取Excel数据
            pointElevationMap = ExcelUtil.importFromExcel(selectedFile, "地表点沉降");
            
            if (pointElevationMap.isEmpty()) {
                AlertUtil.showWarning("数据为空", "所选文件不包含有效数据。");
                return;
            }
            
            // 更新预览表格
            previewData.clear();
            previewData.addAll(pointElevationMap.entrySet());
            
            // 显示第二步
            showStep(2);
            
        } catch (IOException e) {
            AlertUtil.showError("文件读取错误", "无法读取所选文件: " + e.getMessage());
        }
    }
    
    /**
     * 处理第二步返回按钮事件
     */
    @FXML
    private void handleBack2(ActionEvent event) {
        showStep(1);
    }
    
    /**
     * 处理第二步下一步按钮事件
     */
    @FXML
    private void handleNext2(ActionEvent event) {
        // 显示第三步
        showStep(3);
        importProgressIndicator.setProgress(-1); // 显示不确定进度
        
        // 在后台线程中处理导入
        new Thread(() -> {
            try {
                // 导入数据
                importedPoints = ExcelUtil.importSettlementPointsFromExcel(
                    selectedFile, 
                    existingPoints, 
                    createMissingPointsCheckBox.isSelected()
                );
                
                // 更新UI
                javafx.application.Platform.runLater(() -> {
                    importProgressIndicator.setProgress(1.0);
                    importSummaryLabel.setText(String.format(
                        "导入完成！共导入 %d 个测点数据。\n新增测点: %d 个\n更新测点: %d 个",
                        pointElevationMap.size(),
                        importedPoints.size(),
                        pointElevationMap.size() - importedPoints.size()
                    ));
                    finishButton.setDisable(false);
                    importSuccessful = true;
                });
                
            } catch (Exception e) {
                // 更新UI显示错误
                javafx.application.Platform.runLater(() -> {
                    importProgressIndicator.setProgress(0);
                    importSummaryLabel.setText("导入失败: " + e.getMessage());
                    finishButton.setDisable(true);
                    importSuccessful = false;
                });
            }
        }).start();
    }
    
    /**
     * 处理第三步返回按钮事件
     */
    @FXML
    private void handleBack3(ActionEvent event) {
        showStep(2);
    }
    
    /**
     * 处理完成按钮事件
     */
    @FXML
    private void handleFinish(ActionEvent event) {
        dialogStage.close();
    }
}
