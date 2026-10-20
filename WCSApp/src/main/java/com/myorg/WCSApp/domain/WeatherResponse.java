package com.myorg.WCSApp.domain;

import java.time.OffsetDateTime;

public class WeatherResponse {
    private Location location;
    private OffsetDateTime observedAt;
    private Double temperatureC;
    private Double feelsLikeC;
    private String conditionDescription;
    private Double pressureHpa;
    private Double humidityPercent;
    private Double windSpeedMps;
    private Double windDirectionDegrees;
    private Double cloudCoverPercent;
    private Integer visibilityMeters;

    public WeatherResponse() {
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public OffsetDateTime getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(OffsetDateTime observedAt) {
        this.observedAt = observedAt;
    }

    public Double getTemperatureC() {
        return temperatureC;
    }

    public void setTemperatureC(Double temperatureC) {
        this.temperatureC = temperatureC;
    }

    public Double getFeelsLikeC() {
        return feelsLikeC;
    }

    public void setFeelsLikeC(Double feelsLikeC) {
        this.feelsLikeC = feelsLikeC;
    }

    public String getConditionDescription() {
        return conditionDescription;
    }

    public void setConditionDescription(String conditionDescription) {
        this.conditionDescription = conditionDescription;
    }

    public Double getPressureHpa() {
        return pressureHpa;
    }

    public void setPressureHpa(Double pressureHpa) {
        this.pressureHpa = pressureHpa;
    }

    public Double getHumidityPercent() {
        return humidityPercent;
    }

    public void setHumidityPercent(Double humidityPercent) {
        this.humidityPercent = humidityPercent;
    }

    public Double getWindSpeedMps() {
        return windSpeedMps;
    }

    public void setWindSpeedMps(Double windSpeedMps) {
        this.windSpeedMps = windSpeedMps;
    }

    public Double getWindDirectionDegrees() {
        return windDirectionDegrees;
    }

    public void setWindDirectionDegrees(Double windDirectionDegrees) {
        this.windDirectionDegrees = windDirectionDegrees;
    }

    public Double getCloudCoverPercent() {
        return cloudCoverPercent;
    }

    public void setCloudCoverPercent(Double cloudCoverPercent) {
        this.cloudCoverPercent = cloudCoverPercent;
    }

    public Integer getVisibilityMeters() {
        return visibilityMeters;
    }

    public void setVisibilityMeters(Integer visibilityMeters) {
        this.visibilityMeters = visibilityMeters;
    }
}
