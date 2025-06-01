package com.github.marcelektro.simplefilehost.service.auth;

import com.github.marcelektro.simplefilehost.service.ServiceResult;

public interface AuthService {

    ServiceResult<String> registerUser(String id, String username, String password) throws Exception;
    ServiceResult<AuthResult> login(String username, String password) throws Exception;
    ServiceResult<Integer> validateTokenAndGetUserId(String token) throws Exception;



    record AuthResult(int userId, String token, String username) {}

}
