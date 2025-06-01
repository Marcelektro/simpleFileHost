package com.github.marcelektro.simplefilehost.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

public class HashUtil {

    private static final SecureRandom secureRandom = new SecureRandom();


    public static String generateSalt() {

        var salt = new byte[16];
        HashUtil.secureRandom.nextBytes(salt);

        return HexFormat.of().formatHex(salt);
    }

    public static String hashPassword(String password, String salt) {
        try {
            var digest = MessageDigest.getInstance("SHA-512");
            var combined = (password + salt).getBytes(StandardCharsets.UTF_8); // salt appended to password

            var hash = digest.digest(combined);


            return HexFormat.of().formatHex(hash);

        } catch (Exception e) {
            throw new RuntimeException("Error while hashing password", e);
        }
    }

    public static boolean verifyPassword(String plainPassword, String storedHash, String storedSalt) {
        String hashToVerify = hashPassword(plainPassword, storedSalt);

        return hashToVerify.equals(storedHash);
    }

}