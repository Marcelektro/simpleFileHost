package com.github.marcelektro.simplefilehost.controller.auth;

import com.github.marcelektro.simplefilehost.dto.ErrorResponse;
import com.github.marcelektro.simplefilehost.dto.auth.LoginRequestDto;
import com.github.marcelektro.simplefilehost.dto.auth.LoginResponseDto;
import com.github.marcelektro.simplefilehost.dto.auth.MeResponseDto;
import com.github.marcelektro.simplefilehost.service.auth.AuthService;
import com.github.marcelektro.simplefilehost.util.Checks;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    public void handleLogin(Context ctx) throws Exception {

        var req = ctx.bodyValidator(LoginRequestDto.class)
                .check(l -> Checks.nonEmpty(l.getUsername()), "Username must not be empty")
                .check(l -> Checks.nonEmpty(l.getPassword()), "Password must not be empty")
                .get();

        var res = this.authService.login(req.getUsername(), req.getPassword());

        if (!res.isSuccess()) {

            switch (res.getErrorCode()) {
                case "INVALID_CREDENTIALS" -> ctx.status(HttpStatus.UNAUTHORIZED).json(ErrorResponse.of("UNAUTHORIZED", "Invalid username or password"));

                default -> ctx.status(HttpStatus.BAD_REQUEST).json(ErrorResponse.of(res.getErrorCode(), res.getMessage()));
            }

            return;
        }

        ctx.json(new LoginResponseDto(
                res.getData().userId(),
                res.getData().username(),
                res.getData().token()
        ));

    }


    public void handleMe(Context ctx) throws Exception {

        String token = ctx.header("Authorization");

        if (Checks.empty(token)) {
            ctx.status(HttpStatus.UNAUTHORIZED).json(ErrorResponse.of("UNAUTHORIZED", "Authorization token is missing"));
            return;
        }

        var userRes = this.authService.validateTokenAndGetUserId(token);
        if (!userRes.isSuccess()) {

            switch (userRes.getErrorCode()) {
                case "EXPIRED_TOKEN" -> ctx.status(HttpStatus.UNAUTHORIZED).json(ErrorResponse.of("EXPIRED_TOKEN", "The token has expired and must be refreshed"));
                case "TOKEN_VALIDATION_FAILURE" -> ctx.status(HttpStatus.NOT_FOUND).json(ErrorResponse.of("INVALID_TOKEN_PROVIDED", "Provided token is invalid"));

                default -> ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(ErrorResponse.of("INTERNAL_ERROR", "An unexpected validation error occurred: " + userRes.getMessage()));
            }

            return;
        }

        ctx.json(new MeResponseDto(userRes.getData()));
    }

}
