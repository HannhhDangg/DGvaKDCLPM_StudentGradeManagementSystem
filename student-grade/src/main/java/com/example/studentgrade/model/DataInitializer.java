package com.example.studentgrade.model;

import com.example.studentgrade.model.Student;
import com.example.studentgrade.model.Subject;
import com.example.studentgrade.model.TeacherSubject;
import com.example.studentgrade.model.Semester;
import com.example.studentgrade.model.Classroom;
import com.example.studentgrade.model.Enrollment;
import com.example.studentgrade.model.Grade;
import com.example.studentgrade.repository.StudentRepository;
import com.example.studentgrade.repository.SubjectRepository;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

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
                // Khởi tạo Học kỳ nếu chưa có
                boolean isNewDb = false;
                Long semCount = em.createQuery("SELECT COUNT(s) FROM Semester s", Long.class).getSingleResult();
                if (semCount == 0) {
                        isNewDb = true;
                        createSemester("Học kỳ 1", "2025-06-27", "2025-08-30");
                        createSemester("Học kỳ 2", "2025-10-30", "2026-01-05");
                        createSemester("Học kỳ 3", "2026-03-10", "2026-05-20");
                        createSemester("Học kỳ 4", "2026-06-27", "2026-08-30");
                        createSemester("Học kỳ 5", "2026-10-30", "2027-01-05");
                        createSemester("Học kỳ 6", "2027-03-10", "2027-05-20");
                        System.out.println("=============== ĐÃ KHỞI TẠO DỮ LIỆU HỌC KỲ ===============");
                }

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

                // 3. TẠO 20 TÀI KHOẢN SINH VIÊN TỰ ĐỘNG
                List<Student> students = new ArrayList<>();
                for (int i = 1; i <= 20; i++) {
                        students.add(createUser("Sinh viên", String.format("%02d", i), "student_" + i,
                                        "sv" + i + "@edumanage.com",
                                        "STUDENT", "123456"));
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

                // Nhóm 4: Danh sách các môn học bổ sung từ chương trình đào tạo
                Object[][] additionalSubjects = {
                                { "EEE703044", "Kỹ thuật số", 3,
                                                "Nghiên cứu các hệ thống số, cổng logic, mạch tổ hợp và mạch tuần tự cơ bản." },
                                { "CSE703010", "Đánh giá và kiểm định chất lượng phần mềm", 3,
                                                "Các phương pháp kiểm thử, đảm bảo chất lượng và quy trình đánh giá phần mềm." },
                                { "CSE702036", "Mạng máy tính", 2,
                                                "Cấu trúc, giao thức và nguyên lý hoạt động của mạng máy tính, mô hình OSI và TCP/IP." },
                                { "CSE703029", "Lập trình hướng đối tượng", 3,
                                                "Nguyên lý lập trình hướng đối tượng với Java/C++, đóng gói, kế thừa và đa hình." },
                                { "CSE703023", "Kiến trúc máy tính", 3,
                                                "Nghiên cứu cấu trúc và nguyên lý hoạt động của hệ thống máy tính, CPU, bộ nhớ." },
                                { "CSE702025", "Kỹ thuật phần mềm", 2,
                                                "Quy trình phát triển phần mềm, vòng đời phần mềm, mô hình Agile/Scrum." },
                                { "CSE702040", "Nhập môn Công nghệ thông tin", 2,
                                                "Tổng quan về ngành CNTT, máy tính, mạng và các hướng chuyên ngành." },
                                { "CSE703004", "An toàn và bảo mật thông tin", 3,
                                                "Các khái niệm cơ bản về mật mã học, an toàn mạng và bảo vệ dữ liệu." },
                                { "CSE703048", "Phân tích và thiết kế phần mềm", 3,
                                                "Sử dụng UML để phân tích yêu cầu và thiết kế kiến trúc phần mềm." },
                                { "CSE702011", "Điện toán đám mây", 2,
                                                "Tìm hiểu về các mô hình dịch vụ IaaS, PaaS, SaaS và ảo hóa." },
                                { "CSE703038", "Ngôn ngữ lập trình C", 3,
                                                "Cung cấp kiến thức cơ bản về lập trình C, con trỏ, mảng và cấp phát bộ nhớ." },
                                { "CSE703064", "Xây dựng ứng dụng web", 3,
                                                "Phát triển web cơ bản với HTML, CSS, JavaScript và Backend cơ bản." },
                                { "CSE702017", "Hệ điều hành", 2,
                                                "Nguyên lý hoạt động của HĐH, quản lý tiến trình, bộ nhớ và hệ thống file." },
                                { "CSE703006", "Cấu trúc dữ liệu và thuật toán", 3,
                                                "Nghiên cứu danh sách liên kết, cây, đồ thị và các thuật toán sắp xếp, tìm kiếm." },
                                { "CSE702117", "Kỹ năng viết và thuyết trình bằng Tiếng Anh", 2,
                                                "Nâng cao kỹ năng giao tiếp học thuật và chuyên ngành bằng Tiếng Anh." },
                                { "FBE702001", "Quản trị học", 2,
                                                "Cung cấp nền tảng về tổ chức, quản lý, lãnh đạo và ra quyết định trong doanh nghiệp." },
                                { "FTS702001", "Kỹ năng khởi nghiệp và lãnh đạo", 2,
                                                "Kỹ năng lập kế hoạch kinh doanh, tinh thần khởi nghiệp và kỹ năng lãnh đạo nhóm." },
                                { "FTS702003", "Kỹ năng đàm phán, thương lượng", 2,
                                                "Phát triển kỹ năng giao tiếp, chiến lược và kỹ thuật đàm phán hiệu quả." },
                                { "FTS702004", "Kỹ năng tư duy sáng tạo và phản biện", 2,
                                                "Phương pháp rèn luyện tư duy logic, phân tích vấn đề đa chiều." },
                                { "FTS702002", "Kỹ năng quản lý dự án", 2,
                                                "Kiến thức cơ bản về lập kế hoạch, phân bổ nguồn lực và quản lý rủi ro dự án." },
                                { "CSE702051", "Thiết kế web nâng cao", 2,
                                                "Xây dựng ứng dụng web hiện đại, sử dụng framework và tối ưu hóa UI/UX." },
                                { "CSE702005", "Bảo mật ứng dụng và hệ thống", 2,
                                                "Kỹ thuật tấn công và phòng thủ mạng, bảo mật ứng dụng web và hệ điều hành." },
                                { "CSE703018", "Hệ nhúng", 3,
                                                "Thiết kế và lập trình cho các hệ thống nhúng vi điều khiển." },
                                { "CSE702063", "Ứng dụng phân tán", 2,
                                                "Cơ sở lý thuyết và công nghệ xây dựng các hệ thống tính toán phân tán." },
                                { "CSE702022", "Khai phá dữ liệu", 2,
                                                "Kỹ thuật Data Mining, trích xuất tri thức và mô hình hóa dữ liệu." },
                                { "CSE702049", "Quản trị dự án công nghệ thông tin", 2,
                                                "Phương pháp quản lý các dự án phần mềm chuyên sâu." },
                                { "CSE703110", "Kiến trúc phần mềm", 3,
                                                "Các mẫu kiến trúc (Design Patterns) và mô hình triển khai phần mềm quy mô lớn." },
                                { "CSE702060", "Trực quan hoá dữ liệu", 2,
                                                "Kỹ thuật và công cụ vẽ biểu đồ, Data Visualization để báo cáo phân tích." },
                                { "CSE702103", "Linux và phần mềm mã nguồn mở", 2,
                                                "Quản trị hệ điều hành Linux và sử dụng các công cụ mã nguồn mở." },
                                { "CSE703102", "Thương mại điện tử", 3,
                                                "Mô hình kinh doanh trực tuyến, thanh toán điện tử và hệ thống thương mại điện tử." },
                                { "CSE703098", "Lập trình phân tán", 3,
                                                "Lập trình đa luồng, sockets, RMI và các dịch vụ Web Services." },
                                { "CSE703009", "Công nghệ .Net", 3,
                                                "Phát triển phần mềm trên nền tảng .NET Framework/Core bằng C#." },
                                { "CSE702046", "Phân tích nghiệp vụ kinh doanh", 2,
                                                "Kỹ năng phân tích nghiệp vụ, quy trình và đặc tả yêu cầu phần mềm." },
                                { "CSE702031", "Lập trình phân tích dữ liệu với python", 2,
                                                "Sử dụng Python (Pandas, NumPy) để phân tích và xử lý dữ liệu." },
                                { "CSE702133", "Ứng dụng WebGIS", 2,
                                                "Xây dựng các hệ thống thông tin địa lý trên nền tảng web." },
                                { "CSE703007", "Chương trình dịch", 3,
                                                "Nguyên lý hoạt động của trình biên dịch, phân tích từ vựng và cú pháp." },
                                { "CSE702043", "Phân tích dữ liệu", 2,
                                                "Phương pháp thống kê và công cụ trực quan hóa hỗ trợ phân tích dữ liệu." },
                                { "CSE703130", "Công nghệ Java", 3,
                                                "Lập trình ứng dụng doanh nghiệp quy mô lớn với Java EE/Spring Boot." },
                                { "CSE703099", "Các hệ nhúng", 3,
                                                "Thực hành lập trình nhúng chuyên sâu trên các nền tảng vi điều khiển." },
                                { "CSE703132", "Lập trình C nâng cao", 3,
                                                "Kỹ thuật lập trình C cấp cao, tối ưu hóa bộ nhớ và cấu trúc dữ liệu." },
                                { "CSE702028", "Lập trình cho trí tuệ nhân tạo", 2,
                                                "Sử dụng các thư viện AI để giải quyết bài toán thị giác máy tính và NLP." },
                                { "CSE703112", "Bảo mật hệ thống", 3,
                                                "Bảo vệ hệ thống mạng và máy chủ khỏi các rủi ro an ninh mạng." },
                                { "CSE702033", "Lập trình trò chơi", 2,
                                                "Thiết kế và phát triển game sử dụng Unity/Unreal Engine." },
                                { "CSE703032", "Lập trình song song", 3,
                                                "Kỹ thuật lập trình song song và đa luồng, CUDA, OpenMP." },
                                { "CSE703015", "Đồ hoạ máy tính và thực tế ảo", 3,
                                                "Nguyên lý đồ họa 3D, OpenGL và công nghệ thực tế ảo VR/AR." },
                                { "CSE703093", "An toàn phần mềm", 3,
                                                "Lập trình an toàn (Secure Coding) để ngăn ngừa lỗ hổng bảo mật." },
                                { "CSE703037", "Mạng nơron và học sâu", 3,
                                                "Nghiên cứu Deep Learning, mạng nơ-ron tích chập (CNN), mạng hồi quy (RNN)." },
                                { "EEE703068", "Thị giác máy tính", 3,
                                                "Xử lý ảnh, trích xuất đặc trưng và nhận diện đối tượng bằng thị giác máy." },
                                { "CSE703097", "Công nghệ chuỗi khối", 3,
                                                "Nguyên lý hoạt động của Blockchain, Smart Contracts và tiền điện tử." },
                                { "CSE703054", "Tích hợp và phân tích dữ liệu lớn", 3,
                                                "Công nghệ Big Data, Hadoop, Spark và xử lý dữ liệu phân tán." },
                                { "CSE703065", "Xử lý ngôn ngữ tự nhiên", 3,
                                                "Kỹ thuật NLP, phân tích cú pháp, phân tích cảm xúc và dịch tự động." },
                                { "CSE703134", "Xử lý dữ liệu GIS thông minh", 3,
                                                "Phân tích dữ liệu không空间 đa chiều và hệ thống thông tin địa lý." },
                                { "CSE704067", "Thực tập tốt nghiệp", 4,
                                                "Kỳ thực tập và làm việc thực tế tại các doanh nghiệp CNTT để lấy kinh nghiệm." },
                                { "CSE702131", "Đồ án cơ sở Công nghệ Thông tin", 2,
                                                "Sinh viên thực hiện đồ án nhỏ để vận dụng kiến thức cơ sở ngành." },
                                { "CSE702053", "Thực tập công nghiệp", 2,
                                                "Kỳ thực tập chuyên sâu tập trung vào quy trình công nghiệp phần mềm." },
                                { "CSE703014", "Đồ án liên ngành", 3,
                                                "Thực hiện dự án kết hợp nhiều chuyên ngành, giải quyết bài toán quy mô lớn." },
                                { "CSE710068", "Đồ án tốt nghiệp", 10,
                                                "Nghiên cứu khoa học và hoàn thành sản phẩm khóa luận tốt nghiệp kỹ sư." },
                                { "FFS708066", "Giáo dục quốc phòng - an ninh", 8,
                                                "Cung cấp kiến thức quốc phòng, an ninh và rèn luyện thể lực, kỷ luật quân sự." },
                                { "FFS701072", "Chạy 1", 1,
                                                "Giáo dục thể chất cơ bản: rèn luyện sức bền thông qua môn chạy bộ ngoài trời." },
                                { "FFS701073", "Aerobic", 1,
                                                "Giáo dục thể chất: tập luyện thể dục nhịp điệu giúp nâng cao sức khỏe và độ dẻo dai." },
                                { "FFS701068", "Bóng chuyền", 1,
                                                "Giáo dục thể chất: tìm hiểu luật chơi, rèn luyện kỹ thuật và chiến thuật bóng chuyền." },
                                { "FFS701070", "Cầu lông", 1,
                                                "Giáo dục thể chất: luật chơi, kỹ thuật phát bóng, đập cầu và di chuyển trên sân." },
                                { "FFS701069", "Bóng đá", 1,
                                                "Giáo dục thể chất: kỹ thuật dẫn bóng, chuyền bóng và phối hợp đồng đội môn thể thao vua." },
                                { "FFS701067", "Bóng rổ", 1,
                                                "Giáo dục thể chất: kỹ thuật ném rổ, phòng thủ, qua người và chiến thuật bóng rổ." },
                                { "FEL704000", "Tiếng Anh bổ trợ", 2,
                                                "Tăng cường vốn từ vựng học thuật và ngữ pháp tiếng Anh tổng quát cho sinh viên." }
                };

                for (Object[] row : additionalSubjects) {
                        String code = (String) row[0];
                        String name = (String) row[1];
                        int credits = (int) row[2];
                        String desc = (String) row[3];

                        Subject sub = createSubject(code, name, credits, desc);

                        // Phân công môn học cho giảng viên tương ứng dựa trên mã môn
                        if (code.startsWith("FFS") || code.startsWith("FBE") || code.startsWith("FTS")
                                        || code.startsWith("FEL")) {
                                assignTeacherToSubject(gvLyLuan.getId(), sub.getId());
                        } else {
                                assignTeacherToSubject(gvCNTT.getId(), sub.getId());
                        }
                }

                if (isNewDb) {
                        List<Semester> sems = em
                                        .createQuery("SELECT s FROM Semester s ORDER BY s.id ASC", Semester.class)
                                        .setMaxResults(3).getResultList();
                        List<Subject> allSubs = em
                                        .createQuery("SELECT s FROM Subject s ORDER BY s.id ASC", Subject.class)
                                        .getResultList();

                        if (sems.size() == 3 && allSubs.size() >= 18) {
                                int subIndex = 0;
                                for (int i = 0; i < 3; i++) {
                                        Semester currentSem = sems.get(i);
                                        for (int j = 0; j < 6; j++) { // Tạo 6 môn cho mỗi Học kỳ
                                                Subject sub = allSubs.get(subIndex++);
                                                Student assignedTeacher = gvCNTT;
                                                try {
                                                        Long tid = em.createQuery(
                                                                        "SELECT ts.teacherId FROM TeacherSubject ts WHERE ts.subjectId = :sid",
                                                                        Long.class)
                                                                        .setParameter("sid", sub.getId())
                                                                        .setMaxResults(1).getSingleResult();
                                                        Student t = em.find(Student.class, tid);
                                                        if (t != null)
                                                                assignedTeacher = t;
                                                } catch (Exception e) {
                                                }

                                                createClassAndGrades(currentSem, sub, assignedTeacher, students);
                                        }
                                }
                        }
                }

                System.out.println("=============== ĐÃ KHỞI TẠO DỮ LIỆU MẪU THÀNH CÔNG ===============");
                System.out.println("Admin: admin / 123456");
                System.out.println("Giáo viên: teacher_toan, teacher_lyluan, teacher_cntt / 123456");
                System.out.println("Sinh viên: student_1 ... student_20 / 123456");
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

        private Semester createSemester(String name, String startDate, String endDate) {
                Semester s = new Semester();
                s.setName(name);
                s.setStartDate(startDate);
                s.setEndDate(endDate);
                em.persist(s);
                return s;
        }

        private void createClassAndGrades(Semester sem, Subject sub, Student teacher, List<Student> students) {
                int batchSize = 10;
                int totalStudents = students.size();
                int numClasses = (int) Math.ceil((double) totalStudents / batchSize);

                for (int batch = 0; batch < numClasses; batch++) {
                        String classCode = String.format("N%02d", batch + 1); // VD: N01, N02

                        Classroom c = new Classroom();
                        c.setName(sub.getName() + " - " + classCode + " (" + sem.getName() + ")");
                        c.setDescription(sub.getDescription());
                        c.setTeacherId(teacher.getId());
                        c.setSubjectId(sub.getId());
                        c.setSubjectName(sub.getName());
                        c.setSemesterId(sem.getId());
                        c.setSemesterName(sem.getName());
                        c.setStartDate(sem.getStartDate());
                        c.setEndDate(sem.getEndDate());
                        em.persist(c);

                        int startIdx = batch * batchSize;
                        int endIdx = Math.min(startIdx + batchSize, totalStudents);

                        for (int i = startIdx; i < endIdx; i++) {
                                Student s = students.get(i);
                                Enrollment e = new Enrollment();
                                e.setStudentId(s.getId());
                                e.setClassroomId(c.getId());
                                e.setStatus("APPROVED");
                                em.persist(e);

                                Grade g = new Grade();
                                g.setStudent(s);
                                g.setSubject(sub);

                                // Năng lực cốt lõi của sinh viên (giúp định hình GPA tổng từ 5.5 đến 9.2)
                                double[] abilities = { 9.2, 8.8, 8.4, 7.8, 7.5, 7.0, 6.5, 6.0, 5.5, 6.8 };
                                double ability = abilities[i % 10];

                                // Phong độ ngẫu nhiên của sinh viên trong môn học CỤ THỂ này (dao động -2.5 đến
                                // +2.5 điểm)
                                // Giúp 1 sinh viên có môn Giỏi, môn Khá, môn Trung bình, thậm chí môn Kém/Trượt
                                double subjectAbility = ability + (Math.random() * 5.0 - 2.5);

                                double a = Math.max(0, Math.min(10.0, subjectAbility + (Math.random() * 2 - 1)));
                                double m = Math.max(0, Math.min(10.0, subjectAbility + (Math.random() * 2 - 1)));
                                double f = Math.max(0, Math.min(10.0, subjectAbility + (Math.random() * 2 - 1)));

                                a = Math.round(a * 10.0) / 10.0;
                                m = Math.round(m * 10.0) / 10.0;
                                f = Math.round(f * 10.0) / 10.0;

                                g.setAttendanceScore(a);
                                g.setMidtermScore(m);
                                g.setFinalExamScore(f);
                                double finalScore = Math.round(((a * 0.1) + (m * 0.3) + (f * 0.6)) * 10.0) / 10.0;
                                g.setFinalScore(finalScore);

                                em.persist(g);
                        }
                }
        }
}