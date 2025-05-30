package com.monitor.view;

import com.monitor.model.SettlementData;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.Predicate;

/**
 * 搜索和筛选面板组件
 */
public class SearchFilterPanel extends VBox {

    private TextField searchField;
    private ComboBox<String> filterTypeComboBox;
    private ComboBox<String> sortOrderComboBox;
    private CheckBox showWarningsOnlyCheckBox;

    private FilteredList<SettlementData> filteredData;
    private ObservableList<SettlementData> originalData;

    /**
     * 创建搜索和筛选面板
     * @param data 原始数据列表
     */
    public SearchFilterPanel(ObservableList<SettlementData> data) {
        this.originalData = data;
        this.filteredData = new FilteredList<>(data);

        setPadding(new Insets(10));
        setSpacing(10);

        // 创建搜索框
        HBox searchBox = new HBox(10);
        Label searchLabel = new Label("搜索测点:");
        searchField = new TextField();
        searchField.setPromptText("输入测点编号...");
        searchField.setPrefWidth(200);

        // 添加搜索功能
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            updateFilter();
        });

        searchBox.getChildren().addAll(searchLabel, searchField);

        // 创建筛选选项
        HBox filterBox = new HBox(10);
        Label filterLabel = new Label("筛选类型:");
        filterTypeComboBox = new ComboBox<>();
        filterTypeComboBox.getItems().addAll("全部", "超过速率警戒值", "超过累计警戒值");
        filterTypeComboBox.setValue("全部");

        // 添加筛选功能
        filterTypeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateFilter();
        });

        filterBox.getChildren().addAll(filterLabel, filterTypeComboBox);

        // 创建排序选项
        HBox sortBox = new HBox(10);
        Label sortLabel = new Label("排序方式:");
        sortOrderComboBox = new ComboBox<>();
        sortOrderComboBox.getItems().addAll("按测点顺序", "按累计变化量", "按变化速率");
        sortOrderComboBox.setValue("按测点顺序");

        // 添加排序功能
        sortOrderComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateSort();
        });

        sortBox.getChildren().addAll(sortLabel, sortOrderComboBox);

        // 创建仅显示警告选项
        showWarningsOnlyCheckBox = new CheckBox("仅显示超警戒值的测点");
        showWarningsOnlyCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            updateFilter();
        });

        // 添加所有组件到面板
        getChildren().addAll(searchBox, filterBox, sortBox, showWarningsOnlyCheckBox);
    }

    /**
     * 更新筛选条件
     */
    private void updateFilter() {
        String searchText = searchField.getText().toLowerCase();
        String filterType = filterTypeComboBox.getValue();
        boolean showWarningsOnly = showWarningsOnlyCheckBox.isSelected();

        Predicate<SettlementData> searchPredicate = data -> {
            if (searchText == null || searchText.isEmpty()) {
                return true;
            }
            return data.getPointCode().toLowerCase().contains(searchText);
        };

        Predicate<SettlementData> filterPredicate = data -> {
            if (filterType.equals("全部") && !showWarningsOnly) {
                return true;
            } else if (filterType.equals("超过速率警戒值") || showWarningsOnly) {
                // 使用变化率判断是否超过警戒值
                return Math.abs(data.getChangeRate()) > 5; // 假设5mm/天是速率警戒值
            } else if (filterType.equals("超过累计警戒值")) {
                return Math.abs(data.getCumulativeChange()) > 10; // 假设10mm是累计警戒值
            }
            return true;
        };

        filteredData.setPredicate(searchPredicate.and(filterPredicate));
    }

    /**
     * 更新排序方式
     */
    private void updateSort() {
        String sortOrder = sortOrderComboBox.getValue();

        if (sortOrder.equals("按累计变化量")) {
            FXCollections.sort(filteredData, (data1, data2) ->
                Double.compare(Math.abs(data2.getCumulativeChange()), Math.abs(data1.getCumulativeChange())));
        } else if (sortOrder.equals("按变化速率")) {
            FXCollections.sort(filteredData, (data1, data2) ->
                Double.compare(Math.abs(data2.getChangeRate()), Math.abs(data1.getChangeRate())));
        } else {
            // 恢复原始顺序
            FXCollections.sort(filteredData, (data1, data2) -> 0);
        }
    }

    /**
     * 获取筛选后的数据列表
     * @return 筛选后的数据列表
     */
    public FilteredList<SettlementData> getFilteredData() {
        return filteredData;
    }

    /**
     * 重置所有筛选条件
     */
    public void resetFilters() {
        searchField.clear();
        filterTypeComboBox.setValue("全部");
        sortOrderComboBox.setValue("按测点顺序");
        showWarningsOnlyCheckBox.setSelected(false);
    }
}
