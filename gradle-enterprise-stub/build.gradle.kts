plugins {
    `java-gradle-plugin`
}

group = "org.example"
version = "3.19.2"

gradlePlugin {
    plugins {
        create("stubGradleEnterprise") {
            id = "com.gradle.enterprise"
            implementationClass = "org.example.GradleEnterpriseStubPlugin"
        }
    }
}
