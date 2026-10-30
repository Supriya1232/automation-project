package tests;

import base.BaseTest;
import org.junit.jupiter.api.Test;
import pages.HomePage;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HomePageSmokeTest extends BaseTest {
    @Test
    void homepageLoads() {
        HomePage homePage = new HomePage(driver).open();

        assertTrue(homePage.currentUrl().startsWith("https://srapune.gov.in"),
                "Browser should remain on the SRA Pune website");
        assertTrue(homePage.pageBodyIsVisible(), "Homepage body should be visible");
        assertTrue(homePage.title() != null && !homePage.title().isBlank(),
                "Homepage should have a non-empty page title");
    }
}
