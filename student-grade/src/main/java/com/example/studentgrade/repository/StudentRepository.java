package com.example.studentgrade.repository;

import com.example.studentgrade.model.Student;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class StudentRepository implements PanacheRepository<Student> {
    // PanacheRepository tự động cung cấp các hàm find, persist, listAll...

    public List<Student> searchByName(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return listAll();
        }
        return list("LOWER(name) LIKE LOWER(?1)", "%" + keyword + "%");
    }

    public Student findByUsername(String username) {
        return find("username", username).firstResult();
    }
}
