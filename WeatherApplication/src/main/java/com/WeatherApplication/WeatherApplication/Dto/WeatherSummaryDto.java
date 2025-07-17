package com.WeatherApplication.WeatherApplication.Dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherSummaryDto {
    private String city;
    private double averageTemperature;
    private String hottestDay;
    private String coldestDay;
}