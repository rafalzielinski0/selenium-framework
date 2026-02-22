plugins {
    java
    id("io.qameta.allure") version "2.12.0"
}

group = "com.example"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

val selenideVersion = "7.14.0"
val junitVersion   = "5.11.4"
val allureVersion  = "2.30.0"

dependencies {
    // Selenide (includes Selenium WebDriver)
    testImplementation("com.codeborne:selenide:$selenideVersion")

    // JUnit 5
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Allure
    testImplementation("io.qameta.allure:allure-junit5:$allureVersion")
    testImplementation("io.qameta.allure:allure-selenide:$allureVersion")

    // Logging
    testImplementation("org.slf4j:slf4j-simple:2.0.16")
}

allure {
    version.set(allureVersion)

    adapter {
        autoconfigure.set(true)
        aspectjWeaver.set(false)
        categoriesFile.set(
            layout.projectDirectory.file("src/test/resources/allure-categories.json")
        )

        frameworks {
            junit5 {
                enabled.set(true)
                autoconfigureListeners.set(true)
            }
        }
    }

    report {
        reportDir.set(layout.buildDirectory.dir("reports/allure-report"))
    }
}

// allureReport zawsze poprzedzone uruchomieniem testów;
// czyści stary raport przed generowaniem (allure CLI nie nadpisuje bez --clean)
tasks.named("allureReport") {
    dependsOn(tasks.named("test"))
    doFirst {
        delete(layout.buildDirectory.dir("reports/allure-report"))
    }
}

// Workaround na bug allure-gradle 2.12.0:
// Plugin zostawia katalog tam gdzie Gradle spodziewa się pliku-znacznika.
// Gradle waliduje outputy tasków PRZED wykonaniem doFirst, więc kasujemy
// ten katalog w fazie konfiguracji (przed walidacją).
layout.buildDirectory.dir("copy-categories").get().asFile.deleteRecursively()

tasks.withType<Test> {
    useJUnitPlatform()

    val headless:      String by project
    val baseUrl:       String by project
    val browser:       String by project
    val browserBinary: String by project

    systemProperty("selenide.headless",      headless)
    systemProperty("selenide.baseUrl",       baseUrl)
    systemProperty("selenide.browser",       browser)
    systemProperty("selenide.browserBinary", browserBinary)
    systemProperty("allure.results.directory",
        layout.buildDirectory.dir("allure-results").get().asFile.path)

    testLogging {
        events("passed", "skipped", "failed")
        showExceptions  = true
        showCauses      = true
        showStackTraces = false
    }
    failFast = false
}
