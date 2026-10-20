package com.myorg.WCSApp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/legacy")
public class ApiController {

    @GetMapping("/weather")
    public Map<String, String> legacyWeatherEndpoint() {
        return Map.of("message", "This endpoint is deprecated. Use /api/weather instead.");
    }
}
