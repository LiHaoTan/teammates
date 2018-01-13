package teammates.test.cases.browsertests;


import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.thoughtworks.selenium.webdriven.JavascriptLibrary;

import teammates.test.driver.TestProperties;

public class BasicFocusTest {

    public static void main(String[] args) {
        System.setProperty("webdriver.firefox.bin", TestProperties.FIREFOX_PATH);
        FirefoxProfile profile = new FirefoxProfile();
        profile.setEnableNativeEvents(true);
        // whether focusmanager.testmode is enabled or not select onchange is guaranteed to succeed
        // if using Selenium select
        profile.setPreference("focusmanager.testmode", true);

        WebDriver driver = new FirefoxDriver(profile);
        driver.manage().window().maximize();

        driver.get("https://jsfiddle.net/LiHaoTan/pn8ozt80/");

        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);

        driver.switchTo().frame("result");

        WebDriverWait wait = new WebDriverWait(driver, 4);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("abc")));
        final WebElement textInputElement = driver.findElement(By.id("abc"));

        // click to focus element
        textInputElement.click();

        // always trigger change event unless text is empty
        // we manually add some text, but this works with the input default attribute too
        //textInputElement.sendKeys("a");
        //textInputElement.clear();

        // If the browser is in focus change event is triggered, otherwise it never triggers even if
        // focusmanger.testmode is set to true
        textInputElement.sendKeys("TESTING!" + Keys.TAB);

        // no matter what other methods used to blur the input element the change event never
        // executes for input text
        //alternativeBlur1(driver);
        //alternativeBlur2(driver, textInputElement);

        // alternatively click on the other element to blur
        //driver.findElement(By.id("ice-cream")).click();

        //manualTriggerChangeEvent(driver, textInputElement);

        // the guaranteed succeeding change event
        //Select select = new Select(driver.findElement(By.id("ice-cream")));
        //select.selectByVisibleText("Chocolate");
    }

    public static void manualTriggerChangeEvent(WebDriver driver, WebElement textInputElement) {
        // manually trigger change event
        JavascriptLibrary javascript = new JavascriptLibrary();
        javascript.callEmbeddedSelenium(driver, "triggerEvent", textInputElement, "change");
    }

    public static void alternativeBlur2(WebDriver driver, WebElement textInputElement) {
        // alternative 2 to blur element
        JavascriptLibrary javascript = new JavascriptLibrary();
        javascript.callEmbeddedSelenium(driver, "triggerEvent", textInputElement, "blur");
    }

    public static void alternativeBlur1(WebDriver driver) {
        // alternative 1 to blur element
        driver.switchTo().activeElement().sendKeys(Keys.TAB);
    }
}

