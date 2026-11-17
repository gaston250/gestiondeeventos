package com.mdp.eventos.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class AppConfig {
    private static final AppConfig INSTANCE = cargar();

    private final Properties properties;

    private AppConfig(Properties properties) {
        this.properties = properties;
    }

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    public String getDatabaseUrl() {
        return getRequired("db.url");
    }

    public String getDatabaseUser() {
        return getRequired("db.user");
    }

    public String getDatabasePassword() {
        return System.getProperty("db.password", properties.getProperty("db.password", ""));
    }

    private String getRequired(String key) {
        String value = System.getProperty(key, properties.getProperty(key));
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta la propiedad requerida: " + key);
        }
        return value.trim();
    }

    private static AppConfig cargar() {
        Properties properties = new Properties();
        InputStream input = AppConfig.class.getResourceAsStream("/config.properties");
        if (input == null) {
            try {
                input = Files.newInputStream(Path.of("src", "config.properties"));
            } catch (IOException exception) {
                throw new IllegalStateException(
                    "No se encontró config.properties en el classpath ni en src/config.properties.",
                    exception
                );
            }
        }
        try (InputStream configInput = input) {
            properties.load(configInput);
            return new AppConfig(properties);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer config.properties.", exception);
        }
    }
}
