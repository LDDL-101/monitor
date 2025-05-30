package com.monitor.util;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 图标工具类，用于生成简单的图标并保存为文件
 */
public class IconUtil {

    /**
     * 生成所有需要的图标并保存到指定目录
     */
    public static void generateIcons() {
        String targetDir = "src/main/resources/images";
        
        // 确保目录存在
        try {
            Files.createDirectories(Paths.get(targetDir));
        } catch (IOException e) {
            System.err.println("创建图标目录失败: " + e.getMessage());
            return;
        }
        
        // 生成各种图标
        createAppIcon(targetDir + "/app_icon.png");
        createIcon(targetDir + "/new.png", Color.DODGERBLUE);
        createIcon(targetDir + "/open.png", Color.GREEN);
        createIcon(targetDir + "/save.png", Color.DARKBLUE);
        createIcon(targetDir + "/save_as.png", Color.BLUE);
        createIcon(targetDir + "/add_module.png", Color.ORANGE);
        createIcon(targetDir + "/properties.png", Color.PURPLE);
        createIcon(targetDir + "/save_default.png", Color.DARKGREEN);
        createIcon(targetDir + "/load_default.png", Color.TEAL);
        createIcon(targetDir + "/parameters.png", Color.MAROON);
        createIcon(targetDir + "/user.png", Color.GRAY);
    }
    
    /**
     * 创建应用程序图标
     * @param filePath 文件路径
     */
    private static void createAppIcon(String filePath) {
        WritableImage image = new WritableImage(32, 32);
        PixelWriter pixelWriter = image.getPixelWriter();
        
        // 绘制铁轨图标
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                // 背景
                Color color = Color.LIGHTBLUE;
                
                // 铁轨横条
                if ((y == 10 || y == 22) && x >= 5 && x <= 27) {
                    color = Color.DARKBLUE;
                }
                
                // 铁轨枕木
                if (x >= 10 && x <= 22 && (y % 4 == 0 || y % 4 == 1) && y >= 6 && y <= 26) {
                    color = Color.DARKBLUE;
                }
                
                pixelWriter.setColor(x, y, color);
            }
        }
        
        // 使用JavaFX的Image类不能直接保存图片，所以这里我们只是生成了图像
        // 在实际的项目中，您需要使用其他库（如JavaFX SnapshotParameters）来保存图像
        System.out.println("应用图标应该保存到: " + filePath);
    }
    
    /**
     * 创建简单的颜色图标
     * @param filePath 文件路径
     * @param color 图标颜色
     */
    private static void createIcon(String filePath, Color color) {
        WritableImage image = new WritableImage(32, 32);
        PixelWriter pixelWriter = image.getPixelWriter();
        
        // 绘制简单的方形图标
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                if (x < 2 || y < 2 || x > 29 || y > 29) {
                    // 边框
                    pixelWriter.setColor(x, y, Color.BLACK);
                } else {
                    // 内部填充
                    pixelWriter.setColor(x, y, color);
                }
            }
        }
        
        // 使用JavaFX的Image类不能直接保存图片，所以这里我们只是生成了图像
        // 在实际的项目中，您需要使用其他库（如JavaFX SnapshotParameters）来保存图像
        System.out.println("图标应该保存到: " + filePath);
    }
} 