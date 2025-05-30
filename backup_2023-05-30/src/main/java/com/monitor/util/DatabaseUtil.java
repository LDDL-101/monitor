package com.monitor.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseUtil {
    private static final Properties properties = new Properties();
    private static String url;
    private static String username;
    private static String password;
    private static boolean initialized = false;

    private DatabaseUtil() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        try (InputStream input = DatabaseUtil.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find database.properties");
            }

            properties.load(input);
            url = properties.getProperty("jdbc.url");
            username = properties.getProperty("jdbc.username");
            password = properties.getProperty("jdbc.password");

            // Load the JDBC driver
            String driver = properties.getProperty("jdbc.driver");
            Class.forName(driver);

            initialized = true;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Error initializing database connection", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            initialize();
        }
        return DriverManager.getConnection(url, username, password);
    }

    public static void createTablesIfNotExist() {
        try (Connection conn = getConnection()) {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS monitoring_data (" +
                    "id SERIAL PRIMARY KEY, " +
                    "device_id VARCHAR(50) NOT NULL, " +
                    "timestamp TIMESTAMP NOT NULL, " +
                    "value DOUBLE PRECISION NOT NULL, " +
                    "unit VARCHAR(20), " +
                    "parameter VARCHAR(50) NOT NULL, " +
                    "location VARCHAR(100), " +
                    "alert BOOLEAN DEFAULT FALSE, " +
                    "notes TEXT" +
                    ")";
            
            conn.createStatement().execute(createTableSQL);
            
            // Create indexes for better query performance
            conn.createStatement().execute(
                    "CREATE INDEX IF NOT EXISTS idx_monitoring_data_device_id ON monitoring_data(device_id)");
            conn.createStatement().execute(
                    "CREATE INDEX IF NOT EXISTS idx_monitoring_data_timestamp ON monitoring_data(timestamp)");
            conn.createStatement().execute(
                    "CREATE INDEX IF NOT EXISTS idx_monitoring_data_parameter ON monitoring_data(parameter)");
            conn.createStatement().execute(
                    "CREATE INDEX IF NOT EXISTS idx_monitoring_data_location ON monitoring_data(location)");
            conn.createStatement().execute(
                    "CREATE INDEX IF NOT EXISTS idx_monitoring_data_alert ON monitoring_data(alert)");
            
        } catch (SQLException e) {
            throw new RuntimeException("Error creating database tables", e);
        }
    }
} 