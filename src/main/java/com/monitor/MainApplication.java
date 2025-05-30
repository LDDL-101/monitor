package com.monitor;

import com.monitor.util.DatabaseUtil;
import com.monitor.util.IconUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class MainApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // 生成图标
        IconUtil.generateIcons();

        // 初始化数据库
        DatabaseUtil.initialize();
        DatabaseUtil.createTablesIfNotExist();

        // 加载主界面
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("/fxml/main.fxml"));
        Parent root = fxmlLoader.load();

        // 获取屏幕尺寸 - 使用可视区域而不是完整屏幕尺寸（避免覆盖任务栏）
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        // 设置场景大小为屏幕可视区域的90%
        double sceneWidth = screenBounds.getWidth() * 0.9;
        double sceneHeight = screenBounds.getHeight() * 0.9;

        Scene scene = new Scene(root, sceneWidth, sceneHeight);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        scene.setFill(Color.TRANSPARENT); // 设置场景背景为透明

        // 设置无边框窗口
        primaryStage.initStyle(StageStyle.TRANSPARENT);

        // 设置窗口标题和图标
        primaryStage.setTitle("中铁一院测研院工程监测管理软件");

        // 设置窗口属性
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        // 居中显示
        primaryStage.setX((screenBounds.getWidth() - sceneWidth) / 2);
        primaryStage.setY((screenBounds.getHeight() - sceneHeight) / 2);

        primaryStage.setScene(scene);
        
        // 获取控制器并设置屏幕边界和窗口管理器
        com.monitor.controller.MainController controller = fxmlLoader.getController();
        controller.setScreenBounds(screenBounds);
        
        // 添加窗口关闭事件处理
        primaryStage.setOnCloseRequest(event -> {
            // 调用控制器的关闭处理方法
            controller.handleApplicationClose();
            
            // 阻止默认关闭行为，由控制器处理
            event.consume();
        });
        
        // 显示窗口
        primaryStage.show();
        
        // 在场景显示后初始化窗口管理器
        controller.setupWindowManager(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}