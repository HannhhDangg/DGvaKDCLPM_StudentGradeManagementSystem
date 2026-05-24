package com.example.studentgrade.repository;

import com.example.studentgrade.model.Subject;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SubjectRepository implements PanacheRepository<Subject> {
    // PanacheRepository đã cung cấp sẵn các phương thức listAll(), persist(),
    // delete()
    // Bạn có thể thêm các phương thức tìm kiếm tuỳ chỉnh ở đây nếu cần (VD: tìm
    // theo mã môn)
}