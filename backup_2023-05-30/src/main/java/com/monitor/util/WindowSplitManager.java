package com.monitor.util;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.util.HashMap;
import java.util.Map;

/**
 * 窗口分割管理器，用于处理编辑区域的分窗显示
 */
public class WindowSplitManager {
    
    // 主编辑区域
    private BorderPane mainEditorArea;
    
    // 当前活动的TabPane
    private TabPane activeTabPane;
    
    // 所有TabPane的映射，用于跟踪
    private Map<String, TabPane> tabPaneMap = new HashMap<>();
    
    // 分割方向枚举
    public enum SplitDirection {
        HORIZONTAL, VERTICAL
    }
    
    /**
     * 构造函数
     * @param mainEditorArea 主编辑区域
     * @param initialTabPane 初始TabPane
     */
    public WindowSplitManager(BorderPane mainEditorArea, TabPane initialTabPane) {
        this.mainEditorArea = mainEditorArea;
        this.activeTabPane = initialTabPane;
        
        // 将初始TabPane添加到映射中
        tabPaneMap.put("main", initialTabPane);
        
        // 设置初始TabPane为活动
        setActiveTabPane(initialTabPane);
    }
    
    /**
     * 分割当前活动的TabPane
     * @param direction 分割方向
     * @return 新创建的TabPane
     */
    public TabPane splitActiveTabPane(SplitDirection direction) {
        // 创建新的TabPane
        TabPane newTabPane = createTabPane();
        
        // 获取当前活动TabPane的父容器
        Node parent = activeTabPane.getParent();
        
        if (parent instanceof SplitPane) {
            // 如果父容器是SplitPane，则添加新的TabPane到SplitPane
            SplitPane splitPane = (SplitPane) parent;
            
            // 检查分割方向是否与SplitPane的方向一致
            boolean isHorizontal = direction == SplitDirection.HORIZONTAL;
            boolean splitPaneIsHorizontal = splitPane.getOrientation() == Orientation.HORIZONTAL;
            
            if (isHorizontal == splitPaneIsHorizontal) {
                // 如果方向一致，直接添加到SplitPane
                int index = splitPane.getItems().indexOf(activeTabPane);
                splitPane.getItems().add(index + 1, newTabPane);
                
                // 调整分割位置
                double[] dividerPositions = splitPane.getDividerPositions();
                splitPane.setDividerPosition(index, (dividerPositions[index] + 1.0) / 2);
            } else {
                // 如果方向不一致，创建新的SplitPane
                SplitPane newSplitPane = createSplitPane(direction);
                
                // 替换当前TabPane为新的SplitPane
                int index = splitPane.getItems().indexOf(activeTabPane);
                splitPane.getItems().set(index, newSplitPane);
                
                // 将当前TabPane和新TabPane添加到新的SplitPane
                newSplitPane.getItems().addAll(activeTabPane, newTabPane);
            }
        } else if (parent instanceof StackPane || parent instanceof BorderPane) {
            // 如果父容器是StackPane或BorderPane，创建新的SplitPane
            SplitPane newSplitPane = createSplitPane(direction);
            
            // 将当前TabPane和新TabPane添加到新的SplitPane
            newSplitPane.getItems().addAll(activeTabPane, newTabPane);
            
            // 替换主编辑区域的中心内容
            mainEditorArea.setCenter(newSplitPane);
        }
        
        // 设置新TabPane为活动
        setActiveTabPane(newTabPane);
        
        // 生成唯一ID并添加到映射
        String id = "tabPane_" + System.currentTimeMillis();
        tabPaneMap.put(id, newTabPane);
        
        return newTabPane;
    }
    
    /**
     * 关闭指定的TabPane
     * @param tabPane 要关闭的TabPane
     */
    public void closeTabPane(TabPane tabPane) {
        // 如果只有一个TabPane，不允许关闭
        if (tabPaneMap.size() <= 1) {
            return;
        }
        
        // 获取TabPane的父容器
        Node parent = tabPane.getParent();
        
        if (parent instanceof SplitPane) {
            SplitPane splitPane = (SplitPane) parent;
            
            // 如果SplitPane只有两个子项，则替换SplitPane为另一个子项
            if (splitPane.getItems().size() == 2) {
                int index = splitPane.getItems().indexOf(tabPane);
                Node otherItem = splitPane.getItems().get(1 - index);
                
                // 获取SplitPane的父容器
                Node grandParent = splitPane.getParent();
                
                if (grandParent instanceof SplitPane) {
                    // 如果祖父容器是SplitPane，替换父SplitPane为另一个子项
                    SplitPane grandSplitPane = (SplitPane) grandParent;
                    int parentIndex = grandSplitPane.getItems().indexOf(splitPane);
                    grandSplitPane.getItems().set(parentIndex, otherItem);
                } else if (grandParent instanceof StackPane || grandParent instanceof BorderPane) {
                    // 如果祖父容器是StackPane或BorderPane，替换主编辑区域的中心内容
                    mainEditorArea.setCenter(otherItem);
                }
            } else {
                // 如果SplitPane有多个子项，直接移除当前TabPane
                splitPane.getItems().remove(tabPane);
            }
        }
        
        // 从映射中移除TabPane
        for (Map.Entry<String, TabPane> entry : tabPaneMap.entrySet()) {
            if (entry.getValue() == tabPane) {
                tabPaneMap.remove(entry.getKey());
                break;
            }
        }
        
        // 如果关闭的是活动TabPane，设置另一个TabPane为活动
        if (tabPane == activeTabPane && !tabPaneMap.isEmpty()) {
            setActiveTabPane(tabPaneMap.values().iterator().next());
        }
    }
    
    /**
     * 将标签页移动到另一个TabPane
     * @param tab 要移动的标签页
     * @param targetTabPane 目标TabPane
     */
    public void moveTabToPane(Tab tab, TabPane targetTabPane) {
        // 获取标签页所在的TabPane
        TabPane sourceTabPane = tab.getTabPane();
        
        if (sourceTabPane != null) {
            // 从源TabPane移除标签页
            sourceTabPane.getTabs().remove(tab);
            
            // 添加到目标TabPane
            targetTabPane.getTabs().add(tab);
            targetTabPane.getSelectionModel().select(tab);
            
            // 设置目标TabPane为活动
            setActiveTabPane(targetTabPane);
            
            // 如果源TabPane没有标签页了，关闭它
            if (sourceTabPane.getTabs().isEmpty()) {
                closeTabPane(sourceTabPane);
            }
        }
    }
    
    /**
     * 设置活动TabPane
     * @param tabPane 要设置为活动的TabPane
     */
    public void setActiveTabPane(TabPane tabPane) {
        // 取消之前活动TabPane的高亮样式
        if (activeTabPane != null) {
            activeTabPane.getStyleClass().remove("active-tab-pane");
        }
        
        // 设置新的活动TabPane
        activeTabPane = tabPane;
        
        // 添加高亮样式
        if (activeTabPane != null) {
            if (!activeTabPane.getStyleClass().contains("active-tab-pane")) {
                activeTabPane.getStyleClass().add("active-tab-pane");
            }
        }
    }
    
    /**
     * 获取当前活动的TabPane
     * @return 当前活动的TabPane
     */
    public TabPane getActiveTabPane() {
        return activeTabPane;
    }
    
    /**
     * 创建新的TabPane
     * @return 新创建的TabPane
     */
    private TabPane createTabPane() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        
        // 添加点击事件，设置为活动TabPane
        tabPane.setOnMouseClicked(event -> setActiveTabPane(tabPane));
        
        return tabPane;
    }
    
    /**
     * 创建新的SplitPane
     * @param direction 分割方向
     * @return 新创建的SplitPane
     */
    private SplitPane createSplitPane(SplitDirection direction) {
        SplitPane splitPane = new SplitPane();
        
        // 设置方向
        if (direction == SplitDirection.HORIZONTAL) {
            splitPane.setOrientation(Orientation.HORIZONTAL);
        } else {
            splitPane.setOrientation(Orientation.VERTICAL);
        }
        
        return splitPane;
    }
}
