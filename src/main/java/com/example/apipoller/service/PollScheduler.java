package com.example.apipoller.service;

import com.example.apipoller.api.ApiService;
import com.example.apipoller.api.ApiServiceFactory;
import com.example.apipoller.config.AppConfig;
import com.example.apipoller.writer.DataWriter;
import com.example.apipoller.writer.DataWriterFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Планировщик задач опроса API, гарантирующий выполнение
 * не более N одновременных задач.
 */
public class PollScheduler {
    private static final Logger logger = Logger.getLogger(PollScheduler.class.getName());
    
    private final AppConfig config;
    private final ExecutorService executor;
    private final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    private final List<PollTask> tasks = new ArrayList<>();
    private final DataWriter writer;
    private boolean isRunning = false;
    
    private Thread coordinatorThread;

    public PollScheduler(AppConfig config) {
        this.config = config;
        this.executor = Executors.newFixedThreadPool(config.getMaxThreads());
        this.writer = DataWriterFactory.createWriter(config.getOutputFormat(), config.getOutputFile());
    }

    /**
     * Запускает планировщик задач
     */
    public synchronized void start() {
        if (isRunning) {
            return;
        }
        
        isRunning = true;
        
        // Создание задач опроса для каждого сервиса
        for (String serviceName : config.getServices()) {
            try {
                ApiService apiService = ApiServiceFactory.createService(serviceName);
                PollTask task = new PollTask(apiService, writer, taskQueue, 
                                           config.getTimeoutSeconds(), TimeUnit.SECONDS);
                tasks.add(task);
                taskQueue.add(task);
            } catch (IllegalArgumentException e) {
                logger.warning("Skipping unknown service: " + serviceName);
            }
        }
        
        if (tasks.isEmpty()) {
            logger.warning("No valid services configured. Exiting.");
            return;
        }
        
        // Запуск координатора задач
        coordinatorThread = new Thread(this::coordinateTasks);
        coordinatorThread.setDaemon(true);
        coordinatorThread.start();
        
        logger.info("Scheduler started with " + tasks.size() + " services and " + 
                   config.getMaxThreads() + " max concurrent threads");
    }

    /**
     * Останавливает планировщик задач
     */
    public synchronized void shutdown() {
        if (!isRunning) {
            return;
        }
        
        isRunning = false;
        
        // Остановка всех задач
        for (PollTask task : tasks) {
            task.stop();
        }
        
        // Остановка исполнителя и координатора
        executor.shutdownNow();
        if (coordinatorThread != null) {
            coordinatorThread.interrupt();
        }
        
        // Закрытие писателя
        try {
            writer.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error closing writer", e);
        }
        
        logger.info("Scheduler shut down");
    }

    /**
     * Координирует выполнение задач, обеспечивая ограничение
     * на максимальное количество одновременно выполняемых задач.
     */
    private void coordinateTasks() {
        int maxThreads = config.getMaxThreads();
        List<Future<?>> runningTasks = new ArrayList<>();
        
        try {
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                // Очистка завершенных задач
                runningTasks.removeIf(Future::isDone);
                
                // Запуск новых задач, если есть свободные потоки
                while (runningTasks.size() < maxThreads && !taskQueue.isEmpty()) {
                    Runnable task = taskQueue.take();
                    runningTasks.add(executor.submit(task));
                    logger.fine("Started new task, now running: " + runningTasks.size());
                }
                
                // Небольшая пауза перед следующей проверкой
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            logger.info("Coordinator thread interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in coordinator thread", e);
        }
    }
}
