package com.example.studentgrade.controller;

import com.example.studentgrade.model.Student;
import com.example.studentgrade.model.Classroom;
import com.example.studentgrade.model.Grade;
import com.example.studentgrade.model.Subject;
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
        return s != null ? s : new Student(); // Fallback an toàn tránh Null
    }

    private Student getLoggedTeacher(String cookieUsername) {
        String username = cookieUsername != null ? cookieUsername : "teacher_toan";
        Student t = repository.findByUsername(username);
        return t != null ? t : new Student();
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
            return Response.seeOther(URI.create("/dashboard")).cookie(cookie).build();
        }
        // Backdoor cho Teacher để dễ test (bắt đầu bằng giangvien...)
        if (username != null && username.startsWith("teacher_") && "123456".equals(password)) {
            return Response.seeOther(URI.create("/teacher")).cookie(cookie).build();
        }
        // Backdoor cho Student để dễ test (bắt đầu bằng student...)
        if (username != null && username.startsWith("student_") && "123456".equals(password)) {
            return Response.seeOther(URI.create("/student")).cookie(cookie).build();
        }
        return Response.seeOther(URI.create("/login?error=true")).build();
    }

    @GET
    @Path("/dashboard")
    @Produces(MediaType.TEXT_HTML)
    public String getDashboard() {
        Long totalUsers = (Long) em.createQuery("SELECT COUNT(s) FROM Student s").getSingleResult();
        Double average = (Double) em.createQuery("SELECT AVG(g.finalScore) FROM Grade g WHERE g.finalScore IS NOT NULL")
                .getSingleResult();
        if (average == null)
            average = 0.0;
        average = Math.round(average * 10.0) / 10.0;

        Long totalGrades = (Long) em.createQuery("SELECT COUNT(g) FROM Grade g WHERE g.finalScore IS NOT NULL")
                .getSingleResult();
        Long passedGrades = (Long) em.createQuery("SELECT COUNT(g) FROM Grade g WHERE g.finalScore >= 4.0")
                .getSingleResult();
        double passRatio = totalGrades == 0 ? 0.0 : (passedGrades * 100.0 / totalGrades);
        passRatio = Math.round(passRatio * 10.0) / 10.0;

        Map<String, Long> gradesDist = new HashMap<>();
        gradesDist.put("A",
                (Long) em.createQuery("SELECT COUNT(g) FROM Grade g WHERE g.finalScore >= 8.5").getSingleResult());
        gradesDist.put("B",
                (Long) em.createQuery("SELECT COUNT(g) FROM Grade g WHERE g.finalScore >= 7.0 AND g.finalScore < 8.5")
                        .getSingleResult());
        gradesDist.put("C",
                (Long) em.createQuery("SELECT COUNT(g) FROM Grade g WHERE g.finalScore >= 5.5 AND g.finalScore < 7.0")
                        .getSingleResult());
        gradesDist.put("D",
                (Long) em.createQuery("SELECT COUNT(g) FROM Grade g WHERE g.finalScore >= 4.0 AND g.finalScore < 5.5")
                        .getSingleResult());
        gradesDist
                .put("F",
                        (Long) em.createQuery(
                                "SELECT COUNT(g) FROM Grade g WHERE g.finalScore < 4.0 AND g.finalScore IS NOT NULL")
                                .getSingleResult());

        return dashboard
                .data("total", totalUsers)
                .data("average", String.valueOf(average))
                .data("passRatio", String.valueOf(passRatio))
                .data("grades", gradesDist)
                .render();
    }

    @GET
    @Path("/teacher")
    @Produces(MediaType.TEXT_HTML)
    public String getTeacherDashboard(@CookieParam("logged_in_username") String cookieUsername) {
        Student teacher = getLoggedTeacher(cookieUsername);
        List<Classroom> classrooms = em
                .createQuery("SELECT c FROM Classroom c WHERE c.teacherId = :tid", Classroom.class)
                .setParameter("tid", teacher.getId())
                .getResultList();

        List<TeacherClassDTO> classes = new ArrayList<>();
        int totalStudents = 0;
        for (Classroom c : classrooms) {
            List<Student> studentsInClass = em.createQuery(
                    "SELECT s FROM Student s WHERE s.id IN (SELECT e.studentId FROM Enrollment e WHERE e.classroomId = :cid)",
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
            classes.add(new TeacherClassDTO(c.getName(), studentGrades));
            totalStudents += studentsInClass.size();
        }

        return this.teacher.data("classes", classes)
                .data("totalClasses", classrooms.size())
                .data("totalStudents", totalStudents)
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
            @FormParam("attendanceScore") Double attendanceScore,
            @FormParam("midtermScore") Double midtermScore,
            @FormParam("finalExamScore") Double finalExamScore) {

        Double finalScore = null;
        if (attendanceScore != null && midtermScore != null && finalExamScore != null) {
            finalScore = (attendanceScore * 0.1) + (midtermScore * 0.3) + (finalExamScore * 0.6);
            finalScore = Math.round(finalScore * 10.0) / 10.0; // Làm tròn 1 chữ số thập phân
        }

        if (subjectId != null) {
            try {
                Grade g = em
                        .createQuery("SELECT g FROM Grade g WHERE g.student.id = :sid AND g.subject.id = :subId",
                                Grade.class)
                        .setParameter("sid", studentId)
                        .setParameter("subId", subjectId)
                        .getSingleResult();
                g.setAttendanceScore(attendanceScore);
                g.setMidtermScore(midtermScore);
                g.setFinalExamScore(finalExamScore);
                g.setFinalScore(finalScore);
                em.merge(g);
            } catch (Exception e) {
                Grade g = new Grade();
                Student s = em.find(Student.class, studentId);
                Subject sub = em.find(Subject.class, subjectId);
                if (s != null && sub != null) {
                    g.setStudent(s);
                    g.setSubject(sub);
                    g.setAttendanceScore(attendanceScore);
                    g.setMidtermScore(midtermScore);
                    g.setFinalExamScore(finalExamScore);
                    g.setFinalScore(finalScore);
                    em.persist(g);
                }
            }
        }

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

        List<GradeDTO> semGrades = new ArrayList<>();
        for (Grade g : grades) {
            semGrades.add(new GradeDTO(g.getSubject().getCode(), g.getSubject().getName(), g.getSubject().getCredits(),
                    g.getFinalScore()));
        }

        List<SemesterDTO> semesters = List.of(
                new SemesterDTO("Học kỳ 1", "05/09", "20/01", semGrades));

        return this.student.data("semesters", semesters)
                .data("user", student)
                .render();
    }

    @GET
    @Path("/student/classes")
    @Produces(MediaType.TEXT_HTML)
    public String getStudentClasses(@CookieParam("logged_in_username") String cookieUsername) {
        Student student = getLoggedStudent(cookieUsername);
        List<Classroom> classrooms = em.createQuery(
                "SELECT c FROM Classroom c WHERE c.id IN (SELECT e.classroomId FROM Enrollment e WHERE e.studentId = :sid)",
                Classroom.class)
                .setParameter("sid", student.getId())
                .getResultList();

        List<StudentClassDTO> currentClasses = new ArrayList<>();
        for (Classroom c : classrooms) {
            Student teacher = em.find(Student.class, c.getTeacherId());
            String teacherName = teacher != null ? teacher.getFullName() : "Chưa có";
            String schedule = c.getStartDate() + " đến " + c.getEndDate();
            currentClasses.add(
                    new StudentClassDTO(c.getId().toString(), c.getName(), c.getSubjectName(), teacherName, schedule));
        }

        return studentClasses.data("classes", currentClasses)
                .data("user", student)
                .render();
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
        for (Grade g : grades) {
            int credits = g.getSubject().getCredits();
            transcript.add(new DetailedGradeDTO(g.getSubject().getCode(), g.getSubject().getName(), credits,
                    g.getAttendanceScore(), g.getMidtermScore(), g.getFinalExamScore(), g.getFinalScore()));
            if (g.getFinalScore() != null) {
                totalCredits += credits;
                totalScore += g.getFinalScore() * credits;
            }
        }

        double cumulativeGPA = totalCredits == 0 ? 0.0 : Math.round((totalScore / totalCredits) * 100.0) / 100.0;
        String classification = "Chưa xếp loại";
        if (cumulativeGPA >= 8.5)
            classification = "Giỏi";
        else if (cumulativeGPA >= 7.0)
            classification = "Khá";
        else if (cumulativeGPA >= 5.5)
            classification = "Trung bình";
        else if (cumulativeGPA > 0)
            classification = "Yếu";

        return studentTranscript.data("transcript", transcript)
                .data("cumulativeGPA", cumulativeGPA)
                .data("totalCredits", totalCredits)
                .data("classification", classification)
                .data("user", student)
                .render();
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

        public String getLetterGrade() {
            if (score == null)
                return "N/A";
            if (score >= 8.5)
                return "Giỏi (A)";
            if (score >= 7.0)
                return "Khá (B)";
            if (score >= 5.5)
                return "Trung bình (C)";
            if (score >= 4.0)
                return "Yếu (D)";
            return "Trượt (F)";
        }

        public String getBadgeClass() {
            if (score == null)
                return "na";
            if (score >= 7.0)
                return "good";
            if (score >= 5.5)
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

        public SemesterDTO(String name, String startDate, String endDate, List<GradeDTO> grades) {
            this.name = name;
            this.startDate = startDate;
            this.endDate = endDate;
            this.grades = grades;
            this.semesterGPA = calculateGPA();
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
    }

    public static class StudentClassDTO {
        public String id;
        public String name;
        public String subject;
        public String teacher;
        public String schedule;

        public StudentClassDTO(String id, String name, String subject, String teacher, String schedule) {
            this.id = id;
            this.name = name;
            this.subject = subject;
            this.teacher = teacher;
            this.schedule = schedule;
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

        public String getLetterGrade() {
            if (finalScore == null)
                return "N/A";
            if (finalScore >= 8.5)
                return "Giỏi (A)";
            if (finalScore >= 7.0)
                return "Khá (B)";
            if (finalScore >= 5.5)
                return "Trung bình (C)";
            if (finalScore >= 4.0)
                return "Yếu (D)";
            return "Trượt (F)";
        }

        public String getBadgeClass() {
            if (finalScore == null)
                return "na";
            if (finalScore >= 7.0)
                return "good";
            if (finalScore >= 5.5)
                return "average";
            return "bad";
        }
    }

    public static class TeacherClassDTO {
        public String className;
        public List<StudentGradeDTO> students;

        public TeacherClassDTO(String className, List<StudentGradeDTO> students) {
            this.className = className;
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

        public String getLetterGrade() {
            if (finalScore == null)
                return "N/A";
            if (finalScore >= 8.5)
                return "Giỏi (A)";
            if (finalScore >= 7.0)
                return "Khá (B)";
            if (finalScore >= 5.5)
                return "Trung bình (C)";
            if (finalScore >= 4.0)
                return "Yếu (D)";
            return "Trượt (F)";
        }

        public String getBadgeClass() {
            if (finalScore == null)
                return "na";
            if (finalScore >= 7.0)
                return "good";
            if (finalScore >= 5.5)
                return "average";
            return "bad";
        }
    }
}