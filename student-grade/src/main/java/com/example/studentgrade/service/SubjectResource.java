package com.example.studentgrade.service;

import com.example.studentgrade.model.Subject;
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

@Path("/subjects")
@Produces(MediaType.TEXT_HTML)
public class SubjectResource {

    @Inject
    EntityManager em;

    @Inject
    @Location("subjects")
    Template subjectsTemplate;

    @GET
    public String getSubjectsPage(@QueryParam("page") @DefaultValue("1") int page) {
        int pageSize = 10;

        Long totalRecords = em.createQuery("SELECT COUNT(s) FROM Subject s", Long.class).getSingleResult();
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

        if (page < 1)
            page = 1;
        if (page > totalPages && totalPages > 0)
            page = totalPages;

        List<Subject> subjects = em.createQuery("SELECT s FROM Subject s ORDER BY s.id DESC", Subject.class)
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
        return subjectsTemplate.data("subjects", subjects)
                .data("currentPage", page)
                .data("totalPages", totalPages).render();
    }

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response addSubject(@FormParam("code") String code,
            @FormParam("name") String name,
            @FormParam("description") String description) {
        Subject subject = new Subject();
        subject.setCode(code);
        subject.setName(name);
        subject.setDescription(description);
        em.persist(subject);

        return Response.seeOther(URI.create("/subjects")).build();
    }

    @POST
    @Path("/delete/{id}")
    @Transactional
    public Response deleteSubject(@PathParam("id") Long id) {
        Subject subject = em.find(Subject.class, id);
        if (subject != null) {
            em.remove(subject);
        }
        return Response.seeOther(URI.create("/subjects")).build();
    }
}