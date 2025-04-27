package com.example.apipoller.api;

import com.example.apipoller.model.ApiRecord;
import java.io.IOException;
import java.util.List;

/**
 * Интерфейс для сервисов, выполняющих запросы к API
 */
public interface ApiService {
    /**
     * Возвращает название сервиса (для логирования и идентификации)
     * @return строковое название сервиса
     */
    String getServiceName();
    
    /**
     * Выполняет запрос к API и возвращает список новых записей
     * @return список новых записей
     * @throws IOException если произошла ошибка при запросе
     */
    List<ApiRecord> fetchData() throws IOException;
}
