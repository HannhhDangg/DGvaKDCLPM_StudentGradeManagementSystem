package com.example.studentgrade.service;

import com.example.studentgrade.model.Student;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

@Path("/users")
@Produces(MediaType.TEXT_HTML)
public class UserResource {

    // Gọi đến file users.html mới
    @Inject
    @Location("users")
    Template usersTemplate;
    @Inject
    StudentService userService;

    @GET
    public String getUsers(@QueryParam("search") String search) {
        List<Student> users;
        if (search != null && !search.trim().isEmpty()) {
            users = userService.searchUsers(search);
        } else {
            users = userService.getAllUsers();
        }

        String safeSearch = search == null ? "" : search;

        return usersTemplate
                .data("users", users)
                .data("search", safeSearch)
                .data("error", "") // Tránh lỗi Null trong template
                .render();
    }

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addUser(@FormParam("lastName") String lastName,
            @FormParam("firstName") String firstName,
            @FormParam("username") String username,
            @FormParam("email") String email,
            @FormParam("role") String role,
            @FormParam("password") String password) {
        try {
            Student user = new Student();
            user.setLastName(lastName);
            user.setFirstName(firstName);
            user.setUsername(username);
            user.setEmail(email);
            user.setRole(role);
            user.setPassword(password);

            userService.saveUser(user);
            return Response.seeOther(URI.create("/users")).build();
        } catch (Exception e) {
            return Response.ok(usersTemplate
                    .data("users", userService.getAllUsers())
                    .data("search", "")
                    .data("error", "Lỗi khi thêm người dùng. Vui lòng kiểm tra lại.")
                    .render()).build();
        }
    }

    @POST
    @Path("/delete/{id}")
    public Response deleteUser(@PathParam("id") Long id) {
        userService.deleteUser(id);
        return Response.seeOther(URI.create("/users")).build();
    }
}