package com.musinsa.sonar.musa.rules;

import java.io.IOException;
import java.io.InputStream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MusaConventionRules implements RulesDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(MusaConventionRules.class);

    public static final String REPOSITORY_KEY = "musa-convention";
    public static final String REPOSITORY_NAME = "Musinsa Convention";
    public static final String LANGUAGE_KEY = "swift";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void define(Context context) {
        NewRepository repository = context.createRepository(REPOSITORY_KEY, LANGUAGE_KEY)
            .setName(REPOSITORY_NAME);

        defineRuleFromResource(repository, "reactor_import_order", RuleType.CODE_SMELL, "musinsa", "convention", "reactor");
        defineRuleFromResource(repository, "view_controller_suffix", RuleType.CODE_SMELL, "musinsa", "convention", "naming");
        defineRuleFromResource(repository, "mark_section", RuleType.CODE_SMELL, "musinsa", "convention", "documentation");
        defineRuleFromResource(repository, "reactor_action_naming", RuleType.CODE_SMELL, "musinsa", "convention", "reactor", "naming");
        defineRuleFromResource(repository, "no_force_unwrap_iboutlet", RuleType.BUG, "musinsa", "convention", "iboutlet");

        repository.done();
    }

    private void defineRuleFromResource(NewRepository repository, String key, RuleType type, String... tags) {
        JsonNode ruleMetadata = loadRuleMetadata(key);
        if (ruleMetadata == null) {
            LOG.warn("Rule metadata not found for key: {}", key);
            return;
        }
        String name = ruleMetadata.path("name").asText(key);
        String description = ruleMetadata.path("description").asText("");
        String severity = ruleMetadata.path("severity").asText("MAJOR");

        NewRule rule = repository.createRule(key)
            .setName(name)
            .setHtmlDescription(description)
            .setSeverity(severity)
            .setType(type);

        for (String tag : tags) {
            rule.addTags(tag);
        }
    }

    private JsonNode loadRuleMetadata(String ruleKey) {
        String resourcePath = "/com/musinsa/sonar/musa/rules/" + ruleKey + ".json";
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                return null;
            }
            return MAPPER.readTree(is);
        } catch (IOException e) {
            LOG.error("Failed to load rule metadata for {}: {}", ruleKey, e.getMessage());
            return null;
        }
    }
}
