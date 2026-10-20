package com.myorg.WCSApp.provider;

import com.myorg.WCSApp.domain.CurrentWeatherRequest;
import com.myorg.WCSApp.domain.WeatherResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenWeatherProvider implements WeatherProvider {
    private final String apiKey;

    public OpenWeatherProvider(@Value("${weather.openweather.api-key:demo-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getProviderName() {
        return "openweather";
    }

    @Override
    public WeatherResponse fetchCurrentWeather(CurrentWeatherRequest request) {
        if ("demo-key".equals(apiKey)) {
            throw new RecoverableWeatherException("OpenWeather is not configured; falling back to the secondary provider");
        }

        return new WeatherResponse();
    }
}
