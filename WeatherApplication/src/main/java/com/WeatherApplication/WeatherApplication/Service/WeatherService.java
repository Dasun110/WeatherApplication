package com.WeatherApplication.WeatherApplication.Service;


import com.WeatherApplication.WeatherApplication.Dto.WeatherApiResponse;
import com.WeatherApplication.WeatherApplication.Dto.WeatherSummaryDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WeatherService {

    private final WebClient webClient;

    @Value("${openweathermap.api.key}")
    private String apiKey;

    @Autowired
    public WeatherService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.openweathermap.org/data/2.5").build();
    }

    @Async
    @Cacheable(value = "weatherCache", key = "#city", unless = "#result == null")
    public WeatherSummaryDto getWeatherSummary(String city) {
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("City name cannot be empty");
        }

        Mono<WeatherApiResponse> responseMono = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/forecast")
                        .queryParam("q", city)
                        .queryParam("appid", apiKey)
                        .queryParam("units", "metric")
                        .build())
                .retrieve()
                .bodyToMono(WeatherApiResponse.class);

        WeatherApiResponse response;
        try {
            response = responseMono.block();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch weather data for " + city, e);
        }

        if (response == null || response.getList() == null) {
            throw new RuntimeException("Invalid response from weather API");
        }

        // Filter data for the last 7 days
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(7);

        List<WeatherApiResponse.WeatherData> weatherData = response.getList().stream()
                .filter(data -> {
                    LocalDate date = LocalDate.parse(data.getDt_txt().substring(0, 10));
                    return !date.isBefore(sevenDaysAgo) && !date.isAfter(today);
                })
                .toList();

        if (weatherData.isEmpty()) {
            throw new RuntimeException("No weather data available for the last 7 days");
        }

        // Group by date and compute daily averages
        Map<LocalDate, Double> dailyAvgTemps = weatherData.stream()
                .collect(Collectors.groupingBy(
                        data -> LocalDate.parse(data.getDt_txt().substring(0, 10)),
                        Collectors.averagingDouble(data -> data.getMain().getTemp())
                ));

        double averageTemperature = dailyAvgTemps.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        Map.Entry<LocalDate, Double> hottestDay = dailyAvgTemps.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        Map.Entry<LocalDate, Double> coldestDay = dailyAvgTemps.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .orElse(null);

        return new WeatherSummaryDto(
                city,
                averageTemperature,
                hottestDay != null ? hottestDay.getKey().format(DateTimeFormatter.ISO_LOCAL_DATE) : null,
                coldestDay != null ? coldestDay.getKey().format(DateTimeFormatter.ISO_LOCAL_DATE) : null
        );
    }
}