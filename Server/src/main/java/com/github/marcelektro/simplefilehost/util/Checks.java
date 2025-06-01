package com.github.marcelektro.simplefilehost.util;

import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class Checks {

    public static Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    /**
     * Checks if provided string is non-null and non-empty
     */
    public static boolean nonEmpty(@Nullable String s) {
        return s != null && !s.isEmpty();
    }

    /**
     * Checks if provided string is null or empty
     */
    public static boolean empty(@Nullable String s) {
        return s == null || s.isEmpty();
    }


    /**
     * Checks if provided object is non-null
     */
    public static boolean nonNull(@Nullable Object o) {
        return o != null;
    }

}
