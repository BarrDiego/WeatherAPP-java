package com.myorg.WCSApp.provider;

import com.myorg.WCSApp.domain.CurrentWeatherRequest;
import com.myorg.WCSApp.domain.WeatherResponse;

public interface WeatherProvider {
    String getProviderName();

    WeatherResponse fetchCurrentWeather(CurrentWeatherRequest request);
}
