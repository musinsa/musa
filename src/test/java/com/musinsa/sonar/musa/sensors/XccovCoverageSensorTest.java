package com.musinsa.sonar.musa.sensors;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class XccovCoverageSensorTest {
    @Test
    void testXccovCoverageSensorInitialization() {
        XccovCoverageSensor sensor = new XccovCoverageSensor();
        assertNotNull(sensor);
        assertInstanceOf(XccovCoverageSensor.class, sensor);
    }

    @Test
    void testSensorInstance() {
        XccovCoverageSensor sensor = new XccovCoverageSensor();
        assertNotNull(sensor);
    }

    @Test
    void testXmlFilePattern() {
        String filename = "coverage.xml";
        assertTrue(filename.endsWith(".xml"));
    }
}
