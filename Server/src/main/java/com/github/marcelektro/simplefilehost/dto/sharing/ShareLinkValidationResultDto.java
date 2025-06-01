package com.github.marcelektro.simplefilehost.dto.sharing;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ShareLinkValidationResultDto {

    private String linkId;

    private String fileId;
    private String filename;
    private long fileSize;

    private boolean hasPassword;
    private boolean validPassword;
    private LocalDateTime expiry;
    private boolean hasExpired;

}
