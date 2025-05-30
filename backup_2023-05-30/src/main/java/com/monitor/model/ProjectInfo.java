package com.monitor.model;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目信息模型类
 * 用于存储项目的基本信息
 */
public class ProjectInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String name;
    private String description;
    private String organization;
    private String manager;
    private List<String> monitoringItems;
    private transient File projectFile; // 使用transient避免序列化此字段
    
    /**
     * 默认构造函数
     */
    public ProjectInfo() {
        this.monitoringItems = new ArrayList<>();
    }
    
    /**
     * 带参数的构造函数
     */
    public ProjectInfo(String id, String name, String description, String organization, String manager) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.organization = organization;
        this.manager = manager;
        this.monitoringItems = new ArrayList<>();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getOrganization() {
        return organization;
    }
    
    public void setOrganization(String organization) {
        this.organization = organization;
    }
    
    public String getManager() {
        return manager;
    }
    
    public void setManager(String manager) {
        this.manager = manager;
    }
    
    public List<String> getMonitoringItems() {
        return monitoringItems;
    }
    
    public void setMonitoringItems(List<String> monitoringItems) {
        this.monitoringItems = monitoringItems;
    }
    
    /**
     * 获取项目文件
     * @return 项目文件对象
     */
    public File getProjectFile() {
        return projectFile;
    }
    
    /**
     * 设置项目文件
     * @param projectFile 项目文件对象
     */
    public void setProjectFile(File projectFile) {
        this.projectFile = projectFile;
    }
    
    /**
     * 添加监测测项
     * @param item 测项名称
     */
    public void addMonitoringItem(String item) {
        if (monitoringItems == null) {
            monitoringItems = new ArrayList<>();
        }
        if (!monitoringItems.contains(item)) {
            monitoringItems.add(item);
        }
    }
    
    /**
     * 移除监测测项
     * @param item 测项名称
     * @return 是否成功移除
     */
    public boolean removeMonitoringItem(String item) {
        if (monitoringItems != null) {
            return monitoringItems.remove(item);
        }
        return false;
    }
    
    @Override
    public String toString() {
        return name;
    }
} 