package base;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
import java.util.Map;

/** Creates an isolated Chrome session for each UI test flow. */
public final class DriverFactory {
    private DriverFactory() { }

    public static WebDriver create() {
        ChromeOptions options = new ChromeOptions();
        if (Boolean.parseBoolean(System.getProperty("headless", "true"))) {
            options.addArguments("--headless=new");
        }
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.setExperimentalOption("prefs", Map.of("plugins.always_open_pdf_externally", false));
        options.addArguments("--window-size=1440,1000", "--disable-gpu", "--no-sandbox");

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
        return driver;
    }
}
