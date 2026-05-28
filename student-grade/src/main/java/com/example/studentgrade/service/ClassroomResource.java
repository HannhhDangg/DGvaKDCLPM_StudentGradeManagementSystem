package com.example.studentgrade.service;

import com.example.studentgrade.model.Classroom;
import com.example.studentgrade.model.Student;
import com.example.studentgrade.model.Semester;
import com.example.studentgrade.model.Subject;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

@Path("/classes")
@Produces(MediaType.TEXT_HTML)
public class ClassroomResource {

    @Inject
    ClassroomService classroomService;

    @Inject
    @Location("classes")
    Template classesTemplate;

    @GET
    public String getClassesPage(@CookieParam("logged_in_username") String cookieUsername,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("subjectId") Long subjectId,
            @QueryParam("semesterId") Long semesterId) {

        String username = cookieUsername != null ? cookieUsername : "teacher_toan";
        Student teacher = classroomService.getTeacherByUsername(username);

        Long loggedTeacherId = teacher.getId();

        int pageSize = 10;
        Long totalRecords = classroomService.countClassesByTeacher(loggedTeacherId, subjectId, semesterId);
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

        if (page < 1)
            page = 1;
        if (page > totalPages && totalPages > 0)
            page = totalPages;

        List<Classroom> classesOnPage = classroomService.getClassesByTeacher(loggedTeacherId, subjectId, semesterId,
                page, pageSize);
        List<Subject> subjects = classroomService.getSubjectsByTeacher(loggedTeacherId);
        List<Semester> semesters = classroomService.getAllSemesters();

        // Phân loại lớp học thành các nhóm: sắp diễn ra, đang diễn ra và đã kết thúc
        // để giao diện có thể hiển thị rõ ràng hơn cho giáo viên.
        List<Classroom> upcomingClasses = new ArrayList<>();
        List<Classroom> ongoingClasses = new ArrayList<>();
        List<Classroom> pastClasses = new ArrayList<>();
        String currentDate = LocalDate.now().toString();

        for (Classroom c : classesOnPage) {
            String startDate = c.getStartDate();
            String endDate = c.getEndDate();

            if (endDate != null && !endDate.isEmpty() && endDate.compareTo(currentDate) < 0) {
                pastClasses.add(c);
            } else if (startDate != null && !startDate.isEmpty() && startDate.compareTo(currentDate) > 0) {
                upcomingClasses.add(c);
            } else {
                ongoingClasses.add(c);
            }
        }

        return classesTemplate.data("classes", classesOnPage) // Giữ lại "classes" để tương thích ngược với template cũ
                .data("upcomingClasses", upcomingClasses)
                .data("ongoingClasses", ongoingClasses)
                .data("pastClasses", pastClasses)
                .data("subjects", subjects)
                .data("semesters", semesters)
                .data("user", teacher) // Truyền thông tin user thực tế ra giao diện
                .data("currentPage", page)
                .data("totalPages", totalPages)
                .data("selectedSubjectId", subjectId)
                .data("selectedSemesterId", semesterId)
                .render();
    }

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addClassroom(
            @CookieParam("logged_in_username") String cookieUsername,
            @FormParam("subjectId") Long subjectId,
            @FormParam("subjectName") String subjectName,
            @FormParam("classCode") String classCode,
            @FormParam("semesterId") Long semesterId,
            @FormParam("description") String description) {

        String username = cookieUsername != null ? cookieUsername : "teacher_toan";
        classroomService.addClassroom(username, subjectId, subjectName, classCode, semesterId, description);

        return Response.seeOther(URI.create("/classes")).build();
    }

    @POST
    @Path("/delete/{id}")
    public Response deleteClassroom(@CookieParam("logged_in_username") String cookieUsername,
            @PathParam("id") Long id) {
        String username = cookieUsername != null ? cookieUsername : "teacher_toan";
        classroomService.deleteClassroom(username, id);
        return Response.seeOther(URI.create("/classes")).build();
    }

    @POST
    @Path("/edit")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response editClassroom(
            @CookieParam("logged_in_username") String cookieUsername,
            @FormParam("id") Long id,
            @FormParam("name") String name,
            @FormParam("semesterId") Long semesterId,
            @FormParam("description") String description) {

        String username = cookieUsername != null ? cookieUsername : "teacher_toan";
        classroomService.editClassroom(username, id, name, semesterId, description);
        return Response.seeOther(URI.create("/classes")).build();
    }
}