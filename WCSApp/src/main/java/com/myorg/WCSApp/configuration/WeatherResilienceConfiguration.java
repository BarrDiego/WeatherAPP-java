package com.myorg.WCSApp.configuration;

import com.myorg.WCSApp.provider.RecoverableWeatherException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class WeatherResilienceConfiguration {
    @Bean
    CircuitBreakerRegistry weatherCircuitBreakerRegistry(
            @Value("${weather.circuit-breaker.sliding-window-size:10}") int slidingWindowSize,
            @Value("${weather.circuit-breaker.minimum-number-of-calls:5}") int minimumNumberOfCalls,
            @Value("${weather.circuit-breaker.failure-rate-threshold:50}") float failureRateThreshold,
            @Value("${weather.circuit-breaker.wait-duration-in-open-state:30s}") Duration waitDuration,
            @Value("${weather.circuit-breaker.permitted-calls-in-half-open-state:2}") int permittedCallsInHalfOpenState) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(waitDuration)
                .permittedNumberOfCallsInHalfOpenState(permittedCallsInHalfOpenState)
                .recordException(exception -> exception instanceof RecoverableWeatherException)
                .build();
        return CircuitBreakerRegistry.of(config);
    }
}
