package com.example.studentgrade.controller;

import com.example.studentgrade.model.Student;
import com.example.studentgrade.repository.StudentRepository;
import com.example.studentgrade.service.StudentService;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;

@Path("/")
@Blocking // BẮT BUỘC PHẢI CÓ ĐỂ TRÁNH LỖI CRASH KHI ĐỌC DATABASE
public class StudentController {

    @Inject
    @Location("login")
    Template login;
    @Inject
    @Location("dashboard")
    Template dashboard;
    @Inject
    @Location("teacher")
    Template teacher; // Cần tạo file teacher.html
    @Inject
    @Location("student")
    Template student; // Cần tạo file student.html

    @Inject
    StudentService studentService;
    @Inject
    StudentRepository repository;

    @GET
    public Response root() {
        return Response.seeOther(URI.create("/login")).build();
    }

    @GET
    @Path("/login")
    @Produces(MediaType.TEXT_HTML)
    public String getLoginPage(@QueryParam("error") String error) {
        return login.data("error", error != null).render();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response doLogin(@FormParam("username") String username, @FormParam("password") String password) {
        // Kiểm tra tài khoản trong Database
        Student user = repository.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            if ("ADMIN".equals(user.getRole()))
                return Response.seeOther(URI.create("/dashboard")).build();
            if ("TEACHER".equals(user.getRole()))
                return Response.seeOther(URI.create("/teacher")).build();
            if ("STUDENT".equals(user.getRole()))
                return Response.seeOther(URI.create("/student")).build();
        }

        // Cửa hậu (Backdoor) cho Admin phòng trường hợp DB trống
        if ("Admin".equals(username) && "123456".equals(password)) {
            return Response.seeOther(URI.create("/dashboard")).build();
        }
        return Response.seeOther(URI.create("/login?error=true")).build();
    }

    @GET
    @Path("/dashboard")
    @Produces(MediaType.TEXT_HTML)
    public String getDashboard() {
        return dashboard.data("total", studentService.getTotalUsers())
                .data("average", "0.00").data("passRatio", "0.0").data("grades", Collections.emptyMap()).render();
    }

    @GET
    @Path("/teacher")
    @Produces(MediaType.TEXT_HTML)
    public String getTeacherDashboard() {
        return teacher.data("dummy", "Data").render();
    }

    @GET
    @Path("/student")
    @Produces(MediaType.TEXT_HTML)
    public String getStudentDashboard() {
        return student.data("dummy", "Data").render();
    }
}