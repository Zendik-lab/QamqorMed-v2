package com.qamqormed.util;

import java.util.regex.Pattern;

public final class PhoneNormalizer {

    private static final Pattern NORMALIZED_PHONE = Pattern.compile("\\+7\\d{10}");

    private PhoneNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Некорректный номер телефона");
        }

        String digits = value.replaceAll("\\D", "");
        if (digits.startsWith("8") && digits.length() == 11) {
            digits = "7" + digits.substring(1);
        } else if (digits.length() == 10) {
            digits = "7" + digits;
        }

        String normalized = "+" + digits;
        if (!NORMALIZED_PHONE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Некорректный номер телефона");
        }
        return normalized;
    }
}
