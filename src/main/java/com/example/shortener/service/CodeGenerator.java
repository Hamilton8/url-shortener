package com.example.shortener.service;

import java.util.Random;

public final class CodeGenerator {
    static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private CodeGenerator() {}

    public static String generate(Random random, int length) {
        if (length <= 0) throw new IllegalArgumentException("length must be positive");
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return sb.toString();
    }
}
