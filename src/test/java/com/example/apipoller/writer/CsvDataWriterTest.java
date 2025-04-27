package com.example.apipoller.writer;

import com.example.apipoller.model.NewsRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CsvDataWriterTest {
    private Path tempFile;
    private CsvDataWriter writer;

    @BeforeEach
    public void setUp() throws Exception {
        tempFile = Files.createTempFile("test", ".csv");
        writer = new CsvDataWriter(tempFile);
    }

    @AfterEach
    public void tearDown() throws Exception {
        writer.close();
        Files.deleteIfExists(tempFile);
    }

    @Test
    public void testWriteRecords() throws Exception {
        NewsRecord record = new NewsRecord("title", "desc", "url", "source", "2025-04-27T00:00:00Z", "author");
        writer.writeRecords(List.of(record));
        String content = Files.readString(tempFile);
        assertTrue(content.contains("title"));
        assertTrue(content.contains("url"));
    }
}
