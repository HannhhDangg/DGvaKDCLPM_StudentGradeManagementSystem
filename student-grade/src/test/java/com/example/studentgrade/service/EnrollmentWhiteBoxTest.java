package com.example.studentgrade.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class EnrollmentWhiteBoxTest {

    @Inject
    EnrollmentResource enrollmentResource;

    @Test
    public void testTuitionFeeCalculationBranch() {
        // KHÓA 2 & 3: KIỂM THỬ HỘP TRẮNG (Statement/Branch Coverage)
        // Gọi API Phê duyệt để bắt hệ thống phải chạy qua các lệnh if/else tính tiền:
        // "TECHNICAL", "BASIC", "GENERAL" trong hàm updateTuitionFee.
        
        // Mock approve 1 enrollment ID có sẵn từ DataInitializer
        Response response = enrollmentResource.approveEnrollment(1L, "APPROVED");
        assertEquals(303, response.getStatus());
    }
}