# Tiến độ triển khai

| Giai đoạn | Trạng thái | Bằng chứng hoặc điều kiện nghiệm thu |
|---|---|---|
| 0 · Kiểm kê và thiết kế | Hoàn thành | Đối chiếu README, thiết kế DB, ERD, V1–V7, pom, source MVC, 34 Bean và báo cáo kiểm thử nền tảng. Checker tĩnh đạt 34 bảng, 265 cột và 60 FK. Ma trận đủ 34 dòng. |
| 1 · JDBC Connection & MVC Foundation | COMPLETED | Build WAR, transaction giả lập, HTTP Tomcat và SELECT trên MySQL thật đều đạt. Phạm vi lần này chỉ gồm nền tảng JDBC/MVC. Auth trong roadmap vẫn chờ triển khai. |
| 2 · Admin và đào tạo | Chưa triển khai | Kiểm tra phân quyền, ghi danh, khóa tài khoản, rollback và audit. |
| 2A · Authentication và phiên đăng nhập | PARTIAL | Build, kiểm thử cô lập và smoke HTTP đạt. Chờ nghiệm thu login/logout trên MySQL thật với dữ liệu được phép. |
| 3 · Câu hỏi | Chưa triển khai | Kiểm tra quyền bank, version bất biến, số đáp án đúng và file cùng version. |
| 4 · Đề và phân công | Chưa triển khai | Publish nguyên tử, snapshot policy, kiểm tra cùng môn, cửa sổ và override. |
| 5 · Làm bài | Chưa triển khai | Hai request start đồng thời chỉ tạo một lượt. Autosave đúng option, giữ thứ tự sau khi resume. |
| 6 · Kết quả | Chưa triển khai | Submit lặp chỉ chấm một lần, độ chính xác DECIMAL, quyền xem và điều chỉnh điểm có audit. |
| 7 · Giám sát | Chưa triển khai | Retry event không nhân đôi, kiểm tra quyền tải evidence, lưu reviewer và lý do. |
| 8 · Phúc khảo và báo cáo | Chưa triển khai | Đơn đúng chủ lượt, thông báo đúng người, báo cáo khớp policy chọn lượt. |

Ở mỗi giai đoạn, ghi ngày, người phụ trách, file thay đổi, lệnh test, kết quả thực tế và quyết định còn mở trước khi chuyển trạng thái. Lần rà soát giai đoạn 0 chỉ chạy checker tĩnh. Kết quả WAR/Tomcat trước đó thuộc [báo cáo nền tảng](verification.md). Migration và seed chưa được chạy trong giai đoạn 0.
## Nghiệm thu JDBC/MVC ngày 20/11/2026

- Tạo `util/ConnectionFactory.java`, `service/JdbcUnitOfWork.java`, `dao/RoleDao.java` dưới `src/main/java/vn/edu/onlineexam`, cùng `scripts/JdbcSmoke.java` và `scripts/VerifyJdbcTransaction.java`. Cập nhật tài liệu tiến độ này. POM đã có Connector/J 9.6.0 ở scope runtime.
- Cấu hình DB_URL (jdbc:mysql://host:port/schema, không nhúng thông tin đăng nhập), DB_USER và DB_PASSWORD bằng môi trường của tiến trình Tomcat/CLI. Dùng tài khoản riêng với quyền tối thiểu. Factory mở connection khi Service cần và đặt UTC cho session.
- Service dùng read cho một truy vấn độc lập hoặc transaction cho một use case cần tính nguyên tử. Truyền cùng Connection vào từng DAO trong callback. DAO đóng statement và result set. Unit of work đóng connection cả khi lỗi. Không tự retry giao dịch khi commit báo lỗi vì kết quả commit có thể chưa rõ.
- Mapper dùng tên cột, getString cho chuỗi nullable, getObject với kiểu wrapper cho số/Boolean nullable, getBigDecimal cho DECIMAL và getObject với LocalDateTime.class cho DATETIME(3) theo UTC. Optional.empty biểu diễn thiếu hàng. Giá trị NULL trong hàng vẫn là null.
- Controller kiểm tra định dạng tham số trước khi gọi Service. Service kiểm tra điều kiện nghiệp vụ. Trả 400 cho đầu vào sai, 403 cho thiếu quyền, 404 cho tài nguyên không tồn tại khi được phép tiết lộ. Với SQLException, trả lỗi server chung, không đưa message của driver, URL, credential hoặc stack trace vào response. GET forward DTO bằng request attribute tới JSP trong WEB-INF/views, chỉ hiển thị dữ liệu đã escape.
- Maven Wrapper clean package đạt với release 21. Test độc lập `java -cp target/classes scripts/VerifyJdbcTransaction.java` đạt trên Connection giả lập. HTTP Tomcat kiểm tra /home 200 cùng tiếng Việt, welcome route, CSS 200 và truy cập trực tiếp JSP trả 404.
- Smoke test JDBC đã biên dịch. Khi có schema thử nghiệm đã triển khai V1 và tài khoản SELECT được phép, cấu hình ba biến trên rồi chạy từ gốc repo sau build bằng PowerShell:

  ```powershell
  java -cp "target/classes;target/online-exam/WEB-INF/lib/*" scripts/JdbcSmoke.java
  ```

  CLI xác minh DATABASE(), UTC, charset client/connection, liệt kê bảng và tra một role hiện có bằng RoleDao. Nếu roles trống, CLI kiểm tra Optional.empty. Nếu bảng chưa tồn tại, SELECT thất bại.

  Lần nghiệm thu bổ sung dùng JDBC URL `jdbc:mysql://127.0.0.1:3306/online_exam_dev`, user `root` do chủ máy chỉ định, Connector/J 9.6.0. Maven clean package và VerifyJdbcTransaction đạt. CLI đã chạy với classpath trên và dừng trước khi mở kết nối vì thiếu DB_PASSWORD trong môi trường. Đây là kết quả của lần thử trước khi chủ máy cấu hình mật khẩu. Chỉ cập nhật JdbcSmoke.java và tài liệu tiến độ trong lần này.
- Cần có luồng xác thực/phân quyền theo hợp đồng trước khi mở các chức năng Admin. Chưa triển khai chức năng nghiệp vụ tiếp theo.

### Bổ sung cấu hình .env

ConnectionFactory và CLI JdbcSmoke dùng chung JdbcSettings. Đã tạo .env.example và bản .env cục bộ có DB_PASSWORD trống. Quy tắc Git hiện tại đã bỏ qua .env. README hướng dẫn terminal, IntelliJ và đường dẫn tuyệt đối cho Tomcat.

Maven clean package đạt. Kiểm tra nội dung WAR và Git index xác nhận .env nằm ngoài cả hai. Kiểm thử cấu hình độc lập đạt cho fallback file, ưu tiên từng biến môi trường, bỏ qua file khi đủ biến và từ chối dòng sai định dạng. Smoke HTTP Tomcat đạt cho /home, welcome route, CSS và bảo vệ WEB-INF. CLI thật dừng trước khi kết nối vì thiếu DB_PASSWORD. Kết quả lần chạy sau trên máy phát triển được ghi dưới đây.

### Kết quả nghiệm thu cuối cùng trên MySQL thật

Theo kết quả terminal do chủ dự án cung cấp, JdbcSmoke kết nối online_exam_dev qua Connector/J 9.6.0, xác nhận UTC và utf8mb4, nhìn thấy 34 bảng cơ sở và kết thúc với PASS: JDBC read-only smoke. RoleDao thực hiện SELECT thành công trên bảng roles trống và trả Optional.empty. Kết quả này hoàn tất phạm vi JDBC/MVC giai đoạn 1.

Kiểm thử commit/rollback và đóng kết nối dùng Connection giả lập. Smoke test MySQL chỉ đọc, chưa kiểm tra transaction ghi hoặc rollback trên MySQL thật, ánh xạ một dòng Role có dữ liệu, hay các ràng buộc của toàn bộ 34 bảng.

**Dữ liệu chuẩn bị cho 2A.** Tại thời điểm smoke test, roles thiếu cả ADMIN, TEACHER và STUDENT. Seed phát triển dự kiến gán ba vai trò này cho DEV_ADMIN, DEV_TEACHER và DEV_STUDENT. Smoke test chưa kiểm tra users, user_roles hoặc các bảng nghiệp vụ nên trạng thái các dữ liệu mẫu đó chưa xác định. Giai đoạn 2A chưa bắt đầu.

### 2A — Kết quả ngày 20/11/2026

AuthServlet phục vụ GET/POST /login, POST /logout và trang tài khoản tối thiểu cho ba vai trò. AuthenticationService xác minh email, PasswordHasher, ACTIVE và quyền, ghi login_sessions cùng last_login_at trong một transaction. Sau thành công, Servlet đổi session ID và CSRF token. AuthenticationFilter kiểm tra CSRF cho request thay đổi trạng thái và kiểm tra lại phiên, trạng thái tài khoản, quyền cho các route dưới /admin/, /teacher/, /student/. Trước khi thêm module mới phải đặt route trong vùng bảo vệ và kiểm tra quyền đối tượng tại Service.

HttpSession timeout 30 phút. MySQL giới hạn idle 30 phút, thời gian tuyệt đối 8 giờ. Khi hết hạn, hàng phiên được giữ làm lịch sử. Logout thu hồi phiên tại DB và hủy HttpSession. Cookie dùng HttpOnly, SameSite=Lax, tracking bằng cookie. Tomcat đánh dấu Secure khi request tới connector HTTPS; khi dùng reverse proxy cần cấu hình connector nhận đúng scheme. Giới hạn 10 lần thử/địa chỉ peer/5 phút nằm trong bộ nhớ một JVM.

File mới: AuthUser.java, AuthDao.java, AuthenticationService.java, AuthServlet.java, AuthenticationFilter.java, WEB-INF/views/login.jsp, WEB-INF/views/account.jsp, META-INF/context.xml, scripts/VerifyAuth.java. File cập nhật: WEB-INF/web.xml, scripts/smoke-test.ps1 và tài liệu này.

Maven clean package đạt. VerifyAuth (chạy riêng, servlet/JDBC/DAO giả lập) đạt cho đúng/sai mật khẩu, user thiếu/khóa/inactive/thiếu role, tạo và đổi ID phiên, ba role và từ chối sai quyền, phiên mất hiệu lực, logout, giới hạn đăng nhập và bind định danh có dấu nháy bằng PreparedStatement. HTTP Tomcat thật đạt cho trang chủ, CSS, bảo vệ WEB-INF, JSP login, redirect khách, logout GET 405 và POST thiếu CSRF 403. Chưa chạy trình duyệt HTTPS hoặc login thành công qua Tomcat + MySQL. Kiểm thử JDBC giai đoạn 1 chỉ là SELECT.

Để nghiệm thu phần còn lại cần schema thử nghiệm biệt lập, user ACTIVE có hash đúng PasswordHasher, role và user_roles tương ứng. Sau khi chủ dự án cho phép ghi trên DB thử, thực hiện login/logout qua Tomcat, so cookie trước/sau và kiểm tra last_login_at, session_hash, last_seen_at, expires_at, revoked_at. Kiểm tra lại URL bảo vệ sau logout. Chưa chạy seed hay thao tác ghi MySQL trong đợt này.

### Dữ liệu development cho 2A — COMPLETED

Ngày 20/11/2026, kiểm tra qua TCP 127.0.0.1:3306 xác nhận đúng online_exam_dev, đủ 34 bảng theo V1–V7, tất cả trống và kiểm tra FK đang bật. Dùng MySQL CLI ở thư mục MySQL Server 9.6, chạy development-accounts.sql trước rồi development_data.sql. Hai transaction đã commit. Đối chiếu số hàng của cả 34 bảng sau seed xác nhận 25 hàng mới trong 17 bảng. DEV_ADMIN, DEV_TEACHER và DEV_STUDENT đều ACTIVE, mỗi tài khoản đúng một role cùng tên tương ứng. SELECT theo quan hệ đề thi xác nhận WEB101, DEV_WEB, ngân hàng Web mẫu và đề mẫu DRAFT. JdbcSmoke đạt với UTC, utf8mb4, 34 bảng và RoleDao ánh xạ được role có thật. Không chạy lại seed. Phần dữ liệu đã sẵn sàng; nghiệm thu login/logout qua Tomcat trên MySQL thật vẫn chờ, trạng thái chung 2A là PARTIAL.

### Nghiệm thu 2A trên Tomcat và MySQL — PARTIAL

WAR build với release 21, chạy trên Tomcat 10.1.61 bằng JDK 25.0.2. WAR triển khai thử khớp checksum với bản package. URL thực tế trong thời gian test là http://127.0.0.1:18080/online-exam/login. Tomcat đọc cấu hình qua ONLINE_EXAM_ENV_FILE. Server thử tự dừng khi kết thúc.

Đạt trên HTTP thật: trang chủ, welcome, CSS, bảo vệ WEB-INF, JSP login, redirect khách ở cả ba vùng quyền, GET logout 405, POST thiếu/sai CSRF 403, cookie HttpOnly/SameSite=Lax. Một lần sai mật khẩu DEV_STUDENT và một email không tồn tại đều trả 401 với thông báo chung, không thấy dấu hiệu lộ hash/SQL/stack trace được kiểm tra. VerifyAuth xác nhận xử lý phiên mất hiệu lực bằng dữ liệu giả lập. Timeout theo thời gian thật và HTTPS chưa kiểm thử.

Tạo scripts/AcceptAuth.java, bổ sung hai switch cho scripts/smoke-test.ps1. Chưa chạy login đúng cho ADMIN/TEACHER/STUDENT vì cần người giữ mật khẩu nhập cục bộ. Từ gốc repo chạy PowerShell:
```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.2'
$env:ONLINE_EXAM_ENV_FILE=(Resolve-Path .env).Path
.\scripts\smoke-test.ps1 -TomcatHome 'C:\Program Files\Apache Software Foundation\Tomcat 10.1' -AuthMysql -InteractivePasswords
```
Chạy khi không có người khác dùng ba tài khoản DEV. Script nhập ẩn từng mật khẩu, kiểm tra ACTIVE, đổi session ID, route đúng/sai quyền, last_login_at, hàng phiên mới, POST logout, revoked_at và phát lại cookie cũ. Chỉ ghi nhận đạt khi có PASS cho cả ba vai trò. Kiểm thử idle timeout riêng: với server chạy liên tục, để phiên hợp lệ không gửi request trong hơn 30 phút rồi mở trang account; kỳ vọng chuyển về login. Không sửa thời gian hoặc dữ liệu phiên để giả lập hết hạn trên MySQL.
