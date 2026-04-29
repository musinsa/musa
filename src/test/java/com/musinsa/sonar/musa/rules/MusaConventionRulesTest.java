package com.musinsa.sonar.musa.rules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.server.rule.RulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class MusaConventionRulesTest {

    private RulesDefinition.Context context;
    private MusaConventionRules definition;

    @BeforeEach
    public void setUp() {
        context = new RulesDefinition.Context();
        definition = new MusaConventionRules();
    }

    @Test
    public void testDefineRepository() {
        definition.define(context);

        RulesDefinition.Repository repository = context.repository(MusaConventionRules.REPOSITORY_KEY);
        assertThat(repository).isNotNull();
        assertThat(repository.key()).isEqualTo("musa-convention");
        assertThat(repository.name()).isEqualTo("Musinsa Convention");
        assertThat(repository.language()).isEqualTo("swift");
    }

    @Test
    public void testLoadAllRules() {
        definition.define(context);

        RulesDefinition.Repository repository = context.repository(MusaConventionRules.REPOSITORY_KEY);
        assertThat(repository.rules()).hasSize(5);
    }

    @Test
    public void testReactorImportOrderRule() {
        definition.define(context);

        RulesDefinition.Repository repository = context.repository(MusaConventionRules.REPOSITORY_KEY);
        RulesDefinition.Rule rule = repository.rule("reactor_import_order");

        assertThat(rule).isNotNull();
        assertThat(rule.key()).isEqualTo("reactor_import_order");
        assertThat(rule.type().toString()).isEqualTo("CODE_SMELL");
        assertThat(rule.tags()).contains("musinsa", "convention", "reactor");
    }

    @Test
    public void testViewControllerSuffixRule() {
        definition.define(context);

        RulesDefinition.Repository repository = context.repository(MusaConventionRules.REPOSITORY_KEY);
        RulesDefinition.Rule rule = repository.rule("view_controller_suffix");

        assertThat(rule).isNotNull();
        assertThat(rule.key()).isEqualTo("view_controller_suffix");
        assertThat(rule.type().toString()).isEqualTo("CODE_SMELL");
        assertThat(rule.tags()).contains("musinsa", "convention", "naming");
    }

    @Test
    public void testNoForceUnwrapIboutletRule() {
        definition.define(context);

        RulesDefinition.Repository repository = context.repository(MusaConventionRules.REPOSITORY_KEY);
        RulesDefinition.Rule rule = repository.rule("no_force_unwrap_iboutlet");

        assertThat(rule).isNotNull();
        assertThat(rule.key()).isEqualTo("no_force_unwrap_iboutlet");
        assertThat(rule.type().toString()).isEqualTo("BUG");
        assertThat(rule.tags()).contains("musinsa", "convention", "iboutlet");
    }

    @Test
    public void testAllRulesHaveNames() {
        definition.define(context);

        RulesDefinition.Repository repository = context.repository(MusaConventionRules.REPOSITORY_KEY);
        for (RulesDefinition.Rule rule : repository.rules()) {
            assertThat(rule.name()).isNotEmpty();
        }
    }
}
