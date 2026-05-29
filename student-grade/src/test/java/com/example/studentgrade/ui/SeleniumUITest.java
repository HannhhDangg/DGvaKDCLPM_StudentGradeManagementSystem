package com.example.studentgrade.ui;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class SeleniumUITest {

    private WebDriver driver;

    @BeforeEach
    public void setup() {
        ChromeOptions options = new ChromeOptions();
        // Thêm "--headless=new" nếu muốn chạy ngầm không hiện UI trình duyệt
        // options.addArguments("--headless=new"); 
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--remote-allow-origins=*");
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
    }

    @Test
    public void testE2EWorkflow() throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        // 1. Truy cập vào trang web THỰC TẾ (Quarkus chạy test ở port 8081)
        driver.get("http://localhost:8081/login");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("username")));

        // Kịch bản 1: Kiểm thử hộp đen - Nhập sai mật khẩu
        driver.findElement(By.name("username")).sendKeys("Admin");
        driver.findElement(By.name("password")).sendKeys("wrongpass");
        driver.findElement(By.tagName("button")).click();

        // Chờ hiển thị lỗi URL
        wait.until(ExpectedConditions.urlContains("error=true"));
        assertTrue(driver.getCurrentUrl().contains("error=true"), "Phải xuất hiện lỗi error=true");

        // Kịch bản 2: Đăng nhập thành công với Backdoor account
        driver.findElement(By.name("username")).clear();
        driver.findElement(By.name("username")).sendKeys("admin");
        driver.findElement(By.name("password")).clear();
        driver.findElement(By.name("password")).sendKeys("123456");
        driver.findElement(By.tagName("button")).click();
        
        wait.until(ExpectedConditions.urlContains("/dashboard"));
    }

    @AfterEach
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }
}