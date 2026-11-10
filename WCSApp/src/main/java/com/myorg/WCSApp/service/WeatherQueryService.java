package com.myorg.WCSApp.service;

import com.myorg.WCSApp.domain.CurrentWeatherRequest;
import com.myorg.WCSApp.domain.WeatherResponse;
import com.myorg.WCSApp.provider.RecoverableWeatherException;
import com.myorg.WCSApp.provider.WeatherProvider;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WeatherQueryService {
    private final WeatherProvider primaryProvider;
    private final WeatherProvider secondaryProvider;
    private final boolean fallbackEnabled;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public WeatherQueryService(@Qualifier("openWeatherProvider") WeatherProvider primaryProvider,
                               @Qualifier("openMeteoProvider") WeatherProvider secondaryProvider,
                               @Value("${weather.fallback.enabled:true}") boolean fallbackEnabled,
                               CircuitBreakerRegistry circuitBreakerRegistry) {
        this.primaryProvider = primaryProvider;
        this.secondaryProvider = secondaryProvider;
        this.fallbackEnabled = fallbackEnabled;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    public WeatherResponse getCurrentWeather(CurrentWeatherRequest request) {
        try {
            return fetchWithCircuitBreaker(primaryProvider, request);
        } catch (RecoverableWeatherException ex) {
            if (!fallbackEnabled) {
                throw ex;
            }
            return fetchWithCircuitBreaker(secondaryProvider, request);
        }
    }

    private WeatherResponse fetchWithCircuitBreaker(WeatherProvider provider, CurrentWeatherRequest request) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(provider.getProviderName());
        try {
            return circuitBreaker.executeSupplier(() -> provider.fetchCurrentWeather(request));
        } catch (CallNotPermittedException ex) {
            throw new RecoverableWeatherException(
                    provider.getProviderName() + " circuit breaker is open", ex);
        }
    }
}
