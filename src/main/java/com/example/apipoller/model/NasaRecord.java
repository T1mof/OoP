package com.example.apipoller.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Модель данных для записей NASA API
 */
public class NasaRecord implements ApiRecord {
    private final String id;
    private final String title;
    private final String date;
    private final String explanation;
    private final String url;
    private final String mediaType;
    private final String copyright;

    public NasaRecord(String id, String title, String date, 
                     String explanation, String url, 
                     String mediaType, String copyright) {
        this.id = id != null ? id : "";
        this.title = title != null ? title : "";
        this.date = date != null ? date : "";
        this.explanation = explanation != null ? explanation : "";
        this.url = url != null ? url : "";
        this.mediaType = mediaType != null ? mediaType : "";
        this.copyright = copyright != null ? copyright : "";
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "nasa");
        map.put("id", id);
        map.put("title", title);
        map.put("date", date);
        map.put("explanation", explanation);
        map.put("url", url);
        map.put("mediaType", mediaType);
        map.put("copyright", copyright);
        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NasaRecord that = (NasaRecord) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "NasaRecord{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", date='" + date + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
