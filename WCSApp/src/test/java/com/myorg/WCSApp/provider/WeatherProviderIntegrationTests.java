package com.myorg.WCSApp.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.flatbuffers.FlatBufferBuilder;
import com.myorg.WCSApp.domain.CurrentWeatherRequest;
import com.myorg.WCSApp.domain.WeatherResponse;
import com.myorg.WCSApp.service.WeatherQueryService;
import com.openmeteo.sdk.Unit;
import com.openmeteo.sdk.Variable;
import com.openmeteo.sdk.VariableWithValues;
import com.openmeteo.sdk.VariablesWithTime;
import com.openmeteo.sdk.WeatherApiResponse;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherProviderIntegrationTests {
    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<String> requestQuery = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void openWeatherMapsResponseAndSendsQueryParameters() {
        respond("/weather", 200, """
                {
                  "coord":{"lon":-58.38,"lat":-34.60},
                  "weather":[{"description":"cielo claro"}],
                  "main":{"temp":21.4,"feels_like":20.8,"pressure":1012,"humidity":58},
                  "visibility":10000,
                  "wind":{"speed":3.2,"deg":180},
                  "clouds":{"all":12},
                  "dt":1794326400,
                  "timezone":-10800
                }
                """);

        OpenWeatherProvider provider = new OpenWeatherProvider(
                baseUrl,
                "test-api-key",
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                new ObjectMapper());
        WeatherResponse response = provider.fetchCurrentWeather(request());

        assertEquals(21.4, response.getTemperatureC());
        assertEquals("cielo claro", response.getConditionDescription());
        assertEquals(-58.38, response.getLocation().getLongitude());
        assertEquals("-03:00", response.getLocation().getTimezone());
        assertEquals(10000, response.getVisibilityMeters());
        assertTrue(requestQuery.get().contains("appid=test-api-key"));
        assertTrue(requestQuery.get().contains("units=metric"));
        assertTrue(requestQuery.get().contains("lang=es"));
        assertTrue(requestQuery.get().contains("lat=-34.6"));
    }

    @Test
    void openMeteoDecodesFlatBuffersResponseAndRequestsCurrentVariables() {
        respond("/forecast", 200, flatBuffersResponse());

        OpenMeteoProvider provider = new OpenMeteoProvider(
                baseUrl,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1));
        WeatherResponse response = provider.fetchCurrentWeather(request());

        assertEquals(18.5, response.getTemperatureC());
        assertEquals(17.2, response.getFeelsLikeC(), 0.001);
        assertEquals("parcialmente nublado", response.getConditionDescription());
        assertEquals(68.0, response.getHumidityPercent(), 0.001);
        assertEquals(12.4, response.getWindSpeedMps(), 0.001);
        assertEquals("America/Argentina/Buenos_Aires", response.getLocation().getTimezone());
        assertNotNull(response.getObservedAt());
        assertTrue(requestQuery.get().contains("format=flatbuffers"));
        assertTrue(requestQuery.get().contains("timezone=auto"));
        assertTrue(requestQuery.get().contains("temperature_2m"));
    }

    @Test
    void missingOpenWeatherKeyTriggersOpenMeteoFallback() {
        OpenWeatherProvider primary = new OpenWeatherProvider(
                baseUrl,
                "",
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                new ObjectMapper());
        WeatherResponse fallbackResponse = new WeatherResponse();
        WeatherQueryService service = new WeatherQueryService(
                primary, new StubWeatherProvider(fallbackResponse), true, CircuitBreakerRegistry.ofDefaults());

        assertEquals(fallbackResponse, service.getCurrentWeather(request()));
        assertTrue(requestQuery.get() == null);
    }

    @Test
    void recoverableOpenWeatherFailureIsNotFallbackWhenFallbackIsDisabled() {
        OpenWeatherProvider primary = new OpenWeatherProvider(
                baseUrl,
                "",
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                new ObjectMapper());
        WeatherQueryService service = new WeatherQueryService(
                primary, new StubWeatherProvider(new WeatherResponse()), false, CircuitBreakerRegistry.ofDefaults());

        assertThrows(RecoverableWeatherException.class, () -> service.getCurrentWeather(request()));
    }

    @Test
    void invalidOpenMeteoPayloadIsReportedAsRecoverableProviderFailure() {
        respond("/forecast", 200, new byte[]{1, 2, 3});
        OpenMeteoProvider provider = new OpenMeteoProvider(
                baseUrl,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1));

        assertThrows(RecoverableWeatherException.class, () -> provider.fetchCurrentWeather(request()));
    }

    private void respond(String path, int status, String body) {
        respond(path, status, body.getBytes(StandardCharsets.UTF_8));
    }

    private void respond(String path, int status, byte[] body) {
        server.createContext(path, exchange -> {
            requestQuery.set(exchange.getRequestURI().getRawQuery());
            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            exchange.sendResponseHeaders(status, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
    }

    private static CurrentWeatherRequest request() {
        CurrentWeatherRequest request = new CurrentWeatherRequest();
        request.setLatitude(-34.6);
        request.setLongitude(-58.38);
        request.setLang("es");
        return request;
    }

    private static byte[] flatBuffersResponse() {
        FlatBufferBuilder builder = new FlatBufferBuilder();
        int[] variables = {
                variable(builder, Variable.temperature, Unit.celsius, 18.5f, 2),
                variable(builder, Variable.apparent_temperature, Unit.celsius, 17.2f, 0),
                variable(builder, Variable.relative_humidity, Unit.percentage, 68f, 2),
                variable(builder, Variable.pressure_msl, Unit.hectopascal, 1014f, 0),
                variable(builder, Variable.wind_speed, Unit.metre_per_second, 12.4f, 10),
                variable(builder, Variable.wind_direction, Unit.degree_direction, 195f, 10),
                variable(builder, Variable.cloud_cover, Unit.percentage, 50f, 0),
                variable(builder, Variable.visibility, Unit.metre, 12000f, 0),
                variable(builder, Variable.weather_code, Unit.wmo_code, 2f, 0)
        };
        int variablesVector = VariablesWithTime.createVariablesVector(builder, variables);
        int current = VariablesWithTime.createVariablesWithTime(builder, 1794326400L, 0L, 0, variablesVector);
        int timezone = builder.createString("America/Argentina/Buenos_Aires");
        int timezoneAbbreviation = builder.createString("GMT-3");
        int response = WeatherApiResponse.createWeatherApiResponse(
                builder,
                -34.6f,
                -58.38f,
                25f,
                0f,
                0L,
                0,
                -10800,
                timezone,
                timezoneAbbreviation,
                current,
                0,
                0,
                0,
                0);
        WeatherApiResponse.finishSizePrefixedWeatherApiResponseBuffer(builder, response);
        return builder.sizedByteArray();
    }

    private static int variable(
            FlatBufferBuilder builder, int variable, int unit, float value, int altitude) {
        return VariableWithValues.createVariableWithValues(
                builder, variable, unit, value, 0, 0, (short) altitude, 0, (short) 0, (short) 0, (short) 0, (short) 0);
    }

    private record StubWeatherProvider(WeatherResponse response) implements WeatherProvider {
        @Override
        public String getProviderName() {
            return "open-meteo";
        }

        @Override
        public WeatherResponse fetchCurrentWeather(CurrentWeatherRequest request) {
            return response;
        }
    }
}
