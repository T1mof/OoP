package com.example.apipoller.service;

import com.example.apipoller.api.ApiService;
import com.example.apipoller.model.ApiRecord;
import com.example.apipoller.model.NewsRecord;
import com.example.apipoller.writer.DataWriter;
import mockit.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class PollTaskTest {

    @Tested
    private PollTask pollTask;

    @Injectable
    private ApiService apiService;

    @Injectable
    private DataWriter writer;

    // Используем реальный объект вместо мока
    private LinkedBlockingQueue<Runnable> taskQueue;
    
    @Injectable
    private long timeout = 1L;

    @Injectable
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    @BeforeEach
    public void setUp() {
        taskQueue = new LinkedBlockingQueue<>();
        // Явно создаем объект PollTask с реальной очередью
        pollTask = new PollTask(apiService, writer, taskQueue, timeout, timeUnit);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRunWithNewRecords() throws Exception {
        // Создание тестовой записи
        NewsRecord record = new NewsRecord(
            "Test Title", "Test Description", "https://example.com",
            "Test Source", "2025-04-27T00:00:00Z", "Test Author"
        );

        // Настройка ожиданий
        new Expectations() {{
            apiService.getServiceName(); result = "test";
            apiService.fetchData(); result = List.of(record);
        }};

        // Запуск задачи
        pollTask.run();

        // Проверка вызовов
        new Verifications() {{
            // Проверяем, что запись была вызвана один раз
            writer.writeRecords((List<ApiRecord>) any); times = 1;
        }};
        
        // Проверка размера очереди напрямую, а не через мокирование
        assertEquals(1, taskQueue.size(), "Задача должна быть добавлена в очередь");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRunWithNoNewRecords() throws Exception {
        // Настройка ожиданий
        new Expectations() {{
            apiService.getServiceName(); result = "test";
            apiService.fetchData(); result = Collections.emptyList();
        }};

        // Запуск задачи
        pollTask.run();

        // Проверка вызовов
        new Verifications() {{
            // Не должно быть вызовов записи
            writer.writeRecords((List<ApiRecord>) any); times = 0;
        }};
        
        // Проверка размера очереди напрямую
        assertEquals(1, taskQueue.size(), "Задача должна быть добавлена в очередь");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStop() throws Exception {
        // Остановка задачи
        pollTask.stop();
        pollTask.run();

        // Проверка вызовов
        new Verifications() {{
            apiService.fetchData(); times = 0;
            writer.writeRecords((List<ApiRecord>) any); times = 0;
        }};
        
        // Проверка размера очереди напрямую
        assertEquals(0, taskQueue.size(), "Задача не должна быть добавлена в очередь");
    }
}
