package com.WeatherApplication.WeatherApplication.Controller;


import com.WeatherApplication.WeatherApplication.Dto.WeatherSummaryDto;
import com.WeatherApplication.WeatherApplication.Service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeatherController {

    private final WeatherService weatherService;

    @Autowired
    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/weather")
    public ResponseEntity<WeatherSummaryDto> getWeatherSummary(@RequestParam String city) {
        try {
            WeatherSummaryDto summary = weatherService.getWeatherSummary(city);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
}