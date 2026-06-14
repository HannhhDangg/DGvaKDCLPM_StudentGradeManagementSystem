# EduManage - Hệ thống Quản lý Đào tạo và Điểm số

## 📖 Giới thiệu dự án
EduManage là một hệ thống phần mềm quản lý thông tin đào tạo toàn diện, được thiết kế để số hóa và tối ưu hóa các quy trình quản lý học vụ tại cơ sở giáo dục. Hệ thống kết nối ba đối tượng người dùng chính: **Quản trị viên (Admin)**, **Giảng viên (Teacher)** và **Sinh viên (Student)** thông qua các luồng nghiệp vụ khép kín từ khâu đăng ký môn học, quản lý lớp học, chấm điểm đến quản lý học phí.

Dự án này được phát triển kết hợp với quy trình kiểm thử nghiêm ngặt, phục vụ trực tiếp cho học phần **Đánh giá và Kiểm thử Phần mềm**.

---

## 🚀 Các chức năng chi tiết của hệ thống

### 1. Chức năng dành cho Quản trị viên (Admin)
Admin là người có toàn quyền kiểm soát hệ thống, đóng vai trò vận hành cốt lõi với các chức năng:
- **Quản lý Người dùng (Users):** Thêm mới, xóa, tìm kiếm và phân quyền tài khoản (Admin, Teacher, Student). Hỗ trợ bộ lọc xem danh sách người dùng theo vai trò.
- **Quản lý Môn học (Subjects):** Thêm, sửa, xóa thông tin môn học (Mã môn, Tên môn, Số tín chỉ, Mô tả). Đặc biệt, hệ thống phân loại môn học thành các nhóm (`Cơ bản`, `Đại cương`, `Kỹ thuật`) để làm cơ sở tính học phí động.
- **Quản lý Học kỳ (Semesters):** Khởi tạo và theo dõi các học kỳ (Tên học kỳ, Ngày bắt đầu, Ngày kết thúc) để lên lịch đào tạo cho toàn trường.
- **Thống kê & Báo cáo (Dashboard):** Cung cấp các biểu đồ trực quan (sử dụng Chart.js) thống kê về biến động điểm số, số lượng sinh viên, tình trạng học vụ chung.
- **Quản lý Học phí & Công nợ (Tuition):** 
  - Xem Dashboard thống kê tài chính: Tổng học phí đã thu, chưa thu, số lượng sinh viên đã hoàn thành hoặc còn nợ học phí.
  - Theo dõi chi tiết công nợ của từng sinh viên, hiển thị rõ số tín chỉ đã học và số tiền cần nộp.
  - Ghi nhận thanh toán thủ công (Tiền mặt, Chuyển khoản, Quẹt thẻ POS) cho sinh viên tại quầy. Hệ thống phân biệt rõ trạng thái thanh toán (Hoàn thành / Chưa đóng).

### 2. Chức năng dành cho Giảng viên (Teacher)
Giảng viên chịu trách nhiệm về chuyên môn và quản lý trực tiếp các lớp học được phân công:
- **Quản lý Lớp học (Classrooms):**
  - Khởi tạo lớp học mới cho các môn học được phân công giảng dạy.
  - Danh sách lớp được phân loại thông minh thành 3 nhóm: **Sắp diễn ra**, **Đang diễn ra**, và **Đã kết thúc** dựa trên so sánh thời gian thực với cấu hình học kỳ.
  - Chỉnh sửa thông tin hoặc xóa các lớp học do mình tạo ra.
- **Phê duyệt Đăng ký môn (Enrollments):** Tiếp nhận yêu cầu đăng ký từ sinh viên và thực hiện xét duyệt (Duyệt/Từ chối) sinh viên vào lớp.
- **Quản lý Điểm số (Grading):** 
  - Quản lý và nhập các đầu điểm thành phần: **Điểm Chuyên cần (10%)**, **Điểm Giữa kỳ (30%)** và **Điểm Cuối kỳ (60%)**.
  - Tính năng nội suy điểm: Hệ thống sẽ tự động tính toán Điểm tổng kết cực kỳ chính xác khi giảng viên cung cấp đủ 3 đầu điểm thành phần.

### 3. Chức năng dành cho Sinh viên (Student)
Sinh viên tương tác với hệ thống để theo dõi toàn bộ quá trình học tập cá nhân:
- **Đăng ký môn học (Enrollment):** 
  - Xem danh sách các lớp học đang mở theo từng học kỳ.
  - Gửi yêu cầu đăng ký tham gia lớp học và theo dõi trạng thái chờ giảng viên phê duyệt.
- **Xem Bảng điểm (Transcript):** 
  - Theo dõi bảng điểm chi tiết các môn học đã được phê duyệt.
  - Theo dõi các cột điểm thành phần. Điểm tổng kết sẽ chỉ hiển thị khi giảng viên đã hoàn tất việc chấm điểm cuối kỳ.
- **Tra cứu và Thanh toán Học phí:**
  - Hệ thống tự động tính toán tổng học phí phải nộp dựa trên số tín chỉ đăng ký thành công và đơn giá tùy theo loại môn học (ví dụ: Kỹ thuật - 750k/tín, Cơ bản - 650k/tín, Đại cương - 600k/tín).
  - Xem biến động công nợ chi tiết và tiến hành thanh toán học phí qua hệ thống.

---

## 🛠 Công cụ và Công nghệ sử dụng
- **Backend:** Java 21, Quarkus Framework, Hibernate ORM (Panache), RESTeasy Reactive.
- **Cơ sở dữ liệu:** H2 Database (có tích hợp `DataInitializer` tự động sinh dữ liệu mẫu và quan hệ phức tạp), hỗ trợ nâng cấp mượt mà sang MySQL/PostgreSQL.
- **Frontend:** HTML5, CSS3, JavaScript, Bootstrap 5, Chart.js, Qute Templating Engine.
- **Công cụ Kiểm thử:** JUnit 5, Selenium WebDriver (Automated E2E UI), JaCoCo (Đo lường Code Coverage).

---

## 🧪 Đánh giá và Kiểm thử Phần mềm (Software Testing & Evaluation)
Để minh chứng cho độ ổn định, hệ thống đi kèm với một bộ Test Suites mô phỏng các kịch bản kiểm thử sát thực tế nhất, áp dụng linh hoạt các lý thuyết kiểm thử:

### 1. Kiểm thử Hộp trắng (White-box Testing)
Thực hiện tại file `CoreLogicWhiteBoxTest.java`, can thiệp trực tiếp vào mã nguồn và cấu trúc cơ sở dữ liệu:
- **Kiểm tra luồng thuật toán (Statement/Branch Coverage):** Khẳng định thuật toán tính điểm (10% - 30% - 60%) luôn ra kết quả đúng. Bẫy lỗi (Edge Case) để ngăn chặn việc tính điểm tổng kết khi thiếu điểm thi cuối kỳ.
- **Kiểm thử Chuyển đổi Trạng thái (State Transitions):** Đảm bảo vòng đời của Học phí vận hành chuẩn: Nộp một phần (Giữ trạng thái `UNPAID`), Nộp đủ 100% tiền (Đổi trạng thái sang `PAID`). Đặc biệt, kiểm thử bảo mật từ chối giao dịch với số tiền âm (`-200.000`).
- **Kiểm thử Bảo mật (Negative Path):** Ngăn chặn hệ thống xử lý khi sinh viên tự ý gửi API gọi lệnh hủy đăng ký đối với các lớp học đã được giảng viên `APPROVED`.

### 2. Kiểm thử Hộp đen (Black-box Testing)
Thực hiện tại file `GradeBlackBoxTest.java`:
- **Phân tích Giá trị Biên (Boundary Value Analysis):** Truyền các đầu vào biên cho API lưu điểm của Giảng viên với giá trị lớn nhất (10.0) và nhỏ nhất (0.0). Khẳng định hệ thống tiếp nhận và xử lý mượt mà ở các ngưỡng sát giới hạn mà không bị crash.

### 3. Tự động hóa Kiểm thử Giao diện (E2E / UI Testing)
Sử dụng Selenium WebDriver (`SeleniumUITest.java`) để giả lập thao tác mở trình duyệt, nhấp chuột, gõ phím của người dùng thật:
- **Luồng Tích hợp Tuần tự (End-to-End Sequential Workflow):** Kịch bản mô phỏng tương tác thời gian thực (Real-time) trên cùng một cửa sổ trình duyệt bằng cách thực hiện tuần tự việc Đăng nhập/Đăng xuất qua từng vai trò:
  1. **Test Security:** Cố tình đăng nhập sai mật khẩu để xác minh hệ thống từ chối truy cập.
  2. **Luồng Admin:** Đăng nhập, điều hướng thêm mới một Học kỳ, sau đó đăng xuất.
  3. **Luồng Teacher 1:** Đăng nhập, mở Lớp học phần mới cho học kỳ vừa tạo, sau đó đăng xuất.
  4. **Luồng Student:** Đăng nhập, xem danh sách lớp, gửi yêu cầu đăng ký vào lớp học mới, đăng xuất.
  5. **Luồng Teacher 2:** Đăng nhập lại, tiếp nhận và "Duyệt" yêu cầu của sinh viên, mở modal nhập điểm.
     - *Negative Test (Kiểm thử Tiêu cực):* Cố tình gõ điểm ngoài khoảng cho phép (Chuyên cần `15.0`, Giữa kỳ `-5.0`) để xác minh Form (HTML5 Validation) tự động chặn đứng hành vi lưu sai quy định.
     - *Happy Path:* Sau khi kiểm chứng cảnh báo, script tự động sửa lại điểm hợp lệ (9.0, 8.5, 9.0) và lưu thành công, sau đó đăng xuất.

> **💡 Điểm nhấn Kỹ thuật (Technical Highlight):**
> Kịch bản E2E này tập trung vào tính liền mạch và kiểm tra bảo mật Session theo thời gian (Timeline-based State Transition). Bằng cách xử lý đăng nhập/đăng xuất (Login/Logout) tuần tự qua nhiều Route khác nhau, test script đảm bảo rằng các trạng thái được lưu trữ trong **H2 In-Memory Database** được cập nhật tức thời và chia sẻ chính xác giữa các quyền (Roles), đồng thời đánh giá được cơ chế chặn truy cập trái phép.