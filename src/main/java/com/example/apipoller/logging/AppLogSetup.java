package com.example.apipoller.logging;

import java.io.IOException;
import java.util.logging.*;

public class AppLogSetup {
    private static final String LOG_FILE = "app.log";

    public static void setupLogger() throws IOException {
        // Получаем корневой логгер
        Logger rootLogger = Logger.getLogger("");
        
        // Удаляем все существующие обработчики (включая консольный)
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }
        
        // Настраиваем корневой уровень логирования
        rootLogger.setLevel(Level.INFO);
        
        // Создаем файловый обработчик с возможностью добавления (append=true)
        FileHandler fileHandler = new FileHandler(LOG_FILE, true);
        
        // Используем более информативный форматтер
        fileHandler.setFormatter(new SimpleFormatter() {
            private static final String format = "[%1$tF %1$tT] [%2$s] %3$s: %4$s %n";

            @Override
            public synchronized String format(LogRecord record) {
                return String.format(format,
                        record.getMillis(),
                        record.getLevel().getName(),
                        record.getLoggerName(),
                        record.getMessage());
            }
        });
        
        // Устанавливаем уровень логирования для обработчика
        fileHandler.setLevel(Level.INFO);
        
        // Добавляем обработчик к корневому логгеру
        rootLogger.addHandler(fileHandler);
        
        // Тестовая запись для подтверждения настройки
        Logger logger = Logger.getLogger(AppLogSetup.class.getName());
        logger.info("Логирование в файл " + LOG_FILE + " настроено");
        
        // Отключаем логирование в консоль от родительского обработчика
        logger.setUseParentHandlers(false);
    }
}
