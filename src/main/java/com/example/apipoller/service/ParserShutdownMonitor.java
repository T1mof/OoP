package com.example.apipoller.service;

import java.lang.management.ManagementFactory;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Демон-поток для мониторинга и корректного завершения работы парсера.
 * Реагирует на команды пользователя и сигналы завершения,
 * обеспечивает плавную остановку.
 */
public class ParserShutdownMonitor implements Runnable {
    private static final Logger logger = Logger.getLogger(ParserShutdownMonitor.class.getName());
    private static final long CHECK_INTERVAL_MS = 500; // Интервал проверки состояния
    private static final long HEALTH_CHECK_INTERVAL_MS = 30000; // Интервал проверки состояния системы (30 секунд)
    
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);
    private final CountDownLatch shutdownComplete = new CountDownLatch(1);
    private final PollScheduler scheduler;
    private long lastHealthCheckTime = 0;
    private Thread commandListenerThread;
    
    public ParserShutdownMonitor(PollScheduler scheduler) {
        this.scheduler = scheduler;
        
        // Добавляем обработчик сигнала Ctrl+C для немедленного завершения
        try {
            Signal.handle(new Signal("INT"), new SignalHandler() {
                @Override
                public void handle(Signal signal) {
                    logger.info("Received Ctrl+C signal, exiting immediately");
                    // Немедленный выход без запроса подтверждения
                    System.exit(130); // 130 - код выхода для SIGINT
                }
            });
            logger.info("Ctrl+C signal handler registered");
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Failed to register Ctrl+C handler: " + e.getMessage(), e);
        } catch (UnsatisfiedLinkError e) {
            logger.log(Level.WARNING, "Native signal handling not supported on this platform", e);
        }
    }
    
    @Override
    public void run() {
        logger.info("Parser shutdown monitor started");
        
        try {
            // Цикл проверки состояния
            while (!shutdownRequested.get()) {
                // Периодическая проверка состояния системы
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastHealthCheckTime >= HEALTH_CHECK_INTERVAL_MS) {
                    performHealthCheck();
                    lastHealthCheckTime = currentTime;
                }
                
                Thread.sleep(CHECK_INTERVAL_MS);
            }
            
            // Когда запрошено завершение, выполняем корректную остановку
            logger.info("Shutdown requested, stopping parser gracefully...");
            
            try {
                // Останавливаем планировщик и ждем завершения активных задач
                scheduler.shutdown();
                
                // Можно добавить дополнительные действия по очистке ресурсов
                // Например, сохранение кэша, финальный экспорт данных и т.д.
                
                logger.info("Parser has been shut down gracefully");
            } catch (IllegalStateException e) {
                logger.log(Level.SEVERE, "Scheduler in illegal state during shutdown", e);
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "Error during parser shutdown", e);
            }
        } catch (InterruptedException e) {
            logger.info("Shutdown monitor thread interrupted");
            Thread.currentThread().interrupt();
        } finally {
            // Сигнализируем о завершении процесса остановки
            shutdownComplete.countDown();
        }
    }
    
    /**
     * Запускает поток для обработки команд, введенных пользователем
     */
    public void startCommandListener() {
        commandListenerThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            try {
                logger.info("Command listener started. Type 'stop' to gracefully shutdown the application.");
                System.out.println("Command listener started. Type 'stop' to gracefully shutdown the application.");
                
                while (!shutdownRequested.get()) {
                    if (scanner.hasNextLine()) {
                        String command = scanner.nextLine().trim().toLowerCase();
                        processCommand(command);
                    }
                }
            } catch (NoSuchElementException e) {
                logger.log(Level.WARNING, "System input stream closed or unavailable", e);
            } catch (IllegalStateException e) {
                logger.log(Level.WARNING, "Scanner closed or in invalid state", e);
            } catch (SecurityException e) {
                logger.log(Level.WARNING, "Security restriction when accessing system input", e);
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "Unexpected error processing command input", e);
            } finally {
                scanner.close();
            }
        }, "command-listener");
        
        commandListenerThread.setDaemon(true);
        commandListenerThread.start();
        logger.info("Command listener thread started");
    }
    
    /**
     * Обрабатывает введенную команду
     * 
     * @param command строка с командой
     */
    private void processCommand(String command) {
        switch (command) {
            case "stop":
                logger.info("Received 'stop' command from console. Initiating graceful shutdown...");
                System.out.println("Shutdown initiated. Please wait for the application to complete all tasks...");
                requestShutdown();
                
                try {
                    // Ждем завершения всех задач с таймаутом
                    if (!awaitTermination(5000)) {
                        logger.warning("Parser shutdown timed out");
                        System.out.println("Timed out waiting for tasks to complete.");
                    } else {
                        logger.info("All tasks completed successfully");
                        System.out.println("All tasks completed successfully.");
                    }
                } catch (InterruptedException e) {
                    logger.warning("Shutdown process interrupted");
                    Thread.currentThread().interrupt();
                }
                
                // Добавляем вызов System.exit(0) для полного завершения JVM
                logger.info("Exiting application");
                System.exit(0);
                break;
            case "status":
                showStatus();
                break;
            case "help":
                showHelp();
                break;
            default:
                if (!command.isEmpty()) {
                    System.out.println("Unknown command: " + command + ". Type 'help' for available commands.");
                }
        }
    }
    
    /**
     * Отображает текущий статус приложения
     */
    private void showStatus() {
        Runtime runtime = Runtime.getRuntime();
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;
        
        System.out.println("\n=== API Poller Status ===");
        System.out.println("Application status: " + (shutdownRequested.get() ? "Shutting down" : "Running"));
        System.out.println("Memory usage: " + usedMemory + "MB of " + totalMemory + 
                          "MB (max: " + maxMemory + "MB)");
        System.out.println("Memory utilization: " + (usedMemory * 100 / totalMemory) + "%");
        System.out.println("Thread count: " + Thread.activeCount());
        System.out.println("Uptime: " + formatUptime());
        System.out.println("======================\n");
    }
    
    /**
     * Отображает справку по доступным командам
     */
    private void showHelp() {
        System.out.println("\n=== API Poller Commands ===");
        System.out.println("stop   - Gracefully shutdown the application");
        System.out.println("status - Show current application status");
        System.out.println("help   - Show this help information");
        System.out.println("=========================\n");
    }
    
    /**
     * Форматирует время работы приложения в читаемом виде
     */
    private String formatUptime() {
        long uptime = System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getStartTime();
        long uptimeInSeconds = uptime / 1000;
        long hours = uptimeInSeconds / 3600;
        long minutes = (uptimeInSeconds % 3600) / 60;
        long seconds = uptimeInSeconds % 60;
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    
    /**
     * Запрашивает завершение работы парсера
     */
    public void requestShutdown() {
        shutdownRequested.set(true);
        logger.info("Shutdown requested");
    }
    
    /**
     * Ожидает полного завершения работы парсера
     * @param timeoutMs максимальное время ожидания в миллисекундах
     * @return true, если парсер успешно завершил работу, false - если истекло время ожидания
     */
    public boolean awaitTermination(long timeoutMs) throws InterruptedException {
        return shutdownComplete.await(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    /**
     * Выполняет проверку состояния системы и приложения
     */
    private void performHealthCheck() {
        try {
            // Проверка доступной памяти
            Runtime runtime = Runtime.getRuntime();
            long freeMemory = runtime.freeMemory() / (1024 * 1024);
            long totalMemory = runtime.totalMemory() / (1024 * 1024);
            long maxMemory = runtime.maxMemory() / (1024 * 1024);
            long usedMemory = totalMemory - freeMemory;
            
            // Логирование использования памяти
            logger.fine(String.format(
                "Memory usage: %dMB used of %dMB total (max: %dMB, %d%% used)",
                usedMemory, totalMemory, maxMemory, (usedMemory * 100 / totalMemory)
            ));
            
            if (usedMemory > totalMemory * 0.9) {  // Если используется более 90% памяти
                logger.warning(String.format(
                    "Memory usage critical: %dMB used of %dMB total (%d%% used)",
                    usedMemory, totalMemory, (usedMemory * 100 / totalMemory)
                ));
                
                // Запуск сборщика мусора если память на исходе
                System.gc();
            }
            
        } catch (ArithmeticException e) {
            logger.log(Level.WARNING, "Arithmetic error in health check calculations", e);
        } catch (SecurityException e) {
            logger.log(Level.WARNING, "Security restriction when accessing runtime information", e);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Unexpected error performing health check", e);
        }
    }
}
