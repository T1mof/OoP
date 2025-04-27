package com.example.apipoller.model;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class NewsRecordTest {

    @Test
    public void testConstructorAndGetters() {
        NewsRecord record = new NewsRecord(
            "Test Title",
            "Test Description",
            "https://example.com/news",
            "Example Source",
            "2025-04-27T10:15:30Z",
            "John Doe"
        );
        
        assertEquals("https://example.com/news", record.getId());
        
        Map<String, Object> map = record.toMap();
        assertEquals("news", map.get("type"));
        assertEquals("Test Title", map.get("title"));
        assertEquals("Test Description", map.get("description"));
        assertEquals("https://example.com/news", map.get("url"));
        assertEquals("Example Source", map.get("source"));
        assertEquals("2025-04-27T10:15:30Z", map.get("publishedAt"));
        assertEquals("John Doe", map.get("author"));
    }
    
    @Test
    public void testNullValues() {
        NewsRecord record = new NewsRecord(null, null, null, null, null, null);
        
        assertEquals("", record.getId());
        
        Map<String, Object> map = record.toMap();
        assertEquals("news", map.get("type"));
        assertEquals("", map.get("title"));
        assertEquals("", map.get("description"));
        assertEquals("", map.get("url"));
        assertEquals("", map.get("source"));
        assertEquals("", map.get("publishedAt"));
        assertEquals("", map.get("author"));
    }
    
    @Test
    public void testEqualsAndHashCode() {
        NewsRecord record1 = new NewsRecord(
            "Title 1", "Desc 1", "https://example.com/1", "Source 1", "2025-04-27T10:00:00Z", "Author 1"
        );
        
        NewsRecord record2 = new NewsRecord(
            "Title 2", "Desc 2", "https://example.com/1", "Source 2", "2025-04-27T11:00:00Z", "Author 2"
        );
        
        NewsRecord record3 = new NewsRecord(
            "Title 1", "Desc 1", "https://example.com/2", "Source 1", "2025-04-27T10:00:00Z", "Author 1"
        );
        
        // Записи с одинаковым URL должны быть равны
        assertEquals(record1, record2);
        assertEquals(record1.hashCode(), record2.hashCode());
        
        // Записи с разными URL не должны быть равны
        assertNotEquals(record1, record3);
        assertNotEquals(record1.hashCode(), record3.hashCode());
        
        // Запись не равна null или объекту другого класса
        assertNotEquals(record1, null);
        assertNotEquals(record1, "не запись");
    }
    
    @Test
    public void testToString() {
        NewsRecord record = new NewsRecord(
            "Test Title", "Test Description", "https://example.com/news", 
            "Example Source", "2025-04-27T10:15:30Z", "John Doe"
        );
        
        String toString = record.toString();
        assertTrue(toString.contains("Test Title"));
        assertTrue(toString.contains("Example Source"));
        assertTrue(toString.contains("https://example.com/news"));
    }
}
