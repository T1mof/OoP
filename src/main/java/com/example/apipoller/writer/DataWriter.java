package com.example.apipoller.writer;

import com.example.apipoller.model.ApiRecord;

import java.io.IOException;
import java.util.List;

/**
 * Интерфейс для компонентов, ответственных за запись данных в файл
 */
public interface DataWriter {
    /**
     * Записывает список записей в файл
     * @param records список записей для записи
     * @throws IOException если произошла ошибка при записи
     */
    void writeRecords(List<ApiRecord> records) throws IOException;
    
    /**
     * Закрывает ресурсы, связанные с писателем
     * @throws IOException если произошла ошибка при закрытии
     */
    void close() throws IOException;
}
