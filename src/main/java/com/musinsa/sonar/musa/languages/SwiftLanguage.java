package com.musinsa.sonar.musa.languages;

import org.sonar.api.resources.Language;

public class SwiftLanguage implements Language {

    public static final String KEY = "swift";
    public static final String NAME = "Swift";

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String[] getFileSuffixes() {
        return new String[]{"swift"};
    }

    @Override
    public boolean publishAllFiles() {
        return true;
    }
}
