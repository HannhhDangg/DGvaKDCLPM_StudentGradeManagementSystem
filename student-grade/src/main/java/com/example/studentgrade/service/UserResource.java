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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.EntityManager;

@Path("/users")
@Produces(MediaType.TEXT_HTML)
public class UserResource {

    // Gọi đến file users.html mới
    @Inject
    @Location("users")
    Template usersTemplate;
    @Inject
    StudentService userService;
    @Inject
    EntityManager em;

    @GET
    public String getUsers(@QueryParam("search") String search, @QueryParam("role") String role,
            @QueryParam("page") @DefaultValue("1") int page) {
        int pageSize = 10;

        String qlString = "SELECT u FROM Student u";
        String countQlString = "SELECT COUNT(u) FROM Student u";

        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        // Điều kiện 1: Tìm kiếm theo chữ
        if (search != null && !search.trim().isEmpty()) {
            conditions.add(
                    "(lower(u.firstName) LIKE :search OR lower(u.lastName) LIKE :search OR lower(u.username) LIKE :search)");
            params.put("search", "%" + search.toLowerCase() + "%");
        }

        // Điều kiện 2: Lọc theo vai trò (Role)
        if (role != null && !role.trim().isEmpty() && !role.equals("all")) {
            conditions.add("u.role = :role");
            params.put("role", role);
        }

        // Gộp các điều kiện
        if (!conditions.isEmpty()) {
            String whereClause = " WHERE " + String.join(" AND ", conditions);
            qlString += whereClause;
            countQlString += whereClause;
        }

        jakarta.persistence.TypedQuery<Long> countQuery = em.createQuery(countQlString, Long.class);
        params.forEach(countQuery::setParameter);

        Long totalRecords = countQuery.getSingleResult();
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

        if (page < 1)
            page = 1;
        if (page > totalPages && totalPages > 0)
            page = totalPages;

        qlString += " ORDER BY u.id DESC";
        jakarta.persistence.TypedQuery<Student> query = em.createQuery(qlString, Student.class);
        params.forEach(query::setParameter);

        List<Student> users = query
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();

        return usersTemplate.data("users", users)
                .data("search", search == null ? "" : search)
                .data("role", role == null ? "all" : role)
                .data("currentPage", page)
                .data("totalPages", totalPages)
                .data("error", "") // Tránh lỗi null báo ngoài UI
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