package com.example.studentgrade.service;

import com.example.studentgrade.model.Student;
import com.example.studentgrade.repository.StudentRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class StudentService {

    @Inject
    StudentRepository repository;

    public List<Student> getAllUsers() {
        return repository.listAll();
    }

    public List<Student> searchUsers(String keyword) {
        String searchPattern = "%" + keyword.toLowerCase() + "%";
        return repository.find(
                "lower(lastName) like ?1 or lower(firstName) like ?2 or lower(username) like ?3 or lower(email) like ?4",
                searchPattern, searchPattern, searchPattern, searchPattern).list();
    }

    @Transactional
    public void saveUser(Student user) {
        repository.persist(user);
    }

    @Transactional
    public void updateUser(Long id, Student updatedData) {
        Student user = repository.findById(id);
        if (user != null) {
            user.setFirstName(updatedData.getFirstName());
            user.setLastName(updatedData.getLastName());
            user.setEmail(updatedData.getEmail());
            user.setRole(updatedData.getRole());
        }
    }

    @Transactional
    public void deleteUser(Long id) {
        repository.deleteById(id);
    }

    public long getTotalUsers() {
        return repository.count();
    }
}
