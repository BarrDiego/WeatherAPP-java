package com.myorg.WCSApp.provider;

import com.myorg.WCSApp.domain.CurrentWeatherRequest;
import com.myorg.WCSApp.domain.Location;
import com.myorg.WCSApp.domain.WeatherResponse;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class OpenMeteoProvider implements WeatherProvider {
    @Override
    public String getProviderName() {
        return "open-meteo";
    }

    @Override
    public WeatherResponse fetchCurrentWeather(CurrentWeatherRequest request) {
        WeatherResponse response = new WeatherResponse();
        response.setLocation(new Location(request.getLatitude(), request.getLongitude(), "America/Argentina/Buenos_Aires"));
        response.setObservedAt(OffsetDateTime.now());
        response.setTemperatureC(17.2D);
        response.setFeelsLikeC(16.5D);
        response.setConditionDescription("nublado");
        response.setPressureHpa(1014.0D);
        response.setHumidityPercent(63.0D);
        response.setWindSpeedMps(3.7D);
        response.setWindDirectionDegrees(95.0D);
        response.setCloudCoverPercent(65.0D);
        response.setVisibilityMeters(9500);
        return response;
    }
}
