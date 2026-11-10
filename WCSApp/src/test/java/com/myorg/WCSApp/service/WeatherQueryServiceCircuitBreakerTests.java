package com.myorg.WCSApp.service;

import com.myorg.WCSApp.domain.CurrentWeatherRequest;
import com.myorg.WCSApp.domain.Location;
import com.myorg.WCSApp.domain.WeatherResponse;
import com.myorg.WCSApp.provider.RecoverableWeatherException;
import com.myorg.WCSApp.provider.WeatherProvider;
import com.myorg.WCSApp.provider.WeatherProviderException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherQueryServiceCircuitBreakerTests {
    @Test
    void opensPrimaryCircuitAfterRecoverableFailuresAndUsesFallbackWithoutCallingPrimary() {
        AtomicInteger primaryCalls = new AtomicInteger();
        AtomicInteger secondaryCalls = new AtomicInteger();
        WeatherProvider primary = provider("openweather", primaryCalls, true);
        WeatherProvider secondary = provider("open-meteo", secondaryCalls, false);
        CircuitBreakerRegistry registry = circuitBreakerRegistry();
        WeatherQueryService service = new WeatherQueryService(primary, secondary, true, registry);

        for (int i = 0; i < 3; i++) {
            assertEquals("open-meteo", service.getCurrentWeather(request()).getLocation().getTimezone());
        }

        assertEquals("open-meteo", service.getCurrentWeather(request()).getLocation().getTimezone());
        assertEquals(3, primaryCalls.get());
        assertEquals(4, secondaryCalls.get());
        assertEquals("OPEN", registry.circuitBreaker("openweather").getState().name());
    }

    @Test
    void opensEachProviderCircuitAndReturnsControlledProviderFailureWhenBothAreOpen() {
        AtomicInteger primaryCalls = new AtomicInteger();
        AtomicInteger secondaryCalls = new AtomicInteger();
        WeatherProvider primary = provider("openweather", primaryCalls, true);
        WeatherProvider secondary = provider("open-meteo", secondaryCalls, true);
        CircuitBreakerRegistry registry = circuitBreakerRegistry();
        WeatherQueryService service = new WeatherQueryService(primary, secondary, true, registry);

        for (int i = 0; i < 3; i++) {
            assertThrows(RecoverableWeatherException.class, () -> service.getCurrentWeather(request()));
        }
        assertThrows(RecoverableWeatherException.class, () -> service.getCurrentWeather(request()));

        assertEquals(3, primaryCalls.get());
        assertEquals(3, secondaryCalls.get());
        assertEquals("OPEN", registry.circuitBreaker("openweather").getState().name());
        assertEquals("OPEN", registry.circuitBreaker("open-meteo").getState().name());
    }

    @Test
    void doesNotOpenCircuitForNonRecoverableProviderErrors() {
        AtomicInteger secondaryCalls = new AtomicInteger();
        WeatherProvider primary = new WeatherProvider() {
            @Override
            public String getProviderName() {
                return "openweather";
            }

            @Override
            public WeatherResponse fetchCurrentWeather(CurrentWeatherRequest request) {
                throw new WeatherProviderException("Invalid provider credentials");
            }
        };
        CircuitBreakerRegistry registry = circuitBreakerRegistry();
        WeatherQueryService service = new WeatherQueryService(
                primary, provider("open-meteo", secondaryCalls, false), true, registry);

        assertThrows(WeatherProviderException.class, () -> service.getCurrentWeather(request()));

        assertEquals(0, secondaryCalls.get());
        assertEquals("CLOSED", registry.circuitBreaker("openweather").getState().name());
        assertEquals(0, registry.circuitBreaker("openweather").getMetrics().getNumberOfFailedCalls());
    }

    @Test
    void closesCircuitAfterSuccessfulHalfOpenProbe() throws InterruptedException {
        AtomicInteger primaryCalls = new AtomicInteger();
        AtomicInteger secondaryCalls = new AtomicInteger();
        WeatherProvider primary = new WeatherProvider() {
            @Override
            public String getProviderName() {
                return "openweather";
            }

            @Override
            public WeatherResponse fetchCurrentWeather(CurrentWeatherRequest request) {
                if (primaryCalls.incrementAndGet() <= 3) {
                    throw new RecoverableWeatherException("OpenWeather is unavailable");
                }
                WeatherResponse response = new WeatherResponse();
                response.setLocation(new Location(
                        request.getLatitude(), request.getLongitude(), "openweather"));
                return response;
            }
        };
        CircuitBreakerRegistry registry = circuitBreakerRegistry(Duration.ofMillis(50));
        WeatherQueryService service = new WeatherQueryService(
                primary, provider("open-meteo", secondaryCalls, false), true, registry);

        for (int i = 0; i < 3; i++) {
            service.getCurrentWeather(request());
        }
        assertEquals("OPEN", registry.circuitBreaker("openweather").getState().name());
        Thread.sleep(100);

        assertEquals("openweather", service.getCurrentWeather(request()).getLocation().getTimezone());
        assertEquals("CLOSED", registry.circuitBreaker("openweather").getState().name());
        assertEquals(3, secondaryCalls.get());
    }

    private static CircuitBreakerRegistry circuitBreakerRegistry() {
        return circuitBreakerRegistry(Duration.ofMinutes(1));
    }

    private static CircuitBreakerRegistry circuitBreakerRegistry(Duration waitDuration) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(3)
                .failureRateThreshold(50)
                .waitDurationInOpenState(waitDuration)
                .permittedNumberOfCallsInHalfOpenState(1)
                .recordException(exception -> exception instanceof RecoverableWeatherException)
                .build();
        return CircuitBreakerRegistry.of(config);
    }

    private static WeatherProvider provider(String name, AtomicInteger calls, boolean fail) {
        return new WeatherProvider() {
            @Override
            public String getProviderName() {
                return name;
            }

            @Override
            public WeatherResponse fetchCurrentWeather(CurrentWeatherRequest request) {
                calls.incrementAndGet();
                if (fail) {
                    throw new RecoverableWeatherException(name + " is unavailable");
                }
                WeatherResponse response = new WeatherResponse();
                response.setLocation(new com.myorg.WCSApp.domain.Location(
                        request.getLatitude(), request.getLongitude(), name));
                return response;
            }
        };
    }

    private static CurrentWeatherRequest request() {
        CurrentWeatherRequest request = new CurrentWeatherRequest();
        request.setLatitude(-34.6);
        request.setLongitude(-58.38);
        return request;
    }
}
