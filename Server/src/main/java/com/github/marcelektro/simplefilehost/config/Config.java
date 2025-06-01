package com.github.marcelektro.simplefilehost.config;


import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("ClassCanBeRecord")
@AllArgsConstructor
@EqualsAndHashCode
public class Config {

    public final boolean debugMode;

    public final String host;
    public final int port;

    public final String dataDirectory;

    public final String jwtSecretKey;

    public final List<String> corsAllowedOrigins;




    public static Config defaultConfig() {
        return new Config(
                false,
                "localhost",
                8080,
                "data",
                "replaceMe_" + UUID.randomUUID().toString().replace("-", ""),
                List.of("http://localhost:3000", "http://localhost:8080")
        );
    }

}
