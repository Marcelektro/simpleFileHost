package com.github.marcelektro.simplefilehost.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class LoginResponseDto {

    private int userId;
    private String username;
    private String token;

}
