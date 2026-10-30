package base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver;

public abstract class BaseTest {
    protected WebDriver driver;

    @BeforeEach
    void setUp() {
        driver = DriverFactory.create();
    }

    @AfterEach
    void tearDown() {
        if (driver != null && !Boolean.getBoolean("keep.browser.open")) {
            driver.quit();
        } else if (driver != null) {
            System.out.println("Chrome left open as requested by -Dkeep.browser.open=true");
        }
    }
}
