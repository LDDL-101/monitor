package com.monitor.model;

import java.time.LocalDateTime;

public class MonitoringData {
    private Long id;
    private String deviceId;
    private LocalDateTime timestamp;
    private Double value;
    private String unit;
    private String parameter;
    private String location;
    private Boolean alert;
    private String notes;

    public MonitoringData() {
    }

    public MonitoringData(Long id, String deviceId, LocalDateTime timestamp, Double value, String unit, 
                          String parameter, String location, Boolean alert, String notes) {
        this.id = id;
        this.deviceId = deviceId;
        this.timestamp = timestamp;
        this.value = value;
        this.unit = unit;
        this.parameter = parameter;
        this.location = location;
        this.alert = alert;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Boolean getAlert() {
        return alert;
    }

    public void setAlert(Boolean alert) {
        this.alert = alert;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "MonitoringData{" +
                "id=" + id +
                ", deviceId='" + deviceId + '\'' +
                ", timestamp=" + timestamp +
                ", value=" + value +
                ", unit='" + unit + '\'' +
                ", parameter='" + parameter + '\'' +
                ", location='" + location + '\'' +
                ", alert=" + alert +
                ", notes='" + notes + '\'' +
                '}';
    }
} 