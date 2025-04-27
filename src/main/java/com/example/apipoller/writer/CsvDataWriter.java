package com.example.apipoller.writer;

import com.example.apipoller.model.ApiRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Писатель данных в формате CSV
 */
public class CsvDataWriter implements DataWriter {
    private static final Logger logger = Logger.getLogger(CsvDataWriter.class.getName());
    
    private final Path outputPath;
    private final Object writeLock = new Object();
    private final Set<String> writtenHeaders = new HashSet<>();

    public CsvDataWriter(Path outputPath) {
        this.outputPath = outputPath;
        
        // Создаем файл, если он не существует
        try {
            if (!Files.exists(outputPath)) {
                Files.createFile(outputPath);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error initializing CSV file", e);
        }
    }

    @Override
    public void writeRecords(List<ApiRecord> records) throws IOException {
        if (records == null || records.isEmpty()) {
            return;
        }

        synchronized (writeLock) {
            try (FileWriter fileWriter = new FileWriter(outputPath.toFile(), true);
                 CSVPrinter csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT)) {
                
                for (ApiRecord record : records) {
                    Map<String, Object> map = record.toMap();
                    List<String> headers = new ArrayList<>(map.keySet());
                    
                    // Если это новый набор заголовков, запишем их
                    String headersKey = String.join(",", headers);
                    if (!writtenHeaders.contains(headersKey)) {
                        csvPrinter.printRecord(headers);
                        writtenHeaders.add(headersKey);
                    }
                    
                    // Запись значений
                    List<Object> values = new ArrayList<>();
                    for (String header : headers) {
                        values.add(map.get(header));
                    }
                    csvPrinter.printRecord(values);
                }
                
                logger.info("Successfully wrote " + records.size() + " records to CSV file: " + outputPath);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error writing to CSV file", e);
                throw e;
            }
        }
    }

    @Override
    public void close() throws IOException {
        // Для CSV-писателя не требуется специальное закрытие ресурсов
    }
}
