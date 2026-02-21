package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;
import pages.LoginPage;

@Feature("Login")
class LoginTest extends BaseTest {

    @Test
    @Story("Successful login")
    @Description("Valid credentials should redirect to the secure area with a success message")
    @Severity(SeverityLevel.BLOCKER)
    void successfulLogin() {
        new LoginPage()
                .<LoginPage>open()
                .loginAs("tomsmith", "SuperSecretPassword!")
                .verifyFlashMessage("You logged into a secure area!");
    }

    @Test
    @Story("Failed login")
    @Description("Wrong password should keep the user on the login page with an error message")
    @Severity(SeverityLevel.CRITICAL)
    void wrongPasswordShowsError() {
        new LoginPage()
                .<LoginPage>open()
                .loginAs("tomsmith", "wrongpassword")
                .verifyFlashMessage("Your password is invalid!");
    }

    @Test
    @Story("Failed login")
    @Description("Submitting empty fields should show a validation error message")
    @Severity(SeverityLevel.NORMAL)
    void emptyFieldsShowValidationError() {
        new LoginPage()
                .<LoginPage>open()
                .clickSubmit()
                .verifyFlashMessage("Your username is invalid!");
    }
}
