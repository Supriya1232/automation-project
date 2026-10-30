package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import pages.modules.HomepageModule;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Finds navigation links on the live site and visits them sequentially. */
public class SiteModulesPage {
    private static final String DEFAULT_URL = "https://srapune.gov.in/";
    private final WebDriver driver;
    private final WebDriverWait wait;
    private String mainWindow;

    public SiteModulesPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    public SiteModulesPage openHome() {
        if (mainWindow == null) {
            mainWindow = driver.getWindowHandle();
        }
        driver.get(System.getProperty("base.url", DEFAULT_URL));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        wait.until(driver -> "complete".equals(
                ((JavascriptExecutor) driver).executeScript("return document.readyState")));
        return this;
    }

    public List<ModuleLink> findNavigationModules() {
        URI home = URI.create(System.getProperty("base.url", DEFAULT_URL));
        Map<String, ModuleLink> uniqueLinks = new LinkedHashMap<>();

        for (WebElement link : driver.findElements(By.cssSelector(
                "header a[href], nav a[href], [role='navigation'] a[href]"))) {
            if (!link.isDisplayed()) {
                continue;
            }

            String label = link.getText().trim();
            String href = link.getAttribute("href");
            if (label.isBlank() || href == null || href.isBlank() || !isSameSite(home, href)) {
                continue;
            }

            URI target = URI.create(href);
            String normalizedTarget = canonicalUrl(href);
            String normalizedHome = canonicalUrl(home.toString());
            if (normalizedTarget.equals(normalizedHome) || target.getPath().equals("/")) {
                continue;
            }
            uniqueLinks.putIfAbsent(href, new ModuleLink(label, href));
        }
        return new ArrayList<>(uniqueLinks.values());
    }

    public List<PdfLink> findPdfLinks() {
        Map<String, PdfLink> pdfLinks = new LinkedHashMap<>();
        for (WebElement link : driver.findElements(By.cssSelector("a[href]"))) {
            String href = link.getAttribute("href");
            String label = link.getText().trim();
            String searchable = ((href == null ? "" : href) + " " + label + " "
                    + link.getAttribute("title") + " " + link.getAttribute("aria-label") + " "
                    + link.getAttribute("type")).toLowerCase();
            if (href != null && (searchable.contains(".pdf") || searchable.contains("application/pdf"))) {
                pdfLinks.putIfAbsent(href, new PdfLink(label.isBlank() ? href : label, href,
                        driver.getCurrentUrl(), null));
            }
        }
        return new ArrayList<>(pdfLinks.values());
    }

    public void click(ModuleLink module) {
        WebElement link = wait.until(driver -> driver.findElements(By.cssSelector(
                        "header a[href], nav a[href], [role='navigation'] a[href]")).stream()
                .filter(WebElement::isDisplayed)
                .filter(candidate -> sameDestination(candidate.getAttribute("href"), module.href()))
                .findFirst()
                .orElse(null));
        boolean opensNewWindow = "_blank".equalsIgnoreCase(link.getAttribute("target"));
        Set<String> previousHandles = new HashSet<>(driver.getWindowHandles());
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", link);
        try {
            wait.until(ExpectedConditions.elementToBeClickable(link)).click();
        } catch (ElementNotInteractableException clickError) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", link);
        }

        if (opensNewWindow) {
            wait.until(driver -> driver.getWindowHandles().size() > previousHandles.size());
            for (String handle : driver.getWindowHandles()) {
                if (!previousHandles.contains(handle)) {
                    driver.switchTo().window(handle);
                    break;
                }
            }
        }
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        wait.until(driver -> "complete".equals(
                ((JavascriptExecutor) driver).executeScript("return document.readyState")));
        pause(5_000); // Give each module five seconds to finish rendering before scrolling.
    }

    public String clickPdf(PdfLink pdf) {
        if (pdf.directModule() != null) {
            driver.get(System.getProperty("base.url", DEFAULT_URL));
            return pdf.directModule().clickPdfInNewTab(driver);
        }
        driver.get(pdf.sourceUrl());
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        wait.until(driver -> "complete".equals(
                ((JavascriptExecutor) driver).executeScript("return document.readyState")));
        scrollThroughPage(); // Load PDF links that may be lazy-loaded below the fold.

        WebElement link = wait.until(driver -> driver.findElements(By.cssSelector("a[href]")).stream()
                .filter(WebElement::isDisplayed)
                .filter(candidate -> sameDestination(candidate.getAttribute("href"), pdf.href()))
                .findFirst()
                .orElse(null));
        String startingUrl = driver.getCurrentUrl();
        Set<String> previousHandles = new HashSet<>(driver.getWindowHandles());
        if (link.getDomAttribute("download") != null) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].removeAttribute('download');", link);
        }
        ((JavascriptExecutor) driver).executeScript("arguments[0].setAttribute('target', '_blank');", link);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", link);
        try {
            wait.until(ExpectedConditions.elementToBeClickable(link)).click();
        } catch (ElementNotInteractableException clickError) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", link);
        }

        try {
            wait.until(driver -> driver.getWindowHandles().size() > previousHandles.size());
            for (String handle : driver.getWindowHandles()) {
                if (!previousHandles.contains(handle)) {
                    driver.switchTo().window(handle);
                    break;
                }
            }
            wait.until(driver -> !"about:blank".equals(driver.getCurrentUrl()));
        } catch (org.openqa.selenium.TimeoutException noNewTab) {
            String openedUrl = driver.getCurrentUrl();
            driver.switchTo().window(mainWindow);
            driver.get(pdf.sourceUrl());
            driver.switchTo().newWindow(WindowType.TAB);
            driver.get(openedUrl.equals(startingUrl) ? pdf.href() : openedUrl);
        }
        pause(5_000); // Let the PDF viewer render before validating the document response.
        return driver.getCurrentUrl();
    }

    public void switchToMainWindowPreservingPdfTabs() {
        if (mainWindow != null && driver.getWindowHandles().contains(mainWindow)) {
            driver.switchTo().window(mainWindow);
        }
    }

    public void scrollThroughPage() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        long previousHeight = -1;
        int stableAtBottom = 0;
        for (int i = 0; i < 80 && stableAtBottom < 2; i++) {
            js.executeScript("window.scrollBy({top: Math.max(400, window.innerHeight * 0.75), behavior: 'smooth'});");
            pause(500);
            long height = ((Number) js.executeScript("return document.documentElement.scrollHeight;")).longValue();
            long position = ((Number) js.executeScript("return window.scrollY + window.innerHeight;")).longValue();
            if (position + 5 >= height && height == previousHeight) {
                stableAtBottom++;
            } else {
                stableAtBottom = 0;
            }
            previousHeight = height;
        }
        js.executeScript("window.scrollTo({top: 0, behavior: 'smooth'});");
        pause(300);
    }

    public String currentUrl() {
        return driver.getCurrentUrl();
    }

    public void returnToMainWindow() {
        for (String handle : new ArrayList<>(driver.getWindowHandles())) {
            if (!handle.equals(mainWindow)) {
                driver.switchTo().window(handle);
                driver.close();
            }
        }
        driver.switchTo().window(mainWindow);
    }

    private static boolean isSameSite(URI home, String href) {
        try {
            URI target = URI.create(href);
            return target.getHost() != null && target.getHost().equalsIgnoreCase(home.getHost());
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static boolean sameDestination(String first, String second) {
        if (first == null || second == null) {
            return false;
        }
        try {
            return canonicalUrl(first).equals(canonicalUrl(second));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static String canonicalUrl(String value) {
        URI uri = URI.create(value).normalize();
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        String authority = uri.getAuthority() == null ? "" : uri.getAuthority().toLowerCase();
        String path = uri.getPath() == null || uri.getPath().isEmpty() ? "/" : uri.getPath();
        return scheme + "://" + authority + path
                + (uri.getQuery() == null ? "" : "?" + uri.getQuery());
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while scrolling the module page", e);
        }
    }

    public record ModuleLink(String label, String href) { }
    public record PdfLink(String label, String href, String sourceUrl, HomepageModule directModule) { }
}
