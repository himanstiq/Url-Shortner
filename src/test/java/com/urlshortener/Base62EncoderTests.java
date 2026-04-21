package com.urlshortener;

import com.urlshortener.util.Base62Encoder;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTests {

    @Test
    void generateRandom_defaultLength_is6Chars() {
        String code = Base62Encoder.generateRandom();
        assertEquals(6, code.length(), "Default length should be 6");
    }

    @Test
    void generateRandom_customLength_respectsLength() {
        assertEquals(8,  Base62Encoder.generateRandom(8).length());
        assertEquals(12, Base62Encoder.generateRandom(12).length());
    }

    @Test
    void generateRandom_onlyBase62Chars() {
        String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        for (int i = 0; i < 1000; i++) {
            String code = Base62Encoder.generateRandom();
            for (char c : code.toCharArray()) {
                assertTrue(alphabet.indexOf(c) >= 0,
                        "Character '" + c + "' is not in Base62 alphabet");
            }
        }
    }

    @Test
    void generateRandom_highUniqueness() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 10_000; i++) codes.add(Base62Encoder.generateRandom());
        // With 56 billion possible codes, collision in 10k attempts is astronomically unlikely
        assertEquals(10_000, codes.size(), "Expected all 10,000 codes to be unique");
    }

    @Test
    void encode_zero_returnsSingleChar() {
        String result = Base62Encoder.encode(0);
        assertEquals("0", result);
    }

    @Test
    void encode_known_values() {
        assertEquals("1",  Base62Encoder.encode(1));
        assertEquals("a",  Base62Encoder.encode(36));  // index 36 in alphabet is 'a'
        assertEquals("10", Base62Encoder.encode(62));  // 62 in base-62 is "10"
    }

    @Test
    void generateRandom_illegalLength_throws() {
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.generateRandom(0));
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.generateRandom(-1));
    }

    @Test
    void encode_negative_throws() {
        assertThrows(IllegalArgumentException.class, () -> Base62Encoder.encode(-1));
    }
}
