package tests;

import com.codeborne.selenide.logevents.SelenideLogger;
import config.TestConfiguration;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

import static com.codeborne.selenide.Selenide.closeWebDriver;

/**
 * Base class for all test classes.
 * Handles Selenide configuration and Allure listener setup.
 */
public abstract class BaseTest {

    @BeforeAll
    static void globalSetup() {
        TestConfiguration.configure();
        SelenideLogger.addListener("AllureSelenide",
                new AllureSelenide()
                        .screenshots(true)
                        .savePageSource(false));
    }

    @AfterEach
    void tearDown() {
        closeWebDriver();
    }
}
