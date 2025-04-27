package com.example.apipoller;

import com.example.apipoller.config.AppConfig;
import com.example.apipoller.service.PollScheduler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Главный класс приложения, отвечающий за запуск планировщика задач
 * по опросу REST API и сохранению результатов в файл.
 */
public class ApiPollerApp {
    private static final Logger logger = Logger.getLogger(ApiPollerApp.class.getName());

    public static void main(String[] args) {
        try {
            // Парсинг аргументов командной строки
            AppConfig config = AppConfig.fromArgs(args);
            logger.info("Starting API Poller with config: " + config);

            // Создание и запуск планировщика задач
            PollScheduler scheduler = new PollScheduler(config);
            scheduler.start();

            // Регистрация hook для корректного завершения
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down...");
                scheduler.shutdown();
            }));

            // Блокировка основного потока, чтобы приложение не завершалось
            Thread.currentThread().join();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error starting application: " + e.getMessage(), e);
            System.exit(1);
        }
    }
}
