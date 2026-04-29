package com.musinsa.sonar.musa.rules;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

public class MusaConventionRules implements RulesDefinition {

    public static final String REPOSITORY_KEY = "musa-convention";
    public static final String REPOSITORY_NAME = "Musinsa Convention";
    public static final String LANGUAGE_KEY = "swift";

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
        JsonObject ruleMetadata = loadRuleMetadata(key);
        if (ruleMetadata == null) {
            return;
        }
        String name = ruleMetadata.has("name") ? ruleMetadata.get("name").getAsString() : key;
        String description = ruleMetadata.has("description") ? ruleMetadata.get("description").getAsString() : "";
        String severity = ruleMetadata.has("severity") ? ruleMetadata.get("severity").getAsString() : "MAJOR";

        NewRule rule = repository.createRule(key)
            .setName(name)
            .setHtmlDescription(description)
            .setSeverity(severity)
            .setType(type);

        for (String tag : tags) {
            rule.addTags(tag);
        }
    }

    private JsonObject loadRuleMetadata(String ruleKey) {
        String resourcePath = "/com/musinsa/sonar/musa/rules/" + ruleKey + ".json";
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                return null;
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return new Gson().fromJson(json, JsonObject.class);
        } catch (IOException e) {
            return null;
        }
    }
}
