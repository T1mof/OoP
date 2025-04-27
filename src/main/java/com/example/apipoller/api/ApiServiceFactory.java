package com.example.apipoller.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Фабрика для создания сервисов API по их названию
 */
public class ApiServiceFactory {
    private static final Map<String, ApiService> services = new HashMap<>();
    
    static {
        // Регистрация доступных сервисов
        services.put("news", new NewsApiService());
        services.put("weather", new WeatherApiService());
        services.put("nasa", new NasaApiService());
    }
    
    /**
     * Создает сервис API по его названию
     * @param serviceName название сервиса
     * @return экземпляр ApiService или null, если сервис не найден
     */
    public static ApiService createService(String serviceName) {
        ApiService service = services.get(serviceName.toLowerCase());
        if (service == null) {
            throw new IllegalArgumentException("Unknown service: " + serviceName);
        }
        return service;
    }
    
    /**
     * Проверяет, поддерживается ли сервис с указанным названием
     * @param serviceName название сервиса
     * @return true, если сервис поддерживается
     */
    public static boolean isServiceSupported(String serviceName) {
        return services.containsKey(serviceName.toLowerCase());
    }
}
