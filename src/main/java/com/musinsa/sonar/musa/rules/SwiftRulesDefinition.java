package com.musinsa.sonar.musa.rules;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

public class SwiftRulesDefinition implements RulesDefinition {

    public static final String REPOSITORY_KEY = "swiftlint";
    public static final String REPOSITORY_NAME = "SwiftLint";
    public static final String LANGUAGE_KEY = "swift";

    @Override
    public void define(Context context) {
        NewRepository repository = context.createRepository(REPOSITORY_KEY, LANGUAGE_KEY)
            .setName(REPOSITORY_NAME);

        defineRuleFromResource(repository, "force_cast", RuleType.BUG);
        defineRuleFromResource(repository, "force_try", RuleType.BUG);
        defineRuleFromResource(repository, "cyclomatic_complexity", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "file_length", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "function_body_length", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "line_length", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "nesting", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "type_body_length", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "trailing_whitespace", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "empty_enum_arguments", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "unused_closure_parameter", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "void_return", RuleType.CODE_SMELL);

        repository.done();
    }

    private void defineRuleFromResource(NewRepository repository, String key, RuleType type) {
        JsonObject ruleMetadata = loadRuleMetadata(key);
        if (ruleMetadata == null) {
            return;
        }
        String name = ruleMetadata.has("name") ? ruleMetadata.get("name").getAsString() : key;
        String description = ruleMetadata.has("description") ? ruleMetadata.get("description").getAsString() : "";
        String severity = ruleMetadata.has("severity") ? ruleMetadata.get("severity").getAsString() : "MAJOR";

        repository.createRule(key)
            .setName(name)
            .setHtmlDescription(description)
            .setSeverity(severity)
            .setType(type)
            .addTags("swiftlint", "swift");
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
