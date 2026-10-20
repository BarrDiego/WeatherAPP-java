package com.myorg.WCSApp.client;

public final class ApiClient {
    private ApiClient() {
    }

    public static String buildWeatherRequest(double latitude, double longitude) {
        return String.format("https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&units=metric", latitude, longitude);
    }
}
