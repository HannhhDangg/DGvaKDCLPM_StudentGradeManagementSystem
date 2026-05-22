package com.example.studentgrade.controller;

import com.example.studentgrade.model.Student;
import com.example.studentgrade.repository.StudentRepository;
import com.example.studentgrade.service.StudentService;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;

@Path("/")
public class StudentController {

    @Inject
    Template index;
    @Inject
    StudentService studentService;
    @Inject
    StudentRepository repository;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getIndexPage() {
        return index.data("students", repository.listAll());
    }

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addStudent(@FormParam("name") String name, @FormParam("score") double score) {
        try {
            Student s = new Student();
            s.setName(name);
            s.setScore(score);
            studentService.saveStudent(s);
        } catch (IllegalArgumentException e) {
            System.err.println("Lỗi thêm sinh viên (Dữ liệu không hợp lệ): " + e.getMessage());
        }
        return Response.seeOther(URI.create("/")).build();
    }
}
