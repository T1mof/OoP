package com.example.apipoller.model;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class WeatherRecordTest {

    @Test
    public void testConstructorAndGetters() {
        WeatherRecord record = new WeatherRecord(
            "London",
            15.5,
            5.2,
            80,
            "Cloudy",
            1714200000
        );
        
        assertEquals("London_1714200000", record.getId());
        
        Map<String, Object> map = record.toMap();
        assertEquals("weather", map.get("type"));
        assertEquals("London", map.get("city"));
        assertEquals(15.5, map.get("temperature"));
        assertEquals(5.2, map.get("windSpeed"));
        assertEquals(80, map.get("humidity"));
        assertEquals("Cloudy", map.get("condition"));
        assertEquals(1714200000L, map.get("timestamp"));
    }
    
    @Test
    public void testNullValues() {
        WeatherRecord record = new WeatherRecord(
            null,
            15.5,
            5.2,
            80,
            null,
            1714200000
        );
        
        assertEquals("_1714200000", record.getId());
        
        Map<String, Object> map = record.toMap();
        assertEquals("weather", map.get("type"));
        assertEquals("", map.get("city"));
        assertEquals(15.5, map.get("temperature"));
        assertEquals(5.2, map.get("windSpeed"));
        assertEquals(80, map.get("humidity"));
        assertEquals("", map.get("condition"));
        assertEquals(1714200000L, map.get("timestamp"));
    }
    
    @Test
    public void testEqualsAndHashCode() {
        WeatherRecord record1 = new WeatherRecord(
            "London", 15.5, 5.2, 80, "Cloudy", 1714200000
        );
        
        WeatherRecord record2 = new WeatherRecord(
            "London", 20.0, 10.0, 60, "Sunny", 1714200000
        );
        
        WeatherRecord record3 = new WeatherRecord(
            "London", 15.5, 5.2, 80, "Cloudy", 1714300000
        );
        
        WeatherRecord record4 = new WeatherRecord(
            "Paris", 15.5, 5.2, 80, "Cloudy", 1714200000
        );
        
        // Записи с одинаковым городом и временем должны быть равны
        assertEquals(record1, record2);
        assertEquals(record1.hashCode(), record2.hashCode());
        
        // Записи с разным временем не должны быть равны
        assertNotEquals(record1, record3);
        assertNotEquals(record1.hashCode(), record3.hashCode());
        
        // Записи с разными городами не должны быть равны
        assertNotEquals(record1, record4);
        assertNotEquals(record1.hashCode(), record4.hashCode());
        
        // Запись не равна null или объекту другого класса
        assertNotEquals(record1, null);
        assertNotEquals(record1, "не запись");
    }
    
    @Test
    public void testToString() {
        WeatherRecord record = new WeatherRecord(
            "London", 15.5, 5.2, 80, "Cloudy", 1714200000
        );
        
        String toString = record.toString();
        assertTrue(toString.contains("London"));
        assertTrue(toString.contains("15.5"));
        assertTrue(toString.contains("Cloudy"));
    }
}
