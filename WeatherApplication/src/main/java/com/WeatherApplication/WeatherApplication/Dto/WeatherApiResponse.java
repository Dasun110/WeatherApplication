package com.WeatherApplication.WeatherApplication.Dto;

import lombok.Data;

import java.util.List;

@Data
public class WeatherApiResponse {
    private List<WeatherData> list;

    @Data
    public static class WeatherData {
        private String dt_txt;
        private Main main;

        @Data
        public static class Main {
            private double temp;
        }
    }
}