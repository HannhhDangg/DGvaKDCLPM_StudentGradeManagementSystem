package com.example.studentgrade.service;

import com.example.studentgrade.model.Student;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class StudentServiceTest {

    @Inject
    StudentService studentService;

    @Test
    void testUserCreationAndCount() {
        long initialCount = studentService.getTotalUsers();

        Student user = new Student();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setUsername("testuser123");
        user.setEmail("test@domain.com");
        user.setRole("STUDENT");
        user.setPassword("123456");

        studentService.saveUser(user);

        assertEquals(initialCount + 1, studentService.getTotalUsers(), "Tổng số người dùng phải tăng thêm 1");
    }
}
