package com.example.studentgrade.service;

import com.example.studentgrade.model.Classroom;
import com.example.studentgrade.model.Student;
import com.example.studentgrade.model.Semester;
import com.example.studentgrade.model.Subject;
import com.example.studentgrade.controller.StudentController;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.List;

@Path("/classes")
@Produces(MediaType.TEXT_HTML)
public class ClassroomResource {

    @Inject
    EntityManager em;

    @Inject
    @Location("classes")
    Template classesTemplate;

    @GET
    public String getClassesPage(@CookieParam("logged_in_username") String cookieUsername,
            @QueryParam("page") @DefaultValue("1") int page) {
        int pageSize = 10;
        Long totalRecords = em.createQuery("SELECT COUNT(c) FROM Classroom c", Long.class).getSingleResult();
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

        if (page < 1)
            page = 1;
        if (page > totalPages && totalPages > 0)
            page = totalPages;

        List<Classroom> classes = em.createQuery("SELECT c FROM Classroom c ORDER BY c.startDate DESC", Classroom.class)
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();

        String username = cookieUsername != null ? cookieUsername : "teacher_toan";
        Student teacher;
        try {
            teacher = em.createQuery("SELECT s FROM Student s WHERE s.username = :username", Student.class)
                    .setParameter("username", username)
                    .setMaxResults(1).getSingleResult();
        } catch (Exception e) {
            teacher = new Student();
            teacher.setId(0L); // Tránh lỗi null nếu DB chưa kịp tạo
            teacher.setFirstName("User");
            teacher.setLastName("Teacher");
        }

        Long loggedTeacherId = teacher.getId();

        // Truy vấn bảng trung gian TeacherSubject để lấy danh sách môn học được phép
        // dạy
        List<Subject> subjects = em.createQuery(
                "SELECT s FROM Subject s JOIN TeacherSubject ts ON s.id = ts.subjectId WHERE ts.teacherId = :tid",
                Subject.class)
                .setParameter("tid", loggedTeacherId)
                .getResultList();

        List<Semester> semesters = em.createQuery("SELECT s FROM Semester s ORDER BY s.startDate DESC", Semester.class)
                .getResultList();

        return classesTemplate.data("classes", classes)
                .data("subjects", subjects)
                .data("semesters", semesters)
                .data("user", teacher) // Truyền thông tin user thực tế ra giao diện
                .data("currentPage", page)
                .data("totalPages", totalPages)
                .render();
    }

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response addClassroom(
            @CookieParam("logged_in_username") String cookieUsername,
            @FormParam("subjectId") Long subjectId,
            @FormParam("subjectName") String subjectName,
            @FormParam("classCode") String classCode,
            @FormParam("semesterId") Long semesterId,
            @FormParam("description") String description) {

        String username = cookieUsername != null ? cookieUsername : "teacher_toan";
        Student creator = null;
        try {
            creator = em.createQuery("SELECT s FROM Student s WHERE s.username = :username", Student.class)
                    .setParameter("username", username)
                    .setMaxResults(1).getSingleResult();
        } catch (Exception e) {
        }

        Classroom classroom = new Classroom();
        // Tự động ghép tên môn học và mã nhóm để tạo tên Lớp
        classroom.setName(subjectName + " (" + classCode + ")");
        classroom.setSubjectId(subjectId);
        classroom.setSubjectName(subjectName);
        classroom.setDescription(description);

        if (semesterId != null) {
            Semester sem = em.find(Semester.class, semesterId);
            if (sem != null) {
                classroom.setSemesterId(sem.getId());
                classroom.setSemesterName(sem.getName());
                classroom.setStartDate(sem.getStartDate());
                classroom.setEndDate(sem.getEndDate());
            }
        } else {
            // Tự động gán cho Học kỳ hiện hành hoặc sắp tới
            String currentDate = java.time.LocalDate.now().toString();
            List<Semester> sems = em
                    .createQuery("SELECT s FROM Semester s WHERE s.endDate >= :currentDate ORDER BY s.startDate ASC",
                            Semester.class)
                    .setParameter("currentDate", currentDate)
                    .getResultList();
            if (!sems.isEmpty()) {
                classroom.setSemesterId(sems.get(0).getId());
                classroom.setSemesterName(sems.get(0).getName());
                classroom.setStartDate(sems.get(0).getStartDate());
                classroom.setEndDate(sems.get(0).getEndDate());
            } else {
                List<Semester> allSems = em
                        .createQuery("SELECT s FROM Semester s ORDER BY s.startDate DESC", Semester.class)
                        .getResultList();
                if (!allSems.isEmpty()) {
                    classroom.setSemesterId(allSems.get(0).getId());
                    classroom.setSemesterName(allSems.get(0).getName());
                    classroom.setStartDate(allSems.get(0).getStartDate());
                    classroom.setEndDate(allSems.get(0).getEndDate());
                }
            }
        }

        if (creator != null) {
            classroom.setTeacherId(creator.getId());
        }
        em.persist(classroom);

        return Response.seeOther(URI.create("/classes")).build();
    }

    @POST
    @Path("/delete/{id}")
    @Transactional
    public Response deleteClassroom(@PathParam("id") Long id) {
        Classroom classroom = em.find(Classroom.class, id);
        if (classroom != null) {
            em.remove(classroom);
        }
        return Response.seeOther(URI.create("/classes")).build();
    }

    @POST
    @Path("/edit")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response editClassroom(
            @FormParam("id") Long id,
            @FormParam("name") String name,
            @FormParam("semesterId") Long semesterId,
            @FormParam("description") String description) {
        Classroom classroom = em.find(Classroom.class, id);
        if (classroom != null) {
            classroom.setName(name);
            classroom.setDescription(description);

            if (semesterId != null) {
                Semester sem = em.find(Semester.class, semesterId);
                if (sem != null) {
                    classroom.setSemesterId(sem.getId());
                    classroom.setSemesterName(sem.getName());
                    classroom.setStartDate(sem.getStartDate());
                    classroom.setEndDate(sem.getEndDate());
                }
            } else if (classroom.getSemesterId() == null) {
                String currentDate = java.time.LocalDate.now().toString();
                List<Semester> sems = em
                        .createQuery(
                                "SELECT s FROM Semester s WHERE s.endDate >= :currentDate ORDER BY s.startDate ASC",
                                Semester.class)
                        .setParameter("currentDate", currentDate)
                        .getResultList();
                if (!sems.isEmpty()) {
                    classroom.setSemesterId(sems.get(0).getId());
                    classroom.setSemesterName(sems.get(0).getName());
                    classroom.setStartDate(sems.get(0).getStartDate());
                    classroom.setEndDate(sems.get(0).getEndDate());
                } else {
                    List<Semester> allSems = em
                            .createQuery("SELECT s FROM Semester s ORDER BY s.startDate DESC", Semester.class)
                            .getResultList();
                    if (!allSems.isEmpty()) {
                        classroom.setSemesterId(allSems.get(0).getId());
                        classroom.setSemesterName(allSems.get(0).getName());
                        classroom.setStartDate(allSems.get(0).getStartDate());
                        classroom.setEndDate(allSems.get(0).getEndDate());
                    }
                }
            }
            em.merge(classroom);
        }
        return Response.seeOther(URI.create("/classes")).build();
    }
}