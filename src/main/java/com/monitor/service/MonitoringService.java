package com.monitor.service;

import com.monitor.model.MonitoringData;
import com.monitor.repository.MonitoringDataRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class MonitoringService {
    private final MonitoringDataRepository repository;

    public MonitoringService(MonitoringDataRepository repository) {
        this.repository = repository;
    }

    public MonitoringData saveData(MonitoringData data) {
        // Add business logic as needed before saving
        if (data.getTimestamp() == null) {
            data.setTimestamp(LocalDateTime.now());
        }
        
        // Check alert conditions
        checkAlertConditions(data);
        
        return repository.save(data);
    }

    public Optional<MonitoringData> getDataById(Long id) {
        return repository.findById(id);
    }

    public List<MonitoringData> getAllData() {
        return repository.findAll();
    }

    public void deleteData(Long id) {
        repository.delete(id);
    }

    public List<MonitoringData> getDataByDeviceId(String deviceId) {
        return repository.findByDeviceId(deviceId);
    }

    public List<MonitoringData> getDataByParameter(String parameter) {
        return repository.findByParameter(parameter);
    }

    public List<MonitoringData> getDataByTimeRange(LocalDateTime start, LocalDateTime end) {
        return repository.findByTimestampBetween(start, end);
    }

    public List<MonitoringData> getDataByLocationAndParameter(String location, String parameter) {
        return repository.findByLocationAndParameter(location, parameter);
    }

    public List<MonitoringData> getAlerts() {
        return repository.findByAlert(true);
    }
    
    private void checkAlertConditions(MonitoringData data) {
        // Implement alert logic based on parameters and values
        // This is a simplified example
        if (data.getParameter().equals("temperature")) {
            if (data.getValue() > 30.0) {
                data.setAlert(true);
            }
        } else if (data.getParameter().equals("humidity")) {
            if (data.getValue() > 80.0) {
                data.setAlert(true);
            }
        } else if (data.getParameter().equals("pressure")) {
            if (data.getValue() < 990.0) {
                data.setAlert(true);
            }
        }
    }
} 