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
    public String getSubjectsPage() {
        List<Subject> subjects = em.createQuery("SELECT s FROM Subject s", Subject.class).getResultList();
        return subjectsTemplate.data("subjects", subjects).render();
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