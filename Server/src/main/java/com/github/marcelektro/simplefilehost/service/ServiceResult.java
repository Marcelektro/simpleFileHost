package com.github.marcelektro.simplefilehost.service;

import lombok.Getter;

@Getter
public class ServiceResult<T> {

    private boolean success;
    private T data; // Optional, only present if success is true
    private String errorCode; // Optional, only present if success is false
    private String message; // Optional, only present if success is false


    public static <T> ServiceResult<T> success(T data) {
        var result = new ServiceResult<T>();
        result.success = true;
        result.data = data;
        return result;
    }

    public static <T> ServiceResult<T> failure(String errorCode, String message) {
        var result = new ServiceResult<T>();
        result.success = false;
        result.errorCode = errorCode;
        result.message = message;
        return result;
    }

}

