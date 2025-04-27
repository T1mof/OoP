package com.example.apipoller.api;

import com.example.apipoller.config.AppConfig;
import com.example.apipoller.model.ApiRecord;
import com.example.apipoller.model.NewsRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
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

    public NewsApiService() {
        this.httpClient = HttpClients.createDefault();
        this.mapper = new ObjectMapper();
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
                    if (response.getCode() != 200) {
                        logger.warning("News API returned status code: " + response.getCode());
                        throw new IOException("API returned status code: " + response.getCode());
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
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error fetching data from News API", e);
            throw new IOException("Error fetching data from News API: " + e.getMessage(), e);
        }
    }
}
