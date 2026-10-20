package com.myorg.WCSApp.domain;

public class Location {
    private Double latitude;
    private Double longitude;
    private String timezone;

    public Location() {
    }

    public Location(Double latitude, Double longitude, String timezone) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timezone = timezone;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
