package com.github.marcelektro.simplefilehost.dto.sharing;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ShareLinkDto {

    private String shareLinkId;
    private LocalDateTime expiry;
    private String password;

}
