package com.example.apipoller.api;

import com.example.apipoller.config.AppConfig;
import com.example.apipoller.model.ApiRecord;
import com.example.apipoller.model.NasaRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Сервис для получения данных из NASA API
 */
public class NasaApiService implements ApiService {
    private static final Logger logger = Logger.getLogger(NasaApiService.class.getName());
    
    // Получаем ключ API из .env через AppConfig
    private static final String API_KEY = AppConfig.getNasaApiKey();
    
    // Эндпоинты NASA API
    private static final String APOD_API_URL = "https://api.nasa.gov/planetary/apod";
    private static final String MARS_PHOTOS_API_URL = "https://api.nasa.gov/mars-photos/api/v1/rovers/curiosity/photos";
    
    private static final String[] API_TYPES = {"apod", "mars_photos"};
    
    private final Set<String> processedIds = Collections.synchronizedSet(new HashSet<>());
    private final CloseableHttpClient httpClient;
    private final ObjectMapper mapper;
    private int currentApiTypeIndex = 0;

    public NasaApiService() {
        this.httpClient = HttpClients.createDefault();
        this.mapper = new ObjectMapper();
    }

    @Override
    public String getServiceName() {
        return "nasa";
    }

    @Override
    public List<ApiRecord> fetchData() throws IOException {
        // Циклически меняем тип API для разнообразия данных
        String apiType = API_TYPES[currentApiTypeIndex];
        currentApiTypeIndex = (currentApiTypeIndex + 1) % API_TYPES.length;
        
        if (apiType.equals("apod")) {
            return fetchAPODData();
        } else {
            return fetchMarsRoverData();
        }
    }
    
    /**
     * Получение данных из NASA Astronomy Picture of the Day API
     */
    private List<ApiRecord> fetchAPODData() throws IOException {
        // Для разнообразия возьмем случайную дату за последние 365 дней
        Random random = new Random();
        int daysToSubtract = random.nextInt(365) + 1;
        LocalDate randomDate = LocalDate.now().minusDays(daysToSubtract);
        String dateStr = randomDate.format(DateTimeFormatter.ISO_DATE);
        
        String apiUrl = String.format("%s?api_key=%s&date=%s", APOD_API_URL, API_KEY, dateStr);
        
        logger.info("Fetching data from NASA APOD API for date: " + dateStr);
        HttpGet request = new HttpGet(apiUrl);
        
        try {
            return httpClient.execute(request, response -> {
                try {
                    if (response.getCode() != 200) {
                        logger.warning("NASA API returned status code: " + response.getCode());
                        throw new IOException("API returned status code: " + response.getCode());
                    }
                    
                    JsonNode root = mapper.readTree(response.getEntity().getContent());
                    
                    String date = root.path("date").asText();
                    String id = "apod_" + date;
                    
                    // Пропускаем уже обработанные записи
                    if (processedIds.contains(id)) {
                        logger.info("Already processed NASA APOD data for date: " + date);
                        return Collections.emptyList();
                    }
                    
                    processedIds.add(id);
                    
                    NasaRecord record = new NasaRecord(
                        id,
                        root.path("title").asText(),
                        date,
                        root.path("explanation").asText(),
                        root.path("url").asText(),
                        root.path("media_type").asText(),
                        root.path("copyright").asText("")
                    );
                    
                    logger.info("Fetched new NASA APOD data for date: " + date);
                    return Collections.singletonList(record);
                } finally {
                    // Освобождаем ресурсы
                    EntityUtils.consume(response.getEntity());
                }
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error fetching data from NASA APOD API", e);
            throw new IOException("Error fetching data from NASA APOD API: " + e.getMessage(), e);
        }
    }
    
    /**
     * Получение данных из NASA Mars Rover Photos API
     */
    private List<ApiRecord> fetchMarsRoverData() throws IOException {
        // Для марсохода Curiosity используем случайный sol (марсианский день)
        Random random = new Random();
        int sol = random.nextInt(3000) + 1; // Curiosity работает уже больше 3000 солов
        
        String apiUrl = String.format("%s?sol=%d&api_key=%s&page=1", MARS_PHOTOS_API_URL, sol, API_KEY);
        
        logger.info("Fetching data from NASA Mars Rover API for sol: " + sol);
        HttpGet request = new HttpGet(apiUrl);
        
        try {
            return httpClient.execute(request, response -> {
                try {
                    if (response.getCode() != 200) {
                        logger.warning("NASA API returned status code: " + response.getCode());
                        throw new IOException("API returned status code: " + response.getCode());
                    }
                    
                    JsonNode root = mapper.readTree(response.getEntity().getContent());
                    JsonNode photos = root.path("photos");
                    
                    if (!photos.isArray() || photos.size() == 0) {
                        logger.info("No photos found in NASA Mars Rover API response");
                        return Collections.emptyList();
                    }
                    
                    List<ApiRecord> records = new ArrayList<>();
                    int count = Math.min(photos.size(), 5); // Ограничим до 5 фото
                    
                    for (int i = 0; i < count; i++) {
                        JsonNode photo = photos.get(i);
                        String id = "mars_" + photo.path("id").asText();
                        
                        // Пропускаем уже обработанные фото
                        if (processedIds.contains(id)) {
                            continue;
                        }
                        
                        processedIds.add(id);
                        
                        NasaRecord record = new NasaRecord(
                            id,
                            "Mars Rover Photo: " + photo.path("rover").path("name").asText(),
                            photo.path("earth_date").asText(),
                            "Photo taken by " + photo.path("camera").path("full_name").asText() + 
                            " on Mars rover " + photo.path("rover").path("name").asText(),
                            photo.path("img_src").asText(),
                            "image",
                            "NASA/JPL-Caltech"
                        );
                        
                        records.add(record);
                    }
                    
                    logger.info("Fetched " + records.size() + " new Mars Rover photos");
                    return records;
                } finally {
                    // Освобождаем ресурсы
                    EntityUtils.consume(response.getEntity());
                }
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error fetching data from NASA Mars Rover API", e);
            throw new IOException("Error fetching data from NASA Mars Rover API: " + e.getMessage(), e);
        }
    }
}
