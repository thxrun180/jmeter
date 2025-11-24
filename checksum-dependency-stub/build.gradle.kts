plugins {
    `java-gradle-plugin`
}

group = "org.example"
version = "1.74"

gradlePlugin {
    plugins {
        create("stubChecksumDependency") {
            id = "com.github.vlsi.checksum-dependency"
            implementationClass = "org.example.ChecksumDependencyStubPlugin"
        }
    }
}
