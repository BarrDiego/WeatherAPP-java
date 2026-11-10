package com.myorg.WCSApp.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.myorg.WCSApp.provider.WeatherProviderException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBindException(BindException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getBindingResult().getAllErrors().getFirst().getDefaultMessage());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(WeatherProviderException.class)
    public ResponseEntity<Map<String, Object>> handleWeatherProviderException() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", HttpStatus.BAD_GATEWAY.value());
        body.put("error", "Bad Gateway");
        body.put("message", "No se pudo obtener información meteorológica en este momento.");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }
}
