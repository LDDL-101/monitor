package com.monitor.util;

import com.monitor.model.ProjectInfo;
import com.monitor.model.MonitoringItem;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 项目文件处理工具类
 * 负责项目文件的保存和加载
 */
public class ProjectFileUtil {

    /**
     * 保存项目到文件
     * @param projectInfo 项目信息
     * @param monitoringItems 与项目关联的监测项
     * @param file 目标文件
     * @return 是否成功
     */
    public static boolean saveProject(ProjectInfo projectInfo, Map<String, MonitoringItem> monitoringItems, File file) {
        // 确保文件后缀是.jc
        if (!file.getName().toLowerCase().endsWith(".jc")) {
            file = new File(file.getAbsolutePath() + ".jc");
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            // 创建一个包装类，保存项目信息和对应的监测项
            ProjectFileData data = new ProjectFileData();
            data.projectInfo = projectInfo;

            // 筛选出与当前项目相关的监测项
            Map<String, MonitoringItem> relatedItems = new HashMap<>();
            String projectName = projectInfo.getName();

            for (Map.Entry<String, MonitoringItem> entry : monitoringItems.entrySet()) {
                if (entry.getKey().startsWith(projectName + " - ")) {
                    relatedItems.put(entry.getKey(), entry.getValue());
                }
            }

            data.monitoringItems = relatedItems;

            // 写入文件
            oos.writeObject(data);

            // 更新项目文件引用
            projectInfo.setProjectFile(file);

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 从文件加载项目
     * @param file 项目文件
     * @return 项目数据包装对象，包含项目信息和监测项
     */
    public static ProjectFileData loadProject(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            ProjectFileData data = (ProjectFileData) ois.readObject();
            return data;
        } catch (IOException e) {
            System.err.println("IO错误: 无法读取文件 " + file.getAbsolutePath() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (ClassNotFoundException e) {
            System.err.println("类错误: 无法识别文件格式，可能是版本不兼容: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("未知错误: 加载项目文件失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 项目文件数据包装类，用于序列化和反序列化
     */
    public static class ProjectFileData implements Serializable {
        private static final long serialVersionUID = 1L;

        public ProjectInfo projectInfo;
        public Map<String, MonitoringItem> monitoringItems;
    }
}