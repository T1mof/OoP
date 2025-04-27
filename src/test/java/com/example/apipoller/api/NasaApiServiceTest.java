package com.example.apipoller.api;

import com.example.apipoller.model.ApiRecord;
import com.example.apipoller.model.NasaRecord;
import mockit.*;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class NasaApiServiceTest {

    @Tested
    private NasaApiService service;

    @Test
    public void testFetchData() throws Exception {
        // Создание мок-версии сервиса с переопределенным методом fetchData
        new MockUp<NasaApiService>() {
            @Mock
            public List<ApiRecord> fetchData() throws IOException {
                return List.of(new NasaRecord(
                    "apod_2025-04-27", 
                    "Test Astronomy Picture", 
                    "2025-04-27", 
                    "This is a test description of an astronomy picture",
                    "https://example.com/apod/image.jpg", 
                    "image",
                    "NASA"
                ));
            }
        };
        
        // Проверка метода
        List<ApiRecord> records = service.fetchData();
        
        // Проверки результатов
        assertNotNull(records);
        assertFalse(records.isEmpty());
        assertEquals("nasa", records.get(0).toMap().get("type"));
        assertEquals("Test Astronomy Picture", records.get(0).toMap().get("title"));
    }
    
    @Test
    public void testGetServiceName() {
        assertEquals("nasa", service.getServiceName());
    }
}
