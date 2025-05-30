package com.monitor.util;

import java.io.IOException;
import java.util.List;

import com.monitor.model.SettlementPoint;
import com.monitor.view.DataImportWizardController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * 对话框工具类
 */
public class DialogUtil {
    
    /**
     * 显示数据导入向导对话框
     * 
     * @param parentWindow 父窗口
     * @param existingPoints 已存在的测点列表
     * @return 导入的测点列表，如果取消则返回null
     */
    public static List<SettlementPoint> showDataImportWizard(Window parentWindow, List<SettlementPoint> existingPoints) {
        try {
            // 加载FXML
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(DialogUtil.class.getResource("/fxml/dialogs/DataImportWizardDialog.fxml"));
            BorderPane page = loader.load();
            
            // 创建对话框
            Stage dialogStage = new Stage();
            dialogStage.setTitle("数据导入向导");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(parentWindow);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);
            
            // 设置控制器
            DataImportWizardController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setExistingPoints(existingPoints);
            
            // 显示对话框并等待关闭
            dialogStage.showAndWait();
            
            // 返回导入的测点
            if (controller.isImportSuccessful()) {
                return controller.getImportedPoints();
            } else {
                return null;
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("对话框错误", "无法加载数据导入向导: " + e.getMessage());
            return null;
        }
    }
}
