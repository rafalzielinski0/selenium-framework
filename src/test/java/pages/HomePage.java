package pages;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

/**
 * Page Object for the application's home page.
 * Demonstrates the standard POM pattern used across this framework.
 */
public class HomePage extends BasePage {

    // --- Locators -----------------------------------------------------------
    private final SelenideElement heading   = $("h1");
    private final SelenideElement loginLink = $("a[href*='login']");

    // --- Navigation ---------------------------------------------------------

    @Override
    protected String getPath() {
        return "/";
    }

    // --- Actions ------------------------------------------------------------

    @Step("Click login link")
    public void clickLogin() {
        loginLink.shouldBe(visible).click();
    }

    // --- Assertions (fluent) ------------------------------------------------

    @Step("Verify heading is visible")
    public HomePage verifyHeadingVisible() {
        heading.shouldBe(visible);
        return this;
    }
}
