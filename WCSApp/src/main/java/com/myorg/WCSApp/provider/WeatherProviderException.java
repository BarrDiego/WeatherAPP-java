package com.myorg.WCSApp.provider;

public class WeatherProviderException extends RuntimeException {
    public WeatherProviderException(String message) {
        super(message);
    }

    public WeatherProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
