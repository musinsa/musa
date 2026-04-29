package com.musinsa.sonar.musa.rules;

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

public class SwiftQualityProfile implements BuiltInQualityProfilesDefinition {

    public static final String PROFILE_NAME = "Musinsa Swift";

    @Override
    public void define(Context context) {
        NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(
            PROFILE_NAME, "swift");
        profile.setDefault(true);

        // SwiftLint rules
        activateRule(profile, SwiftRulesDefinition.REPOSITORY_KEY, "force_cast");
        activateRule(profile, SwiftRulesDefinition.REPOSITORY_KEY, "force_try");
        activateRule(profile, SwiftRulesDefinition.REPOSITORY_KEY, "cyclomatic_complexity");
        activateRule(profile, SwiftRulesDefinition.REPOSITORY_KEY, "file_length");
        activateRule(profile, SwiftRulesDefinition.REPOSITORY_KEY, "function_body_length");
        activateRule(profile, SwiftRulesDefinition.REPOSITORY_KEY, "line_length");
        activateRule(profile, SwiftRulesDefinition.REPOSITORY_KEY, "nesting");
        activateRule(profile, SwiftRulesDefinition.REPOSITORY_KEY, "type_body_length");
        activateRule(profile, SwiftRulesDefinition.REPOSITORY_KEY, "trailing_whitespace");
        activateRule(profile, SwiftRulesDefinition.REPOSITORY_KEY, "empty_enum_arguments");
        activateRule(profile, SwiftRulesDefinition.REPOSITORY_KEY, "unused_closure_parameter");
        activateRule(profile, SwiftRulesDefinition.REPOSITORY_KEY, "void_return");

        // Musinsa convention rules
        activateRule(profile, MusaConventionRules.REPOSITORY_KEY, "reactor_import_order");
        activateRule(profile, MusaConventionRules.REPOSITORY_KEY, "view_controller_suffix");
        activateRule(profile, MusaConventionRules.REPOSITORY_KEY, "mark_section");
        activateRule(profile, MusaConventionRules.REPOSITORY_KEY, "reactor_action_naming");
        activateRule(profile, MusaConventionRules.REPOSITORY_KEY, "no_force_unwrap_iboutlet");

        profile.done();
    }

    private void activateRule(NewBuiltInQualityProfile profile, String repoKey, String ruleKey) {
        profile.activateRule(repoKey, ruleKey);
    }
}
