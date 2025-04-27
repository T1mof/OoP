package com.example.apipoller.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Модель данных для новостной записи
 */
public class NewsRecord implements ApiRecord {
    private final String title;
    private final String description;
    private final String url;
    private final String source;
    private final String publishedAt;
    private final String author;

    public NewsRecord(String title, String description, String url, 
                     String source, String publishedAt, String author) {
        this.title = title != null ? title : "";
        this.description = description != null ? description : "";
        this.url = url != null ? url : "";
        this.source = source != null ? source : "";
        this.publishedAt = publishedAt != null ? publishedAt : "";
        this.author = author != null ? author : "";
    }

    @Override
    public String getId() {
        return url;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "news");
        map.put("title", title);
        map.put("description", description);
        map.put("url", url);
        map.put("source", source);
        map.put("publishedAt", publishedAt);
        map.put("author", author);
        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NewsRecord that = (NewsRecord) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public String toString() {
        return "NewsRecord{" +
                "title='" + title + '\'' +
                ", source='" + source + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
