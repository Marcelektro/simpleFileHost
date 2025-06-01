package com.github.marcelektro.simplefilehost.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@Slf4j
public class ConfigManager {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    private final File file;
    private Config config;


    public ConfigManager(File configFile) {
        this.file = configFile;
        log.debug("Initializing ConfigManager with file: {}", configFile.getAbsolutePath());
    }


    public void initConfig() {
        if (!this.file.exists()) {
            log.info("Config file does not exist, creating default config: {}", this.file.getAbsolutePath());
            this.config = Config.defaultConfig();
            saveConfig();
            return;
        }

        log.debug("Loading config file: {}", this.file);
        try {
            this.config = GSON.fromJson(new FileReader(this.file), Config.class);

        } catch (IOException e) {
            log.error("Error reading config file: {}", this.file.getAbsolutePath(), e);
            throw new RuntimeException("Failed to read config file", e);
        }
    }


    public void saveConfig() {
        log.debug("Saving config file: {}", this.file.getAbsolutePath());

        if (this.file.getParentFile().mkdirs()) {
            log.info("Created parent directories for config {}", this.file.getAbsolutePath());
        }

         try (var writer = new FileWriter(this.file)) {
             GSON.toJson(this.config, writer);

         } catch (IOException e) {
             log.error("Error saving config file: {}", this.file.getAbsolutePath(), e);
             throw new RuntimeException("Failed to save config file", e);
         }
    }


    public Config get() {
        if (this.config == null)
            throw new IllegalStateException("Config has not been initialized yet!");
        return this.config;
    }

}
