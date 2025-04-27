package com.example.apipoller.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Класс для хранения конфигурации приложения,
 * полученной из аргументов командной строки и .env файла.
 */
public class AppConfig {
    private static final Dotenv dotenv;
    
    // Статический блок для загрузки .env файла при инициализации класса
    static {
        try {
            dotenv = Dotenv.configure().ignoreIfMissing().load();
        } catch (Exception e) {
            System.err.println("Warning: Failed to load .env file: " + e.getMessage());
            throw e;
        }
    }
    
    // Геттеры для API ключей
    public static String getNewsApiKey() {
        String key = dotenv.get("NEWS_API_KEY");
        if (key == null || key.isEmpty()) {
            System.err.println("Warning: NEWS_API_KEY not found in .env file, using default value");
            return "YOUR_NEWS_API_KEY"; // Значение по умолчанию
        }
        return key;
    }

    public static String getWeatherApiKey() {
        String key = dotenv.get("WEATHER_API_KEY");
        if (key == null || key.isEmpty()) {
            System.err.println("Warning: WEATHER_API_KEY not found in .env file, using default value");
            return "YOUR_WEATHER_API_KEY"; // Значение по умолчанию
        }
        return key;
    }

    public static String getNasaApiKey() {
        String key = dotenv.get("NASA_API_KEY");
        if (key == null || key.isEmpty()) {
            System.err.println("Warning: NASA_API_KEY not found in .env file, using default value");
            return "DEMO_KEY"; // Демо-ключ NASA по умолчанию
        }
        return key;
    }
    
    // Существующие поля и методы
    private final int maxThreads;
    private final int timeoutSeconds;
    private final List<String> services;
    private final String outputFormat;
    private final Path outputFile;

    public AppConfig(int maxThreads, int timeoutSeconds, List<String> services, String outputFormat) {
        this.maxThreads = maxThreads;
        this.timeoutSeconds = timeoutSeconds;
        this.services = services;
        this.outputFormat = outputFormat.toLowerCase();
        this.outputFile = Paths.get("output." + this.outputFormat);
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public List<String> getServices() {
        return services;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public Path getOutputFile() {
        return outputFile;
    }

    /**
     * Создает объект конфигурации из аргументов командной строки.
     *
     * @param args аргументы командной строки
     * @return сконфигурированный объект AppConfig
     * @throws IllegalArgumentException если аргументы некорректны
     */
    public static AppConfig fromArgs(String[] args) {
        if (args.length < 4) {
            throw new IllegalArgumentException(
                "Usage: java ApiPollerApp <maxThreads> <timeoutSec> <format:json|csv> <service1> [service2 ...]"
            );
        }

        int maxThreads = Integer.parseInt(args[0]);
        if (maxThreads <= 0) {
            throw new IllegalArgumentException("maxThreads must be positive");
        }

        int timeoutSec = Integer.parseInt(args[1]);
        if (timeoutSec <= 0) {
            throw new IllegalArgumentException("timeoutSec must be positive");
        }

        String format = args[2].toLowerCase();
        if (!format.equals("json") && !format.equals("csv")) {
            throw new IllegalArgumentException("format must be 'json' or 'csv'");
        }

        List<String> services = Arrays.asList(args).subList(3, args.length);
        if (services.isEmpty()) {
            throw new IllegalArgumentException("At least one service must be specified");
        }

        return new AppConfig(maxThreads, timeoutSec, services, format);
    }

    @Override
    public String toString() {
        return "AppConfig{" +
               "maxThreads=" + maxThreads +
               ", timeoutSeconds=" + timeoutSeconds +
               ", services=" + services +
               ", outputFormat='" + outputFormat + '\'' +
               ", outputFile='" + outputFile + '\'' +
               '}';
    }
}
