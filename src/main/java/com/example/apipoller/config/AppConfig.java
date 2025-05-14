package com.example.apipoller.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Класс для хранения конфигурации приложения,
 * полученной из аргументов командной строки и .env файла.
 */
public class AppConfig {
    private static final Logger logger = Logger.getLogger(AppConfig.class.getName());
    private static final Dotenv dotenv;
    
    // Статический блок для загрузки .env файла при инициализации класса
    static {
        try {
            dotenv = Dotenv.configure().ignoreIfMissing().load();
            logger.info("Environment configuration loaded successfully");
        } catch (DotenvException e) {
            logger.log(Level.SEVERE, "Failed to parse .env file: " + e.getMessage(), e);
            throw new ConfigurationException("Error parsing .env file: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error loading .env file: " + e.getMessage(), e);
            throw new ConfigurationException("Unexpected error loading .env file: " + e.getMessage(), e);
        }
    }
    
    // Геттеры для API ключей
    public static String getNewsApiKey() {
        String key = dotenv.get("NEWS_API_KEY");
        if (key == null || key.isEmpty()) {
            logger.warning("NEWS_API_KEY not found in .env file, using default value");
            return "YOUR_NEWS_API_KEY"; // Значение по умолчанию
        }
        return key;
    }

    public static String getWeatherApiKey() {
        String key = dotenv.get("WEATHER_API_KEY");
        if (key == null || key.isEmpty()) {
            logger.warning("WEATHER_API_KEY not found in .env file, using default value");
            return "YOUR_WEATHER_API_KEY"; // Значение по умолчанию
        }
        return key;
    }

    public static String getNasaApiKey() {
        String key = dotenv.get("NASA_API_KEY");
        if (key == null || key.isEmpty()) {
            logger.warning("NASA_API_KEY not found in .env file, using default value");
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
     * @throws ConfigurationException если аргументы некорректны
     */
    public static AppConfig fromArgs(String[] args) throws ConfigurationException {
        try {
            if (args.length < 4) {
                throw new ConfigurationException(
                    "Not enough arguments. Usage: java ApiPollerApp <maxThreads> <timeoutSec> <format:json|csv> <service1> [service2 ...]"
                );
            }

            int maxThreads;
            try {
                maxThreads = Integer.parseInt(args[0]);
                if (maxThreads <= 0) {
                    throw new ConfigurationException("maxThreads must be positive");
                }
            } catch (NumberFormatException e) {
                throw new ConfigurationException("Invalid maxThreads value: " + args[0] + ". Must be a valid integer.", e);
            }

            int timeoutSec;
            try {
                timeoutSec = Integer.parseInt(args[1]);
                if (timeoutSec <= 0) {
                    throw new ConfigurationException("timeoutSec must be positive");
                }
            } catch (NumberFormatException e) {
                throw new ConfigurationException("Invalid timeoutSec value: " + args[1] + ". Must be a valid integer.", e);
            }

            String format = args[2].toLowerCase();
            if (!format.equals("json") && !format.equals("csv")) {
                throw new ConfigurationException("Invalid format: " + format + ". Format must be 'json' or 'csv'");
            }

            List<String> services = Arrays.asList(args).subList(3, args.length);
            if (services.isEmpty()) {
                throw new ConfigurationException("At least one service must be specified");
            }

            // Проверка, что все указанные сервисы поддерживаются
            for (String service : services) {
                if (!isServiceSupported(service)) {
                    throw new ConfigurationException("Unsupported service: " + service + 
                                                    ". Supported services are: news, weather, nasa");
                }
            }

            logger.info("Application configuration loaded successfully from command-line arguments");
            return new AppConfig(maxThreads, timeoutSec, services, format);
        } catch (ConfigurationException e) {
            throw e; // передаем дальше наше собственное исключение
        } catch (Exception e) {
            // Перехват всех остальных исключений и оборачивание их в ConfigurationException
            throw new ConfigurationException("Error parsing command-line arguments: " + e.getMessage(), e);
        }
    }
    
    /**
     * Проверяет, поддерживается ли указанный сервис
     * 
     * @param service имя сервиса
     * @return true, если сервис поддерживается
     */
    private static boolean isServiceSupported(String service) {
        return service.equals("news") || service.equals("weather") || service.equals("nasa");
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
    
    /**
     * Пользовательское исключение для ошибок конфигурации
     */
    public static class ConfigurationException extends RuntimeException {
        public ConfigurationException(String message) {
            super(message);
        }
        
        public ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Метод для загрузки конфигурации и валидации API ключей
     */
    public static void loadConfig() {
        // Проверяем наличие API ключей, чтобы обеспечить раннюю обратную связь
        getNewsApiKey();
        getWeatherApiKey();
        getNasaApiKey();
        logger.info("API keys validation complete");
    }
}
