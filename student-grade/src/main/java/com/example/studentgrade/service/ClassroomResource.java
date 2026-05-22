package com.example.studentgrade.service;

import com.example.studentgrade.model.Classroom;
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
    public String getClassesPage() {
        List<Classroom> classes = em.createQuery("SELECT c FROM Classroom c", Classroom.class).getResultList();
        return classesTemplate.data("classes", classes).render();
    }

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response addClassroom(@FormParam("name") String name,
            @FormParam("description") String description) {
        Classroom classroom = new Classroom();
        classroom.setName(name);
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
}