package com.example.apipoller.writer;

import java.nio.file.Path;

/**
 * Фабрика для создания писателей данных в разных форматах
 */
public class DataWriterFactory {
    /**
     * Создает писатель данных для указанного формата и файла
     * @param format формат данных ("json" или "csv")
     * @param outputPath путь к выходному файлу
     * @return подходящая реализация DataWriter
     * @throws IllegalArgumentException если формат не поддерживается
     */
    public static DataWriter createWriter(String format, Path outputPath) {
        switch (format.toLowerCase()) {
            case "json":
                return new JsonDataWriter(outputPath);
            case "csv":
                return new CsvDataWriter(outputPath);
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }
}
