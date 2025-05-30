package com.monitor.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/**
 * 提供各种警告和信息窗口的实用工具类
 */
public class AlertUtil {

    /**
     * 显示信息提示窗口
     *
     * @param title 窗口标题
     * @param message 信息内容
     */
    public static void showInformation(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示警告窗口
     *
     * @param title 窗口标题
     * @param message 警告内容
     */
    public static void showWarning(String title, String message) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示错误窗口
     *
     * @param title 窗口标题
     * @param message 错误内容
     */
    public static void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示确认窗口并返回用户选择结果
     *
     * @param title 窗口标题
     * @param message 确认信息
     * @return 用户选择的按钮类型
     */
    public static ButtonType showConfirmation(String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.CANCEL);
    }

    /**
     * 显示确认窗口并返回用户选择结果（布尔值形式）
     *
     * @param title 窗口标题
     * @param message 确认信息
     * @return 如果用户选择OK按钮则返回true，否则返回false
     */
    public static boolean showConfirmationDialog(String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
    
    /**
     * 显示确认窗口并返回用户选择结果（布尔值形式），支持自定义按钮文本
     *
     * @param title 窗口标题
     * @param message 确认信息
     * @param okButtonText 确认按钮文本
     * @param cancelButtonText 取消按钮文本
     * @return 如果用户选择确认按钮则返回true，否则返回false
     */
    public static boolean showConfirmationDialog(String title, String message, String okButtonText, String cancelButtonText) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // 创建自定义按钮
        ButtonType okButton = new ButtonType(okButtonText);
        ButtonType cancelButton = new ButtonType(cancelButtonText);
        
        // 设置按钮
        alert.getButtonTypes().setAll(okButton, cancelButton);
        
        // 显示对话框并获取用户选择
        return alert.showAndWait().orElse(cancelButton) == okButton;
    }

    /**
     * 为Alert窗口设置所有者Stage
     *
     * @param alert 警告窗口
     * @param owner 所有者Stage
     */
    public static void setOwner(Alert alert, Stage owner) {
        if (owner != null) {
            alert.initOwner(owner);
        }
    }
}