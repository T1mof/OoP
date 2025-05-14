package com.example.apipoller.api;

import com.example.apipoller.config.AppConfig;
import com.example.apipoller.model.ApiRecord;
import com.example.apipoller.model.NasaRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.StatusLine;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Сервис для получения данных с NASA API
 */
public class NasaApiService implements ApiService {
    private static final Logger logger = Logger.getLogger(NasaApiService.class.getName());
    
    // Получаем ключ API из .env через AppConfig
    private static final String API_KEY = AppConfig.getNasaApiKey();
    private static final String APOD_API_URL = "https://api.nasa.gov/planetary/apod";
    private static final String MARS_PHOTOS_API_URL = "https://api.nasa.gov/mars-photos/api/v1/rovers/curiosity/photos";
    private static final String[] API_TYPES = {"apod", "mars_photos"};
    
    private final Set<String> processedIds = Collections.synchronizedSet(new HashSet<>());
    private final CloseableHttpClient httpClient;
    private final ObjectMapper mapper;
    private int currentApiTypeIndex = 0;

    /**
     * Конструктор по умолчанию
     */
    public NasaApiService() {
        this(HttpClients.createDefault());
    }
    
    /**
     * Конструктор с инъекцией HTTP клиента для тестирования
     * 
     * @param httpClient HTTP клиент для выполнения запросов
     */
    public NasaApiService(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
        this.mapper = new ObjectMapper();
    }

    @Override
    public String getServiceName() {
        return "nasa";
    }

    @Override
    public List<ApiRecord> fetchData() throws IOException {
        String apiType = API_TYPES[currentApiTypeIndex];
        currentApiTypeIndex = (currentApiTypeIndex + 1) % API_TYPES.length;
        
        if (apiType.equals("apod")) {
            return fetchAPODData();
        } else {
            return fetchMarsRoverData();
        }
    }
    
    /**
     * Получает данные с NASA Astronomy Picture of the Day API
     * @return список записей с данными APOD
     * @throws IOException если произошла ошибка при запросе
     */
    private List<ApiRecord> fetchAPODData() throws IOException {
        // Выбираем случайную дату за последний год
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
                    int statusCode = response.getCode();
                    if (statusCode != 200) {
                        String statusMessage = new StatusLine(response).getReasonPhrase();
                        logger.warning("NASA API returned status code: " + statusCode + " - " + statusMessage);
                        throw new IOException("API returned status code: " + statusCode + " - " + statusMessage);
                    }
                    
                    JsonNode root = mapper.readTree(response.getEntity().getContent());
                    
                    String date = root.path("date").asText();
                    String id = "apod_" + date;
                    
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
                    EntityUtils.consume(response.getEntity());
                }
            });
        } catch (ConnectTimeoutException e) {
            logger.log(Level.SEVERE, "Connection timeout when accessing NASA APOD API", e);
            throw new IOException("Connection timeout when accessing NASA APOD API: " + e.getMessage(), e);
        } catch (SocketTimeoutException e) {
            logger.log(Level.SEVERE, "Socket timeout when reading from NASA APOD API", e);
            throw new IOException("Socket timeout when reading from NASA APOD API: " + e.getMessage(), e);
        } catch (HttpHostConnectException e) {
            logger.log(Level.SEVERE, "Unable to connect to NASA APOD API host", e);
            throw new IOException("Unable to connect to NASA APOD API host: " + e.getMessage(), e);
        } catch (ClientProtocolException e) {
            logger.log(Level.SEVERE, "HTTP protocol error when accessing NASA APOD API", e);
            throw new IOException("HTTP protocol error when accessing NASA APOD API: " + e.getMessage(), e);
        } catch (JsonProcessingException e) {
            logger.log(Level.SEVERE, "Error parsing JSON from NASA APOD API response", e);
            throw new IOException("Error parsing JSON from NASA APOD API response: " + e.getMessage(), e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "I/O error when accessing NASA APOD API", e);
            throw e;
        }
    }
    
    /**
     * Получает данные с NASA Mars Rover Photos API
     * @return список записей с данными Mars Rover
     * @throws IOException если произошла ошибка при запросе
     */
    private List<ApiRecord> fetchMarsRoverData() throws IOException {
        // Выбираем случайный sol (марсианский день) из доступных для Curiosity
        Random random = new Random();
        int sol = random.nextInt(3000) + 1;
        
        String apiUrl = String.format("%s?sol=%d&api_key=%s&page=1", MARS_PHOTOS_API_URL, sol, API_KEY);
        
        logger.info("Fetching data from NASA Mars Rover API for sol: " + sol);
        HttpGet request = new HttpGet(apiUrl);
        
        try {
            return httpClient.execute(request, response -> {
                try {
                    int statusCode = response.getCode();
                    if (statusCode != 200) {
                        String statusMessage = new StatusLine(response).getReasonPhrase();
                        logger.warning("NASA Mars Rover API returned status code: " + statusCode + " - " + statusMessage);
                        throw new IOException("API returned status code: " + statusCode + " - " + statusMessage);
                    }
                    
                    JsonNode root = mapper.readTree(response.getEntity().getContent());
                    JsonNode photos = root.path("photos");
                    
                    if (photos == null || photos.isEmpty() || !photos.isArray()) {
                        logger.info("No photos found for sol: " + sol);
                        return Collections.emptyList();
                    }
                    
                    // Выбираем случайную фотографию из полученных
                    int numPhotos = photos.size();
                    int photoIndex = random.nextInt(numPhotos);
                    JsonNode photoNode = photos.get(photoIndex);
                    
                    String id = "mars_" + photoNode.path("id").asText();
                    
                    // Пропускаем уже обработанные фотографии
                    if (processedIds.contains(id)) {
                        logger.info("Already processed Mars Rover photo with id: " + id);
                        return Collections.emptyList();
                    }
                    
                    processedIds.add(id);
                    
                    // Создание записи с данными фотографии
                    String earthDate = photoNode.path("earth_date").asText();
                    JsonNode cameraNode = photoNode.path("camera");
                    String cameraName = cameraNode.path("full_name").asText();
                    String imageUrl = photoNode.path("img_src").asText();
                    JsonNode roverNode = photoNode.path("rover");
                    String roverName = roverNode.path("name").asText();
                    
                    NasaRecord record = new NasaRecord(
                        id,
                        "Mars Rover Photo by " + cameraName,
                        earthDate,
                        "Photo taken by " + roverName + " rover on Mars using " + cameraName,
                        imageUrl,
                        "image",
                        "NASA/JPL"
                    );
                    
                    logger.info("Fetched new Mars Rover photo data for sol: " + sol);
                    return Collections.singletonList(record);
                } finally {
                    EntityUtils.consume(response.getEntity());
                }
            });
        } catch (ConnectTimeoutException e) {
            logger.log(Level.SEVERE, "Connection timeout when accessing NASA Mars Rover API", e);
            throw new IOException("Connection timeout when accessing NASA Mars Rover API: " + e.getMessage(), e);
        } catch (SocketTimeoutException e) {
            logger.log(Level.SEVERE, "Socket timeout when reading from NASA Mars Rover API", e);
            throw new IOException("Socket timeout when reading from NASA Mars Rover API: " + e.getMessage(), e);
        } catch (HttpHostConnectException e) {
            logger.log(Level.SEVERE, "Unable to connect to NASA Mars Rover API host", e);
            throw new IOException("Unable to connect to NASA Mars Rover API host: " + e.getMessage(), e);
        } catch (ClientProtocolException e) {
            logger.log(Level.SEVERE, "HTTP protocol error when accessing NASA Mars Rover API", e);
            throw new IOException("HTTP protocol error when accessing NASA Mars Rover API: " + e.getMessage(), e);
        } catch (JsonProcessingException e) {
            logger.log(Level.SEVERE, "Error parsing JSON from NASA Mars Rover API response", e);
            throw new IOException("Error parsing JSON from NASA Mars Rover API response: " + e.getMessage(), e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "I/O error when accessing NASA Mars Rover API", e);
            throw e;
        }
    }
}
