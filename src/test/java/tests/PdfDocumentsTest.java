package tests;

import base.BaseTest;
import org.junit.jupiter.api.Test;
import pages.SiteModulesPage;
import pages.SiteModulesPage.ModuleLink;
import pages.SiteModulesPage.PdfLink;
import pages.modules.HomepageModule;
import pages.modules.HomepageModules;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfDocumentsTest extends BaseTest {
    private static final int PDF_HEADER_SCAN_BYTES = 1024;

    @Test
    void clickOpenAndValidateEveryDiscoveredPdf() throws Exception {
        String homeUrl = System.getProperty("base.url", "https://srapune.gov.in/");
        SiteModulesPage page = new SiteModulesPage(driver).openHome();
        String mainWindow = driver.getWindowHandle();
        Map<String, PdfLink> pdfs = new LinkedHashMap<>();

        collectPdfs(page, pdfs);
        page.scrollThroughPage();
        collectPdfs(page, pdfs);

        for (HomepageModule module : HomepageModules.all()) {
            page.openHome();
            String targetUrl = module.targetUrl(driver);
            try {
                String openedUrl = module.click(driver); // Clicks each named homepage module and waits five seconds.
                if (isPdfUrl(targetUrl)) {
                    pdfs.putIfAbsent(targetUrl, new PdfLink(module.label(), targetUrl, homeUrl, module));
                } else if (isPdfUrl(openedUrl)) {
                    pdfs.putIfAbsent(openedUrl, new PdfLink(module.label(), openedUrl, homeUrl, module));
                } else {
                    page.scrollThroughPage();
                    collectPdfs(page, pdfs);
                }
            } finally {
                HomepageModule.closeExtraWindows(driver, mainWindow);
            }
        }

        // Include ordinary top-navigation pages as well as the homepage cards.
        List<ModuleLink> navigation = page.openHome().findNavigationModules();
        for (ModuleLink module : navigation) {
            page.openHome();
            page.click(module);
            page.scrollThroughPage();
            collectPdfs(page, pdfs);
            page.returnToMainWindow();
        }

        assertFalse(pdfs.isEmpty(), "No PDF links were found on the homepage or its modules.");

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        List<String> errors = new ArrayList<>();

        for (PdfLink pdf : pdfs.values()) {
            try {
                String openedUrl = page.clickPdf(pdf); // Clicks the actual PDF link, waits five seconds, and opens it.
                System.out.printf("PDF OPENED | %s | %s%n", pdf.label(), openedUrl);
                checkPdf(client, pdf, errors);
            } catch (Exception e) {
                String error = String.format("PDF OPEN ERROR | %s | %s | %s: %s",
                        pdf.label(), pdf.href(), e.getClass().getSimpleName(), e.getMessage());
                System.out.println(error);
                errors.add(error);
            } finally {
                page.switchToMainWindowPreservingPdfTabs();
            }
        }

        assertTrue(errors.isEmpty(), "PDF validation errors:\n" + String.join("\n", errors));
    }

    private void collectPdfs(SiteModulesPage page, Map<String, PdfLink> pdfs) {
        for (PdfLink pdf : page.findPdfLinks()) {
            pdfs.putIfAbsent(pdf.href(), pdf);
        }
    }

    private boolean isPdfUrl(String url) {
        return url != null && url.toLowerCase().contains(".pdf");
    }

    private void checkPdf(HttpClient client, PdfLink pdf, List<String> errors) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(pdf.href()))
                    .timeout(Duration.ofSeconds(30))
                    .header("Range", "bytes=0-1023")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            String contentType = response.headers().firstValue("content-type").orElse("unknown");
            boolean successfulStatus = response.statusCode() >= 200 && response.statusCode() < 300;
            boolean pdfSignature;
            try (InputStream body = response.body()) {
                pdfSignature = hasPdfSignature(body.readNBytes(PDF_HEADER_SCAN_BYTES));
            }

            if (successfulStatus && pdfSignature) {
                System.out.printf("PDF PASS | %s | HTTP %d | %s%n", pdf.label(), response.statusCode(), contentType);
            } else {
                String error = String.format("PDF FAIL | %s | %s | HTTP %d | content-type=%s | pdf-signature=%s",
                        pdf.label(), pdf.href(), response.statusCode(), contentType, pdfSignature);
                System.out.println(error);
                errors.add(error);
            }
        } catch (Exception e) {
            String error = String.format("PDF ERROR | %s | %s | %s: %s",
                    pdf.label(), pdf.href(), e.getClass().getSimpleName(), e.getMessage());
            System.out.println(error);
            errors.add(error);
        }
    }

    private boolean hasPdfSignature(byte[] bytes) {
        byte[] signature = "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        int maxStart = Math.min(bytes.length - signature.length, PDF_HEADER_SCAN_BYTES - signature.length);
        for (int start = 0; start <= maxStart; start++) {
            boolean match = true;
            for (int i = 0; i < signature.length; i++) {
                if (bytes[start + i] != signature[i]) {
                    match = false;
                    break;
                }
            }
            if (match) return true;
        }
        return false;
    }
}
