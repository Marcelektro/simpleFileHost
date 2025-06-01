package com.github.marcelektro.simplefilehost.dto.sharing;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CreateShareLinkRequestDto {

    private String fileId;

    @Nullable
    private String password;

    @Nullable
    private LocalDateTime expiry;

}
