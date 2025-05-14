package com.example.apipoller.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class ApiServiceFactoryTest {

    @Test
    public void testCreateServiceWithValidNames() {
        // Проверка создания всех поддерживаемых сервисов
        ApiService newsService = ApiServiceFactory.createService("news");
        assertNotNull(newsService);
        assertTrue(newsService instanceof NewsApiService);
        assertEquals("news", newsService.getServiceName());

        ApiService weatherService = ApiServiceFactory.createService("weather");
        assertNotNull(weatherService);
        assertTrue(weatherService instanceof WeatherApiService);
        assertEquals("weather", weatherService.getServiceName());

        ApiService nasaService = ApiServiceFactory.createService("nasa");
        assertNotNull(nasaService);
        assertTrue(nasaService instanceof NasaApiService);
        assertEquals("nasa", nasaService.getServiceName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"NEWS", "Weather", "NASA"})
    public void testCreateServiceCaseInsensitive(String serviceName) {
        // Проверка, что имена сервисов не чувствительны к регистру
        ApiService service = ApiServiceFactory.createService(serviceName);
        assertNotNull(service);
        assertEquals(serviceName.toLowerCase(), service.getServiceName());
    }

    @Test
    public void testCreateServiceWithInvalidName() {
        // Проверка выброса исключения для неизвестного сервиса
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ApiServiceFactory.createService("unknown_service")
        );
        assertTrue(exception.getMessage().contains("Unknown service"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"news", "weather", "nasa"})
    public void testIsServiceSupported_ValidServices(String serviceName) {
        assertTrue(ApiServiceFactory.isServiceSupported(serviceName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "invalid", "", "xyz"})
    public void testIsServiceSupported_InvalidServices(String serviceName) {
        assertFalse(ApiServiceFactory.isServiceSupported(serviceName));
    }
    
    @Test
    public void testIsServiceSupported_CaseInsensitive() {
        assertTrue(ApiServiceFactory.isServiceSupported("NEWS"));
        assertTrue(ApiServiceFactory.isServiceSupported("Weather"));
        assertTrue(ApiServiceFactory.isServiceSupported("nAsA"));
    }
}
