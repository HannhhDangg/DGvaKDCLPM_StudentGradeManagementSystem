package com.example.studentgrade.repository;

import com.example.studentgrade.model.Student;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StudentRepository implements PanacheRepository<Student> {
    // PanacheRepository tự động cung cấp các hàm find, persist, listAll...
}
