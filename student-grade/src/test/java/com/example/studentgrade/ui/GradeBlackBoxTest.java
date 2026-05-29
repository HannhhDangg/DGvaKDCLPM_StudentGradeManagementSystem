package com.example.studentgrade.ui;

import com.example.studentgrade.controller.StudentController;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class GradeBlackBoxTest {

    @Inject
    StudentController studentController;

    @Test
    public void testBoundaryValuesForGrades() {
        // KHÓA 2: KIỂM THỬ HỘP ĐEN - PHÂN TÍCH GIÁ TRỊ BIÊN (Boundary Value Analysis)
        // Kiểm tra luồng Cập nhật điểm của Giảng viên với các giá trị biên

        // 1. Cập nhật thành công với giá trị biên trên (10.0)
        Response response1 = studentController.saveGrade(1L, 1L, "10.0", "10.0", "10.0");
        assertEquals(303, response1.getStatus()); // HTTP 303 là Redirect thành công

        // 2. Cập nhật thành công với giá trị biên dưới (0.0)
        Response response2 = studentController.saveGrade(1L, 1L, "0.0", "0.0", null);
        assertEquals(303, response2.getStatus());
    }
}