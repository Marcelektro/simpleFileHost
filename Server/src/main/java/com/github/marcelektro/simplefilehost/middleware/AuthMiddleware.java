package com.github.marcelektro.simplefilehost.middleware;

import com.github.marcelektro.simplefilehost.dto.ErrorResponse;
import com.github.marcelektro.simplefilehost.service.auth.AuthService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.UnauthorizedResponse;

public class AuthMiddleware {

    private final AuthService authService;

    public AuthMiddleware(AuthService authService) {
        this.authService = authService;
    }


    public void validateAuthToken(Context ctx) throws Exception {
        String token = ctx.header("Authorization");

        if (token == null || token.isEmpty()) {
            ctx.status(HttpStatus.UNAUTHORIZED).json(ErrorResponse.of("UNAUTHORIZED", "Authorization token is missing"));
            throw new UnauthorizedResponse("Authorization token is missing");
        }

        var userId = this.authService.validateTokenAndGetUserId(token);

        if (!userId.isSuccess()) {
            switch (userId.getErrorCode()) {
                case "EXPIRED_TOKEN" ->
                        ctx.status(HttpStatus.UNAUTHORIZED).json(ErrorResponse.of("EXPIRED_TOKEN", "The token has expired and must be refreshed"));
                case "TOKEN_VALIDATION_FAILURE" ->
                        ctx.status(HttpStatus.UNAUTHORIZED).json(ErrorResponse.of("INVALID_TOKEN", "The token is invalid or malformed"));

                default -> ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .json(ErrorResponse.of("INTERNAL_ERROR", "An unexpected validation error occurred: " + userId.getMessage()));
            }

            throw new UnauthorizedResponse("Token validation failed: " + userId.getErrorCode());
        }

        // If token is valid, store userId in context for later use
        ctx.attribute("userId", String.valueOf(userId.getData()));

    }

}
