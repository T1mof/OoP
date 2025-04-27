package com.example.apipoller.service;

import com.example.apipoller.api.ApiService;
import com.example.apipoller.model.ApiRecord;
import com.example.apipoller.writer.DataWriter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Задача для опроса API и записи результатов в файл.
 */
public class PollTask implements Runnable {
    private static final Logger logger = Logger.getLogger(PollTask.class.getName());
    
    private final ApiService apiService;
    private final DataWriter writer;
    private final BlockingQueue<Runnable> taskQueue;
    private final long timeout;
    private final TimeUnit timeUnit;
    private boolean isStopped = false;

    public PollTask(ApiService apiService, DataWriter writer, BlockingQueue<Runnable> taskQueue, 
                   long timeout, TimeUnit timeUnit) {
        this.apiService = apiService;
        this.writer = writer;
        this.taskQueue = taskQueue;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    @Override
    public void run() {
        if (isStopped) {
            return;
        }
        
        String serviceName = apiService.getServiceName();
        logger.info("Polling service: " + serviceName);
        
        try {
            // Запрос данных от API
            List<ApiRecord> records = apiService.fetchData();
            
            // Если получены новые записи, записываем их в файл
            if (records != null && !records.isEmpty()) {
                logger.info("Got " + records.size() + " new records from " + serviceName);
                writer.writeRecords(records);
            } else {
                logger.info("No new records from " + serviceName);
            }
            
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error polling " + serviceName + ": " + e.getMessage(), e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error polling " + serviceName, e);
        } finally {
            if (!isStopped) {
                // Планируем следующее выполнение задачи после таймаута
                try {
                    // Имитируем задержку
                    timeUnit.sleep(timeout);
                    
                    // Помещаем задачу обратно в очередь
                    taskQueue.put(this);
                    logger.info("Scheduled next poll for " + serviceName + " after " + timeout + " " + timeUnit);
                } catch (InterruptedException e) {
                    logger.info("Poll task for " + serviceName + " was interrupted");
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Останавливает дальнейшее планирование задачи
     */
    public void stop() {
        isStopped = true;
    }
}
