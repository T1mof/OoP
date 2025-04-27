package com.example.apipoller.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
        assertThrows(IllegalArgumentException.class, () -> AppConfig.fromArgs(new String[]{}));
        assertThrows(IllegalArgumentException.class, () -> AppConfig.fromArgs(new String[]{"0", "10", "json", "news"}));
        assertThrows(IllegalArgumentException.class, () -> AppConfig.fromArgs(new String[]{"3", "0", "json", "news"}));
        assertThrows(IllegalArgumentException.class, () -> AppConfig.fromArgs(new String[]{"3", "10", "xml", "news"}));
        assertThrows(IllegalArgumentException.class, () -> AppConfig.fromArgs(new String[]{"3", "10", "json"}));
    }
}
