package com.example.apipoller.start;

import com.example.apipoller.config.AppConfig;
import com.example.apipoller.config.AppConfig.ConfigurationException;
import com.example.apipoller.logging.AppLogSetup;
import com.example.apipoller.service.ParserShutdownMonitor;
import com.example.apipoller.service.PollScheduler;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Главный класс приложения, отвечающий за запуск планировщика задач
 * по опросу REST API и сохранению результатов в файл.
 */
public class ApiPollerApp {
    private static final Logger logger = Logger.getLogger(ApiPollerApp.class.getName());

    public static void main(String[] args) throws IOException {
        ParserShutdownMonitor shutdownMonitor = null;
        
        try {
            AppLogSetup.setupLogger();
            // Загрузка конфигурации
            AppConfig.loadConfig();
            AppConfig config = AppConfig.fromArgs(args);
            logger.info("Starting API Poller v1.0.0 with config: " + config);

            // Создание и запуск планировщика задач
            PollScheduler scheduler = new PollScheduler(config);
            scheduler.start();
            
            // Создание и запуск демон-потока для завершения парсера
            shutdownMonitor = new ParserShutdownMonitor(scheduler);
            Thread monitorThread = new Thread(shutdownMonitor, "parser-shutdown-monitor");
            monitorThread.setDaemon(true); // Устанавливаем поток как демон
            monitorThread.start();
            logger.info("Parser shutdown monitor daemon started");
            
            // Запуск слушателя команд для обработки пользовательского ввода
            shutdownMonitor.startCommandListener();

            // Регистрация стандартного shutdown hook
            final ParserShutdownMonitor finalMonitor = shutdownMonitor;
            try {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    logger.info("JVM shutdown initiated, notifying parser shutdown monitor...");
                    try {
                        // Запрашиваем graceful shutdown через монитор
                        finalMonitor.requestShutdown();
                        
                        // Ждем завершения работы парсера с таймаутом
                        if (!finalMonitor.awaitTermination(10000)) {
                            logger.warning("Parser could not be shut down gracefully within timeout");
                        }
                    } catch (InterruptedException e) {
                        logger.warning("Shutdown process interrupted");
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Error during shutdown process", e);
                    }
                }));
                logger.info("Shutdown hook registered");
            } catch (SecurityException e) {
                logger.log(Level.WARNING, "Could not register shutdown hook due to security restrictions", e);
            } catch (IllegalStateException e) {
                logger.log(Level.WARNING, "Could not register shutdown hook - VM is already shutting down", e);
            }

            // Блокировка основного потока
            logger.info("API Poller is running. Type 'help' for available commands.");
            Thread.currentThread().join();
            
        } catch (ConfigurationException e) {
            logger.log(Level.SEVERE, "Configuration error: " + e.getMessage(), e);
            System.exit(1);
        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, "Invalid argument: " + e.getMessage(), e);
            System.exit(1);
        } catch (InterruptedException e) {
            logger.info("Application interrupted");
            // Запрашиваем завершение парсера если был создан монитор
            if (shutdownMonitor != null) {
                shutdownMonitor.requestShutdown();
                try {
                    // Даем немного времени на корректное завершение
                    if (!shutdownMonitor.awaitTermination(3000)) {
                        logger.warning("Parser shutdown timed out during application interrupt");
                    }
                } catch (InterruptedException ie) {
                    logger.warning("Shutdown wait interrupted");
                    Thread.currentThread().interrupt();
                }
            }
            Thread.currentThread().interrupt();
            System.exit(0);
        } catch (SecurityException e) {
            logger.log(Level.SEVERE, "Security error: " + e.getMessage(), e);
            System.exit(1);
        } catch (OutOfMemoryError e) {
            logger.log(Level.SEVERE, "Out of memory: " + e.getMessage(), e);
            System.exit(2);
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Runtime error: " + e.getMessage(), e);
            System.exit(1);
        } catch (Error e) {
            logger.log(Level.SEVERE, "Critical error: " + e.getMessage(), e);
            System.exit(3);
        }
    }
}
