package com.musinsa.sonar.musa.sensors;

import com.musinsa.sonar.musa.languages.SwiftLanguage;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class XccovCoverageSensor implements Sensor {

    private static final Logger LOG = LoggerFactory.getLogger(XccovCoverageSensor.class);
    private static final String REPORT_PATH_KEY = "sonar.swift.coverage.reportPath";
    private static final String DEFAULT_REPORT_PATH = "coverage.xml";

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor
            .name("Xccov Coverage Sensor")
            .onlyOnLanguage(SwiftLanguage.KEY);
    }

    @Override
    public void execute(SensorContext context) {
        String reportPath = context.config()
            .get(REPORT_PATH_KEY)
            .orElse(DEFAULT_REPORT_PATH);

        File reportFile = new File(context.fileSystem().baseDir(), reportPath);
        if (!reportFile.exists()) {
            LOG.warn("Coverage report not found at: {}", reportFile.getAbsolutePath());
            return;
        }

        try {
            Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(reportFile);
            doc.getDocumentElement().normalize();

            NodeList fileNodes = doc.getElementsByTagName("file");
            int filesImported = 0;

            for (int i = 0; i < fileNodes.getLength(); i++) {
                Element fileElement = (Element) fileNodes.item(i);
                String filePath = fileElement.getAttribute("path");

                FileSystem fs = context.fileSystem();
                InputFile inputFile = fs.inputFile(fs.predicates().hasAbsolutePath(filePath));

                if (inputFile == null) {
                    LOG.debug("File not indexed, skipping coverage: {}", filePath);
                    continue;
                }

                NewCoverage coverage = context.newCoverage().onFile(inputFile);
                NodeList lines = fileElement.getElementsByTagName("lineToCover");

                for (int j = 0; j < lines.getLength(); j++) {
                    Element lineEl = (Element) lines.item(j);
                    int lineNumber = Integer.parseInt(lineEl.getAttribute("lineNumber"));
                    boolean covered = Boolean.parseBoolean(lineEl.getAttribute("covered"));
                    int hits = covered ? 1 : 0;
                    coverage.lineHits(lineNumber, hits);
                }

                coverage.save();
                filesImported++;
            }

            LOG.info("Imported coverage for {} files from {}", filesImported, reportFile.getName());
        } catch (Exception e) {
            LOG.error("Failed to parse coverage report: {}", e.getMessage());
        }
    }
}
