package com.example.studentgrade.service;

import com.example.studentgrade.model.Classroom;
import com.example.studentgrade.model.Semester;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Path("/semesters")
@Produces(MediaType.TEXT_HTML)
@Blocking
public class SemesterResource {

    @Inject
    EntityManager em;

    @Inject
    @Location("semesters")
    Template semestersTemplate;

    @GET
    public String getSemestersPage(@QueryParam("search") String search, @QueryParam("status") String status,
            @QueryParam("error") String error,
            @QueryParam("success") String success,
            @QueryParam("page") @DefaultValue("1") int page) {

        int pageSize = 10;

        String qlString = "SELECT s FROM Semester s";
        Map<String, Object> params = new HashMap<>();
        List<String> whereClauses = new ArrayList<>();
        String currentDate = java.time.LocalDate.now().toString();

        if (search != null && !search.trim().isEmpty()) {
            whereClauses.add("lower(s.name) LIKE :search");
            params.put("search", "%" + search.toLowerCase() + "%");
        }

        if ("ongoing".equals(status)) {
            whereClauses.add("s.endDate >= :currentDate");
            params.put("currentDate", currentDate);
        } else if ("completed".equals(status)) {
            whereClauses.add("s.endDate < :currentDate");
            params.put("currentDate", currentDate);
        }

        if (!whereClauses.isEmpty()) {
            qlString += " WHERE " + String.join(" AND ", whereClauses);
        }

        // Đếm tổng số lượng bản ghi để phân trang
        String countQlString = "SELECT COUNT(s) FROM Semester s"
                + (whereClauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", whereClauses));
        jakarta.persistence.TypedQuery<Long> countQuery = em.createQuery(countQlString, Long.class);
        params.forEach(countQuery::setParameter);
        Long totalRecords = countQuery.getSingleResult();
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

        if (page < 1)
            page = 1;
        if (page > totalPages && totalPages > 0)
            page = totalPages;

        qlString += " ORDER BY s.startDate DESC";

        jakarta.persistence.TypedQuery<Semester> query = em.createQuery(qlString, Semester.class);
        params.forEach(query::setParameter);
        query.setFirstResult((page - 1) * pageSize);
        query.setMaxResults(pageSize);
        List<Semester> semesters = query.getResultList();

        Map<Long, Long> classCounts = new HashMap<>();
        Map<Long, Long> completedCounts = new HashMap<>();
        Map<Long, Integer> progressMap = new HashMap<>();
        Map<Long, List<Classroom>> classesBySemester = new HashMap<>();

        for (Semester s : semesters) {
            Long count = em.createQuery("SELECT COUNT(c) FROM Classroom c WHERE c.semesterId = :sid", Long.class)
                    .setParameter("sid", s.getId())
                    .getSingleResult();
            classCounts.put(s.getId(), count);

            Long completed = em
                    .createQuery(
                            "SELECT COUNT(c) FROM Classroom c WHERE c.semesterId = :sid AND c.endDate < :currentDate",
                            Long.class)
                    .setParameter("sid", s.getId())
                    .setParameter("currentDate", currentDate)
                    .getSingleResult();
            completedCounts.put(s.getId(), completed);

            int progress = count > 0 ? (int) ((completed * 100) / count) : 0;
            progressMap.put(s.getId(), progress);

            List<Classroom> semClasses = em
                    .createQuery("SELECT c FROM Classroom c WHERE c.semesterId = :sid", Classroom.class)
                    .setParameter("sid", s.getId())
                    .getResultList();
            classesBySemester.put(s.getId(), semClasses);
        }

        return semestersTemplate.data("semesters", semesters)
                .data("classCounts", classCounts)
                .data("completedCounts", completedCounts)
                .data("progressMap", progressMap)
                .data("classesBySemester", classesBySemester)
                .data("search", search == null ? "" : search)
                .data("status", status == null ? "all" : status)
                .data("error", error != null ? error : "")
                .data("success", success != null ? success : "")
                .data("currentPage", page)
                .data("totalPages", totalPages).render();
    }

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response addSemester(@FormParam("name") String name, @FormParam("startDate") String startDate,
            @FormParam("endDate") String endDate) {
        Semester semester = new Semester();
        semester.setName(name);
        semester.setStartDate(startDate);
        semester.setEndDate(endDate);
        em.persist(semester);
        return Response.seeOther(URI.create("/semesters")).build();
    }

    @POST
    @Path("/edit")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response editSemester(@FormParam("id") Long id, @FormParam("name") String name,
            @FormParam("startDate") String startDate, @FormParam("endDate") String endDate) {
        Semester semester = em.find(Semester.class, id);
        if (semester != null) {
            semester.setName(name);
            semester.setStartDate(startDate);
            semester.setEndDate(endDate);
            em.merge(semester);
        }
        return Response.seeOther(URI.create("/semesters")).build();
    }

    @POST
    @Path("/delete/{id}")
    @Transactional
    public Response deleteSemester(@PathParam("id") Long id) {
        Semester semester = em.find(Semester.class, id);
        if (semester != null) {
            // Kiểm tra xem có lớp học nào đang dùng học kỳ này không
            Long classCount = em.createQuery("SELECT COUNT(c) FROM Classroom c WHERE c.semesterId = :sid", Long.class)
                    .setParameter("sid", id)
                    .getSingleResult();
            if (classCount > 0) {
                return Response.seeOther(URI.create("/semesters?error=has_classes")).build();
            }
            em.remove(semester);
        }
        return Response.seeOther(URI.create("/semesters")).build();
    }

    @POST
    @Path("/transfer")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response transferClasses(@FormParam("fromSemesterId") Long fromSemesterId,
            @FormParam("toSemesterId") Long toSemesterId) {
        if (fromSemesterId == null || toSemesterId == null || fromSemesterId.equals(toSemesterId)) {
            return Response.seeOther(URI.create("/semesters?error=invalid_transfer")).build();
        }

        Semester toSemester = em.find(Semester.class, toSemesterId);
        if (toSemester == null) {
            return Response.seeOther(URI.create("/semesters?error=not_found")).build();
        }

        List<com.example.studentgrade.model.Classroom> classrooms = em
                .createQuery("SELECT c FROM Classroom c WHERE c.semesterId = :fromId",
                        com.example.studentgrade.model.Classroom.class)
                .setParameter("fromId", fromSemesterId)
                .getResultList();

        for (com.example.studentgrade.model.Classroom c : classrooms) {
            c.setSemesterId(toSemester.getId());
            c.setSemesterName(toSemester.getName());
            c.setStartDate(toSemester.getStartDate());
            c.setEndDate(toSemester.getEndDate());
            em.merge(c);
        }

        return Response.seeOther(URI.create("/semesters?success=transferred")).build();
    }
}