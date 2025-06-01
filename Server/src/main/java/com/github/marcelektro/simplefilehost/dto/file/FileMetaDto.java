package com.github.marcelektro.simplefilehost.dto.file;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class FileMetaDto {

    private String fileId;
    private String filename;
    private long size;
    private LocalDateTime uploadedAt;
    private int sharedLinksCount;

}
