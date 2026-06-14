package com.example.studentgrade.ui;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

@QuarkusTest
public class SeleniumUITest {

    private WebDriver driver;
    private WebDriverWait wait;

    // Port mặc định của Quarkus Test là 8081 (nếu bạn dùng cổng khác hãy đổi ở đây)
    private static final String BASE_URL = "http://localhost:8081";

    @BeforeEach
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        // Bỏ comment dòng dưới nếu muốn trình duyệt chạy ngầm (không hiển thị UI) để test nhanh hơn
        // options.addArguments("--headless");

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            // Lệnh quit() sẽ đóng toàn bộ các tab và kết thúc phiên làm việc của ChromeDriver
            driver.quit();
        }
    }

    @Test
    @DisplayName("Kịch bản E2E Tuần tự: Sai Pass -> Admin -> Teacher 1 -> Student -> Teacher 2")
    public void testEndToEndSequentialWorkflow() {

        // 1. Test nhập sai mật khẩu (Security)
        driver.get(BASE_URL + "/login");
        sleep(1000); // Dừng 1s để xem form đăng nhập
        login("student_1", "wrong_password");
        // Assert: Chờ URL xuất hiện param error
        wait.until(ExpectedConditions.urlContains("error"));
        sleep(1000); // Xem thông báo lỗi hiện ra
        logout(); // Reset cookie

        // 2. Luồng Admin: Đăng nhập -> Thêm Học kỳ -> Đăng xuất
        driver.get(BASE_URL + "/login");
        login("admin", "123456");
        sleep(1000);
        driver.get(BASE_URL + "/semesters"); // Điều hướng trang quản lý Học kỳ
        sleep(1000);
        
        // Mở Modal Thêm Học kỳ
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@data-bs-target='#addSemesterModal']"))).click();
        sleep(1000); // Xem modal mở lên
        
        // Điền Form
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@id='addSemesterModal']//input[@name='name']"))).sendKeys("Học kỳ Mùa Thu 2026");
        
        // Dùng JavascriptExecutor để vượt qua lỗi định dạng ngày tháng (Locale) của thẻ <input type="date">
        WebElement startInput = driver.findElement(By.xpath("//div[@id='addSemesterModal']//input[@name='startDate']"));
        WebElement endInput = driver.findElement(By.xpath("//div[@id='addSemesterModal']//input[@name='endDate']"));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].value='2026-07-01';", startInput); // Cách hôm nay (14/06) đúng 17 ngày, thỏa mãn điều kiện < 35 ngày
        js.executeScript("arguments[0].value='2026-08-30';", endInput);
        
        sleep(1000); // Xem form đã được gõ chữ
        driver.findElement(By.xpath("//div[@id='addSemesterModal']//button[@type='submit']")).click();
        
        // Chờ giao diện load xong dòng Học kỳ mới
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//td[contains(text(), 'Học kỳ Mùa Thu 2026')]")));
        sleep(1500); // Xem kết quả cập nhật trên bảng
        logout();

        // 3. Luồng Teacher 1: Đăng nhập -> Thêm Lớp học -> Đăng xuất
        driver.get(BASE_URL + "/login");
        login("teacher_cntt", "123456");
        sleep(1000);
        driver.get(BASE_URL + "/classes"); // Điều hướng trang quản lý Lớp học
        sleep(1000);
        
        // Mở Modal Thêm Lớp học
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@data-bs-target='#addClassModal']"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("subjectSelect")));
        sleep(1000);
        
        // Chọn môn học đầu tiên trong danh sách (option 2 vì option 1 là placeholder)
        driver.findElement(By.xpath("//select[@id='subjectSelect']/option[2]")).click(); 
        driver.findElement(By.name("classCode")).sendKeys("TEST-FALL");
        // Chọn Học kỳ vừa tạo
        driver.findElement(By.xpath("//div[@id='addClassModal']//select[@name='semesterId']/option[contains(text(), 'Học kỳ Mùa Thu 2026')]")).click();
        sleep(1000); // Xem form tạo lớp
        driver.findElement(By.xpath("//div[@id='addClassModal']//button[@type='submit']")).click();
        
        // Chờ lớp học mới được tạo thành công
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//td[contains(., 'TEST-FALL')]")));
        sleep(1500); // Dừng để xem danh sách có lớp mới
        logout();

        // 4. Luồng Student: Đăng nhập -> Đăng ký môn -> Đăng xuất
        driver.get(BASE_URL + "/login");
        login("student_1", "123456");
        sleep(1000);
        driver.get(BASE_URL + "/student/enroll"); // Điều hướng trang Đăng ký môn
        sleep(1500); // Chậm lại để xem danh sách môn học
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//tr[td[contains(., 'TEST-FALL')]]")));
        WebElement btnRegister = driver.findElement(By.xpath("//tr[td[contains(., 'TEST-FALL')]]//button[contains(text(), 'Đăng ký')]"));
        
        // Cuộn màn hình để nút không bị che bởi Topbar, sau đó dùng click() của Selenium để kích hoạt đúng sự kiện onsubmit (bật Alert)
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", btnRegister);
        sleep(500);
        btnRegister.click();
        
        // Xử lý hộp thoại Xác nhận (Confirm Alert) của JS sinh ra do có thẻ onsubmit
        wait.until(ExpectedConditions.alertIsPresent());
        sleep(1000); // Chờ 1s xem cảnh báo
        driver.switchTo().alert().accept();
        
        // Chờ lớp học nhảy lên bảng "Chờ duyệt"
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//span[contains(text(), 'Chờ duyệt')]")));
        sleep(1500); // Xem lớp nhảy lên bảng đang chờ duyệt
        logout();

        // 5. Luồng Teacher 2: Đăng nhập lại -> Duyệt sinh viên -> Nhập điểm -> Đăng xuất
        driver.get(BASE_URL + "/login");
        login("teacher_cntt", "123456");
        sleep(1000);
        driver.get(BASE_URL + "/teacher/enrollments"); // Điều hướng trang duyệt danh sách đăng ký
        sleep(1500);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//tr[td[contains(., 'TEST-FALL')]]")));
        
        WebElement btnApprove = driver.findElement(By.xpath("//tr[td[contains(., 'TEST-FALL')]]//button[contains(text(), 'Duyệt')]"));
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", btnApprove);
        sleep(500);
        btnApprove.click();
        sleep(1000);
        
        // Quay về trang Lớp của tôi để Nhập điểm
        driver.get(BASE_URL + "/teacher");
        sleep(1500);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[contains(text(), 'TEST-FALL')]")));
        
        // Bấm nút Nhập điểm
        WebElement btnGrade = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(), 'Nhập điểm')]")));
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", btnGrade);
        sleep(500);
        btnGrade.click();
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("gradeAtt"))).clear();
        sleep(500);
        
        // --- BỔ SUNG: Test Negative - Nhập điểm sai (Ngoài khoảng 0-10) ---
        driver.findElement(By.id("gradeAtt")).sendKeys("15.0"); // Biên trên: Vượt quá 10
        driver.findElement(By.id("gradeMid")).clear();
        driver.findElement(By.id("gradeMid")).sendKeys("-5.0"); // Biên dưới: Nhỏ hơn 0
        sleep(1500); // Dừng lại để bạn xem Selenium cố tình gõ sai
        driver.findElement(By.xpath("//div[@id='gradeModal']//button[@type='submit']")).click();
        
        // Assert: Xác minh form KHÔNG bị submit, trình duyệt đã chặn lại bằng HTML5 Validation
        boolean isAttInvalid = (Boolean) js.executeScript("return !document.getElementById('gradeAtt').checkValidity();");
        Assertions.assertTrue(isAttInvalid, "Trường Chuyên cần phải báo lỗi invalid khi nhập 15.0");
        sleep(1000); // Xem cảnh báo màu đỏ của trình duyệt
        // ------------------------------------------------------------------

        // Nhập lại điểm ĐÚNG để đi tiếp luồng Happy Path
        driver.findElement(By.id("gradeAtt")).clear();
        driver.findElement(By.id("gradeAtt")).sendKeys("9.0");
        driver.findElement(By.id("gradeMid")).clear();
        driver.findElement(By.id("gradeMid")).sendKeys("8.5");
        driver.findElement(By.id("gradeExam")).clear();
        driver.findElement(By.id("gradeExam")).sendKeys("9.0");
        sleep(1500); // Xem điểm đã được sửa lại cho đúng
        driver.findElement(By.xpath("//div[@id='gradeModal']//button[@type='submit']")).click();
        
        // Chờ modal đóng và điểm CC(9.0) hiển thị ra bảng
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//td[contains(text(), '9.0')]")));
        sleep(2000); // Xem kết quả sau khi duyệt và chấm điểm hoàn tất
        logout();
    }

    // --- Helper Method ---
    private void login(String username, String password) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username"))).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.id("loginBtn")).click();
    }

    private void logout() {
        // Xóa sạch cookie để đảm bảo phiên làm việc được reset hoàn toàn
        driver.manage().deleteAllCookies();
        driver.get(BASE_URL + "/login"); // Chuyển về trang login vì dự án không có mapping /logout
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}