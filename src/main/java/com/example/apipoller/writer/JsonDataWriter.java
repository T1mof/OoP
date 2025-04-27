package com.example.apipoller.writer;

import com.example.apipoller.model.ApiRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Писатель данных в формате JSON
 */
public class JsonDataWriter implements DataWriter {
    private static final Logger logger = Logger.getLogger(JsonDataWriter.class.getName());
    
    private final Path outputPath;
    private final ObjectMapper mapper;
    private final Object writeLock = new Object();

    public JsonDataWriter(Path outputPath) {
        this.outputPath = outputPath;
        this.mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        // Создаем пустой JSON-массив, если файл не существует
        try {
            if (!Files.exists(outputPath)) {
                Files.write(outputPath, "[]".getBytes(), 
                           StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error initializing JSON file", e);
        }
    }

    @Override
    public void writeRecords(List<ApiRecord> records) throws IOException {
        if (records == null || records.isEmpty()) {
            return;
        }

        synchronized (writeLock) {
            try {
                // Читаем существующие записи
                List<Map<String, Object>> existingRecords = new ArrayList<>();
                if (Files.exists(outputPath) && Files.size(outputPath) > 2) {
                    existingRecords = mapper.readValue(outputPath.toFile(), 
                                                     mapper.getTypeFactory().constructCollectionType(
                                                         List.class, Map.class));
                }
                
                // Добавляем новые записи
                for (ApiRecord record : records) {
                    existingRecords.add(record.toMap());
                }
                
                // Записываем обновленный список записей
                mapper.writeValue(outputPath.toFile(), existingRecords);
                
                logger.info("Successfully wrote " + records.size() + " records to JSON file: " + outputPath);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error writing to JSON file", e);
                throw e;
            }
        }
    }

    @Override
    public void close() throws IOException {
        // Для JSON-писателя не требуется специальное закрытие ресурсов
    }
}
