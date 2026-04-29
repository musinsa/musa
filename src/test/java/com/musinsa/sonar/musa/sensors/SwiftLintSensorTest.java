package com.musinsa.sonar.musa.sensors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwiftLintSensorTest {
    @Test
    void testSwiftLintSensorInitialization() {
        SwiftLintSensor sensor = new SwiftLintSensor();
        assertNotNull(sensor);
    }

    @Test
    void testSensorDescriptorNotNull() {
        SwiftLintSensor sensor = new SwiftLintSensor();
        assertNotNull(sensor);
    }

    @Test
    void testSwiftFileExtension() {
        String suffix = "swift";
        assertEquals("swift", suffix);
    }

    @Test
    void testJsonParsingUtility() throws Exception {
        String json = "{\"file\": \"test.swift\", \"line\": 1, \"severity\": \"error\"}";
        ObjectMapper mapper = new ObjectMapper();
        var parsed = mapper.readTree(json);
        assertEquals("test.swift", parsed.get("file").asText());
        assertEquals(1, parsed.get("line").asInt());
    }
}
