package com.github.marcelektro.simplefilehost.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SharedLink {

    private String id;
    private String fileId;
    private LocalDateTime expiry;
    private String password;

}
