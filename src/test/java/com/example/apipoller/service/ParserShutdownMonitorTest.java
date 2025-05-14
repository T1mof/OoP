package com.example.apipoller.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParserShutdownMonitorTest {

    @Mock
    private PollScheduler mockScheduler;

    private ParserShutdownMonitor monitor;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final InputStream originalIn = System.in;

    @BeforeEach
    public void setUp() {
        monitor = new ParserShutdownMonitor(mockScheduler);
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    @Test
    public void testInitialization() {
        assertNotNull(monitor);
    }

    @Test
    public void testRequestShutdown() throws Exception {
        // Проверяем начальное состояние
        Field field = ParserShutdownMonitor.class.getDeclaredField("shutdownRequested");
        field.setAccessible(true);
        
        assertFalse(((AtomicBoolean)field.get(monitor)).get());
        
        // Запрашиваем завершение
        monitor.requestShutdown();
        
        // Проверяем, что состояние изменилось
        assertTrue(((AtomicBoolean)field.get(monitor)).get());
    }

    @Test
    public void testRunAndGracefulShutdown() throws Exception {
        // Запускаем монитор в отдельном потоке
        Thread monitorThread = new Thread(monitor);
        monitorThread.start();
        
        // Ждем запуск
        Thread.sleep(500);
        
        // Запрашиваем завершение
        monitor.requestShutdown();
        
        // Ждем завершения
        monitorThread.join(1000);
        
        // Проверяем, что scheduler.shutdown был вызван
        verify(mockScheduler, times(1)).shutdown();
        
        // Поток должен завершиться
        assertFalse(monitorThread.isAlive());
    }

    @Test
    public void testAwaitTermination() throws Exception {
        // Запускаем монитор в отдельном потоке
        Thread monitorThread = new Thread(monitor);
        monitorThread.start();
        
        // Запрашиваем завершение
        monitor.requestShutdown();
        
        // Ожидаем завершение
        boolean terminated = monitor.awaitTermination(1000);
        
        // Проверяем успешное завершение
        assertTrue(terminated);
        
        // Очистка
        monitorThread.join();
    }

    @Test
    public void testProcessCommandStop() throws Exception {
        // Получаем доступ к приватному методу через рефлексию
        Method processCommand = ParserShutdownMonitor.class.getDeclaredMethod(
                "processCommand", String.class);
        processCommand.setAccessible(true);
        
        // Вызываем метод с командой "stop"
        processCommand.invoke(monitor, "stop");
        
        // Проверяем, что shutdownRequested установлен в true
        Field field = ParserShutdownMonitor.class.getDeclaredField("shutdownRequested");
        field.setAccessible(true);
        assertTrue(((AtomicBoolean)field.get(monitor)).get());
        
        // Проверяем вывод
        String output = outputStream.toString();
        assertTrue(output.contains("Shutdown initiated"));
    }

    @Test
    public void testProcessCommandStatus() throws Exception {
        // Получаем доступ к приватному методу через рефлексию
        Method processCommand = ParserShutdownMonitor.class.getDeclaredMethod(
                "processCommand", String.class);
        processCommand.setAccessible(true);
        
        // Вызываем метод с командой "status"
        processCommand.invoke(monitor, "status");
        
        // Проверяем вывод
        String output = outputStream.toString();
        assertTrue(output.contains("=== API Poller Status ==="));
        assertTrue(output.contains("Application status: Running"));
        assertTrue(output.contains("Memory usage:"));
        assertTrue(output.contains("Thread count:"));
        assertTrue(output.contains("Uptime:"));
    }

    @Test
    public void testProcessCommandHelp() throws Exception {
        // Получаем доступ к приватному методу через рефлексию
        Method processCommand = ParserShutdownMonitor.class.getDeclaredMethod(
                "processCommand", String.class);
        processCommand.setAccessible(true);
        
        // Вызываем метод с командой "help"
        processCommand.invoke(monitor, "help");
        
        // Проверяем вывод
        String output = outputStream.toString();
        assertTrue(output.contains("=== API Poller Commands ==="));
        assertTrue(output.contains("stop   - Gracefully shutdown the application"));
        assertTrue(output.contains("status - Show current application status"));
        assertTrue(output.contains("help   - Show this help information"));
    }

    @Test
    public void testProcessCommandUnknown() throws Exception {
        // Получаем доступ к приватному методу через рефлексию
        Method processCommand = ParserShutdownMonitor.class.getDeclaredMethod(
                "processCommand", String.class);
        processCommand.setAccessible(true);
        
        // Вызываем метод с неизвестной командой
        processCommand.invoke(monitor, "unknown");
        
        // Проверяем вывод
        String output = outputStream.toString();
        assertTrue(output.contains("Unknown command: unknown"));
        assertTrue(output.contains("Type 'help' for available commands"));
    }

    @Test
    public void testPerformHealthCheck() throws Exception {
        // Получаем доступ к приватному методу через рефлексию
        Method performHealthCheck = ParserShutdownMonitor.class.getDeclaredMethod(
                "performHealthCheck");
        performHealthCheck.setAccessible(true);
        
        // Вызываем метод
        performHealthCheck.invoke(monitor);
        
        // Проверяем отсутствие исключений
    }
    
    @Test
    public void testAwaitTerminationTimeout() throws Exception {
        // Без вызова countDown на задвижке awaitTermination должен выдать таймаут
        boolean terminated = monitor.awaitTermination(100);
        
        // Должен быть таймаут
        assertFalse(terminated);
    }
    
    @Test
    public void testFormatUptime() throws Exception {
        // Получаем доступ к приватному методу через рефлексию
        Method formatUptime = ParserShutdownMonitor.class.getDeclaredMethod(
                "formatUptime");
        formatUptime.setAccessible(true);
        
        // Вызываем метод
        String result = (String) formatUptime.invoke(monitor);
        
        // Проверяем формат вывода
        assertTrue(result.matches("\\d{2}:\\d{2}:\\d{2}"));
    }
}
