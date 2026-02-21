package tests;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;
import pages.HomePage;

@Feature("Home Page")
class HomePageTest extends BaseTest {

    @Test
    @Story("Page loads")
    @Description("Verify the home page heading is visible after navigation")
    void homePageHeadingIsVisible() {
        new HomePage()
                .<HomePage>open()
                .verifyHeadingVisible();
    }
}
