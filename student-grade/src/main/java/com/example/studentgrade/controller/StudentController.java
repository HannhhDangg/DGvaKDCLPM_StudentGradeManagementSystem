package com.example.studentgrade.controller;

import com.example.studentgrade.model.Student;
import com.example.studentgrade.repository.StudentRepository;
import com.example.studentgrade.service.StudentService;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Path("/")
@Blocking
public class StudentController {

    // Inject giao diện index.html làm trang chủ mới
    @Inject
    @Location("index")
    Template index;
    @Inject
    @Location("login")
    Template login;
    @Inject
    @Location("dashboard")
    Template dashboard;
    @Inject
    @Location("teacher")
    Template teacher;
    @Inject
    @Location("student")
    Template student;

    @Inject
    StudentService studentService;
    @Inject
    StudentRepository repository;

    // Route mặc định trang chủ "/" gọi trang index mới
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String root() {
        return index.render();
    }

    @GET
    @Path("/login")
    @Produces(MediaType.TEXT_HTML)
    public String getLoginPage(@QueryParam("error") String error) {
        // Truyền error (nếu có)
        return login.data("error", error != null ? "true" : "").render();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response doLogin(@FormParam("username") String username, @FormParam("password") String password) {
        Student user = repository.findByUsername(username);

        if (user != null && user.getPassword().equals(password)) {
            if ("ADMIN".equals(user.getRole()))
                return Response.seeOther(URI.create("/dashboard")).build();
            if ("TEACHER".equals(user.getRole()))
                return Response.seeOther(URI.create("/teacher")).build();
            if ("STUDENT".equals(user.getRole()))
                return Response.seeOther(URI.create("/student")).build();
        }

        // Backdoor cho Admin để dễ test
        if ("Admin".equals(username) && "123456".equals(password)) {
            return Response.seeOther(URI.create("/dashboard")).build();
        }
        // Backdoor cho Teacher để dễ test (bắt đầu bằng giangvien...)
        if (username != null && username.startsWith("giangvien") && "987654321".equals(password)) {
            return Response.seeOther(URI.create("/teacher")).build();
        }
        // Backdoor cho Student để dễ test (bắt đầu bằng student...)
        if (username != null && username.startsWith("student") && "12345678".equals(password)) {
            return Response.seeOther(URI.create("/student")).build();
        }
        return Response.seeOther(URI.create("/login?error=true")).build();
    }

    @GET
    @Path("/dashboard")
    @Produces(MediaType.TEXT_HTML)
    public String getDashboard() {
        // Cung cấp dữ liệu cho Chart.js và các thẻ thống kê
        return dashboard
                .data("total", studentService.getTotalUsers())
                .data("average", "8.5")
                .data("passRatio", "92.5")
                .data("grades", Collections.emptyMap())
                .render();
    }

    @GET
    @Path("/teacher")
    @Produces(MediaType.TEXT_HTML)
    public String getTeacherDashboard() {
        List<StudentGradeDTO> mockStudents = List.of(
                new StudentGradeDTO(1L, "Nguyễn Văn Sinh Viên", "sv@domain.com", 8.5, "123456"),
                new StudentGradeDTO(2L, "Trần Thị B", "tranb@domain.com", null, "123456"));
        return teacher.data("students", mockStudents)
                .data("totalClasses", 2)
                .data("totalStudents", 45)
                .render();
    }

    @GET
    @Path("/student")
    @Produces(MediaType.TEXT_HTML)
    public String getStudentDashboard() {
        List<GradeDTO> mockGrades = List.of(
                new GradeDTO("INT101", "Lập trình cơ bản", 3, 8.5),
                new GradeDTO("INT102", "Cơ sở dữ liệu", 3, 7.0),
                new GradeDTO("INT103", "Mạng máy tính", 3, null));
        return student.data("grades", mockGrades).data("gpa", 3.2).render();
    }

    public static class GradeDTO {
        public String subjectCode;
        public String subjectName;
        public int credits;
        public Double score;

        public GradeDTO(String subjectCode, String subjectName, int credits, Double score) {
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.credits = credits;
            this.score = score;
        }

        public String getLetterGrade() {
            if (score == null)
                return "N/A";
            if (score >= 8.5)
                return "Giỏi (A)";
            if (score >= 7.0)
                return "Khá (B)";
            if (score >= 5.5)
                return "Trung bình (C)";
            if (score >= 4.0)
                return "Yếu (D)";
            return "Trượt (F)";
        }

        public String getBadgeClass() {
            return score == null ? "na" : (score >= 7.0 ? "good" : "");
        }
    }

    public static class StudentGradeDTO {
        public Long id;
        public String name;
        public String email;
        public String password;
        public Double score;

        public StudentGradeDTO(Long id, String name, String email, Double score, String password) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.score = score;
            this.password = password;
        }

        public String getLetterGrade() {
            if (score == null)
                return "N/A";
            if (score >= 8.5)
                return "Giỏi (B+)";
            return "Khá (B)";
        }

        public String getBadgeClass() {
            return score == null ? "na" : "good";
        }
    }
}