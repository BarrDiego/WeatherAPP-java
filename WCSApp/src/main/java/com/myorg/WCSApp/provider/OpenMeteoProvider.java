package com.myorg.WCSApp.provider;

import com.myorg.WCSApp.domain.CurrentWeatherRequest;
import com.myorg.WCSApp.domain.Location;
import com.myorg.WCSApp.domain.WeatherResponse;
import com.openmeteo.sdk.Unit;
import com.openmeteo.sdk.Variable;
import com.openmeteo.sdk.VariableWithValues;
import com.openmeteo.sdk.VariablesSearch;
import com.openmeteo.sdk.VariablesWithTime;
import com.openmeteo.sdk.WeatherApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Component
public class OpenMeteoProvider implements WeatherProvider {
    private final RestClient restClient;

    public OpenMeteoProvider(
            @Value("${weather.openmeteo.base-url}") String baseUrl,
            @Value("${weather.http.connect-timeout:2s}") Duration connectTimeout,
            @Value("${weather.http.read-timeout:5s}") Duration readTimeout) {
        this.restClient = WeatherRestClientFactory.create(baseUrl, connectTimeout, readTimeout);
    }

    @Override
    public String getProviderName() {
        return "open-meteo";
    }

    @Override
    public WeatherResponse fetchCurrentWeather(CurrentWeatherRequest request) {
        byte[] body;
        try {
            body = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/forecast")
                            .queryParam("latitude", request.getLatitude())
                            .queryParam("longitude", request.getLongitude())
                            .queryParam("current", String.join(",",
                                    "temperature_2m",
                                    "apparent_temperature",
                                    "relative_humidity_2m",
                                    "pressure_msl",
                                    "wind_speed_10m",
                                    "wind_direction_10m",
                                    "cloud_cover",
                                    "visibility",
                                    "weather_code"))
                            .queryParam("timezone", "auto")
                            .queryParam("wind_speed_unit", "ms")
                            .queryParam("format", "flatbuffers")
                            .build())
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 429 || status >= 500) {
                throw new RecoverableWeatherException("Open-Meteo is temporarily unavailable", ex);
            }
            throw new WeatherProviderException("Open-Meteo rejected the request (HTTP " + status + ")");
        } catch (ResourceAccessException ex) {
            throw new RecoverableWeatherException("Open-Meteo could not be reached", ex);
        }

        try {
            return mapResponse(decode(body), request);
        } catch (DateTimeException | IllegalArgumentException | IndexOutOfBoundsException ex) {
            throw new RecoverableWeatherException("Open-Meteo returned an invalid response", ex);
        }
    }

    private static WeatherApiResponse decode(byte[] body) {
        if (body == null || body.length < Integer.BYTES * 2) {
            throw new RecoverableWeatherException("Open-Meteo returned an empty or truncated response");
        }
        ByteBuffer buffer = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN);
        int prefixedSize = buffer.getInt(0);
        if (prefixedSize != body.length - Integer.BYTES) {
            throw new RecoverableWeatherException("Open-Meteo returned an invalid FlatBuffers payload");
        }
        buffer.position(Integer.BYTES);
        return WeatherApiResponse.getRootAsWeatherApiResponse(buffer);
    }

    private static WeatherResponse mapResponse(WeatherApiResponse apiResponse, CurrentWeatherRequest request) {
        VariablesWithTime current = apiResponse.current();
        if (current == null) {
            throw new RecoverableWeatherException("Open-Meteo response has no current conditions");
        }
        String timezone = apiResponse.timezone();
        if (timezone == null || timezone.isBlank()) {
            throw new RecoverableWeatherException("Open-Meteo response has no timezone");
        }

        ZoneId zone = ZoneId.of(timezone);
        Instant observedInstant = Instant.ofEpochSecond(current.time());
        WeatherResponse response = new WeatherResponse();
        response.setLocation(new Location((double) apiResponse.latitude(), (double) apiResponse.longitude(), timezone));
        response.setObservedAt(observedInstant.atZone(zone).toOffsetDateTime());
        response.setTemperatureC(value(current, Variable.temperature, 2, Unit.celsius, "temperature_2m"));
        response.setFeelsLikeC(value(current, Variable.apparent_temperature, 0, Unit.celsius, "apparent_temperature"));
        response.setConditionDescription(conditionDescription(
                value(current, Variable.weather_code, 0, Unit.wmo_code, "weather_code"), request.getLang()));
        response.setPressureHpa(value(current, Variable.pressure_msl, 0, Unit.hectopascal, "pressure_msl"));
        response.setHumidityPercent(value(current, Variable.relative_humidity, 2, Unit.percentage, "relative_humidity_2m"));
        response.setWindSpeedMps(value(current, Variable.wind_speed, 10, Unit.metre_per_second, "wind_speed_10m"));
        response.setWindDirectionDegrees(value(current, Variable.wind_direction, 10, Unit.degree_direction, "wind_direction_10m"));
        response.setCloudCoverPercent(value(current, Variable.cloud_cover, 0, Unit.percentage, "cloud_cover"));
        Double visibility = value(current, Variable.visibility, 0, Unit.metre, "visibility");
        response.setVisibilityMeters(visibility == null ? null : visibility.intValue());
        return response;
    }

    private static Double value(VariablesWithTime current, int variableId, int altitude, int expectedUnit, String name) {
        VariableWithValues variable = new VariablesSearch(current)
                .variable(variableId)
                .altitude(altitude)
                .first();
        if (variable == null) {
            return null;
        }
        if (variable.unit() != expectedUnit) {
            throw new RecoverableWeatherException("Open-Meteo returned an unexpected unit for " + name);
        }
        float value = variable.value();
        return Float.isFinite(value) ? (double) value : null;
    }

    private static String conditionDescription(Double code, String language) {
        if (code == null) {
            return null;
        }
        int wmoCode = code.intValue();
        boolean english = language != null && language.toLowerCase().startsWith("en");
        return switch (wmoCode) {
            case 0 -> english ? "clear sky" : "cielo despejado";
            case 1 -> english ? "mainly clear" : "mayormente despejado";
            case 2 -> english ? "partly cloudy" : "parcialmente nublado";
            case 3 -> english ? "overcast" : "nublado";
            case 45, 48 -> english ? "fog" : "niebla";
            case 51, 53, 55 -> english ? "drizzle" : "llovizna";
            case 56, 57 -> english ? "freezing drizzle" : "llovizna engelante";
            case 61, 63, 65 -> english ? "rain" : "lluvia";
            case 66, 67 -> english ? "freezing rain" : "lluvia engelante";
            case 71, 73, 75, 77 -> english ? "snowfall" : "nevadas";
            case 80, 81, 82 -> english ? "rain showers" : "chaparrones";
            case 85, 86 -> english ? "snow showers" : "chaparrones de nieve";
            case 95, 96, 99 -> english ? "thunderstorm" : "tormenta";
            default -> english ? "unknown conditions" : "condiciones desconocidas";
        };
    }
}
