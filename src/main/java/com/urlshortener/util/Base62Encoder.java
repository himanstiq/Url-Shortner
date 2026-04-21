package com.urlshortener.util;

import java.security.SecureRandom;

/**
 * Generates compact, URL-safe Base62 codes from a cryptographically secure random source.
 *
 * <p>Alphabet: {@code 0-9 A-Z a-z} (62 characters).
 * A 6-character code yields 62⁶ ≈ 56 billion unique values — plenty for most deployments.
 *
 * <p>Thread-safe: {@link SecureRandom} is internally synchronised.
 */
public final class Base62Encoder {

    private static final String ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /** Default code length. 6 chars → 56.8 billion combinations. */
    public static final int DEFAULT_LENGTH = 6;

    private static final SecureRandom RANDOM = new SecureRandom();

    private Base62Encoder() { /* utility class */ }

    /**
     * Generates a random Base62 string of {@link #DEFAULT_LENGTH} characters.
     */
    public static String generateRandom() {
        return generateRandom(DEFAULT_LENGTH);
    }

    /**
     * Generates a random Base62 string of the specified length.
     *
     * @param length desired length (must be &gt; 0)
     * @throws IllegalArgumentException if {@code length} is not positive
     */
    public static String generateRandom(int length) {
        if (length <= 0) throw new IllegalArgumentException("length must be positive");

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /**
     * Encodes a non-negative long into its Base62 representation.
     * Useful for sequential ID-based generation strategies.
     *
     * @param number non-negative value to encode
     * @throws IllegalArgumentException if {@code number} is negative
     */
    public static String encode(long number) {
        if (number < 0) throw new IllegalArgumentException("number must be non-negative");
        if (number == 0) return String.valueOf(ALPHABET.charAt(0));

        StringBuilder sb = new StringBuilder();
        while (number > 0) {
            sb.insert(0, ALPHABET.charAt((int) (number % 62)));
            number /= 62;
        }
        return sb.toString();
    }
}
