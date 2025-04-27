package com.example.apipoller.api;

import com.example.apipoller.model.ApiRecord;
import com.example.apipoller.model.WeatherRecord;
import mockit.*;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class WeatherApiServiceTest {

    @Tested
    private WeatherApiService service;

    @Test
    public void testFetchData() throws Exception {
        // Создание мок-версии сервиса с переопределенным методом fetchData
        new MockUp<WeatherApiService>() {
            @Mock
            public List<ApiRecord> fetchData() throws IOException {
                return List.of(new WeatherRecord(
                    "Test City", 
                    20.5, 
                    5.2, 
                    80, 
                    "Sunny", 
                    System.currentTimeMillis() / 1000
                ));
            }
        };
        
        // Проверка метода
        List<ApiRecord> records = service.fetchData();
        
        // Проверки результатов
        assertNotNull(records);
        assertFalse(records.isEmpty());
        assertEquals("weather", records.get(0).toMap().get("type"));
        assertEquals("Test City", records.get(0).toMap().get("city"));
    }
    
    @Test
    public void testGetServiceName() {
        assertEquals("weather", service.getServiceName());
    }
}
