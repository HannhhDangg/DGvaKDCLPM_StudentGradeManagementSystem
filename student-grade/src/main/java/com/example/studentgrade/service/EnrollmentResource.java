package com.example.studentgrade.service;

import com.example.studentgrade.model.Enrollment;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;

@Path("/enrollments")
@Produces(MediaType.TEXT_HTML)
public class EnrollmentResource {

    @Inject
    EntityManager em;

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response enrollStudent(
            @FormParam("studentId") Long studentId,
            @FormParam("classroomId") Long classroomId) {

        // Kiểm tra giới hạn 30 sinh viên trong một lớp
        Long count = em.createQuery("SELECT COUNT(e) FROM Enrollment e WHERE e.classroomId = :cid", Long.class)
                .setParameter("cid", classroomId)
                .getSingleResult();

        if (count >= 30) {
            // Lớp học đã đầy (Trong thực tế ta sẽ truyền kèm biến báo lỗi ra UI)
            return Response.seeOther(URI.create("/classes?error=limit")).build();
        }

        // Kiểm tra sinh viên đã có trong lớp chưa
        Long existing = em
                .createQuery("SELECT COUNT(e) FROM Enrollment e WHERE e.classroomId = :cid AND e.studentId = :sid",
                        Long.class)
                .setParameter("cid", classroomId)
                .setParameter("sid", studentId)
                .getSingleResult();

        if (existing == 0) {
            Enrollment enrollment = new Enrollment();
            enrollment.setStudentId(studentId);
            enrollment.setClassroomId(classroomId);
            em.persist(enrollment);
        }

        return Response.seeOther(URI.create("/classes")).build();
    }
}