package com.example.studentgrade.service;

import com.example.studentgrade.model.Classroom;
import com.example.studentgrade.model.Student;
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
    public String getClassesPage(@CookieParam("logged_in_username") String cookieUsername) {
        List<Classroom> classes = em.createQuery("SELECT c FROM Classroom c", Classroom.class).getResultList();

        String username = cookieUsername != null ? cookieUsername : "teacher_toan";
        Student teacher;
        try {
            teacher = em.createQuery("SELECT s FROM Student s WHERE s.username = :username", Student.class)
                    .setParameter("username", username)
                    .setMaxResults(1).getSingleResult();
        } catch (Exception e) {
            teacher = new Student();
            teacher.setId(0L); // Tránh lỗi null nếu DB chưa kịp tạo
        }

        Long loggedTeacherId = teacher.getId();

        // Truy vấn bảng trung gian TeacherSubject để lấy danh sách môn học được phép
        // dạy
        List<Subject> subjects = em.createQuery(
                "SELECT s FROM Subject s JOIN TeacherSubject ts ON s.id = ts.subjectId WHERE ts.teacherId = :tid",
                Subject.class)
                .setParameter("tid", loggedTeacherId)
                .getResultList();

        return classesTemplate.data("classes", classes)
                .data("subjects", subjects)
                .data("user", teacher) // Truyền thông tin user thực tế ra giao diện
                .render();
    }

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response addClassroom(
            @FormParam("subjectId") Long subjectId,
            @FormParam("subjectName") String subjectName,
            @FormParam("classCode") String classCode,
            @FormParam("description") String description) {
        Classroom classroom = new Classroom();
        // Tự động ghép tên môn học và mã nhóm để tạo tên Lớp
        classroom.setName(subjectName + " (" + classCode + ")");
        classroom.setSubjectId(subjectId);
        classroom.setSubjectName(subjectName);
        classroom.setDescription(description);
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
            @FormParam("startDate") String startDate,
            @FormParam("endDate") String endDate,
            @FormParam("description") String description) {
        Classroom classroom = em.find(Classroom.class, id);
        if (classroom != null) {
            classroom.setName(name);
            classroom.setStartDate(startDate);
            classroom.setEndDate(endDate);
            classroom.setDescription(description);
            em.merge(classroom);
        }
        return Response.seeOther(URI.create("/classes")).build();
    }
}