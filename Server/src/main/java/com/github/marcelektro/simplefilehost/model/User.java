package com.github.marcelektro.simplefilehost.model;

import lombok.Data;

@Data
public class User {

    private String id;
    private String username;

    private String passwordHash;
    private String passwordSalt;

}
