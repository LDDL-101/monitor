package com.monitor.controller;

import com.monitor.MainApplication;
import com.monitor.model.*;
import com.monitor.util.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.application.Platform;
import javafx.scene.Cursor;
import java.io.File;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.time.format.DateTimeFormatter;

import com.monitor.util.ProjectFileUtil;
import com.monitor.util.ProjectFileUtil.ProjectFileData;
import com.monitor.view.MonitoringItemEditor;
import com.monitor.view.ProjectDialog;
import java.util.UUID;
import java.lang.reflect.Field;
import java.io.IOException;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.Parent;
import com.monitor.controller.SettlementDataController;
import com.monitor.util.AlertUtil;
import com.monitor.model.SettlementDataStorage;
import com.monitor.controller.PileTopDisplacementController;
import com.monitor.controller.ColumnDisplacementController;
import com.monitor.model.PileDisplacementData;
import com.monitor.model.MeasurementRecord;
import java.time.LocalDate;
import java.time.LocalTime;
import com.monitor.controller.PileTopHorizontalDisplacementController;
import com.monitor.model.PileTopHorizontalDisplacementDataStorage;

public class MainController implements Initializable {

    private double xOffset = 0;
    private double yOffset = 0;
    private boolean isMaximized = false;
    
    // 窗口调整大小的阈值和状态
    private static final int RESIZE_PADDING = 5;
    private double startX = 0;
    private double startY = 0;
    private double startWidth = 0;
    private double startHeight = 0;
    private boolean resizeLeft = false;
    private boolean resizeRight = false;
    private boolean resizeTop = false;
    private boolean resizeBottom = false;
    
    // 屏幕边界，用于最大化行为
    private Rectangle2D screenBounds;
    
    // 数据存储
    private Map<String, MonitoringItem> monitoringItems = new HashMap<>();
    private Map<String, ProjectInfo> projects = new HashMap<>();

    @FXML
    private BorderPane mainPane;

    @FXML
    private Label titleLabel;

    @FXML
    private ImageView appIcon;

    @FXML
    private Button minimizeButton;

    @FXML
    private Button maximizeButton;

    @FXML
    private Button closeButton;

    @FXML
    private Label userLabel;
    
    // 工具栏按钮
    @FXML
    private Button newButton;

    @FXML
    private Button openButton;

    @FXML
    private Button saveButton;

    @FXML
    private Button saveAsButton;

    @FXML
    private Button addModuleButton;

    @FXML
    private Button projectPropertiesButton;

    @FXML
    private Button saveDefaultButton;

    @FXML
    private Button loadDefaultButton;

    @FXML
    private Button parametersButton;
    
    // 侧边栏组件
    @FXML
    private VBox sidebarPane;
    
    @FXML
    private TreeView<String> projectTreeView;
    
    @FXML
    private TreeView<String> reportsTreeView;
    
    @FXML
    private TreeView<String> overviewTreeView;
    
    // 编辑器区域
    @FXML
    private TabPane editorTabPane;

    // 工具栏相关
    @FXML
    private ToolBar mainToolBar;
    
    @FXML
    private CheckMenuItem toggleToolbarMenuItem;

    @FXML private HBox windowHeader;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 设置UI组件事件
        setupUI();
        
        // 设置侧边栏
        setupSidebar();
        
        // 设置编辑器区域
        setupEditorArea();
        
        // 设置自动保存定时器
        setupAutoSaveTimer();

        // 设置窗口标题栏拖动功能
        windowHeader.setOnMousePressed(this::handleHeaderMousePressed);
        windowHeader.setOnMouseDragged(this::handleHeaderMouseDragged);
    }
    
    /**
     * 设置窗口管理器 - 在Scene创建后调用
     * 此方法需要在Scene创建完成后由外部调用
     */
    public void setupWindowManager(Stage stage) {
        // 初始化窗口管理器
        WindowManager windowManager = new WindowManager(stage, mainPane.getScene());
        
        // 设置窗口事件处理
        windowManager.setupWindowHandlers(titleLabel);
        
        // 拖拽标题栏移动窗口
        titleLabel.setOnMousePressed(event -> windowManager.handleMousePressed(event));
        titleLabel.setOnMouseDragged(event -> windowManager.handleMouseDragged(event));
        
        // 添加应用图标的拖拽事件
        appIcon.setOnMousePressed(event -> windowManager.handleMousePressed(event));
        appIcon.setOnMouseDragged(event -> windowManager.handleMouseDragged(event));
        
        // 查找标题栏HBox并设置拖拽事件
        Node titleBar = mainPane.lookup(".app-title");
        if (titleBar != null) {
            // 为整个标题栏添加拖拽事件，但排除按钮
            titleBar.setOnMousePressed(event -> {
                Node target = (Node) event.getTarget();
                // 确保点击的不是按钮或其子元素
                if (!isButtonOrChild(target, minimizeButton) && 
                    !isButtonOrChild(target, maximizeButton) && 
                    !isButtonOrChild(target, closeButton)) {
                    windowManager.handleMousePressed(event);
                }
            });
            titleBar.setOnMouseDragged(event -> {
                Node target = (Node) event.getTarget();
                if (!isButtonOrChild(target, minimizeButton) && 
                    !isButtonOrChild(target, maximizeButton) && 
                    !isButtonOrChild(target, closeButton)) {
                    windowManager.handleMouseDragged(event);
                }
            });
        }
        
        // 窗口控制按钮事件
        minimizeButton.setOnAction(event -> stage.setIconified(true));
        maximizeButton.setOnAction(event -> windowManager.maximizeOrRestoreWindow());
        closeButton.setOnAction(event -> handleApplicationClose());
    }

    /**
     * 检查目标节点是否是指定按钮或其子元素
     */
    private boolean isButtonOrChild(Node target, Button button) {
        while (target != null) {
            if (target == button) {
                return true;
            }
            target = target.getParent();
        }
        return false;
    }

    /**
     * 设置自动保存定时器，每5分钟自动保存所有有更改的项目
     */
    private void setupAutoSaveTimer() {
        // 创建定时器任务
        java.util.Timer timer = new java.util.Timer(true);
        timer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                // 在JavaFX线程上执行保存操作
                Platform.runLater(() -> {
                    autoSaveProjects();
                });
            }
        }, 5 * 60 * 1000, 5 * 60 * 1000); // 延迟5分钟后开始，每5分钟执行一次
    }

    /**
     * 自动保存所有有更改的项目
     */
    private void autoSaveProjects() {
        int savedCount = 0;

        // 检查所有项目
        for (ProjectInfo project : projects.values()) {
            // 只保存有更改且已经有文件路径的项目
            if (hasUnsavedChanges(project) && project.getProjectFile() != null) {
                boolean success = saveProject(project);
                if (success) {
                    savedCount++;
                }
            }
        }

        if (savedCount > 0) {
            System.out.println("自动保存完成，已保存 " + savedCount + " 个项目");
        }
    }

    public void setScreenBounds(Rectangle2D screenBounds) {
        this.screenBounds = screenBounds;
    }

    private void setupUI() {
        // 设置工具栏按钮事件
        newButton.setOnAction(event -> handleNewProject());
        openButton.setOnAction(event -> handleOpenProject());
        saveButton.setOnAction(event -> handleSaveProject());
        saveAsButton.setOnAction(event -> handleSaveAsProject());
        addModuleButton.setOnAction(event -> handleAddModule());
        projectPropertiesButton.setOnAction(event -> handleProjectProperties());
        saveDefaultButton.setOnAction(event -> handleSaveDefault());
        loadDefaultButton.setOnAction(event -> handleLoadDefault());
        parametersButton.setOnAction(event -> handleParameters());
        
        // 设置欢迎界面按钮事件
        setupWelcomeButtons();
        
        // 设置工具栏显示/隐藏功能
        if (toggleToolbarMenuItem != null) {
            // 初始状态与CheckMenuItem的selected属性一致
            mainToolBar.setVisible(toggleToolbarMenuItem.isSelected());
            mainToolBar.setManaged(toggleToolbarMenuItem.isSelected());
            
            // 监听CheckMenuItem的选中状态变化
            toggleToolbarMenuItem.selectedProperty().addListener((observable, oldValue, newValue) -> {
                // 同步工具栏的可见性和管理状态
                mainToolBar.setVisible(newValue);
                mainToolBar.setManaged(newValue); // 这确保布局会重新计算
            });
        }
    }
    
    /**
     * 设置欢迎界面按钮事件
     */
    private void setupWelcomeButtons() {
        // 在欢迎界面标签页中查找按钮
        if (editorTabPane != null && !editorTabPane.getTabs().isEmpty()) {
            Tab welcomeTab = editorTabPane.getTabs().get(0);
            if (welcomeTab != null && welcomeTab.getText().equals("欢迎")) {
                BorderPane welcomePane = (BorderPane) welcomeTab.getContent();
                if (welcomePane != null) {
                    VBox centerVBox = (VBox) welcomePane.getCenter();
                    if (centerVBox != null) {
                        for (Node node : centerVBox.getChildren()) {
                            if (node instanceof HBox) {
                                HBox buttonBox = (HBox) node;
                                for (Node btnNode : buttonBox.getChildren()) {
                                    if (btnNode instanceof Button) {
                                        Button button = (Button) btnNode;
                                        if (button.getText().equals("新建工程")) {
                                            button.setOnAction(event -> handleNewProject());
                                        } else if (button.getText().equals("打开工程")) {
                                            button.setOnAction(event -> handleOpenProject());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private void setupSidebar() {
        // 设置工程管理树
        TreeItem<String> projectRoot = new TreeItem<>("工程列表");
        projectRoot.setExpanded(true);
        
        // 不再添加示例项目
        projectTreeView.setRoot(projectRoot);
        projectTreeView.setShowRoot(true);

        // 加载默认目录中的项目文件
        loadProjectsFromDefaultDirectory();
        
        // 设置监测报表树
        TreeItem<String> reportsRoot = new TreeItem<>("报表类型");
        reportsRoot.setExpanded(true);
        
        reportsRoot.getChildren().add(new TreeItem<>("日报表"));
        reportsRoot.getChildren().add(new TreeItem<>("周报表"));
        reportsRoot.getChildren().add(new TreeItem<>("月报表"));
        reportsRoot.getChildren().add(new TreeItem<>("季报表"));
        reportsRoot.getChildren().add(new TreeItem<>("年报表"));
        
        reportsTreeView.setRoot(reportsRoot);
        reportsTreeView.setShowRoot(true);
        
        // 设置监测概况树
        TreeItem<String> overviewRoot = new TreeItem<>("监测概况");
        overviewRoot.setExpanded(true);
        
        TreeItem<String> alerts = new TreeItem<>("预警信息");
        alerts.getChildren().add(new TreeItem<>("一级预警"));
        alerts.getChildren().add(new TreeItem<>("二级预警"));
        alerts.getChildren().add(new TreeItem<>("三级预警"));
        
        TreeItem<String> statistics = new TreeItem<>("统计数据");
        statistics.getChildren().add(new TreeItem<>("位移监测"));
        statistics.getChildren().add(new TreeItem<>("沉降监测"));
        statistics.getChildren().add(new TreeItem<>("水位监测"));
        statistics.getChildren().add(new TreeItem<>("应力监测"));
        
        overviewRoot.getChildren().addAll(alerts, statistics);
        overviewTreeView.setRoot(overviewRoot);
        overviewTreeView.setShowRoot(true);

        // 确保侧边栏组件填充所有可用空间
        VBox.setVgrow(sidebarPane, javafx.scene.layout.Priority.ALWAYS);

        // 获取TitledPane组件
        for (Node node : sidebarPane.getChildren()) {
            if (node instanceof TitledPane) {
                TitledPane titledPane = (TitledPane) node;
                VBox.setVgrow(titledPane, javafx.scene.layout.Priority.ALWAYS);

                // 设置内容区域
                if (titledPane.getContent() instanceof TreeView) {
                    TreeView<?> treeView = (TreeView<?>) titledPane.getContent();
                    VBox.setVgrow(treeView, javafx.scene.layout.Priority.ALWAYS);
                }
            }
        }
        
        // 设置树视图的点击事件
        projectTreeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<String> selectedItem = projectTreeView.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    String itemName = selectedItem.getValue();
                    
                    // 检查是否是"地表点沉降"项
                    if (itemName.equals("地表点沉降")) {
                        // 找到所属项目
                        TreeItem<String> parent = selectedItem.getParent();
                        if (parent != null && !parent.getValue().equals("工程列表")) {
                            // 使用项目名称打开沉降视图
                            openSubsidenceView(parent.getValue());
                        } else {
                            // 如果没有找到父项目，使用默认名称
                            openSubsidenceView("工程列表");
                        }
                    }
                    // 检查是否是"桩顶竖向位移"项
                    else if (itemName.equals("桩顶竖向位移")) {
                        // 找到所属项目
                        TreeItem<String> parent = selectedItem.getParent();
                        if (parent != null && !parent.getValue().equals("工程列表")) {
                            // 使用项目名称和测项名称打开位移视图
                            openMonitoringItemWithProject(parent.getValue(), itemName);
                        } else {
                            // 如果没有找到父项目，直接打开
                            openMonitoringItemTab(itemName);
                        }
                    }
                    // 检查是否是"立柱竖向位移"项
                    else if (itemName.equals("立柱竖向位移")) {
                        // 找到所属项目
                        TreeItem<String> parent = selectedItem.getParent();
                        if (parent != null && !parent.getValue().equals("工程列表")) {
                            // 使用项目名称和测项名称打开立柱竖向位移视图
                            openMonitoringItemWithProject(parent.getValue(), itemName);
                        } else {
                            // 如果没有找到父项目，直接打开
                            openMonitoringItemTab(itemName);
                        }
                    }
                    // 检查是否是"地下水位"项
                    else if (itemName.equals("地下水位")) {
                        // 找到所属项目
                        TreeItem<String> parent = selectedItem.getParent();
                        if (parent != null && !parent.getValue().equals("工程列表")) {
                            // 使用项目名称和测项名称打开地下水位视图
                            openMonitoringItemWithProject(parent.getValue(), itemName);
                        } else {
                            // 如果没有找到父项目，直接打开
                            openMonitoringItemTab(itemName);
                        }
                    }
                    // 检查是否是"建筑物沉降"项
                    else if (itemName.equals("建筑物沉降")) {
                        // 找到所属项目
                        TreeItem<String> parent = selectedItem.getParent();
                        if (parent != null && !parent.getValue().equals("工程列表")) {
                            // 使用项目名称和测项名称打开建筑物沉降视图
                            openMonitoringItemWithProject(parent.getValue(), itemName);
                        } else {
                            // 如果没有找到父项目，直接打开
                            openMonitoringItemTab(itemName);
                        }
                    }
                    // 检查是否是"钢支撑轴力"项
                    else if (itemName.equals("钢支撑轴力")) {
                        // 找到所属项目
                        TreeItem<String> parent = selectedItem.getParent();
                        if (parent != null && !parent.getValue().equals("工程列表")) {
                            // 使用项目名称和测项名称打开钢支撑轴力视图
                            openMonitoringItemWithProject(parent.getValue(), itemName);
                        } else {
                            // 如果没有找到父项目，直接打开
                            openMonitoringItemTab(itemName);
                        }
                    }
                    // 检查是否是"砼支撑轴力"项
                    else if (itemName.equals("砼支撑轴力")) {
                        // 找到所属项目
                        TreeItem<String> parent = selectedItem.getParent();
                        if (parent != null && !parent.getValue().equals("工程列表")) {
                            // 使用项目名称和测项名称打开砼支撑轴力视图
                            openMonitoringItemWithProject(parent.getValue(), itemName);
                        } else {
                            // 如果没有找到父项目，直接打开
                            openMonitoringItemTab(itemName);
                        }
                    }
                    // 检查是否是"深部水平位移"项
                    else if (itemName.equals("深部水平位移")) {
                        // 找到所属项目
                        TreeItem<String> parent = selectedItem.getParent();
                        if (parent != null && !parent.getValue().equals("工程列表")) {
                            // 使用项目名称和测项名称打开深部水平位移视图
                            openMonitoringItemWithProject(parent.getValue(), itemName);
                        } else {
                            // 如果没有找到父项目，直接打开
                            openMonitoringItemTab(itemName);
                        }
                    }
                    // 检查是否是"桩顶水平位移"项
                    else if (itemName.equals("桩顶水平位移")) {
                        // 找到所属项目
                        TreeItem<String> parent = selectedItem.getParent();
                        if (parent != null && !parent.getValue().equals("工程列表")) {
                            // 使用项目名称和测项名称打开桩顶水平位移视图
                            openMonitoringItemWithProject(parent.getValue(), itemName);
                        } else {
                            // 如果没有找到父项目，直接打开
                            openMonitoringItemTab(itemName);
                        }
                    }
                    // 检查是否是监测点
                    else if (itemName.contains("监测点")) {
                        // 找到所属项目
                        TreeItem<String> parent = selectedItem.getParent();
                        while (parent != null && parent.getParent() != null && !parent.getParent().getValue().equals("工程列表")) {
                            parent = parent.getParent();
                        }

                        if (parent != null) {
                            // 传递项目名称打开测项
                            openMonitoringItemWithProject(parent.getValue(), itemName);
                        } else {
                            // 没有找到父项目，直接打开
                        openMonitoringItemTab(itemName);
                        }
                    } else if (!itemName.equals("工程列表")) {
                        // 如果是项目节点，检查是否直接是工程列表的子节点
                        if (selectedItem.getParent() != null && selectedItem.getParent().getValue().equals("工程列表")) {
                        openProjectTab(itemName);
                        }
                    }
                }
            }
        });
        
        reportsTreeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<String> selectedItem = reportsTreeView.getSelectionModel().getSelectedItem();
                if (selectedItem != null && !selectedItem.getValue().equals("报表类型")) {
                    openReportTab(selectedItem.getValue());
                }
            }
        });
        
        overviewTreeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<String> selectedItem = overviewTreeView.getSelectionModel().getSelectedItem();
                if (selectedItem != null && !selectedItem.getValue().equals("监测概况")) {
                    // 检查是否是 "沉降监测" 项
                    if (selectedItem.getValue().equals("沉降监测")) {
                        openSubsidenceView("概览");
                    } else {
                    openOverviewTab(selectedItem.getValue());
                }
            }
            }
        });

        // 设置工程管理树的右键菜单
        projectTreeView.setOnContextMenuRequested(event -> {
            TreeItem<String> selectedItem = projectTreeView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                if (selectedItem.getValue().equals("工程列表")) {
                    // 为工程列表节点创建右键菜单
                    ContextMenu contextMenu = new ContextMenu();

                    // 添加"打开项目"菜单项
                    MenuItem openMenuItem = new MenuItem("打开项目");
                    openMenuItem.setOnAction(e -> handleOpenProject());

                    contextMenu.getItems().add(openMenuItem);
        
                    // 显示菜单
                    contextMenu.show(projectTreeView, event.getScreenX(), event.getScreenY());
                } else if (!selectedItem.getValue().equals("工程列表")) {
                    // 检查是否是项目节点（一级节点）
                    if (selectedItem.getParent() != null && selectedItem.getParent().getValue().equals("工程列表")) {
                        // 创建右键菜单
                        ContextMenu contextMenu = new ContextMenu();

                        // 添加测项菜单项
                        MenuItem addItemMenuItem = new MenuItem("添加测项");
                        addItemMenuItem.setOnAction(e -> handleAddMonitoringItem(selectedItem));
        
                        // 属性菜单项
                        MenuItem propertiesMenuItem = new MenuItem("属性");
                        propertiesMenuItem.setOnAction(e -> handleProjectProperties(selectedItem));
        
                        // 移除菜单项
                        MenuItem removeMenuItem = new MenuItem("移除");
                        removeMenuItem.setOnAction(e -> handleRemoveProject(selectedItem));

                        contextMenu.getItems().addAll(addItemMenuItem, propertiesMenuItem, removeMenuItem);

                        // 显示菜单
                        contextMenu.show(projectTreeView, event.getScreenX(), event.getScreenY());
                    }
                }
            }
        });
    }

    private void setupEditorArea() {
        // 设置标签页关闭按钮行为
        editorTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
    }

    /**
     * 创建示例数据的方法（已禁用）
     * 如需创建测试数据，可在需要时手动调用此方法
     */
    private void createSampleData() {
        // 已禁用自动创建测试数据
        // 如需测试，可在需要时手动调用此方法
    }
    
    private void openProjectTab(String projectName) {
        // 检查是否已经存在相同标题的标签页
        Tab existingTab = findTab(projectName);
        
        if (existingTab != null) {
            // 如果存在，则选中该标签页
            editorTabPane.getSelectionModel().select(existingTab);
        } else {
            // 如果不存在，则创建新的标签页
            Tab tab = new Tab(projectName);

            // 查找项目信息
            ProjectInfo projectInfo = findProjectByName(projectName);
            
            // 创建项目编辑内容
            BorderPane content = new BorderPane();

            if (projectInfo != null) {
                // 创建信息显示面板
                GridPane infoGrid = new GridPane();
                infoGrid.setHgap(10);
                infoGrid.setVgap(10);
                infoGrid.setPadding(new Insets(20));
                infoGrid.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 1;");

                // 标题
                Label titleLabel = new Label("项目详细信息");
                titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
                infoGrid.add(titleLabel, 0, 0, 2, 1);

                // 项目信息
                infoGrid.add(new Label("项目名称:"), 0, 1);
                infoGrid.add(new Label(projectInfo.getName()), 1, 1);

                infoGrid.add(new Label("监测单位:"), 0, 2);
                infoGrid.add(new Label(projectInfo.getOrganization()), 1, 2);

                infoGrid.add(new Label("监测负责人:"), 0, 3);
                infoGrid.add(new Label(projectInfo.getManager()), 1, 3);

                infoGrid.add(new Label("项目描述:"), 0, 4);
                Label descLabel = new Label(projectInfo.getDescription());
                descLabel.setWrapText(true);
                infoGrid.add(descLabel, 1, 4);

                // 测项列表
                infoGrid.add(new Label("监测测项:"), 0, 5);

                VBox itemsBox = new VBox(5);
                for (String item : projectInfo.getMonitoringItems()) {
                    HBox itemBox = new HBox(10);
                    itemBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    // 图标取决于测项类型
                    String iconPath = "/images/item_icon.png"; // 默认图标

                    switch (getItemType(item)) {
                        case "沉降":
                            iconPath = "/images/settlement_icon.png";
                            break;
                        case "位移":
                            iconPath = "/images/displacement_icon.png";
                            break;
                        case "倾斜":
                            iconPath = "/images/inclination_icon.png";
                            break;
                        case "水位":
                            iconPath = "/images/water_level_icon.png";
                            break;
                        case "应力":
                            iconPath = "/images/stress_icon.png";
                            break;
                    }

                    try {
                        ImageView icon = new ImageView(new javafx.scene.image.Image(getClass().getResourceAsStream(iconPath)));
                        icon.setFitHeight(16);
                        icon.setFitWidth(16);
                        itemBox.getChildren().add(icon);
                    } catch (Exception e) {
                        // 如果图标加载失败，使用文本标签代替
                        Label iconLabel = new Label("•");
                        itemBox.getChildren().add(iconLabel);
                    }

                    // 测项名称
                    Label itemLabel = new Label(item);
                    itemBox.getChildren().add(itemLabel);

                    // 如果此测项有数据，添加一个查看按钮
                    MonitoringItem monItem = monitoringItems.get(item);
                    if (monItem != null && monItem.getRecords() != null && !monItem.getRecords().isEmpty()) {
                        Button viewButton = new Button("查看");
                        viewButton.setStyle("-fx-font-size: 10;");
                        viewButton.setOnAction(event -> openMonitoringItemTab(item));
                        itemBox.getChildren().add(viewButton);
                    }

                    itemsBox.getChildren().add(itemBox);
                }

                infoGrid.add(itemsBox, 1, 5);

                // 项目操作按钮
                HBox buttonBox = new HBox(10);
                buttonBox.setPadding(new Insets(20, 0, 0, 0));
                buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

                Button editButton = new Button("编辑项目");
                editButton.getStyleClass().add("primary-button");
                editButton.setOnAction(event -> {
                    // 找到项目在树中的节点
                    TreeItem<String> projectRoot = projectTreeView.getRoot();
                    for (TreeItem<String> projectNode : projectRoot.getChildren()) {
                        if (projectNode.getValue().equals(projectName)) {
                            handleProjectProperties(projectNode);
                            // 更新标签页标题（如果项目名称已更改）
                            tab.setText(projectInfo.getName());
                            break;
                        }
                    }
                });

                Button dataImportButton = new Button("数据导入");
                dataImportButton.getStyleClass().add("secondary-button");
                dataImportButton.setOnAction(event -> {
                    // 实现数据导入功能
                    System.out.println("导入数据到项目：" + projectName);
                });

                Button generateReportButton = new Button("生成报表");
                generateReportButton.getStyleClass().add("secondary-button");
                generateReportButton.setOnAction(event -> {
                    // 实现报表生成功能
                    System.out.println("为项目生成报表：" + projectName);
                });

                buttonBox.getChildren().addAll(editButton, dataImportButton, generateReportButton);
                infoGrid.add(buttonBox, 0, 6, 2, 1);

                // 将信息面板放入内容区域
                content.setCenter(infoGrid);
            } else {
                // 如果找不到项目信息，显示简单消息
            Label label = new Label("项目：" + projectName + " 的详细信息");
            label.setStyle("-fx-font-size: 16; -fx-padding: 20;");
            content.setCenter(label);
            }
            
            tab.setContent(content);
            
            // 添加到标签页并选中
            editorTabPane.getTabs().add(tab);
            editorTabPane.getSelectionModel().select(tab);
        }
    }
    
    private void openMonitoringItemTab(String itemName) {
        // 检查该测项标签页是否已经打开
        Tab existingTab = findTab(itemName);
        if (existingTab != null) {
            editorTabPane.getSelectionModel().select(existingTab);
            return;
        }

        // 获取项目名和测项类型
        String[] parts = itemName.split(" - ");
        if (parts.length < 2) {
            System.err.println("测项名称格式不正确: " + itemName);
            return;
        }

        String projectName = parts[0];
        String itemType = getItemType(parts[1]);

        // 创建新标签页
            Tab tab = new Tab(itemName);
        tab.setClosable(true);

        // 根据测项类型设置不同的内容
        try {
            switch (itemType) {
                case "沉降":
                    // 加载沉降视图
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SettlementDataView.fxml"));
                    Parent root = loader.load();

                    // 获取控制器并设置数据
                    SettlementDataController controller = loader.getController();

                    // 如果存在相应的监测项，加载其数据
            MonitoringItem item = monitoringItems.get(itemName);
                    if (item != null && item.getRecords() != null && !item.getRecords().isEmpty()) {
                        controller.loadFromMeasurementRecords(item.getRecords());
                        System.out.println("已加载沉降监测数据: " + item.getRecords().size() + " 条记录");
                    }

                    // 保存控制器引用到节点的userData中（便于后续查找）
                    root.setUserData(controller);

                    tab.setContent(root);
                    break;
                case "位移":
                    // 加载桩顶位移视图
                    FXMLLoader displacementLoader = new FXMLLoader(getClass().getResource("/fxml/PileTopDisplacementView.fxml"));
                    Parent displacementRoot = displacementLoader.load();

                    // 获取控制器并设置数据
                    PileTopDisplacementController displacementController = displacementLoader.getController();
                    displacementController.setStage(getStage());

                    // 如果存在相应的监测项，加载其数据
                    MonitoringItem displacementItem = monitoringItems.get(itemName);
                    if (displacementItem != null) {
                        // 如果存在桩顶竖向位移数据存储对象，优先使用它
                        if (displacementItem.getPileDisplacementDataStorage() != null) {
                            displacementController.loadFromSettlementDataStorage(displacementItem.getPileDisplacementDataStorage());
                            System.out.println("已从桩顶竖向位移数据存储对象加载数据");
                        }
                        // 如果没有桩顶竖向位移数据存储对象，但存在测量记录，则使用测量记录
                        else if (displacementItem.getRecords() != null && !displacementItem.getRecords().isEmpty()) {
                            displacementController.loadFromMeasurementRecords(displacementItem.getRecords());
                            System.out.println("已加载桩顶位移监测数据: " + displacementItem.getRecords().size() + " 条记录");
                        }
                    }

                    // 保存控制器引用到节点的userData中（便于后续查找）
                    displacementRoot.setUserData(displacementController);

                    tab.setContent(displacementRoot);
                    break;
                case "钢支撑轴力":
                    // 加载钢支撑轴力视图
                    FXMLLoader axialForceLoader = new FXMLLoader(getClass().getResource("/fxml/SteelSupportAxialForceView.fxml"));
                    Parent axialForceRoot = axialForceLoader.load();

                    // 获取控制器并设置数据
                    SteelSupportAxialForceController axialForceController = axialForceLoader.getController();
                    axialForceController.setStage(getStage());

                    // 如果存在相应的监测项，加载其数据
                    MonitoringItem axialForceItem = monitoringItems.get(itemName);
                    if (axialForceItem != null) {
                        // 如果存在钢支撑轴力数据存储对象，优先使用它
                        if (axialForceItem.getSteelSupportAxialForceDataStorage() != null) {
                            axialForceController.loadFromSteelSupportAxialForceDataStorage(axialForceItem.getSteelSupportAxialForceDataStorage());
                            System.out.println("已从钢支撑轴力数据存储对象加载数据");
                        }
                        // 如果没有钢支撑轴力数据存储对象，但存在测量记录，则使用测量记录
                        else if (axialForceItem.getRecords() != null && !axialForceItem.getRecords().isEmpty()) {
                            axialForceController.loadFromMeasurementRecords(axialForceItem.getRecords());
                            System.out.println("已加载钢支撑轴力监测数据: " + axialForceItem.getRecords().size() + " 条记录");
                        }
                    }

                    // 保存控制器引用到节点的userData中（便于后续查找）
                    axialForceRoot.setUserData(axialForceController);

                    tab.setContent(axialForceRoot);
                    break;
                case "立柱竖向位移":
                    // 加载立柱竖向位移视图
                    FXMLLoader columnLoader = new FXMLLoader(getClass().getResource("/fxml/ColumnDisplacementView.fxml"));
                    Parent columnRoot = columnLoader.load();

                    // 获取控制器并设置数据
                    ColumnDisplacementController columnController = columnLoader.getController();
                    columnController.setStage(getStage());

                    // 如果存在相应的监测项，加载其数据
                    MonitoringItem columnItem = monitoringItems.get(itemName);
                    if (columnItem != null) {
                        // 如果存在立柱竖向位移数据存储对象，优先使用它
                        if (columnItem.getColumnDisplacementDataStorage() != null) {
                            columnController.loadFromDataStorage(columnItem.getColumnDisplacementDataStorage());
                            System.out.println("已从立柱竖向位移数据存储对象加载数据");
                        }
                        // 如果没有立柱竖向位移数据存储对象，但存在测量记录，则使用测量记录
                        else if (columnItem.getRecords() != null && !columnItem.getRecords().isEmpty()) {
                            // 转换测量记录为立柱位移数据并加载
                            List<ColumnDisplacementData> displacementDataList = convertMeasurementRecordsToColumnDisplacementData(columnItem.getRecords());
                            LocalDateTime importTime = LocalDateTime.now();
                            columnController.getColumnDisplacementDataStorage().addDataBlock(importTime, displacementDataList, 
                                "导入于 " + importTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                            columnController.loadFromDataStorage(columnController.getColumnDisplacementDataStorage());
                            System.out.println("已加载立柱竖向位移监测数据: " + columnItem.getRecords().size() + " 条记录");
                        }
                    }

                    // 保存控制器引用到节点的userData中（便于后续查找）
                    columnRoot.setUserData(columnController);

                    tab.setContent(columnRoot);
                    break;
                case "深部水平位移":
                    // 加载深部水平位移视图
                    FXMLLoader deepHorizontalLoader = new FXMLLoader(getClass().getResource("/fxml/DeepHorizontalDisplacementView.fxml"));
                    Parent deepHorizontalRoot = deepHorizontalLoader.load();

                    // 获取控制器并设置数据
                    DeepHorizontalDisplacementController deepHorizontalController = deepHorizontalLoader.getController();
                    deepHorizontalController.setStage(getStage());

                    // 如果存在相应的监测项，加载其数据
                    MonitoringItem deepHorizontalItem = monitoringItems.get(itemName);
                    if (deepHorizontalItem != null) {
                        // 如果存在深部水平位移数据存储对象，优先使用它
                        if (deepHorizontalItem.getDeepHorizontalDisplacementDataStorage() != null) {
                            deepHorizontalController.loadFromDeepHorizontalDisplacementDataStorage(deepHorizontalItem.getDeepHorizontalDisplacementDataStorage());
                            System.out.println("已从深部水平位移数据存储对象加载数据");
                        }
                        // 如果没有深部水平位移数据存储对象，但存在测量记录，则使用测量记录
                        else if (deepHorizontalItem.getRecords() != null && !deepHorizontalItem.getRecords().isEmpty()) {
                            deepHorizontalController.loadFromMeasurementRecords(deepHorizontalItem.getRecords());
                            System.out.println("已加载深部水平位移监测数据: " + deepHorizontalItem.getRecords().size() + " 条记录");
                        }
                    }

                    // 保存控制器引用到节点的userData中（便于后续查找）
                    deepHorizontalRoot.setUserData(deepHorizontalController);

                    tab.setContent(deepHorizontalRoot);
                    break;
                case "桩顶水平位移":
                    try {
                        // 加载桩顶水平位移视图
                        FXMLLoader horizontalLoader = new FXMLLoader(getClass().getResource("/fxml/PileTopHorizontalDisplacementView.fxml"));
                        Parent horizontalRoot = horizontalLoader.load();
                        
                        // 获取控制器并设置数据
                        PileTopHorizontalDisplacementController horizontalController = horizontalLoader.getController();
                        horizontalController.setStage(getStage());
                        
                        MonitoringItem horizontalItem = monitoringItems.get(itemName);
                        if (horizontalItem != null) {
                            if (horizontalItem.getPileTopHorizontalDisplacementDataStorage() != null) {
                                horizontalController.loadFromPileTopHorizontalDisplacementDataStorage(horizontalItem.getPileTopHorizontalDisplacementDataStorage());
                            } else if (horizontalItem.getRecords() != null && !horizontalItem.getRecords().isEmpty()) {
                                horizontalController.loadFromMeasurementRecords(horizontalItem.getRecords());
                            }
                        }
                        
                        // 设置控制器作为用户数据
                        horizontalRoot.setUserData(horizontalController);
                        
                        tab.setContent(horizontalRoot);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Label errorLabel = new Label("加载桩顶水平位移模块时出错：" + e.getMessage());
                        errorLabel.setWrapText(true);
                        errorLabel.setPadding(new Insets(10));
                        tab.setContent(errorLabel);
                    }
                    break;
                default:
                    // 默认显示
                    Label defaultLabel = new Label(itemName + " - 监测模块 - 正在开发中");
                    defaultLabel.setPadding(new Insets(20));
                    tab.setContent(defaultLabel);
                    break;
            }
        } catch (IOException e) {
            System.err.println("无法加载测项视图: " + e.getMessage());
            e.printStackTrace();

            // 创建错误提示
            Label errorLabel = new Label("加载模块时出错: " + e.getMessage());
            errorLabel.setPadding(new Insets(20));
            tab.setContent(errorLabel);
        }

        // 添加标签页并选中
            editorTabPane.getTabs().add(tab);
            editorTabPane.getSelectionModel().select(tab);
    }
    
    /**
     * 从监测点名称推断类型
     */
    private String getItemType(String itemName) {
        if (itemName.contains("沉降") || itemName.contains("地表点")) {
            return "沉降";
        } else if (itemName.contains("桩顶水平位移")) {
            return "桩顶水平位移";
        } else if (itemName.contains("深部水平位移")) {
            return "深部水平位移";
        } else if (itemName.contains("位移") || itemName.contains("桩顶") || itemName.contains("立柱")) {
            return "位移";
        } else if (itemName.contains("水位")) {
            return "水位";
        } else if (itemName.contains("倾斜")) {
            return "倾斜";
        } else if (itemName.contains("应力")) {
            return "应力";
        } else if (itemName.contains("建筑物")) {
            return "沉降";
        } else if (itemName.contains("钢支撑轴力")) {
            return "钢支撑轴力";
        } else if (itemName.contains("砼支撑轴力")) {
            return "砼支撑轴力";
        } else {
        return "其他";
        }
    }
    
    private void openReportTab(String reportName) {
        // 检查是否已经存在相同标题的标签页
        Tab existingTab = findTab(reportName);
        
        if (existingTab != null) {
            // 如果存在，则选中该标签页
            editorTabPane.getSelectionModel().select(existingTab);
        } else {
            // 如果不存在，则创建新的标签页
            Tab tab = new Tab(reportName);
            
            // 创建报表编辑内容
            BorderPane content = new BorderPane();
            Label label = new Label("报表：" + reportName + " 的生成界面");
            label.setStyle("-fx-font-size: 16; -fx-padding: 20;");
            content.setCenter(label);
            
            tab.setContent(content);
            
            // 添加到标签页并选中
            editorTabPane.getTabs().add(tab);
            editorTabPane.getSelectionModel().select(tab);
        }
    }
    
    private void openOverviewTab(String overviewName) {
        // 检查是否已经存在相同标题的标签页
        Tab existingTab = findTab(overviewName);
        
        if (existingTab != null) {
            // 如果已存在，选中该标签页
            editorTabPane.getSelectionModel().select(existingTab);
        } else {
            // 根据概览类别打开不同的视图
            switch (overviewName) {
                case "沉降监测":
                    // 打开沉降监测视图，使用"概览"作为项目名
                    final String projectName = "概览";
                    openSubsidenceView(projectName);
                    break;
                case "项目统计":
                    openProjectStatisticsView();
                    break;
                default:
                    showUndevelopedFeatureMessage(overviewName);
                    break;
            }
        }
    }
    
    private Tab findTab(String title) {
        ObservableList<Tab> tabs = editorTabPane.getTabs();
        for (Tab tab : tabs) {
            if (tab.getText().equals(title)) {
                return tab;
            }
        }
        return null;
    }

    private void setupWindowControls() {
        // 设置拖拽功能
        titleLabel.setOnMousePressed(this::handleMousePressed);
        titleLabel.setOnMouseDragged(this::handleMouseDragged);
        appIcon.setOnMousePressed(this::handleMousePressed);
        appIcon.setOnMouseDragged(this::handleMouseDragged);
        
        // 设置窗口控制按钮
        minimizeButton.setOnAction(event -> minimizeWindow());
        maximizeButton.setOnAction(event -> maximizeOrRestoreWindow());
        closeButton.setOnAction(event -> closeWindow());
        
        // 双击标题栏最大化/还原窗口
        titleLabel.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                maximizeOrRestoreWindow();
            }
        });
    }
    
    private void setupResizeHandling() {
        // 设置窗口边缘调整大小功能
        mainPane.setOnMouseMoved(this::updateCursor);
        mainPane.setOnMousePressed(this::handleResizeStart);
        mainPane.setOnMouseDragged(this::handleResize);
        mainPane.setOnMouseReleased(event -> resetResizeFlags());
    }
    
    private void updateCursor(MouseEvent event) {
        if (isMaximized) {
            mainPane.setCursor(Cursor.DEFAULT);
            return;
        }
        
        double mouseX = event.getSceneX();
        double mouseY = event.getSceneY();
        double width = mainPane.getWidth();
        double height = mainPane.getHeight();
        
        // 检测鼠标是否在窗口边缘
        boolean left = mouseX < RESIZE_PADDING;
        boolean right = mouseX > width - RESIZE_PADDING;
        boolean top = mouseY < RESIZE_PADDING;
        boolean bottom = mouseY > height - RESIZE_PADDING;
        
        // 根据鼠标位置设置光标样式
        if (top && left) {
            mainPane.setCursor(Cursor.NW_RESIZE);
        } else if (top && right) {
            mainPane.setCursor(Cursor.NE_RESIZE);
        } else if (bottom && left) {
            mainPane.setCursor(Cursor.SW_RESIZE);
        } else if (bottom && right) {
            mainPane.setCursor(Cursor.SE_RESIZE);
        } else if (left) {
            mainPane.setCursor(Cursor.W_RESIZE);
        } else if (right) {
            mainPane.setCursor(Cursor.E_RESIZE);
        } else if (top) {
            mainPane.setCursor(Cursor.N_RESIZE);
        } else if (bottom) {
            mainPane.setCursor(Cursor.S_RESIZE);
        } else {
            mainPane.setCursor(Cursor.DEFAULT);
        }
    }
    
    private void handleResizeStart(MouseEvent event) {
        if (isMaximized) return;
        
        double mouseX = event.getSceneX();
        double mouseY = event.getSceneY();
        double width = mainPane.getWidth();
        double height = mainPane.getHeight();
        
        // 检测鼠标是否在窗口边缘
        resizeLeft = mouseX < RESIZE_PADDING;
        resizeRight = mouseX > width - RESIZE_PADDING;
        resizeTop = mouseY < RESIZE_PADDING;
        resizeBottom = mouseY > height - RESIZE_PADDING;
        
        if (resizeLeft || resizeRight || resizeTop || resizeBottom) {
            Stage stage = getStage();
            startX = stage.getX();
            startY = stage.getY();
            startWidth = stage.getWidth();
            startHeight = stage.getHeight();
            event.consume();
        }
    }
    
    private void handleResize(MouseEvent event) {
        if (isMaximized) return;
        
        if (resizeLeft || resizeRight || resizeTop || resizeBottom) {
            Stage stage = getStage();
            double mouseX = event.getScreenX();
            double mouseY = event.getScreenY();
            
            // 调整宽度和位置
            if (resizeLeft) {
                double newWidth = startWidth + (startX - mouseX);
                if (newWidth > stage.getMinWidth()) {
                    stage.setWidth(newWidth);
                    stage.setX(mouseX);
                }
            } else if (resizeRight) {
                double newWidth = mouseX - startX;
                if (newWidth > stage.getMinWidth()) {
                    stage.setWidth(newWidth);
                }
            }
            
            // 调整高度和位置
            if (resizeTop) {
                double newHeight = startHeight + (startY - mouseY);
                if (newHeight > stage.getMinHeight()) {
                    stage.setHeight(newHeight);
                    stage.setY(mouseY);
                }
            } else if (resizeBottom) {
                double newHeight = mouseY - startY;
                if (newHeight > stage.getMinHeight()) {
                    stage.setHeight(newHeight);
                }
            }
            
            event.consume();
        }
    }
    
    private void resetResizeFlags() {
        resizeLeft = false;
        resizeRight = false;
        resizeTop = false;
        resizeBottom = false;
        mainPane.setCursor(Cursor.DEFAULT);
    }

    private void handleMousePressed(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    private void handleMouseDragged(MouseEvent event) {
        if (isMaximized) {
            return; // 如果窗口已最大化，不允许拖拽
        }
        
        Stage stage = getStage();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    private void minimizeWindow() {
        getStage().setIconified(true);
    }

    private void maximizeOrRestoreWindow() {
        Stage stage = getStage();
        isMaximized = !isMaximized;
        
        if (isMaximized) {
            // 记住窗口恢复大小前的大小和位置
            double screenWidth = screenBounds != null ? screenBounds.getWidth() : Screen.getPrimary().getVisualBounds().getWidth();
            double screenHeight = screenBounds != null ? screenBounds.getHeight() : Screen.getPrimary().getVisualBounds().getHeight();
            
            // 设置窗口大小为屏幕可视区域大小（不覆盖任务栏）
            stage.setX(0);
            stage.setY(0);
            stage.setWidth(screenWidth);
            stage.setHeight(screenHeight);
            
            maximizeButton.setText("❐");
        } else {
            // 恢复到默认大小
            stage.setWidth(800);
            stage.setHeight(600);
            stage.centerOnScreen();
            maximizeButton.setText("□");
        }
    }

    private void closeWindow() {
        // 检查是否有未保存的项目
        List<ProjectInfo> unsavedProjects = new ArrayList<>();
        for (ProjectInfo project : projects.values()) {
            if (hasUnsavedChanges(project)) {
                unsavedProjects.add(project);
            }
        }

        if (!unsavedProjects.isEmpty()) {
            // 如果有未保存的项目，提示用户是否保存
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("未保存的更改");
            alert.setHeaderText("有" + unsavedProjects.size() + "个项目包含未保存的更改");

            StringBuilder content = new StringBuilder("是否在退出前保存以下项目？\n");
            for (ProjectInfo project : unsavedProjects) {
                content.append("- ").append(project.getName()).append("\n");
            }
            alert.setContentText(content.toString());

            ButtonType saveButtonType = new ButtonType("保存并退出");
            ButtonType exitButtonType = new ButtonType("直接退出");
            ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(saveButtonType, exitButtonType, cancelButtonType);

            alert.showAndWait().ifPresent(response -> {
                if (response == saveButtonType) {
                    // 保存所有未保存的项目
                    for (ProjectInfo project : unsavedProjects) {
                        saveProject(project);
                    }
                    getStage().close();
                } else if (response == exitButtonType) {
                    // 直接退出
        getStage().close();
                }
                // 如果是取消，不做任何操作
            });
        } else {
            // 如果没有未保存的项目，直接退出
            getStage().close();
        }
    }

    private Stage getStage() {
        return (Stage) mainPane.getScene().getWindow();
    }

    private void handleNewProject() {
        System.out.println("新建项目...");

        // 创建并显示项目对话框
        ProjectDialog dialog = new ProjectDialog();
        dialog.setHeaderText("请输入新项目的详细信息");

        // 显示对话框并等待用户响应
        dialog.showAndWait().ifPresent(projectInfo -> {
            // 生成唯一ID
            String projectId = "P" + System.currentTimeMillis();
            projectInfo.setId(projectId);

            // 保存项目信息
            projects.put(projectId, projectInfo);

            // 创建项目节点
            TreeItem<String> projectNode = new TreeItem<>(projectInfo.getName());
            projectNode.setExpanded(true);

            // 创建测项子节点
            for (String itemName : projectInfo.getMonitoringItems()) {
                // 为每个测项创建子节点
                TreeItem<String> itemNode = new TreeItem<>(itemName);
                projectNode.getChildren().add(itemNode);

                // 创建相应的监测项对象
                MonitoringItem item = new MonitoringItem(
                        itemName.replaceAll("\\s+", ""),  // 移除空格作为ID
                        itemName,  // 名称
                        getItemType(itemName),  // 类型
                        projectInfo.getName() + " - " + itemName  // 位置
                );
                monitoringItems.put(getFullItemName(projectInfo.getName(), itemName), item);
            }

            // 将项目添加到工程树中
            TreeItem<String> projectRoot = projectTreeView.getRoot();
            projectRoot.getChildren().add(projectNode);

            // 获取用户在对话框中选择的文件
            File selectedFile = projectInfo.getProjectFile();

            // 保存项目到文件
            boolean success = ProjectFileUtil.saveProject(projectInfo, monitoringItems, selectedFile);

            if (success) {
                // 确保项目文件路径被正确设置
                projectInfo.setProjectFile(selectedFile);
            }

            if (success) {
                // 提示保存成功
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("项目创建成功");
                alert.setHeaderText(null);
                alert.setContentText("项目 " + projectInfo.getName() + " 已成功创建并保存到文件：\n" + selectedFile.getAbsolutePath());
                alert.showAndWait();

                // 自动打开项目标签页
                openProjectTab(projectInfo.getName());
            } else {
                // 提示保存失败
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("保存失败");
                alert.setHeaderText("无法保存项目");
                alert.setContentText("保存项目 " + projectInfo.getName() + " 到文件时出错。");
                alert.showAndWait();
            }
        });
    }

    private void handleOpenProject() {
        System.out.println("打开项目...");

        // 创建文件选择器
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("打开工程监测项目");
        fileChooser.getExtensionFilters().add(
            new ExtensionFilter("工程监测项目文件", "*.jc")
        );

        // 显示打开文件对话框
        File selectedFile = fileChooser.showOpenDialog(getStage());

        if (selectedFile != null) {
            // 从文件加载项目
            ProjectFileData data = ProjectFileUtil.loadProject(selectedFile);

            if (data != null && data.projectInfo != null) {
                // 检查是否已存在同名项目
                boolean exists = false;
                String projectName = data.projectInfo.getName();

                for (ProjectInfo existingProject : projects.values()) {
                    if (existingProject.getName().equals(projectName)) {
                        exists = true;
                        break;
                    }
                }

                if (exists) {
                    // 显示项目已存在的提示
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("项目已存在");
                    alert.setHeaderText("已存在同名项目: " + projectName);
                    alert.setContentText("是否要替换现有项目？");

                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            // 用户同意替换，执行加载
                            loadProjectData(data, selectedFile);
                        }
                    });
                } else {
                    // 直接加载项目
                    loadProjectData(data, selectedFile);
                }
            } else {
                // 加载失败，显示错误信息
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("加载失败");
                alert.setHeaderText("无法加载项目文件");
                alert.setContentText("所选文件可能已损坏或不是有效的项目文件。");
                alert.showAndWait();
            }
        }
    }

    /**
     * 加载项目数据到程序中
     */
    private void loadProjectData(ProjectFileData data, File file) {
        // 生成项目ID（如果没有）
        if (data.projectInfo.getId() == null || data.projectInfo.getId().isEmpty()) {
            data.projectInfo.setId("P" + System.currentTimeMillis());
        }

        // 更新项目文件引用
        data.projectInfo.setProjectFile(file);

        // 保存项目信息
        projects.put(data.projectInfo.getId(), data.projectInfo);

        // 将监测项添加到数据存储
        if (data.monitoringItems != null) {
            monitoringItems.putAll(data.monitoringItems);
            
            // 检查是否有深部水平位移数据
            for (Map.Entry<String, MonitoringItem> entry : data.monitoringItems.entrySet()) {
                if (entry.getKey().contains("深部水平位移")) {
                    MonitoringItem item = entry.getValue();
                    if (item.getDeepHorizontalDisplacementDataStorage() != null) {
                        DeepHorizontalDisplacementDataStorage storage = item.getDeepHorizontalDisplacementDataStorage();
                        System.out.println("已加载深部水平位移数据: 测点数量=" + 
                                (storage.getPoints() != null ? storage.getPoints().size() : 0) + 
                                ", 数据块数量=" + storage.getDataBlockCount());
                    } else {
                        System.out.println("警告: 深部水平位移项目没有关联的数据存储对象");
                    }
                }
            }
        }

        // 创建项目节点并添加到树中
        TreeItem<String> projectNode = new TreeItem<>(data.projectInfo.getName());
        projectNode.setExpanded(true);

        // 为项目中的每个测项创建子节点
        for (String itemName : data.projectInfo.getMonitoringItems()) {
            TreeItem<String> itemNode = new TreeItem<>(itemName);
            projectNode.getChildren().add(itemNode);
        }

        // 添加到工程树
        TreeItem<String> projectRoot = projectTreeView.getRoot();
        projectRoot.getChildren().add(projectNode);

        // 提示加载成功
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("加载成功");
        alert.setHeaderText(null);
        alert.setContentText("项目 " + data.projectInfo.getName() + " 已成功加载\n文件路径：" + file.getAbsolutePath());
        alert.showAndWait();

        // 打开项目标签页
        openProjectTab(data.projectInfo.getName());

        System.out.println("项目 " + data.projectInfo.getName() + " 加载成功");
    }

    /**
     * 检查项目是否有未保存的更改
     * @param project 要检查的项目
     * @return 如果有未保存的更改返回true
     */
    private boolean hasUnsavedChanges(ProjectInfo project) {
        // 如果项目没有关联文件，则认为有未保存的更改
        if (project.getProjectFile() == null) {
            return true;
        }

        // 如果文件不存在，则认为有未保存的更改
        if (!project.getProjectFile().exists()) {
            return true;
        }

        // 检查是否有打开的标签页包含该项目的数据
        for (Tab tab : editorTabPane.getTabs()) {
            if (tab.getText().contains(project.getName()) && 
                (tab.getText().contains("地表点沉降") || 
                 tab.getText().contains("桩顶竖向位移") || 
                 tab.getText().contains("立柱竖向位移"))) {
                // 如果有打开的监测数据标签页，则认为可能有未保存的更改
                return true;
            }
        }

        return false;
    }

    /**
     * 保存所有项目数据
     * @param projectToSave 要保存的项目
     */
    private void saveAllProjectData(ProjectInfo projectToSave) {
        // 首先从打开的标签页收集数据
        collectSettlementDataFromOpenTabs(projectToSave);

        // 然后确保所有监测项都正确关联到项目
        String projectName = projectToSave.getName();
        for (String itemName : projectToSave.getMonitoringItems()) {
            String fullItemName = getFullItemName(projectName, itemName);
            MonitoringItem item = monitoringItems.get(fullItemName);
            
            if (item == null) {
                // 如果不存在，创建新的监测项
                item = new MonitoringItem(
                    itemName.replaceAll("\\s+", ""),  // 移除空格作为ID
                    itemName,  // 名称
                    getItemType(itemName),  // 类型
                    projectName + " - " + itemName  // 位置
                );
                monitoringItems.put(fullItemName, item);
            }
            
            // 特别处理深部水平位移数据 - 即使标签页未打开，也确保数据存储对象被正确保存
            if (itemName.equals("深部水平位移")) {
                if (item.getDeepHorizontalDisplacementDataStorage() != null) {
                    // 已经有数据存储对象，确保它被保留
                    System.out.println("确保保存已有的深部水平位移数据: " + item.getName());
                    
                    // 确保也保存了DataStorage中的测点配置和数据块
                    DeepHorizontalDisplacementDataStorage storage = item.getDeepHorizontalDisplacementDataStorage();
                    if (storage.getDataBlockCount() > 0) {
                        System.out.println("保存深部水平位移数据块数量: " + storage.getDataBlockCount());
                    }
                } else {
                    // 如果没有数据存储对象，创建一个空的
                    System.out.println("为深部水平位移创建新的数据存储对象");
                    item.setDeepHorizontalDisplacementDataStorage(new DeepHorizontalDisplacementDataStorage());
                }
            }
        }
    }

    private void handleSaveProject() {
        System.out.println("保存项目...");

        // 获取当前选中的标签页
        Tab selectedTab = editorTabPane.getSelectionModel().getSelectedItem();

        // 如果没有选中标签页，或者选中的不是项目标签页，则提示用户先选择一个项目
        if (selectedTab == null) {
            showSelectProjectDialog();
            return;
        }

        // 尝试找到对应的项目
        ProjectInfo projectToSave = null;
        String projectName = selectedTab.getText();

        // 检查是否是已知项目名称
        projectToSave = findProjectByName(projectName);

        // 如果没有找到，可能标签页名称包含了测项名称，尝试从树中查找
        if (projectToSave == null) {
            TreeItem<String> projectRoot = projectTreeView.getRoot();
            for (TreeItem<String> projectNode : projectRoot.getChildren()) {
                // 检查是否是此项目的测项标签页
                String currentProjectName = projectNode.getValue();
                if (projectName.startsWith(currentProjectName + " - ")) {
                    projectToSave = findProjectByName(currentProjectName);
                    break;
                }
            }
        }

        // 如果仍未找到，提示用户选择项目
        if (projectToSave == null) {
            showSelectProjectDialog();
            return;
        }

        // 在保存前，收集所有项目数据
        saveAllProjectData(projectToSave);

        // 检查项目是否已经有关联的文件
        File selectedFile = null;
        if (projectToSave.getProjectFile() != null && projectToSave.getProjectFile().exists()) {
            // 使用已有的文件
            selectedFile = projectToSave.getProjectFile();
            System.out.println("自动保存项目到: " + selectedFile.getAbsolutePath());
        } else {
            // 创建文件选择器
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("保存工程监测项目");
            fileChooser.getExtensionFilters().add(
                new ExtensionFilter("工程监测项目文件", "*.jc")
            );
            fileChooser.setInitialFileName(projectToSave.getName() + ".jc");

            // 显示保存文件对话框
            selectedFile = fileChooser.showSaveDialog(getStage());
        }

        if (selectedFile != null) {
            // 保存项目到文件
            boolean success = ProjectFileUtil.saveProject(projectToSave, monitoringItems, selectedFile);

            if (success) {
                // 更新项目文件路径
                projectToSave.setProjectFile(selectedFile);

                // 提示保存成功
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("保存成功");
                alert.setHeaderText(null);
                alert.setContentText("项目 " + projectToSave.getName() + " 已成功保存到文件：\n" + selectedFile.getAbsolutePath());
                alert.showAndWait();
            } else {
                // 提示保存失败
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("保存失败");
                alert.setHeaderText("无法保存项目");
                alert.setContentText("保存项目 " + projectToSave.getName() + " 到文件时出错。");
                alert.showAndWait();
            }
        }
    }

    /**
     * 从所有打开的沉降数据标签页中收集数据，并存储到相应的监测项中
     *
     * @param project 当前项目
     */
    private void collectSettlementDataFromOpenTabs(ProjectInfo project) {
        for (Tab tab : editorTabPane.getTabs()) {
            String tabTitle = tab.getText();
            
            // 只处理当前项目的标签页
            if (tabTitle.startsWith(project.getName() + " - ")) {
                // 根据标签内容提取控制器并收集数据
                Parent tabContent = (Parent) tab.getContent();
                if (tabContent != null) {
                    findAndCollectFromController(tabContent, project);
                }
            }
        }
    }

    private void findAndCollectFromController(Parent parent, ProjectInfo project) {
                        // 尝试从userData获取控制器
        Object userData = parent.getUserData();

        // 如果userData是控制器，直接处理
        if (userData instanceof SettlementDataController) {
            SettlementDataController controller = (SettlementDataController) userData;
            
            // 检查是否有更改
            if (controller.hasDataChanged()) {
                // 获取所有测量记录
                List<MeasurementRecord> records = controller.getMeasurementRecordsForSaving();
                
                // 获取或创建监测项
                String itemName = "地表点沉降";
                String fullItemName = getFullItemName(project.getName(), itemName);
                MonitoringItem item = monitoringItems.get(fullItemName);
                
                if (item == null) {
                    item = new MonitoringItem();
                    item.setId(UUID.randomUUID().toString());
                    item.setName(itemName);
                    item.setType("沉降");
                    item.setLocation(project.getName() + " - " + itemName);
                    monitoringItems.put(fullItemName, item);
                            }

                // 获取数据存储对象
                SettlementDataStorage storage = controller.getSettlementDataStorage();
                
                // 更新监测项数据
                item.setRecords(records);
                item.setSettlementDataStorage(storage);
                
                // 添加到项目中
                project.addMonitoringItem(itemName);
                
                System.out.println("已收集沉降监测数据: " + records.size() + " 条记录");
                        } else {
                System.out.println("沉降监测数据未更改，不需要保存");
            }
        }
        // 处理PileTopDisplacementController
        else if (userData instanceof PileTopDisplacementController) {
            PileTopDisplacementController controller = (PileTopDisplacementController) userData;
            
            // 收集桩顶位移数据...
            if (controller.hasDataChanged()) {
                List<MeasurementRecord> records = controller.getMeasurementRecordsForSaving();
                
                String itemName = "桩顶竖向位移";
                String fullItemName = getFullItemName(project.getName(), itemName);
                MonitoringItem item = monitoringItems.get(fullItemName);

                if (item == null) {
                    item = new MonitoringItem();
                    item.setId(UUID.randomUUID().toString());
                    item.setName(itemName);
                    item.setType("位移");
                    item.setLocation(project.getName() + " - " + itemName);
                    monitoringItems.put(fullItemName, item);
                }
                
                // 获取数据存储对象
                PileDisplacementDataStorage storage = controller.getSettlementDataStorage();
                
                // 更新监测项数据
                item.setRecords(records);
                item.setPileDisplacementDataStorage(storage);
                
                // 添加到项目中
                project.addMonitoringItem(itemName);
                
                System.out.println("已收集桩顶位移监测数据: " + records.size() + " 条记录");
            }
        }
        // 处理ColumnDisplacementController
        else if (userData instanceof ColumnDisplacementController) {
            ColumnDisplacementController controller = (ColumnDisplacementController) userData;

            // 收集立柱位移数据
            List<MeasurementRecord> records = controller.getMeasurementRecordsForSaving();
            
            String itemName = "立柱竖向位移";
            String fullItemName = getFullItemName(project.getName(), itemName);
            MonitoringItem item = monitoringItems.get(fullItemName);
            
            if (item == null) {
                item = new MonitoringItem();
                item.setId(UUID.randomUUID().toString());
                item.setName(itemName);
                item.setType("位移");
                item.setLocation(project.getName() + " - " + itemName);
                monitoringItems.put(fullItemName, item);
                            }

            // 获取数据存储对象
            ColumnDisplacementDataStorage storage = controller.getColumnDisplacementDataStorage();
            
            // 更新监测项数据
            item.setRecords(records);
            item.setColumnDisplacementDataStorage(storage);
            
            // 添加到项目中
            project.addMonitoringItem(itemName);
            
            System.out.println("已收集立柱位移监测数据: " + records.size() + " 条记录");
        }
        // 处理GroundwaterLevelController
        else if (userData instanceof GroundwaterLevelController) {
            GroundwaterLevelController controller = (GroundwaterLevelController) userData;
            
            // 收集地下水位数据
                    List<MeasurementRecord> records = controller.getMeasurementRecordsForSaving();
            
            String itemName = "地下水位";
            String fullItemName = getFullItemName(project.getName(), itemName);
            MonitoringItem item = monitoringItems.get(fullItemName);
            
            if (item == null) {
                item = new MonitoringItem();
                item.setId(UUID.randomUUID().toString());
                item.setName(itemName);
                item.setType("水位");
                item.setLocation(project.getName() + " - " + itemName);
                monitoringItems.put(fullItemName, item);
                    }

            // 获取数据存储对象
            GroundwaterLevelDataStorage storage = controller.getGroundwaterLevelDataStorage();
            
            // 更新监测项数据
            item.setRecords(records);
            item.setGroundwaterLevelDataStorage(storage);
            
            // 添加到项目中
            project.addMonitoringItem(itemName);
            
            System.out.println("已收集地下水位监测数据: " + records.size() + " 条记录");
                }
        // 处理BuildingSettlementController
        else if (userData instanceof BuildingSettlementController) {
            BuildingSettlementController controller = (BuildingSettlementController) userData;
            
            // 收集建筑物沉降数据
                    List<MeasurementRecord> records = controller.getMeasurementRecordsForSaving();
            
            String itemName = "建筑物沉降";
            String fullItemName = getFullItemName(project.getName(), itemName);
            MonitoringItem item = monitoringItems.get(fullItemName);
            
            if (item == null) {
                item = new MonitoringItem();
                item.setId(UUID.randomUUID().toString());
                item.setName(itemName);
                item.setType("沉降");
                item.setLocation(project.getName() + " - " + itemName);
                monitoringItems.put(fullItemName, item);
                    }

            // 获取数据存储对象
            BuildingSettlementDataStorage storage = controller.getBuildingSettlementDataStorage();
            
            // 更新监测项数据
            item.setRecords(records);
            item.setBuildingSettlementDataStorage(storage);
            
            // 添加到项目中
            project.addMonitoringItem(itemName);
            
            System.out.println("已收集建筑物沉降监测数据: " + records.size() + " 条记录");
                }
        // 处理SteelSupportAxialForceController
        else if (userData instanceof SteelSupportAxialForceController) {
            SteelSupportAxialForceController controller = (SteelSupportAxialForceController) userData;
            
            // 收集钢支撑轴力数据
            List<MeasurementRecord> records = controller.getMeasurementRecordsForSaving();
            
            String itemName = "钢支撑轴力";
            String fullItemName = getFullItemName(project.getName(), itemName);
            MonitoringItem item = monitoringItems.get(fullItemName);
            
            if (item == null) {
                item = new MonitoringItem();
                item.setId(UUID.randomUUID().toString());
                item.setName(itemName);
                item.setType("轴力");
                item.setLocation(project.getName() + " - " + itemName);
                monitoringItems.put(fullItemName, item);
            }

            // 获取数据存储对象
            SteelSupportAxialForceDataStorage storage = controller.getSteelSupportAxialForceDataStorage();
            
            // 更新监测项数据
            item.setRecords(records);
            item.setSteelSupportAxialForceDataStorage(storage);
            
            // 添加到项目中
            project.addMonitoringItem(itemName);
            
            System.out.println("已收集钢支撑轴力监测数据: " + records.size() + " 条记录");
        }
        // 处理ConcreteSupportAxialForceController
        else if (userData instanceof ConcreteSupportAxialForceController) {
            ConcreteSupportAxialForceController controller = (ConcreteSupportAxialForceController) userData;
            
            // 收集砼支撑轴力数据...
            if (controller.hasDataChanged()) {
                List<MeasurementRecord> records = controller.getMeasurementRecordsForSaving();
                
                String itemName = "砼支撑轴力";
                String fullItemName = getFullItemName(project.getName(), itemName);
                MonitoringItem item = monitoringItems.get(fullItemName);

                if (item == null) {
                    item = new MonitoringItem();
                    item.setId(UUID.randomUUID().toString());
                    item.setName(itemName);
                    item.setType("支撑轴力");
                    item.setLocation(project.getName() + " - " + itemName);
                    monitoringItems.put(fullItemName, item);
                }
                
                // 获取数据存储对象
                ConcreteSupportAxialForceDataStorage storage = controller.getConcreteSupportAxialForceDataStorage();
                
                // 更新监测项数据
                item.setRecords(records);
                item.setConcreteSupportAxialForceDataStorage(storage);
                
                // 添加到项目中
                project.addMonitoringItem(itemName);
                
                System.out.println("已收集砼支撑轴力数据: " + records.size() + " 条记录");
            } else {
                System.out.println("砼支撑轴力数据未更改，不需要保存");
            }
        }
        // 处理DeepHorizontalDisplacementController
        else if (userData instanceof DeepHorizontalDisplacementController) {
            DeepHorizontalDisplacementController controller = (DeepHorizontalDisplacementController) userData;
            
            // 收集深部水平位移数据
            List<MeasurementRecord> records = controller.getMeasurementRecordsForSaving();
            
            String itemName = "深部水平位移";
            String fullItemName = getFullItemName(project.getName(), itemName);
            MonitoringItem item = monitoringItems.get(fullItemName);

            if (item == null) {
                item = new MonitoringItem();
                item.setId(UUID.randomUUID().toString());
                item.setName(itemName);
                item.setType("位移");
                item.setLocation(project.getName() + " - " + itemName);
                monitoringItems.put(fullItemName, item);
            }
            
            // 获取数据存储对象 - 使用getOrCreate方法确保存储对象不为空
            DeepHorizontalDisplacementDataStorage storage = controller.getDeepHorizontalDisplacementDataStorage();
            
            // 打印验证数据块
            System.out.println("深部水平位移数据块数量: " + storage.getDataBlockTimestamps().size());
            for (LocalDateTime timestamp : storage.getDataBlockTimestamps()) {
                List<?> dataList = storage.getDataBlock(timestamp);
                System.out.println("  数据块 " + timestamp + ": " + (dataList != null ? dataList.size() : 0) + " 条记录");
            }
            
            // 确保存储对象包含完整的数据块信息
            DeepHorizontalDisplacementDataStorage completeStorage = item.getOrCreateDeepHorizontalDisplacementDataStorage();
            completeStorage.setPoints(storage.getPoints());
            completeStorage.setCustomDaysForRateCalculation(storage.getCustomDaysForRateCalculation());
            completeStorage.setSelectedDataBlocks(storage.getSelectedDataBlocks());
            
            // 清除旧数据块并添加新数据块
            for (LocalDateTime timestamp : new ArrayList<>(completeStorage.getDataBlockTimestamps())) {
                completeStorage.removeDataBlock(timestamp);
            }
            
            // 复制所有数据块
            for (LocalDateTime timestamp : storage.getDataBlockTimestamps()) {
                completeStorage.addDataBlock(timestamp, storage.getDataBlock(timestamp), 
                    storage.getDataBlockDescription(timestamp));
            }
            
            // 更新监测项数据
            item.setRecords(records);
            
            // 添加到项目中
            project.addMonitoringItem(itemName);
            
            System.out.println("已收集深部水平位移数据: " + records.size() + " 条记录, " + 
                storage.getDataBlockTimestamps().size() + " 个数据块");
        }
        // 处理PileTopHorizontalDisplacementController
        else if (userData instanceof PileTopHorizontalDisplacementController) {
            PileTopHorizontalDisplacementController controller = (PileTopHorizontalDisplacementController) userData;
            
            // 收集桩顶水平位移数据
            List<MeasurementRecord> records = controller.getMeasurementRecordsForSaving();
            
            String itemName = "桩顶水平位移";
            String fullItemName = getFullItemName(project.getName(), itemName);
            MonitoringItem item = monitoringItems.get(fullItemName);
            
            if (item == null) {
                item = new MonitoringItem();
                item.setId(UUID.randomUUID().toString());
                item.setName(itemName);
                item.setType("位移");
                item.setLocation(project.getName() + " - " + itemName);
                monitoringItems.put(fullItemName, item);
            }
            
            // 获取数据存储对象
            PileTopHorizontalDisplacementDataStorage storage = controller.getPileTopHorizontalDisplacementDataStorage();
            
            // 更新监测项数据
            item.setRecords(records);
            item.setPileTopHorizontalDisplacementDataStorage(storage);
            
            // 添加到项目中
            project.addMonitoringItem(itemName);
            
            System.out.println("已收集桩顶水平位移监测数据: " + records.size() + " 条记录");
        }
        // 如果userData不是控制器，递归搜索子节点
        else {
            for (Node child : parent.getChildrenUnmodifiable()) {
                if (child instanceof Parent) {
                    findAndCollectFromController((Parent) child, project);
                }
            }
        }
    }

    private void handleSaveAsProject() {
        System.out.println("项目另存为...");

        // 获取当前选中的标签页
        Tab selectedTab = editorTabPane.getSelectionModel().getSelectedItem();

        // 如果没有选中标签页，或者选中的不是项目标签页，则提示用户先选择一个项目
        if (selectedTab == null) {
            showSelectProjectDialog();
            return;
        }

        // 尝试找到对应的项目
        ProjectInfo projectToSave = null;
        String projectName = selectedTab.getText();

        // 检查是否是已知项目名称
        projectToSave = findProjectByName(projectName);

        // 如果没有找到，可能标签页名称包含了测项名称，尝试从树中查找
        if (projectToSave == null) {
            TreeItem<String> projectRoot = projectTreeView.getRoot();
            for (TreeItem<String> projectNode : projectRoot.getChildren()) {
                // 检查是否是此项目的测项标签页
                String currentProjectName = projectNode.getValue();
                if (projectName.startsWith(currentProjectName + " - ")) {
                    projectToSave = findProjectByName(currentProjectName);
                    break;
                }
            }
        }

        // 如果仍未找到，提示用户选择项目
        if (projectToSave == null) {
            showSelectProjectDialog();
            return;
        }

        final ProjectInfo finalProjectToSave = projectToSave;

        // 在保存前，收集所有项目数据
        saveAllProjectData(finalProjectToSave);

        // 创建文件选择器
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("项目另存为");
        fileChooser.getExtensionFilters().add(
            new ExtensionFilter("工程监测项目文件", "*.jc")
        );
        fileChooser.setInitialFileName(finalProjectToSave.getName() + ".jc");

        // 显示保存文件对话框
        File selectedFile = fileChooser.showSaveDialog(getStage());

        if (selectedFile != null) {
            // 创建一个新的项目副本
            ProjectInfo projectCopy = new ProjectInfo();
            projectCopy.setId(finalProjectToSave.getId());
            projectCopy.setName(finalProjectToSave.getName());
            projectCopy.setDescription(finalProjectToSave.getDescription());
            projectCopy.setOrganization(finalProjectToSave.getOrganization());
            projectCopy.setManager(finalProjectToSave.getManager());
            projectCopy.setMonitoringItems(new ArrayList<>(finalProjectToSave.getMonitoringItems()));

            // 保存项目副本到文件
            boolean success = ProjectFileUtil.saveProject(projectCopy, monitoringItems, selectedFile);

            if (success) {
                // 更新项目文件路径
                projectCopy.setProjectFile(selectedFile);

                // 提示保存成功
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("另存为成功");
                alert.setHeaderText(null);
                alert.setContentText("项目 " + projectCopy.getName() + " 已成功另存为：\n" + selectedFile.getAbsolutePath());
                alert.showAndWait();
            } else {
                // 提示保存失败
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("另存为失败");
                alert.setHeaderText("无法保存项目");
                alert.setContentText("保存项目 " + projectCopy.getName() + " 到文件时出错。");
                alert.showAndWait();
            }
        }
    }

    private void handleAddModule() {
        System.out.println("添加监测模块...");

        // 查询当前选中项
        Tab selectedTab = editorTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) {
            AlertUtil.showWarning("未选择项目", "请先选择一个项目，再添加监测模块。");
            return;
        }

        // 检查是否是项目标签页
        String tempProjectName = selectedTab.getText();
        ProjectInfo tempProject = findProjectByName(tempProjectName);

        if (tempProject == null) {
            // 尝试从项目-测项格式中提取项目名
            if (tempProjectName.contains(" - ")) {
                tempProjectName = tempProjectName.split(" - ")[0];
                tempProject = findProjectByName(tempProjectName);
            }
        }

        if (tempProject == null) {
            AlertUtil.showWarning("未找到项目", "请确保选中的标签页是项目标签页。");
            return;
        }

        // 创建final变量用于lambda表达式
        final String projectName = tempProjectName;
        final ProjectInfo project = tempProject;

        // 显示模块选择对话框
        ChoiceDialog<String> dialog = new ChoiceDialog<>("地表点沉降", Arrays.asList(
                "地表点沉降", "桩顶位移", "钢支撑轴力", "水位", "裂缝", "倾斜"));
        dialog.setTitle("添加监测模块");
        dialog.setHeaderText("请选择要添加的监测模块");
        dialog.setContentText("模块类型:");

        dialog.showAndWait().ifPresent(moduleType -> {
            // 检查模块是否已经添加过
            boolean alreadyAdded = project.getMonitoringItems().contains(moduleType);

            if (alreadyAdded) {
                // 如果模块已添加，询问是否打开
                ButtonType openButton = new ButtonType("打开现有", ButtonBar.ButtonData.YES);
                ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);

                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "监测模块'" + moduleType + "'已存在。是否打开？",
                        openButton, cancelButton);
                alert.setTitle("模块已存在");

                alert.showAndWait().ifPresent(buttonType -> {
                    if (buttonType == openButton) {
                        openMonitoringItemWithProject(projectName, moduleType);
                    }
                });
            } else {
                // 添加新的监测模块
                project.addMonitoringItem(moduleType);

                // 添加到工程树
                TreeItem<String> projectNode = findProjectNode(projectName);
                if (projectNode != null) {
                    TreeItem<String> moduleNode = new TreeItem<>(moduleType);
                    projectNode.getChildren().add(moduleNode);
                }

                // 打开监测模块
                openMonitoringItemWithProject(projectName, moduleType);

                System.out.println("已添加监测模块: " + moduleType + " 到项目: " + projectName);
            }
        });
    }

    private void handleProjectProperties() {
        System.out.println("工程属性...");
        // 这里可以添加显示工程属性的逻辑
    }

    private void handleSaveDefault() {
        System.out.println("保存默认值...");
        // 保存所有项目到默认目录
        saveProjectsToDefaultDirectory();
    }

    /**
     * 保存所有项目到默认目录
     */
    private void saveProjectsToDefaultDirectory() {
        // 默认项目目录，使用用户文档目录下的"工程监测项目"文件夹
        String userHome = System.getProperty("user.home");
        File projectsDir = new File(userHome, "工程监测项目");

        // 如果目录不存在，创建它
        if (!projectsDir.exists()) {
            boolean created = projectsDir.mkdirs();
            if (!created) {
                System.err.println("无法创建默认项目目录: " + projectsDir.getAbsolutePath());

                // 显示错误提示
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("保存错误");
                alert.setHeaderText("无法创建默认项目目录");
                alert.setContentText("请确保您有权限创建目录: " + projectsDir.getAbsolutePath());
                alert.showAndWait();
                return;
            }
        }

        int savedCount = 0;
        List<String> failedProjects = new ArrayList<>();

        // 保存所有项目
        for (ProjectInfo project : projects.values()) {
            // 先收集项目数据
            saveAllProjectData(project);

            // 创建项目文件
            File projectFile = new File(projectsDir, project.getName() + ".jc");

            // 保存项目到文件
            boolean success = ProjectFileUtil.saveProject(project, monitoringItems, projectFile);

            if (success) {
                // 更新项目文件路径
                project.setProjectFile(projectFile);
                savedCount++;
            } else {
                failedProjects.add(project.getName());
            }
        }

        // 显示保存结果
        if (savedCount > 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("保存成功");
            alert.setHeaderText(null);
            alert.setContentText("已成功将 " + savedCount + " 个项目保存到默认目录\n" + projectsDir.getAbsolutePath());
            alert.showAndWait();
        }

        // 如果有失败的项目，显示错误信息
        if (!failedProjects.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("保存错误");
            alert.setHeaderText("以下项目保存失败");

            StringBuilder content = new StringBuilder();
            for (String projectName : failedProjects) {
                content.append("- ").append(projectName).append("\n");
            }

            alert.setContentText(content.toString());
            alert.showAndWait();
        }
    }

    private void handleLoadDefault() {
        System.out.println("调取默认值...");
        // 加载默认目录中的项目文件
        loadProjectsFromDefaultDirectory();
    }

    /**
     * 从默认目录加载项目文件
     */
    private void loadProjectsFromDefaultDirectory() {
        // 默认项目目录，使用用户文档目录下的"工程监测项目"文件夹
        String userHome = System.getProperty("user.home");
        File projectsDir = new File(userHome, "工程监测项目");

        // 如果目录不存在，创建它
        if (!projectsDir.exists()) {
            boolean created = projectsDir.mkdirs();
            if (!created) {
                System.err.println("无法创建默认项目目录: " + projectsDir.getAbsolutePath());
                return;
            }
        }

        // 获取目录中的所有.jc文件
        File[] projectFiles = projectsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jc"));

        if (projectFiles == null || projectFiles.length == 0) {
            System.out.println("默认目录中没有找到项目文件");
            return;
        }

        int loadedCount = 0;

        // 加载每个项目文件
        for (File file : projectFiles) {
            ProjectFileData data = ProjectFileUtil.loadProject(file);

            if (data != null && data.projectInfo != null) {
                // 检查是否已存在同名项目
                boolean exists = false;
                String projectName = data.projectInfo.getName();

                for (ProjectInfo existingProject : projects.values()) {
                    if (existingProject.getName().equals(projectName)) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    // 加载项目
                    loadProjectData(data, file);
                    loadedCount++;
                }
            }
        }

        System.out.println("从默认目录加载了 " + loadedCount + " 个项目");
    }

    private void handleParameters() {
        System.out.println("参数定义...");
        // 这里可以添加参数定义的逻辑
    }

    /**
     * 处理添加测项菜单项点击事件
     */
    private void handleAddMonitoringItem(TreeItem<String> projectNode) {
        // 创建测项选择对话框
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("添加测项");
        dialog.setHeaderText("选择要添加的测项类型");

        // 设置按钮
        ButtonType addButtonType = new ButtonType("添加", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // 创建测项类型下拉框
        ComboBox<String> itemTypeCombo = new ComboBox<>();
        itemTypeCombo.getItems().addAll(
            "地表点沉降", "桩顶竖向位移", "桩顶水平位移", "钢支撑轴力",
            "砼支撑轴力", "立柱竖向位移", "建筑物沉降", "建筑物倾斜",
            "地下水位", "深部水平位移"
        );
        itemTypeCombo.setPromptText("选择测项类型");

        // 创建对话框内容
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.getChildren().add(itemTypeCombo);
        dialog.getDialogPane().setContent(content);

        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return itemTypeCombo.getValue();
            }
            return null;
        });

        // 显示对话框并处理结果
        dialog.showAndWait().ifPresent(itemType -> {
            if (itemType != null && !itemType.isEmpty()) {
                // 检查项目节点下是否已存在该测项
                boolean exists = false;
                for (TreeItem<String> child : projectNode.getChildren()) {
                    if (child.getValue().equals(itemType)) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    // 添加测项到树中
                    TreeItem<String> newItemNode = new TreeItem<>(itemType);
                    projectNode.getChildren().add(newItemNode);

                    // 创建监测项对象
                    MonitoringItem item = new MonitoringItem(
                            itemType.replaceAll("\\s+", ""),  // 移除空格作为ID
                            itemType,  // 名称
                            getItemType(itemType),  // 类型
                            projectNode.getValue() + " - " + itemType  // 位置
                    );

                    // 使用全名（项目名+测项名）作为键
                    String fullItemName = getFullItemName(projectNode.getValue(), itemType);
                    monitoringItems.put(fullItemName, item);

                    // 更新项目信息中的测项列表
                    for (ProjectInfo project : projects.values()) {
                        if (project.getName().equals(projectNode.getValue())) {
                            project.addMonitoringItem(itemType);
                            break;
                        }
                    }
                }
            }
        });
    }

    /**
     * 处理项目属性菜单项点击事件
     */
    private void handleProjectProperties(TreeItem<String> projectNode) {
        // 查找项目信息
        final ProjectInfo projectInfo = findProjectByName(projectNode.getValue());

        if (projectInfo != null) {
            // 创建并显示项目编辑对话框
            ProjectDialog dialog = new ProjectDialog(projectInfo);
            dialog.setHeaderText("编辑项目属性");

            // 显示对话框并等待用户响应
            dialog.showAndWait().ifPresent(updatedInfo -> {
                // 更新项目信息
                projectInfo.setName(updatedInfo.getName());
                projectInfo.setDescription(updatedInfo.getDescription());
                projectInfo.setOrganization(updatedInfo.getOrganization());
                projectInfo.setManager(updatedInfo.getManager());

                // 更新测项列表（删除已移除的测项，添加新增的测项）
                List<String> oldItems = new ArrayList<>(projectInfo.getMonitoringItems());
                List<String> newItems = updatedInfo.getMonitoringItems();

                // 移除已删除的测项
                List<TreeItem<String>> itemsToRemove = new ArrayList<>();
                for (TreeItem<String> itemNode : projectNode.getChildren()) {
                    if (!newItems.contains(itemNode.getValue())) {
                        itemsToRemove.add(itemNode);
                    }
                }
                projectNode.getChildren().removeAll(itemsToRemove);

                // 添加新增的测项
                for (String newItem : newItems) {
                    if (!oldItems.contains(newItem)) {
                        TreeItem<String> newItemNode = new TreeItem<>(newItem);
                        projectNode.getChildren().add(newItemNode);

                        // 创建监测项对象
                        MonitoringItem item = new MonitoringItem(
                                newItem.replaceAll("\\s+", ""),  // 移除空格作为ID
                                newItem,  // 名称
                                getItemType(newItem),  // 类型
                                projectInfo.getName() + " - " + newItem  // 位置
                        );
                        monitoringItems.put(getFullItemName(projectInfo.getName(), newItem), item);
                    }
                }

                // 更新项目测项列表
                projectInfo.setMonitoringItems(newItems);

                // 更新项目节点名称
                projectNode.setValue(projectInfo.getName());
            });
        }
    }

    /**
     * 根据项目名称查找项目信息
     */
    private ProjectInfo findProjectByName(String projectName) {
        for (ProjectInfo project : projects.values()) {
            if (project.getName().equals(projectName)) {
                return project;
            }
        }
        return null;
    }

    /**
     * 处理移除项目菜单项点击事件
     */
    private void handleRemoveProject(TreeItem<String> projectNode) {
        // 查找项目信息
        final ProjectInfo projectToRemove = findProjectByName(projectNode.getValue());
        final String projectId = findProjectIdByName(projectNode.getValue());

        if (projectToRemove != null && projectId != null) {
            // 显示确认对话框
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认移除");
            alert.setHeaderText("是否确认移除项目：" + projectToRemove.getName());
            alert.setContentText("此操作将移除该项目及其所有测项数据，且不可恢复。");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    // 移除项目信息
                    projects.remove(projectId);

                    // 获取所有需要关闭的标签页和移除的监测项
                    List<Tab> tabsToRemove = new ArrayList<>();
                    List<String> itemsToRemove = new ArrayList<>();

                    // 移除每个测项
                    for (TreeItem<String> itemNode : projectNode.getChildren()) {
                        String itemName = itemNode.getValue();
                        String fullItemName = getFullItemName(projectNode.getValue(), itemName);

                        // 添加到要移除的监测项列表
                        itemsToRemove.add(fullItemName);

                        // 查找对应的标签页
                        Tab itemTab = findTab(fullItemName);
                        if (itemTab != null) {
                            tabsToRemove.add(itemTab);
                        }
                    }

                    // 移除树节点
                    projectTreeView.getRoot().getChildren().remove(projectNode);

                    // 移除相关的标签页
                    editorTabPane.getTabs().removeAll(tabsToRemove);

                    // 检查项目本身的标签页
                    Tab projectTab = findTab(projectToRemove.getName());
                    if (projectTab != null) {
                        editorTabPane.getTabs().remove(projectTab);
                    }

                    // 移除所有监测项数据
                    for (String item : itemsToRemove) {
                        monitoringItems.remove(item);
                    }

                    // 显示操作成功信息
                    System.out.println("项目 " + projectToRemove.getName() + " 已成功移除");
                }
            });
        }
    }

    /**
     * 根据项目名称查找项目ID
     */
    private String findProjectIdByName(String projectName) {
        for (Map.Entry<String, ProjectInfo> entry : projects.entrySet()) {
            if (entry.getValue().getName().equals(projectName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 保存指定的项目
     * @param project 要保存的项目
     * @return 是否保存成功
     */
    private boolean saveProject(ProjectInfo project) {
        // 收集项目数据
        saveAllProjectData(project);

        // 检查项目是否已经有关联的文件
        File selectedFile = null;
        if (project.getProjectFile() != null && project.getProjectFile().exists()) {
            // 使用已有的文件
            selectedFile = project.getProjectFile();
            System.out.println("自动保存项目到: " + selectedFile.getAbsolutePath());
        } else {
            // 创建文件选择器
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("保存工程监测项目");
            fileChooser.getExtensionFilters().add(
                new ExtensionFilter("工程监测项目文件", "*.jc")
            );
            fileChooser.setInitialFileName(project.getName() + ".jc");

            // 显示保存文件对话框
            selectedFile = fileChooser.showSaveDialog(getStage());
        }

        if (selectedFile != null) {
            // 保存项目到文件
            boolean success = ProjectFileUtil.saveProject(project, monitoringItems, selectedFile);

            if (success) {
                // 更新项目文件路径
                project.setProjectFile(selectedFile);
                System.out.println("项目 " + project.getName() + " 已成功保存到文件：" + selectedFile.getAbsolutePath());
                return true;
            } else {
                System.err.println("保存项目 " + project.getName() + " 到文件时出错。");
            }
        }

        return false;
    }

    // 添加一个辅助方法，用于获取带项目前缀的测项全名
    private String getFullItemName(String projectName, String itemName) {
        return projectName.isEmpty() ? itemName : projectName + " - " + itemName;
    }

    // 添加新方法，处理带项目名称的测项打开
    private void openMonitoringItemWithProject(String projectName, String itemName) {
        // 处理不同类型的监测项
        if (itemName.equals("地表点沉降")) {
            openSubsidenceView(projectName);
        } else if (itemName.equals("地下水位")) {
            openGroundwaterLevelView(projectName);
        } else if (itemName.equals("桩顶竖向位移")) {
            openPileTopDisplacementView(projectName);
        } else if (itemName.equals("立柱竖向位移")) {
            openColumnDisplacementView(projectName);
        } else if (itemName.equals("建筑物沉降")) {
            openBuildingSettlementView(projectName);
        } else if (itemName.equals("钢支撑轴力")) {
            openSteelSupportAxialForceView(projectName);
        } else if (itemName.equals("砼支撑轴力")) {
            openConcreteSupportAxialForceView(projectName);
        } else if (itemName.equals("深部水平位移")) {
            openDeepHorizontalDisplacementView(projectName);
        } else if (itemName.equals("桩顶水平位移")) {
            openPileTopHorizontalDisplacementView(projectName);
        } else {
            // 其他监测项
        String fullItemName = getFullItemName(projectName, itemName);

            // 先尝试查找是否已打开相应标签页
        Tab existingTab = findTab(fullItemName);
        if (existingTab != null) {
            editorTabPane.getSelectionModel().select(existingTab);
            return;
        }

            // 如果没有打开，显示提示信息
            showErrorDialog("功能未实现", itemName + " 模块开发中", "该测项功能正在开发中，敬请期待！");
        }
    }

    /**
     * 打开沉降监测视图
     */
    private void openSubsidenceView(String projectName) {
        String fullItemName = getFullItemName(projectName, "地表点沉降");

        // 检查该测项标签页是否已经打开
        Tab existingTab = findTab(fullItemName);
        if (existingTab != null) {
            editorTabPane.getSelectionModel().select(existingTab);
            return;
        }

        try {
            // 获取或创建监测项
            MonitoringItem item = monitoringItems.get(fullItemName);
            if (item == null) {
                item = new MonitoringItem();
                item.setId(UUID.randomUUID().toString());
                item.setName("地表点沉降");
                item.setType("地表点沉降");  // 更新为更详细的类型
                item.setLocation(projectName + " - 地表点沉降");
                monitoringItems.put(fullItemName, item);
            }

            // 加载FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SettlementDataView.fxml"));
            Parent root = loader.load();

            // 获取控制器并设置数据
            SettlementDataController controller = loader.getController();
            if (controller != null) {
                // 设置Stage
                controller.setStage(getStage());

                // 如果存在沉降数据存储对象，优先使用它
                if (item.getSettlementDataStorage() != null) {
                    controller.loadFromSettlementDataStorage(item.getSettlementDataStorage());
                    System.out.println("已从沉降数据存储对象加载数据");
                }
                // 如果没有沉降数据存储对象，但存在测量记录，则使用测量记录
                else if (item.getRecords() != null && !item.getRecords().isEmpty()) {
                    controller.loadFromMeasurementRecords(item.getRecords());
                    System.out.println("已加载沉降监测数据: " + item.getRecords().size() + " 条记录");
                }

                // 保存控制器引用到节点的userData中
                root.setUserData(controller);
            }

            // 创建标签页
            Tab tab = new Tab(fullItemName);
            tab.setClosable(true);
            tab.setContent(root);

            // 添加标签页并选中
            editorTabPane.getTabs().add(tab);
            editorTabPane.getSelectionModel().select(tab);

        } catch (IOException e) {
            System.err.println("无法加载沉降监测视图: " + e.getMessage());
            e.printStackTrace();

            // 显示错误信息
            AlertUtil.showError("加载错误", "无法加载沉降监测视图: " + e.getMessage());
        }
    }

    /**
     * 显示选择项目对话框
     */
    private void showSelectProjectDialog() {
        // 创建项目选择对话框
        Dialog<ProjectInfo> dialog = new Dialog<>();
        dialog.setTitle("选择项目");
        dialog.setHeaderText("请选择要保存的项目");

        // 设置按钮
        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // 创建列表视图显示所有项目
        ListView<ProjectInfo> projectListView = new ListView<>();
        projectListView.getItems().addAll(projects.values());
        projectListView.setCellFactory(param -> new ListCell<ProjectInfo>() {
            @Override
            protected void updateItem(ProjectInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        dialog.getDialogPane().setContent(projectListView);

        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return projectListView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        // 显示对话框并处理结果
        dialog.showAndWait().ifPresent(selectedProject -> {
            if (selectedProject != null) {
                // 找到对应的标签页并选中
                Tab projectTab = findTab(selectedProject.getName());
                if (projectTab != null) {
                    editorTabPane.getSelectionModel().select(projectTab);
                }

                // 再次调用保存方法
                handleSaveProject();
            }
        });
    }

    /**
     * 打开项目统计视图
     */
    private void openProjectStatisticsView() {
        // 创建新标签页
        Tab tab = new Tab("项目统计");
        tab.setClosable(true);

        // 创建统计内容
        BorderPane content = new BorderPane();
        Label label = new Label("项目统计视图 - 开发中");
        label.setStyle("-fx-font-size: 16; -fx-padding: 20;");
        content.setCenter(label);

        tab.setContent(content);

        // 添加到标签页并选中
        editorTabPane.getTabs().add(tab);
        editorTabPane.getSelectionModel().select(tab);
    }

    /**
     * 显示功能开发中的消息
     */
    private void showUndevelopedFeatureMessage(String featureName) {
        // 创建新标签页
        Tab tab = new Tab(featureName);
        tab.setClosable(true);

        // 创建内容
        BorderPane content = new BorderPane();
        Label label = new Label(featureName + " - 功能开发中");
        label.setStyle("-fx-font-size: 16; -fx-padding: 20;");
        content.setCenter(label);

        tab.setContent(content);

        // 添加到标签页并选中
        editorTabPane.getTabs().add(tab);
        editorTabPane.getSelectionModel().select(tab);
    }

    /**
     * 在TreeView中查找项目节点
     *
     * @param projectName 项目名称
     * @return 项目节点，如果未找到返回null
     */
    private TreeItem<String> findProjectNode(String projectName) {
        TreeItem<String> root = projectTreeView.getRoot();
        if (root == null) {
            return null;
        }

        for (TreeItem<String> node : root.getChildren()) {
            if (node.getValue().equals(projectName)) {
                return node;
            }
        }

        return null;
    }

    /**
     * 处理监控概览项的点击事件
     */
    private void handleOverviewItemClick(String itemName) {
        switch (itemName) {
            case "沉降监测":
                final String projectName = "概览";
                openSubsidenceView(projectName);
                break;
            case "项目统计":
                openProjectStatisticsView();
                break;
            default:
                showUndevelopedFeatureMessage(itemName);
                break;
        }
    }

    private void handleHeaderMousePressed(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    private void handleHeaderMouseDragged(MouseEvent event) {
        if (isMaximized) {
            return; // 如果窗口已最大化，不允许拖拽
        }
        
        Stage stage = getStage();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    /**
     * 将测量记录转换为立柱位移数据列表
     */
    private List<ColumnDisplacementData> convertMeasurementRecordsToColumnDisplacementData(List<MeasurementRecord> records) {
        List<ColumnDisplacementData> result = new ArrayList<>();
        
        for (MeasurementRecord record : records) {
            ColumnDisplacementData data = new ColumnDisplacementData();
            data.setPointCode(record.getId());
            data.setMeasurementDate(record.getMeasureTime().toLocalDate());
            
            // 从测量记录中提取数值作为当前高程
            data.setCurrentElevation(record.getValue());
            
            // 初始高程和前次高程设置默认值，后续可能需要根据实际情况调整
            double initialElevation = record.getValue(); // 暂时使用当前值作为初始值
            data.setInitialElevation(initialElevation);
            data.setPreviousElevation(initialElevation);
            
            // 设置默认的里程
            data.setMileage("");
            
            // 历史累计值默认为0
            data.setHistoricalCumulative(0);
            
            // 计算变化值（因为初始值和当前值相同，所以变化值为0）
            data.setCurrentChange(0);
            data.setCumulativeChange(0);
            
            // 变化速率设为0
            data.setChangeRate(0);
            
            result.add(data);
        }
        
        return result;
    }

    /**
     * 打开地下水位视图
     * @param projectName 项目名称
     */
    private void openGroundwaterLevelView(String projectName) {
        try {
            // 加载FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/GroundwaterLevelView.fxml"));
            Parent root = loader.load();
            root.setUserData(loader.getController());
            
            // 创建新标签页
            Tab tab = new Tab(projectName + " - 地下水位");
            tab.setContent(root);
            tab.setClosable(true);
            
            // 获取控制器
            GroundwaterLevelController controller = loader.getController();
            
            // 如果已有该工程的地下水位监测项，获取其数据
            String fullItemName = projectName + " - 地下水位";
            MonitoringItem groundwaterLevelItem = monitoringItems.get(fullItemName);
            
            if (groundwaterLevelItem == null) {
                // 如果不存在，创建新的监测项
                groundwaterLevelItem = new MonitoringItem();
                groundwaterLevelItem.setId(UUID.randomUUID().toString());
                groundwaterLevelItem.setName("地下水位");
                groundwaterLevelItem.setType("水位");
                monitoringItems.put(fullItemName, groundwaterLevelItem);
            }
            
            // 如果有已保存的测量记录，加载它们
            if (groundwaterLevelItem.getRecords() != null && !groundwaterLevelItem.getRecords().isEmpty()) {
                controller.loadFromMeasurementRecords(groundwaterLevelItem.getRecords());
            }
            
            // 如果有已保存的数据存储对象，加载它
            if (groundwaterLevelItem.getGroundwaterLevelDataStorage() != null) {
                GroundwaterLevelDataStorage storage = groundwaterLevelItem.getGroundwaterLevelDataStorage();
                controller.loadFromGroundwaterLevelDataStorage(storage);
            }
            
            // 添加标签页
            editorTabPane.getTabs().add(tab);
            editorTabPane.getSelectionModel().select(tab);
            
        } catch (IOException e) {
            showErrorDialog("加载错误", "无法加载地下水位视图", e.getMessage());
        }
    }

    // This method should be near the openSubsidenceView and openGroundwaterLevelView methods
    private void openPileTopDisplacementView(String projectName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PileTopDisplacementView.fxml"));
            Parent root = loader.load();
            
            // 获取控制器
            PileTopDisplacementController controller = loader.getController();
            controller.setStage(getStage());
            
            // 创建新标签页
            Tab tab = new Tab(projectName + " - 桩顶竖向位移");
            tab.setContent(root);
            tab.setClosable(true);
            
            // 添加到标签页面板
            editorTabPane.getTabs().add(tab);
            editorTabPane.getSelectionModel().select(tab);
            
            // 获取项目关联的监测项
            String fullItemName = getFullItemName(projectName, "桩顶竖向位移");
            MonitoringItem item = monitoringItems.get(fullItemName);
            
            // 如果存在关联的监测项，加载其数据
            if (item != null && item.getPileDisplacementDataStorage() != null) {
                // 使用符合命名规范的新方法
                controller.loadFromPileDisplacementDataStorage(item.getPileDisplacementDataStorage());
            }
            
        } catch (IOException e) {
            showErrorDialog("加载错误", "无法加载桩顶竖向位移视图", e.getMessage());
        }
    }

    private void openColumnDisplacementView(String projectName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ColumnDisplacementView.fxml"));
            Parent root = loader.load();
            
            // 获取控制器
            ColumnDisplacementController controller = loader.getController();
            controller.setStage(getStage());
            
            // 创建新标签页
            Tab tab = new Tab(projectName + " - 立柱竖向位移");
            tab.setContent(root);
            tab.setClosable(true);
            
            // 添加到标签页面板
            editorTabPane.getTabs().add(tab);
            editorTabPane.getSelectionModel().select(tab);
            
            // 获取项目关联的监测项
            String fullItemName = getFullItemName(projectName, "立柱竖向位移");
            MonitoringItem item = monitoringItems.get(fullItemName);
            
            // 如果存在关联的监测项，加载其数据
            if (item != null && item.getColumnDisplacementDataStorage() != null) {
                // 使用符合命名规范的新方法
                controller.loadFromColumnDisplacementDataStorage(item.getColumnDisplacementDataStorage());
            }
            
        } catch (IOException e) {
            showErrorDialog("加载错误", "无法加载立柱竖向位移视图", e.getMessage());
        }
    }

    // Add showErrorDialog method that matches the same style as the AlertUtil utility
    private void showErrorDialog(String title, String header, String content) {
        AlertUtil.showError(title, header + "\n\n" + content);
    }

    /**
     * 打开建筑物沉降视图
     */
    private void openBuildingSettlementView(String projectName) {
        String fullItemName = getFullItemName(projectName, "建筑物沉降");

        // 检查该测项标签页是否已经打开
        Tab existingTab = findTab(fullItemName);
        if (existingTab != null) {
            editorTabPane.getSelectionModel().select(existingTab);
            return;
        }

        try {
            // 获取或创建监测项
            MonitoringItem item = monitoringItems.get(fullItemName);
            if (item == null) {
                item = new MonitoringItem();
                item.setId(UUID.randomUUID().toString());
                item.setName("建筑物沉降");
                item.setType("建筑物沉降");  
                item.setLocation(projectName + " - 建筑物沉降");
                monitoringItems.put(fullItemName, item);
            }

            // 加载FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BuildingSettlementView.fxml"));
            Parent root = loader.load();

            // 获取控制器并设置数据
            BuildingSettlementController controller = loader.getController();
            if (controller != null) {
                // 设置Stage
                controller.setStage(getStage());

                // 如果存在建筑物沉降数据存储对象，优先使用它
                if (item.getBuildingSettlementDataStorage() != null) {
                    controller.loadFromBuildingSettlementDataStorage(item.getBuildingSettlementDataStorage());
                    System.out.println("已从建筑物沉降数据存储对象加载数据");
                }
                // 如果没有建筑物沉降数据存储对象，但存在测量记录，则使用测量记录
                else if (item.getRecords() != null && !item.getRecords().isEmpty()) {
                    controller.loadFromMeasurementRecords(item.getRecords());
                    System.out.println("已加载建筑物沉降监测数据: " + item.getRecords().size() + " 条记录");
                }

                // 保存控制器引用到节点的userData中
                root.setUserData(controller);
            }

            // 创建标签页
            Tab tab = new Tab(fullItemName);
            tab.setClosable(true);
            tab.setContent(root);

            // 添加标签页并选中
            editorTabPane.getTabs().add(tab);
            editorTabPane.getSelectionModel().select(tab);

        } catch (IOException e) {
            System.err.println("无法加载建筑物沉降监测视图: " + e.getMessage());
            e.printStackTrace();

            // 显示错误信息
            AlertUtil.showError("加载错误", "无法加载建筑物沉降监测视图: " + e.getMessage());
        }
    }

    /**
     * 打开钢支撑轴力视图
     */
    private void openSteelSupportAxialForceView(String projectName) {
        String fullItemName = getFullItemName(projectName, "钢支撑轴力");

        // 检查该测项标签页是否已经打开
        Tab existingTab = findTab(fullItemName);
        if (existingTab != null) {
            editorTabPane.getSelectionModel().select(existingTab);
            return;
        }

        try {
            // 获取或创建监测项
            MonitoringItem item = monitoringItems.get(fullItemName);
            if (item == null) {
                item = new MonitoringItem();
                item.setId(UUID.randomUUID().toString());
                item.setName("钢支撑轴力");
                item.setType("钢支撑轴力");  
                item.setLocation(projectName + " - 钢支撑轴力");
                monitoringItems.put(fullItemName, item);
            }

            // 加载FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SteelSupportAxialForceView.fxml"));
            Parent root = loader.load();

            // 获取控制器并设置数据
            SteelSupportAxialForceController axialForceController = loader.getController();
            axialForceController.setStage(getStage());

            // 如果存在相应的监测项，加载其数据
            if (item != null) {
                // 如果存在钢支撑轴力数据存储对象，优先使用它
                if (item.getSteelSupportAxialForceDataStorage() != null) {
                    axialForceController.loadFromSteelSupportAxialForceDataStorage(item.getSteelSupportAxialForceDataStorage());
                    System.out.println("已从钢支撑轴力数据存储对象加载数据");
                }
                // 如果没有钢支撑轴力数据存储对象，但存在测量记录，则使用测量记录
                else if (item.getRecords() != null && !item.getRecords().isEmpty()) {
                    axialForceController.loadFromMeasurementRecords(item.getRecords());
                    System.out.println("已加载钢支撑轴力监测数据: " + item.getRecords().size() + " 条记录");
                }
            }

            // 保存控制器引用到节点的userData中
            root.setUserData(axialForceController);

            // 创建标签页
            Tab tab = new Tab(fullItemName);
            tab.setClosable(true);
            tab.setContent(root);

            // 添加标签页并选中
            editorTabPane.getTabs().add(tab);
            editorTabPane.getSelectionModel().select(tab);

        } catch (IOException e) {
            System.err.println("无法加载钢支撑轴力监测视图: " + e.getMessage());
            e.printStackTrace();

            // 显示错误信息
            AlertUtil.showError("加载错误", "无法加载钢支撑轴力监测视图: " + e.getMessage());
        }
    }

    /**
     * 打开砼支撑轴力视图
     */
    private void openConcreteSupportAxialForceView(String projectName) {
        String fullItemName = getFullItemName(projectName, "砼支撑轴力");

        // 检查该测项标签页是否已经打开
        Tab existingTab = findTab(fullItemName);
        if (existingTab != null) {
            editorTabPane.getSelectionModel().select(existingTab);
            return;
        }

        try {
            // 获取或创建监测项
            MonitoringItem item = monitoringItems.get(fullItemName);
            if (item == null) {
                item = new MonitoringItem();
                item.setId(UUID.randomUUID().toString());
                item.setName("砼支撑轴力");
                item.setType("砼支撑轴力");  
                item.setLocation(projectName + " - 砼支撑轴力");
                monitoringItems.put(fullItemName, item);
            }

            // 加载FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ConcreteSupportAxialForceView.fxml"));
            Parent root = loader.load();

            // 获取控制器并设置数据
            ConcreteSupportAxialForceController controller = loader.getController();
            if (controller != null) {
                // 设置Stage
                controller.setStage(getStage());

                // 如果存在砼支撑轴力数据存储对象，优先使用它
                if (item.getConcreteSupportAxialForceDataStorage() != null) {
                    controller.loadFromConcreteSupportAxialForceDataStorage(item.getConcreteSupportAxialForceDataStorage());
                    System.out.println("已从砼支撑轴力数据存储对象加载数据");
                }
                // 如果没有砼支撑轴力数据存储对象，但存在测量记录，则使用测量记录
                else if (item.getRecords() != null && !item.getRecords().isEmpty()) {
                    controller.loadFromMeasurementRecords(item.getRecords());
                    System.out.println("已加载砼支撑轴力监测数据: " + item.getRecords().size() + " 条记录");
                }

                // 保存控制器引用到节点的userData中
                root.setUserData(controller);
            }

            // 创建标签页
            Tab tab = new Tab(fullItemName);
            tab.setClosable(true);
            tab.setContent(root);

            // 添加标签页并选中
            editorTabPane.getTabs().add(tab);
            editorTabPane.getSelectionModel().select(tab);

        } catch (IOException e) {
            System.err.println("无法加载砼支撑轴力监测视图: " + e.getMessage());
            e.printStackTrace();

            // 显示错误信息
            AlertUtil.showError("加载错误", "无法加载砼支撑轴力监测视图: " + e.getMessage());
        }
    }

    /**
     * 打开深部水平位移视图
     * @param projectName 项目名称
     */
    private void openDeepHorizontalDisplacementView(String projectName) {
        String fullItemName = getFullItemName(projectName, "深部水平位移");

        // 检查该测项标签页是否已经打开
        Tab existingTab = findTab(fullItemName);
        if (existingTab != null) {
            editorTabPane.getSelectionModel().select(existingTab);
            return;
        }

        try {
            // 获取或创建监测项
            MonitoringItem item = monitoringItems.get(fullItemName);
            if (item == null) {
                item = new MonitoringItem();
                item.setId(UUID.randomUUID().toString());
                item.setName("深部水平位移");
                item.setType("深部水平位移");
                item.setLocation(projectName + " - 深部水平位移");
                monitoringItems.put(fullItemName, item);
            }

            // 加载FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DeepHorizontalDisplacementView.fxml"));
            Parent root = loader.load();

            // 获取控制器并设置数据
            DeepHorizontalDisplacementController controller = loader.getController();
            if (controller != null) {
                // 设置Stage
                controller.setStage(getStage());

                // 获取或创建深部水平位移数据存储对象，并打印详细信息
                boolean dataLoaded = false;
                
                // 使用getOrCreateDeepHorizontalDisplacementDataStorage确保始终存在存储对象
                DeepHorizontalDisplacementDataStorage storage = item.getOrCreateDeepHorizontalDisplacementDataStorage();
                int pointCount = storage.getPoints() != null ? storage.getPoints().size() : 0;
                int dataBlockCount = storage.getDataBlockCount();
                
                System.out.println("准备加载深部水平位移数据: 测点数=" + pointCount + ", 数据块数=" + dataBlockCount);
                
                if (dataBlockCount > 0 || pointCount > 0) {
                    controller.loadFromDeepHorizontalDisplacementDataStorage(storage);
                    System.out.println("已从深部水平位移数据存储对象加载数据");
                    dataLoaded = true;
                } else {
                    System.out.println("深部水平位移数据存储对象存在但没有数据");
                }

                // 如果没有加载数据存储对象，但存在测量记录，则使用测量记录
                if (!dataLoaded && item.getRecords() != null && !item.getRecords().isEmpty()) {
                    controller.loadFromMeasurementRecords(item.getRecords());
                    System.out.println("已加载深部水平位移监测数据: " + item.getRecords().size() + " 条记录");
                }

                // 保存控制器引用到节点的userData中
                root.setUserData(controller);
            }

            // 创建标签页
            Tab tab = new Tab(fullItemName);
            tab.setClosable(true);
            tab.setContent(root);

            // 添加标签页并选中
            editorTabPane.getTabs().add(tab);
            editorTabPane.getSelectionModel().select(tab);

        } catch (IOException e) {
            System.err.println("无法加载深部水平位移视图: " + e.getMessage());
            e.printStackTrace();

            // 显示错误信息
            AlertUtil.showError("加载错误", "无法加载深部水平位移视图: " + e.getMessage());
        }
    }

    /**
     * 打开桩顶水平位移视图
     */
    private void openPileTopHorizontalDisplacementView(String projectName) {
        String fullItemName = getFullItemName(projectName, "桩顶水平位移");

        // 检查该测项标签页是否已经打开
        Tab existingTab = findTab(fullItemName);
        if (existingTab != null) {
            editorTabPane.getSelectionModel().select(existingTab);
            return;
        }

        try {
            // 获取或创建监测项
            MonitoringItem item = monitoringItems.get(fullItemName);
            if (item == null) {
                item = new MonitoringItem();
                item.setId(UUID.randomUUID().toString());
                item.setName("桩顶水平位移");
                item.setType("桩顶水平位移");
                item.setLocation(projectName + " - 桩顶水平位移");
                monitoringItems.put(fullItemName, item);
            }

            // 加载FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PileTopHorizontalDisplacementView.fxml"));
            Parent root = loader.load();

            // 获取控制器并设置数据
            PileTopHorizontalDisplacementController controller = loader.getController();
            if (controller != null) {
                // 设置Stage
                controller.setStage(getStage());

                // 如果存在桩顶水平位移数据存储对象，优先使用它
                if (item.getPileTopHorizontalDisplacementDataStorage() != null) {
                    controller.loadFromPileTopHorizontalDisplacementDataStorage(item.getPileTopHorizontalDisplacementDataStorage());
                    System.out.println("已从桩顶水平位移数据存储对象加载数据");
                }
                // 如果没有桩顶水平位移数据存储对象，但存在测量记录，则使用测量记录
                else if (item.getRecords() != null && !item.getRecords().isEmpty()) {
                    controller.loadFromMeasurementRecords(item.getRecords());
                    System.out.println("已加载桩顶水平位移监测数据: " + item.getRecords().size() + " 条记录");
                }

                // 保存控制器引用到节点的userData中
                root.setUserData(controller);
            }

            // 创建标签页
            Tab tab = new Tab(fullItemName);
            tab.setClosable(true);
            tab.setContent(root);

            // 添加标签页并选中
            editorTabPane.getTabs().add(tab);
            editorTabPane.getSelectionModel().select(tab);

        } catch (IOException e) {
            System.err.println("无法加载桩顶水平位移监测视图: " + e.getMessage());
            e.printStackTrace();

            // 显示错误信息
            AlertUtil.showError("加载错误", "无法加载桩顶水平位移监测视图: " + e.getMessage());
        }
    }

    /**
     * 自动保存当前项目
     * 此方法供子控制器调用以在数据上传后触发自动保存
     */
    public void autoSaveCurrentProject() {
        // 获取当前选中的标签页
        Tab selectedTab = editorTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) {
            System.out.println("自动保存失败：没有选中的标签页");
            return;
        }

        // 尝试找到对应的项目
        String tabTitle = selectedTab.getText();
        ProjectInfo projectToSave = null;

        // 检查是否是项目标签页
        projectToSave = findProjectByName(tabTitle);

        // 如果没有找到，可能标签页名称包含了测项名称，尝试提取项目名
        if (projectToSave == null) {
            if (tabTitle.contains(" - ")) {
                String projectName = tabTitle.split(" - ")[0];
                projectToSave = findProjectByName(projectName);
            }
        }

        if (projectToSave != null) {
            // 在保存前，收集所有项目数据
            saveAllProjectData(projectToSave);
            
            // 保存项目
            if (projectToSave.getProjectFile() != null) {
                saveProject(projectToSave);
                System.out.println("已自动保存项目: " + projectToSave.getName());
            } else {
                System.out.println("项目没有关联文件，无法自动保存: " + projectToSave.getName());
            }
        } else {
            System.out.println("自动保存失败：无法找到关联的项目");
        }
    }

    /**
     * 处理应用程序关闭
     * 用于在应用程序关闭前保存所有数据
     */
    public void handleApplicationClose() {
        // 检查是否有未保存的项目
        List<ProjectInfo> unsavedProjects = new ArrayList<>();
        for (ProjectInfo project : projects.values()) {
            if (hasUnsavedChanges(project) || true) { // 强制视为有更改，确保所有项目都被保存
                unsavedProjects.add(project);
            }
        }

        // 如果有未保存的项目，提示用户是否保存
        if (!unsavedProjects.isEmpty()) {
            // 自动保存所有项目，无需提示
            boolean allSaved = true;
            for (ProjectInfo project : unsavedProjects) {
                if (project.getProjectFile() != null) {
                    // 在保存前，收集所有项目数据
                    saveAllProjectData(project);
                    
                    boolean success = saveProject(project);
                    if (!success) {
                        allSaved = false;
                        System.err.println("自动保存项目失败: " + project.getName());
                    } else {
                        System.out.println("已自动保存项目: " + project.getName());
                    }
                } else {
                    // 如果项目没有关联文件，尝试保存到默认目录
                    String defaultDir = System.getProperty("user.home") + File.separator + "MonitoringProjects";
                    File projectDir = new File(defaultDir);
                    if (!projectDir.exists()) {
                        projectDir.mkdirs();
                    }
                    
                    File projectFile = new File(projectDir, project.getName() + ".jc");
                    project.setProjectFile(projectFile);
                    
                    // 在保存前，收集所有项目数据
                    saveAllProjectData(project);
                    
                    boolean success = saveProject(project);
                    if (!success) {
                        allSaved = false;
                        System.err.println("自动保存项目到默认目录失败: " + project.getName());
                    } else {
                        System.out.println("已自动保存项目到默认目录: " + project.getName());
                    }
                }
            }
            
            if (allSaved) {
                System.out.println("所有项目已自动保存，应用程序即将关闭");
            } else {
                System.err.println("部分项目保存失败，应用程序仍将关闭");
            }
        }
        
        // 关闭应用程序
        Platform.exit();
    }
} 