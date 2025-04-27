package com.example.apipoller.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Модель данных для записи о погоде
 */
public class WeatherRecord implements ApiRecord {
    private final String city;
    private final double temperature;
    private final double windSpeed;
    private final int humidity;
    private final String condition;
    private final long timestamp;

    public WeatherRecord(String city, double temperature, double windSpeed, 
                        int humidity, String condition, long timestamp) {
        this.city = city != null ? city : "";
        this.temperature = temperature;
        this.windSpeed = windSpeed;
        this.humidity = humidity;
        this.condition = condition != null ? condition : "";
        this.timestamp = timestamp;
    }

    @Override
    public String getId() {
        return city + "_" + timestamp;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "weather");
        map.put("city", city);
        map.put("temperature", temperature);
        map.put("windSpeed", windSpeed);
        map.put("humidity", humidity);
        map.put("condition", condition);
        map.put("timestamp", timestamp);
        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WeatherRecord that = (WeatherRecord) o;
        return timestamp == that.timestamp && Objects.equals(city, that.city);
    }

    @Override
    public int hashCode() {
        return Objects.hash(city, timestamp);
    }

    @Override
    public String toString() {
        return "WeatherRecord{" +
                "city='" + city + '\'' +
                ", temperature=" + temperature +
                ", condition='" + condition + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
