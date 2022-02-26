plugins {
    java
    id("io.freefair.lombok") version "6.3.0"
}

group = "com.rationaleemotions"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.22")

    annotationProcessor("org.projectlombok:lombok:1.18.22")

    implementation("org.slf4j:slf4j-simple:1.7.36")

    testImplementation("org.testng:testng:7.5")
    testImplementation("org.assertj:assertj-core:3.22.0")
}

tasks.getByName<Test>("test") {
    useTestNG()
}