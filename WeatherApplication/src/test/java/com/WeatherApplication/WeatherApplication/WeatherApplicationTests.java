package com.WeatherApplication.WeatherApplication;

import com.WeatherApplication.WeatherApplication.Dto.WeatherApiResponse;
import com.WeatherApplication.WeatherApplication.Dto.WeatherSummaryDto;
import com.WeatherApplication.WeatherApplication.Service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherApplicationTests {

	@Mock
	private WebClient webClient;

	@Mock
	private WebClient.Builder webClientBuilder;

	@Mock
	@SuppressWarnings("rawtypes")
	private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

	@Mock
	@SuppressWarnings("rawtypes")
	private WebClient.RequestHeadersSpec requestHeadersSpec;

	@Mock
	private WebClient.ResponseSpec responseSpec;

	private WeatherService weatherService;

	@BeforeEach
	void setUp() throws Exception {
		when(webClientBuilder.baseUrl(any(String.class))).thenReturn(webClientBuilder);
		when(webClientBuilder.build()).thenReturn(webClient);

		// Create manually AFTER mocks are setup
		weatherService = new WeatherService(webClientBuilder);

		// Inject API key via reflection
		Field apiKeyField = WeatherService.class.getDeclaredField("apiKey");
		apiKeyField.setAccessible(true);
		apiKeyField.set(weatherService, "dummy_api_key");
	}

	@Test
	void testGetWeatherSummary_Success() {
		LocalDate today = LocalDate.now();
		LocalDate yesterday = today.minusDays(1);
		LocalDate twoDaysAgo = today.minusDays(2);

		WeatherApiResponse response = new WeatherApiResponse();

		WeatherApiResponse.WeatherData data1 = new WeatherApiResponse.WeatherData();
		data1.setDt_txt(twoDaysAgo + " 12:00:00");
		WeatherApiResponse.WeatherData.Main main1 = new WeatherApiResponse.WeatherData.Main();
		main1.setTemp(20.0);
		data1.setMain(main1);

		WeatherApiResponse.WeatherData data2 = new WeatherApiResponse.WeatherData();
		data2.setDt_txt(yesterday + " 12:00:00");
		WeatherApiResponse.WeatherData.Main main2 = new WeatherApiResponse.WeatherData.Main();
		main2.setTemp(25.0);
		data2.setMain(main2);

		response.setList(Arrays.asList(data1, data2));

		// Mock WebClient call chain
		when(webClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(WeatherApiResponse.class)).thenReturn(Mono.just(response));

		WeatherSummaryDto summary = weatherService.getWeatherSummary("London");

		assertNotNull(summary);
		assertEquals("London", summary.getCity());
		assertEquals(22.5, summary.getAverageTemperature(), 0.01);
		assertEquals(yesterday.toString(), summary.getHottestDay());
		assertEquals(twoDaysAgo.toString(), summary.getColdestDay());
	}

	@Test
	void testGetWeatherSummary_InvalidCity() {
		assertThrows(IllegalArgumentException.class, () -> weatherService.getWeatherSummary(""));
	}

	@Test
	void testGetWeatherSummary_ApiFailure() {
		when(webClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(WeatherApiResponse.class)).thenReturn(Mono.error(new RuntimeException("API error")));

		assertThrows(RuntimeException.class, () -> weatherService.getWeatherSummary("London"));
	}
}
