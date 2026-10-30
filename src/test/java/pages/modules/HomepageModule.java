package pages.modules;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Comparator;
import java.text.Normalizer;

/** Shared click behavior for one named homepage module. */
public abstract class HomepageModule {
    private static final String HOME_URL = "https://srapune.gov.in/";

    public abstract String label();

    protected List<String> labelAliases() {
        return List.of(label());
    }

    public String name() {
        return label();
    }

    public String targetUrl(WebDriver driver) {
        WebElement control = findControl(driver);
        Object value = ((JavascriptExecutor) driver).executeScript(
                "return arguments[0].href || arguments[0].getAttribute('data-href') || " +
                        "arguments[0].getAttribute('data-url') || '';", control);
        return value == null ? "" : value.toString();
    }

    public String click(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        String originalWindow = driver.getWindowHandle();
        Set<String> oldWindows = new HashSet<>(driver.getWindowHandles());
        WebElement control = findControl(driver);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", control);

        try {
            wait.until(ExpectedConditions.elementToBeClickable(control)).click();
        } catch (ElementNotInteractableException clickError) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", control);
        }

        pause(5_000); // Required five-second open/render time for every module.
        for (String handle : driver.getWindowHandles()) {
            if (!oldWindows.contains(handle)) {
                driver.switchTo().window(handle);
                break;
            }
        }
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        return driver.getCurrentUrl();
    }

    public String clickPdfInNewTab(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        String mainWindow = driver.getWindowHandle();
        Set<String> oldWindows = new HashSet<>(driver.getWindowHandles());
        String expectedPdfUrl = targetUrl(driver);
        WebElement control = findControl(driver);
        ((JavascriptExecutor) driver).executeScript(
                "if (arguments[0].tagName.toLowerCase() === 'a') { " +
                        "arguments[0].setAttribute('target', '_blank'); " +
                        "arguments[0].removeAttribute('download'); }", control);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", control);
        try {
            wait.until(ExpectedConditions.elementToBeClickable(control)).click();
        } catch (ElementNotInteractableException clickError) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", control);
        }

        pause(5_000);
        String newWindow = driver.getWindowHandles().stream()
                .filter(handle -> !oldWindows.contains(handle)).findFirst().orElse(null);
        if (newWindow != null) {
            driver.switchTo().window(newWindow);
            wait.until(d -> !"about:blank".equals(d.getCurrentUrl()));
            return driver.getCurrentUrl();
        }

        String openedUrl = driver.getCurrentUrl();
        String pdfUrl = expectedPdfUrl.isBlank() ? openedUrl : expectedPdfUrl;
        driver.switchTo().window(mainWindow);
        driver.get(System.getProperty("base.url", HOME_URL));
        driver.switchTo().newWindow(WindowType.TAB);
        driver.get(pdfUrl);
        pause(5_000);
        return driver.getCurrentUrl();
    }

    public static void closeExtraWindows(WebDriver driver, String mainWindow) {
        for (String handle : new ArrayList<>(driver.getWindowHandles())) {
            if (!handle.equals(mainWindow)) {
                driver.switchTo().window(handle);
                driver.close();
            }
        }
        driver.switchTo().window(mainWindow);
    }

    private WebElement findControl(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        try {
            return wait.until(this::findMatchingControl);
        } catch (TimeoutException timeout) {
            throw new NoSuchElementException("Homepage module control not found for '" + label()
                    + "'. Visible controls: " + visibleControlSummary(driver), timeout);
        }
    }

    private WebElement findMatchingControl(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        List<WebElement> controls = driver.findElements(By.cssSelector(
                "a, button, [role='button'], [onclick], [data-href], [data-url], [class*='btn']"));
        return controls.stream()
                .filter(WebElement::isDisplayed)
                .filter(control -> matchesLabel(readableText(js, control)))
                .min(Comparator.comparingInt(control -> normalize(readableText(js, control)).length()))
                .orElse(null);
    }

    private String readableText(JavascriptExecutor js, WebElement element) {
        Object value = js.executeScript(
                "return [arguments[0].innerText, arguments[0].getAttribute('aria-label'), " +
                        "arguments[0].getAttribute('title'), arguments[0].getAttribute('alt'), " +
                        "...Array.from(arguments[0].querySelectorAll('img[alt]')).map(i => i.alt)]" +
                        ".filter(Boolean).join(' ');", element);
        return value == null ? element.getText() : value.toString();
    }

    private boolean matchesLabel(String actualText) {
        String normalizedActual = normalize(actualText);
        return labelAliases().stream().map(HomepageModule::normalize)
                .anyMatch(alias -> !alias.isBlank() && normalizedActual.contains(alias));
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC)
                .toLowerCase()
                .replaceAll("[\\s\\p{Punct}\\p{Zs}]+", "");
    }

    private String visibleControlSummary(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        List<String> labels = new ArrayList<>();
        for (WebElement element : driver.findElements(By.cssSelector(
                "a, button, [role='button'], [onclick], [data-href], [data-url], [class*='btn']"))) {
            if (element.isDisplayed()) {
                String text = readableText(js, element).trim();
                if (!text.isBlank() && !labels.contains(text)) labels.add(text);
                if (labels.size() == 20) break;
            }
        }
        return labels.toString();
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for module to open", e);
        }
    }
}
