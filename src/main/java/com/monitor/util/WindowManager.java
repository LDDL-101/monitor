package com.monitor.util;

import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * 窗口管理器，用于处理无边框窗口的移动、调整大小等操作
 */
public class WindowManager {
    private Stage stage;
    private Scene scene;
    private double xOffset = 0;
    private double yOffset = 0;
    private boolean isMaximized = false;
    private double restoreX, restoreY, restoreWidth, restoreHeight;
    
    // 窗口调整大小的区域边界宽度
    private static final double RESIZE_BORDER = 5;
    private boolean resizing = false;
    private String resizeDirection = "";
    
    public WindowManager(Stage stage, Scene scene) {
        this.stage = stage;
        this.scene = scene;
        
        // 保存初始窗口位置和大小，用于恢复窗口
        saveWindowBounds();
    }
    
    /**
     * 处理鼠标按下事件，记录鼠标位置用于窗口拖动
     */
    public void handleMousePressed(MouseEvent event) {
        if (isInResizeBorder(event)) {
            resizing = true;
            resizeDirection = getResizeDirection(event);
            event.consume();
        } else {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        }
    }
    
    /**
     * 处理鼠标拖动事件，实现窗口移动或调整大小
     */
    public void handleMouseDragged(MouseEvent event) {
        if (isMaximized) {
            // 如果窗口最大化，则恢复窗口并重新设置拖动位置
            restoreWindow();
            
            // 根据鼠标在窗口中的相对位置重新计算偏移量
            double relativeX = event.getSceneX() / scene.getWidth();
            xOffset = event.getSceneX();
            stage.setX(event.getScreenX() - xOffset);
        } else if (resizing) {
            resizeWindow(event);
        } else {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        }
    }
    
    /**
     * 处理鼠标释放事件，结束窗口大小调整
     */
    public void handleMouseReleased(MouseEvent event) {
        resizing = false;
        resizeDirection = "";
        scene.setCursor(Cursor.DEFAULT);
    }

    /**
     * 处理鼠标移动事件，更新鼠标样式以指示可调整大小的区域
     */
    public void handleMouseMoved(MouseEvent event) {
        if (isMaximized) {
            scene.setCursor(Cursor.DEFAULT);
            return;
        }
        
        if (isInResizeBorder(event)) {
            String direction = getResizeDirection(event);
            switch (direction) {
                case "N":
                case "S":
                    scene.setCursor(Cursor.V_RESIZE);
                    break;
                case "E":
                case "W":
                    scene.setCursor(Cursor.H_RESIZE);
                    break;
                case "NE":
                case "SW":
                    scene.setCursor(Cursor.NE_RESIZE);
                    break;
                case "NW":
                case "SE":
                    scene.setCursor(Cursor.NW_RESIZE);
                    break;
            }
        } else {
            scene.setCursor(Cursor.DEFAULT);
        }
    }
    
    /**
     * 判断鼠标是否在窗口的调整大小边界内
     */
    private boolean isInResizeBorder(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();
        double width = scene.getWidth();
        double height = scene.getHeight();
        
        return x < RESIZE_BORDER || // 左边界
               y < RESIZE_BORDER || // 上边界
               x > width - RESIZE_BORDER || // 右边界
               y > height - RESIZE_BORDER; // 下边界
    }
    
    /**
     * 获取调整大小的方向
     */
    private String getResizeDirection(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();
        double width = scene.getWidth();
        double height = scene.getHeight();
        
        boolean north = y < RESIZE_BORDER;
        boolean south = y > height - RESIZE_BORDER;
        boolean east = x > width - RESIZE_BORDER;
        boolean west = x < RESIZE_BORDER;
        
        if (north && east) return "NE";
        if (north && west) return "NW";
        if (south && east) return "SE";
        if (south && west) return "SW";
        if (north) return "N";
        if (south) return "S";
        if (east) return "E";
        if (west) return "W";
        
        return "";
    }
    
    /**
     * 根据鼠标拖动方向调整窗口大小
     */
    private void resizeWindow(MouseEvent event) {
        double x = event.getScreenX();
        double y = event.getScreenY();
        double currentX = stage.getX();
        double currentY = stage.getY();
        double currentWidth = stage.getWidth();
        double currentHeight = stage.getHeight();
        
        // 设置最小窗口尺寸
        double minWidth = 400;
        double minHeight = 300;
        
        switch (resizeDirection) {
            case "N":
                if (currentHeight + (currentY - y) >= minHeight) {
                    stage.setHeight(currentHeight + (currentY - y));
                    stage.setY(y);
        }
                break;
            case "S":
                double newHeight = y - currentY;
                if (newHeight >= minHeight) {
                    stage.setHeight(newHeight);
                }
                break;
            case "E":
                double newWidth = x - currentX;
                if (newWidth >= minWidth) {
                    stage.setWidth(newWidth);
                }
                break;
            case "W":
                if (currentWidth + (currentX - x) >= minWidth) {
                    stage.setWidth(currentWidth + (currentX - x));
                    stage.setX(x);
        }
                break;
            case "NE":
                if (currentHeight + (currentY - y) >= minHeight) {
                    stage.setHeight(currentHeight + (currentY - y));
                    stage.setY(y);
        }
                double newWidthNE = x - currentX;
                if (newWidthNE >= minWidth) {
                    stage.setWidth(newWidthNE);
                }
                break;
            case "NW":
                if (currentHeight + (currentY - y) >= minHeight) {
                    stage.setHeight(currentHeight + (currentY - y));
                    stage.setY(y);
                }
                if (currentWidth + (currentX - x) >= minWidth) {
                    stage.setWidth(currentWidth + (currentX - x));
                    stage.setX(x);
        }
                break;
            case "SE":
                double newHeightSE = y - currentY;
                if (newHeightSE >= minHeight) {
                    stage.setHeight(newHeightSE);
                }
                double newWidthSE = x - currentX;
                if (newWidthSE >= minWidth) {
                    stage.setWidth(newWidthSE);
                }
                break;
            case "SW":
                double newHeightSW = y - currentY;
                if (newHeightSW >= minHeight) {
                    stage.setHeight(newHeightSW);
                }
                if (currentWidth + (currentX - x) >= minWidth) {
                    stage.setWidth(currentWidth + (currentX - x));
                    stage.setX(x);
                }
                break;
        }
    }

    /**
     * 最大化或恢复窗口
     */
    public void maximizeOrRestoreWindow() {
        if (isMaximized) {
            restoreWindow();
        } else {
            maximizeWindow();
        }
    }
    
    /**
     * 最大化窗口
     */
    private void maximizeWindow() {
        if (!isMaximized) {
            // 保存当前窗口位置和大小
            saveWindowBounds();
            
            // 获取当前屏幕的可用边界
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        
            // 设置窗口填满整个屏幕
        stage.setX(screenBounds.getMinX());
        stage.setY(screenBounds.getMinY());
        stage.setWidth(screenBounds.getWidth());
        stage.setHeight(screenBounds.getHeight());
        
        isMaximized = true;
        }
    }
    
    /**
     * 恢复窗口到之前的大小
     */
    private void restoreWindow() {
        if (isMaximized) {
            // 恢复窗口到之前保存的位置和大小
        stage.setX(restoreX);
        stage.setY(restoreY);
        stage.setWidth(restoreWidth);
        stage.setHeight(restoreHeight);
        
        isMaximized = false;
    }
    }
    
    /**
     * 保存当前窗口位置和大小，用于恢复窗口
     */
    private void saveWindowBounds() {
        restoreX = stage.getX();
        restoreY = stage.getY();
        restoreWidth = stage.getWidth();
        restoreHeight = stage.getHeight();
    }
    
    /**
     * 安装窗口事件处理器
     */
    public void setupWindowHandlers(Node titleNode) {
        scene.setOnMouseMoved(this::handleMouseMoved);
        scene.setOnMouseReleased(this::handleMouseReleased);
        
        // 双击标题栏最大化或恢复窗口
        if (titleNode != null) {
            titleNode.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    maximizeOrRestoreWindow();
                }
            });
        }
    }
}
