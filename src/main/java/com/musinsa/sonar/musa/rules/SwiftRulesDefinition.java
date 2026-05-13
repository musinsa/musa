package com.musinsa.sonar.musa.rules;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwiftRulesDefinition implements RulesDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(SwiftRulesDefinition.class);

    public static final String REPOSITORY_KEY = "swiftlint";
    public static final String REPOSITORY_NAME = "SwiftLint";
    public static final String LANGUAGE_KEY = "swift";

    public static final Set<String> RULE_KEYS = Set.of(
        "force_cast", "force_try", "weak_delegate",
        "cyclomatic_complexity", "file_length", "function_body_length",
        "line_length", "nesting", "type_body_length",
        "trailing_whitespace", "empty_enum_arguments",
        "unused_closure_parameter", "void_return",
        "vertical_whitespace", "trailing_comma", "identifier_name",
        "type_name", "todo", "function_parameter_count",
        "large_tuple", "trailing_newline",
        "superfluous_disable_command", "orphaned_doc_comment",
        "static_over_final_class", "redundant_void_return",
        "unneeded_synthesized_initializer", "return_arrow_whitespace",
        "unused_optional_binding", "optional_data_string_conversion",
        "class_delegate_protocol", "blanket_disable_command"
    );

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void define(Context context) {
        NewRepository repository = context.createRepository(REPOSITORY_KEY, LANGUAGE_KEY)
            .setName(REPOSITORY_NAME);

        defineRuleFromResource(repository, "force_cast", RuleType.BUG);
        defineRuleFromResource(repository, "force_try", RuleType.BUG);
        defineRuleFromResource(repository, "weak_delegate", RuleType.BUG);
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
        defineRuleFromResource(repository, "vertical_whitespace", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "trailing_comma", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "identifier_name", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "type_name", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "todo", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "function_parameter_count", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "large_tuple", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "trailing_newline", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "superfluous_disable_command", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "orphaned_doc_comment", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "static_over_final_class", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "redundant_void_return", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "unneeded_synthesized_initializer", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "return_arrow_whitespace", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "unused_optional_binding", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "optional_data_string_conversion", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "class_delegate_protocol", RuleType.CODE_SMELL);
        defineRuleFromResource(repository, "blanket_disable_command", RuleType.CODE_SMELL);

        repository.done();
    }

    private void defineRuleFromResource(NewRepository repository, String key, RuleType type) {
        JsonNode ruleMetadata = loadRuleMetadata(key);
        if (ruleMetadata == null) {
            LOG.warn("Rule metadata not found for key: {}", key);
            return;
        }
        String name = ruleMetadata.path("name").asText(key);
        String description = ruleMetadata.path("description").asText("");
        String severity = ruleMetadata.path("severity").asText("MAJOR");

        repository.createRule(key)
            .setName(name)
            .setHtmlDescription(description)
            .setSeverity(severity)
            .setType(type)
            .addTags("swiftlint", "swift");
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
