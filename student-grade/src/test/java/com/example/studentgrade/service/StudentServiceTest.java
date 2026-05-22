package com.example.studentgrade.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class StudentServiceTest {

    @Inject
    StudentService studentService;

    // Test hộp đen với kỹ thuật giá trị biên sử dụng ParameterizedTest
    @ParameterizedTest
    @CsvSource({
            "10.0, A",
            "8.5, A",
            "8.4, B",
            "7.0, B",
            "6.9, C",
            "5.5, C",
            "5.4, D",
            "4.0, D",
            "3.9, F",
            "0.0, F"
    })
    void testCalculateGrade_ValidScores(double score, String expectedGrade) {
        assertEquals(expectedGrade, studentService.calculateGrade(score));
    }

    // Test luồng ngoại lệ (hộp trắng)
    @Test
    void testCalculateGrade_Exceptions() {
        assertThrows(IllegalArgumentException.class, () -> studentService.calculateGrade(-0.1));
        assertThrows(IllegalArgumentException.class, () -> studentService.calculateGrade(10.1));
    }
}
