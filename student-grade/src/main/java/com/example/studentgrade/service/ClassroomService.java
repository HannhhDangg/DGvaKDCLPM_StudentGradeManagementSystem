package com.example.studentgrade.service;

import com.example.studentgrade.model.Classroom;
import com.example.studentgrade.model.Semester;
import com.example.studentgrade.model.Student;
import com.example.studentgrade.model.Subject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class ClassroomService {

    @Inject
    EntityManager em;

    public Student getTeacherByUsername(String username) {
        try {
            return em.createQuery("SELECT s FROM Student s WHERE s.username = :username", Student.class)
                    .setParameter("username", username)
                    .setMaxResults(1).getSingleResult();
        } catch (Exception e) {
            Student teacher = new Student();
            teacher.setId(0L);
            teacher.setFirstName("User");
            teacher.setLastName("Teacher");
            return teacher;
        }
    }

    public Long countClassesByTeacher(Long teacherId, Long subjectId, Long semesterId) {
        String qlString = "SELECT COUNT(c) FROM Classroom c WHERE c.teacherId = :tid";
        if (subjectId != null && subjectId > 0)
            qlString += " AND c.subjectId = :subId";
        if (semesterId != null && semesterId > 0)
            qlString += " AND c.semesterId = :semId";

        var query = em.createQuery(qlString, Long.class).setParameter("tid", teacherId);
        if (subjectId != null && subjectId > 0)
            query.setParameter("subId", subjectId);
        if (semesterId != null && semesterId > 0)
            query.setParameter("semId", semesterId);

        return query.getSingleResult();
    }

    public List<Classroom> getClassesByTeacher(Long teacherId, Long subjectId, Long semesterId, int page,
            int pageSize) {
        String qlString = "SELECT c FROM Classroom c WHERE c.teacherId = :tid";
        if (subjectId != null && subjectId > 0)
            qlString += " AND c.subjectId = :subId";
        if (semesterId != null && semesterId > 0)
            qlString += " AND c.semesterId = :semId";
        qlString += " ORDER BY c.startDate DESC";

        var query = em.createQuery(qlString, Classroom.class).setParameter("tid", teacherId);
        if (subjectId != null && subjectId > 0)
            query.setParameter("subId", subjectId);
        if (semesterId != null && semesterId > 0)
            query.setParameter("semId", semesterId);

        return query.setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
    }

    public List<Subject> getSubjectsByTeacher(Long teacherId) {
        return em.createQuery(
                "SELECT s FROM Subject s JOIN TeacherSubject ts ON s.id = ts.subjectId WHERE ts.teacherId = :tid",
                Subject.class)
                .setParameter("tid", teacherId)
                .getResultList();
    }

    public List<Semester> getAllSemesters() {
        return em.createQuery("SELECT s FROM Semester s ORDER BY s.startDate DESC", Semester.class)
                .getResultList();
    }

    @Transactional
    public void addClassroom(String username, Long subjectId, String subjectName, String classCode, Long semesterId,
            String description) {
        Student creator = getTeacherByUsername(username);
        Classroom classroom = new Classroom();
        classroom.setName(subjectName + " (" + classCode + ")");
        classroom.setSubjectId(subjectId);
        classroom.setSubjectName(subjectName);
        classroom.setDescription(description);

        setSemesterInfo(classroom, semesterId);

        if (creator != null && creator.getId() != 0L) {
            classroom.setTeacherId(creator.getId());
        }
        em.persist(classroom);
    }

    @Transactional
    public void deleteClassroom(String username, Long id) {
        Student teacher = getTeacherByUsername(username);
        Classroom classroom = em.find(Classroom.class, id);
        if (classroom != null && teacher != null && teacher.getId().equals(classroom.getTeacherId())) {
            em.remove(classroom);
        }
    }

    @Transactional
    public void editClassroom(String username, Long id, String name, Long semesterId, String description) {
        Student teacher = getTeacherByUsername(username);
        Classroom classroom = em.find(Classroom.class, id);
        if (classroom != null && teacher != null && teacher.getId().equals(classroom.getTeacherId())) {
            classroom.setName(name);
            classroom.setDescription(description);
            setSemesterInfo(classroom, semesterId);
            em.merge(classroom);
        }
    }

    private void setSemesterInfo(Classroom classroom, Long semesterId) {
        if (semesterId != null) {
            Semester sem = em.find(Semester.class, semesterId);
            if (sem != null) {
                classroom.setSemesterId(sem.getId());
                classroom.setSemesterName(sem.getName());
                classroom.setStartDate(sem.getStartDate());
                classroom.setEndDate(sem.getEndDate());
            }
        } else if (classroom.getSemesterId() == null) {
            // Tự động gán cho học kỳ đầu tiên
            List<Semester> sems = getAllSemesters();
            if (!sems.isEmpty()) {
                classroom.setSemesterId(sems.get(0).getId());
                classroom.setSemesterName(sems.get(0).getName());
                classroom.setStartDate(sems.get(0).getStartDate());
                classroom.setEndDate(sems.get(0).getEndDate());
            }
        }
    }
}