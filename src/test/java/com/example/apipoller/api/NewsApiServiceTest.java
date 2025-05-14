package com.example.apipoller.api;

import com.example.apipoller.model.ApiRecord;
import com.example.apipoller.model.NewsRecord;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NewsApiServiceTest {

    @Mock
    private CloseableHttpClient mockHttpClient;
    
    @Mock
    private CloseableHttpResponse mockResponse;
    
    @Mock
    private HttpEntity mockEntity;
    
    private NewsApiService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        service = new NewsApiService() {
            @Override
            protected CloseableHttpClient createHttpClient() {
                return mockHttpClient;
            }
        };
    }

    @Test
    public void testGetServiceName() {
        assertEquals("news", service.getServiceName());
    }

    @Test
    public void testFetchData_Success() throws IOException {
        // Создаем тестовый JSON ответ
        ObjectNode article1 = objectMapper.createObjectNode();
        article1.put("title", "Test News Title 1");
        article1.put("description", "Test Description 1");
        article1.put("url", "https://example.com/news/1");
        article1.put("publishedAt", "2025-04-27T00:00:00Z");
        article1.put("author", "Author 1");
        ObjectNode source1 = objectMapper.createObjectNode();
        source1.put("name", "Source 1");
        article1.set("source", source1);

        ObjectNode article2 = objectMapper.createObjectNode();
        article2.put("title", "Test News Title 2");
        article2.put("description", "Test Description 2");
        article2.put("url", "https://example.com/news/2");
        article2.put("publishedAt", "2025-04-28T00:00:00Z");
        article2.put("author", "Author 2");
        ObjectNode source2 = objectMapper.createObjectNode();
        source2.put("name", "Source 2");
        article2.set("source", source2);

        ArrayNode articles = objectMapper.createArrayNode();
        articles.add(article1);
        articles.add(article2);

        ObjectNode root = objectMapper.createObjectNode();
        root.set("articles", articles);

        String jsonResponse = objectMapper.writeValueAsString(root);

        when(mockResponse.getCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream(jsonResponse.getBytes()));

        // Исправление: используем doAnswer вместо when/thenAnswer
        doAnswer(invocation -> {
            HttpClientResponseHandler<?> handler = invocation.getArgument(1);
            return handler.handleResponse(mockResponse);
        }).when(mockHttpClient).execute(any(ClassicHttpRequest.class), (HttpClientResponseHandler<?>) any());

        List<ApiRecord> records = service.fetchData();

        assertNotNull(records);
        assertEquals(2, records.size());

        NewsRecord record1 = (NewsRecord) records.get(0);
        assertEquals("Test News Title 1", record1.toMap().get("title"));
        assertEquals("https://example.com/news/1", record1.getId());
        assertEquals("Source 1", record1.toMap().get("source"));

        NewsRecord record2 = (NewsRecord) records.get(1);
        assertEquals("Test News Title 2", record2.toMap().get("title"));
        assertEquals("https://example.com/news/2", record2.getId());
    }

    @Test
    public void testFetchData_ErrorStatus() throws IOException {
        when(mockResponse.getCode()).thenReturn(404);
        when(mockResponse.getEntity()).thenReturn(mockEntity);

        // Исправление: используем doAnswer вместо when/thenAnswer
        doAnswer(invocation -> {
            HttpClientResponseHandler<?> handler = invocation.getArgument(1);
            return handler.handleResponse(mockResponse);
        }).when(mockHttpClient).execute(any(ClassicHttpRequest.class), (HttpClientResponseHandler<?>) any());

        assertThrows(IOException.class, () -> service.fetchData());
    }

    @Test
    public void testFetchData_EmptyArticles() throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.set("articles", objectMapper.createArrayNode());
        String jsonResponse = objectMapper.writeValueAsString(root);

        when(mockResponse.getCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream(jsonResponse.getBytes()));

        // Исправление: используем doAnswer вместо when/thenAnswer
        doAnswer(invocation -> {
            HttpClientResponseHandler<?> handler = invocation.getArgument(1);
            return handler.handleResponse(mockResponse);
        }).when(mockHttpClient).execute(any(ClassicHttpRequest.class), (HttpClientResponseHandler<?>) any());

        List<ApiRecord> records = service.fetchData();

        assertNotNull(records);
        assertTrue(records.isEmpty());
    }

    @Test
    public void testFetchData_DuplicateUrls() throws IOException {
        // Создаем тестовый JSON ответ с дублирующейся статьей
        ObjectNode article = objectMapper.createObjectNode();
        article.put("title", "Duplicate News");
        article.put("description", "Duplicate Description");
        article.put("url", "https://example.com/news/dup");
        article.put("publishedAt", "2025-04-29T00:00:00Z");
        article.put("author", "Author Dup");
        ObjectNode source = objectMapper.createObjectNode();
        source.put("name", "Source Dup");
        article.set("source", source);

        ArrayNode articles = objectMapper.createArrayNode();
        articles.add(article);

        ObjectNode root = objectMapper.createObjectNode();
        root.set("articles", articles);

        String jsonResponse = objectMapper.writeValueAsString(root);

        when(mockResponse.getCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream(jsonResponse.getBytes()));

        // Исправление: используем doAnswer вместо when/thenAnswer
        doAnswer(invocation -> {
            HttpClientResponseHandler<?> handler = invocation.getArgument(1);
            return handler.handleResponse(mockResponse);
        }).when(mockHttpClient).execute(any(ClassicHttpRequest.class), (HttpClientResponseHandler<?>) any());

        // Первый вызов должен вернуть запись
        List<ApiRecord> firstCall = service.fetchData();
        assertEquals(1, firstCall.size());

        // Второй вызов с тем же URL должен вернуть пустой список (из-за фильтрации дубликатов)
        List<ApiRecord> secondCall = service.fetchData();
        assertTrue(secondCall.isEmpty());
    }
}
