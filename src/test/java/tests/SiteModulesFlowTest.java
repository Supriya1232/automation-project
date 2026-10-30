package tests;

import base.DriverFactory;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import pages.SiteModulesPage;
import pages.modules.HomepageModule;
import pages.modules.HomepageModules;

import java.time.Duration;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SiteModulesFlowTest {
    @TestFactory
    Stream<DynamicTest> everyHomepageModuleOpensAndScrolls() {
        String homeUrl = System.getProperty("base.url", "https://srapune.gov.in/");
        return HomepageModules.all().stream().map(module -> DynamicTest.dynamicTest(module.name(), () -> {
            WebDriver moduleDriver = DriverFactory.create();
            String mainWindow = null;
            try {
                moduleDriver.get(homeUrl);
                new WebDriverWait(moduleDriver, Duration.ofSeconds(30))
                        .until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
                mainWindow = moduleDriver.getWindowHandle();
                String openedUrl = module.click(moduleDriver);
                assertTrue(moduleDriver.findElement(By.tagName("body")).isDisplayed(),
                        "Module page body should be visible for: " + module.name());
                if (!openedUrl.toLowerCase().contains(".pdf")) {
                    new SiteModulesPage(moduleDriver).scrollThroughPage();
                }
                System.out.printf("MODULE PASS | %s | %s%n", module.name(), openedUrl);
            } finally {
                if (mainWindow != null) {
                    try {
                        HomepageModule.closeExtraWindows(moduleDriver, mainWindow);
                    } catch (WebDriverException cleanupError) {
                        System.out.printf("MODULE CLEANUP | %s | browser session already unavailable%n",
                                module.name());
                    }
                }
                try {
                    moduleDriver.quit();
                } catch (WebDriverException ignored) {
                    // A timed-out module must not prevent the remaining modules from running.
                }
            }
        }));
    }
}
