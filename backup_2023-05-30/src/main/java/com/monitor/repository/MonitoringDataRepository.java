package com.monitor.repository;

import com.monitor.model.MonitoringData;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MonitoringDataRepository {
    
    // CRUD operations
    MonitoringData save(MonitoringData data);
    
    Optional<MonitoringData> findById(Long id);
    
    List<MonitoringData> findAll();
    
    void delete(Long id);
    
    // Custom queries
    List<MonitoringData> findByDeviceId(String deviceId);
    
    List<MonitoringData> findByParameter(String parameter);
    
    List<MonitoringData> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    List<MonitoringData> findByLocationAndParameter(String location, String parameter);
    
    List<MonitoringData> findByAlert(Boolean alert);
} 