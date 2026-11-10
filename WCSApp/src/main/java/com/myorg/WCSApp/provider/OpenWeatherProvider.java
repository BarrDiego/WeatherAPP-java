package com.myorg.WCSApp.provider;

import com.myorg.WCSApp.domain.CurrentWeatherRequest;
import com.myorg.WCSApp.domain.Location;
import com.myorg.WCSApp.domain.WeatherResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class OpenWeatherProvider implements WeatherProvider {
    private final RestClient restClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public OpenWeatherProvider(
            @Value("${weather.openweather.base-url}") String baseUrl,
            @Value("${weather.openweather.api-key:}") String apiKey,
            @Value("${weather.http.connect-timeout:2s}") Duration connectTimeout,
            @Value("${weather.http.read-timeout:5s}") Duration readTimeout,
            ObjectMapper objectMapper) {
        this.restClient = WeatherRestClientFactory.create(baseUrl, connectTimeout, readTimeout);
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderName() {
        return "openweather";
    }

    @Override
    public WeatherResponse fetchCurrentWeather(CurrentWeatherRequest request) {
        if (apiKey == null || apiKey.isBlank() || "demo-key".equals(apiKey)) {
            throw new RecoverableWeatherException("OpenWeather is not configured; falling back to the secondary provider");
        }

        byte[] body;
        try {
            body = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/weather")
                            .queryParam("lat", request.getLatitude())
                            .queryParam("lon", request.getLongitude())
                            .queryParam("appid", apiKey)
                            .queryParam("units", "metric")
                            .queryParam("lang", request.getLang())
                            .build())
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 429 || status >= 500) {
                throw new RecoverableWeatherException("OpenWeather is temporarily unavailable", ex);
            }
            throw new WeatherProviderException("OpenWeather rejected the request (HTTP " + status + ")");
        } catch (ResourceAccessException ex) {
            throw new RecoverableWeatherException("OpenWeather could not be reached", ex);
        }

        try {
            if (body == null || body.length == 0) {
                throw new IOException("Empty response");
            }
            return mapResponse(objectMapper.readTree(body), request);
        } catch (IOException | DateTimeException | IllegalArgumentException ex) {
            throw new RecoverableWeatherException("OpenWeather returned an invalid response", ex);
        }
    }

    private WeatherResponse mapResponse(JsonNode root, CurrentWeatherRequest request) throws IOException {
        if (root == null || !root.isObject()) {
            throw new IOException("Response is not a JSON object");
        }
        JsonNode main = root.path("main");
        JsonNode weather = root.path("weather");
        JsonNode firstCondition = weather.isArray() && !weather.isEmpty() ? weather.get(0) : null;
        int timezoneOffset = requiredInt(root, "timezone");
        long observedEpochSeconds = requiredLong(root, "dt");

        WeatherResponse response = new WeatherResponse();
        response.setLocation(new Location(
                optionalDouble(root.path("coord"), "lat", request.getLatitude()),
                optionalDouble(root.path("coord"), "lon", request.getLongitude()),
                ZoneOffset.ofTotalSeconds(timezoneOffset).getId()));
        response.setObservedAt(OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(observedEpochSeconds), ZoneOffset.ofTotalSeconds(timezoneOffset)));
        response.setTemperatureC(requiredDouble(main, "temp"));
        response.setFeelsLikeC(optionalDouble(main, "feels_like"));
        response.setConditionDescription(optionalText(firstCondition, "description"));
        response.setPressureHpa(optionalDouble(main, "pressure"));
        response.setHumidityPercent(optionalDouble(main, "humidity"));
        response.setWindSpeedMps(optionalDouble(root.path("wind"), "speed"));
        response.setWindDirectionDegrees(optionalDouble(root.path("wind"), "deg"));
        response.setCloudCoverPercent(optionalDouble(root.path("clouds"), "all"));
        response.setVisibilityMeters(optionalInteger(root, "visibility"));
        return response;
    }

    private static double requiredDouble(JsonNode node, String field) throws IOException {
        Double value = optionalDouble(node, field);
        if (value == null) {
            throw new IOException("Missing numeric field: " + field);
        }
        return value;
    }

    private static int requiredInt(JsonNode node, String field) throws IOException {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()) {
            throw new IOException("Missing integer field: " + field);
        }
        return value.intValue();
    }

    private static long requiredLong(JsonNode node, String field) throws IOException {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
            throw new IOException("Missing integer field: " + field);
        }
        return value.longValue();
    }

    private static Double optionalDouble(JsonNode node, String field) throws IOException {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isNumber() || !Double.isFinite(value.doubleValue())) {
            throw new IOException("Invalid numeric field: " + field);
        }
        return value.doubleValue();
    }

    private static Double optionalDouble(JsonNode node, String field, Double defaultValue) throws IOException {
        Double value = optionalDouble(node, field);
        return value == null ? defaultValue : value;
    }

    private static Integer optionalInteger(JsonNode node, String field) throws IOException {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw new IOException("Invalid integer field: " + field);
        }
        return value.intValue();
    }

    private static String optionalText(JsonNode node, String field) throws IOException {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw new IOException("Invalid text field: " + field);
        }
        return value.textValue();
    }
}
