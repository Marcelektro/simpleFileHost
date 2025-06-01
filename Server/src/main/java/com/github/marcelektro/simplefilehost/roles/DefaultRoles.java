package com.github.marcelektro.simplefilehost.roles;

import io.javalin.security.RouteRole;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public class DefaultRoles implements RouteRole {

    public static final DefaultRoles ANONYMOUS = new DefaultRoles("ANONYMOUS");
    public static final DefaultRoles USER = new DefaultRoles("USER");
    public static final DefaultRoles ADMIN = new DefaultRoles("ADMIN");

    private final String name;

}
