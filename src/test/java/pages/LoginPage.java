package pages;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class LoginPage extends BasePage {

    // --- Locators -----------------------------------------------------------

    private final SelenideElement usernameInput = $("#username");
    private final SelenideElement passwordInput = $("#password");
    private final SelenideElement submitButton  = $("[type=submit]");
    private final SelenideElement flashMessage  = $(".flash");

    // --- Navigation ---------------------------------------------------------

    @Override
    protected String getPath() {
        return "/login";
    }

    // --- Actions ------------------------------------------------------------

    @Step("Enter username: {username}")
    public LoginPage enterUsername(String username) {
        usernameInput.shouldBe(visible).setValue(username);
        return this;
    }

    @Step("Enter password")
    public LoginPage enterPassword(String password) {
        passwordInput.shouldBe(visible).setValue(password);
        return this;
    }

    @Step("Click submit")
    public LoginPage clickSubmit() {
        submitButton.shouldBe(visible).click();
        return this;
    }

    @Step("Login as {username}")
    public LoginPage loginAs(String username, String password) {
        return enterUsername(username)
                .enterPassword(password)
                .clickSubmit();
    }

    // --- Assertions (fluent) ------------------------------------------------

    @Step("Verify flash message contains: {expectedText}")
    public LoginPage verifyFlashMessage(String expectedText) {
        flashMessage.shouldBe(visible).shouldHave(text(expectedText));
        return this;
    }

    @Step("Verify login form is visible")
    public LoginPage verifyPageLoaded() {
        usernameInput.shouldBe(visible);
        passwordInput.shouldBe(visible);
        submitButton.shouldBe(visible);
        return this;
    }
}
