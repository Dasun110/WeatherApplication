package com.WeatherApplication.WeatherApplication.Controller;

import com.WeatherApplication.WeatherApplication.Dto.WeatherSummaryDto;
import com.WeatherApplication.WeatherApplication.Service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
public class WeatherController {

    private final WeatherService weatherService;

    @Autowired
    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/weather")
    public ResponseEntity<?> getWeatherSummary(@RequestParam String city) {
        try {
            CompletableFuture<WeatherSummaryDto> future = weatherService.getWeatherSummary(city);
            WeatherSummaryDto summary = future.get(); // Block until result is available
            return ResponseEntity.ok(summary);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(500).body(new ErrorResponse("Interrupted while fetching weather data: " + e.getMessage()));
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Invalid input: " + e.getCause().getMessage()));
            }
            return ResponseEntity.status(500).body(new ErrorResponse("Server error: " + e.getCause().getMessage()));
        }
    }
    private static class ErrorResponse {
        private final String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }
}