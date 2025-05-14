package com.example.apipoller.api;

import com.example.apipoller.config.AppConfig;
import com.example.apipoller.model.ApiRecord;
import com.example.apipoller.model.WeatherRecord;
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
 * Сервис для получения данных с OpenWeatherMap API
 */
public class WeatherApiService implements ApiService {
    private static final Logger logger = Logger.getLogger(WeatherApiService.class.getName());
    
    // Получаем ключ API из .env через AppConfig
    private static final String API_KEY = AppConfig.getWeatherApiKey();
    
    // Координаты городов для циклического опроса
    private static final Map<String, double[]> CITIES = Map.of(
        "London", new double[]{51.5074, -0.1278},
        "New York", new double[]{40.7128, -74.0060},
        "Moscow", new double[]{55.7558, 37.6173},
        "Tokyo", new double[]{35.6762, 139.6503},
        "Berlin", new double[]{52.5200, 13.4050}
    );
    
    private final Set<String> processedIds = Collections.synchronizedSet(new HashSet<>());
    private final CloseableHttpClient httpClient;
    private final ObjectMapper mapper;
    private int currentCityIndex = 0;
    private final List<String> cityNames = new ArrayList<>(CITIES.keySet());

    /**
     * Конструктор по умолчанию
     */
    public WeatherApiService() {
        this.httpClient = createHttpClient();
        this.mapper = new ObjectMapper();
    }
    
    /**
     * Конструктор с инъекцией HTTP-клиента для тестирования
     * 
     * @param httpClient HTTP-клиент для выполнения запросов
     */
    protected WeatherApiService(CloseableHttpClient httpClient) {
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

    /**
     * Проверяет, обрабатывалась ли уже запись с указанным id.
     * Метод используется только в тестах.
     * 
     * @param id Идентификатор записи для проверки
     * @return true, если запись уже обрабатывалась
     */
    protected boolean isProcessed(String id) {
        return processedIds.contains(id);
    }


    @Override
    public String getServiceName() {
        return "weather";
    }

    @Override
    public List<ApiRecord> fetchData() throws IOException {
        // Циклически меняем город для разнообразия данных
        String cityName = cityNames.get(currentCityIndex);
        currentCityIndex = (currentCityIndex + 1) % cityNames.size();
        
        double[] coords = CITIES.get(cityName);
        
        // Правильная структура запроса по координатам
        String apiUrl = String.format(
            "https://api.openweathermap.org/data/2.5/weather?lat=%.6f&lon=%.6f&units=metric&appid=%s",
            coords[0], coords[1], API_KEY
        );
        
        logger.info("Fetching weather data for " + cityName + " using coordinates");
        HttpGet request = new HttpGet(apiUrl);
        
        try {
            // Используем execute с HttpClientResponseHandler
            return httpClient.execute(request, response -> {
                try {
                    int statusCode = response.getCode();
                    if (statusCode != 200) {
                        String statusMessage = new StatusLine(response).getReasonPhrase();
                        logger.warning("Weather API returned status code: " + statusCode + " - " + statusMessage);
                        throw new IOException("API returned status code: " + statusCode + " - " + statusMessage);
                    }
                    
                    JsonNode root = mapper.readTree(response.getEntity().getContent());
                    
                    // Получение данных из JSON ответа
                    String city = root.path("name").asText();
                    long timestamp = root.path("dt").asLong();
                    String id = city + "_" + timestamp;
                    
                    // Пропускаем уже обработанные данные о погоде
                    if (processedIds.contains(id)) {
                        logger.info("Already processed weather data for " + city);
                        return Collections.emptyList();
                    }
                    
                    processedIds.add(id);
                    
                    // Извлечение нужных данных из JSON
                    JsonNode main = root.path("main");
                    JsonNode wind = root.path("wind");
                    JsonNode weather = root.path("weather").isEmpty() ? 
                        mapper.createObjectNode() : root.path("weather").get(0);
                    
                    double temperature = main.path("temp").asDouble();
                    int humidity = main.path("humidity").asInt();
                    double windSpeed = wind.path("speed").asDouble();
                    String condition = weather.path("main").asText("");
                    
                    // Создание записи о погоде
                    WeatherRecord record = new WeatherRecord(
                        city,
                        temperature,
                        windSpeed,
                        humidity,
                        condition,
                        timestamp
                    );
                    
                    logger.info("Fetched new weather data for " + city);
                    return Collections.singletonList(record);
                } finally {
                    // Освобождаем ресурсы
                    EntityUtils.consume(response.getEntity());
                }
            });
        } catch (ConnectTimeoutException e) {
            logger.log(Level.SEVERE, "Connection timeout when accessing Weather API for " + cityName, e);
            throw new IOException("Connection timeout when accessing Weather API for " + cityName + ": " + e.getMessage(), e);
        } catch (SocketTimeoutException e) {
            logger.log(Level.SEVERE, "Socket timeout when reading from Weather API for " + cityName, e);
            throw new IOException("Socket timeout when reading from Weather API for " + cityName + ": " + e.getMessage(), e);
        } catch (HttpHostConnectException e) {
            logger.log(Level.SEVERE, "Unable to connect to Weather API host for " + cityName, e);
            throw new IOException("Unable to connect to Weather API host for " + cityName + ": " + e.getMessage(), e);
        } catch (ClientProtocolException e) {
            logger.log(Level.SEVERE, "HTTP protocol error when accessing Weather API for " + cityName, e);
            throw new IOException("HTTP protocol error when accessing Weather API for " + cityName + ": " + e.getMessage(), e);
        } catch (JsonProcessingException e) {
            logger.log(Level.SEVERE, "Error parsing JSON from Weather API response for " + cityName, e);
            throw new IOException("Error parsing JSON from Weather API response for " + cityName + ": " + e.getMessage(), e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "I/O error when accessing Weather API for " + cityName, e);
            throw e;
        }
    }
}
