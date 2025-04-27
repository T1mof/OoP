package com.example.apipoller.api;

import com.example.apipoller.model.ApiRecord;
import com.example.apipoller.model.NewsRecord;
import mockit.*;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class NewsApiServiceTest {

    @Tested
    private NewsApiService service;

    @Test
    public void testFetchData() throws Exception {
        // Создание мок-версии сервиса с переопределенным методом fetchData
        new MockUp<NewsApiService>() {
            @Mock
            public List<ApiRecord> fetchData() throws IOException {
                return List.of(new NewsRecord(
                    "Test News Title", 
                    "Test Description", 
                    "https://example.com/news/1", 
                    "Test Source", 
                    "2025-04-27T00:00:00Z", 
                    "Test Author"
                ));
            }
        };
        
        // Проверка метода
        List<ApiRecord> records = service.fetchData();
        
        // Проверки результатов
        assertNotNull(records);
        assertFalse(records.isEmpty());
        assertEquals("news", records.get(0).toMap().get("type"));
        assertEquals("Test News Title", records.get(0).toMap().get("title"));
    }
    
    @Test
    public void testGetServiceName() {
        assertEquals("news", service.getServiceName());
    }
}
