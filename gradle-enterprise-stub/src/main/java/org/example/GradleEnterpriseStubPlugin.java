package org.example;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;

public class GradleEnterpriseStubPlugin implements Plugin<Settings> {
    @Override
    public void apply(Settings settings) {
        settings.getExtensions().create("gradleEnterprise", GradleEnterpriseStubExtension.class);
    }

    public static class GradleEnterpriseStubExtension {
        private final GradleEnterpriseBuildScanStub buildScan = new GradleEnterpriseBuildScanStub();

        public GradleEnterpriseBuildScanStub getBuildScan() {
            return buildScan;
        }

        public void buildScan(Action<? super GradleEnterpriseBuildScanStub> action) {
            action.execute(buildScan);
        }
    }

    public static class GradleEnterpriseBuildScanStub {
        private String termsOfServiceUrl;
        private String termsOfServiceAgree;

        public String getTermsOfServiceUrl() {
            return termsOfServiceUrl;
        }

        public void setTermsOfServiceUrl(String termsOfServiceUrl) {
            this.termsOfServiceUrl = termsOfServiceUrl;
        }

        public String getTermsOfServiceAgree() {
            return termsOfServiceAgree;
        }

        public void setTermsOfServiceAgree(String termsOfServiceAgree) {
            this.termsOfServiceAgree = termsOfServiceAgree;
        }

        public void tag(String tag) {
            // No-op stub for tagging
        }
    }
}
