package com.example.apipoller.model;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для класса NasaRecord
 */
public class NasaRecordTest {

    @Test
    public void testConstructorAndGetters() {
        NasaRecord record = new NasaRecord(
            "test_id",
            "Test Title",
            "2025-04-27",
            "Test Explanation",
            "https://example.com/image.jpg",
            "image",
            "NASA"
        );
        
        assertEquals("test_id", record.getId());
        
        Map<String, Object> map = record.toMap();
        assertEquals("nasa", map.get("type"));
        assertEquals("test_id", map.get("id"));
        assertEquals("Test Title", map.get("title"));
        assertEquals("2025-04-27", map.get("date"));
        assertEquals("Test Explanation", map.get("explanation"));
        assertEquals("https://example.com/image.jpg", map.get("url"));
        assertEquals("image", map.get("mediaType"));
        assertEquals("NASA", map.get("copyright"));
    }
    
    @Test
    public void testNullValues() {
        NasaRecord record = new NasaRecord(null, null, null, null, null, null, null);
        
        assertEquals("", record.getId());
        
        Map<String, Object> map = record.toMap();
        assertEquals("nasa", map.get("type"));
        assertEquals("", map.get("id"));
        assertEquals("", map.get("title"));
        assertEquals("", map.get("date"));
        assertEquals("", map.get("explanation"));
        assertEquals("", map.get("url"));
        assertEquals("", map.get("mediaType"));
        assertEquals("", map.get("copyright"));
    }
    
    @Test
    public void testEqualsAndHashCode() {
        NasaRecord record1 = new NasaRecord(
            "apod_123", "Title 1", "2025-04-27", "Explanation 1",
            "https://example.com/image1.jpg", "image", "Author 1"
        );
        
        NasaRecord record2 = new NasaRecord(
            "apod_123", "Title 2", "2025-04-28", "Explanation 2",
            "https://example.com/image2.jpg", "image", "Author 2"
        );
        
        NasaRecord record3 = new NasaRecord(
            "apod_456", "Title 1", "2025-04-27", "Explanation 1",
            "https://example.com/image1.jpg", "image", "Author 1"
        );
        
        // Записи с одинаковым ID должны быть равны
        assertEquals(record1, record2);
        assertEquals(record1.hashCode(), record2.hashCode());
        
        // Записи с разными ID не должны быть равны
        assertNotEquals(record1, record3);
        assertNotEquals(record1.hashCode(), record3.hashCode());
        
        // Запись не равна null или объекту другого класса
        assertNotEquals(record1, null);
        assertNotEquals(record1, "не запись");
    }
    
    @Test
    public void testToString() {
        NasaRecord record = new NasaRecord(
            "test_id", "Test Title", "2025-04-27", "Test Explanation",
            "https://example.com/image.jpg", "image", "NASA"
        );
        
        String toString = record.toString();
        assertTrue(toString.contains("test_id"));
        assertTrue(toString.contains("Test Title"));
        assertTrue(toString.contains("2025-04-27"));
        assertTrue(toString.contains("https://example.com/image.jpg"));
    }
    
    @Test
    public void testWithRealWorldExample() {
        // Пример данных, как они могли бы приходить из NASA API
        NasaRecord record = new NasaRecord(
            "apod_2025-04-27",
            "Hubble Deep Field",
            "2025-04-27",
            "The Hubble Deep Field is one of the most iconic images captured by the Hubble Space Telescope.",
            "https://apod.nasa.gov/apod/image/hubble_deep_field.jpg",
            "image",
            "NASA, ESA, and the Hubble Heritage Team"
        );
        
        Map<String, Object> map = record.toMap();
        
        assertEquals("nasa", map.get("type"));
        assertEquals("apod_2025-04-27", map.get("id"));
        assertEquals("Hubble Deep Field", map.get("title"));
        assertTrue(((String)map.get("explanation")).contains("iconic images"));
        assertTrue(((String)map.get("url")).contains("apod.nasa.gov"));
    }
}
