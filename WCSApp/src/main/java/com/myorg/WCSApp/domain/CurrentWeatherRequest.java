package com.myorg.WCSApp.domain;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public class CurrentWeatherRequest {
    @NotNull(message = "latitude is required")
    private Double latitude;

    @NotNull(message = "longitude is required")
    private Double longitude;

    private String lang = "es";

    @AssertTrue(message = "latitude must be between -90 and 90")
    public boolean isLatitudeValid() {
        return latitude == null || (latitude >= -90D && latitude <= 90D);
    }

    @AssertTrue(message = "longitude must be between -180 and 180")
    public boolean isLongitudeValid() {
        return longitude == null || (longitude >= -180D && longitude <= 180D);
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

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang == null ? "es" : lang;
    }
}
