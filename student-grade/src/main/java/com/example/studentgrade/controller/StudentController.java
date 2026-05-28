package com.example.studentgrade.controller;

import com.example.studentgrade.model.Student;
import com.example.studentgrade.model.Classroom;
import com.example.studentgrade.model.Enrollment;
import com.example.studentgrade.model.Grade;
import com.example.studentgrade.model.Subject;
import com.example.studentgrade.model.TuitionFee;
import com.example.studentgrade.model.TuitionFee;
import com.example.studentgrade.model.PaymentTransaction;
import com.example.studentgrade.repository.SubjectRepository;
import com.example.studentgrade.repository.StudentRepository;
import com.example.studentgrade.service.StudentService;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
    @Location("student-classes")
    Template studentClasses;
    @Inject
    @Location("student-transcript")
    Template studentTranscript;
    @Inject
    @Location("student-enroll")
    Template studentEnroll;
    @Inject
    @Location("teacher-enrollments")
    Template teacherEnrollments;
    @Inject
    @Location("student-tuition")
    Template studentTuition;

    @Inject
    StudentService studentService;
    @Inject
    StudentRepository repository;
    @Inject
    SubjectRepository subjectRepository;
    @Inject
    EntityManager em;

    private Student getLoggedStudent(String cookieUsername) {
        String username = cookieUsername != null ? cookieUsername : "student_1";
        Student s = repository.findByUsername(username);
        if (s == null) {
            s = new Student();
            s.setFirstName("User");
            s.setLastName("Student");
            s.setUsername(username);
        }
        return s;
    }

    private Student getLoggedTeacher(String cookieUsername) {
        String username = cookieUsername != null ? cookieUsername : "teacher_toan";
        Student t = repository.findByUsername(username);
        if (t == null) {
            t = new Student();
            t.setFirstName("User");
            t.setLastName("Teacher");
            t.setUsername(username);
        }
        return t;
    }

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
    @Transactional
    public Response doLogin(@FormParam("username") String username, @FormParam("password") String password) {
        Student user = repository.findByUsername(username);
        NewCookie cookie = new NewCookie.Builder("logged_in_username").value(username).path("/").build();

        if (user != null && user.getPassword().equals(password)) {
            if ("ADMIN".equalsIgnoreCase(user.getRole()))
                return Response.seeOther(URI.create("/dashboard")).cookie(cookie).build();
            if ("TEACHER".equalsIgnoreCase(user.getRole()))
                return Response.seeOther(URI.create("/teacher")).cookie(cookie).build();
            if ("STUDENT".equalsIgnoreCase(user.getRole()))
                return Response.seeOther(URI.create("/student")).cookie(cookie).build();
        }

        // Backdoor cho Admin để dễ test
        if ("admin".equals(username) && "123456".equals(password)) {
            if (user == null) {
                user = new Student();
                user.setUsername(username);
                user.setPassword(password);
                user.setLastName("Admin");
                user.setFirstName("Hệ thống");
                user.setRole("ADMIN");
                repository.persist(user);
            }
            return Response.seeOther(URI.create("/dashboard")).cookie(cookie).build();
        }
        // Backdoor cho Teacher để dễ test (bắt đầu bằng giangvien...)
        if (username != null && username.startsWith("teacher_") && "123456".equals(password)) {
            if (user == null) {
                user = new Student();
                user.setUsername(username);
                user.setPassword(password);
                user.setLastName("Giáo viên");
                user.setFirstName(username);
                user.setRole("TEACHER");
                repository.persist(user);
            }
            return Response.seeOther(URI.create("/teacher")).cookie(cookie).build();
        }
        // Backdoor cho Student để dễ test (bắt đầu bằng student...)
        if (username != null && username.startsWith("student_") && "123456".equals(password)) {
            if (user == null) {
                user = new Student();
                user.setUsername(username);
                user.setPassword(password);
                user.setLastName("Sinh viên");
                user.setFirstName(username);
                user.setRole("STUDENT");
                repository.persist(user);
            }
            return Response.seeOther(URI.create("/student")).cookie(cookie).build();
        }
        return Response.seeOther(URI.create("/login?error=true")).build();
    }

    @GET
    @Path("/dashboard")
    @Produces(MediaType.TEXT_HTML)
    public String getDashboard(@QueryParam("tuitionSemesterId") String tuitionSemesterIdStr) {
        Long tuitionSemesterId = null;
        if (tuitionSemesterIdStr != null && !tuitionSemesterIdStr.trim().isEmpty()) {
            try {
                tuitionSemesterId = Long.parseLong(tuitionSemesterIdStr);
            } catch (NumberFormatException e) {
            }
        }

        Long totalUsers = (Long) em.createQuery("SELECT COUNT(s) FROM Student s").getSingleResult();
        Double average = (Double) em.createQuery("SELECT AVG(g.finalScore) FROM Grade g WHERE g.finalScore IS NOT NULL")
                .getSingleResult();
        if (average == null)
            average = 0.0;
        average = Math.round(average * 10.0) / 10.0;

        // Tính trung bình cộng toàn hệ thống theo thang điểm 4 cho Admin
        List<Double> allScores = em
                .createQuery("SELECT g.finalScore FROM Grade g WHERE g.finalScore IS NOT NULL", Double.class)
                .getResultList();
        double totalScore4 = 0.0;
        for (Double score : allScores) {
            if (score >= 9.0)
                totalScore4 += 4.0;
            else if (score >= 8.0)
                totalScore4 += 3.5;
            else if (score >= 7.0)
                totalScore4 += 3.0;
            else if (score >= 5.0)
                totalScore4 += 2.0;
            else if (score >= 4.0)
                totalScore4 += 1.0;
        }
        double average4 = allScores.isEmpty() ? 0.0 : Math.round((totalScore4 / allScores.size()) * 100.0) / 100.0;

        Long totalGrades = (Long) em.createQuery("SELECT COUNT(g) FROM Grade g WHERE g.finalScore IS NOT NULL")
                .getSingleResult();
        Long passedGrades = (Long) em.createQuery("SELECT COUNT(g) FROM Grade g WHERE g.finalScore >= 4.0")
                .getSingleResult();
        double passRatio = totalGrades == 0 ? 0.0 : (passedGrades * 100.0 / totalGrades);
        passRatio = Math.round(passRatio * 10.0) / 10.0;

        Map<String, Long> gradesDist = new HashMap<>();
        gradesDist.put("XS",
                (Long) em.createQuery("SELECT COUNT(g) FROM Grade g WHERE g.finalScore >= 9.0").getSingleResult());
        gradesDist.put("G",
                (Long) em.createQuery("SELECT COUNT(g) FROM Grade g WHERE g.finalScore >= 8.0 AND g.finalScore < 9.0")
                        .getSingleResult());
        gradesDist.put("K",
                (Long) em.createQuery("SELECT COUNT(g) FROM Grade g WHERE g.finalScore >= 7.0 AND g.finalScore < 8.0")
                        .getSingleResult());
        gradesDist.put("TB",
                (Long) em.createQuery("SELECT COUNT(g) FROM Grade g WHERE g.finalScore >= 5.0 AND g.finalScore < 7.0")
                        .getSingleResult());
        gradesDist.put("Y",
                (Long) em.createQuery("SELECT COUNT(g) FROM Grade g WHERE g.finalScore >= 4.0 AND g.finalScore < 5.0")
                        .getSingleResult());
        gradesDist.put("Kem",
                (Long) em.createQuery(
                        "SELECT COUNT(g) FROM Grade g WHERE g.finalScore < 4.0 AND g.finalScore IS NOT NULL")
                        .getSingleResult());

        // Tính toán thống kê theo từng học kỳ cho Admin
        List<com.example.studentgrade.model.Semester> semesters = em
                .createQuery("SELECT s FROM Semester s ORDER BY s.startDate DESC",
                        com.example.studentgrade.model.Semester.class)
                .getResultList();
        List<Map<String, Object>> semesterStats = new ArrayList<>();
        for (com.example.studentgrade.model.Semester sem : semesters) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("name", sem.getName());

            List<Double> semScores = em.createQuery(
                    "SELECT g.finalScore FROM Grade g WHERE g.finalScore IS NOT NULL AND g.subject.id IN " +
                            "(SELECT c.subjectId FROM Classroom c WHERE c.semesterId = :semId AND c.id IN " +
                            "(SELECT e.classroomId FROM Enrollment e WHERE e.studentId = g.student.id AND e.status = 'APPROVED'))",
                    Double.class)
                    .setParameter("semId", sem.getId())
                    .getResultList();

            double sAvg = 0.0;
            double sAvg4 = 0.0;
            long sPassed = 0;
            for (Double score : semScores) {
                sAvg += score;
                if (score >= 4.0)
                    sPassed++;

                if (score >= 9.0)
                    sAvg4 += 4.0;
                else if (score >= 8.0)
                    sAvg4 += 3.5;
                else if (score >= 7.0)
                    sAvg4 += 3.0;
                else if (score >= 5.0)
                    sAvg4 += 2.0;
                else if (score >= 4.0)
                    sAvg4 += 1.0;
            }
            if (!semScores.isEmpty()) {
                sAvg = Math.round((sAvg / semScores.size()) * 10.0) / 10.0;
                sAvg4 = Math.round((sAvg4 / semScores.size()) * 100.0) / 100.0;
            }
            double sPassRatio = semScores.isEmpty() ? 0.0
                    : Math.round((sPassed * 100.0 / semScores.size()) * 10.0) / 10.0;

            stat.put("average", sAvg);
            stat.put("average4", sAvg4);
            stat.put("passRatio", sPassRatio);
            stat.put("totalGrades", semScores.size());
            semesterStats.add(stat);
        }

        // Thống kê học kỳ cho biểu đồ tròn
        Long totalSemesters = em.createQuery("SELECT COUNT(s) FROM Semester s", Long.class).getSingleResult();
        Long completedSemesters = em
                .createQuery("SELECT COUNT(s) FROM Semester s WHERE s.endDate < :currentDate", Long.class)
                .setParameter("currentDate", java.time.LocalDate.now().toString())
                .getSingleResult();

        // Thống kê học phí theo kỳ cho biểu đồ
        double totalCollectedForSemRaw = 0.0;
        double totalUncollectedForSemRaw = 0.0;

        if (tuitionSemesterId != null) {
            Double collectedDouble = em.createQuery("SELECT SUM(t.paidAmount) FROM TuitionFee t WHERE t.semesterId = :semId", Double.class)
                .setParameter("semId", tuitionSemesterId)
                .getSingleResult();
            totalCollectedForSemRaw = collectedDouble != null ? collectedDouble : 0.0;

            Double uncollectedDouble = em.createQuery("SELECT SUM(t.totalAmount - t.paidAmount) FROM TuitionFee t WHERE t.status = 'UNPAID' AND t.semesterId = :semId", Double.class)
                .setParameter("semId", tuitionSemesterId)
                .getSingleResult();
            totalUncollectedForSemRaw = uncollectedDouble != null ? uncollectedDouble : 0.0;
        } else {
            // Tính cho tất cả các kỳ
            Double collectedDouble = em.createQuery("SELECT SUM(t.paidAmount) FROM TuitionFee t", Double.class).getSingleResult();
            totalCollectedForSemRaw = collectedDouble != null ? collectedDouble : 0.0;

            Double uncollectedDouble = em.createQuery("SELECT SUM(t.totalAmount - t.paidAmount) FROM TuitionFee t WHERE t.status = 'UNPAID'", Double.class).getSingleResult();
            totalUncollectedForSemRaw = uncollectedDouble != null ? uncollectedDouble : 0.0;
        }

        java.text.NumberFormat currencyFormat = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi", "VN"));
        String totalCollectedForSemStr = currencyFormat.format(totalCollectedForSemRaw);
        String totalUncollectedForSemStr = currencyFormat.format(totalUncollectedForSemRaw);

        return dashboard
                .data("total", totalUsers)
                .data("average", String.valueOf(average))
                .data("average4", String.valueOf(average4)) // Gửi điểm trung bình hệ số 4 ra dashboard
                .data("passRatio", String.valueOf(passRatio))
                .data("grades", gradesDist)
                .data("semesterStats", semesterStats)
                .data("completedSemesters", completedSemesters)
                .data("ongoingSemesters", totalSemesters - completedSemesters)
                .data("allSemestersForTuition", semesters)
                .data("selectedTuitionSemesterId", tuitionSemesterId)
                .data("totalCollectedForSemRaw", totalCollectedForSemRaw)
                .data("totalUncollectedForSemRaw", totalUncollectedForSemRaw)
                .data("totalCollectedForSemStr", totalCollectedForSemStr)
                .data("totalUncollectedForSemStr", totalUncollectedForSemStr)
                .render();
    }

    @GET
    @Path("/teacher")
    @Produces(MediaType.TEXT_HTML)
    public String getTeacherDashboard(
            @CookieParam("logged_in_username") String cookieUsername,
            @QueryParam("search") String search,
            @QueryParam("page") @DefaultValue("1") int page) {

        Student teacher = getLoggedTeacher(cookieUsername);
        int pageSize = 5; // Hiển thị 5 lớp mỗi trang

        String qlString = "SELECT c FROM Classroom c WHERE c.teacherId = :tid";
        String countQlString = "SELECT COUNT(c) FROM Classroom c WHERE c.teacherId = :tid";

        if (search != null && !search.trim().isEmpty()) {
            qlString += " AND lower(c.name) LIKE lower(:search)";
            countQlString += " AND lower(c.name) LIKE lower(:search)";
        }
        qlString += " ORDER BY c.startDate DESC";

        var countQuery = em.createQuery(countQlString, Long.class).setParameter("tid", teacher.getId());
        var query = em.createQuery(qlString, Classroom.class).setParameter("tid", teacher.getId());

        if (search != null && !search.trim().isEmpty()) {
            countQuery.setParameter("search", "%" + search + "%");
            query.setParameter("search", "%" + search + "%");
        }

        Long totalRecords = countQuery.getSingleResult();
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

        if (page < 1)
            page = 1;
        if (page > totalPages && totalPages > 0)
            page = totalPages;

        query.setFirstResult((page - 1) * pageSize);
        query.setMaxResults(pageSize);
        List<Classroom> classrooms = query.getResultList();

        List<TeacherClassDTO> classes = new ArrayList<>();
        for (Classroom c : classrooms) {
            List<Student> studentsInClass = em.createQuery(
                    "SELECT s FROM Student s WHERE s.id IN (SELECT e.studentId FROM Enrollment e WHERE e.classroomId = :cid AND e.status = 'APPROVED')",
                    Student.class)
                    .setParameter("cid", c.getId())
                    .getResultList();

            List<StudentGradeDTO> studentGrades = new ArrayList<>();
            for (Student s : studentsInClass) {
                Grade g = null;
                try {
                    g = em.createQuery("SELECT g FROM Grade g WHERE g.student.id = :sid AND g.subject.id = :subId",
                            Grade.class)
                            .setParameter("sid", s.getId())
                            .setParameter("subId", c.getSubjectId())
                            .getSingleResult();
                } catch (Exception e) {
                }

                if (g != null) {
                    studentGrades
                            .add(new StudentGradeDTO(s.getId(), s.getFullName(), s.getEmail(), g.getAttendanceScore(),
                                    g.getMidtermScore(), g.getFinalExamScore(), g.getFinalScore(), c.getSubjectId()));
                } else {
                    studentGrades.add(new StudentGradeDTO(s.getId(), s.getFullName(), s.getEmail(), null, null, null,
                            null, c.getSubjectId()));
                }
            }

            String semName = c.getSemesterName() != null ? c.getSemesterName() : "Không xác định";
            String schedule = (c.getStartDate() != null ? c.getStartDate() : "Chưa rõ") + " - "
                    + (c.getEndDate() != null ? c.getEndDate() : "Chưa rõ");
            classes.add(new TeacherClassDTO(c.getName(), semName, schedule, studentGrades));
        }

        // Tính tổng số lớp (tất cả) của giáo viên này
        Long totalClasses = em.createQuery("SELECT COUNT(c) FROM Classroom c WHERE c.teacherId = :tid", Long.class)
                .setParameter("tid", teacher.getId())
                .getSingleResult();

        // Tính tổng số sinh viên (DUY NHẤT) của giáo viên này để không bị trùng lặp dẫn
        // tới con số 100
        Long totalUniqueStudents = em.createQuery(
                "SELECT COUNT(DISTINCT e.studentId) FROM Enrollment e WHERE e.status = 'APPROVED' AND e.classroomId IN (SELECT c.id FROM Classroom c WHERE c.teacherId = :tid)",
                Long.class)
                .setParameter("tid", teacher.getId())
                .getSingleResult();

        return this.teacher.data("classes", classes)
                .data("totalClasses", totalClasses)
                .data("totalStudents", totalUniqueStudents)
                .data("search", search == null ? "" : search)
                .data("currentPage", page)
                .data("totalPages", totalPages)
                .data("user", teacher)
                .render();
    }

    @GET
    @Path("/teacher/enrollments")
    @Produces(MediaType.TEXT_HTML)
    public String getTeacherEnrollments(@CookieParam("logged_in_username") String cookieUsername) {
        Student teacher = getLoggedTeacher(cookieUsername);
        List<Classroom> classrooms = em
                .createQuery("SELECT c FROM Classroom c WHERE c.teacherId = :tid ORDER BY c.startDate DESC",
                        Classroom.class)
                .setParameter("tid", teacher.getId())
                .getResultList();

        List<PendingEnrollmentDTO> pendingEnrollments = new ArrayList<>();
        for (Classroom c : classrooms) {
            List<Enrollment> pending = em.createQuery(
                    "SELECT e FROM Enrollment e WHERE e.classroomId = :cid AND e.status = 'PENDING'", Enrollment.class)
                    .setParameter("cid", c.getId())
                    .getResultList();

            for (Enrollment e : pending) {
                Student s = em.find(Student.class, e.getStudentId());
                if (s != null) {
                    pendingEnrollments.add(new PendingEnrollmentDTO(e.getId(), s.getFullName(), c.getName()));
                }
            }
        }

        return teacherEnrollments.data("pendingEnrollments", pendingEnrollments)
                .data("user", teacher)
                .render();
    }

    @POST
    @Path("/teacher/grade")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response saveGrade(
            @FormParam("studentId") Long studentId,
            @FormParam("subjectId") Long subjectId,
            @FormParam("attendanceScore") String attendanceScoreStr,
            @FormParam("midtermScore") String midtermScoreStr,
            @FormParam("finalExamScore") String finalExamScoreStr) {

        if (subjectId == null || studentId == null) {
            return Response.seeOther(URI.create("/teacher?error=invalid_input")).build();
        }

        Grade grade;
        try {
            grade = em
                    .createQuery("SELECT g FROM Grade g WHERE g.student.id = :sid AND g.subject.id = :subId",
                            Grade.class)
                    .setParameter("sid", studentId)
                    .setParameter("subId", subjectId)
                    .getSingleResult();
        } catch (Exception e) {
            grade = new Grade();
            Student s = em.find(Student.class, studentId);
            Subject sub = em.find(Subject.class, subjectId);
            if (s != null && sub != null) {
                grade.setStudent(s);
                grade.setSubject(sub);
            } else {
                return Response.seeOther(URI.create("/teacher?error=notfound")).build();
            }
        }

        // Chỉ cập nhật các điểm được gửi từ form (không phải null)
        // Điều này cho phép cập nhật riêng lẻ từng cột điểm mà không làm ảnh hưởng dữ
        // liệu cũ
        if (attendanceScoreStr != null && !attendanceScoreStr.trim().isEmpty()) {
            try {
                grade.setAttendanceScore(Double.parseDouble(attendanceScoreStr));
            } catch (NumberFormatException e) {
            }
        }

        if (midtermScoreStr != null && !midtermScoreStr.trim().isEmpty()) {
            try {
                grade.setMidtermScore(Double.parseDouble(midtermScoreStr));
            } catch (NumberFormatException e) {
            }
        }

        if (finalExamScoreStr != null && !finalExamScoreStr.trim().isEmpty()) {
            try {
                grade.setFinalExamScore(Double.parseDouble(finalExamScoreStr));
            } catch (NumberFormatException e) {
            }
        }

        // Tính lại điểm tổng kết nếu cả 3 cột điểm thành phần đều đã có
        if (grade.getAttendanceScore() != null && grade.getMidtermScore() != null
                && grade.getFinalExamScore() != null) {
            double finalScoreValue = (grade.getAttendanceScore() * 0.1) + (grade.getMidtermScore() * 0.3)
                    + (grade.getFinalExamScore() * 0.6);
            grade.setFinalScore(Math.round(finalScoreValue * 10.0) / 10.0);
        }

        em.merge(grade);

        return Response.seeOther(URI.create("/teacher")).build();
    }

    @GET
    @Path("/student")
    @Produces(MediaType.TEXT_HTML)
    public String getStudentDashboard(@CookieParam("logged_in_username") String cookieUsername) {
        Student student = getLoggedStudent(cookieUsername);
        List<Grade> grades = em.createQuery("SELECT g FROM Grade g WHERE g.student.id = :sid", Grade.class)
                .setParameter("sid", student.getId())
                .getResultList();

        // Chỉ hiển thị các Học kỳ đã qua hoặc sắp bắt đầu (trước 30 ngày)
        String thresholdDate = java.time.LocalDate.now().plusDays(30).toString();
        List<com.example.studentgrade.model.Semester> dbSemesters = em
                .createQuery("SELECT s FROM Semester s WHERE s.startDate <= :thresholdDate ORDER BY s.startDate DESC",
                        com.example.studentgrade.model.Semester.class)
                .setParameter("thresholdDate", thresholdDate)
                .getResultList();
        List<SemesterDTO> semesters = new ArrayList<>();
        List<Grade> processedGrades = new ArrayList<>();

        for (com.example.studentgrade.model.Semester sem : dbSemesters) {
            List<Long> subjectIdsInSem = em.createQuery(
                    "SELECT c.subjectId FROM Classroom c WHERE c.semesterId = :semId AND c.id IN (SELECT e.classroomId FROM Enrollment e WHERE e.studentId = :sid AND e.status = 'APPROVED')",
                    Long.class)
                    .setParameter("semId", sem.getId())
                    .setParameter("sid", student.getId())
                    .getResultList();

            List<GradeDTO> semGrades = new ArrayList<>();
            java.util.Set<Long> processedSubIds = new java.util.HashSet<>();
            for (Long subId : subjectIdsInSem) {
                if (processedSubIds.contains(subId)) continue;
                processedSubIds.add(subId);
                
                Subject sub = em.find(Subject.class, subId);
                if (sub == null) continue;
                
                Double finalScore = null;
                Grade foundGrade = null;
                for (Grade g : grades) {
                    if (g.getSubject().getId().equals(subId)) {
                        finalScore = g.getFinalScore();
                        foundGrade = g;
                        break;
                    }
                }
                if (foundGrade != null) {
                    processedGrades.add(foundGrade);
                }
                semGrades.add(new GradeDTO(sub.getCode(), sub.getName(), sub.getCredits(), finalScore));
            }
            
            if (!semGrades.isEmpty()) {
                semesters.add(new SemesterDTO(sem.getName(), sem.getStartDate(), sem.getEndDate(), semGrades));
            }
        }

        // Gom các điểm không thuộc lớp học nào (dữ liệu mồ côi nếu có)
        List<GradeDTO> orphanGrades = new ArrayList<>();
        for (Grade g : grades) {
            if (!processedGrades.contains(g)) {
                orphanGrades.add(new GradeDTO(g.getSubject().getCode(), g.getSubject().getName(),
                        g.getSubject().getCredits(), g.getFinalScore()));
            }
        }
        if (!orphanGrades.isEmpty()) {
            semesters.add(new SemesterDTO("Học kỳ Khác (Tự do)", "", "", orphanGrades));
        }

        return this.student.data("semesters", semesters)
                .data("user", student)
                .render();
    }

    @GET
    @Path("/student/classes")
    @Produces(MediaType.TEXT_HTML)
    public String getStudentClasses(
            @CookieParam("logged_in_username") String cookieUsername,
            @QueryParam("page") @DefaultValue("1") int page) {
        Student student = getLoggedStudent(cookieUsername);

        int pageSize = 10;
        Long totalRecords = em.createQuery(
                "SELECT COUNT(c) FROM Classroom c WHERE c.id IN (SELECT e.classroomId FROM Enrollment e WHERE e.studentId = :sid AND e.status = 'APPROVED')",
                Long.class)
                .setParameter("sid", student.getId())
                .getSingleResult();
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

        if (page < 1)
            page = 1;
        if (page > totalPages && totalPages > 0)
            page = totalPages;

        List<Classroom> classrooms = new ArrayList<>();
        if (totalRecords > 0) {
            classrooms = em.createQuery(
                    "SELECT c FROM Classroom c WHERE c.id IN (SELECT e.classroomId FROM Enrollment e WHERE e.studentId = :sid AND e.status = 'APPROVED') ORDER BY c.startDate DESC",
                    Classroom.class)
                    .setParameter("sid", student.getId())
                    .setFirstResult((page - 1) * pageSize)
                    .setMaxResults(pageSize)
                    .getResultList();
        }

        List<StudentClassDTO> ongoingClasses = new ArrayList<>();
        List<StudentClassDTO> completedClasses = new ArrayList<>();
        String currentDateStr = java.time.LocalDate.now().toString();

        for (Classroom c : classrooms) {
            Student teacher = c.getTeacherId() != null ? em.find(Student.class, c.getTeacherId()) : null;
            String teacherName = teacher != null ? teacher.getFullName() : "Chưa có";
            String schedule = (c.getStartDate() != null ? c.getStartDate() : "Chưa rõ") + " đến "
                    + (c.getEndDate() != null ? c.getEndDate() : "Chưa rõ");
            Long enrolledCount = (Long) em
                    .createQuery(
                            "SELECT COUNT(e) FROM Enrollment e WHERE e.classroomId = :cid AND e.status = 'APPROVED'")
                    .setParameter("cid", c.getId())
                    .getSingleResult();

            StudentClassDTO dto = new StudentClassDTO(c.getId().toString(), c.getName(), c.getSubjectName(),
                    teacherName, schedule,
                    enrolledCount, 10);

            if (c.getEndDate() != null && c.getEndDate().compareTo(currentDateStr) < 0) {
                completedClasses.add(dto);
            } else {
                ongoingClasses.add(dto);
            }
        }

        return studentClasses.data("ongoingClasses", ongoingClasses)
                .data("completedClasses", completedClasses)
                .data("user", student)
                .data("currentPage", page)
                .data("totalPages", totalPages)
                .render();
    }

    @GET
    @Path("/student/enroll")
    @Produces(MediaType.TEXT_HTML)
    public String getStudentEnroll(@CookieParam("logged_in_username") String cookieUsername) {
        Student student = getLoggedStudent(cookieUsername);
        Long sid = student.getId() != null ? student.getId() : -1L;

        String maxThresholdDate = java.time.LocalDate.now().plusDays(35).toString(); // Mở đăng ký trước 35 ngày
        String minThresholdDate = java.time.LocalDate.now().plusDays(7).toString();

        List<Classroom> availableClassrooms = em.createQuery(
                "SELECT c FROM Classroom c WHERE c.id NOT IN (SELECT e.classroomId FROM Enrollment e WHERE e.studentId = :sid AND e.status IN ('APPROVED', 'PENDING')) "
                        +
                        "AND c.startDate <= :maxThresholdDate AND c.startDate >= :minThresholdDate ORDER BY c.startDate DESC",
                Classroom.class)
                .setParameter("sid", sid)
                .setParameter("maxThresholdDate", maxThresholdDate)
                .setParameter("minThresholdDate", minThresholdDate)
                .getResultList();
        List<StudentClassDTO> availableClasses = new ArrayList<>();
        for (Classroom c : availableClassrooms) {
            Student teacher = c.getTeacherId() != null ? em.find(Student.class, c.getTeacherId()) : null;
            String teacherName = teacher != null ? teacher.getFullName() : "Chưa có";
            String schedule = (c.getStartDate() != null ? c.getStartDate() : "Chưa rõ") + " đến "
                    + (c.getEndDate() != null ? c.getEndDate() : "Chưa rõ");
            Long enrolledCount = (Long) em
                    .createQuery(
                            "SELECT COUNT(e) FROM Enrollment e WHERE e.classroomId = :cid AND e.status = 'APPROVED'")
                    .setParameter("cid", c.getId())
                    .getSingleResult();
            availableClasses.add(new StudentClassDTO(c.getId().toString(), c.getName(), c.getSubjectName(), teacherName,
                    schedule, enrolledCount, 10)); // 10 là sỉ số tối đa
        }

        List<Classroom> pendingClassrooms = em.createQuery(
                "SELECT c FROM Classroom c WHERE c.id IN (SELECT e.classroomId FROM Enrollment e WHERE e.studentId = :sid AND e.status = 'PENDING') ORDER BY c.startDate DESC",
                Classroom.class)
                .setParameter("sid", sid)
                .getResultList();
        List<StudentClassDTO> pendingClasses = new ArrayList<>();
        for (Classroom c : pendingClassrooms) {
            Student teacher = c.getTeacherId() != null ? em.find(Student.class, c.getTeacherId()) : null;
            String teacherName = teacher != null ? teacher.getFullName() : "Chưa có";
            String schedule = (c.getStartDate() != null ? c.getStartDate() : "Chưa rõ") + " đến "
                    + (c.getEndDate() != null ? c.getEndDate() : "Chưa rõ");
            Long enrolledCount = (Long) em
                    .createQuery(
                            "SELECT COUNT(e) FROM Enrollment e WHERE e.classroomId = :cid AND e.status = 'APPROVED'")
                    .setParameter("cid", c.getId())
                    .getSingleResult();
            pendingClasses.add(new StudentClassDTO(c.getId().toString(), c.getName(), c.getSubjectName(), teacherName,
                    schedule, enrolledCount, 10));
        }

        return studentEnroll.data("availableClasses", availableClasses)
                .data("pendingClasses", pendingClasses)
                .data("user", student)
                .render();
    }

    @POST
    @Path("/student/enroll/cancel")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response cancelEnrollment(
            @CookieParam("logged_in_username") String cookieUsername,
            @FormParam("classroomId") Long classroomId) {
        Student student = getLoggedStudent(cookieUsername);
        if (student != null && classroomId != null) {
            em.createQuery(
                    "DELETE FROM Enrollment e WHERE e.studentId = :sid AND e.classroomId = :cid AND e.status = 'PENDING'")
                    .setParameter("sid", student.getId())
                    .setParameter("cid", classroomId)
                    .executeUpdate();
        }
        return Response.seeOther(URI.create("/student/enroll")).build();
    }

    @GET
    @Path("/student/tuition")
    @Produces(MediaType.TEXT_HTML)
    public String getStudentTuition(@CookieParam("logged_in_username") String cookieUsername) {
        Student student = getLoggedStudent(cookieUsername);
        List<TuitionFee> fees = em
                .createQuery("SELECT t FROM TuitionFee t WHERE t.studentId = :sid ORDER BY t.semesterId DESC",
                        TuitionFee.class)
                .setParameter("sid", student.getId())
                .getResultList();

        List<TuitionFeeDTO> tuitionList = new ArrayList<>();
        double totalDebt = 0.0;
        double totalPaid = 0.0; // Biến tính tổng tiền đã nộp

        for (TuitionFee fee : fees) {
            com.example.studentgrade.model.Semester sem = em.find(com.example.studentgrade.model.Semester.class,
                    fee.getSemesterId());
            String semName = sem != null ? sem.getName() : "Không xác định";
            double remaining = fee.getTotalAmount() - fee.getPaidAmount();
            if (remaining > 0)
                totalDebt += remaining;
            if (fee.getPaidAmount() != null)
                totalPaid += fee.getPaidAmount(); // Tính dồn tiền đã nộp

            List<PaymentTransaction> transactions = em
                    .createQuery("SELECT p FROM PaymentTransaction p WHERE p.tuitionFeeId = :tid ORDER BY p.id DESC",
                            PaymentTransaction.class)
                    .setParameter("tid", fee.getId())
                    .getResultList();

            List<PaymentTransactionDTO> transactionDTOs = new ArrayList<>();
            for (PaymentTransaction tx : transactions) {
                transactionDTOs.add(new PaymentTransactionDTO(tx));
            }

            tuitionList.add(new TuitionFeeDTO(fee, semName, remaining, transactionDTOs));
        }

        double totalRequired = totalDebt + totalPaid;

        return studentTuition.data("tuitionList", tuitionList)
                .data("totalDebtStr",
                        java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi", "VN")).format(totalDebt))
                .data("totalPaidStr",
                        java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi", "VN")).format(totalPaid))
                .data("totalRequiredStr",
                        java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi", "VN")).format(totalRequired))
                .data("totalDebtRaw", totalDebt)
                .data("totalPaidRaw", totalPaid)
                .data("user", student)
                .render();
    }

    @POST
    @Path("/student/tuition/pay")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response studentPayTuition(
            @CookieParam("logged_in_username") String cookieUsername,
            @FormParam("tuitionFeeId") Long tuitionFeeId,
            @FormParam("amount") Double amount) {
        Student student = getLoggedStudent(cookieUsername);
        TuitionFee fee = em.find(TuitionFee.class, tuitionFeeId);
        if (fee != null && fee.getStudentId().equals(student.getId()) && amount != null && amount > 0) {
            double currentPaid = fee.getPaidAmount() != null ? fee.getPaidAmount() : 0.0;
            double newPaid = currentPaid + amount;

            if (newPaid >= fee.getTotalAmount()) {
                fee.setPaidAmount(fee.getTotalAmount());
                fee.setStatus("PAID");
            } else {
                fee.setPaidAmount(newPaid);
                fee.setStatus("UNPAID");
            }
            em.merge(fee);

            PaymentTransaction tx = new PaymentTransaction();
            tx.setTuitionFeeId(fee.getId());
            tx.setAmount(amount);
            tx.setPaymentDate(java.time.LocalDate.now().toString());
            tx.setMethod("Thanh toán trực tuyến (Mô phỏng)");
            em.persist(tx);
        }
        return Response.seeOther(URI.create("/student/tuition")).build();
    }

    @GET
    @Path("/student/transcript")
    @Produces(MediaType.TEXT_HTML)
    public String getStudentTranscript(@CookieParam("logged_in_username") String cookieUsername) {
        Student student = getLoggedStudent(cookieUsername);
        List<Grade> grades = em.createQuery("SELECT g FROM Grade g WHERE g.student.id = :sid", Grade.class)
                .setParameter("sid", student.getId())
                .getResultList();

        List<DetailedGradeDTO> transcript = new ArrayList<>();
        int totalCredits = 0;
        double totalScore = 0;
        double totalScore4 = 0;
        for (Grade g : grades) {
            int credits = g.getSubject().getCredits();
            transcript.add(new DetailedGradeDTO(g.getSubject().getCode(), g.getSubject().getName(), credits,
                    g.getAttendanceScore(), g.getMidtermScore(), g.getFinalExamScore(), g.getFinalScore()));
            if (g.getFinalScore() != null) {
                totalCredits += credits;
                totalScore += g.getFinalScore() * credits;

                double score4 = 0.0;
                if (g.getFinalScore() >= 9.0)
                    score4 = 4.0;
                else if (g.getFinalScore() >= 8.0)
                    score4 = 3.5;
                else if (g.getFinalScore() >= 7.0)
                    score4 = 3.0;
                else if (g.getFinalScore() >= 5.0)
                    score4 = 2.0;
                else if (g.getFinalScore() >= 4.0)
                    score4 = 1.0;
                totalScore4 += score4 * credits;
            }
        }

        double cumulativeGPA = totalCredits == 0 ? 0.0 : Math.round((totalScore / totalCredits) * 100.0) / 100.0;
        double cumulativeGPA4 = totalCredits == 0 ? 0.0 : Math.round((totalScore4 / totalCredits) * 100.0) / 100.0;
        String classification = "Chưa xếp loại";
        if (totalCredits > 0) {
            if (cumulativeGPA4 >= 3.6)
                classification = "Xuất sắc";
            else if (cumulativeGPA4 >= 3.2)
                classification = "Giỏi";
            else if (cumulativeGPA4 >= 2.5)
                classification = "Khá";
            else if (cumulativeGPA4 >= 2.0)
                classification = "Trung bình";
            else if (cumulativeGPA4 >= 1.0)
                classification = "Yếu";
            else
                classification = "Kém";
        }

        return studentTranscript.data("transcript", transcript)
                .data("cumulativeGPA", cumulativeGPA)
                .data("cumulativeGPA4", cumulativeGPA4)
                .data("totalCredits", totalCredits)
                .data("classification", classification)
                .data("user", student)
                .render();
    }

    @GET
    @Path("/api/student/{id}/transcript")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStudentTranscriptJson(@PathParam("id") Long studentId) {
        List<Grade> grades = em.createQuery("SELECT g FROM Grade g WHERE g.student.id = :sid", Grade.class)
                .setParameter("sid", studentId)
                .getResultList();

        List<DetailedGradeDTO> transcript = new ArrayList<>();
        int totalCredits = 0;
        double totalScore = 0;
        double totalScore4 = 0;
        for (Grade g : grades) {
            int credits = g.getSubject().getCredits();
            DetailedGradeDTO dto = new DetailedGradeDTO(g.getSubject().getCode(), g.getSubject().getName(), credits,
                    g.getAttendanceScore(), g.getMidtermScore(), g.getFinalExamScore(), g.getFinalScore());
            transcript.add(dto);
            if (g.getFinalScore() != null) {
                totalCredits += credits;
                totalScore += g.getFinalScore() * credits;
                totalScore4 += (dto.getScore4() != null ? dto.getScore4() : 0.0) * credits;
            }
        }

        double cumulativeGPA = totalCredits == 0 ? 0.0 : Math.round((totalScore / totalCredits) * 100.0) / 100.0;
        double cumulativeGPA4 = totalCredits == 0 ? 0.0 : Math.round((totalScore4 / totalCredits) * 100.0) / 100.0;
        String classification = "Chưa xếp loại";
        if (totalCredits > 0) {
            if (cumulativeGPA4 >= 3.6)
                classification = "Xuất sắc";
            else if (cumulativeGPA4 >= 3.2)
                classification = "Giỏi";
            else if (cumulativeGPA4 >= 2.5)
                classification = "Khá";
            else if (cumulativeGPA4 >= 2.0)
                classification = "Trung bình";
            else if (cumulativeGPA4 >= 1.0)
                classification = "Yếu";
            else
                classification = "Kém";
        }

        Map<String, Object> result = new HashMap<>();
        result.put("transcript", transcript);
        result.put("gpa", cumulativeGPA);
        result.put("gpa4", cumulativeGPA4);
        result.put("credits", totalCredits);
        result.put("classification", classification);

        return Response.ok(result).build();
    }

    @POST
    @Path("/student/profile")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response updateStudentProfile(
            @CookieParam("logged_in_username") String cookieUsername,
            @FormParam("firstName") String firstName,
            @FormParam("lastName") String lastName,
            @FormParam("username") String username,
            @FormParam("password") String password,
            @FormParam("avatarBase64") String avatarBase64) {

        String currentUsername = cookieUsername != null ? cookieUsername : "student_1";
        Student target = repository.findByUsername(currentUsername);
        String redirectUrl = "/student";

        if (target != null && "TEACHER".equalsIgnoreCase(target.getRole())) {
            redirectUrl = "/teacher";
        } else if (target != null && "ADMIN".equalsIgnoreCase(target.getRole())) {
            redirectUrl = "/dashboard";
        }

        if (target != null) {
            target.setFirstName(firstName);
            target.setLastName(lastName);
            target.setUsername(username);
            if (password != null && !password.isEmpty())
                target.setPassword(password);
            if (avatarBase64 != null && !avatarBase64.isEmpty())
                target.setAvatar(avatarBase64);

            em.merge(target); // Lưu trực tiếp vào Database
        }

        NewCookie newCookie = new NewCookie.Builder("logged_in_username").value(username).path("/").build();
        return Response.seeOther(URI.create(redirectUrl)).cookie(newCookie).build();
    }

    @POST
    @Path("/users/edit")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response editUser(
            @FormParam("id") Long id,
            @FormParam("lastName") String lastName,
            @FormParam("firstName") String firstName,
            @FormParam("username") String username,
            @FormParam("email") String email,
            @FormParam("role") String role) {
        Student user = em.find(Student.class, id);
        if (user != null) {
            user.setLastName(lastName);
            user.setFirstName(firstName);
            user.setUsername(username);
            user.setEmail(email);
            user.setRole(role);
            em.merge(user);
        }
        return Response.seeOther(URI.create("/users")).build();
    }

    @POST
    @Path("/users/reset-password")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response resetUserPassword(@FormParam("id") Long id, @FormParam("newPassword") String newPassword) {
        Student user = em.find(Student.class, id);
        if (user != null && newPassword != null && !newPassword.trim().isEmpty()) {
            user.setPassword(newPassword);
            em.merge(user);
        }
        return Response.seeOther(URI.create("/users")).build();
    }

    @POST
    @Path("/subjects/edit")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response editSubject(
            @FormParam("id") Long id,
            @FormParam("code") String code,
            @FormParam("name") String name,
            @FormParam("description") String description) {
        Subject subject = em.find(Subject.class, id);
        if (subject != null) {
            subject.setCode(code);
            subject.setName(name);
            subject.setDescription(description);
            em.merge(subject);
        }
        return Response.seeOther(URI.create("/subjects")).build();
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

        public Double getScore4() {
            if (score == null)
                return null;
            if (score >= 9.0)
                return 4.0;
            if (score >= 8.0)
                return 3.5;
            if (score >= 7.0)
                return 3.0;
            if (score >= 5.0)
                return 2.0;
            if (score >= 4.0)
                return 1.0;
            return 0.0;
        }

        public String getLetterGrade() {
            if (score == null)
                return "N/A";
            if (score >= 9.0)
                return "Xuất sắc";
            if (score >= 8.0)
                return "Giỏi";
            if (score >= 7.0)
                return "Khá";
            if (score >= 5.0)
                return "Trung bình";
            if (score >= 4.0)
                return "Yếu";
            return "Kém";
        }

        public String getBadgeClass() {
            if (score == null)
                return "na";
            if (score >= 8.0)
                return "good";
            if (score >= 5.0)
                return "average";
            return "bad";
        }
    }

    public static class SemesterDTO {
        public String name;
        public String startDate;
        public String endDate;
        public List<GradeDTO> grades;
        public Double semesterGPA;
        public Double semesterGPA4;

        public SemesterDTO(String name, String startDate, String endDate, List<GradeDTO> grades) {
            this.name = name;
            this.startDate = startDate;
            this.endDate = endDate;
            this.grades = grades;
            this.semesterGPA = calculateGPA();
            this.semesterGPA4 = calculateGPA4();
        }

        private Double calculateGPA() {
            if (grades == null || grades.isEmpty())
                return 0.0;
            double totalScore = 0;
            int totalCredits = 0;
            for (GradeDTO g : grades) {
                if (g.score != null) {
                    totalScore += (g.score * g.credits);
                    totalCredits += g.credits;
                }
            }
            return totalCredits == 0 ? 0.0 : Math.round((totalScore / totalCredits) * 100.0) / 100.0;
        }

        private Double calculateGPA4() {
            if (grades == null || grades.isEmpty())
                return 0.0;
            double totalScore4 = 0;
            int totalCredits = 0;
            for (GradeDTO g : grades) {
                if (g.score != null) {
                    totalScore4 += (g.getScore4() * g.credits);
                    totalCredits += g.credits;
                }
            }
            return totalCredits == 0 ? 0.0 : Math.round((totalScore4 / totalCredits) * 100.0) / 100.0;
        }
    }

    public static class StudentClassDTO {
        public String id;
        public String name;
        public String subject;
        public String teacher;
        public String schedule;
        public long enrolledCount;
        public int maxCapacity;

        public StudentClassDTO(String id, String name, String subject, String teacher, String schedule) {
            this.id = id;
            this.name = name;
            this.subject = subject;
            this.teacher = teacher;
            this.schedule = schedule;
            this.enrolledCount = 0;
            this.maxCapacity = 10;
        }

        public StudentClassDTO(String id, String name, String subject, String teacher, String schedule,
                long enrolledCount, int maxCapacity) {
            this.id = id;
            this.name = name;
            this.subject = subject;
            this.teacher = teacher;
            this.schedule = schedule;
            this.enrolledCount = enrolledCount;
            this.maxCapacity = maxCapacity;
        }

        public boolean isFull() {
            return enrolledCount >= maxCapacity;
        }
    }

    public static class DetailedGradeDTO {
        public String subjectCode;
        public String subjectName;
        public int credits;
        public Double attendanceScore;
        public Double midtermScore;
        public Double finalExamScore; // Thêm điểm cuối kỳ
        public Double finalScore;

        public DetailedGradeDTO(String subjectCode, String subjectName, int credits, Double attendanceScore,
                Double midtermScore, Double finalExamScore, Double finalScore) {
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.credits = credits;
            this.attendanceScore = attendanceScore;
            this.midtermScore = midtermScore;
            this.finalExamScore = finalExamScore;
            this.finalScore = finalScore;
        }

        public Double getScore4() {
            if (finalScore == null)
                return null;
            if (finalScore >= 9.0)
                return 4.0;
            if (finalScore >= 8.0)
                return 3.5;
            if (finalScore >= 7.0)
                return 3.0;
            if (finalScore >= 5.0)
                return 2.0;
            if (finalScore >= 4.0)
                return 1.0;
            return 0.0;
        }

        public String getLetterGrade() {
            if (finalScore == null)
                return "N/A";
            if (finalScore >= 9.0)
                return "Xuất sắc";
            if (finalScore >= 8.0)
                return "Giỏi";
            if (finalScore >= 7.0)
                return "Khá";
            if (finalScore >= 5.0)
                return "Trung bình";
            if (finalScore >= 4.0)
                return "Yếu";
            return "Kém";
        }

        public String getBadgeClass() {
            if (finalScore == null)
                return "na";
            if (finalScore >= 8.0)
                return "good";
            if (finalScore >= 5.0)
                return "average";
            return "bad";
        }
    }

    public static class TeacherClassDTO {
        public String className;
        public String semesterName;
        public String schedule;
        public List<StudentGradeDTO> students;

        public TeacherClassDTO(String className, String semesterName, String schedule, List<StudentGradeDTO> students) {
            this.className = className;
            this.semesterName = semesterName;
            this.schedule = schedule;
            this.students = students;
        }
    }

    public static class StudentGradeDTO {
        public Long id;
        public String name;
        public String email;
        public Double attendanceScore;
        public Double midtermScore;
        public Double finalExamScore;
        public Double finalScore;
        public Long subjectId;

        public StudentGradeDTO(Long id, String name, String email, Double att, Double mid, Double exam, Double fin,
                Long subjectId) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.attendanceScore = att;
            this.midtermScore = mid;
            this.finalExamScore = exam;
            this.finalScore = fin;
            this.subjectId = subjectId;
        }

        public Double getScore4() {
            if (finalScore == null)
                return null;
            if (finalScore >= 9.0)
                return 4.0;
            if (finalScore >= 8.0)
                return 3.5;
            if (finalScore >= 7.0)
                return 3.0;
            if (finalScore >= 5.0)
                return 2.0;
            if (finalScore >= 4.0)
                return 1.0;
            return 0.0;
        }

        public String getLetterGrade() {
            if (finalScore == null)
                return "N/A";
            if (finalScore >= 9.0)
                return "Xuất sắc";
            if (finalScore >= 8.0)
                return "Giỏi";
            if (finalScore >= 7.0)
                return "Khá";
            if (finalScore >= 5.0)
                return "Trung bình";
            if (finalScore >= 4.0)
                return "Yếu";
            return "Kém";
        }

        public String getBadgeClass() {
            if (finalScore == null)
                return "na";
            if (finalScore >= 8.0)
                return "good";
            if (finalScore >= 5.0)
                return "average";
            return "bad";
        }
    }

    public static class PendingEnrollmentDTO {
        public Long enrollmentId;
        public String studentName;
        public String className;

        public PendingEnrollmentDTO(Long enrollmentId, String studentName, String className) {
            this.enrollmentId = enrollmentId;
            this.studentName = studentName;
            this.className = className;
        }
    }

    public static class TuitionFeeDTO {
        public TuitionFee fee;
        public String semesterName;
        public Double remainingAmount;
        public List<PaymentTransactionDTO> transactions;

        public TuitionFeeDTO(TuitionFee fee, String semesterName, Double remainingAmount,
                List<PaymentTransactionDTO> transactions) {
            this.fee = fee;
            this.semesterName = semesterName;
            this.remainingAmount = remainingAmount;
            this.transactions = transactions;
        }

        public String getTotalAmountStr() {
            return java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi", "VN"))
                    .format(fee.getTotalAmount());
        }

        public String getPaidAmountStr() {
            return java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi", "VN"))
                    .format(fee.getPaidAmount());
        }

        public String getRemainingAmountStr() {
            return java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi", "VN")).format(remainingAmount);
        }
    }

    public static class PaymentTransactionDTO {
        private final PaymentTransaction tx;

        public PaymentTransactionDTO(PaymentTransaction tx) {
            this.tx = tx;
        }

        public String getPaymentDate() {
            return tx.getPaymentDate();
        }

        public String getMethod() {
            return tx.getMethod();
        }

        public String getAmountStr() {
            return java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi", "VN")).format(tx.getAmount());
        }
    }
}