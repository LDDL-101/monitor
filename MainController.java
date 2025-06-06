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
import com.monitor.controller.WarningInfoController;
import com.monitor.controller.StatisticsController;

public class MainController implements Initializable {

    private double xOffset = 0;
    private double yOffset = 0;
    private boolean isMaximized = false;
    
    // 绐楀彛璋冩暣澶у皬鐨勯槇鍊煎拰鐘舵€?    private static final int RESIZE_PADDING = 5;
    private double startX = 0;
    private double startY = 0;
    private double startWidth = 0;
    private double startHeight = 0;
    private boolean resizeLeft = false;
    private boolean resizeRight = false;
    private boolean resizeTop = false;
    private boolean resizeBottom = false;
    
    // 灞忓箷杈圭晫锛岀敤浜庢渶澶у寲琛屼负
    private Rectangle2D screenBounds;
    
    // 鏁版嵁瀛樺偍
    private Map<String, MonitoringItem> monitoringItems = new HashMap<>();
    private Map<String, ProjectInfo> projects = new HashMap<>();

    // 鍦∕ainController绫荤殑寮€澶存坊鍔犻潤鎬佸疄渚嬪彉閲?    // 鍗曚緥瀹炰緥
    private static MainController instance;
    
    /**
     * 鑾峰彇MainController鐨勫崟渚嬪疄渚?     */
    public static MainController getInstance() {
        return instance;
    }

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
    
    // 宸ュ叿鏍忔寜閽?    @FXML
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
    
    // 渚ц竟鏍忕粍浠?    @FXML
    private VBox sidebarPane;
    
    @FXML
    private TreeView<String> projectTreeView;
    
    @FXML
    private TreeView<String> reportsTreeView;
    
    @FXML
    private TreeView<String> overviewTreeView;
    
    // 缂栬緫鍣ㄥ尯鍩?    @FXML
    private TabPane editorTabPane;

    // 宸ュ叿鏍忕浉鍏?    @FXML
    private ToolBar mainToolBar;
    
    @FXML
    private CheckMenuItem toggleToolbarMenuItem;

    @FXML private HBox windowHeader;

    // 娣诲姞涓€涓垚鍛樺彉閲忕敤浜庡瓨鍌ㄩ」鐩甀D涓庨璀︿俊鎭垪琛ㄧ殑鏄犲皠鍏崇郴
    private Map<String, List<WarningInfo>> projectWarningMap = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 璁剧疆鍗曚緥瀹炰緥
        instance = this;
        
        // 璁剧疆UI缁勪欢浜嬩欢
        setupUI();
        
        // 璁剧疆渚ц竟鏍?        setupSidebar();
        
        // 璁剧疆缂栬緫鍣ㄥ尯鍩?        setupEditorArea();
        
        // 璁剧疆鑷姩淇濆瓨瀹氭椂鍣?        setupAutoSaveTimer();

        // 璁剧疆绐楀彛鏍囬鏍忔嫋鍔ㄥ姛鑳?        windowHeader.setOnMousePressed(this::handleHeaderMousePressed);
        windowHeader.setOnMouseDragged(this::handleHeaderMouseDragged);
    }
    
    /**
     * 璁剧疆绐楀彛绠＄悊鍣?- 鍦⊿cene鍒涘缓鍚庤皟鐢?     * 姝ゆ柟娉曢渶瑕佸湪Scene鍒涘缓瀹屾垚鍚庣敱澶栭儴璋冪敤
     */
    public void setupWindowManager(Stage stage) {
        // 鍒濆鍖栫獥鍙ｇ鐞嗗櫒
        WindowManager windowManager = new WindowManager(stage, mainPane.getScene());
        
        // 璁剧疆绐楀彛浜嬩欢澶勭悊
        windowManager.setupWindowHandlers(titleLabel);
        
        // 鎷栨嫿鏍囬鏍忕Щ鍔ㄧ獥鍙?        titleLabel.setOnMousePressed(event -> windowManager.handleMousePressed(event));
        titleLabel.setOnMouseDragged(event -> windowManager.handleMouseDragged(event));
        
        // 娣诲姞搴旂敤鍥炬爣鐨勬嫋鎷戒簨浠?        appIcon.setOnMousePressed(event -> windowManager.handleMousePressed(event));
        appIcon.setOnMouseDragged(event -> windowManager.handleMouseDragged(event));
        
        // 鏌ユ壘鏍囬鏍廐Box骞惰缃嫋鎷戒簨浠?        Node titleBar = mainPane.lookup(".app-title");
        if (titleBar != null) {
            // 涓烘暣涓爣棰樻爮娣诲姞鎷栨嫿浜嬩欢锛屼絾鎺掗櫎鎸夐挳
            titleBar.setOnMousePressed(event -> {
                Node target = (Node) event.getTarget();
                // 纭繚鐐瑰嚮鐨勪笉鏄寜閽垨鍏跺瓙鍏冪礌
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
        
        // 绐楀彛鎺у埗鎸夐挳浜嬩欢
        minimizeButton.setOnAction(event -> stage.setIconified(true));
        maximizeButton.setOnAction(event -> windowManager.maximizeOrRestoreWindow());
        closeButton.setOnAction(event -> handleApplicationClose());
    }

    /**
     * 妫€鏌ョ洰鏍囪妭鐐规槸鍚︽槸鎸囧畾鎸夐挳鎴栧叾瀛愬厓绱?     */
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
     * 璁剧疆鑷姩淇濆瓨瀹氭椂鍣紝姣?鍒嗛挓鑷姩淇濆瓨鎵€鏈夋湁鏇存敼鐨勯」鐩?     */
    private void setupAutoSaveTimer() {
        // 鍒涘缓瀹氭椂鍣ㄤ换鍔?        java.util.Timer timer = new java.util.Timer(true);
        timer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                // 鍦↗avaFX绾跨▼涓婃墽琛屼繚瀛樻搷浣?                Platform.runLater(() -> {
                    autoSaveProjects();
                });
            }
        }, 5 * 60 * 1000, 5 * 60 * 1000); // 寤惰繜5鍒嗛挓鍚庡紑濮嬶紝姣?鍒嗛挓鎵ц涓€娆?    }

    /**
     * 鑷姩淇濆瓨鎵€鏈夋湁鏇存敼鐨勯」鐩?     */
    private void autoSaveProjects() {
        int savedCount = 0;

        // 妫€鏌ユ墍鏈夐」鐩?        for (ProjectInfo project : projects.values()) {
            // 鍙繚瀛樻湁鏇存敼涓斿凡缁忔湁鏂囦欢璺緞鐨勯」鐩?            if (hasUnsavedChanges(project) && project.getProjectFile() != null) {
                boolean success = saveProject(project);
                if (success) {
                    savedCount++;
                }
            }
        }

        if (savedCount > 0) {
            System.out.println("鑷姩淇濆瓨瀹屾垚锛屽凡淇濆瓨 " + savedCount + " 涓」鐩?);
        }
    }

    public void setScreenBounds(Rectangle2D screenBounds) {
        this.screenBounds = screenBounds;
    }

    private void setupUI() {
        // 璁剧疆宸ュ叿鏍忔寜閽簨浠?        newButton.setOnAction(event -> handleNewProject());
        openButton.setOnAction(event -> handleOpenProject());
        saveButton.setOnAction(event -> handleSaveProject());
        saveAsButton.setOnAction(event -> handleSaveAsProject());
        addModuleButton.setOnAction(event -> handleAddModule());
        projectPropertiesButton.setOnAction(event -> handleProjectProperties());
        saveDefaultButton.setOnAction(event -> handleSaveDefault());
        loadDefaultButton.setOnAction(event -> handleLoadDefault());
        parametersButton.setOnAction(event -> handleParameters());
        
        // 璁剧疆娆㈣繋鐣岄潰鎸夐挳浜嬩欢
        setupWelcomeButtons();
        
        // 璁剧疆宸ュ叿鏍忔樉绀?闅愯棌鍔熻兘
        if (toggleToolbarMenuItem != null) {
            // 鍒濆鐘舵€佷笌CheckMenuItem鐨剆elected灞炴€т竴鑷?            mainToolBar.setVisible(toggleToolbarMenuItem.isSelected());
            mainToolBar.setManaged(toggleToolbarMenuItem.isSelected());
            
            // 鐩戝惉CheckMenuItem鐨勯€変腑鐘舵€佸彉鍖?            toggleToolbarMenuItem.selectedProperty().addListener((observable, oldValue, newValue) -> {
                // 鍚屾宸ュ叿鏍忕殑鍙鎬у拰绠＄悊鐘舵€?                mainToolBar.setVisible(newValue);
                mainToolBar.setManaged(newValue); // 杩欑淇濆竷灞€浼氶噸鏂拌绠?            });
        }
    }
    
    /**
     * 璁剧疆娆㈣繋鐣岄潰鎸夐挳浜嬩欢
     */
    private void setupWelcomeButtons() {
        // 鍦ㄦ杩庣晫闈㈡爣绛鹃〉涓煡鎵炬寜閽?        if (editorTabPane != null && !editorTabPane.getTabs().isEmpty()) {
            Tab welcomeTab = editorTabPane.getTabs().get(0);
            if (welcomeTab != null && welcomeTab.getText().equals("娆㈣繋")) {
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
                                        if (button.getText().equals("鏂板缓宸ョ▼")) {
                                            button.setOnAction(event -> handleNewProject());
                                        } else if (button.getText().equals("鎵撳紑宸ョ▼")) {
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
        // 璁剧疆宸ョ▼绠＄悊鏍?        TreeItem<String> projectRoot = new TreeItem<>("宸ョ▼鍒楄〃");
        projectRoot.setExpanded(true);
        
        // 涓嶅啀娣诲姞绀轰緥椤圭洰
        projectTreeView.setRoot(projectRoot);
        projectTreeView.setShowRoot(true);

        // 鍔犺浇榛樿鐩綍涓殑椤圭洰鏂囦欢
        loadProjectsFromDefaultDirectory();
        
        // 璁剧疆鐩戞祴鎶ヨ〃鏍?        TreeItem<String> reportsRoot = new TreeItem<>("鎶ヨ〃绫诲瀷");
        reportsRoot.setExpanded(true);
        
        reportsRoot.getChildren().add(new TreeItem<>("鏃ユ姤琛?));
        reportsRoot.getChildren().add(new TreeItem<>("鍛ㄦ姤琛?));
        reportsRoot.getChildren().add(new TreeItem<>("鏈堟姤琛?));
        reportsRoot.getChildren().add(new TreeItem<>("瀛ｆ姤琛?));
        reportsRoot.getChildren().add(new TreeItem<>("骞存姤琛?));
        
        reportsTreeView.setRoot(reportsRoot);
        reportsTreeView.setShowRoot(true);
        
        // 璁剧疆鐩戞祴姒傚喌鏍?        TreeItem<String> overviewRoot = new TreeItem<>("鐩戞祴姒傚喌");
        overviewRoot.setExpanded(true);
        
        // 鏇存柊姒傝鏍戜互鍙嶆槧褰撳墠椤圭洰鍒楄〃
        refreshOverviewTree();
        
        overviewTreeView.setRoot(overviewRoot);
        overviewTreeView.setShowRoot(true);

        // 纭繚渚ц竟鏍忕粍浠跺～鍏呮墍鏈夊彲鐢ㄧ┖闂?        VBox.setVgrow(sidebarPane, javafx.scene.layout.Priority.ALWAYS);

        // 鑾峰彇TitledPane缁勪欢
        for (Node node : sidebarPane.getChildren()) {
            if (node instanceof TitledPane) {
                TitledPane titledPane = (TitledPane) node;
                VBox.setVgrow(titledPane, javafx.scene.layout.Priority.ALWAYS);

                // 璁剧疆鍐呭鍖哄煙
                if (titledPane.getContent() instanceof TreeView) {
                    TreeView<?> treeView = (TreeView<?>) titledPane.getContent();
                    VBox.setVgrow(treeView, javafx.scene.layout.Priority.ALWAYS);
                }
            }
        }
        
        // 璁剧疆鏍戣鍥剧殑鐐瑰嚮浜嬩欢
        projectTreeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<String> selectedItem = projectTreeView.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    String itemName = selectedItem.getValue();
                    
                    // 妫€鏌ユ槸鍚︽槸"鍦拌〃鐐规矇闄?椤?                    if (itemName.equals("鍦拌〃鐐规矇闄?)) {
                        // 鎵惧埌鎵€灞為」鐩?                        TreeItem<String> parent = selectedItem.getParent();
                        if (parent != null && !parent.getValue().equals("宸ョ▼鍒楄〃")) {
                            // 浣跨敤椤圭洰鍚嶇О鎵撳紑娌夐檷瑙嗗浘
                            openSubsidenceView(parent.getValue());
                        } else {
                            // 濡傛灉娌℃湁鎵惧埌鐖堕」鐩紝浣跨敤榛樿鍚嶇О
                            openSubsidenceView("宸ョ▼鍒楄〃");
                        }
                    }
                    // 妫€鏌ユ槸鍚︽槸"妗╅《绔栧悜浣嶇Щ"椤?                    else if (itemName.equals("妗╅《绔栧悜浣嶇Щ")) {
                        // 鎵惧埌鎵€灞為」鐩?                        TreeItem<String> parent = selectedItem.getParent();
                        if (parent != null && !parent.getValue().equals("宸ョ▼鍒楄〃")) {
                            // 浣跨敤椤圭洰鍚嶇О鍜屾祴椤瑰悕绉版墦寮€浣嶇Щ瑙嗗浘
                            openMonitoringItemWithProject(parent.getValue(), itemName);
                        } else {
                            // 濡傛灉娌℃湁鎵惧埌鐖堕」鐩紝鐩存帴鎵撳紑
                            openMonitoringItemTab(itemName);
                        }
                    }
                    // 妫€鏌ユ槸鍚︽槸"绔嬫煴绔栧悜浣嶇Щ"椤?                    else if (itemName.equals("绔嬫煴绔栧悜浣嶇Щ")) {
                        // 鎵惧埌鎵€灞為」鐩?                        TreeItem<String> parent = selectedItem.getParent();
                        if (parent != null && !parent.getValue().equals("宸ョ▼鍒楄〃")) {
                            // 浣跨敤椤圭洰鍚嶇О鍜屾祴椤瑰悕绉版墦寮€绔嬫煴绔栧悜浣嶇Щ瑙嗗浘
                            openMonitoringItemWithProject(parent.getValue(), itemName);
                        } else {
                            // 濡傛灉娌℃湁鎵惧埌鐖堕」鐩紝鐩存帴鎵撳紑
                            openMonitoringItemTab(itemName);
                        }
                    }
                    // 妫€鏌ユ槸鍚︽槸"鍦颁笅姘翠綅"椤?                    else if (itemName.equals("鍦颁笅姘翠綅")) {
                        // 鎵惧埌鎵€灞為」鐩?                        TreeItem<String> parent = selectedItem.getParent();
                        if (parent != null && !parent.getValue().equals("宸ョ▼鍒楄〃")) {
                            // 浣跨敤椤圭洰鍚嶇О鍜屾祴椤瑰悕绉版墦寮€鍦颁笅姘翠綅瑙嗗浘
                            openMonitoringItemWithProject(parent.getValue(), itemName);
                        } else {
                            // 濡傛灉娌℃湁鎵惧埌鐖堕」鐩紝鐩存帴鎵撳紑
                            openMonitoringItemTab(itemName);
                        }
                    }
                    // 妫€鏌ユ槸鍚︽槸"寤虹瓚鐗╂矇闄?椤?                    else if (itemName.equals("寤虹瓚鐗╂矇闄?)) {
                        // 鎵惧埌鎵€灞為」鐩?                        TreeItem<String> parent = selectedItem.getParent();
                        if (parent != null && !parent.getValue().equals("宸ョ▼鍒楄〃")) {
                            // 浣跨敤椤圭洰鍚嶇О鍜屾祴椤瑰悕绉版墦寮€寤虹瓚鐗╂矇闄嶈鍥?                            openMonitoringItemWithProject(parent.getValue(), itemName);
                        } else {
                            // 濡傛灉娌℃湁鎵惧埌鐖堕」鐩紝鐩存帴鎵撳紑
                            openMonitoringItemTab(itemName);
                        }
                    }
                    // 妫€鏌ユ槸鍚︽槸"閽㈡敮鎾戣酱鍔?椤?                    else if (itemName.equals("閽㈡敮鎾戣酱鍔?)) {
                        // 鎵惧埌鎵€灞為」鐩?                        TreeItem<String> parent = selectedItem.getParent();
                        if (parent != null && !parent.getValue().equals("宸ョ▼鍒楄〃")) {
                            // 浣跨敤椤圭洰鍚嶇О鍜屾祴椤瑰悕绉版墦寮€閽㈡敮鎾戣酱鍔涜鍥?                            openMonitoringItemWithProject(parent.getValue(), itemName);
                        } else {
                            // 濡傛灉娌℃湁鎵惧埌鐖堕」鐩紝鐩存帴鎵撳紑
                            openMonitoringItemTab(itemName);
                        }
                    }
                    // 妫€鏌ユ槸鍚︽槸"鐮兼敮鎾戣酱鍔?椤?                    else if (itemName.equals("鐮兼敮鎾戣酱鍔?)) {
                        // 鎵惧埌鎵€灞為」鐩?                        TreeItem<String> parent = selectedItem.getParent();
                        if (parent != null && !parent.getValue().equals("宸ョ▼鍒楄〃")) {
                            // 浣跨敤椤圭洰鍚嶇О鍜屾祴椤瑰悕绉版墦寮€鐮兼敮鎾戣酱鍔涜鍥?                            openMonitoringItemWithProject(parent.getValue(), itemName);
                        } else {
                            // 濡傛灉娌℃湁鎵惧埌鐖堕」鐩紝鐩存帴鎵撳紑
                            openMonitoringItemTab(itemName);
                        }
                    }
                    // 妫€鏌ユ槸鍚︽槸"娣遍儴姘村钩浣嶇Щ"椤?                    else if (itemName.equals("娣遍儴姘村钩浣嶇Щ")) {
                        // 鎵惧埌鎵€灞為」鐩?                        TreeItem<String> parent = selectedItem.getParent();
                        if (parent != null && !parent.getValue().equals("宸ョ▼鍒楄〃")) {
                            // 浣跨敤椤圭洰鍚嶇О鍜屾祴椤瑰悕绉版墦寮€娣遍儴姘村钩浣嶇Щ瑙嗗浘
                            openMonitoringItemWithProject(parent.getValue(), itemName);
                        } else {
                            // 濡傛灉娌℃湁鎵惧埌鐖堕」鐩紝鐩存帴鎵撳紑
                            openMonitoringItemTab(itemName);
                        }
                    }
                    // 妫€鏌ユ槸鍚︽槸"妗╅《姘村钩浣嶇Щ"椤?                    else if (itemName.equals("妗╅《姘村钩浣嶇Щ")) {
                        // 鎵惧埌鎵€灞為」鐩?                        TreeItem<String> parent = selectedItem.getParent();
                        if (parent != null && !parent.getValue().equals("宸ョ▼鍒楄〃")) {
                            // 浣跨敤椤圭洰鍚嶇О鍜屾祴椤瑰悕绉版墦寮€妗╅《姘村钩浣嶇Щ瑙嗗浘
                            openMonitoringItemWithProject(parent.getValue(), itemName);
                        } else {
                            // 濡傛灉娌℃湁鎵惧埌鐖堕」鐩紝鐩存帴鎵撳紑
                            openMonitoringItemTab(itemName);
                        }
                    }
                    // 妫€鏌ユ槸鍚︽槸鐩戞祴鐐?                    else if (itemName.contains("鐩戞祴鐐?)) {
                        // 鎵惧埌鎵€灞為」鐩?                        TreeItem<String> parent = selectedItem.getParent();
                        while (parent != null && parent.getParent() != null && !parent.getParent().getValue().equals("宸ョ▼鍒楄〃")) {
                            parent = parent.getParent();
                        }

                        if (parent != null) {
                            // 浼犻€掗」鐩悕绉版墦寮€娴嬮」
                            openMonitoringItemWithProject(parent.getValue(), itemName);
                        } else {
                            // 娌℃湁鎵惧埌鐖堕」鐩紝鐩存帴鎵撳紑
                        openMonitoringItemTab(itemName);
                        }
                    } else if (!itemName.equals("宸ョ▼鍒楄〃")) {
                        // 濡傛灉鏄」鐩妭鐐癸紝妫€鏌ユ槸鍚︾洿鎺ユ槸宸ョ▼鍒楄〃鐨勫瓙鑺傜偣
                        if (selectedItem.getParent() != null && selectedItem.getParent().getValue().equals("宸ョ▼鍒楄〃")) {
                        openProjectTab(itemName);
                        }
                    }
                }
            }
        });
        
        reportsTreeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<String> selectedItem = reportsTreeView.getSelectionModel().getSelectedItem();
                if (selectedItem != null && !selectedItem.getValue().equals("鎶ヨ〃绫诲瀷")) {
                    openReportTab(selectedItem.getValue());
                }
            }
        });
        
        overviewTreeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<String> selectedItem = overviewTreeView.getSelectionModel().getSelectedItem();
                if (selectedItem != null && !selectedItem.getValue().equals("鐩戞祴姒傚喌")) {
                    // 妫€鏌ユ槸鍚︽槸娌夐檷鐩戞祴绛夌粺璁￠」
                    String itemName = selectedItem.getValue();
                    if (itemName.equals("娌夐檷鐩戞祴") || itemName.equals("浣嶇Щ鐩戞祴") || 
                        itemName.equals("姘翠綅鐩戞祴") || itemName.equals("搴斿姏鐩戞祴")) {
                        // 鏌ユ壘鐖惰妭鐐规槸鍚︽槸椤圭洰鑺傜偣
                        TreeItem<String> parent = selectedItem.getParent();
                        if (parent != null) {
                            String parentName = parent.getValue();
                            if (parentName.equals("缁熻鏁版嵁")) {
                                // 鏌ユ壘椤圭洰鑺傜偣
                                TreeItem<String> projectNode = parent.getParent();
                                if (projectNode != null && !projectNode.getValue().equals("鐩戞祴姒傚喌")) {
                                    // 浣跨敤椤圭洰鍚嶇О鎵撳紑瀵瑰簲瑙嗗浘
                                    if (itemName.equals("娌夐檷鐩戞祴")) {
                                        openSubsidenceView(projectNode.getValue());
                                    } else {
                                        openOverviewTab(itemName + " - " + projectNode.getValue());
                                    }
                                    return;
                                }
                            }
                        }
                        // 榛樿鏁版嵁琛屼负锛氭墦寮€姒傝瑙嗗浘
                        if (itemName.equals("娌夐檷鐩戞祴")) {
                        openSubsidenceView("姒傝");
                    } else {
                            openOverviewTab(itemName);
                        }
                    } else if (itemName.equals("涓绾ч璀?) || itemName.equals("浜岀骇棰勮") || itemName.equals("涓夌骇棰勮")) {
                        // 鏌ユ壘鐖惰妭鐐规槸鍚︽槸棰勮淇℃伅鑺傜偣
                        TreeItem<String> parent = selectedItem.getParent();
                        if (parent != null && parent.getValue().equals("棰勮淇℃伅")) {
                            // 鏌ユ壘椤圭洰鑺傜偣
                            TreeItem<String> projectNode = parent.getParent();
                            if (projectNode != null && !projectNode.getValue().equals("鐩戞祴姒傚喌")) {
                                // 浣跨敤椤圭洰鍚嶇О鎵撳紑棰勮淇℃伅瑙嗗浘
                                openOverviewTab(itemName + " - " + projectNode.getValue());
                                return;
                            }
                        }
                        // 榛樿鏁版嵁琛屼负
                        openOverviewTab(itemName);
                    } else if (itemName.equals("棰勮淇℃伅")) {
                        // 澶勭悊鍙屽嚮"棰勮淇℃伅"锛屾墦寮€棰勮淇℃伅瑙嗗浘
                        // 鏌ユ壘鐖惰妭鐐规槸鍚︽槸椤圭洰鑺傜偣
                        TreeItem<String> parent = selectedItem.getParent();
                        if (parent != null && !parent.getValue().equals("鐩戞祴姒傚喌")) {
                            // 浣跨敤椤圭洰鍚嶇О鎵撳紑棰勮淇℃伅瑙嗗浘
                            openWarningInfoView(parent.getValue());
                        } else {
                            // 涓嶅厑璁告墦寮€鍏ㄥ眬棰勮淇℃伅锛屾樉绀烘彁绀轰俊鎭?                            AlertUtil.showInformation("鎻愮ず", "璇烽€夋嫨鍏蜂綋椤圭洰鐨勯璀︿俊鎭煡鐪?);
                        }
                    } else if (itemName.equals("缁熻鏁版嵁")) {
                        // 澶勭悊鍙屽嚮"缁熻鏁版嵁"鑺傜偣
                        TreeItem<String> parent = selectedItem.getParent();
                        if (parent != null && !parent.getValue().equals("鐩戞祴姒傚喌")) {
                            // 浣跨敤椤圭洰鍚嶇О鎵撳紑缁熻鏁版嵁瑙嗗浘
                            openStatisticsView(parent.getValue());
                        } else {
                            AlertUtil.showInformation("鎻愮ず", "璇烽€夋嫨鍏蜂綋椤圭洰鐨勭粺璁℃暟鎹煡鐪?);
                        }
                    } else {
                        openOverviewTab(itemName);
                    }
                }
            }
        });

        // 璁剧疆宸ョ▼绠＄悊鏍戠殑鍙抽敭鑿滃崟
        projectTreeView.setOnContextMenuRequested(event -> {
            TreeItem<String> selectedItem = projectTreeView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                if (selectedItem.getValue().equals("宸ョ▼鍒楄〃")) {
                    // 涓哄伐绋嬪垪琛ㄨ妭鐐瑰垱寤哄彸閿?                    ContextMenu contextMenu = new ContextMenu();

                    // 娣诲姞"鎵撳紑椤圭洰"鑿滃崟椤?                    MenuItem openMenuItem = new MenuItem("鎵撳紑椤圭洰");
                    openMenuItem.setOnAction(e -> handleOpenProject());

                    contextMenu.getItems().add(openMenuItem);
        
                    // 鏄剧ず鑿滃崟
                    contextMenu.show(projectTreeView, event.getScreenX(), event.getScreenY());
                } else if (!selectedItem.getValue().equals("宸ョ▼鍒楄〃")) {
                    // 妫€鏌ユ槸鍚︽槸椤圭洰鑺傜偣锛堜竴绾ц妭鐐癸級
                    if (selectedItem.getParent() != null && selectedItem.getParent().getValue().equals("宸ョ▼鍒楄〃")) {
                        // 鍒涘缓鍙抽敭鑿滃崟
                        ContextMenu contextMenu = new ContextMenu();

                        // 娣诲姞娴嬮」鑿滃崟椤?                        MenuItem addItemMenuItem = new MenuItem("娣诲姞娴嬮」");
                        addItemMenuItem.setOnAction(e -> handleAddMonitoringItem(selectedItem));
        
                        // 灞炴€ц彍鍗曢」
                        MenuItem propertiesMenuItem = new MenuItem("灞炴€?);
                        propertiesMenuItem.setOnAction(e -> handleProjectProperties(selectedItem));
        
                        // 绉婚櫎鑿滃崟椤?                        MenuItem removeMenuItem = new MenuItem("绉婚櫎");
                        removeMenuItem.setOnAction(e -> handleRemoveProject(selectedItem));

                        contextMenu.getItems().addAll(addItemMenuItem, propertiesMenuItem, removeMenuItem);

                        // 鏄剧ず鑿滃崟
                        contextMenu.show(projectTreeView, event.getScreenX(), event.getScreenY());
                    }
                }
            }
        });
    }

    private void setupEditorArea() {
        // 璁剧疆鏍囩椤靛叧闂?        editorTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
    }

    /**
     * 鍒涘缓绀轰緥鏁版嵁鐨勬柟娉曪紙宸茬鐢?     * 濡傞渶鍒涘缓娴嬭瘯鏁版嵁锛屽彲鍦ㄩ渶瑕佹椂鎵嬪姩璋冪敤姝ゆ柟娉?     */
    private void createSampleData() {
        // 宸茬鐢ㄨ嚜鍔ㄥ垱寤烘祴璇曟暟鎹?        // 濡傞渶娴嬭瘯锛屽彲鍦ㄩ渶瑕佹椂鎵嬪姩璋冪敤姝ゆ柟娉?    }
    
    private void openProjectTab(String projectName) {
        // 妫€鏌ユ槸鍚﹀凡缁忓瓨鍦ㄧ浉鍚屾爣棰樼殑鏍囩?        Tab existingTab = findTab(projectName);
        
        if (existingTab != null) {
            // 濡傛灉瀛樺湪锛屽垯閫変腑璇ユ爣绛鹃〉
            editorTabPane.getSelectionModel().select(existingTab);
        } else {
            // 濡傛灉涓嶅瓨鍦紝鍒欏垱寤烘柊鐨勬爣绛鹃〉
            Tab tab = new Tab(projectName);

            // 鏌ユ壘椤圭洰淇℃伅
            ProjectInfo projectInfo = findProjectByName(projectName);
            
            // 鍒涘缓椤圭洰缂栬緫鍐呭鍖哄煙
            BorderPane content = new BorderPane();

            if (projectInfo != null) {
                // 鍒涘缓淇℃伅鏄剧ず闈㈡澘
                GridPane infoGrid = new GridPane();
                infoGrid.setHgap(10);
                infoGrid.setVgap(10);
                infoGrid.setPadding(new Insets(20));
                infoGrid.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 1;");

                // 鏍囬鏄剧ず闈㈡澘
                Label titleLabel = new Label("椤圭洰璇︾粏淇℃伅");
                titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
                infoGrid.add(titleLabel, 0, 0, 2, 1);

                // 椤圭洰淇℃伅
                infoGrid.add(new Label("椤圭洰鍚嶇О:"), 0, 1);
                infoGrid.add(new Label(projectInfo.getName()), 1, 1);

                infoGrid.add(new Label("鐩戞祴鍗曚綅:"), 0, 2);
                infoGrid.add(new Label(projectInfo.getOrganization()), 1, 2);

                infoGrid.add(new Label("鐩戞祴璐熻矗浜?"), 0, 3);
                infoGrid.add(new Label(projectInfo.getManager()), 1, 3);

                infoGrid.add(new Label("椤圭洰鎻忚堪:"), 0, 4);
                Label descLabel = new Label(projectInfo.getDescription());
                descLabel.setWrapText(true);
                infoGrid.add(descLabel, 1, 4);

                // 娴嬮」鍒楄〃
                infoGrid.add(new Label("鐩戞祴娴嬮」:"), 0, 5);

                VBox itemsBox = new VBox(5);
                for (String item : projectInfo.getMonitoringItems()) {
                    HBox itemBox = new HBox(10);
                    itemBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    // 鍥炬爣鍙栧喅浜庢祴椤圭被鍨?                    String iconPath = "/images/item_icon.png"; // 榛樿鏁版嵁鍥炬爣

                    switch (getItemType(item)) {
                        case "娌夐檷":
                            iconPath = "/images/settlement_icon.png";
                            break;
                        case "浣嶇Щ":
                            iconPath = "/images/displacement_icon.png";
                            break;
                        case "鍊炬枩":
                            iconPath = "/images/inclination_icon.png";
                            break;
                        case "姘翠綅":
                            iconPath = "/images/water_level_icon.png";
                            break;
                        case "搴斿姏":
                            iconPath = "/images/stress_icon.png";
                            break;
                    }

                    try {
                        ImageView icon = new ImageView(new javafx.scene.image.Image(getClass().getResourceAsStream(iconPath)));
                        icon.setFitHeight(16);
                        icon.setFitWidth(16);
                        itemBox.getChildren().add(icon);
                    } catch (Exception e) {
                        // 濡傛灉鍥炬爣鍔犺浇澶辫触锛屼娇鐢ㄦ枃鏈?                        Label iconLabel = new Label("鈥?);
                        itemBox.getChildren().add(iconLabel);
                    }

                    // 娴嬮」鍚嶇О
                    Label itemLabel = new Label(item);
                    itemBox.getChildren().add(itemLabel);

                    // 濡傛灉姝ゆ祴椤规湁鏁版嵁锛屾坊鍔犱竴涓鏄煡鐪嬫寜閽?                    MonitoringItem monItem = monitoringItems.get(item);
                    if (monItem != null && monItem.getRecords() != null && !monItem.getRecords().isEmpty()) {
                        Button viewButton = new Button("鏌ョ湅");
                        viewButton.setStyle("-fx-font-size: 10;");
                        viewButton.setOnAction(event -> openMonitoringItemTab(item));
                        itemBox.getChildren().add(viewButton);
                    }

                    itemsBox.getChildren().add(itemBox);
                }

                infoGrid.add(itemsBox, 1, 5);

                // 椤圭洰鎿嶄綔鎸夐挳
                HBox buttonBox = new HBox(10);
                buttonBox.setPadding(new Insets(20, 0, 0, 0));
                buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

                Button editButton = new Button("缂栬緫椤圭洰");
                editButton.getStyleClass().add("primary-button");
                editButton.setOnAction(event -> {
                    // 鎵惧埌椤圭洰鍦ㄦ爲涓?                    TreeItem<String> projectRoot = projectTreeView.getRoot();
                    for (TreeItem<String> projectNode : projectRoot.getChildren()) {
                        if (projectNode.getValue().equals(projectName)) {
                            handleProjectProperties(projectNode);
                            // 鏇存柊鏍囩椤垫爣棰橈紙濡傛灉椤圭洰鍚嶇О宸叉洿鏀癸級
                            tab.setText(projectInfo.getName());
                            break;
                        }
                    }
                });

                Button dataImportButton = new Button("鏁版嵁瀵煎叆");
                dataImportButton.getStyleClass().add("secondary-button");
                dataImportButton.setOnAction(event -> {
                    // 瀹炵幇鏁版嵁瀵煎叆鍔熻兘
                    System.out.println("瀵煎叆鏁版嵁鍒伴」鐩? + projectName);
                });

                Button generateReportButton = new Button("鐢熸垚鎶ヨ〃");
                generateReportButton.getStyleClass().add("secondary-button");
                generateReportButton.setOnAction(event -> {
                    // 瀹炵幇鎶ヨ〃鐢熸垚鍔熻兘
                    System.out.println("涓洪」鐩? + projectName);
                });

                buttonBox.getChildren().addAll(editButton, dataImportButton, generateReportButton);
                infoGrid.add(buttonBox, 0, 6, 2, 1);

                // 灏嗕俊鎭潰鏉挎斁鍏ュ唴瀹瑰尯鍩?                content.setCenter(infoGrid);
            } else {
                // 濡傛灉鎵句笉鍒伴」鐩?                Label label = new Label("椤圭洰" + projectName + " 鐨勮? + projectName + " 鐨勮?);
            label.setStyle("-fx-font-size: 16; -fx-padding: 20;");
            content.setCenter(label);
            }
            
            tab.setContent(content);
            
            // 娣诲姞鍒版爣绛鹃〉骞堕€変腑
            editorTabPane.getTabs().add(tab);
            editorTabPane.getSelectionModel().select(tab);
        }
    }
    
    private void openMonitoringItemTab(String itemName) {
        // 妫€鏌ヨ鏍囩椤垫槸鍚﹀凡缁忔墦寮€
        Tab existingTab = findTab(itemName);
        if (existingTab != null) {
            editorTabPane.getSelectionModel().select(existingTab);
            return;
        }

        // 鑾峰彇椤圭洰鍚嶅拰娴嬮」绫诲瀷
        String[] parts = itemName.split(" - ");
        if (parts.length < 2) {
            System.err.println("娴嬮」鍚嶇О鏍煎紡涓嶆繚? " + itemName);
            return;
        }

        String projectName = parts[0];
        String itemType = getItemType(parts[1]);

        // 鍒涘缓鏂版爣绛鹃〉
            Tab tab = new Tab(itemName);
        tab.setClosable(true);

        // 鏍规嵁娴嬮」绫诲瀷璁剧疆涓嶅悓鐨勫唴瀹?        try {
            switch (itemType) {
                case "娌夐檷":
                    // 鍔犺浇娌夐檷瑙嗗浘
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SettlementDataView.fxml"));
                    Parent root = loader.load();

                    // 鑾峰彇鎺у埗鍣ㄥ苟璁剧疆鏁版嵁
                    SettlementDataController controller = loader.getController();

                    // 濡傛灉瀛樺湪鐩稿簲鐨勭洃娴嬮」锛屽姞杞藉叾鏁版嵁
            MonitoringItem item = monitoringItems.get(itemName);
                    if (item != null && item.getRecords() != null && !item.getRecords().isEmpty()) {
                        controller.loadFromMeasurementRecords(item.getRecords());
                        System.out.println("宸插姞杞芥矇闄嶇洃娴嬫暟鎹? " + item.getRecords().size() + " 鏉¤?");
                    }

                    // 淇濆瓨鎺у埗鍣ㄥ紩鐢ㄥ埌鑺傜偣鐨剈serData涓?紙渚夸簬鍚庣画鏌ユ壘锛?                    root.setUserData(controller);

                    tab.setContent(root);
                    break;
                case "浣嶇Щ":
                    // 鍔犺浇妗╅《浣嶇Щ瑙嗗浘
                    FXMLLoader displacementLoader = new FXMLLoader(getClass().getResource("/fxml/PileTopDisplacementView.fxml"));
                    Parent displacementRoot = displacementLoader.load();

                    // 鑾峰彇鎺у埗鍣ㄥ苟璁剧疆鏁版嵁
                    PileTopDisplacementController displacementController = displacementLoader.getController();
                    displacementController.setStage(getStage());

                    // 濡傛灉瀛樺湪鐩稿簲鐨勭洃娴嬮」锛屽姞杞藉叾鏁版嵁
                    MonitoringItem displacementItem = monitoringItems.get(itemName);
                    if (displacementItem != null) {
                        // 濡傛灉瀛樺湪妗╅《绔栧悜浣嶇Щ鏁版嵁瀛樺偍瀵硅薄锛屼紭鍏堜娇鐢ㄥ畠
                        if (displacementItem.getPileDisplacementDataStorage() != null) {
                            displacementController.loadFromSettlementDataStorage(displacementItem.getPileDisplacementDataStorage());
                            System.out.println("宸蹭粠妗╅《绔栧悜浣嶇Щ鏁版嵁瀛樺偍瀵硅薄鍔犺浇鏁版嵁");
                        }
                        // 濡傛灉娌℃湁妗╅《绔栧悜浣嶇Щ鏁版嵁瀛樺偍瀵硅薄锛屼絾瀛樺湪娴嬮噺璁板綍锛屽垯浣跨敤娴嬮噺璁板綍
                        else if (displacementItem.getRecords() != null && !displacementItem.getRecords().isEmpty()) {
                            displacementController.loadFromMeasurementRecords(displacementItem.getRecords());
                            System.out.println("宸插姞杞芥々椤朵綅绉荤洃娴嬫暟鎹? " + displacementItem.getRecords().size() + " 鏉¤?");
                        }
                    }

                    // 淇濆瓨鎺у埗鍣ㄥ紩鐢ㄥ埌鑺傜偣鐨剈serData涓?紙渚夸簬鍚庣画鏌ユ壘锛?                    displacementRoot.setUserData(displacementController);

                    tab.setContent(displacementRoot);
                    break;
                case "閽㈡敮鎾戣酱鍔?:
                    // 鍔犺浇閽㈡敮鎾戣酱鍔涜鍥?                    FXMLLoader axialForceLoader = new FXMLLoader(getClass().getResource("/fxml/SteelSupportAxialForceView.fxml"));
                    Parent axialForceRoot = axialForceLoader.load();

                    // 鑾峰彇鎺у埗鍣ㄥ苟璁剧疆鏁版嵁
                    SteelSupportAxialForceController axialForceController = axialForceLoader.getController();
                    axialForceController.setStage(getStage());

                    // 濡傛灉瀛樺湪鐩稿簲鐨勭洃娴嬮」锛屽姞杞藉叾鏁版嵁
                    MonitoringItem axialForceItem = monitoringItems.get(itemName);
                    if (axialForceItem != null) {
                        // 濡傛灉瀛樺湪閽㈡敮鎾戣酱鍔涙暟鎹?                        if (axialForceItem.getSteelSupportAxialForceDataStorage() != null) {
                            axialForceController.loadFromSteelSupportAxialForceDataStorage(axialForceItem.getSteelSupportAxialForceDataStorage());
                            System.out.println("宸蹭粠閽㈡敮鎾戣酱鍔涙暟鎹?");
                        }
                        // 濡傛灉娌℃湁閽㈡敮鎾戣酱鍔涙暟鎹?                        else if (axialForceItem.getRecords() != null && !axialForceItem.getRecords().isEmpty()) {
                            axialForceController.loadFromMeasurementRecords(axialForceItem.getRecords());
                            System.out.println("宸插姞杞介挗鏀鏈夋湁鏇存敼鐨勯」鐩?);
                        }
                    }

                    // 淇濆瓨鎺у埗鍣ㄥ紩鐢ㄥ埌鑺傜偣鐨剈serData涓?紙渚夸簬鍚庣画鏌ユ壘锛?                    axialForceRoot.setUserData(axialForceController);

                    tab.setContent(axialForceRoot);
                    break;
                case "绔嬫煴绔栧悜浣嶇Щ":
                    // 鍔犺浇绔嬫煴绔栧悜浣嶇Щ瑙嗗浘
                    FXMLLoader columnLoader = new FXMLLoader(getClass().getResource("/fxml/ColumnDisplacementView.fxml"));
                    Parent columnRoot = columnLoader.load();

                    // 鑾峰彇鎺у埗鍣ㄥ苟璁剧疆鏁版嵁
                    ColumnDisplacementController columnController = columnLoader.getController();
                    columnController.setStage(getStage());

                    // 濡傛灉瀛樺湪鐩稿簲鐨勭洃娴嬮」锛屽姞杞藉叾鏁版嵁
                    MonitoringItem columnItem = monitoringItems.get(itemName);
                    if (columnItem != null) {
                        // 濡傛灉瀛樺湪绔嬫煴绔栧悜浣嶇Щ鏁版嵁瀛樺偍瀵硅薄锛屼紭鍏堜娇鐢ㄥ畠
                        if (columnItem.getColumnDisplacementDataStorage() != null) {
                            columnController.loadFromDataStorage(columnItem.getColumnDisplacementDataStorage());
                            System.out.println("宸蹭粠绔嬫煴绔栧悜浣嶇Щ鏁版嵁瀛樺偍瀵硅薄鍔犺浇鏁版嵁");
                        }
                        // 濡傛灉娌℃湁绔嬫煴绔栧悜浣嶇Щ鏁版嵁瀛樺偍瀵硅薄锛屼絾瀛樺湪娴嬮噺璁板綍锛屽垯浣跨敤娴嬮噺璁板綍
                        else if (columnItem.getRecords() != null && !columnItem.getRecords().isEmpty()) {
                            // 杞鏂规硶涓虹珛鏌变綅绉绘暟鎹?                            List<ColumnDisplacementData> displacementDataList = convertMeasurementRecordsToColumnDisplacementData(columnItem.getRecords());
                            LocalDateTime importTime = LocalDateTime.now();
                            columnController.getColumnDisplacementDataStorage().addDataBlock(importTime, displacementDataList, 
                                "瀵煎叆浜?" + importTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                            columnController.loadFromDataStorage(columnController.getColumnDisplacementDataStorage());
                            System.out.println("宸插姞杞界珛鏌辩珫鍚戜綅绉荤洃娴嬫暟鎹? " + columnItem.getRecords().size() + " 鏉¤?");
                        }
                    }

                    // 淇濆瓨鎺у埗鍣ㄥ紩鐢ㄥ埌鑺傜偣鐨剈serData涓?紙渚夸簬鍚庣画鏌ユ壘锛?                    columnRoot.setUserData(columnController);

                    tab.setContent(columnRoot);
                    break;
                case "娣遍儴姘村钩浣嶇Щ":
                    // 鍔犺浇娣遍儴姘村钩浣嶇Щ瑙嗗浘
                    FXMLLoader deepHorizontalLoader = new FXMLLoader(getClass().getResource("/fxml/DeepHorizontalDisplacementView.fxml"));
                    Parent deepHorizontalRoot = deepHorizontalLoader.load();

                    // 鑾峰彇鎺у埗鍣ㄥ苟璁剧疆鏁版嵁
                    DeepHorizontalDisplacementController deepHorizontalController = deepHorizontalLoader.getController();
                    deepHorizontalController.setStage(getStage());

                    // 濡傛灉瀛樺湪鐩稿簲鐨勭洃娴嬮」锛屽姞杞藉叾鏁版嵁
                    MonitoringItem deepHorizontalItem = monitoringItems.get(itemName);
                    if (deepHorizontalItem != null) {
                        // 濡傛灉瀛樺湪娣遍儴姘村钩浣嶇Щ鏁版嵁瀛樺偍瀵硅薄锛屼紭鍏堜娇鐢ㄥ畠
                        if (deepHorizontalItem.getDeepHorizontalDisplacementDataStorage() != null) {
                            deepHorizontalController.loadFromDeepHorizontalDisplacementDataStorage(deepHorizontalItem.getDeepHorizontalDisplacementDataStorage());
                            System.out.println("宸蹭粠娣遍儴姘村钩浣嶇Щ鏁版嵁瀛樺偍瀵硅薄鍔犺浇鏁版嵁");
                        }
                        // 濡傛灉娌℃湁娣遍儴姘村钩浣嶇Щ鏁版嵁瀛樺偍瀵硅薄锛屼絾瀛樺湪娴嬮噺璁板綍锛屽垯浣跨敤娴嬮噺璁板綍
                        else if (deepHorizontalItem.getRecords() != null && !deepHorizontalItem.getRecords().isEmpty()) {
                            deepHorizontalController.loadFromMeasurementRecords(deepHorizontalItem.getRecords());
                            System.out.println("宸插姞杞芥繁閮ㄦ按骞充綅绉荤洃娴嬫暟鎹? " + deepHorizontalItem.getRecords().size() + " 鏉¤?");
                        }
                    }

                    // 淇濆瓨鎺у埗鍣ㄥ紩鐢ㄥ埌鑺傜偣鐨剈serData涓?紙渚夸簬鍚庣画鏌ユ壘锛?                    deepHorizontalRoot.setUserData(deepHorizontalController);

                    tab.setContent(deepHorizontalRoot);
                    break;
                case "妗╅《姘村钩浣嶇Щ":
                    try {
                        // 鍔犺浇妗╅《姘村钩浣嶇Щ瑙嗗浘
                        FXMLLoader horizontalLoader = new FXMLLoader(getClass().getResource("/fxml/PileTopHorizontalDisplacementView.fxml"));
                        Parent horizontalRoot = horizontalLoader.load();
                        
                        // 鑾峰彇鎺у埗鍣ㄥ苟璁剧疆鏁版嵁
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
                        
                        // 璁剧疆鎺у埗鍣ㄤ綔涓虹敤鎴锋暟鎹?                        horizontalRoot.setUserData(horizontalController);
                        
                        tab.setContent(horizontalRoot);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Label errorLabel = new Label("鍔犺浇妗╅《姘村钩浣嶇Щ妯"潡鏃跺嚭閿欙細" + e.getMessage());
                        errorLabel.setWrapText(true);
                        errorLabel.setPadding(new Insets(10));
                        tab.setContent(errorLabel);
                    }
                    break;
                default:
                    // 榛樿鏄剧ず
                    Label defaultLabel = new Label(itemName + " - 鐩戞祴妯"潡 - 姝ｅ湪寮€鍙戜腑");
                    defaultLabel.setPadding(new Insets(20));
                    tab.setContent(defaultLabel);
                    break;
            }
        } catch (IOException e) {
            System.err.println("鏃犳硶鍔犺浇娴嬮」瑙嗗浘: " + e.getMessage());
            e.printStackTrace();

            // 鍒涘缓閿欒鎻愮ず
            Label errorLabel = new Label("鍔犺浇妯"潡鏃跺嚭閿? " + e.getMessage());
            errorLabel.setPadding(new Insets(20));
            tab.setContent(errorLabel);
        }

        // 娣诲姞鏍囩椤靛苟閫変腑
            editorTabPane.getTabs().add(tab);
            editorTabPane.getSelectionModel().select(tab);
    }
    
    /**
     * 浠庣洃娴嬬偣鍚嶇О鎺ㄦ柇绫诲瀷
     */
    private String getItemType(String itemName) {
        if (itemName.contains("娌夐檷") || itemName.contains("鍦拌〃鐐?)) {
            return "娌夐檷";
        } else if (itemName.contains("妗╅《姘村钩浣嶇Щ")) {
            return "妗╅《姘村钩浣嶇Щ";
        } else if (itemName.contains("娣遍儴姘村钩浣嶇Щ")) {
            return "娣遍儴姘村钩浣嶇Щ";
        } else if (itemName.contains("浣嶇Щ") || itemName.contains("妗╅《") || itemName.contains("绔嬫煴")) {
            return "浣嶇Щ";
        } else if (itemName.contains("姘翠綅")) {
            return "姘翠綅";
        } else if (itemName.contains("鍊炬枩")) {
            return "鍊炬枩";
        } else if (itemName.contains("搴斿姏")) {
            return "搴斿姏";
        } else if (itemName.contains("寤虹瓚鐗?)) {
            return "娌夐檷";
        } else if (itemName.contains("閽㈡敮鎾戣酱鍔?)) {
            return "閽㈡敮鎾戣酱鍔?;
        } else if (itemName.contains("鐮兼敮鎾戣酱鍔?)) {
            return "鐮兼敮鎾戣酱鍔?;
        } else {
        return "鍏朵粬";
        }
    }
    
    private void openReportTab(String reportName) {
        // 妫€鏌ユ槸鍚﹀凡缁忓瓨鍦ㄧ浉鍚屾爣棰樼殑鏍囩?        Tab existingTab = findTab(reportName);
        
        if (existingTab != null) {
            // 濡傛灉瀛樺湪锛屽垯閫変腑璇ユ爣绛鹃〉
            editorTabPane.getSelectionModel().select(existingTab);
        } else {
            // 濡傛灉涓嶅瓨鍦紝鍒欏垱寤烘柊鐨勬爣绛鹃〉
            Tab tab = new Tab(reportName);
            
            // 鍒涘缓鎶ヨ〃缂栬緫鍐呭鍖哄煙
            BorderPane content = new BorderPane();
            Label label = new Label("鎶ヨ〃" + reportName + " 鐨勭敓鎴愮晫闈?);
            label.setStyle("-fx-font-size: 16; -fx-padding: 20;");
            content.setCenter(label);
            
            tab.setContent(content);
            
            // 娣诲姞鍒版爣绛鹃〉骞堕€変腑
            editorTabPane.getTabs().add(tab);
            editorTabPane.getSelectionModel().select(tab);
        }
    }
    
    private void openOverviewTab(String overviewName) {
        // 妫€鏌ユ槸鍚﹀凡缁忓瓨鍦ㄧ浉鍚屾爣棰樼殑鏍囩?        Tab existingTab = findTab(overviewName);
        
        if (existingTab != null) {
            // 濡傛灉宸插瓨鍦紝閫変腑璇ユ爣绛鹃〉
            editorTabPane.getSelectionModel().select(existingTab);
        } else {
            // 鏍规嵁姒傝鏍戜互鍙嶆槧褰撳墠椤圭洰鍒楄〃
            switch (overviewName) {
                case "娌夐檷鐩戞祴":
                    // 鎵撳紑娌夐檷鐩戞祴瑙嗗浘锛屼娇鐢?姒傝鏍戜互"浣滀负椤圭洰鍚?                    final String projectName = "姒傝";
                    openSubsidenceView(projectName);
                    break;
                case "椤圭洰缁熻鏁版嵁":
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
        // 璁剧疆鎷栨嫿鍔熻兘
        titleLabel.setOnMousePressed(this::handleMousePressed);
        titleLabel.setOnMouseDragged(this::handleMouseDragged);
        appIcon.setOnMousePressed(this::handleMousePressed);
        appIcon.setOnMouseDragged(this::handleMouseDragged);
        
        // 璁剧疆绐楀彛鎺у埗鎸夐挳
        minimizeButton.setOnAction(event -> minimizeWindow());
        maximizeButton.setOnAction(event -> maximizeOrRestoreWindow());
        closeButton.setOnAction(event -> closeWindow());
        
        // 鍙屽嚮鏍囬鏍忔渶澶у寲/杩樺師绐楀彛
        titleLabel.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                maximizeOrRestoreWindow();
            }
        });
    }
    
    private void setupResizeHandling() {
        // 璁剧疆绐楀彛杈圭紭璋冩暣澶у皬鍔熻兘
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
        
        // 妫€娴嬮紶鏍囨槸鍚﹀湪绐楀彛杈圭紭
        boolean left = mouseX < RESIZE_PADDING;
        boolean right = mouseX > width - RESIZE_PADDING;
        boolean top = mouseY < RESIZE_PADDING;
        boolean bottom = mouseY > height - RESIZE_PADDING;
        
        // 鏍规嵁榧犳爣浣嶇疆璁剧疆鍏夋爣鏍峰紡
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
        
        // 妫€娴嬮紶鏍囨槸鍚﹀湪绐楀彛杈圭紭
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
            
            // 璋冩暣瀹藉害鍜屼綅缃?            if (resizeLeft) {
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
            
            // 璋冩暣楂樺害鍜屼綅缃?            if (resizeTop) {
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
            return; // 濡傛灉绐楀彛宸叉渶澶у寲锛屼笉鍏佽鎷栨嫿
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
            // 璁颁綇绐楀彛鎭㈠鍓嶇殑澶у皬鍜屼綅缃?            double screenWidth = screenBounds != null ? screenBounds.getWidth() : Screen.getPrimary().getVisualBounds().getWidth();
            double screenHeight = screenBounds != null ? screenBounds.getHeight() : Screen.getPrimary().getVisualBounds().getHeight();
            
            // 璁剧疆绐楀彛澶у皬涓哄睆骞曞彲瑙嗗尯鍩熷ぇ灏忥紙涓嶈鐩栦换鍔℃爮锛?            stage.setX(0);
            stage.setY(0);
            stage.setWidth(screenWidth);
            stage.setHeight(screenHeight);
            
            maximizeButton.setText("鉂?");
        } else {
            // 鎭㈠鍒伴粯璁ゅぇ灏?            stage.setWidth(800);
            stage.setHeight(600);
            stage.centerOnScreen();
            maximizeButton.setText("鈻?);
        }
    }

    private void closeWindow() {
        // 妫€鏌ユ槸鍚︽湁鏈繚瀛樼殑椤圭洰
        List<ProjectInfo> unsavedProjects = new ArrayList<>();
        for (ProjectInfo project : projects.values()) {
            if (hasUnsavedChanges(project)) {
                unsavedProjects.add(project);
            }
        }

        if (!unsavedProjects.isEmpty()) {
            // 濡傛灉鏈夋湭淇濆瓨鐨勯」鐩紝鎻愮ず鐢ㄦ埛鏄惁淇濆瓨
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("鏈繚瀛樼殑鏇存敼");
            alert.setHeaderText("鏈? + unsavedProjects.size() + "涓」鐩寘鍚湭淇濆瓨鐨勬洿鏀?);

            StringBuilder content = new StringBuilder("鏄惁鍦ㄩ€€鍑哄墠淇濆瓨浠ヤ笅椤圭洰锛焅n");
            for (ProjectInfo project : unsavedProjects) {
                content.append("- ").append(project.getName()).append("\n");
            }
            alert.setContentText(content.toString());

            ButtonType saveButtonType = new ButtonType("淇濆瓨骞堕€€鍑?);
            ButtonType exitButtonType = new ButtonType("鐩存帴閫€鍑?);
            ButtonType cancelButtonType = new ButtonType("鍙栨秷", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(saveButtonType, exitButtonType, cancelButtonType);

            alert.showAndWait().ifPresent(response -> {
                if (response == saveButtonType) {
                    // 淇濆瓨鎵€鏈夋湭淇濆瓨鐨勯」鐩?                    for (ProjectInfo project : unsavedProjects) {
                        saveProject(project);
                    }
                    getStage().close();
                } else if (response == exitButtonType) {
                    // 鐩存帴閫€鍑?        getStage().close();
                }
                // 濡傛灉鏄彇娑堬紝涓嶅仛浠讳綍鎿嶄綔
            });
        } else {
            // 濡傛灉娌℃湁鏈繚瀛樼殑椤圭洰锛岀洿鎺ラ€€鍑?            getStage().close();
        }
    }

    private Stage getStage() {
        return (Stage) mainPane.getScene().getWindow();
    }

    private void handleNewProject() {
        System.out.println("鏂板缓椤圭洰...");

        // 鍒涘缓骞舵樉绀洪」鐩璇濇
        ProjectDialog dialog = new ProjectDialog();
        dialog.setHeaderText("璇疯緭鍏ユ柊椤圭洰鐨勮缁嗕俊鎭?);

        // 鏄剧ず瀵硅瘽妗嗗苟绛夊緟鐢ㄦ埛鍝嶅簲
        dialog.showAndWait().ifPresent(projectInfo -> {
            // 鐢熸垚鍞竴ID
            String projectId = "P" + System.currentTimeMillis();
            projectInfo.setId(projectId);

            // 淇濆瓨椤圭洰淇℃伅
            projects.put(projectId, projectInfo);

            // 鍒涘缓椤圭洰鑺傜偣
            TreeItem<String> projectNode = new TreeItem<>(projectInfo.getName());
            projectNode.setExpanded(true);

            // 鍒涘缓娴嬮」瀛愯妭鐐?            for (String itemName : projectInfo.getMonitoringItems()) {
                // 涓烘瘡涓祴椤瑰垱寤哄瓙鑺傜偣
                TreeItem<String> itemNode = new TreeItem<>(itemName);
                projectNode.getChildren().add(itemNode);

                // 鍒涘缓鐩稿簲鐨勭洃娴嬮」瀵硅薄
                MonitoringItem item = new MonitoringItem(
                        itemName.replaceAll("\\s+", ""),  // 绉婚櫎绌烘牸浣滀负ID
                        itemName,  // 鍚嶇О
                        getItemType(itemName),  // 绫诲瀷
                        projectInfo.getName() + " - " + itemName  // 浣嶇疆
                );
                monitoringItems.put(getFullItemName(projectInfo.getName(), itemName), item);
            }

            // 灏嗛」鐩坊鍔犲埌宸ョ▼鏍戜腑
            TreeItem<String> projectRoot = projectTreeView.getRoot();
            projectRoot.getChildren().add(projectNode);
            
            // 鍒锋柊鐩戞祴姒傚喌鏍戜互鍖呭惈鏂伴」鐩?            refreshOverviewTree();

            // 鑾峰彇鐢ㄦ埛鍦ㄥ璇濇涓€夋嫨鐨勬枃浠?            File selectedFile = projectInfo.getProjectFile();

            // 淇濆瓨椤圭洰鍒版枃浠?            boolean success = ProjectFileUtil.saveProject(projectInfo, monitoringItems, selectedFile);

            if (success) {
                // 纭繚椤圭洰鏂囦欢璺緞琚纭缃?                projectInfo.setProjectFile(selectedFile);
            }

            if (success) {
                // 鎻愮ず淇濆瓨鎴愬姛
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("椤圭洰鍒涘缓鎴愬姛");
                alert.setHeaderText(null);
                alert.setContentText("椤圭洰 " + projectInfo.getName() + " 宸叉垚鍔熷垱寤哄苟淇濆瓨鍒版枃浠讹細\n" + selectedFile.getAbsolutePath());
                alert.showAndWait();

                // 鑷姩鎵撳紑椤圭洰鏍囩椤?                openProjectTab(projectInfo.getName());
            } else {
                // 鎻愮ず淇濆瓨澶辫触
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("淇濆瓨澶辫触");
                alert.setHeaderText("鏃犳硶淇濆瓨椤圭洰");
                alert.setContentText("淇濆瓨椤圭洰 " + projectInfo.getName() + " 鍒版枃浠舵椂鍑洪敊銆?);
                alert.showAndWait();
            }
        });
    }

    private void handleOpenProject() {
        System.out.println("鎵撳紑椤圭洰...");

        // 鍒涘缓鏂囦欢閫夋嫨鍣?        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("鎵撳紑宸ョ▼鐩戞祴椤圭洰");
        fileChooser.getExtensionFilters().add(
            new ExtensionFilter("宸ョ▼鐩戞祴椤圭洰鏂囦欢", "*.jc")
        );

        // 鏄剧ず鎵撳紑鏂囦欢瀵硅瘽妗?        File selectedFile = fileChooser.showOpenDialog(getStage());

        if (selectedFile != null) {
            // 浠庢枃浠跺姞杞介」鐩?            ProjectFileData data = ProjectFileUtil.loadProject(selectedFile);

            if (data != null && data.projectInfo != null) {
                // 妫€鏌ユ槸鍚﹀凡瀛樺湪鍚屽悕椤圭洰
                boolean exists = false;
                String projectName = data.projectInfo.getName();

                for (ProjectInfo existingProject : projects.values()) {
                    if (existingProject.getName().equals(projectName)) {
                        exists = true;
                        break;
                    }
                }

                if (exists) {
                    // 鏄剧ず椤圭洰宸插瓨鍦ㄧ殑鎻愮ず
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("椤圭洰宸插瓨鍦?);
                    alert.setHeaderText("宸插瓨鍦ㄥ悓鍚嶉」鐩? " + projectName);
                    alert.setContentText("鏄惁瑕佹浛鎹㈢幇鏈夐」鐩紵");

                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            // 鐢ㄦ埛鍚屾剰鏇挎崲锛屾墽琛屽姞杞?                            loadProjectData(data, selectedFile);
                        }
                    });
                } else {
                    // 鐩存帴鍔犺浇椤圭洰
                    loadProjectData(data, selectedFile);
                }
            } else {
                // 鍔犺浇澶辫触锛屾樉绀洪敊璇俊鎭?                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("鍔犺浇澶辫触");
                alert.setHeaderText("鏃犳硶鍔犺浇椤圭洰鏂囦欢");
                alert.setContentText("鎵€閫夋枃浠跺彲鑳藉凡鎹熷潖鎴栦笉鏄湁鏁堢殑椤圭洰鏂囦欢銆?);
                alert.showAndWait();
            }
        }
    }

    /**
     * 鍔犺浇椤圭洰鏁版嵁鍒扮▼搴忎腑
     */
    private void loadProjectData(ProjectFileData data, File file) {
        // 鐢熸垚椤圭洰ID锛堝鏋滄病鏈夛級
        if (data.projectInfo.getId() == null || data.projectInfo.getId().isEmpty()) {
            data.projectInfo.setId("P" + System.currentTimeMillis());
        }
        
        // 璁板綍椤圭洰ID鐢ㄤ簬璋冭瘯
        String projectId = data.projectInfo.getId();
        System.out.println("鍔犺浇椤圭洰锛? + data.projectInfo.getName() + "锛孖D锛? + projectId);

        // 鏇存柊椤圭洰鏂囦欢寮曠敤
        data.projectInfo.setProjectFile(file);

        // 淇濆瓨椤圭洰淇℃伅
        projects.put(projectId, data.projectInfo);

        // 灏嗙洃娴嬮」娣诲姞鍒版暟鎹瓨鍌?        if (data.monitoringItems != null) {
            monitoringItems.putAll(data.monitoringItems);
            
            // 妫€鏌ユ槸鍚︽湁娣遍儴姘村钩浣嶇Щ鏁版嵁
            for (Map.Entry<String, MonitoringItem> entry : data.monitoringItems.entrySet()) {
                if (entry.getKey().contains("娣遍儴姘村钩浣嶇Щ")) {
                    MonitoringItem item = entry.getValue();
                    if (item.getDeepHorizontalDisplacementDataStorage() != null) {
                        DeepHorizontalDisplacementDataStorage storage = item.getDeepHorizontalDisplacementDataStorage();
                        System.out.println("宸插姞杞芥繁閮ㄦ按骞充綅绉绘暟鎹? 娴嬬偣鏁伴噺=" + 
                                (storage.getPoints() != null ? storage.getPoints().size() : 0) + 
                                ", 鏁版嵁鍧楁暟閲?" + storage.getDataBlockCount());
                    } else {
                        System.out.println("璀﹀憡: 娣遍儴姘村钩浣嶇Щ椤圭洰娌℃湁鍏宠仈鐨勬暟鎹瓨鍌ㄥ璞?);
                    }
                }
            }
        }

        // 澶勭悊棰勮淇℃伅鏁版嵁锛堝鏋滃瓨鍦級骞跺瓨鍏rojectWarningMap
        if (data.warningInfoList != null && !data.warningInfoList.isEmpty()) {
            System.out.println("椤圭洰鍖呭惈 " + data.warningInfoList.size() + " 鏉￠璀︿俊鎭褰?);
            // 瀛樺偍棰勮淇℃伅鍒皃rojectWarningMap
            projectWarningMap.put(projectId, new ArrayList<>(data.warningInfoList));
            System.out.println("宸插皢棰勮淇℃伅瀛樺叆projectWarningMap锛岄」鐩甀D: " + projectId);
        }

        // 鍒涘缓椤圭洰鑺傜偣骞舵坊鍔犲埌鏍戜腑
        TreeItem<String> projectNode = new TreeItem<>(data.projectInfo.getName());
        projectNode.setExpanded(true);

        // 涓洪」鐩腑鐨勬瘡涓祴椤瑰垱寤哄瓙鑺傜偣
        for (String itemName : data.projectInfo.getMonitoringItems()) {
            TreeItem<String> itemNode = new TreeItem<>(itemName);
            projectNode.getChildren().add(itemNode);
        }

        // 娣诲姞鍒板伐绋嬫爲
        TreeItem<String> projectRoot = projectTreeView.getRoot();
        projectRoot.getChildren().add(projectNode);
        
        // 鍒锋柊鐩戞祴姒傚喌鏍戜互鍖呭惈鏂伴」鐩?        refreshOverviewTree();

        // 鎻愮ず鍔犺浇鎴愬姛
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("鍔犺浇鎴愬姛");
        alert.setHeaderText(null);
        alert.setContentText("椤圭洰 " + data.projectInfo.getName() + " 宸叉垚鍔熷姞杞絓n鏂囦欢璺緞锛? + file.getAbsolutePath());
        alert.showAndWait();

        // 鎵撳紑椤圭洰鏍囩椤?        openProjectTab(data.projectInfo.getName());

        System.out.println("椤圭洰 " + data.projectInfo.getName() + " 鍔犺浇鎴愬姛");
    }
    
    /**
     * 灏嗛璀︿俊鎭姞杞藉埌鎸囧畾鐨勬爣绛鹃〉
     * @param tab 瑕佸姞杞界殑鏍囩椤?     * @param projectInfo 椤圭洰淇℃伅
     * @param warningInfoList 棰勮淇℃伅鍒楄〃
     */
    private void loadWarningInfoIntoTab(Tab tab, ProjectInfo projectInfo, List<WarningInfo> warningInfoList) {
        try {
            Parent content = (Parent) tab.getContent();
            WarningInfoController controller = getWarningInfoController(content);
            
            if (controller != null) {
                controller.loadWarningData(projectInfo, warningInfoList);
                System.out.println("宸插姞杞?" + warningInfoList.size() + " 鏉￠璀︿俊鎭褰?);
            } else {
                System.err.println("鏃犳硶鑾峰彇棰勮淇℃伅鎺у埗鍣?);
            }
        } catch (Exception e) {
            System.err.println("鍔犺浇棰勮淇℃伅鍒版爣绛鹃〉鏃跺嚭閿? " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 鑾峰彇棰勮淇℃伅鎺у埗鍣?     * @param content 鍐呭鑺傜偣
     * @return 棰勮淇℃伅鎺у埗鍣紝濡傛灉鎵句笉鍒板垯杩斿洖null
     */
    private WarningInfoController getWarningInfoController(Node content) {
        if (content == null) {
            return null;
        }
        
        // 灏濊瘯浠嶣orderPane涓幏鍙?        if (content instanceof BorderPane) {
            BorderPane pane = (BorderPane) content;
            
            // 鑾峰彇BorderPane鐨勬帶鍒跺櫒
            Object controller = pane.getUserData();
            if (controller instanceof WarningInfoController) {
                return (WarningInfoController) controller;
            }
        }
        
        // 灏濊瘯鏌ユ壘鍐呴儴鑺傜偣
        if (content instanceof Parent) {
            Parent parent = (Parent) content;
            
            // 濡傛灉鏄疭crollPane锛屾鏌ュ叾鍐呭
            if (parent instanceof ScrollPane) {
                ScrollPane scrollPane = (ScrollPane) parent;
                Node scrollContent = scrollPane.getContent();
                if (scrollContent instanceof Parent) {
                    return getWarningInfoController(scrollContent);
                }
            }
            
            // 灏濊瘯閬嶅巻瀛愯妭鐐?            for (Node child : parent.getChildrenUnmodifiable()) {
                if (child instanceof BorderPane) {
                    WarningInfoController controller = getWarningInfoController(child);
                    if (controller != null) {
                        return controller;
                    }
                }
            }
        }
        
        // 杩斿洖null琛ㄧず鏈壘鍒?        return null;
    }

    /**
     * 妫€鏌ラ」鐩槸鍚︽湁鏈繚瀛樼殑鏇存敼
     * @param project 瑕佹鏌ョ殑椤圭洰
     * @return 濡傛灉鏈夋湭淇濆瓨鐨勬洿鏀硅繑鍥瀟rue
     */
    private boolean hasUnsavedChanges(ProjectInfo project) {
        // 濡傛灉椤圭洰娌℃湁鍏宠仈鏂囦欢锛屽垯璁や负鏈夋湭淇濆瓨鐨勬洿鏀?        if (project.getProjectFile() == null) {
            return true;
        }

        // 濡傛灉鏂囦欢涓嶅瓨鍦紝鍒欒涓烘湁鏈繚瀛樼殑鏇存敼
        if (!project.getProjectFile().exists()) {
            return true;
        }

        // 妫€鏌ユ槸鍚︽湁鎵撳紑鐨勬爣绛鹃〉鍖呭惈璇ラ」鐩殑鏁版嵁
        for (Tab tab : editorTabPane.getTabs()) {
            if (tab.getText().contains(project.getName()) && 
                (tab.getText().contains("鍦拌〃鐐规矇闄?) || 
                 tab.getText().contains("妗╅《绔栧悜浣嶇Щ") || 
                 tab.getText().contains("绔嬫煴绔栧悜浣嶇Щ"))) {
                // 濡傛灉鏈夋墦寮€鐨勭洃娴嬫暟鎹爣绛鹃〉锛屽垯璁や负鍙兘鏈夋湭淇濆瓨鐨勬洿鏀?                return true;
            }
        }

        return false;
    }

    /**
     * 淇濆瓨鎵€鏈夐」鐩暟鎹?     * @param projectToSave 瑕佷繚瀛樼殑椤圭洰
     */
    private void saveAllProjectData(ProjectInfo projectToSave) {
        // 棣栧厛浠庢墦寮€鐨勬爣绛鹃〉鏀堕泦鏁版嵁
        collectSettlementDataFromOpenTabs(projectToSave);

        // 鐒跺悗纭繚鎵€鏈夌洃娴嬮」閮芥纭叧鑱斿埌椤圭洰
        String projectName = projectToSave.getName();
        for (String itemName : projectToSave.getMonitoringItems()) {
            String fullItemName = getFullItemName(projectName, itemName);
            MonitoringItem item = monitoringItems.get(fullItemName);
            
            if (item == null) {
                // 濡傛灉涓嶅瓨鍦紝鍒涘缓鏂扮殑鐩戞祴椤?                item = new MonitoringItem(
                    itemName.replaceAll("\\s+", ""),  // 绉婚櫎绌烘牸浣滀负ID
                    itemName,  // 鍚嶇О
                    getItemType(itemName),  // 绫诲瀷
                    projectName + " - " + itemName  // 浣嶇疆
                );
                monitoringItems.put(fullItemName, item);
            }
            
            // 鐗瑰埆澶勭悊娣遍儴姘村钩浣嶇Щ鏁版嵁 - 鍗充娇鏍囩椤垫湭鎵撳紑锛屼篃纭繚鏁版嵁瀛樺偍瀵硅薄琚纭繚瀛?            if (itemName.equals("娣遍儴姘村钩浣嶇Щ")) {
                if (item.getDeepHorizontalDisplacementDataStorage() != null) {
                    // 宸茬粡鏈夋暟鎹瓨鍌ㄥ璞★紝纭繚瀹冭淇濈暀
                    System.out.println("纭繚淇濆瓨宸叉湁鐨勬繁閮ㄦ按骞充綅绉绘暟鎹? " + item.getName());
                    
                    // 纭繚涔熶繚瀛樹簡DataStorage涓殑娴嬬偣閰嶇疆鍜屾暟鎹潡
                    DeepHorizontalDisplacementDataStorage storage = item.getDeepHorizontalDisplacementDataStorage();
                    if (storage.getDataBlockCount() > 0) {
                        System.out.println("淇濆瓨娣遍儴姘村钩浣嶇Щ鏁版嵁鍧楁暟閲? " + storage.getDataBlockCount());
                    }
                } else {
                    // 濡傛灉娌℃湁鏁版嵁瀛樺偍瀵硅薄锛屽垱寤轰竴涓┖鐨?                    System.out.println("涓烘繁閮ㄦ按骞充綅绉诲垱寤烘柊鐨勬暟鎹瓨鍌ㄥ璞?);
                    item.setDeepHorizontalDisplacementDataStorage(new DeepHorizontalDisplacementDataStorage());
                }
            }
        }
    }

    private void handleSaveProject() {
        System.out.println("淇濆瓨椤圭洰...");

        // 鑾峰彇褰撳墠閫変腑鐨勬爣绛鹃〉
        Tab selectedTab = editorTabPane.getSelectionModel().getSelectedItem();

        // 濡傛灉娌℃湁閫変腑鏍囩椤碉紝鎴栬€呴€変腑鐨勪笉鏄」鐩爣绛鹃〉锛屽垯鎻愮ず鐢ㄦ埛鍏堥€夋嫨涓€涓」鐩?        if (selectedTab == null) {
            showSelectProjectDialog();
            return;
        }

        // 灏濊瘯鎵惧埌瀵瑰簲鐨勯」鐩?        ProjectInfo projectToSave = null;
        String projectName = selectedTab.getText();

        // 妫€鏌ユ槸鍚︽槸宸茬煡椤圭洰鍚嶇О
        projectToSave = findProjectByName(projectName);

        // 濡傛灉娌℃湁鎵惧埌锛屽彲鑳芥爣绛鹃〉鍚嶇О鍖呭惈浜嗘祴椤瑰悕绉帮紝灏濊瘯浠庢爲涓煡鎵?        if (projectToSave == null) {
            TreeItem<String> projectRoot = projectTreeView.getRoot();
            for (TreeItem<String> projectNode : projectRoot.getChildren()) {
                // 妫€鏌ユ槸鍚︽槸姝ら」鐩殑娴嬮」鏍囩椤?                String currentProjectName = projectNode.getValue();
                if (projectName.startsWith(currentProjectName + " - ")) {
                    projectToSave = findProjectByName(currentProjectName);
                    break;
                }
            }
        }

        // 濡傛灉浠嶆湭鎵惧埌锛屾彁绀虹敤鎴烽€夋嫨椤圭洰
        if (projectToSave == null) {
            showSelectProjectDialog();
            return;
        }

        // 鍦ㄤ繚瀛樺墠锛屾敹闆嗘墍鏈夐」鐩暟鎹?        saveAllProjectData(projectToSave);

        // 妫€鏌ラ」鐩槸鍚﹀凡缁忔湁鍏宠仈鐨勬枃浠?        File selectedFile = null;
        if (projectToSave.getProjectFile() != null && projectToSave.getProjectFile().exists()) {
            // 浣跨敤宸叉湁鐨勬枃浠?            selectedFile = projectToSave.getProjectFile();
            System.out.println("鑷姩淇濆瓨椤圭洰鍒? " + selectedFile.getAbsolutePath());
        } else {
            // 鍒涘缓鏂囦欢閫夋嫨鍣?            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("淇濆瓨宸ョ▼鐩戞祴椤圭洰");
            fileChooser.getExtensionFilters().add(
                new ExtensionFilter("宸ョ▼鐩戞祴椤圭洰鏂囦欢", "*.jc")
            );
            fileChooser.setInitialFileName(projectToSave.getName() + ".jc");

            // 鏄剧ず淇濆瓨鏂囦欢瀵硅瘽妗?            selectedFile = fileChooser.showSaveDialog(getStage());
        }

        if (selectedFile != null) {
            try {
                // 鑾峰彇浠庨璀︿俊鎭爣绛鹃〉鏀堕泦鐨勯璀︽暟鎹?                List<WarningInfo> collectedWarnings = projectWarningMap.get(projectToSave.getId());
                
                // 濡傛灉娌℃湁鏀堕泦鍒伴璀︽暟鎹紝灏濊瘯浠庡叾浠栨爣绛鹃〉鑾峰彇
                if (collectedWarnings == null) {
                    collectedWarnings = getWarningDataForProject(projectToSave);
                }
                
                // 纭繚棰勮淇℃伅鍒楄〃涓嶄负null
                List<WarningInfo> warningInfoList = collectedWarnings != null ? 
                    collectedWarnings : new ArrayList<>();
                
                if (warningInfoList.size() > 0) {
                    System.out.println("淇濆瓨椤圭洰鏃跺寘鍚?" + warningInfoList.size() + " 鏉￠璀︿俊鎭?);
                }
                
                // 淇濆瓨椤圭洰鍒版枃浠讹紝鍖呮嫭棰勮淇℃伅
                boolean success = ProjectFileUtil.saveProject(projectToSave, monitoringItems, warningInfoList, selectedFile);

            if (success) {
                // 鏇存柊椤圭洰鏂囦欢璺緞
                projectToSave.setProjectFile(selectedFile);

                // 鎻愮ず淇濆瓨鎴愬姛
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("淇濆瓨鎴愬姛");
                alert.setHeaderText(null);
                alert.setContentText("椤圭洰 " + projectToSave.getName() + " 宸叉垚鍔熶繚瀛樺埌鏂囦欢锛歕n" + selectedFile.getAbsolutePath());
                alert.showAndWait();
            } else {
                // 鎻愮ず淇濆瓨澶辫触
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("淇濆瓨澶辫触");
                alert.setHeaderText("鏃犳硶淇濆瓨椤圭洰");
                alert.setContentText("淇濆瓨椤圭洰 " + projectToSave.getName() + " 鍒版枃浠舵椂鍑洪敊銆?);
                alert.showAndWait();
                }
            } catch (Exception e) {
                // 鎻愮ず淇濆瓨澶辫触
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("淇濆瓨澶辫触");
                alert.setHeaderText("淇濆瓨椤圭洰鏃跺彂鐢熼敊璇?);
                alert.setContentText("閿欒淇℃伅: " + e.getMessage());
                alert.showAndWait();
                e.printStackTrace();
            }
        }
    }

    /**
     * 浠庢墍鏈夋墦寮€鐨勬矇闄嶆暟鎹爣绛鹃〉涓敹闆嗘暟鎹紝骞跺瓨鍌ㄥ埌鐩稿簲鐨勭洃娴嬮」涓?     *
     * @param project 褰撳墠椤圭洰
     */
    private void collectSettlementDataFromOpenTabs(ProjectInfo project) {
        for (Tab tab : editorTabPane.getTabs()) {
            String tabTitle = tab.getText();
            
            // 鍙鐞嗗綋鍓嶉」鐩殑鏍囩椤?            if (tabTitle.startsWith(project.getName() + " - ") || tabTitle.equals("棰勮淇℃伅") || tabTitle.equals(project.getName() + " - 棰勮淇℃伅")) {
                // 鏍规嵁鏍囩鍐呭鎻愬彇鎺у埗鍣ㄥ苟鏀堕泦鏁版嵁
                Parent tabContent = (Parent) tab.getContent();
                if (tabContent != null) {
                    findAndCollectFromController(tabContent, project);
                }
            }
        }
    }

    private void findAndCollectFromController(Parent parent, ProjectInfo project) {
                        // 灏濊瘯浠巙serData鑾峰彇鎺у埗鍣?        Object userData = parent.getUserData();

        // 濡傛灉userData鏄帶鍒跺櫒锛岀洿鎺ュ鐞?        if (userData instanceof SettlementDataController) {
            SettlementDataController controller = (SettlementDataController) userData;
            
            // 妫€鏌ユ槸鍚︽湁鏇存敼
            if (controller.hasDataChanged()) {
                // 鑾峰彇鎵€鏈夋祴閲忚褰?                List<MeasurementRecord> records = controller.getMeasurementRecordsForSaving();
                
                // 鑾峰彇鎴栧垱寤虹洃娴嬮」
                String itemName = "鍦拌〃鐐规矇闄?;
                String fullItemName = getFullItemName(project.getName(), itemName);
                MonitoringItem item = monitoringItems.get(fullItemName);
                
                if (item == null) {
                    item = new MonitoringItem();
                    item.setId(UUID.randomUUID().toString());
                    item.setName(itemName);
                    item.setType("娌夐檷");
                    item.setLocation(project.getName() + " - " + itemName);
                    monitoringItems.put(fullItemName, item);
                            }

                // 鑾峰彇鏁版嵁瀛樺偍瀵硅薄
                SettlementDataStorage storage = controller.getSettlementDataStorage();
                
                // 鏇存柊鐩戞祴椤规暟鎹?                item.setRecords(records);
                item.setSettlementDataStorage(storage);
                
                // 娣诲姞鍒伴」鐩腑
                project.addMonitoringItem(itemName);
                
                System.out.println("宸叉敹闆嗘矇闄嶇洃娴嬫暟鎹? " + records.size() + " 鏉¤褰?);
                        } else {
                System.out.println("娌夐檷鐩戞祴鏁版嵁鏈洿鏀癸紝涓嶉渶瑕佷繚瀛?);
            }
        }
        // 澶勭悊WarningInfoController
        else if (userData instanceof WarningInfoController) {
            WarningInfoController controller = (WarningInfoController) userData;
            
            // 鑾峰彇棰勮鏁版嵁
            List<WarningInfo> warningInfoList = controller.getWarningDataList();
            
            if (warningInfoList != null && !warningInfoList.isEmpty()) {
                // 灏嗛璀︽暟鎹笌椤圭洰鍏宠仈璧锋潵锛屼絾涓嶇洿鎺ヤ繚瀛?                // 淇濆瓨鎿嶄綔灏嗙敱saveProject缁熶竴澶勭悊
                projectWarningMap.put(project.getId(), warningInfoList);
                System.out.println("宸叉敹闆嗛璀︿俊鎭暟鎹? " + warningInfoList.size() + " 鏉¤褰?);
            } else {
                System.out.println("棰勮淇℃伅鏁版嵁涓虹┖锛屼笉闇€瑕佷繚瀛?);
            }
        }
        // 澶勭悊PileTopDisplacementController
        else if (userData instanceof PileTopDisplacementController) {
            PileTopDisplacementController controller = (PileTopDisplacementController) userData;
            
            // 鏀堕泦妗╅《浣嶇Щ鏁版嵁...
            if (controller.hasDataChanged()) {
                List<MeasurementRecord> records = controller.getMeasurementRecordsForSaving();
                
                String itemName = "妗╅《绔栧悜浣嶇Щ";
                String fullItemName = getFullItemName(project.getName(), itemName);
                MonitoringItem item = monitoringItems.get(fullItemName);

                if (item == null) {
                    item = new MonitoringItem();
                    item.setId(UUID.randomUUID().toString());
                    item.setName(itemName);
                    item.setType("浣嶇Щ");
                    item.setLocation(project.getName() + " - " + itemName);
                    monitoringItems.put(fullItemName, item);
                }
                
                // 鑾峰彇鏁版嵁瀛樺偍瀵硅薄
                PileDisplacementDataStorage storage = controller.getSettlementDataStorage();
                
                // 鏇存柊鐩戞祴椤规暟鎹?                item.setRecords(records);
                item.setPileDisplacementDataStorage(storage);
                
                // 娣诲姞鍒伴」鐩腑
                project.addMonitoringItem(itemName);
                
                System.out.println("宸叉敹闆嗘々椤朵綅绉荤洃娴嬫暟鎹? " + records.size() + " 鏉¤褰?);
            }
        }
        // 澶勭悊ColumnDisplacementController
        else if (userData instanceof ColumnDisplacementController) {
            ColumnDisplacementController controller = (ColumnDisplacementController) userData;

            // 鏀堕泦绔嬫煴浣嶇Щ鏁版嵁
            List<MeasurementRecord> records = controller.getMeasurementRecordsForSaving();
            
            String itemName = "绔嬫煴绔栧悜浣嶇Щ";
            String fullItemName = getFullItemName(project.getName(), itemName);
            MonitoringItem item = monitoringItems.get(fullItemName);
            
            if (item == null) {
                item = new MonitoringItem();
                item.setId(UUID.randomUUID().toString());
                item.setName(itemName);
                item.setType("浣嶇Щ");
                item.setLocation(project.getName() + " - " + itemName);
                monitoringItems.put(fullItemName, item);
                            }

            // 鑾峰彇鏁版嵁瀛樺偍瀵硅薄
            ColumnDisplacementDataStorage storage = controller.getColumnDisplacementDataStorage();
            
            // 鏇存柊鐩戞祴椤规暟鎹?            item.setRecords(records);
            item.setColumnDisplacementDataStorage(storage);
            
            // 娣诲姞鍒伴」鐩腑
            project.addMonitoringItem(itemName);
            
            System.out.println("宸叉敹闆嗙珛鏌变綅绉荤洃娴嬫暟鎹? " + records.size() + " 鏉¤褰?);
        }
        // 澶勭悊GroundwaterLevelController
        else if (userData instanceof GroundwaterLevelController) {
            GroundwaterLevelController controller = (GroundwaterLevelController) userData;
            
            // 鏀堕泦鍦颁笅姘翠綅鏁版嵁
                    List<MeasurementRecord> records = controller.getMeasurementRecordsForSaving();
            
            String itemName = "鍦颁笅姘翠綅";
            String fullItemName = getFullItemName(project.getName(), itemName);
            MonitoringItem item = monitoringItems.get(fullItemName);
            
            if (item == null) {
                item = new MonitoringItem();
                item.setId(UUID.randomUUID().toString());
                item.setName(itemName);
                item.setType("姘翠綅");
                item.setLocation(project.getName() + " - " + itemName);
                monitoringItems.put(fullItemName, item);
                    }

            // 鑾峰彇鏁版嵁瀛樺偍瀵硅薄
            GroundwaterLevelDataStorage storage = controller.getGroundwaterLevelDataStorage();
            
            // 鏇存柊鐩戞祴椤规暟鎹?            item.setRecords(records);
            item.setGroundwaterLevelDataStorage(storage);
            
            // 娣诲姞鍒伴」鐩腑
            project.addMonitoringItem(itemName);
            
            System.out.println("宸叉敹闆嗗湴涓嬫按浣嶇洃娴嬫暟鎹? " + records.size() + " 鏉¤褰?);
                }
        // 澶勭悊BuildingSettlementController
        else if (userData instanceof BuildingSettlementController) {
            BuildingSettlementController controller = (BuildingSettlementController) userData;
            
            // 鏀堕泦寤虹瓚鐗╂矇闄嶆暟鎹?                    List<MeasurementRecord> records = controller.getMeasurementRecordsForSaving();
            
            String itemName = "寤虹瓚鐗╂矇闄?;
            String fullItemName = getFullItemName(project.getName(), itemName);
            MonitoringItem item = monitoringItems.get(fullItemName);
            
            if (item == null) {
                item = new MonitoringItem();
                item.setId(UUID.randomUUID().toString());
                item.setName(itemName);
                item.setType("娌夐檷");
                item.setLocation(project.getName() + " - " + itemName);
                monitoringItems.put(fullItemName, item);
                    }

            // 鑾峰彇鏁版嵁瀛樺偍瀵硅薄
            BuildingSettlementDataStorage storage = controller.getBuildingSettlementDataStorage();
            
            // 鏇存柊鐩戞祴椤规暟鎹?            item.setRecords(records);
            item.setBuildingSettlementDataStorage(storage);
            
            // 娣诲姞鍒伴」鐩腑
            project.addMonitoringItem(itemName);
            
            System.out.println("宸叉敹闆嗗缓绛戠墿娌夐檷鐩戞祴鏁版嵁: " + records.size() + " 鏉¤褰?);
                }
        // 澶勭悊SteelSupportAxialForceController
        else if (userData instanceof SteelSupportAxialForceController) {
            SteelSupportAxialForceController controller = (SteelSupportAxialForceController) userData;
            
            // 鏀堕泦閽㈡敮鎾戣酱鍔涙暟鎹?            List<MeasurementRecord> records = controller.getMeasurementRecordsForSaving();
            
            String itemName = "閽㈡敮鎾戣酱鍔?;
            String fullItemName = getFullItemName(project.getName(), itemName);
            MonitoringItem item = monitoringItems.get(fullItemName);
            
            if (item == null) {
                item = new MonitoringItem();
                item.setId(UUID.randomUUID().toString());
                item.setName(itemName);
                item.setType("杞村姏");
                item.setLocation(project.getName() + " - " + itemName);
                monitoringItems.put(fullItemName, item);
            }

            // 鑾峰彇鏁版嵁瀛樺偍瀵硅薄
            SteelSupportAxialForceDataStorage storage = controller.getSteelSupportAxialForceDataStorage();
            
            // 鏇存柊鐩戞祴椤规暟鎹?            item.setRecords(records);
            item.setSteelSupportAxialForceDataStorage(storage);
            
            // 娣诲姞鍒伴」鐩腑
            project.addMonitoringItem(itemName);
            
            System.out.println("宸叉敹闆嗛挗鏀拺杞村姏鐩戞祴鏁版嵁: " + records.size() + " 鏉¤褰?);
        }
        // 澶勭悊ConcreteSupportAxialForceController
        else if (userData instanceof ConcreteSupportAxialForceController) {
            ConcreteSupportAxialForceController controller = (ConcreteSupportAxialForceController) userData;
            
            // 鏀堕泦鐮兼敮鎾戣酱鍔涙暟鎹?..
            if (controller.hasDataChanged()) {
                List<MeasurementRecord> records = controller.getMeasurementRecordsForSaving();
                
                String itemName = "鐮兼敮鎾戣酱鍔?;
                String fullItemName = getFullItemName(project.getName(), itemName);
                MonitoringItem item = monitoringItems.get(fullItemName);

                if (item == null) {
                    item = new MonitoringItem();
                    item.setId(UUID.randomUUID().toString());
                    item.setName(itemName);
                    item.setType("鏀拺杞村姏");
                    item.setLocation(project.getName() + " - " + itemName);
                    monitoringItems.put(fullItemName, item);
                }
                
                // 鑾峰彇鏁版嵁瀛樺偍瀵硅薄
                ConcreteSupportAxialForceDataStorage storage = controller.getConcreteSupportAxialForceDataStorage();
                
                // 鏇存柊鐩戞祴椤规暟鎹?                item.setRecords(records);
                item.setConcreteSupportAxialForceDataStorage(storage);
                
                // 娣诲姞鍒伴」鐩腑
                project.addMonitoringItem(itemName);
                
                System.out.println("宸叉敹闆嗙牸鏀拺杞村姏鏁版嵁: " + records.size() + " 鏉¤褰?);
            } else {
                System.out.println("鐮兼敮鎾戣酱鍔涙暟鎹湭鏇存敼锛屼笉闇€瑕佷繚瀛?);
            }
        }
        // 澶勭悊DeepHorizontalDisplacementController
        else if (userData instanceof DeepHorizontalDisplacementController) {
            DeepHorizontalDisplacementController controller = (DeepHorizontalDisplacementController) userData;
            
            // 鏀堕泦娣遍儴姘村钩浣嶇Щ鏁版嵁
            List<MeasurementRecord> records = controller.getMeasurementRecordsForSaving();
            
            String itemName = "娣遍儴姘村钩浣嶇Щ";
            String fullItemName = getFullItemName(project.getName(), itemName);
            MonitoringItem item = monitoringItems.get(fullItemName);

            if (item == null) {
                item = new MonitoringItem();
                item.setId(UUID.randomUUID().toString());
                item.setName(itemName);
                item.setType("浣嶇Щ");
                item.setLocation(project.getName() + " - " + itemName);
                monitoringItems.put(fullItemName, item);
            }
            
            // 鑾峰彇鏁版嵁瀛樺偍瀵硅薄 - 浣跨敤getOrCreate鏂规硶纭繚瀛樺偍瀵硅薄涓嶄负绌?            DeepHorizontalDisplacementDataStorage storage = controller.getDeepHorizontalDisplacementDataStorage();
            
            // 鎵撳嵃楠岃瘉鏁版嵁鍧?            System.out.println("娣遍儴姘村钩浣嶇Щ鏁版嵁鍧楁暟閲? " + storage.getDataBlockTimestamps().size());
            for (LocalDateTime timestamp : storage.getDataBlockTimestamps()) {
                List<?> dataList = storage.getDataBlock(timestamp);
                System.out.println("  鏁版嵁鍧?" + timestamp + ": " + (dataList != null ? dataList.size() : 0) + " 鏉¤褰?);
            }
            
            // 纭繚瀛樺偍瀵硅薄鍖呭惈瀹屾暣鐨勬暟鎹潡淇℃伅
            DeepHorizontalDisplacementDataStorage completeStorage = item.getOrCreateDeepHorizontalDisplacementDataStorage();
            completeStorage.setPoints(storage.getPoints());
            completeStorage.setCustomDaysForRateCalculation(storage.getCustomDaysForRateCalculation());
            completeStorage.setSelectedDataBlocks(storage.getSelectedDataBlocks());
            
            // 娓呴櫎鏃ф暟鎹潡骞舵坊鍔犳柊鏁版嵁鍧?            for (LocalDateTime timestamp : new ArrayList<>(completeStorage.getDataBlockTimestamps())) {
                completeStorage.removeDataBlock(timestamp);
            }
            
            // 澶嶅埗鎵€鏈夋暟鎹潡
            for (LocalDateTime timestamp : storage.getDataBlockTimestamps()) {
                completeStorage.addDataBlock(timestamp, storage.getDataBlock(timestamp), 
                    storage.getDataBlockDescription(timestamp));
            }
            
            // 鏇存柊鐩戞祴椤规暟鎹?            item.setRecords(records);
            
            // 娣诲姞鍒伴」鐩腑
            project.addMonitoringItem(itemName);
            
            System.out.println("宸叉敹闆嗘繁閮ㄦ按骞充綅绉绘暟鎹? " + records.size() + " 鏉¤褰? " + 
                storage.getDataBlockTimestamps().size() + " 涓暟鎹潡");
        }
        // 澶勭悊PileTopHorizontalDisplacementController
        else if (userData instanceof PileTopHorizontalDisplacementController) {
            PileTopHorizontalDisplacementController controller = (PileTopHorizontalDisplacementController) userData;
            
            // 鏀堕泦妗╅《姘村钩浣嶇Щ鏁版嵁
            List<MeasurementRecord> records = controller.getMeasurementRecordsForSaving();
            
            String itemName = "妗╅《姘村钩浣嶇Щ";
            String fullItemName = getFullItemName(project.getName(), itemName);
            MonitoringItem item = monitoringItems.get(fullItemName);
            
            if (item == null) {
                item = new MonitoringItem();
                item.setId(UUID.randomUUID().toString());
                item.setName(itemName);
                item.setType("浣嶇Щ");
                item.setLocation(project.getName() + " - " + itemName);
                monitoringItems.put(fullItemName, item);
            }
            
            // 鑾峰彇鏁版嵁瀛樺偍瀵硅薄
            PileTopHorizontalDisplacementDataStorage storage = controller.getPileTopHorizontalDisplacementDataStorage();
            
            // 鏇存柊鐩戞祴椤规暟鎹?            item.setRecords(records);
            item.setPileTopHorizontalDisplacementDataStorage(storage);
            
            // 娣诲姞鍒伴」鐩腑
            project.addMonitoringItem(itemName);
            
            System.out.println("宸叉敹闆嗘々椤舵按骞充綅绉荤洃娴嬫暟鎹? " + records.size() + " 鏉¤褰?);
        }
        // 濡傛灉userData涓嶆槸鎺у埗鍣紝閫掑綊鎼滅储瀛愯妭鐐?        else {
            for (Node child : parent.getChildrenUnmodifiable()) {
                if (child instanceof Parent) {
                    findAndCollectFromController((Parent) child, project);
                }
            }
        }
    }

    private void handleSaveAsProject() {
        System.out.println("椤圭洰鍙﹀瓨涓?..");

        // 鑾峰彇褰撳墠閫変腑鐨勬爣绛鹃〉
        Tab selectedTab = editorTabPane.getSelectionModel().getSelectedItem();

        // 濡傛灉娌℃湁閫変腑鏍囩椤碉紝鎴栬€呴€変腑鐨勪笉鏄」鐩爣绛鹃〉锛屽垯鎻愮ず鐢ㄦ埛鍏堥€夋嫨涓€涓」鐩?        if (selectedTab == null) {
            showSelectProjectDialog();
            return;
        }

        // 灏濊瘯鎵惧埌瀵瑰簲鐨勯」鐩?        ProjectInfo projectToSave = null;
        String projectName = selectedTab.getText();

        // 妫€鏌ユ槸鍚︽槸宸茬煡椤圭洰鍚嶇О
        projectToSave = findProjectByName(projectName);

        // 濡傛灉娌℃湁鎵惧埌锛屽彲鑳芥爣绛鹃〉鍚嶇О鍖呭惈浜嗘祴椤瑰悕绉帮紝灏濊瘯浠庢爲涓煡鎵?        if (projectToSave == null) {
            TreeItem<String> projectRoot = projectTreeView.getRoot();
            for (TreeItem<String> projectNode : projectRoot.getChildren()) {
                // 妫€鏌ユ槸鍚︽槸姝ら」鐩殑娴嬮」鏍囩椤?                String currentProjectName = projectNode.getValue();
                if (projectName.startsWith(currentProjectName + " - ")) {
                    projectToSave = findProjectByName(currentProjectName);
                    break;
                }
            }
        }

        // 濡傛灉浠嶆湭鎵惧埌锛屾彁绀虹敤鎴烽€夋嫨椤圭洰
        if (projectToSave == null) {
            showSelectProjectDialog();
            return;
        }

        final ProjectInfo finalProjectToSave = projectToSave;

        // 鍦ㄤ繚瀛樺墠锛屾敹闆嗘墍鏈夐」鐩暟鎹?        saveAllProjectData(finalProjectToSave);

        // 鍒涘缓鏂囦欢閫夋嫨鍣?        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("椤圭洰鍙﹀瓨涓?);
        fileChooser.getExtensionFilters().add(
            new ExtensionFilter("宸ョ▼鐩戞祴椤圭洰鏂囦欢", "*.jc")
        );
        fileChooser.setInitialFileName(finalProjectToSave.getName() + ".jc");

        // 鏄剧ず淇濆瓨鏂囦欢瀵硅瘽妗?        File selectedFile = fileChooser.showSaveDialog(getStage());

        if (selectedFile != null) {
            // 鍒涘缓涓€涓柊鐨勯」鐩壇鏈?            ProjectInfo projectCopy = new ProjectInfo();
            projectCopy.setId(finalProjectToSave.getId());
            projectCopy.setName(finalProjectToSave.getName());
            projectCopy.setDescription(finalProjectToSave.getDescription());
            projectCopy.setOrganization(finalProjectToSave.getOrganization());
            projectCopy.setManager(finalProjectToSave.getManager());
            projectCopy.setMonitoringItems(new ArrayList<>(finalProjectToSave.getMonitoringItems()));

            // 淇濆瓨椤圭洰鍓湰鍒版枃浠?            boolean success = ProjectFileUtil.saveProject(projectCopy, monitoringItems, selectedFile);

            if (success) {
                // 鏇存柊椤圭洰鏂囦欢璺緞
                projectCopy.setProjectFile(selectedFile);

                // 鎻愮ず淇濆瓨鎴愬姛
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("鍙﹀瓨涓烘垚鍔?);
                alert.setHeaderText(null);
                alert.setContentText("椤圭洰 " + projectCopy.getName() + " 宸叉垚鍔熷彟瀛樹负锛歕n" + selectedFile.getAbsolutePath());
                alert.showAndWait();
            } else {
                // 鎻愮ず淇濆瓨澶辫触
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("鍙﹀瓨涓哄け璐?);
                alert.setHeaderText("鏃犳硶淇濆瓨椤圭洰");
                alert.setContentText("淇濆瓨椤圭洰 " + projectCopy.getName() + " 鍒版枃浠舵椂鍑洪敊銆?);
                alert.showAndWait();
            }
        }
    }

    private void handleAddModule() {
        System.out.println("娣诲姞鐩戞祴妯″潡...");

        // 鏌ヨ褰撳墠閫変腑椤?        Tab selectedTab = editorTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) {
            AlertUtil.showWarning("鏈€夋嫨椤圭洰", "璇峰厛閫夋嫨涓€涓」鐩紝鍐嶆坊鍔犵洃娴嬫ā鍧椼€?);
            return;
        }

        // 妫€鏌ユ槸鍚︽槸椤圭洰鏍囩椤?        String tempProjectName = selectedTab.getText();
        ProjectInfo tempProject = findProjectByName(tempProjectName);

        if (tempProject == null) {
            // 灏濊瘯浠庨」鐩?娴嬮」鏍煎紡涓彁鍙栭」鐩悕
            if (tempProjectName.contains(" - ")) {
                tempProjectName = tempProjectName.split(" - ")[0];
                tempProject = findProjectByName(tempProjectName);
            }
        }

        if (tempProject == null) {
            AlertUtil.showWarning("鏈壘鍒伴」鐩?, "璇风‘淇濋€変腑鐨勬爣绛鹃〉鏄」鐩爣绛鹃〉銆?);
            return;
        }

        // 鍒涘缓final鍙橀噺鐢ㄤ簬lambda琛ㄨ揪寮?        final String projectName = tempProjectName;
        final ProjectInfo project = tempProject;

        // 鏄剧ず妯″潡閫夋嫨瀵硅瘽妗?        ChoiceDialog<String> dialog = new ChoiceDialog<>("鍦拌〃鐐规矇闄?, Arrays.asList(
                "鍦拌〃鐐规矇闄?, "妗╅《浣嶇Щ", "閽㈡敮鎾戣酱鍔?, "姘翠綅", "瑁傜紳", "鍊炬枩"));
        dialog.setTitle("娣诲姞鐩戞祴妯″潡");
        dialog.setHeaderText("璇烽€夋嫨瑕佹坊鍔犵殑鐩戞祴妯″潡");
        dialog.setContentText("妯″潡绫诲瀷:");

        dialog.showAndWait().ifPresent(moduleType -> {
            // 妫€鏌ユā鍧楁槸鍚﹀凡缁忔坊鍔犺繃
            boolean alreadyAdded = project.getMonitoringItems().contains(moduleType);

            if (alreadyAdded) {
                // 濡傛灉妯″潡宸叉坊鍔狅紝璇㈤棶鏄惁鎵撳紑
                ButtonType openButton = new ButtonType("鎵撳紑鐜版湁", ButtonBar.ButtonData.YES);
                ButtonType cancelButton = new ButtonType("鍙栨秷", ButtonBar.ButtonData.CANCEL_CLOSE);

                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "鐩戞祴妯″潡'" + moduleType + "'宸插瓨鍦ㄣ€傛槸鍚︽墦寮€锛?,
                        openButton, cancelButton);
                alert.setTitle("妯″潡宸插瓨鍦?);

                alert.showAndWait().ifPresent(buttonType -> {
                    if (buttonType == openButton) {
                        openMonitoringItemWithProject(projectName, moduleType);
                    }
                });
            } else {
                // 娣诲姞鏂扮殑鐩戞祴妯″潡
                project.addMonitoringItem(moduleType);

                // 娣诲姞鍒板伐绋嬫爲
                TreeItem<String> projectNode = findProjectNode(projectName);
                if (projectNode != null) {
                    TreeItem<String> moduleNode = new TreeItem<>(moduleType);
                    projectNode.getChildren().add(moduleNode);
                }

                // 鎵撳紑鐩戞祴妯″潡
                openMonitoringItemWithProject(projectName, moduleType);

                System.out.println("宸叉坊鍔犵洃娴嬫ā鍧? " + moduleType + " 鍒伴」鐩? " + projectName);
            }
        });
    }

    private void handleProjectProperties() {
        System.out.println("宸ョ▼灞炴€?..");
        // 杩欓噷鍙互娣诲姞鏄剧ず宸ョ▼灞炴€х殑閫昏緫
    }

    private void handleSaveDefault() {
        System.out.println("淇濆瓨榛樿鍊?..");
        // 淇濆瓨鎵€鏈夐」鐩埌榛樿鐩綍
        saveProjectsToDefaultDirectory();
    }

    /**
     * 淇濆瓨鎵€鏈夐」鐩埌榛樿鐩綍
     */
    private void saveProjectsToDefaultDirectory() {
        // 榛樿椤圭洰鐩綍锛屼娇鐢ㄧ敤鎴锋枃妗ｇ洰褰曚笅鐨?宸ョ▼鐩戞祴椤圭洰"鏂囦欢澶?        String userHome = System.getProperty("user.home");
        File projectsDir = new File(userHome, "宸ョ▼鐩戞祴椤圭洰");

        // 濡傛灉鐩綍涓嶅瓨鍦紝鍒涘缓瀹?        if (!projectsDir.exists()) {
            boolean created = projectsDir.mkdirs();
            if (!created) {
                System.err.println("鏃犳硶鍒涘缓榛樿椤圭洰鐩綍: " + projectsDir.getAbsolutePath());

                // 鏄剧ず閿欒鎻愮ず
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("淇濆瓨閿欒");
                alert.setHeaderText("鏃犳硶鍒涘缓榛樿椤圭洰鐩綍");
                alert.setContentText("璇风‘淇濇偍鏈夋潈闄愬垱寤虹洰褰? " + projectsDir.getAbsolutePath());
                alert.showAndWait();
                return;
            }
        }

        int savedCount = 0;
        List<String> failedProjects = new ArrayList<>();

        // 淇濆瓨鎵€鏈夐」鐩?        for (ProjectInfo project : projects.values()) {
            // 鍏堟敹闆嗛」鐩暟鎹?            saveAllProjectData(project);

            // 鍒涘缓椤圭洰鏂囦欢
            File projectFile = new File(projectsDir, project.getName() + ".jc");

            // 淇濆瓨椤圭洰鍒版枃浠?            boolean success = ProjectFileUtil.saveProject(project, monitoringItems, projectFile);

            if (success) {
                // 鏇存柊椤圭洰鏂囦欢璺緞
                project.setProjectFile(projectFile);
                savedCount++;
            } else {
                failedProjects.add(project.getName());
            }
        }

        // 鏄剧ず淇濆瓨缁撴灉
        if (savedCount > 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("淇濆瓨鎴愬姛");
            alert.setHeaderText(null);
            alert.setContentText("宸叉垚鍔熷皢 " + savedCount + " 涓」鐩繚瀛樺埌榛樿鐩綍\n" + projectsDir.getAbsolutePath());
            alert.showAndWait();
        }

        // 濡傛灉鏈夊け璐ョ殑椤圭洰锛屾樉绀洪敊璇俊鎭?        if (!failedProjects.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("淇濆瓨閿欒");
            alert.setHeaderText("浠ヤ笅椤圭洰淇濆瓨澶辫触");

            StringBuilder content = new StringBuilder();
            for (String projectName : failedProjects) {
                content.append("- ").append(projectName).append("\n");
            }

            alert.setContentText(content.toString());
            alert.showAndWait();
        }
    }

    private void handleLoadDefault() {
        System.out.println("璋冨彇榛樿鍊?..");
        // 鍔犺浇榛樿鐩綍涓殑椤圭洰鏂囦欢
        loadProjectsFromDefaultDirectory();
    }

    /**
     * 浠庨粯璁ょ洰褰曞姞杞介」鐩枃浠?     */
    private void loadProjectsFromDefaultDirectory() {
        // 榛樿椤圭洰鐩綍锛屼娇鐢ㄧ敤鎴锋枃妗ｇ洰褰曚笅鐨?宸ョ▼鐩戞祴椤圭洰"鏂囦欢澶?        String userHome = System.getProperty("user.home");
        File projectsDir = new File(userHome, "宸ョ▼鐩戞祴椤圭洰");

        // 濡傛灉鐩綍涓嶅瓨鍦紝鍒涘缓瀹?        if (!projectsDir.exists()) {
            boolean created = projectsDir.mkdirs();
            if (!created) {
                System.err.println("鏃犳硶鍒涘缓榛樿椤圭洰鐩綍: " + projectsDir.getAbsolutePath());
                return;
            }
        }

        // 鑾峰彇鐩綍涓殑鎵€鏈?jc鏂囦欢
        File[] projectFiles = projectsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jc"));

        if (projectFiles == null || projectFiles.length == 0) {
            System.out.println("榛樿鐩綍涓病鏈夋壘鍒伴」鐩枃浠?);
            return;
        }

        int loadedCount = 0;

        // 鍔犺浇姣忎釜椤圭洰鏂囦欢
        for (File file : projectFiles) {
            ProjectFileData data = ProjectFileUtil.loadProject(file);

            if (data != null && data.projectInfo != null) {
                // 妫€鏌ユ槸鍚﹀凡瀛樺湪鍚屽悕椤圭洰
                boolean exists = false;
                String projectName = data.projectInfo.getName();

                for (ProjectInfo existingProject : projects.values()) {
                    if (existingProject.getName().equals(projectName)) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    // 鍔犺浇椤圭洰
                    loadProjectData(data, file);
                    loadedCount++;
                }
            }
        }

        System.out.println("浠庨粯璁ょ洰褰曞姞杞戒簡 " + loadedCount + " 涓」鐩?);
    }

    private void handleParameters() {
        System.out.println("鍙傛暟瀹氫箟...");
        // 杩欓噷鍙互娣诲姞鍙傛暟瀹氫箟鐨勯€昏緫
    }

    /**
     * 澶勭悊娣诲姞娴嬮」鑿滃崟椤圭偣鍑讳簨浠?     */
    private void handleAddMonitoringItem(TreeItem<String> projectNode) {
        // 鍒涘缓娴嬮」閫夋嫨瀵硅瘽妗?        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("娣诲姞娴嬮」");
        dialog.setHeaderText("閫夋嫨瑕佹坊鍔犵殑娴嬮」绫诲瀷");

        // 璁剧疆鎸夐挳
        ButtonType addButtonType = new ButtonType("娣诲姞", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // 鍒涘缓娴嬮」绫诲瀷涓嬫媺妗?        ComboBox<String> itemTypeCombo = new ComboBox<>();
        itemTypeCombo.getItems().addAll(
            "鍦拌〃鐐规矇闄?, "妗╅《绔栧悜浣嶇Щ", "妗╅《姘村钩浣嶇Щ", "閽㈡敮鎾戣酱鍔?,
            "鐮兼敮鎾戣酱鍔?, "绔嬫煴绔栧悜浣嶇Щ", "寤虹瓚鐗╂矇闄?, "寤虹瓚鐗╁€炬枩",
            "鍦颁笅姘翠綅", "娣遍儴姘村钩浣嶇Щ"
        );
        itemTypeCombo.setPromptText("閫夋嫨娴嬮」绫诲瀷");

        // 鍒涘缓瀵硅瘽妗嗗唴瀹?        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.getChildren().add(itemTypeCombo);
        dialog.getDialogPane().setContent(content);

        // 璁剧疆缁撴灉杞崲鍣?        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return itemTypeCombo.getValue();
            }
            return null;
        });

        // 鏄剧ず瀵硅瘽妗嗗苟澶勭悊缁撴灉
        dialog.showAndWait().ifPresent(itemType -> {
            if (itemType != null && !itemType.isEmpty()) {
                // 妫€鏌ラ」鐩妭鐐逛笅鏄惁宸插瓨鍦ㄨ娴嬮」
                boolean exists = false;
                for (TreeItem<String> child : projectNode.getChildren()) {
                    if (child.getValue().equals(itemType)) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    // 娣诲姞娴嬮」鍒版爲涓?                    TreeItem<String> newItemNode = new TreeItem<>(itemType);
                    projectNode.getChildren().add(newItemNode);

                    // 鍒涘缓鐩戞祴椤瑰璞?                    MonitoringItem item = new MonitoringItem(
                            itemType.replaceAll("\\s+", ""),  // 绉婚櫎绌烘牸浣滀负ID
                            itemType,  // 鍚嶇О
                            getItemType(itemType),  // 绫诲瀷
                            projectNode.getValue() + " - " + itemType  // 浣嶇疆
                    );

                    // 浣跨敤鍏ㄥ悕锛堥」鐩悕+娴嬮」鍚嶏級浣滀负閿?                    String fullItemName = getFullItemName(projectNode.getValue(), itemType);
                    monitoringItems.put(fullItemName, item);

                    // 鏇存柊椤圭洰淇℃伅涓殑娴嬮」鍒楄〃
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
     * 澶勭悊椤圭洰灞炴€ц彍鍗曢」鐐瑰嚮浜嬩欢
     */
    private void handleProjectProperties(TreeItem<String> projectNode) {
        // 鏌ユ壘椤圭洰淇℃伅
        final ProjectInfo projectInfo = findProjectByName(projectNode.getValue());

        if (projectInfo != null) {
            // 鍒涘缓骞舵樉绀洪」鐩紪杈戝璇濇
            ProjectDialog dialog = new ProjectDialog(projectInfo);
            dialog.setHeaderText("缂栬緫椤圭洰灞炴€?);

            // 鏄剧ず瀵硅瘽妗嗗苟绛夊緟鐢ㄦ埛鍝嶅簲
            dialog.showAndWait().ifPresent(updatedInfo -> {
                // 鏇存柊椤圭洰淇℃伅
                projectInfo.setName(updatedInfo.getName());
                projectInfo.setDescription(updatedInfo.getDescription());
                projectInfo.setOrganization(updatedInfo.getOrganization());
                projectInfo.setManager(updatedInfo.getManager());

                // 鏇存柊娴嬮」鍒楄〃锛堝垹闄ゅ凡绉婚櫎鐨勬祴椤癸紝娣诲姞鏂板鐨勬祴椤癸級
                List<String> oldItems = new ArrayList<>(projectInfo.getMonitoringItems());
                List<String> newItems = updatedInfo.getMonitoringItems();

                // 绉婚櫎宸插垹闄ょ殑娴嬮」
                List<TreeItem<String>> itemsToRemove = new ArrayList<>();
                for (TreeItem<String> itemNode : projectNode.getChildren()) {
                    if (!newItems.contains(itemNode.getValue())) {
                        itemsToRemove.add(itemNode);
                    }
                }
                projectNode.getChildren().removeAll(itemsToRemove);

                // 娣诲姞鏂板鐨勬祴椤?                for (String newItem : newItems) {
                    if (!oldItems.contains(newItem)) {
                        TreeItem<String> newItemNode = new TreeItem<>(newItem);
                        projectNode.getChildren().add(newItemNode);

                        // 鍒涘缓鐩戞祴椤瑰璞?                        MonitoringItem item = new MonitoringItem(
                                newItem.replaceAll("\\s+", ""),  // 绉婚櫎绌烘牸浣滀负ID
                                newItem,  // 鍚嶇О
                                getItemType(newItem),  // 绫诲瀷
                                projectInfo.getName() + " - " + newItem  // 浣嶇疆
                        );
                        monitoringItems.put(getFullItemName(projectInfo.getName(), newItem), item);
                    }
                }

                // 鏇存柊椤圭洰娴嬮」鍒楄〃
                projectInfo.setMonitoringItems(newItems);

                // 鏇存柊椤圭洰鑺傜偣鍚嶇О
                projectNode.setValue(projectInfo.getName());
            });
        }
    }

    /**
     * 鏍规嵁椤圭洰鍚嶇О鏌ユ壘椤圭洰淇℃伅
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
     * 澶勭悊绉婚櫎椤圭洰鑿滃崟椤圭偣鍑讳簨浠?     */
    private void handleRemoveProject(TreeItem<String> projectNode) {
        // 鏌ユ壘椤圭洰淇℃伅
        final ProjectInfo projectToRemove = findProjectByName(projectNode.getValue());
        final String projectId = findProjectIdByName(projectNode.getValue());
        final String projectName = projectNode.getValue();

        if (projectToRemove != null && projectId != null) {
            // 鏄剧ず纭瀵硅瘽妗?            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("纭绉婚櫎");
            alert.setHeaderText("鏄惁纭绉婚櫎椤圭洰锛? + projectToRemove.getName());
            alert.setContentText("姝ゆ搷浣滃皢绉婚櫎璇ラ」鐩強鍏舵墍鏈夋祴椤规暟鎹紝涓斾笉鍙仮澶嶃€?);

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    System.out.println("寮€濮嬬Щ闄ら」鐩? " + projectName + " (ID: " + projectId + ")");
                    
                    // 绉婚櫎椤圭洰淇℃伅
                    projects.remove(projectId);

                    // 鑾峰彇鎵€鏈夐渶瑕佸叧闂殑鏍囩椤靛拰绉婚櫎鐨勭洃娴嬮」
                    List<Tab> tabsToRemove = new ArrayList<>();
                    List<String> itemsToRemove = new ArrayList<>();

                    // 绉婚櫎棰勮鏁版嵁
                    if (projectWarningMap.containsKey(projectId)) {
                        System.out.println("绉婚櫎椤圭洰鍏宠仈鐨勯璀︽暟鎹? 鏉＄洰鏁? " + 
                                (projectWarningMap.get(projectId) != null ? projectWarningMap.get(projectId).size() : 0));
                        projectWarningMap.remove(projectId);
                    } else {
                        System.out.println("椤圭洰娌℃湁鍏宠仈鐨勯璀︽暟鎹?);
                    }

                    // 绉婚櫎姣忎釜娴嬮」
                    System.out.println("寮€濮嬬Щ闄ら」鐩腑鐨勬祴椤? 娴嬮」鏁? " + projectNode.getChildren().size());
                    for (TreeItem<String> itemNode : projectNode.getChildren()) {
                        String itemName = itemNode.getValue();
                        String fullItemName = getFullItemName(projectName, itemName);
                        System.out.println("- 绉婚櫎娴嬮」: " + fullItemName);

                        // 娣诲姞鍒拌绉婚櫎鐨勭洃娴嬮」鍒楄〃
                        itemsToRemove.add(fullItemName);

                        // 鏌ユ壘瀵瑰簲鐨勬爣绛鹃〉
                        Tab itemTab = findTab(fullItemName);
                        if (itemTab != null) {
                            tabsToRemove.add(itemTab);
                            System.out.println("  鎵惧埌骞跺噯澶囧叧闂浉鍏虫爣绛鹃〉: " + itemTab.getText());
                        }
                    }

                    // 绉婚櫎椤圭洰鏍戣妭鐐?                    projectTreeView.getRoot().getChildren().remove(projectNode);
                    System.out.println("宸蹭粠椤圭洰鏍戜腑绉婚櫎椤圭洰鑺傜偣");
                    
                    // 鐩存帴浠庣洃娴嬫鍐垫爲涓Щ闄ゅ搴旂殑椤圭洰鑺傜偣
                    TreeItem<String> overviewProjectNode = findOverviewProjectNode(projectName);
                    if (overviewProjectNode != null) {
                        overviewTreeView.getRoot().getChildren().remove(overviewProjectNode);
                        System.out.println("宸蹭粠鐩戞祴姒傚喌鏍戜腑绉婚櫎椤圭洰鑺傜偣: " + projectName);
                    } else {
                        System.out.println("鍦ㄧ洃娴嬫鍐垫爲涓湭鎵惧埌椤圭洰鑺傜偣: " + projectName);
                    }
                    
                    // 鍒锋柊鐩戞祴姒傚喌鏍戜互纭繚鎵€鏈夎妭鐐归兘鏄渶鏂扮殑
                    refreshOverviewTree();

                    // 妫€鏌ラ」鐩湰韬殑鏍囩椤?                    Tab projectTab = findTab(projectName);
                    if (projectTab != null) {
                        tabsToRemove.add(projectTab);
                        System.out.println("鎵惧埌骞跺噯澶囧叧闂」鐩爣绛鹃〉: " + projectTab.getText());
                    }
                    
                    // 妫€鏌ラ璀︿俊鎭爣绛鹃〉
                    Tab warningTab = findTab(projectName + " - 棰勮淇℃伅");
                    if (warningTab != null) {
                        tabsToRemove.add(warningTab);
                        System.out.println("鎵惧埌骞跺噯澶囧叧闂璀︿俊鎭爣绛鹃〉: " + warningTab.getText());
                    }
                    
                    // 绉婚櫎鎵€鏈変笌璇ラ」鐩浉鍏崇殑鏍囩椤?                    // 浣跨敤鏂扮殑鏂规硶鎼滅储鎵€鏈夊甫椤圭洰鍚嶅墠缂€鐨勬爣绛鹃〉
                    List<Tab> relatedTabs = findTabsWithPrefix(projectName + " - ");
                    tabsToRemove.addAll(relatedTabs);
                    System.out.println("鎵惧埌 " + relatedTabs.size() + " 涓浉鍏虫爣绛鹃〉鍑嗗鍏抽棴");
                    
                    // 绉婚櫎鐩稿叧鐨勬爣绛鹃〉
                    editorTabPane.getTabs().removeAll(tabsToRemove);
                    System.out.println("宸插叧闂?" + tabsToRemove.size() + " 涓浉鍏虫爣绛鹃〉");

                    // 绉婚櫎鎵€鏈夌洃娴嬮」鏁版嵁
                    for (String item : itemsToRemove) {
                        monitoringItems.remove(item);
                    }
                    System.out.println("宸茬Щ闄?" + itemsToRemove.size() + " 涓洃娴嬮」鏁版嵁");

                    // 鏄剧ず鎿嶄綔鎴愬姛淇℃伅
                    System.out.println("椤圭洰 " + projectName + " 宸叉垚鍔熺Щ闄?);
                }
            });
        } else {
            System.out.println("鏃犳硶绉婚櫎椤圭洰锛屾湭鎵惧埌椤圭洰淇℃伅鎴朓D");
            if (projectToRemove == null) {
                System.out.println("椤圭洰淇℃伅涓簄ull");
            }
            if (projectId == null) {
                System.out.println("椤圭洰ID涓簄ull");
            }
        }
    }

    /**
     * 鏍规嵁椤圭洰鍚嶇О鏌ユ壘椤圭洰ID
     */
    private String findProjectIdByName(String projectName) {
        if (projectName == null) {
            return null;
        }
        
        System.out.println("鏌ユ壘椤圭洰ID锛岄」鐩悕绉? " + projectName);
        
        for (Map.Entry<String, ProjectInfo> entry : projects.entrySet()) {
            if (entry.getValue().getName().equals(projectName)) {
                String foundId = entry.getKey();
                System.out.println("鎵惧埌椤圭洰ID: " + foundId + " 瀵瑰簲椤圭洰: " + projectName);
                return foundId;
            }
        }
        
        System.out.println("鏈壘鍒伴」鐩甀D锛岄」鐩悕绉? " + projectName);
        System.out.println("褰撳墠椤圭洰鍒楄〃:");
        for (Map.Entry<String, ProjectInfo> entry : projects.entrySet()) {
            System.out.println("- ID: " + entry.getKey() + ", 鍚嶇О: " + entry.getValue().getName());
        }
        
        return null;
    }

    /**
     * 淇濆瓨鎸囧畾鐨勯」鐩?     * @param project 瑕佷繚瀛樼殑椤圭洰
     * @return 鏄惁淇濆瓨鎴愬姛
     */
    private boolean saveProject(ProjectInfo project) {
        // 鏀堕泦椤圭洰鏁版嵁
        saveAllProjectData(project);

        // 妫€鏌ラ」鐩槸鍚﹀凡缁忔湁鍏宠仈鐨勬枃浠?        File selectedFile = null;
        if (project.getProjectFile() != null && project.getProjectFile().exists()) {
            // 浣跨敤宸叉湁鐨勬枃浠?            selectedFile = project.getProjectFile();
            System.out.println("鑷姩淇濆瓨椤圭洰鍒? " + selectedFile.getAbsolutePath());
        } else {
            // 鍒涘缓鏂囦欢閫夋嫨鍣?            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("淇濆瓨宸ョ▼鐩戞祴椤圭洰");
            fileChooser.getExtensionFilters().add(
                new ExtensionFilter("宸ョ▼鐩戞祴椤圭洰鏂囦欢", "*.jc")
            );
            fileChooser.setInitialFileName(project.getName() + ".jc");

            // 鏄剧ず淇濆瓨鏂囦欢瀵硅瘽妗?            selectedFile = fileChooser.showSaveDialog(getStage());
        }

        if (selectedFile != null) {
            try {
                // 鑾峰彇浠庨璀︿俊鎭爣绛鹃〉鏀堕泦鐨勯璀︽暟鎹?                List<WarningInfo> collectedWarnings = projectWarningMap.get(project.getId());
                
                // 濡傛灉娌℃湁鏀堕泦鍒伴璀︽暟鎹紝灏濊瘯浠庡叾浠栨爣绛鹃〉鑾峰彇
                if (collectedWarnings == null) {
                    collectedWarnings = getWarningDataForProject(project);
                }
                
                // 纭繚棰勮淇℃伅鍒楄〃涓嶄负null
                List<WarningInfo> warningInfoList = collectedWarnings != null ? 
                    collectedWarnings : new ArrayList<>();
                
                if (warningInfoList.size() > 0) {
                    System.out.println("淇濆瓨椤圭洰鏃跺寘鍚?" + warningInfoList.size() + " 鏉￠璀︿俊鎭?);
                }
                
                // 淇濆瓨椤圭洰鍒版枃浠讹紝鍖呮嫭棰勮淇℃伅
                boolean success = ProjectFileUtil.saveProject(project, monitoringItems, warningInfoList, selectedFile);

            if (success) {
                // 鏇存柊椤圭洰鏂囦欢璺緞
                project.setProjectFile(selectedFile);
                System.out.println("椤圭洰 " + project.getName() + " 宸叉垚鍔熶繚瀛樺埌鏂囦欢锛? + selectedFile.getAbsolutePath());
                return true;
            } else {
                System.err.println("淇濆瓨椤圭洰 " + project.getName() + " 鍒版枃浠舵椂鍑洪敊銆?);
                }
            } catch (Exception e) {
                System.err.println("淇濆瓨椤圭洰鏃跺彂鐢熼敊璇? " + e.getMessage());
                e.printStackTrace();
            }
        }

        return false;
    }
    
    /**
     * 鑾峰彇椤圭洰鐨勯璀︽暟鎹?     * @param project 椤圭洰淇℃伅
     * @return 棰勮淇℃伅鍒楄〃
     */
    public List<WarningInfo> getWarningDataForProject(ProjectInfo project) {
        if (project == null) return new ArrayList<>();
        
        List<WarningInfo> warningInfoList = new ArrayList<>();
        
        // 鍏堝皾璇曚粠projectWarningMap涓洿鎺ヨ幏鍙?        String projectId = project.getId();
        if (projectId != null && projectWarningMap.containsKey(projectId)) {
            return projectWarningMap.get(projectId);
        }
        
        // 濡傛灉娌℃湁鎵惧埌锛屽皾璇曢€氳繃椤圭洰鍚嶇О鏌ユ壘ID
        projectId = findProjectIdByName(project.getName());
        if (projectId != null && projectWarningMap.containsKey(projectId)) {
            return projectWarningMap.get(projectId);
        }
        
        // 濡傛灉杩樻病鎵惧埌锛屽皾璇曚粠鎵撳紑鐨勬爣绛鹃〉涓幏鍙?        String projectName = project.getName();
        String tabTitle = projectName.equals("鍏ㄥ眬") ? "棰勮淇℃伅" : projectName + " - 棰勮淇℃伅";
        
        Tab tab = findTab(tabTitle);
        if (tab != null && tab.getContent() instanceof Parent) {
            Parent content = (Parent) tab.getContent();
            WarningInfoController controller = getWarningInfoController(content);
            
            if (controller != null) {
                warningInfoList.addAll(controller.getWarningDataList());
        // 濡傛灉鎵句笉鍒颁换浣曢璀︽暟鎹紝杩斿洖绌哄垪琛?        return new ArrayList<>();
    }
} 
