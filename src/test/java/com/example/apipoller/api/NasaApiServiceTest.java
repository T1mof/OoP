package com.example.apipoller.api;

import com.example.apipoller.model.ApiRecord;
import com.example.apipoller.model.NasaRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.HttpHostConnectException;
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
import java.net.SocketTimeoutException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NasaApiServiceTest {

    @Mock
    private CloseableHttpClient mockHttpClient;
    
    @Mock
    private CloseableHttpResponse mockResponse;
    
    @Mock
    private HttpEntity mockEntity;
    
    private NasaApiService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() throws Exception {
        // Создаем реальный ObjectMapper для тестов
        objectMapper = new ObjectMapper();
        
        // Создаем сервис с моком HTTP клиента
        service = new NasaApiService(mockHttpClient);
    }

    @Test
    public void testGetServiceName() {
        assertEquals("nasa", service.getServiceName());
    }
    
    @Test
    public void testFetchAPODData() throws Exception {
        // Устанавливаем apiTypeIndex для APOD
        setApiTypeIndex(service, 0);
        
        // Создаем тестовый JSON ответ
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("date", "2025-04-27");
        rootNode.put("title", "Test Astronomy Picture");
        rootNode.put("explanation", "This is a test description");
        rootNode.put("url", "https://example.com/image.jpg");
        rootNode.put("media_type", "image");
        rootNode.put("copyright", "NASA");
        
        // Настраиваем мок ответы
        when(mockResponse.getCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(
            new ByteArrayInputStream(objectMapper.writeValueAsBytes(rootNode))
        );
        
        // Устранение неоднозначности с помощью явного приведения типа
        doAnswer(invocation -> {
            HttpClientResponseHandler<?> handler = invocation.getArgument(1);
            return handler.handleResponse(mockResponse);
        }).when(mockHttpClient).execute(any(ClassicHttpRequest.class), (HttpClientResponseHandler<?>) any());
        
        // Вызываем метод
        List<ApiRecord> records = service.fetchData();
        
        // Проверки
        assertNotNull(records);
        assertEquals(1, records.size());
        
        NasaRecord record = (NasaRecord) records.get(0);
        assertEquals("apod_2025-04-27", record.getId());
        assertEquals("Test Astronomy Picture", record.toMap().get("title"));
        assertEquals("https://example.com/image.jpg", record.toMap().get("url"));
        assertEquals("nasa", record.toMap().get("type"));
    }
    
    @Test
    public void testFetchMarsRoverData() throws Exception {
        // Устанавливаем apiTypeIndex для Mars Rover
        setApiTypeIndex(service, 1);
        
        // Создаем тестовый JSON ответ с данными Mars Rover
        ObjectNode photoNode = objectMapper.createObjectNode();
        photoNode.put("id", "12345");
        photoNode.put("earth_date", "2025-01-15");
        photoNode.put("img_src", "https://mars.nasa.gov/image.jpg");
        
        ObjectNode cameraNode = objectMapper.createObjectNode();
        cameraNode.put("full_name", "Mars Hand Lens Imager");
        photoNode.set("camera", cameraNode);
        
        ObjectNode roverNode = objectMapper.createObjectNode();
        roverNode.put("name", "Curiosity");
        photoNode.set("rover", roverNode);
        
        ArrayNode photosArray = objectMapper.createArrayNode();
        photosArray.add(photoNode);
        
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.set("photos", photosArray);
        
        // Настраиваем мок ответы
        when(mockResponse.getCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(
            new ByteArrayInputStream(objectMapper.writeValueAsBytes(rootNode))
        );
        
        // Устранение неоднозначности с помощью doAnswer вместо when..thenAnswer
        doAnswer(invocation -> {
            HttpClientResponseHandler<?> handler = invocation.getArgument(1);
            return handler.handleResponse(mockResponse);
        }).when(mockHttpClient).execute(any(ClassicHttpRequest.class), (HttpClientResponseHandler<?>) any());
        
        // Вызываем метод
        List<ApiRecord> records = service.fetchData();
        
        // Проверки
        assertNotNull(records);
        assertEquals(1, records.size());
        
        NasaRecord record = (NasaRecord) records.get(0);
        assertEquals("mars_12345", record.getId());
        assertTrue(record.toMap().get("title").toString().contains("Mars Hand Lens Imager"));
        assertEquals("https://mars.nasa.gov/image.jpg", record.toMap().get("url"));
    }
    
    @Test
    public void testErrorResponse() throws Exception {
        // Настраиваем мок ответа с ошибкой
        when(mockResponse.getCode()).thenReturn(404);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        
        // Устранение неоднозначности с помощью doAnswer вместо when..thenAnswer
        doAnswer(invocation -> {
            HttpClientResponseHandler<?> handler = invocation.getArgument(1);
            return handler.handleResponse(mockResponse);
        }).when(mockHttpClient).execute(any(ClassicHttpRequest.class), (HttpClientResponseHandler<?>) any());
        
        // Проверяем, что метод выбрасывает исключение
        assertThrows(IOException.class, () -> service.fetchData());
    }
    
    @Test
    public void testAlreadyProcessedIds() throws Exception {
        // Устанавливаем apiTypeIndex для APOD
        setApiTypeIndex(service, 0);
        
        // Создаем тестовый JSON ответ
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("date", "2025-04-27");
        rootNode.put("title", "Duplicate Entry");
        rootNode.put("explanation", "This should be filtered");
        rootNode.put("url", "https://example.com/duplicate.jpg");
        rootNode.put("media_type", "image");
        
        // Настраиваем мок ответы
        when(mockResponse.getCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(
            new ByteArrayInputStream(objectMapper.writeValueAsBytes(rootNode))
        );
        
        // Устранение неоднозначности с помощью doAnswer
        doAnswer(invocation -> {
            HttpClientResponseHandler<?> handler = invocation.getArgument(1);
            return handler.handleResponse(mockResponse);
        }).when(mockHttpClient).execute(any(ClassicHttpRequest.class), (HttpClientResponseHandler<?>) any());
        
        // Первый вызов должен вернуть запись
        List<ApiRecord> firstCall = service.fetchData();
        assertEquals(1, firstCall.size());
        
        // Второй вызов с тем же ID должен вернуть пустой список
        List<ApiRecord> secondCall = service.fetchData();
        assertTrue(secondCall.isEmpty());
    }

    @Test
    public void testFetchMarsRoverDataEmptyPhotos() throws Exception {
        // Устанавливаем apiTypeIndex для Mars Rover
        setApiTypeIndex(service, 1);
        
        // Создаем JSON с пустым списком фотографий
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.set("photos", objectMapper.createArrayNode());
        
        when(mockResponse.getCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(
            new ByteArrayInputStream(objectMapper.writeValueAsBytes(rootNode))
        );
        
        doAnswer(invocation -> {
            HttpClientResponseHandler<?> handler = invocation.getArgument(1);
            return handler.handleResponse(mockResponse);
        }).when(mockHttpClient).execute(any(ClassicHttpRequest.class), (HttpClientResponseHandler<?>) any());
        
        // Вызываем метод
        List<ApiRecord> records = service.fetchData();
        
        // Проверяем, что метод вернул пустой список
        assertNotNull(records);
        assertTrue(records.isEmpty());
    }

    @Test
    public void testConnectTimeoutException() throws Exception {
        // Устанавливаем apiTypeIndex для APOD
        setApiTypeIndex(service, 0);
        
        // Мокируем ConnectTimeoutException
        doThrow(new ConnectTimeoutException("Connection timed out"))
            .when(mockHttpClient)
            .execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class));
        
        // Проверяем тип исключения
        IOException thrownException = assertThrows(IOException.class, () -> service.fetchData());
        assertTrue(thrownException.getMessage().contains("Connection timeout"));
    }

    @Test
    public void testSocketTimeoutException() throws Exception {
        // Устанавливаем apiTypeIndex для Mars Rover
        setApiTypeIndex(service, 1);
        
        // Мокируем SocketTimeoutException
        doThrow(new SocketTimeoutException("Read timed out"))
            .when(mockHttpClient)
            .execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class));
        
        // Проверяем тип исключения
        IOException thrownException = assertThrows(IOException.class, () -> service.fetchData());
        assertTrue(thrownException.getMessage().contains("Socket timeout"));
    }

    @Test
    public void testHttpHostConnectException() throws Exception {
        // Устанавливаем apiTypeIndex для APOD
        setApiTypeIndex(service, 0);
        
        // Мокируем HttpHostConnectException
        doThrow(new HttpHostConnectException("Failed to connect to host"))
            .when(mockHttpClient)
            .execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class));
        
        // Проверяем тип исключения
        IOException thrownException = assertThrows(IOException.class, () -> service.fetchData());
        assertTrue(thrownException.getMessage().contains("Unable to connect to NASA"));
    }

    @Test
    public void testClientProtocolException() throws Exception {
        // Устанавливаем apiTypeIndex для APOD
        setApiTypeIndex(service, 0);
        
        // Мокируем ClientProtocolException
        doThrow(new ClientProtocolException("Protocol error"))
            .when(mockHttpClient)
            .execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class));
        
        // Проверяем тип исключения
        IOException thrownException = assertThrows(IOException.class, () -> service.fetchData());
        assertTrue(thrownException.getMessage().contains("HTTP protocol error"));
    }

    @Test
    public void testJsonProcessingException() throws Exception {
        // Устанавливаем apiTypeIndex для Mars Rover
        setApiTypeIndex(service, 1);
        
        // Настраиваем ответ с некорректным JSON
        when(mockResponse.getCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(
            new ByteArrayInputStream("{invalid json}".getBytes())
        );
        
        doAnswer(invocation -> {
            HttpClientResponseHandler<?> handler = invocation.getArgument(1);
            return handler.handleResponse(mockResponse);
        }).when(mockHttpClient).execute(any(ClassicHttpRequest.class), (HttpClientResponseHandler<?>) any());
        
        // Проверяем тип исключения
        IOException thrownException = assertThrows(IOException.class, () -> service.fetchData());
        assertTrue(thrownException.getMessage().contains("Error parsing JSON"));
    }
    
    // Вспомогательный метод для установки индекса API через рефлексию
    private void setApiTypeIndex(NasaApiService service, int index) throws Exception {
        Field field = NasaApiService.class.getDeclaredField("currentApiTypeIndex");
        field.setAccessible(true);
        field.set(service, index);
    }
}
