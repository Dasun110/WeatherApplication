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
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherApplicationTests {

	@Mock
	private WebClient.Builder webClientBuilder;

	private WeatherService weatherService;

	@BeforeEach
	void setUp() throws Exception {
		// Create mock WebClient
		WebClient mockWebClient = mock(WebClient.class);
		WebClient.Builder baseUrlBuilder = mock(WebClient.Builder.class);

		when(webClientBuilder.baseUrl(any(String.class))).thenReturn(baseUrlBuilder);
		when(baseUrlBuilder.build()).thenReturn(mockWebClient);

		// Initialize service with mocked dependencies
		weatherService = new WeatherService(webClientBuilder);

		// Set API key via reflection
		Field apiKeyField = WeatherService.class.getDeclaredField("apiKey");
		apiKeyField.setAccessible(true);
		apiKeyField.set(weatherService, "test-api-key");
	}

	@Test
	void testGetWeatherSummary_Success() throws Exception {
		// Prepare test data
		WeatherApiResponse.WeatherData.Main main1 = new WeatherApiResponse.WeatherData.Main();
		main1.setTemp(20.0);
		WeatherApiResponse.WeatherData data1 = new WeatherApiResponse.WeatherData();
		data1.setDt_txt(LocalDate.now().minusDays(1) + " 12:00:00");
		data1.setMain(main1);

		WeatherApiResponse.WeatherData.Main main2 = new WeatherApiResponse.WeatherData.Main();
		main2.setTemp(25.0);
		WeatherApiResponse.WeatherData data2 = new WeatherApiResponse.WeatherData();
		data2.setDt_txt(LocalDate.now() + " 12:00:00");
		data2.setMain(main2);

		WeatherApiResponse response = new WeatherApiResponse();
		response.setList(Arrays.asList(data1, data2));

		// Mock WebClient chain
		WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
		WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
		ResponseSpec responseSpec = mock(ResponseSpec.class);

		// Get WebClient instance from service via reflection
		Field webClientField = WeatherService.class.getDeclaredField("webClient");
		webClientField.setAccessible(true);
		WebClient webClient = (WebClient) webClientField.get(weatherService);

		// Configure mock behavior
		when(webClient.get()).thenReturn(uriSpec);
		when(uriSpec.uri(any(Function.class))).thenReturn(headersSpec);
		when(headersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);  // Fix for NPE
		when(responseSpec.bodyToMono(WeatherApiResponse.class)).thenReturn(Mono.just(response));

		// Test the method
		CompletableFuture<WeatherSummaryDto> future = weatherService.getWeatherSummary("London");
		WeatherSummaryDto summary = future.get();

		// Verify results
		assertNotNull(summary);
		assertEquals("London", summary.getCity());
		assertEquals(22.5, summary.getAverageTemperature(), 0.01);
		assertEquals(LocalDate.now().toString(), summary.getHottestDay());
		assertEquals(LocalDate.now().minusDays(1).toString(), summary.getColdestDay());
	}

	@Test
	void testGetWeatherSummary_InvalidCity() {
		CompletableFuture<WeatherSummaryDto> future = weatherService.getWeatherSummary("");
		assertThrows(Exception.class, future::get);
	}

	@Test
	void testGetWeatherSummary_ApiFailure() throws Exception {
		// Mock WebClient chain
		WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
		WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
		ResponseSpec responseSpec = mock(ResponseSpec.class);

		// Get WebClient instance from service via reflection
		Field webClientField = WeatherService.class.getDeclaredField("webClient");
		webClientField.setAccessible(true);
		WebClient webClient = (WebClient) webClientField.get(weatherService);

		// Configure mock behavior
		when(webClient.get()).thenReturn(uriSpec);
		when(uriSpec.uri(any(Function.class))).thenReturn(headersSpec);
		when(headersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);  // Fix for NPE
		when(responseSpec.bodyToMono(WeatherApiResponse.class))
				.thenReturn(Mono.error(new RuntimeException("API failed")));

		// Test the method
		CompletableFuture<WeatherSummaryDto> future = weatherService.getWeatherSummary("London");
		assertThrows(Exception.class, future::get);
	}
}