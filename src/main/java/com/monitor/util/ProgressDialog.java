package com.monitor.util;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * 进度对话框
 */
public class ProgressDialog extends Stage {

    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final StringProperty message = new SimpleStringProperty("");
    private final Label messageLabel;

    /**
     * 创建进度对话框
     */
    public ProgressDialog() {
        initStyle(StageStyle.UTILITY);
        initModality(Modality.APPLICATION_MODAL);
        setResizable(false);

        // 创建进度条
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);
        progressBar.progressProperty().bind(progress);

        // 创建消息标签
        messageLabel = new Label();
        messageLabel.textProperty().bind(message);

        // 创建布局
        VBox vbox = new VBox(10);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(20));
        vbox.getChildren().addAll(messageLabel, progressBar);

        // 设置场景
        Scene scene = new Scene(vbox);
        setScene(scene);
    }

    /**
     * 获取进度属性
     */
    public DoubleProperty progressProperty() {
        return progress;
    }

    /**
     * 获取消息属性
     */
    public StringProperty messageProperty() {
        return message;
    }

    // 注意: 不能覆盖Stage类中的final方法setTitle
    // 使用原生的setTitle方法即可

    /**
     * 设置头部文本
     */
    public void setHeaderText(String text) {
        // 可以在这里添加头部标签
    }

    /**
     * 设置内容文本
     */
    public void setContentText(String text) {
        message.set(text);
    }
}
