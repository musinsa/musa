package com.musinsa.sonar.musa.rules;

import org.junit.jupiter.api.Test;
import org.sonar.api.server.rule.RulesDefinition;
import static org.junit.jupiter.api.Assertions.*;

class SwiftRulesDefinitionTest {
    @Test
    void testSwiftLintRulesLoaded() {
        RulesDefinition.Context context = new RulesDefinition.Context();
        new SwiftRulesDefinition().define(context);

        RulesDefinition.Repository repo = context.repository("swiftlint");
        assertNotNull(repo);
        assertEquals(13, repo.rules().size());
    }

    @Test
    void testRuleSeverityAndType() {
        RulesDefinition.Context context = new RulesDefinition.Context();
        new SwiftRulesDefinition().define(context);

        RulesDefinition.Repository repo = context.repository("swiftlint");
        RulesDefinition.Rule rule = repo.rule("force_try");

        assertNotNull(rule);
        assertNotNull(rule.severity());
        assertEquals("BUG", rule.type().toString());
    }

    @Test
    void testMusaConventionRulesLoaded() {
        RulesDefinition.Context context = new RulesDefinition.Context();
        new MusaConventionRules().define(context);

        RulesDefinition.Repository repo = context.repository("musa-convention");
        assertNotNull(repo);
        assertEquals(5, repo.rules().size());
    }
}
