# SRA Pune Selenium Automation (Java)

This Maven project uses Selenium WebDriver, Chrome, and JUnit 5. Test code is under `src/test/java` in the `base`, `pages`, and `tests` packages.

## Open and run in VS Code

1. Install **JDK 17 or later**, **Maven 3.8 or later**, and **Google Chrome**.
2. In VS Code, install the **Extension Pack for Java** (it includes the Java test runner).
3. Select **File → Open Folder** and open this project folder, the one that contains `pom.xml`.
4. Wait for the Java and Maven projects to finish loading.
5. For the visible browser run, open **Terminal → Run Task… → SRA: run homepage test (show Chrome)**. You can also open `HomePageSmokeTest.java` and click **Run Test** above the test method.
6. To run the homepage modules one at a time, choose **Terminal → Run Task… → SRA: click and scroll each site module**. Each module has a separate page class under `src/test/java/pages/modules`, and the test reports each module separately. It clicks the homepage control, waits five seconds, scrolls down the opened page, then returns to the homepage.
7. To check PDF documents, choose **Terminal → Run Task… → SRA: check every PDF document**. It visits each homepage module and navigation page, collects PDF links, opens each PDF in its own Chrome tab, waits five seconds, and validates its HTTP response and PDF signature. The PDF tabs stay open after the test so you can inspect them. Failures are printed in the terminal and included in the Maven test report.

The project includes tasks for the homepage test, sequential module flow, PDF checks, and headless/all-test runs. The tasks run from the project root, so Maven can find `pom.xml`.

## Terminal commands

```bash
mvn -Dtest=HomePageSmokeTest test -Dheadless=false
mvn test -Dheadless=false
```

Chrome runs headlessly by default; set `-Dheadless=false` to watch it. Selenium Manager locates or downloads a compatible ChromeDriver when the test starts, so the first run may need internet access.

## Current flow

`HomePageSmokeTest` opens `https://srapune.gov.in/`, waits for the page body, and verifies the browser stays on the SRA Pune site and the page has a title. `SiteModulesFlowTest` runs one named dynamic test per homepage module; each opens the button, waits five seconds, scrolls the page, and reports its own pass/error. `PdfDocumentsTest` discovers PDF links on the homepage, homepage modules, and navigation pages, opens each PDF in a separate Chrome tab, waits five seconds, checks HTTP status and the PDF file signature, and reports every failure. It leaves Chrome and PDF tabs open when run with `-Dkeep.browser.open=true`. The module labels are in Marathi and can be updated in their individual classes under `pages/modules` if the website changes them.
