# 监测系统 (Monitoring System)

基于JavaFX的监测数据管理系统，提供数据采集、管理、分析及报表输出功能。

## 功能特点

- 监测数据的采集和存储
- 数据可视化展示
- 数据分析和趋势预测
- 报表生成和导出
- 告警管理
- 系统配置管理

## 技术栈

- **编程语言**: Java 17+
- **GUI框架**: JavaFX 17+
- **UI组件**:
  - JFoenix (Material Design UI组件)
  - ControlsFX (增强型JavaFX控件)
- **数据可视化**: JFreeChart
- **报表处理**:
  - Apache POI (Excel/Word)
  - JasperReports (PDF)
- **数据库**:
  - 支持PostgreSQL、MySQL和SQLite
  - 使用JDBC进行数据访问
- **构建工具**: Maven

## 系统要求

- JDK 17 或更高版本
- Maven 3.6 或更高版本
- 数据库 (根据配置选择 PostgreSQL、MySQL 或 SQLite)

## 开发环境设置

1. 克隆项目仓库
2. 使用 IntelliJ IDEA 或其他 IDE 打开项目
3. 安装必要的依赖项 (Maven 会自动处理)
4. 配置数据库连接 (修改 `src/main/resources/database.properties`)

## 构建和运行

使用 Maven 构建项目:

```
mvn clean package
```

运行应用程序:

```
java -jar target/monitor-sft-1.0-SNAPSHOT.jar
```

## 项目结构

```
monitor_sft/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── monitor/
│   │   │           ├── controller/    # 控制器类
│   │   │           ├── model/         # 数据模型
│   │   │           ├── repository/    # 数据访问
│   │   │           ├── service/       # 业务逻辑
│   │   │           ├── util/          # 工具类
│   │   │           ├── view/          # 视图组件
│   │   │           ├── Launcher.java  # 应用启动类
│   │   │           └── MainApplication.java # JavaFX主应用类
│   │   └── resources/
│   │       ├── css/      # 样式文件
│   │       ├── fxml/     # FXML布局文件
│   │       └── images/   # 图片资源
│   └── test/             # 测试代码
└── pom.xml               # Maven项目配置
```

## 数据库配置

在 `src/main/resources/database.properties` 文件中配置数据库连接信息:

### PostgreSQL
```
jdbc.driver=org.postgresql.Driver
jdbc.url=jdbc:postgresql://localhost:5432/monitor_db
jdbc.username=postgres
jdbc.password=password
```

### MySQL
```
jdbc.driver=com.mysql.cj.jdbc.Driver
jdbc.url=jdbc:mysql://localhost:3306/monitor_db?useSSL=false&serverTimezone=UTC
jdbc.username=root
jdbc.password=password
```

### SQLite (默认)
```
jdbc.driver=org.sqlite.JDBC
jdbc.url=jdbc:sqlite:monitor_data.db
jdbc.username=
jdbc.password=
```

## 使用Scene Builder进行UI开发

1. 下载并安装 [Scene Builder](https://gluonhq.com/products/scene-builder/)
2. 在 IntelliJ IDEA 中配置 Scene Builder 路径
3. 右键点击 FXML 文件，选择"Open in Scene Builder"进行可视化编辑 