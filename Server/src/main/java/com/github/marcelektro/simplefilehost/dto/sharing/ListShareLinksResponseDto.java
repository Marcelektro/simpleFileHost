package com.github.marcelektro.simplefilehost.dto.sharing;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ListShareLinksResponseDto {

    private String fileId;
    private List<ShareLinkDto> links;

}
