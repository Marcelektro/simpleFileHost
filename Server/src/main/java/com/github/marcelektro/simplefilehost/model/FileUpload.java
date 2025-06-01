package com.github.marcelektro.simplefilehost.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileUpload {

    private String id; // unique identifier for the file upload

    private String userId; // who uploaded

    private String filename; // filename of the uploaded file including extension
    private long size; // size in bytes
    private LocalDateTime uploadDate;

    private String path; // filesystem path

}
