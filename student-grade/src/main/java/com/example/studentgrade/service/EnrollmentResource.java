package com.example.studentgrade.service;

import com.example.studentgrade.model.Classroom;
import com.example.studentgrade.model.Enrollment;
import com.example.studentgrade.model.Student;
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
            @CookieParam("logged_in_username") String cookieUsername,
            @FormParam("classroomId") String classroomIdStr) {

        String username = cookieUsername != null ? cookieUsername : "student_1";
        Student student = null;
        try {
            student = em.createQuery("SELECT s FROM Student s WHERE s.username = :username", Student.class)
                    .setParameter("username", username)
                    .setMaxResults(1).getSingleResult();
        } catch (Exception e) {
        }

        if (student == null || student.getId() == null) {
            student = new Student();
            student.setUsername(username);
            student.setLastName("Sinh viên");
            student.setFirstName("Tự động");
            student.setRole("STUDENT");
            em.persist(student);
        }
        Long studentId = student.getId();

        if (classroomIdStr == null || classroomIdStr.trim().isEmpty() || "null".equals(classroomIdStr)) {
            return Response.seeOther(URI.create("/student/enroll?error=notfound")).build();
        }

        Long classroomId;
        try {
            classroomId = Long.parseLong(classroomIdStr);
        } catch (NumberFormatException ex) {
            return Response.seeOther(URI.create("/student/enroll?error=notfound")).build();
        }

        Classroom classroom = em.find(Classroom.class, classroomId);
        if (classroom == null) {
            return Response.seeOther(URI.create("/student/enroll?error=notfound")).build();
        }

        // Kiểm tra thời gian đăng ký (đóng trước khi lớp bắt đầu 7 ngày)
        if (classroom.getStartDate() != null && !classroom.getStartDate().isEmpty()) {
            try {
                java.time.LocalDate startDate = java.time.LocalDate.parse(classroom.getStartDate());
                if (java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), startDate) < 7) {
                    return Response.seeOther(URI.create("/student/enroll?error=deadline")).build();
                }
            } catch (Exception e) {
            }
        }

        // Kiểm tra giới hạn 10 sinh viên trong một lớp (chỉ tính những bạn đã được
        // duyệt)
        Long count = em
                .createQuery("SELECT COUNT(e) FROM Enrollment e WHERE e.classroomId = :cid AND e.status = 'APPROVED'",
                        Long.class)
                .setParameter("cid", classroomId)
                .getSingleResult();

        if (count >= 10) {
            return Response.seeOther(URI.create("/student/enroll?error=limit")).build();
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
            enrollment.setStatus("PENDING");
            em.persist(enrollment);
        }

        return Response.seeOther(URI.create("/student/enroll")).build();
    }

    @POST
    @Path("/approve")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response approveEnrollment(@FormParam("enrollmentId") Long enrollmentId,
            @FormParam("status") String status) {
        Enrollment e = em.find(Enrollment.class, enrollmentId);
        if (e != null) {
            String oldStatus = e.getStatus();
            if ("APPROVED".equals(status) && !"APPROVED".equals(oldStatus)) {
                Long count = em.createQuery(
                        "SELECT COUNT(en) FROM Enrollment en WHERE en.classroomId = :cid AND en.status = 'APPROVED'",
                        Long.class)
                        .setParameter("cid", e.getClassroomId())
                        .getSingleResult();
                if (count >= 10)
                    return Response.seeOther(URI.create("/teacher/enrollments?error=limit")).build();
            }
            e.setStatus(status);
            em.merge(e);

            // Tự động tính toán & cập nhật Học phí cho Sinh viên
            if ("APPROVED".equals(status) && !"APPROVED".equals(oldStatus)) {
                updateTuitionFee(e.getStudentId(), e.getClassroomId(), 1); // Sinh viên được duyệt -> Tăng công nợ
            } else if ("APPROVED".equals(oldStatus) && !"APPROVED".equals(status)) {
                updateTuitionFee(e.getStudentId(), e.getClassroomId(), -1); // Bị huỷ duyệt -> Trừ bớt công nợ
            }
        }
        return Response.seeOther(URI.create("/teacher/enrollments")).build();
    }

    private void updateTuitionFee(Long studentId, Long classroomId, int sign) {
        Classroom c = em.find(Classroom.class, classroomId);
        if (c == null) return;
        com.example.studentgrade.model.Subject sub = em.find(com.example.studentgrade.model.Subject.class, c.getSubjectId());
        if (sub == null) return;
        
        int credits = sub.getCredits();
        double amount = 0.0;
        String category = sub.getCategory();
        if ("TECHNICAL".equals(category)) {
            amount = credits * 750000.0;
        } else if ("BASIC".equals(category)) {
            amount = credits * 650000.0;
        } else if ("GENERAL".equals(category)) {
            amount = credits * 600000.0;
        } else {
            amount = credits * 750000.0;
        }
        
        java.util.List<com.example.studentgrade.model.TuitionFee> fees = em.createQuery(
            "SELECT t FROM TuitionFee t WHERE t.studentId = :sid AND t.semesterId = :semId",
            com.example.studentgrade.model.TuitionFee.class)
            .setParameter("sid", studentId)
            .setParameter("semId", c.getSemesterId())
            .getResultList();
            
        com.example.studentgrade.model.TuitionFee fee;
        if (fees.isEmpty()) {
            if (sign < 0) return;
            fee = new com.example.studentgrade.model.TuitionFee();
            fee.setStudentId(studentId);
            fee.setSemesterId(c.getSemesterId());
            fee.setTotalCredits(credits);
            fee.setTotalAmount(amount);
            fee.setPaidAmount(0.0);
            fee.setStatus("UNPAID");
            em.persist(fee);
        } else {
            fee = fees.get(0);
            fee.setTotalCredits(fee.getTotalCredits() + (credits * sign));
            fee.setTotalAmount(fee.getTotalAmount() + (amount * sign));
            
            if (fee.getTotalAmount() <= 0) {
                em.remove(fee);
                return;
            }
            
            if (fee.getPaidAmount() >= fee.getTotalAmount()) {
                fee.setStatus("PAID");
            } else {
                fee.setStatus("UNPAID");
            }
            em.merge(fee);
        }
    }
}