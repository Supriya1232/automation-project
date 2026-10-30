package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class HomePage {
    private static final String DEFAULT_URL = "https://srapune.gov.in/";
    private final WebDriver driver;
    private final WebDriverWait wait;

    public HomePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    public HomePage open() {
        driver.get(System.getProperty("base.url", DEFAULT_URL));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        return this;
    }

    public String title() {
        return driver.getTitle();
    }

    public String currentUrl() {
        return driver.getCurrentUrl();
    }

    public boolean pageBodyIsVisible() {
        WebElement body = wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
        return body.isDisplayed();
    }
}
