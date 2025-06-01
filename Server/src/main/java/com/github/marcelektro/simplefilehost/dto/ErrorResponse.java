package com.github.marcelektro.simplefilehost.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private String errorType;
    private String errorMessage;


    public static ErrorResponse of(String errorType, String errorMessage) {
        return new ErrorResponse(errorType, errorMessage);
    }


    public static final ErrorResponse INTERNAL_SERVER_ERROR = ErrorResponse.of("INTERNAL_SERVER_ERROR", "An error occurred while processing your request");


}
