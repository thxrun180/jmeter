package org.example;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;

public class ChecksumDependencyStubPlugin implements Plugin<Settings> {
    @Override
    public void apply(Settings settings) {
        // No-op stub to satisfy plugin dependency when offline.
    }
}
