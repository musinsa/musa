package com.musinsa.sonar.musa.sensors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musinsa.sonar.musa.languages.SwiftLanguage;
import com.musinsa.sonar.musa.rules.MusaConventionRules;
import com.musinsa.sonar.musa.rules.SwiftRulesDefinition;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class SwiftLintSensor implements Sensor {

    private static final Logger LOG = LoggerFactory.getLogger(SwiftLintSensor.class);
    private static final String REPORT_PATH_KEY = "sonar.swift.swiftlint.reportPath";
    private static final String DEFAULT_REPORT_PATH = "swiftlint.json";

    private static final Set<String> SWIFTLINT_RULES = Set.of(
        "force_cast", "force_try", "weak_delegate",
        "cyclomatic_complexity", "file_length", "function_body_length",
        "line_length", "nesting", "type_body_length",
        "trailing_whitespace", "empty_enum_arguments",
        "unused_closure_parameter", "void_return"
    );

    private static final Set<String> MUSA_CONVENTION_RULES = Set.of(
        "reactor_import_order",
        "view_controller_suffix",
        "mark_section",
        "reactor_action_naming",
        "no_force_unwrap_iboutlet"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor
            .name("SwiftLint Sensor")
            .onlyOnLanguage(SwiftLanguage.KEY)
            .createIssuesForRuleRepositories(
                SwiftRulesDefinition.REPOSITORY_KEY,
                MusaConventionRules.REPOSITORY_KEY);
    }

    @Override
    public void execute(SensorContext context) {
        String reportPath = context.config()
            .get(REPORT_PATH_KEY)
            .orElse(DEFAULT_REPORT_PATH);

        File reportFile = new File(context.fileSystem().baseDir(), reportPath);
        if (!reportFile.exists()) {
            LOG.warn("SwiftLint report not found at: {}", reportFile.getAbsolutePath());
            return;
        }

        try {
            JsonNode issues = objectMapper.readTree(reportFile);
            if (!issues.isArray()) {
                LOG.error("SwiftLint report is not a JSON array: {}", reportFile.getAbsolutePath());
                return;
            }
            int imported = 0;
            for (JsonNode issue : issues) {
                if (importIssue(context, issue)) {
                    imported++;
                }
            }
            LOG.info("Imported {} SwiftLint issues from {}", imported, reportFile.getName());
        } catch (IOException e) {
            LOG.error("Failed to parse SwiftLint report: {}", e.getMessage());
        }
    }

    private boolean importIssue(SensorContext context, JsonNode issue) {
        String ruleId = issue.path("rule_id").asText(null);
        String filePath = issue.path("file").asText(null);
        String reason = issue.path("reason").asText("SwiftLint issue");
        int line = issue.path("line").asInt(1);

        if (ruleId == null || filePath == null) {
            return false;
        }

        String repoKey;
        if (MUSA_CONVENTION_RULES.contains(ruleId)) {
            repoKey = MusaConventionRules.REPOSITORY_KEY;
        } else if (SWIFTLINT_RULES.contains(ruleId)) {
            repoKey = SwiftRulesDefinition.REPOSITORY_KEY;
        } else {
            LOG.debug("Unknown rule '{}', skipping issue at {}:{}", ruleId, filePath, line);
            return false;
        }

        RuleKey ruleKey = RuleKey.of(repoKey, ruleId);
        FileSystem fs = context.fileSystem();
        InputFile inputFile = fs.inputFile(fs.predicates().hasAbsolutePath(filePath));

        if (inputFile == null) {
            LOG.debug("File not indexed, skipping issue: {}", filePath);
            return false;
        }

        try {
            NewIssue newIssue = context.newIssue().forRule(ruleKey);
            NewIssueLocation location = newIssue.newLocation()
                .on(inputFile)
                .at(inputFile.selectLine(Math.max(1, line)))
                .message(reason);
            newIssue.at(location).save();
            return true;
        } catch (Exception e) {
            LOG.warn("Failed to create issue for rule {} at {}:{} — {}", ruleId, filePath, line, e.getMessage());
            return false;
        }
    }
}
