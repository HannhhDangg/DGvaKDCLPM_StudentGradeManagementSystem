package com.example.studentgrade.ui;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.http.TestHTTPResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class SeleniumUITest {

    private WebDriver driver;

    @TestHTTPResource("/")
    URL url;

    @BeforeEach
    public void setup() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // Chạy ngầm không mở cửa sổ trình duyệt để test nhanh hơn
        options.addArguments("--remote-allow-origins=*");
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
    }

    @Test
    public void testAddStudentUI() {
        // 1. Mở trang web
        String targetUrl = url.toString().replace("0.0.0.0", "localhost");
        driver.get(targetUrl);

        // 2. Điền form
        driver.findElement(By.id("nameInput")).sendKeys("Nguyen Van A");
        driver.findElement(By.id("scoreInput")).sendKeys("8.5");

        // 3. Click nút Thêm
        driver.findElement(By.id("submitBtn")).click();

        // 4. Kiểm tra xem dữ liệu có hiển thị ở bảng không
        WebElement gradeCell = driver.findElement(By.cssSelector(".student-row:last-child .student-grade"));
        assertTrue(gradeCell.getText().contains("A"));
    }

    @AfterEach
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
