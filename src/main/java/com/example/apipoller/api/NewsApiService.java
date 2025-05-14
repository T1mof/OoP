package com.example.apipoller.api;

import com.example.apipoller.config.AppConfig;
import com.example.apipoller.model.ApiRecord;
import com.example.apipoller.model.NewsRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.StatusLine;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Сервис для получения данных с News API
 */
public class NewsApiService implements ApiService {
    private static final Logger logger = Logger.getLogger(NewsApiService.class.getName());
    
    // Получаем ключ API из .env через AppConfig
    private static final String API_KEY = AppConfig.getNewsApiKey();
    private static final String API_URL = 
        "https://newsapi.org/v2/top-headlines?country=us&apiKey=" + API_KEY;
    
    private final Set<String> processedIds = Collections.synchronizedSet(new HashSet<>());
    private final CloseableHttpClient httpClient;
    private final ObjectMapper mapper;

    /**
     * Стандартный конструктор
     */
    public NewsApiService() {
        this.httpClient = createHttpClient();
        this.mapper = new ObjectMapper();
    }
    
    /**
     * Конструктор с внедрением HTTP-клиента (для тестирования)
     * 
     * @param httpClient HTTP-клиент для выполнения запросов
     */
    protected NewsApiService(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
        this.mapper = new ObjectMapper();
    }
    
    /**
     * Создает экземпляр HTTP-клиента
     * Метод может быть переопределен в тестах для внедрения мока
     * 
     * @return HTTP-клиент для выполнения запросов
     */
    protected CloseableHttpClient createHttpClient() {
        return HttpClients.createDefault();
    }

    @Override
    public String getServiceName() {
        return "news";
    }

    @Override
    public List<ApiRecord> fetchData() throws IOException {
        logger.info("Fetching data from News API");
        HttpGet request = new HttpGet(API_URL);
        
        try {
            // Используем execute с HttpClientResponseHandler
            return httpClient.execute(request, response -> {
                try {
                    int statusCode = response.getCode();
                    if (statusCode != 200) {
                        String statusMessage = new StatusLine(response).getReasonPhrase();
                        logger.warning("News API returned status code: " + statusCode + " - " + statusMessage);
                        throw new IOException("API returned status code: " + statusCode + " - " + statusMessage);
                    }
                    
                    JsonNode root = mapper.readTree(response.getEntity().getContent());
                    JsonNode articles = root.get("articles");
                    
                    if (articles == null || !articles.isArray()) {
                        logger.info("No articles found in News API response");
                        return Collections.emptyList();
                    }
                    
                    List<ApiRecord> records = new ArrayList<>();
                    
                    for (JsonNode article : articles) {
                        String url = article.path("url").asText();
                        
                        // Пропускаем уже обработанные новости
                        if (processedIds.contains(url)) {
                            continue;
                        }
                        
                        processedIds.add(url);
                        
                        JsonNode sourceNode = article.path("source");
                        String source = sourceNode.has("name") ? sourceNode.get("name").asText() : "Unknown";
                        
                        NewsRecord record = new NewsRecord(
                            article.path("title").asText(),
                            article.path("description").asText(),
                            url,
                            source,
                            article.path("publishedAt").asText(),
                            article.path("author").asText()
                        );
                        
                        records.add(record);
                    }
                    
                    logger.info("Fetched " + records.size() + " new articles from News API");
                    return records;
                } finally {
                    // Освобождаем ресурсы
                    EntityUtils.consume(response.getEntity());
                }
            });
        } catch (ConnectTimeoutException e) {
            logger.log(Level.SEVERE, "Connection timeout when accessing News API", e);
            throw new IOException("Connection timeout when accessing News API: " + e.getMessage(), e);
        } catch (SocketTimeoutException e) {
            logger.log(Level.SEVERE, "Socket timeout when reading from News API", e);
            throw new IOException("Socket timeout when reading from News API: " + e.getMessage(), e);
        } catch (HttpHostConnectException e) {
            logger.log(Level.SEVERE, "Unable to connect to News API host", e);
            throw new IOException("Unable to connect to News API host: " + e.getMessage(), e);
        } catch (ClientProtocolException e) {
            logger.log(Level.SEVERE, "HTTP protocol error when accessing News API", e);
            throw new IOException("HTTP protocol error when accessing News API: " + e.getMessage(), e);
        } catch (JsonProcessingException e) {
            logger.log(Level.SEVERE, "Error parsing JSON from News API response", e);
            throw new IOException("Error parsing JSON from News API response: " + e.getMessage(), e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "I/O error when accessing News API", e);
            throw e;
        }
    }
}
