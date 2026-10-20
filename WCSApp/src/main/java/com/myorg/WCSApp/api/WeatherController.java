package com.myorg.WCSApp.api;

import com.myorg.WCSApp.domain.CurrentWeatherRequest;
import com.myorg.WCSApp.domain.WeatherResponse;
import com.myorg.WCSApp.service.WeatherQueryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class WeatherController {
    private final WeatherQueryService weatherQueryService;

    public WeatherController(WeatherQueryService weatherQueryService) {
        this.weatherQueryService = weatherQueryService;
    }

    @GetMapping("/weather")
    public ResponseEntity<WeatherResponse> getCurrentWeather(@Valid @ModelAttribute CurrentWeatherRequest request) {
        WeatherResponse response = weatherQueryService.getCurrentWeather(request);
        return ResponseEntity.ok(response);
    }
}
