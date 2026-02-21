package config;

import com.codeborne.selenide.Configuration;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Centralizes Selenide/WebDriver configuration.
 * Values are read from system properties set via build.gradle.kts / gradle.properties.
 */
public final class TestConfiguration {

    private TestConfiguration() {}

    public static void configure() {
        Configuration.baseUrl     = System.getProperty("selenide.baseUrl", "https://example.com");
        Configuration.browser     = System.getProperty("selenide.browser", "chrome");
        Configuration.headless    = Boolean.parseBoolean(System.getProperty("selenide.headless", "true"));
        Configuration.timeout     = 8_000;
        Configuration.pageLoadTimeout = 30_000;
        Configuration.screenshots = true;
        Configuration.savePageSource = false;
        Configuration.reportsFolder = "build/selenide-reports";

        // Browser binary – used when Chrome is installed as Flatpak (wrapper script).
        // Override via -PbrowserBinary=... or leave empty to let WebDriverManager decide.
        String browserBinary = System.getProperty("selenide.browserBinary", "");
        if (!browserBinary.isBlank()) {
            Configuration.browserBinary = browserBinary;
        }

        // Extra Chrome flags needed when Chrome runs inside a Flatpak sandbox.
        // --no-sandbox  : avoids double-sandbox crash (Flatpak + Chrome sandbox).
        // --user-data-dir in home : ChromeDriver auto-generates /tmp dirs, but Flatpak
        //   Chrome has a private /tmp and cannot write DevToolsActivePort there.
        //   Using a home-directory path (accessible after 'flatpak override --filesystem=home')
        //   lets ChromeDriver and Chrome share the same path.
        if ("chrome".equalsIgnoreCase(Configuration.browser)) {
            String home = System.getProperty("user.home");
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-gpu");
            options.addArguments("--user-data-dir=" + home + "/.chrome-wd-profile");
            Configuration.browserCapabilities = options;
        }
    }
}
