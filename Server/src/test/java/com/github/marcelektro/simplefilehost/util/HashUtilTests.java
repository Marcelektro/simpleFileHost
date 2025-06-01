package com.github.marcelektro.simplefilehost.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HashUtilTests {

    @Test
    public void testGenerateSalt() {
        var salt = HashUtil.generateSalt();

        assertEquals(32, salt.length());
    }

    @Test
    public void testHashPassword() {
        var password = "testPassword";
        var salt = HashUtil.generateSalt();

        var hashedPassword = HashUtil.hashPassword(password, salt);

        System.out.println("salt: " + salt);
        System.out.println("hashedPassword: " + hashedPassword);

        assertNotNull(hashedPassword);
        assertNotEquals(password, hashedPassword);

        assertTrue(HashUtil.verifyPassword(
                password,
                hashedPassword, salt
        ));

        hashedPassword += "123";

        assertFalse(HashUtil.verifyPassword(
                password,
                hashedPassword, salt
        ));
    }


}
