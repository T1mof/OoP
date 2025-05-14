package com.example.apipoller.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.example.apipoller.config.AppConfig.ConfigurationException;

public class AppConfigTest {

    @Test
    public void testValidArgs() {
        String[] args = {"3", "10", "json", "news", "weather"};
        AppConfig config = AppConfig.fromArgs(args);
        assertEquals(3, config.getMaxThreads());
        assertEquals(10, config.getTimeoutSeconds());
        assertEquals("json", config.getOutputFormat());
        assertTrue(config.getServices().contains("news"));
        assertTrue(config.getServices().contains("weather"));
    }

    @Test
    public void testInvalidArgs() {
        // Заменяем IllegalArgumentException на ConfigurationException
        assertThrows(ConfigurationException.class, () -> AppConfig.fromArgs(new String[]{}));
        assertThrows(ConfigurationException.class, () -> AppConfig.fromArgs(new String[]{"0", "10", "json", "news"}));
        assertThrows(ConfigurationException.class, () -> AppConfig.fromArgs(new String[]{"3", "0", "json", "news"}));
        assertThrows(ConfigurationException.class, () -> AppConfig.fromArgs(new String[]{"3", "10", "xml", "news"}));
        assertThrows(ConfigurationException.class, () -> AppConfig.fromArgs(new String[]{"3", "10", "json"}));
    }
}
