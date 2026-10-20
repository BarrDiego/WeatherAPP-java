package com.myorg.WCSApp.service;

import com.myorg.WCSApp.domain.CurrentWeatherRequest;
import com.myorg.WCSApp.domain.Location;
import com.myorg.WCSApp.domain.WeatherResponse;
import com.myorg.WCSApp.provider.RecoverableWeatherException;
import com.myorg.WCSApp.provider.WeatherProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class WeatherQueryService {
    private final WeatherProvider primaryProvider;
    private final WeatherProvider secondaryProvider;

    public WeatherQueryService(@Qualifier("openWeatherProvider") WeatherProvider primaryProvider,
                              @Qualifier("openMeteoProvider") WeatherProvider secondaryProvider) {
        this.primaryProvider = primaryProvider;
        this.secondaryProvider = secondaryProvider;
    }

    public WeatherResponse getCurrentWeather(CurrentWeatherRequest request) {
        try {
            return primaryProvider.fetchCurrentWeather(request);
        } catch (RecoverableWeatherException ex) {
            return secondaryProvider.fetchCurrentWeather(request);
        }
    }

    public WeatherResponse buildSampleResponse(CurrentWeatherRequest request, String description) {
        WeatherResponse response = new WeatherResponse();
        response.setLocation(new Location(request.getLatitude(), request.getLongitude(), "America/Argentina/Buenos_Aires"));
        response.setObservedAt(OffsetDateTime.now());
        response.setTemperatureC(18.5D);
        response.setFeelsLikeC(17.8D);
        response.setConditionDescription(description);
        response.setPressureHpa(1013.0D);
        response.setHumidityPercent(60.0D);
        response.setWindSpeedMps(4.2D);
        response.setWindDirectionDegrees(120.0D);
        response.setCloudCoverPercent(30.0D);
        response.setVisibilityMeters(10000);
        return response;
    }
}
