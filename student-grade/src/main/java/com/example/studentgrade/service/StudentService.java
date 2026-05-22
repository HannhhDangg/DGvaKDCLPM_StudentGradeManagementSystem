package com.example.studentgrade.service;

import com.example.studentgrade.model.Student;
import com.example.studentgrade.repository.StudentRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class StudentService {

    @Inject
    StudentRepository repository;

    public String calculateGrade(double score) {
        if (score < 0 || score > 10) {
            throw new IllegalArgumentException("Điểm không hợp lệ, phải từ 0 đến 10");
        }
        if (score >= 8.5)
            return "A";
        if (score >= 7.0)
            return "B";
        if (score >= 5.5)
            return "C";
        if (score >= 4.0)
            return "D";
        return "F";
    }

    @Transactional
    public void saveStudent(Student student) {
        student.setGrade(calculateGrade(student.getScore()));
        repository.persist(student);
    }
}
