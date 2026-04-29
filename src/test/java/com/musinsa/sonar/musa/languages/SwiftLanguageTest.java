package com.musinsa.sonar.musa.languages;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwiftLanguageTest {
    private SwiftLanguage language = new SwiftLanguage();

    @Test
    void testLanguageKey() {
        assertEquals("swift", language.getKey());
    }

    @Test
    void testLanguageName() {
        assertEquals("Swift", language.getName());
    }

    @Test
    void testFileSuffixes() {
        String[] suffixes = language.getFileSuffixes();
        assertNotNull(suffixes);
        assertEquals(1, suffixes.length);
        assertEquals("swift", suffixes[0]);
    }
}
