package com.monitor.repository;

import com.monitor.model.MonitoringData;
import com.monitor.util.DatabaseUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcMonitoringDataRepository implements MonitoringDataRepository {

    @Override
    public MonitoringData save(MonitoringData data) {
        String sql;
        if (data.getId() == null) {
            sql = "INSERT INTO monitoring_data (device_id, timestamp, value, unit, parameter, location, alert, notes) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        } else {
            sql = "UPDATE monitoring_data SET device_id = ?, timestamp = ?, value = ?, unit = ?, parameter = ?, " +
                  "location = ?, alert = ?, notes = ? WHERE id = ? RETURNING id";
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, data.getDeviceId());
            stmt.setTimestamp(2, Timestamp.valueOf(data.getTimestamp()));
            stmt.setDouble(3, data.getValue());
            stmt.setString(4, data.getUnit());
            stmt.setString(5, data.getParameter());
            stmt.setString(6, data.getLocation());
            stmt.setBoolean(7, data.getAlert());
            stmt.setString(8, data.getNotes());
            
            if (data.getId() != null) {
                stmt.setLong(9, data.getId());
            }

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                data.setId(rs.getLong("id"));
            }
            return data;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving monitoring data", e);
        }
    }

    @Override
    public Optional<MonitoringData> findById(Long id) {
        String sql = "SELECT * FROM monitoring_data WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                MonitoringData data = mapResultSetToMonitoringData(rs);
                return Optional.of(data);
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error finding monitoring data by id", e);
        }
    }

    @Override
    public List<MonitoringData> findAll() {
        String sql = "SELECT * FROM monitoring_data ORDER BY timestamp DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<MonitoringData> dataList = new ArrayList<>();
            while (rs.next()) {
                dataList.add(mapResultSetToMonitoringData(rs));
            }
            return dataList;
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all monitoring data", e);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM monitoring_data WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting monitoring data", e);
        }
    }

    @Override
    public List<MonitoringData> findByDeviceId(String deviceId) {
        String sql = "SELECT * FROM monitoring_data WHERE device_id = ? ORDER BY timestamp DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, deviceId);
            ResultSet rs = stmt.executeQuery();
            
            List<MonitoringData> dataList = new ArrayList<>();
            while (rs.next()) {
                dataList.add(mapResultSetToMonitoringData(rs));
            }
            return dataList;
        } catch (SQLException e) {
            throw new RuntimeException("Error finding monitoring data by device id", e);
        }
    }

    @Override
    public List<MonitoringData> findByParameter(String parameter) {
        String sql = "SELECT * FROM monitoring_data WHERE parameter = ? ORDER BY timestamp DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, parameter);
            ResultSet rs = stmt.executeQuery();
            
            List<MonitoringData> dataList = new ArrayList<>();
            while (rs.next()) {
                dataList.add(mapResultSetToMonitoringData(rs));
            }
            return dataList;
        } catch (SQLException e) {
            throw new RuntimeException("Error finding monitoring data by parameter", e);
        }
    }

    @Override
    public List<MonitoringData> findByTimestampBetween(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT * FROM monitoring_data WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(start));
            stmt.setTimestamp(2, Timestamp.valueOf(end));
            ResultSet rs = stmt.executeQuery();
            
            List<MonitoringData> dataList = new ArrayList<>();
            while (rs.next()) {
                dataList.add(mapResultSetToMonitoringData(rs));
            }
            return dataList;
        } catch (SQLException e) {
            throw new RuntimeException("Error finding monitoring data by timestamp range", e);
        }
    }

    @Override
    public List<MonitoringData> findByLocationAndParameter(String location, String parameter) {
        String sql = "SELECT * FROM monitoring_data WHERE location = ? AND parameter = ? ORDER BY timestamp DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, location);
            stmt.setString(2, parameter);
            ResultSet rs = stmt.executeQuery();
            
            List<MonitoringData> dataList = new ArrayList<>();
            while (rs.next()) {
                dataList.add(mapResultSetToMonitoringData(rs));
            }
            return dataList;
        } catch (SQLException e) {
            throw new RuntimeException("Error finding monitoring data by location and parameter", e);
        }
    }

    @Override
    public List<MonitoringData> findByAlert(Boolean alert) {
        String sql = "SELECT * FROM monitoring_data WHERE alert = ? ORDER BY timestamp DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, alert);
            ResultSet rs = stmt.executeQuery();
            
            List<MonitoringData> dataList = new ArrayList<>();
            while (rs.next()) {
                dataList.add(mapResultSetToMonitoringData(rs));
            }
            return dataList;
        } catch (SQLException e) {
            throw new RuntimeException("Error finding monitoring data by alert status", e);
        }
    }

    private MonitoringData mapResultSetToMonitoringData(ResultSet rs) throws SQLException {
        MonitoringData data = new MonitoringData();
        data.setId(rs.getLong("id"));
        data.setDeviceId(rs.getString("device_id"));
        data.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
        data.setValue(rs.getDouble("value"));
        data.setUnit(rs.getString("unit"));
        data.setParameter(rs.getString("parameter"));
        data.setLocation(rs.getString("location"));
        data.setAlert(rs.getBoolean("alert"));
        data.setNotes(rs.getString("notes"));
        return data;
    }
} 