package com.example.apipoller.api;

import com.example.apipoller.model.ApiRecord;
import com.example.apipoller.model.WeatherRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WeatherApiServiceTest {

    @Mock
    private CloseableHttpClient mockHttpClient;
    
    @Mock
    private CloseableHttpResponse mockResponse;
    
    @Mock
    private HttpEntity mockEntity;
    
    private WeatherApiService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        service = new WeatherApiService() {
            @Override
            protected CloseableHttpClient createHttpClient() {
                return mockHttpClient;
            }
        };
    }

    @Test
    public void testGetServiceName() {
        assertEquals("weather", service.getServiceName());
    }

    @Test
    public void testFetchData_Success() throws IOException {
        // Создаем тестовый JSON ответ
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("name", "TestCity");
        rootNode.put("dt", 1234567890);
        ObjectNode mainNode = objectMapper.createObjectNode();
        mainNode.put("temp", 25.5);
        mainNode.put("humidity", 60);
        rootNode.set("main", mainNode);
        ObjectNode windNode = objectMapper.createObjectNode();
        windNode.put("speed", 5.5);
        rootNode.set("wind", windNode);
        ArrayNode weatherArray = objectMapper.createArrayNode();
        ObjectNode weatherNode = objectMapper.createObjectNode();
        weatherNode.put("main", "Clear");
        weatherArray.add(weatherNode);
        rootNode.set("weather", weatherArray);

        String jsonResponse = objectMapper.writeValueAsString(rootNode);

        when(mockResponse.getCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream(jsonResponse.getBytes()));

        doAnswer(invocation -> {
            HttpClientResponseHandler<?> handler = invocation.getArgument(1);
            return handler.handleResponse(mockResponse);
        }).when(mockHttpClient).execute(any(ClassicHttpRequest.class), (HttpClientResponseHandler<?>) any());

        List<ApiRecord> records = service.fetchData();

        assertNotNull(records);
        assertEquals(1, records.size());
        WeatherRecord record = (WeatherRecord) records.get(0);
        assertEquals("TestCity_1234567890", record.getId());
        assertEquals("TestCity", record.toMap().get("city"));
        assertEquals(25.5, record.toMap().get("temperature"));
        assertEquals(60, record.toMap().get("humidity"));
        assertEquals(5.5, record.toMap().get("windSpeed"));
        assertEquals("Clear", record.toMap().get("condition"));
    }

    @Test
    public void testFetchData_ErrorStatus() throws IOException {
        when(mockResponse.getCode()).thenReturn(404);
        when(mockResponse.getEntity()).thenReturn(mockEntity);

        doAnswer(invocation -> {
            HttpClientResponseHandler<?> handler = invocation.getArgument(1);
            return handler.handleResponse(mockResponse);
        }).when(mockHttpClient).execute(any(ClassicHttpRequest.class), (HttpClientResponseHandler<?>) any());

        assertThrows(IOException.class, () -> service.fetchData());
    }

    @Test
    public void testFetchData_EmptyWeatherArray() throws IOException {
        // Тестирование ситуации с пустым массивом погоды
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("name", "TestCity");
        rootNode.put("dt", 1234567890);
        rootNode.set("main", objectMapper.createObjectNode().put("temp", 25.5).put("humidity", 60));
        rootNode.set("wind", objectMapper.createObjectNode().put("speed", 5.5));
        rootNode.set("weather", objectMapper.createArrayNode()); // Пустой массив weather

        String jsonResponse = objectMapper.writeValueAsString(rootNode);

        when(mockResponse.getCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream(jsonResponse.getBytes()));

        doAnswer(invocation -> {
            HttpClientResponseHandler<?> handler = invocation.getArgument(1);
            return handler.handleResponse(mockResponse);
        }).when(mockHttpClient).execute(any(ClassicHttpRequest.class), (HttpClientResponseHandler<?>) any());

        List<ApiRecord> records = service.fetchData();

        assertNotNull(records);
        assertEquals(1, records.size());
        WeatherRecord record = (WeatherRecord) records.get(0);
        assertEquals("", record.toMap().get("condition")); // Условие должно быть пустым
    }
    
    @Test
    public void testCityRotation() throws IOException, NoSuchFieldException, IllegalAccessException {
        // Тест проверяет, что сервис циклически меняет город для запросов
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("name", "London");
        rootNode.put("dt", 1234567890);
        rootNode.set("main", objectMapper.createObjectNode().put("temp", 20.0).put("humidity", 70));
        rootNode.set("wind", objectMapper.createObjectNode().put("speed", 4.0));
        rootNode.set("weather", objectMapper.createArrayNode()
            .add(objectMapper.createObjectNode().put("main", "Cloudy")));

        String jsonResponse = objectMapper.writeValueAsString(rootNode);

        when(mockResponse.getCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream(jsonResponse.getBytes()));

        doAnswer(invocation -> {
            HttpClientResponseHandler<?> handler = invocation.getArgument(1);
            return handler.handleResponse(mockResponse);
        }).when(mockHttpClient).execute(any(ClassicHttpRequest.class), (HttpClientResponseHandler<?>) any());

        // Получаем начальное значение индекса города
        Field cityIndexField = WeatherApiService.class.getDeclaredField("currentCityIndex");
        cityIndexField.setAccessible(true);
        int initialIndex = (int) cityIndexField.get(service);
        
        // Вызываем fetchData - должен использоваться текущий город и индекс должен измениться
        service.fetchData();
        
        int newIndex = (int) cityIndexField.get(service);
        assertEquals((initialIndex + 1) % 5, newIndex); // Должен циклически увеличиться
    }

    @Test
    public void testFetchData_MissingFields() throws IOException {
        // Создаем JSON с отсутствующими некоторыми полями или с нулевыми значениями
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("name", "IncompleteCity");
        rootNode.put("dt", 1234567890);
        
        // Создаем неполный main объект (отсутствует humidity)
        ObjectNode mainNode = objectMapper.createObjectNode();
        mainNode.put("temp", 25.5);
        // Намеренно пропускаем humidity
        rootNode.set("main", mainNode);
        
        // Полностью пропускаем wind объект
        // В реальном API такое может случиться из-за проблем с датчиками
        
        // Создаем weather с нестандартной структурой
        ArrayNode weatherArray = objectMapper.createArrayNode();
        ObjectNode weatherNode = objectMapper.createObjectNode();
        // Вместо "main" используем разные поля для проверки устойчивости
        weatherNode.put("description", "Sunny Day");
        // Пропускаем стандартное поле main
        weatherArray.add(weatherNode);
        rootNode.set("weather", weatherArray);

        String jsonResponse = objectMapper.writeValueAsString(rootNode);

        when(mockResponse.getCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream(jsonResponse.getBytes()));

        doAnswer(invocation -> {
            HttpClientResponseHandler<?> handler = invocation.getArgument(1);
            return handler.handleResponse(mockResponse);
        }).when(mockHttpClient).execute(any(ClassicHttpRequest.class), (HttpClientResponseHandler<?>) any());

        // Вызываем тестируемый метод
        List<ApiRecord> records = service.fetchData();

        // Проверки
        assertNotNull(records);
        assertEquals(1, records.size());
        
        WeatherRecord record = (WeatherRecord) records.get(0);
        assertEquals("IncompleteCity_1234567890", record.getId());
        assertEquals("IncompleteCity", record.toMap().get("city"));
        assertEquals(25.5, record.toMap().get("temperature"));
        
        // Проверяем, что даже с отсутствующими полями объект создан правильно
        assertEquals(0, record.toMap().get("humidity")); // По умолчанию int = 0
        assertEquals(0.0, record.toMap().get("windSpeed")); // По умолчанию double = 0.0
        assertEquals("", record.toMap().get("condition")); // По умолчанию String = ""
        
        // Проверяем, что запись была правильно добавлена в кэш
        assertTrue(service.isProcessed("IncompleteCity_1234567890"));
    }
}
