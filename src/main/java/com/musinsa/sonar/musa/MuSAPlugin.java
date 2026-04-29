package com.musinsa.sonar.musa;

import org.sonar.api.Plugin;
import com.musinsa.sonar.musa.languages.SwiftLanguage;
import com.musinsa.sonar.musa.rules.SwiftRulesDefinition;
import com.musinsa.sonar.musa.rules.MusaConventionRules;
import com.musinsa.sonar.musa.rules.SwiftQualityProfile;
import com.musinsa.sonar.musa.sensors.SwiftLintSensor;
import com.musinsa.sonar.musa.sensors.XccovCoverageSensor;

public class MuSAPlugin implements Plugin {

    @Override
    public void define(Context context) {
        context.addExtensions(
            SwiftLanguage.class,
            SwiftRulesDefinition.class,
            MusaConventionRules.class,
            SwiftQualityProfile.class,
            SwiftLintSensor.class,
            XccovCoverageSensor.class
        );
    }
}
