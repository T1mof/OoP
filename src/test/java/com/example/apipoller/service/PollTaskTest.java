package com.example.apipoller.service;

import com.example.apipoller.api.ApiService;
import com.example.apipoller.model.NewsRecord;
import com.example.apipoller.writer.DataWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class PollTaskTest {

    private PollTask pollTask;

    @Mock
    private ApiService apiService;

    @Mock
    private DataWriter writer;

    private LinkedBlockingQueue<Runnable> taskQueue;
    
    private final long timeout = 1L;
    private final TimeUnit timeUnit = TimeUnit.SECONDS;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        taskQueue = new LinkedBlockingQueue<>();
        pollTask = new PollTask(apiService, writer, taskQueue, timeout, timeUnit);
        when(apiService.getServiceName()).thenReturn("test");
    }

    @Test
    public void testRunWithNewRecords() throws Exception {
        // Создание тестовой записи
        NewsRecord record = new NewsRecord(
            "Test Title", "Test Description", "https://example.com",
            "Test Source", "2025-04-27T00:00:00Z", "Test Author"
        );

        // Настройка поведения моков
        when(apiService.fetchData()).thenReturn(List.of(record));

        // Запуск задачи
        pollTask.run();

        // Проверка вызовов
        verify(writer, times(1)).writeRecords(any());
        
        // Проверка размера очереди
        assertEquals(1, taskQueue.size(), "Задача должна быть добавлена в очередь");
    }

    @Test
    public void testRunWithNoNewRecords() throws Exception {
        // Настройка поведения моков
        when(apiService.fetchData()).thenReturn(Collections.emptyList());

        // Запуск задачи
        pollTask.run();

        // Проверка вызовов
        verify(writer, never()).writeRecords(any());
        
        // Проверка размера очереди
        assertEquals(1, taskQueue.size(), "Задача должна быть добавлена в очередь");
    }

    @Test
    public void testRunWithNullRecords() throws Exception {
        // Настройка поведения моков
        when(apiService.fetchData()).thenReturn(null);

        // Запуск задачи
        pollTask.run();

        // Проверка вызовов
        verify(writer, never()).writeRecords(any());
        
        // Проверка размера очереди
        assertEquals(1, taskQueue.size(), "Задача должна быть добавлена в очередь");
    }

    @Test
    public void testStop() throws Exception {
        // Остановка задачи
        pollTask.stop();
        pollTask.run();

        // Проверка вызовов
        verify(apiService, never()).fetchData();
        verify(writer, never()).writeRecords(any());
        
        // Проверка размера очереди
        assertEquals(0, taskQueue.size(), "Задача не должна быть добавлена в очередь");
    }
    
    @Test
    public void testRunWithSocketTimeoutException() throws Exception {
        // Мокируем исключение
        when(apiService.fetchData()).thenThrow(new SocketTimeoutException("Connection timed out"));
        
        // Запуск задачи
        pollTask.run();
        
        // Проверка вызовов
        verify(writer, never()).writeRecords(any());
        assertEquals(1, taskQueue.size(), "Задача должна быть добавлена в очередь");
    }
    
    @Test
    public void testRunWithConnectException() throws Exception {
        // Мокируем исключение
        when(apiService.fetchData()).thenThrow(new ConnectException("Connection failed"));
        
        // Запуск задачи
        pollTask.run();
        
        // Проверка вызовов
        verify(writer, never()).writeRecords(any());
        assertEquals(1, taskQueue.size(), "Задача должна быть добавлена в очередь");
    }
    
    @Test
    public void testRunWithJsonProcessingException() throws Exception {
        // Мокируем исключение
        when(apiService.fetchData()).thenThrow(new JsonProcessingException("JSON parsing error") {});
        
        // Запуск задачи
        pollTask.run();
        
        // Проверка вызовов
        verify(writer, never()).writeRecords(any());
        assertEquals(1, taskQueue.size(), "Задача должна быть добавлена в очередь");
    }
    
    @Test
    public void testRunWithIllegalStateException() throws Exception {
        // Мокируем исключение
        when(apiService.fetchData()).thenThrow(new IllegalStateException("Illegal state"));
        
        // Запуск задачи
        pollTask.run();
        
        // Проверка вызовов
        verify(writer, never()).writeRecords(any());
        assertEquals(1, taskQueue.size(), "Задача должна быть добавлена в очередь");
    }
    
    @Test
    public void testRunWithIOException() throws Exception {
        // Мокируем исключение
        when(apiService.fetchData()).thenThrow(new IOException("IO error"));
        
        // Запуск задачи
        pollTask.run();
        
        // Проверка вызовов
        verify(writer, never()).writeRecords(any());
        assertEquals(1, taskQueue.size(), "Задача должна быть добавлена в очередь");
    }
    
    @Test
    public void testRunWithRuntimeException() throws Exception {
        // Мокируем исключение
        when(apiService.fetchData()).thenThrow(new RuntimeException("Runtime error"));
        
        // Запуск задачи
        pollTask.run();
        
        // Проверка вызовов
        verify(writer, never()).writeRecords(any());
        assertEquals(1, taskQueue.size(), "Задача должна быть добавлена в очередь");
    }
    
    @Test
    public void testScheduleNextExecutionInterruptedException() throws Exception {
        // Создаем мок для TimeUnit, который будет выбрасывать исключение
        TimeUnit mockedTimeUnit = mock(TimeUnit.class);
        doThrow(new InterruptedException("Interrupted")).when(mockedTimeUnit).sleep(anyLong());
        
        // Создаем задачу с мокированным TimeUnit
        PollTask task = new PollTask(apiService, writer, taskQueue, timeout, mockedTimeUnit);
        
        // Запуск задачи
        task.run();
        
        // Задача не должна быть добавлена в очередь из-за прерывания
        assertEquals(0, taskQueue.size(), "Задача не должна быть добавлена в очередь при прерывании");
    }
    
    @Test
    public void testScheduleNextExecutionRejectedExecutionException() throws Exception {
        // Создаем мок для очереди задач с параметризацией и аннотацией для подавления предупреждений
        BlockingQueue<Runnable> mockQueue = mock(BlockingQueue.class);
        doThrow(new RejectedExecutionException("Task rejected")).when(mockQueue).put(any());
        
        // Создаем задачу с мокированной очередью
        PollTask task = new PollTask(apiService, writer, mockQueue, timeout, timeUnit);
        
        // Запуск задачи
        task.run();
        
        // Проверяем, что метод put был вызван
        verify(mockQueue).put(any());
    }
    
    @Test
    public void testScheduleNextExecutionIllegalStateException() throws Exception {
        // Создаем мок для очереди задач с параметризацией и аннотацией для подавления предупреждений
        BlockingQueue<Runnable> mockQueue = mock(BlockingQueue.class);
        doThrow(new IllegalStateException("Queue in illegal state")).when(mockQueue).put(any());
        
        // Создаем задачу с мокированной очередью
        PollTask task = new PollTask(apiService, writer, mockQueue, timeout, timeUnit);
        
        // Запуск задачи
        task.run();
        
        // Проверяем, что метод put был вызван
        verify(mockQueue).put(any());
    }
    
    @Test
    public void testScheduleNextExecutionNullPointerException() throws Exception {
        // Создаем мок для очереди задач с параметризацией и аннотацией для подавления предупреждений
        BlockingQueue<Runnable> mockQueue = mock(BlockingQueue.class);
        doThrow(new NullPointerException("Null pointer")).when(mockQueue).put(any());
        
        // Создаем задачу с мокированной очередью
        PollTask task = new PollTask(apiService, writer, mockQueue, timeout, timeUnit);
        
        // Запуск задачи
        task.run();
        
        // Проверяем, что метод put был вызван
        verify(mockQueue).put(any());
    }
    
    @Test
    public void testScheduleNextExecutionRuntimeException() throws Exception {
        // Создаем мок для очереди задач с параметризацией и аннотацией для подавления предупреждений
        BlockingQueue<Runnable> mockQueue = mock(BlockingQueue.class);
        doThrow(new RuntimeException("Runtime error")).when(mockQueue).put(any());
        
        // Создаем задачу с мокированной очередью
        PollTask task = new PollTask(apiService, writer, mockQueue, timeout, timeUnit);
        
        // Запуск задачи
        task.run();
        
        // Проверяем, что метод put был вызван
        verify(mockQueue).put(any());
    }
}
