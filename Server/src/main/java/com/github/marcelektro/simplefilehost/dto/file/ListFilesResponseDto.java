package com.github.marcelektro.simplefilehost.dto.file;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ListFilesResponseDto {

    private List<FileMetaDto> files;

}
