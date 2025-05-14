package com.example.apipoller.service;

import com.example.apipoller.api.ApiService;
import com.example.apipoller.api.ApiServiceFactory;
import com.example.apipoller.config.AppConfig;
import com.example.apipoller.writer.DataWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  // Исправление ошибок UnnecessaryStubbing
public class PollSchedulerTest {

    @Mock
    private AppConfig mockConfig;
    
    @Mock
    private DataWriter mockWriter;
    
    @Mock
    private ApiService mockApiService;
    
    @Mock
    private ExecutorService mockExecutor;
    
    @Mock
    private PollTask mockPollTask;  // Добавлен мок для PollTask
    
    private PollScheduler pollScheduler;
    
    @BeforeEach
    public void setUp() {
        // Настройка поведения конфигурации
        when(mockConfig.getMaxThreads()).thenReturn(2);
        when(mockConfig.getTimeoutSeconds()).thenReturn(5);
        when(mockConfig.getOutputFormat()).thenReturn("json");
        when(mockConfig.getOutputFile()).thenReturn(Path.of("test.json"));
        when(mockConfig.getServices()).thenReturn(Arrays.asList("news", "weather"));
        
        // Создаем тестируемый класс с возможностью подмены зависимостей
        pollScheduler = new TestPollScheduler(mockConfig, mockWriter, mockExecutor);
    }

    @Test
    public void testConstructor() {
        assertNotNull(pollScheduler);
    }
    
    @Test
    public void testStart() throws Exception {
        // Настройка моков для статических методов
        try (MockedStatic<ApiServiceFactory> mockedFactory = mockStatic(ApiServiceFactory.class)) {
            // Настраиваем ApiServiceFactory.createService
            mockedFactory.when(() -> ApiServiceFactory.createService("news")).thenReturn(mockApiService);
            mockedFactory.when(() -> ApiServiceFactory.createService("weather")).thenReturn(mockApiService);
            
            // Настраиваем поведение mockApiService
            when(mockApiService.getServiceName()).thenReturn("mock-service");
            
            // Запускаем планировщик
            pollScheduler.start();
            
            // Проверяем, что флаг isRunning установлен в true
            Field isRunningField = PollScheduler.class.getDeclaredField("isRunning");
            isRunningField.setAccessible(true);
            assertTrue((Boolean) isRunningField.get(pollScheduler));
            
            // Проверяем, что создано правильное количество задач
            Field tasksField = PollScheduler.class.getDeclaredField("tasks");
            tasksField.setAccessible(true);
            List<PollTask> tasks = (List<PollTask>) tasksField.get(pollScheduler);
            assertEquals(2, tasks.size());
            
            // Проверяем, что запущен координатор задач
            Field coordinatorThreadField = PollScheduler.class.getDeclaredField("coordinatorThread");
            coordinatorThreadField.setAccessible(true);
            Thread coordinatorThread = (Thread) coordinatorThreadField.get(pollScheduler);
            assertNotNull(coordinatorThread);
            assertTrue(coordinatorThread.isDaemon());
            
            // Очистка - останавливаем планировщик
            pollScheduler.shutdown();
        }
    }
    
    @Test
    public void testStartWithUnknownService() throws Exception {
        // Список сервисов с одним неизвестным
        when(mockConfig.getServices()).thenReturn(Arrays.asList("news", "unknown-service"));
        
        try (MockedStatic<ApiServiceFactory> mockedFactory = mockStatic(ApiServiceFactory.class)) {
            // Настраиваем ApiServiceFactory.createService
            mockedFactory.when(() -> ApiServiceFactory.createService("news")).thenReturn(mockApiService);
            mockedFactory.when(() -> ApiServiceFactory.createService("unknown-service"))
                .thenThrow(new IllegalArgumentException("Unknown service"));
            
            when(mockApiService.getServiceName()).thenReturn("news");
            
            // Запускаем планировщик
            pollScheduler.start();
            
            // Проверяем, что создана только одна задача
            Field tasksField = PollScheduler.class.getDeclaredField("tasks");
            tasksField.setAccessible(true);
            List<PollTask> tasks = (List<PollTask>) tasksField.get(pollScheduler);
            assertEquals(1, tasks.size());
            
            // Очистка - останавливаем планировщик
            pollScheduler.shutdown();
        }
    }
    
    @Test
    public void testStartWithNoValidServices() throws Exception {
        // Только неизвестные сервисы
        when(mockConfig.getServices()).thenReturn(Collections.singletonList("unknown-service"));
        
        try (MockedStatic<ApiServiceFactory> mockedFactory = mockStatic(ApiServiceFactory.class)) {
            // Настраиваем ApiServiceFactory.createService
            mockedFactory.when(() -> ApiServiceFactory.createService("unknown-service"))
                .thenThrow(new IllegalArgumentException("Unknown service"));
            
            // Запускаем планировщик
            pollScheduler.start();
            
            // Проверяем, что не создано ни одной задачи
            Field tasksField = PollScheduler.class.getDeclaredField("tasks");
            tasksField.setAccessible(true);
            List<PollTask> tasks = (List<PollTask>) tasksField.get(pollScheduler);
            assertTrue(tasks.isEmpty());
            
            // Проверяем, что координатор не запущен
            Field coordinatorThreadField = PollScheduler.class.getDeclaredField("coordinatorThread");
            coordinatorThreadField.setAccessible(true);
            Thread coordinatorThread = (Thread) coordinatorThreadField.get(pollScheduler);
            assertNull(coordinatorThread);
        }
    }
    
    @Test
    public void testShutdown() throws Exception {
        // Создаем тестовый объект с мок-списком задач
        PollScheduler scheduler = new TestPollSchedulerWithMockTasks(mockConfig, mockWriter, mockExecutor, mockPollTask);
        
        // Запускаем
        Field isRunningField = PollScheduler.class.getDeclaredField("isRunning");
        isRunningField.setAccessible(true);
        isRunningField.set(scheduler, true);
        
        // Останавливаем планировщик
        scheduler.shutdown();
        
        // Проверяем, что флаг isRunning сброшен
        assertFalse((Boolean) isRunningField.get(scheduler));
        
        // Проверяем, что вызваны ожидаемые методы
        verify(mockExecutor).shutdownNow();
        verify(mockWriter).close();
        
        // Проверяем, что задача была остановлена
        verify(mockPollTask).stop();
    }
    
    @Test
    public void testShutdownWhenAlreadyStopped() throws IOException {
        // Создаем планировщик, но не запускаем его
        // (isRunning = false)
        
        // Останавливаем планировщик
        pollScheduler.shutdown();
        
        // Проверяем, что не выполнено взаимодействие с зависимостями
        verifyNoInteractions(mockExecutor);
        verifyNoInteractions(mockWriter);
    }
    
    @Test
    public void testShutdownWithWriterException() throws Exception {
        // Настраиваем исключение при закрытии писателя
        doThrow(new IOException("Test IO exception")).when(mockWriter).close();
        
        // Создаем тестовый объект
        PollScheduler scheduler = new TestPollSchedulerWithMockTasks(mockConfig, mockWriter, mockExecutor, mockPollTask);
        
        // Запускаем
        Field isRunningField = PollScheduler.class.getDeclaredField("isRunning");
        isRunningField.setAccessible(true);
        isRunningField.set(scheduler, true);
        
        // Останавливаем планировщик
        scheduler.shutdown();
        
        // Проверяем, что методы были вызваны, несмотря на исключение
        verify(mockExecutor).shutdownNow();
        verify(mockWriter).close();
    }
    
    @Test
    public void testCoordinateTasksLogic() throws Exception {
        // Создаем мок для Future с правильной типизацией
        @SuppressWarnings("rawtypes")
        Future mockFuture = mock(Future.class);
        when(mockExecutor.submit(any(Runnable.class))).thenReturn(mockFuture);
        
        // Создаем реальную задачу и очередь для тестирования координатора
        Runnable mockRunnable = mock(Runnable.class);
        BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
        taskQueue.add(mockRunnable);
        
        // Получаем доступ к приватным полям
        Field taskQueueField = PollScheduler.class.getDeclaredField("taskQueue");
        taskQueueField.setAccessible(true);
        // Заменяем очередь задач на нашу тестовую
        taskQueueField.set(pollScheduler, taskQueue);
        
        // Запускаем координатор в отдельном потоке
        Thread testThread = new Thread(() -> {
            try {
                // Получаем доступ к приватному методу coordinateTasks
                java.lang.reflect.Method method = PollScheduler.class.getDeclaredMethod("coordinateTasks");
                method.setAccessible(true);
                method.invoke(pollScheduler);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        // Устанавливаем isRunning в true, чтобы coordinateTasks начал работу
        Field isRunningField = PollScheduler.class.getDeclaredField("isRunning");
        isRunningField.setAccessible(true);
        isRunningField.set(pollScheduler, true);
        
        // Запускаем поток
        testThread.start();
        
        // Ждем немного, чтобы coordinateTasks успел запустить задачу
        Thread.sleep(500);
        
        // Устанавливаем isRunning в false, чтобы координатор завершил работу
        isRunningField.set(pollScheduler, false);
        
        // Ждем завершения потока
        testThread.join(1000);
        
        // Проверяем, что задача была отправлена на выполнение
        verify(mockExecutor, atLeastOnce()).submit(mockRunnable);
    }
    
    /**
     * Расширение PollScheduler для тестирования, позволяющее внедрить моки.
     */
    private static class TestPollScheduler extends PollScheduler {
        private final DataWriter mockWriter;
        private final ExecutorService mockExecutor;
        
        public TestPollScheduler(AppConfig config, DataWriter writer, ExecutorService executor) {
            super(config);
            this.mockWriter = writer;
            this.mockExecutor = executor;
            
            // Подменяем поля на наши моки через рефлексию
            try {
                Field writerField = PollScheduler.class.getDeclaredField("writer");
                writerField.setAccessible(true);
                writerField.set(this, mockWriter);
                
                Field executorField = PollScheduler.class.getDeclaredField("executor");
                executorField.setAccessible(true);
                executorField.set(this, mockExecutor);
            } catch (Exception e) {
                throw new RuntimeException("Failed to setup TestPollScheduler", e);
            }
        }
    }
    
    /**
     * Расширение PollScheduler для тестов, требующих моки для задач.
     */
    private static class TestPollSchedulerWithMockTasks extends TestPollScheduler {
        public TestPollSchedulerWithMockTasks(AppConfig config, DataWriter writer, 
                ExecutorService executor, PollTask mockTask) {
            super(config, writer, executor);
            
            try {
                // Создаем список с мок задачей
                List<PollTask> mockTasks = Collections.singletonList(mockTask);
                
                // Подменяем поле tasks
                Field tasksField = PollScheduler.class.getDeclaredField("tasks");
                tasksField.setAccessible(true);
                tasksField.set(this, mockTasks);
            } catch (Exception e) {
                throw new RuntimeException("Failed to setup TestPollSchedulerWithMockTasks", e);
            }
        }
    }
}
