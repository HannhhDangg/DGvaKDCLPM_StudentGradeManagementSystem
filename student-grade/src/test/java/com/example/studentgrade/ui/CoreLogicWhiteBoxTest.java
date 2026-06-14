package com.example.studentgrade.ui;

import com.example.studentgrade.controller.StudentController;

import com.example.studentgrade.model.Enrollment;
import com.example.studentgrade.model.Grade;
import com.example.studentgrade.model.Student;
import com.example.studentgrade.model.Subject;
import com.example.studentgrade.model.TuitionFee;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class CoreLogicWhiteBoxTest {

    @Inject
    StudentController studentController;

    @Inject
    EntityManager em;

    private Student mockStudent;
    private Subject mockSubject;

    // Dữ liệu dùng chung (Fixture)
    @BeforeEach
    @Transactional
    public void setup() {
        // Khắc phục lỗi: Lấy student_1 đã tồn tại trong DataInitializer để DB nhận diện chính xác
        mockStudent = em.createQuery("SELECT s FROM Student s WHERE s.username = 'student_1'", Student.class)
                .setMaxResults(1)
                .getSingleResult();

        // Tạo môn học giả lập
        mockSubject = new Subject();
        mockSubject.setCode("TEST101");
        mockSubject.setCredits(3);
        em.persist(mockSubject);
        
        // Giả lập hệ thống đã tạo bản ghi Grade rỗng từ lúc Đăng ký môn (Enrollment)
        Grade emptyGrade = new Grade();
        emptyGrade.setStudent(mockStudent);
        emptyGrade.setSubject(mockSubject);
        em.persist(emptyGrade);

        em.flush(); // Đảm bảo dữ liệu đẩy xuống DB để các Repository có thể query thấy
    }

    /* =================================================================================
     *  NHÓM 1: KIỂM THỬ LOGIC TÍNH ĐIỂM (GRADE CALCULATION & BOUNDARY)
     * ================================================================================= */

    @Test
    @TestTransaction
    @DisplayName("Happy Path: Khi nhập đủ 3 điểm, hệ thống tự động tính điểm tổng kết chính xác")
    public void testGrade_WhenAllScoresProvided_ShouldCalculateFinalScoreCorrectly() {
        // Arrange: 10% CC + 30% GK + 60% CK
        String att = "8.0"; // 0.8
        String mid = "7.0"; // 2.1
        String fin = "9.0"; // 5.4  => Tổng phải là 8.3

        // Act
        Response response = studentController.saveGrade(mockStudent.getId(), mockSubject.getId(), att, mid, fin);
        em.flush(); // Đẩy lệnh insert/update Grade vừa chạy trong Controller xuống DB
        
        assertEquals(303, response.getStatus(), "API saveGrade phải trả về 303 Redirect");

        // Assert
        Grade gradeInDb = getGradeFromDb();
        assertNotNull(gradeInDb.getFinalScore(), "Điểm tổng kết không được null");
        assertEquals(8.3, gradeInDb.getFinalScore(), "Công thức tính điểm bị sai lệch");
    }

    @Test
    @TestTransaction
    @DisplayName("Edge Case: Chỉ cập nhật điểm thành phần, thiếu điểm thi thì chưa tính tổng kết")
    public void testGrade_WhenMissingFinalExamScore_ShouldNotCalculateFinalScore() {
        // Act: Chỉ gửi điểm CC và Giữa kỳ
        studentController.saveGrade(mockStudent.getId(), mockSubject.getId(), "10.0", "5.0", null);

        // Assert
        Grade gradeInDb = getGradeFromDb();
        assertEquals(10.0, gradeInDb.getAttendanceScore());
        assertEquals(5.0, gradeInDb.getMidtermScore());
        assertNull(gradeInDb.getFinalScore(), "Chưa có điểm thi cuối kỳ thì không được phép tính điểm tổng kết");
    }

    /* =================================================================================
     *  NHÓM 2: KIỂM THỬ TRẠNG THÁI HỌC PHÍ (TUITION FEE STATE TRANSITIONS)
     * ================================================================================= */

    @Test
    @TestTransaction
    @DisplayName("State Transition: Nộp một phần học phí -> Vẫn là UNPAID")
    public void testTuition_PartialPayment_ShouldRemainUnpaid() {
        // Arrange: Nợ 1.000.000 VNĐ
        TuitionFee fee = createMockTuitionFee(1000000.0);

        // Act: Nộp 400.000 VNĐ
        studentController.studentPayTuition(mockStudent.getUsername(), fee.getId(), 400000.0);
        em.flush();

        // Assert
        TuitionFee updatedFee = em.find(TuitionFee.class, fee.getId());
        assertEquals(400000.0, updatedFee.getPaidAmount());
        assertEquals("UNPAID", updatedFee.getStatus(), "Đóng thiếu tiền phải giữ nguyên trạng thái UNPAID");
    }

    @Test
    @TestTransaction
    @DisplayName("State Transition & Boundary: Nộp đủ tiền học phí -> Trạng thái đổi sang PAID")
    public void testTuition_FullPayment_ShouldChangeToPaid() {
        // Arrange
        TuitionFee fee = createMockTuitionFee(1500000.0);

        // Act
        studentController.studentPayTuition(mockStudent.getUsername(), fee.getId(), 1500000.0);
        em.flush();

        // Assert
        TuitionFee updatedFee = em.find(TuitionFee.class, fee.getId());
        assertEquals(1500000.0, updatedFee.getPaidAmount());
        assertEquals("PAID", updatedFee.getStatus(), "Nộp đủ tiền phải chuyển trạng thái sang PAID");
    }

    @Test
    @TestTransaction
    @DisplayName("Negative Path: Không cho phép nộp số tiền âm để hack học phí")
    public void testTuition_NegativePaymentAmount_ShouldNotProcess() {
        // Arrange
        TuitionFee fee = createMockTuitionFee(500000.0);

        // Act: Gửi số tiền âm
        studentController.studentPayTuition(mockStudent.getUsername(), fee.getId(), -200000.0);
        em.flush();

        // Assert
        TuitionFee updatedFee = em.find(TuitionFee.class, fee.getId());
        assertEquals(0.0, updatedFee.getPaidAmount(), "Hệ thống không được phép cộng dồn số tiền âm");
    }

    /* =================================================================================
     *  NHÓM 3: KIỂM THỬ BẢO MẬT & LUỒNG ĐĂNG KÝ (ENROLLMENT SECURITY)
     * ================================================================================= */

    @Test
    @TestTransaction
    @DisplayName("Negative Path: Không cho phép sinh viên huỷ môn khi giáo viên đã APPROVED")
    public void testEnrollment_CancelApprovedClass_ShouldBePrevented() {
        // Arrange: Tạo một bản ghi đăng ký đã được duyệt (APPROVED)
        Enrollment approvedEnrollment = new Enrollment();
        approvedEnrollment.setStudentId(mockStudent.getId());
        approvedEnrollment.setClassroomId(999L);
        approvedEnrollment.setStatus("APPROVED");
        em.persist(approvedEnrollment);
        em.flush();

        // Act: Sinh viên cố tình gọi API huỷ đăng ký
        studentController.cancelEnrollment(mockStudent.getUsername(), 999L);
        em.flush();

        // Assert: Bản ghi phải vẫn tồn tại trên DB, không bị xoá
        Enrollment checkEnrollment = em.find(Enrollment.class, approvedEnrollment.getId());
        assertNotNull(checkEnrollment, "Không được phép xoá lớp học đã APPROVED");
        assertEquals("APPROVED", checkEnrollment.getStatus());
    }

    // ================= HELPER METHODS =================

    private Grade getGradeFromDb() {
        return em.createQuery("SELECT g FROM Grade g WHERE g.student.id = :sid AND g.subject.id = :subId", Grade.class)
                .setParameter("sid", mockStudent.getId())
                .setParameter("subId", mockSubject.getId())
                .getSingleResult();
    }

    private TuitionFee createMockTuitionFee(Double amount) {
        TuitionFee fee = new TuitionFee();
        fee.setStudentId(mockStudent.getId());
        fee.setSemesterId(1L);
        fee.setTotalAmount(amount);
        fee.setPaidAmount(0.0);
        fee.setStatus("UNPAID");
        em.persist(fee);
        em.flush();
        return fee;
    }
}