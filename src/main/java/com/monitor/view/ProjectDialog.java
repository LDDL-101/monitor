package com.monitor.view;

import com.monitor.model.ProjectInfo;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目对话框
 * 用于创建或编辑项目信息
 */
public class ProjectDialog extends Dialog<ProjectInfo> {
    
    private TextField nameField;
    private TextArea descriptionArea;
    private TextField organizationField;
    private TextField managerField;
    private TextField filePathField;
    private Button browseButton;
    private Map<String, CheckBox> itemCheckboxes = new HashMap<>();
    private File selectedFile;
    
    private static final String[] MONITORING_ITEM_TEMPLATES = {
        "地表点沉降", "桩顶竖向位移", "桩顶水平位移", "钢支撑轴力", 
        "砼支撑轴力", "立柱竖向位移", "建筑物沉降", "建筑物倾斜", 
        "地下水位", "深部水平位移"
    };
    
    /**
     * 创建新项目对话框
     */
    public ProjectDialog() {
        this(null);
    }
    
    /**
     * 创建项目对话框，用于编辑现有项目
     * @param projectInfo 项目信息，如果为null则创建新项目
     */
    public ProjectDialog(ProjectInfo projectInfo) {
        setTitle(projectInfo == null ? "新建项目" : "编辑项目");
        
        // 设置对话框按钮
        ButtonType completeButtonType = new ButtonType("完成", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(completeButtonType, cancelButtonType);
        
        // 创建表单
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(0, 0, 10, 0));
        
        nameField = new TextField();
        nameField.setPromptText("请输入项目名称");
        
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("请输入项目描述");
        descriptionArea.setPrefRowCount(3);
        
        organizationField = new TextField();
        organizationField.setPromptText("请输入监测单位");
        
        managerField = new TextField();
        managerField.setPromptText("请输入监测负责人");
        
        // 文件路径选择
        filePathField = new TextField();
        filePathField.setPromptText("项目文件保存路径");
        filePathField.setEditable(false);
        
        browseButton = new Button("浏览...");
        browseButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择项目保存位置");
            fileChooser.getExtensionFilters().add(
                new ExtensionFilter("工程监测项目文件", "*.jc")
            );
            
            // 如果已有项目名称，则使用项目名称作为默认文件名
            if (!nameField.getText().isEmpty()) {
                fileChooser.setInitialFileName(nameField.getText() + ".jc");
            } else {
                fileChooser.setInitialFileName("新建项目.jc");
            }
            
            // 显示保存对话框
            File file = fileChooser.showSaveDialog(getDialogPane().getScene().getWindow());
            if (file != null) {
                selectedFile = file;
                filePathField.setText(file.getAbsolutePath());
            }
        });
        
        HBox fileBox = new HBox(10);
        fileBox.getChildren().addAll(filePathField, browseButton);
        
        grid.add(new Label("项目名称:"), 0, 0);
        grid.add(nameField, 1, 0);
        
        grid.add(new Label("监测单位:"), 0, 1);
        grid.add(organizationField, 1, 1);
        
        grid.add(new Label("监测负责人:"), 0, 2);
        grid.add(managerField, 1, 2);
        
        grid.add(new Label("项目描述:"), 0, 3);
        grid.add(descriptionArea, 1, 3);
        
        // 监测测项选择部分
        VBox itemsBox = new VBox(5);
        itemsBox.setPadding(new Insets(10, 0, 0, 0));
        
        Label itemsLabel = new Label("选择监测测项:");
        itemsLabel.setStyle("-fx-font-weight: bold;");
        itemsBox.getChildren().add(itemsLabel);
        
        // 使用网格布局来排列复选框
        GridPane itemsGrid = new GridPane();
        itemsGrid.setHgap(20);  // 水平间距
        itemsGrid.setVgap(10);  // 垂直间距
        itemsGrid.setPadding(new Insets(5, 0, 0, 0));
        
        int itemsPerRow = 3;  // 每行显示的测项数量
        int row = 0;
        int col = 0;
        
        for (String itemTemplate : MONITORING_ITEM_TEMPLATES) {
            CheckBox checkBox = new CheckBox(itemTemplate);
            itemCheckboxes.put(itemTemplate, checkBox);
            itemsGrid.add(checkBox, col, row);
            
            // 更新行列位置
            col++;
            if (col >= itemsPerRow) {
                col = 0;
                row++;
            }
        }
        
        itemsBox.getChildren().add(itemsGrid);
        
        // 保存位置区域
        VBox saveLocationBox = new VBox(5);
        saveLocationBox.setPadding(new Insets(10, 0, 0, 0));
        
        Label saveLocationLabel = new Label("项目保存位置:");
        saveLocationLabel.setStyle("-fx-font-weight: bold;");
        saveLocationBox.getChildren().addAll(saveLocationLabel, fileBox);
        
        // 组装内容
        content.getChildren().addAll(grid, new Separator(), itemsBox, new Separator(), saveLocationBox);
        getDialogPane().setContent(content);
        
        // 如果是编辑模式，填充现有数据
        if (projectInfo != null) {
            nameField.setText(projectInfo.getName());
            descriptionArea.setText(projectInfo.getDescription());
            organizationField.setText(projectInfo.getOrganization());
            managerField.setText(projectInfo.getManager());
            
            for (String item : projectInfo.getMonitoringItems()) {
                CheckBox cb = itemCheckboxes.get(item);
                if (cb != null) {
                    cb.setSelected(true);
                }
            }
        }
        
        // 设置结果转换器
        setResultConverter(dialogButton -> {
            if (dialogButton == completeButtonType) {
                // 如果用户没有选择保存位置，则显示错误信息并返回null
                if (projectInfo == null && selectedFile == null) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setHeaderText("未选择保存位置");
                    alert.setContentText("请选择项目文件的保存位置。");
                    alert.showAndWait();
                    return null;
                }
                
                ProjectInfo result = new ProjectInfo();
                result.setName(nameField.getText());
                result.setDescription(descriptionArea.getText());
                result.setOrganization(organizationField.getText());
                result.setManager(managerField.getText());
                
                List<String> selectedItems = new ArrayList<>();
                for (Map.Entry<String, CheckBox> entry : itemCheckboxes.entrySet()) {
                    if (entry.getValue().isSelected()) {
                        selectedItems.add(entry.getKey());
                    }
                }
                result.setMonitoringItems(selectedItems);
                
                // 在ProjectInfo中存储选择的文件
                result.setProjectFile(selectedFile);
                
                return result;
            }
            return null;
        });
        
        // 获取对话框的stage并设置大小
        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        stage.setMinWidth(500);
        stage.setMinHeight(600);
        
        // 名称变更时更新建议的文件名
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (selectedFile == null && !newValue.isEmpty()) {
                // 当文件未选择且项目名称变更时，更新文件路径框的提示文本
                filePathField.setPromptText("将保存为: " + newValue + ".jc");
            }
        });
    }
    
    /**
     * 获取选择的保存文件
     * @return 选择的文件对象，如果未选择则返回null
     */
    public File getSelectedFile() {
        return selectedFile;
    }
} 