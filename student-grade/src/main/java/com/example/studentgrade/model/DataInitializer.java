package com.example.studentgrade.model;

import com.example.studentgrade.model.Student;
import com.example.studentgrade.model.Subject;
import com.example.studentgrade.model.TeacherSubject;
import com.example.studentgrade.repository.StudentRepository;
import com.example.studentgrade.repository.SubjectRepository;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class DataInitializer {

        @Inject
        StudentRepository userRepository;

        @Inject
        SubjectRepository subjectRepository;

        @Inject
        EntityManager em;

        @Transactional
        public void onStart(@Observes StartupEvent ev) {
                // Chỉ tạo dữ liệu nếu bảng users đang trống
                if (userRepository.count() > 0) {
                        return;
                }

                // 1. TẠO ADMIN
                createUser("Admin", "Hệ thống", "admin", "admin@edumanage.com", "ADMIN", "123456");

                // 2. TẠO TÀI KHOẢN GIẢNG VIÊN
                Student gvToan = createUser("GV Toán", "Cơ bản", "teacher_toan", "toan@edumanage.com", "TEACHER",
                                "123456");
                Student gvLyLuan = createUser("GV Lý luận", "Chính trị", "teacher_lyluan", "lyluan@edumanage.com",
                                "TEACHER",
                                "123456");
                Student gvCNTT = createUser("GV Công nghệ", "Thông tin", "teacher_cntt", "cntt@edumanage.com",
                                "TEACHER",
                                "123456");

                // 3. TẠO 10 TÀI KHOẢN SINH VIÊN TỰ ĐỘNG
                for (int i = 1; i <= 10; i++) {
                        createUser("Sinh viên", String.format("%02d", i), "student_" + i, "sv" + i + "@edumanage.com",
                                        "STUDENT", "123456");
                }

                // 4. TẠO DỮ LIỆU MÔN HỌC & GÁN CHO GIẢNG VIÊN (Mỗi người 5 môn)

                // Nhóm 1: teacher_toan (Toán & Cơ bản)
                Subject mat101 = createSubject("MAT101", "Giải tích", 3,
                                "Môn học cung cấp nền tảng toán học về giới hạn, đạo hàm, và tích phân. Sinh viên cần làm bài tập hàng tuần và tham gia đầy đủ các buổi thực hành.");
                assignTeacherToSubject(gvToan.getId(), mat101.getId());

                Subject mat102 = createSubject("MAT102", "Lý thuyết xác suất thống kê", 3,
                                "Nghiên cứu các khái niệm cơ bản về xác suất, đại lượng ngẫu nhiên và các phương pháp thống kê toán học ứng dụng.");
                assignTeacherToSubject(gvToan.getId(), mat102.getId());

                Subject mat103 = createSubject("MAT103", "Đại số tuyến tính", 3,
                                "Cung cấp kiến thức về ma trận, định thức, hệ phương trình tuyến tính, không gian vector và ánh xạ tuyến tính.");
                assignTeacherToSubject(gvToan.getId(), mat103.getId());

                Subject mat104 = createSubject("MAT104", "Toán rời rạc", 3,
                                "Môn học thiết yếu cho sinh viên CNTT, bao gồm logic, lý thuyết tập hợp, lý thuyết đồ thị và đại số Boole.");
                assignTeacherToSubject(gvToan.getId(), mat104.getId());

                Subject phy101 = createSubject("PHY101", "Vật lý 1", 3,
                                "Nghiên cứu các định luật cơ bản của cơ học cổ điển, dao động, sóng và nhiệt học.");
                assignTeacherToSubject(gvToan.getId(), phy101.getId());

                // Nhóm 2: teacher_lyluan (Lý luận chính trị)
                Subject pol101 = createSubject("POL101", "Kinh tế chính trị Mác - Lênin", 2,
                                "Nghiên cứu các quy luật kinh tế của phương thức sản xuất tư bản chủ nghĩa và những vấn đề kinh tế trong thời kỳ quá độ lên CNXH ở Việt Nam.");
                assignTeacherToSubject(gvLyLuan.getId(), pol101.getId());

                Subject pol102 = createSubject("POL102", "Pháp luật đại cương", 2,
                                "Cung cấp hệ thống kiến thức cơ bản về Nhà nước và Pháp luật, quyền và nghĩa vụ cơ bản của công dân.");
                assignTeacherToSubject(gvLyLuan.getId(), pol102.getId());

                Subject pol103 = createSubject("POL103", "Lịch sử Đảng cộng sản Việt Nam", 2,
                                "Nghiên cứu quá trình ra đời, lãnh đạo đấu tranh giải phóng dân tộc và xây dựng đất nước của Đảng Cộng sản Việt Nam.");
                assignTeacherToSubject(gvLyLuan.getId(), pol103.getId());

                Subject pol104 = createSubject("POL104", "Chủ nghĩa xã hội khoa học", 2,
                                "Nghiên cứu những vấn đề lý luận chính trị - xã hội cơ bản của Chủ nghĩa Mác - Lênin về giai cấp công nhân và cách mạng XHCN.");
                assignTeacherToSubject(gvLyLuan.getId(), pol104.getId());

                Subject pol105 = createSubject("POL105", "Triết học Mác - Lê nin", 3,
                                "Cung cấp thế giới quan và phương pháp luận triết học duy vật biện chứng, giúp sinh viên nhận thức đúng đắn về tự nhiên, xã hội và tư duy.");
                assignTeacherToSubject(gvLyLuan.getId(), pol105.getId());

                // Nhóm 3: teacher_cntt (Công nghệ thông tin)
                Subject cse201 = createSubject("CSE201", "Khoa học dữ liệu và trí tuệ nhân tạo", 2,
                                "Giới thiệu các thuật toán AI cơ bản, Machine Learning, Deep Learning và quy trình khai phá dữ liệu trong thực tế.");
                assignTeacherToSubject(gvCNTT.getId(), cse201.getId());

                Subject cse202 = createSubject("CSE202", "Lập trình cho thiết bị di động", 2,
                                "Thực hành phát triển ứng dụng di động đa nền tảng sử dụng các công cụ và framework hiện đại (React Native/Flutter/Android).");
                assignTeacherToSubject(gvCNTT.getId(), cse202.getId());

                Subject cse203 = createSubject("CSE203", "Cơ sở dữ liệu", 3,
                                "Nghiên cứu mô hình dữ liệu quan hệ, ngôn ngữ truy vấn SQL, thiết kế CSDL chuẩn hóa. Yêu cầu cài đặt MySQL/PostgreSQL trên máy cá nhân.");
                assignTeacherToSubject(gvCNTT.getId(), cse203.getId());

                Subject cse204 = createSubject("CSE204", "Mạng máy tính", 2,
                                "Tìm hiểu kiến trúc mạng máy tính, cấu trúc và chức năng của mô hình OSI/TCP-IP cùng các giao thức mạng phổ biến.");
                assignTeacherToSubject(gvCNTT.getId(), cse204.getId());

                Subject cse205 = createSubject("CSE205", "Lập trình hướng đối tượng", 3,
                                "Nắm vững 4 tính chất cốt lõi của OOP (Đóng gói, Kế thừa, Đa hình, Trừu tượng) thông qua ngôn ngữ lập trình Java hoặc C++.");
                assignTeacherToSubject(gvCNTT.getId(), cse205.getId());

                System.out.println("=============== ĐÃ KHỞI TẠO DỮ LIỆU MẪU THÀNH CÔNG ===============");
                System.out.println("Admin: admin / 123456");
                System.out.println("Giáo viên: teacher_toan, teacher_lyluan, teacher_cntt / 123456");
                System.out.println("Sinh viên: student_1 ... student_10 / 123456");
        }

        private Student createUser(String lastName, String firstName, String username, String email, String role,
                        String password) {
                Student user = new Student();
                user.setLastName(lastName);
                user.setFirstName(firstName);
                user.setUsername(username);
                user.setEmail(email);
                user.setRole(role);
                user.setPassword(password);
                userRepository.persist(user);
                return user;
        }

        private Subject createSubject(String code, String name, int credits, String description) {
                Subject sub = new Subject();
                sub.setCode(code);
                sub.setName(name);
                sub.setCredits(credits);
                sub.setDescription(description);
                subjectRepository.persist(sub);
                return sub;
        }

        private void assignTeacherToSubject(Long teacherId, Long subjectId) {
                TeacherSubject ts = new TeacherSubject();
                ts.setTeacherId(teacherId);
                ts.setSubjectId(subjectId);
                em.persist(ts);
        }
}