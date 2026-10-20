package com.myorg.WCSApp.provider;

public class RecoverableWeatherException extends WeatherProviderException {
    public RecoverableWeatherException(String message) {
        super(message);
    }

    public RecoverableWeatherException(String message, Throwable cause) {
        super(message, cause);
    }
}
