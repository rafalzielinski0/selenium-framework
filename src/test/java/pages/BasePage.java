package pages;


/**
 * Base class for all Page Objects.
 * Provides shared helpers for navigation and common element interactions.
 */
public abstract class BasePage {

    /** Override to return the relative path opened when navigating to this page. */
    protected abstract String getPath();

    /** Opens the page via its path. Returns {@code this} for fluent chaining. */
    @SuppressWarnings("unchecked")
    public <T extends BasePage> T open() {
        com.codeborne.selenide.Selenide.open(getPath());
        return (T) this;
    }
}
