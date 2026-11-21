# Tiến độ triển khai

| Giai đoạn | Trạng thái | Bằng chứng hoặc điều kiện nghiệm thu |
|---|---|---|
| 0 · Kiểm kê và thiết kế | Hoàn thành | Đối chiếu README, thiết kế DB, ERD, V1–V7, pom, source MVC, 34 Bean và báo cáo kiểm thử nền tảng. Checker tĩnh đạt 34 bảng, 265 cột và 60 FK. Ma trận đủ 34 dòng. |
| 1 · JDBC Connection & MVC Foundation | COMPLETED | Build WAR, transaction giả lập, HTTP Tomcat và SELECT trên MySQL thật đều đạt. Phạm vi lần này chỉ gồm nền tảng JDBC/MVC. Auth trong roadmap vẫn chờ triển khai. |
| 2 · Admin và đào tạo | Chưa triển khai | Kiểm tra phân quyền, ghi danh, khóa tài khoản, rollback và audit. |
| 2A · Authentication và phiên đăng nhập | COMPLETED | Chủ dự án xác nhận cả ba vai trò đã đăng nhập/đăng xuất thành công trên Tomcat + MySQL. TLS và timeout theo thời gian thực vẫn pending. |
| 2B.1 · Danh sách tài khoản | PARTIAL | Đã triển khai GET /admin/users. MySQL SELECT và smoke HTTP khách đạt. Còn xác minh trang kết quả với phiên ADMIN thật và 403 cho hai vai trò còn lại qua HTTP. |
| 3 · Ngân hàng câu hỏi | PARTIAL | 3A hoàn thành; 3B–3C đã nghiệm thu phạm vi chủ sở hữu. Đọc SHARED của chủ khác còn chờ quyết định. Xem biên bản giai đoạn 3 bên dưới. |
| 4 · Đề và phân công | PARTIAL | Đã triển khai cấu hình, biên soạn và lịch thi. Xem biên bản giai đoạn 4 bên dưới. |
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
### Giai đoạn 2B.1

Từ trang tài khoản ADMIN chọn **Quản lý người dùng**. Tìm theo tên, mã hoặc email; lọc role/status; sắp xếp ngày tạo, tên, email hoặc mã, có ID làm khóa phụ. Service kiểm tra ADMIN, mặc định 20 dòng, nhận 1–100 dòng/trang. DAO dùng EXISTS cho lọc role và tổng hợp role trong truy vấn con để mỗi người có đúng một dòng. Chuỗi tìm kiếm và pagination được bind. Chỉ hai enum được phép quyết định ORDER BY.

Tạo UserDao, UserDirectory, UserManagementService, UserManagementServlet, Html, WEB-INF/views/admin/users.jsp và scripts/VerifyUserDirectory.java. Cập nhật AuthenticationFilter để chuyển AuthUser do server xác minh vào request, account.jsp để thêm liên kết và smoke-test.ps1 để kiểm tra khách tại URL mới.

Build WAR đạt. VerifyUserDirectory đạt trên ba tài khoản DEV với SELECT MySQL thật cho search/filter kết hợp, tổng, thứ tự trang, không lặp và không kết quả. Kiểm thử độc lập đạt cho escape HTML, từ chối quyền Service và tham số ngoài whitelist. VerifyAuth giả lập đạt. Smoke Tomcat đạt cho home, CSS, login, logout method, CSRF và chuyển khách từ admin/users về login. Chưa dùng phiên thật trong đợt 2B.1 vì AuthenticationService cập nhật last_seen_at khi xác thực request. Việc xác nhận trang JSP và phân quyền cuối tuyến với phiên thật vẫn chờ. Không test bằng cách thêm user đa vai trò vào dữ liệu mẫu.

### Cổng nghiệm thu trước 2B.2

2B.1 vẫn PARTIAL. Maven clean package, VerifyUserDirectory (SELECT trên online_exam_dev), VerifyAuth (giả lập) và HTTP smoke Tomcat cổng 18081 đã đạt khi kiểm tra lại. Khách bị chuyển về login, các kiểm tra âm tính MySQL/CSRF/cookie đạt. scripts/AcceptAuth.java đã bổ sung kiểm tra HTTP /admin/users trong phiên thật của ba vai trò, gồm tổng số, mã DEV, lọc kết hợp, trang thứ hai và từ chối truy cập ngoài quyền. Chưa chạy phần này vì phải nhập mật khẩu DEV qua console cục bộ. Dùng lệnh nghiệm thu tương tác ở trên với -Port 18081 -AuthMysql -InteractivePasswords. Chờ kết quả PASS cho ba vai trò trước khi triển khai create/update 2B.2.

### 2B.2 — Tạo và sửa tài khoản, 21/11/2026 — PARTIAL

Theo quyết định mới của chủ dự án, triển khai 2B.2 trong khi giữ 2B.1 PARTIAL. Chờ nghiệm thu HTTP với phiên thật cho cả hai giai đoạn.

- UserDao, UserDirectory, UserManagementService, UserManagementServlet và WEB-INF/views/admin/users.jsp đã được mở rộng. Thêm WEB-INF/views/admin/user-form.jsp, scripts/VerifyUserWrites.java. Bổ sung trường hợp GET khách và POST không có CSRF hợp lệ cho hai route mới vào scripts/smoke-test.ps1.
- GET/POST /admin/users/new và /admin/users/edit?id=... sử dụng Filter hiện có và xác minh ADMIN tại Service. Khi tạo, ADMIN phải chủ động chọn vai trò ban đầu theo quyền cấp vai trò trong hợp đồng. Ghi users, liên kết user_roles có assigned_by và audit trong một transaction; lấy status mặc định ACTIVE từ V1. Không có UI đổi vai trò sau đó. Mật khẩu tối thiểu 12 ký tự, yêu cầu xác nhận và băm bằng PasswordHasher. Form lỗi chỉ giữ thông tin hồ sơ và vai trò đã chọn.
- Chỉ sửa họ tên, email chưa xác minh, mã và điện thoại. Trường trống tùy chọn lưu NULL. Giữ email đã xác minh nếu chưa có quy trình xác minh lại. Khóa row FOR UPDATE và đối chiếu updated_at để tránh mất cập nhật. UNIQUE là hàng rào cuối khi hai lần gửi đồng thời. Lỗi dữ liệu 400, thiếu quyền 403, thiếu user 404, trùng hoặc phiên bản cũ 409, thành công redirect 303.
- Build WAR đạt. VerifyUserWrites dùng JDBC double để kiểm tra thao tác create/update, hash, validation, quyền, SQL tham số, rollback khi UNIQUE/thiếu role, stale version, không thấy user, và giới hạn cột profile. Đây không phải thử race bằng hai connection MySQL thật. VerifyUserDirectory đã chạy SELECT trên online_exam_dev; kiểm thử escape HTML và hồi quy VerifyAuth/VerifyJdbcTransaction đạt. Không ghi thử vào online_exam_dev.
- Nghiệm thu giao diện trên DB test riêng: đăng nhập ADMIN, vào /admin/users, chọn Thêm tài khoản, nhập email/mã mới, xác nhận mật khẩu và chọn vai trò; sau khi lưu kiểm tra thông báo, chỉ có một dòng mới, rồi sửa điện thoại từ chính dòng đó. Thử email/mã trùng, gửi bản sửa cũ từ tab thứ hai (409), kiểm tra đăng nhập bằng mật khẩu ban đầu sau sửa. Kiểm tra TEACHER/STUDENT bị 403. Không dùng ba DEV làm đối tượng sửa. HTTP ghi và đua UNIQUE thật vẫn pending.

### 2B.3 — Khóa/mở khóa — PARTIAL (21/11/2026)

2B.1 và 2B.2 tiếp tục PARTIAL. Trang danh sách có thao tác xác nhận cho ACTIVE → LOCKED và LOCKED → ACTIVE. INACTIVE không có thao tác. POST /admin/users/lock và POST /admin/users/unlock dùng CSRF chung. GET không cập nhật dữ liệu. PRG 303 quay về danh sách với các tham số tìm kiếm/lọc/sắp xếp/phân trang đã mã hóa lại từ danh sách khóa cho phép. Controller xử lý 400 (ID/phiên bản sai định dạng), 403 (quyền), 404 (thiếu user), 409 (xung đột).

Trong một transaction, Service khóa hàng roles.ADMIN làm điểm tuần tự hóa, khóa/tải các ADMIN ACTIVE và kiểm tra lại actor từ DB, khóa hàng target, cấm tự khóa/khóa ADMIN hoạt động cuối. UPDATE có điều kiện theo id, status, updated_at. Tăng version đơn điệu như sửa hồ sơ. Khóa tài khoản thu hồi mọi login_sessions chưa revoked và ghi audit; mở khóa chỉ chuyển trạng thái và ghi audit. Không phục hồi session. Luồng login hiện có khóa hàng users, kiểm tra ACTIVE trước khi tạo phiên; filter xác thực session qua JOIN users và điều kiện active/revoked/expiry. Module thay đổi quyền ADMIN tương lai cần tuân thủ cùng thứ tự khóa để giữ bất biến cuối cùng.

File sửa: UserDao, UserDirectory.Row, UserManagementService, UserManagementServlet, WEB-INF/views/admin/users.jsp, scripts/smoke-test.ps1 và tài liệu này. File mới: scripts/VerifyUserStatus.java.

- PASS: Maven clean package, VerifyAuth, VerifyUserWrites và VerifyUserDirectory (phần MySQL dev chỉ SELECT). Hồi quy bao gồm xác thực, hash, CSRF, hồ sơ, bộ lọc và escape HTML.
- PASS: MySQL Server 9.6 thật, instance tạm chỉ bind loopback cổng 13367, schema status_test riêng lấy V1, dữ liệu fixture tự sinh. VerifyUserStatus kiểm tra trạng thái sai, target thiếu, tự khóa, ADMIN cuối, actor không có quyền, version cũ, khóa/mở, login mới, từ chối session trước khóa ngay cả sau khi mở, hai ADMIN khóa chéo qua hai kết nối. Chèn lỗi SQLException có chủ ý tại bước session/audit trên connection thật để kiểm chứng rollback. Đã dừng instance tạm, không đổi .env. Test đầu tiên dùng version trước login và bị từ chối đúng vì login thay đổi last_login_at/updated_at; đã lấy version sau login và chạy đạt trên schema test mới.
- PASS HTTP Tomcat: guest bị chuyển về login tại hai endpoint, POST thiếu/sai CSRF bị 403, home/welcome/CSS/login vẫn hoạt động.
- NOT RUN: xác nhận end-to-end bằng trình duyệt và phiên thật cho ADMIN/TEACHER/STUDENT ở hai endpoint, thông báo/giữ bộ lọc sau POST, GET 405 và các mã 400/404/409 qua HTTP đã đăng nhập. Khi nghiệm thu dùng DB cô lập và tài khoản riêng; đăng nhập đồng thời ADMIN và user thử ở hai trình duyệt, khóa rồi thử request của user, mở khóa rồi thử lại cookie cũ và đăng nhập mới. Không khóa ba DEV. Không triển khai 2C.

## 2C · Quản lý vai trò — PARTIAL

Từ danh sách người dùng, chọn **Vai trò**. GET `/admin/users/roles?id=…` đọc danh mục và các liên kết thực tế qua một truy vấn. POST cùng đường dẫn nhận id, role, action (grant/revoke), csrf và xác nhận khi thu hồi ADMIN. Thành công dùng 303 về danh sách. Một tài khoản có nhiều vai trò theo khóa kép của user_roles.

Service lấy danh tính từ Filter, sau đó trong transaction ghi khóa hàng danh mục ADMIN theo thứ tự của 2B.3, kiểm tra lại ADMIN hoạt động và session trong DB rồi khóa user đích. Khóa kép và số dòng DELETE xác định cấp trùng/thu hồi thiếu. Cấm tự thu hồi ADMIN và thu hồi ADMIN hoạt động cuối. Mọi thay đổi ghi audit và thu hồi các phiên của tài khoản đích trong cùng transaction. Login cũng khóa hàng user nên phiên mở đồng thời được sắp thứ tự với thay đổi quyền. Cả cấp và thu hồi đều yêu cầu đăng nhập lại; Filter tiếp tục kiểm tra phiên và quyền từ DB mỗi request bảo vệ.

| Kiểm tra | Kết quả thực tế |
|---|---|
| Maven Wrapper clean package | PASS, javac release 21 và WAR. Các script nghiệm thu chạy riêng ngoài Surefire. |
| VerifyAuth, VerifyUserWrites, VerifyJdbcTransaction | PASS, giả lập xác thực, tạo/sửa hồ sơ, ràng buộc dữ liệu, commit/rollback. |
| VerifyUserDirectory | PASS, kiểm tra validation/escape và SELECT tìm kiếm, lọc, phân trang trên dev. |
| VerifyRoles, MySQL 9.6 loopback 13367 | PASS, fixture mới, đa vai trò, mã thiếu khỏi danh mục, cấp trùng, thu hồi thiếu, tự thu hồi, ADMIN cuối, khóa/mở khóa, audit, rollback khi lỗi tại bước session hoặc audit. Hai connection cấp cùng liên kết và thu hồi ADMIN chéo, khóa actor khi request đang chờ đều đạt. |
| Tomcat 10.1, HTTP thật cổng 18080 với cookie và MySQL cô lập | PASS, guest redirect, CSRF thiếu/sai, ADMIN xem/cấp/thu hồi, từ chối STUDENT và người có TEACHER+STUDENT, escape tên tài khoản, GET có tham số ghi vẫn chỉ đọc, 400/403/404/409, xác nhận ADMIN, redirect và thông báo cấp thành công, phiên cũ mất hiệu lực, login/logout. |
| Kiểm tra trực quan responsive và thao tác xác nhận trên trình duyệt desktop/mobile | NOT RUN. HTTP client đã gửi form và xác nhận nội dung JSP. Chưa nghiệm thu trực quan. |

File mới: scripts/VerifyRoles.java, src/main/webapp/WEB-INF/views/admin/user-roles.jsp. File sửa: RoleDao.java, UserDirectory.java (DTO), UserManagementService.java, UserManagementServlet.java, admin/users.jsp, scripts/smoke-test.ps1 và tài liệu này.

Chạy lại bằng scripts/smoke-test.ps1 -TomcatHome <Tomcat-10.1> -Port 18080 -RoleMysql khi DB_URL, DB_USER, DB_PASSWORD trỏ đến schema fixture mới trên 127.0.0.1:13367. Tên schema phải là role_test_ theo sau bởi 32 ký tự hex thường; chuẩn bị trước cấu trúc users, roles, user_roles, login_sessions, audit_logs giống V1 và các bảng rỗng. Script tạo dữ liệu thử trong schema này. Trong lần nghiệm thu chỉ đọc metadata và dữ liệu danh sách trên development. Giữ 2B.1/2B.2/2B.3 ở PARTIAL theo các tiêu chí HTTP riêng còn tồn tại.

## 2D · Quản lý môn học — PARTIAL

Roadmap xếp SubjectDao đầu nhóm học vụ của giai đoạn 2 (Admin và đào tạo). 2D là mã tiểu giai đoạn dùng sau 2C. Giai đoạn 3 dành cho ngân hàng câu hỏi.

- GET /{admin,teacher,student}/subjects hỗ trợ tìm mã/tên theo chuỗi literal, lọc ACTIVE/INACTIVE/ARCHIVED, sắp xếp code/name/updated, ASC/DESC và phân trang 1–100 dòng. GET .../subjects/detail?id=... áp dụng cùng phạm vi dữ liệu. Giáo viên đọc môn có offering mình phụ trách; sinh viên đọc môn ENROLLED hoặc COMPLETED, loại WITHDRAWN.
- ADMIN dùng GET/POST /admin/subjects/new và /admin/subjects/edit; GET edit nhận id. POST /admin/subjects/archive đổi trạng thái. Filter kiểm tra CSRF và phiên; Service xác minh ADMIN đang hoạt động và session trong transaction trước khi ghi. Client chỉ sửa mã, tên, mô tả. Mã trùng do UNIQUE MySQL quyết định theo collation thực tế. Cùng phiên bản updated_at, chỉ một cập nhật cạnh tranh thành công.
- Archive khóa hàng subject rồi kiểm tra course_offerings, topics, question_banks và exams. Chính sách của module là từ chối archive khi còn bất kỳ tham chiếu nào trong bốn bảng, kể cả lịch sử. Thao tác thành công ghi audit trong cùng transaction.
- File mới: SubjectDao.java, SubjectManagementService.java, SubjectManagementServlet.java, model/SubjectPage.java, WEB-INF/views/academic/{subjects,subject-detail,subject-form}.jsp, assets/css/subjects.css, scripts/VerifySubjects.java. File sửa: WEB-INF/views/account.jsp, scripts/smoke-test.ps1, docs/implementation-progress.md.
- PASS Maven Wrapper clean package, release 21. VerifyAuth, VerifyUserWrites, VerifyJdbcTransaction đạt trên giả lập; VerifyUserDirectory đạt với SELECT trên dev.
- PASS MySQL Server 9.6 tại loopback 13367, schema fixture mới dựng riêng bằng V1–V4. Hai POST sửa đồng thời trả 303/409, mã trùng trả 409, lỗi audit ép trên connection thật rollback INSERT. Đã kiểm tra archive thành công và chặn archive khi có offering. Chỉ đọc dữ liệu development.
- PASS HTTP Tomcat 10.1 trên 18082 (18080 đang dùng). VerifySubjects kiểm tra danh sách, tạo/sửa, chi tiết, mã/tên bắt buộc, độ dài mã, tìm kiếm literal, sắp xếp/phân trang/lọc, escape HTML, bỏ qua id/status gửi thêm khi tạo, CSRF, quyền ba vai trò, guest, 404 khi thiếu môn hoặc ngoài phạm vi, archive và audit. VerifyRoles chạy cùng fixture và đạt cho login/logout, khóa/mở, vai trò, session và cạnh tranh ADMIN. Maven không tự chạy các script này.
- Chạy lại bằng scripts/smoke-test.ps1 -TomcatHome <Tomcat-10.1> -Port 18082 -SubjectMysql với schema rỗng V1–V4, URL 127.0.0.1:13367/role_test_<32 hex thường> và credential test trong biến môi trường. VerifySubjects dùng bộ fixture VerifyRoles.
- Chưa nghiệm thu trực quan desktop/mobile. Cần kiểm tra các trang danh sách, form lỗi, chi tiết và xác nhận lưu trữ trên hai kích thước màn hình. Chưa có fixture riêng cho từng FK topics/question_banks/exams hoặc trạng thái đăng ký WITHDRAWN/COMPLETED. Giữ các trạng thái 2B và 2C theo biên bản trước.

## 2E · Quản lý lớp học phần — PARTIAL

Roadmap quy định thứ tự các dòng giai đoạn. OfferingDao cùng thuộc dòng 2 với SubjectDao và SchoolClassDao; 2E dùng các lớp hành chính có sẵn. V2 yêu cầu subject_id, class_id, teacher_id, academic_year và semester. Chỉ id là duy nhất. Hai lần tạo cùng các thuộc tính vẫn tạo hai lớp học phần riêng.

- GET /{admin,teacher,student}/offerings và .../offerings/detail?id=... đọc danh sách và chi tiết. Tìm mã/tên môn, mã/tên lớp và tên giáo viên; lọc subject/status, sắp xếp theo whitelist, 1–100 dòng/trang. Giáo viên xem theo teacher_id lấy từ danh tính đã xác thực. Sinh viên xem theo ghi danh ENROLLED hoặc COMPLETED; EXISTS tránh nhân bản hàng.
- ADMIN dùng GET/POST /admin/offerings/new, GET/POST /admin/offerings/edit (đổi giảng viên của lớp ACTIVE), POST /admin/offerings/close. Khi mở form edit truyền id trong query. Filter xử lý session/CSRF. Service khóa ADMIN và kiểm tra lại phiên trong transaction ghi. Khi tạo, khóa môn/lớp ACTIVE và xác minh giảng viên ACTIVE có TEACHER. Chính sách chỉnh sửa theo hợp đồng chỉ cho phân công lại; giữ môn, lớp, năm và kỳ của bản ghi đã tạo.
- Đóng lớp chuyển ACTIVE sang CLOSED, giữ các khóa ngoại, ghi số ghi danh và số phân công thi hiện hữu vào detail_json của audit. ARCHIVED là giá trị đọc/lọc. Khóa hàng offering và so phiên bản trước khi sửa; phiên bản dùng ID audit mới nhất của offering (0 cho dữ liệu cũ). Thay đổi và audit commit/rollback cùng nhau. Cơ chế này áp dụng cho các writer tuân thủ hợp đồng audit.
- Tạo OfferingDao.java, OfferingManagementService.java, OfferingManagementServlet.java, model/OfferingPage.java, WEB-INF/views/academic/offering-{list,form,detail}.jsp và scripts/VerifyOfferings.java. Sửa account.jsp, scripts/smoke-test.ps1, scripts/VerifySubjects.java và tài liệu tiến độ. VerifySubjects chuyển fixture/session đã xác minh sang VerifyOfferings khi bật tùy chọn riêng.
- PASS Maven Wrapper clean package (Java release 21). PASS giả lập VerifyAuth, VerifyUserWrites và VerifyJdbcTransaction; VerifyUserDirectory chỉ SELECT trên dev. PASS MySQL Server 9.6 riêng trên 127.0.0.1:13367 và HTTP Tomcat 10.1 trên 18082. Chạy lại VerifyRoles, VerifySubjects và VerifyOfferings. Bao gồm dữ liệu trùng theo V2, môn thiếu/archived, teacher không hợp lệ, validation, quyền theo bản ghi, guest, CSRF, escape HTML, tìm wildcard literal, sort ngoài whitelist, hai ADMIN cạnh tranh, cập nhật thiếu/cũ, rollback khi audit lỗi, đóng lớp có sinh viên và bảo toàn quyền đọc môn. Test xác minh audit đóng lớp với 1 ghi danh và 0 phân công thi.
- PASS bố cục Edge headless từ HTML trả về bởi Tomcat với CSS dự án, đã bỏ trường CSRF. Bốn trang list/detail/new/edit không tràn chiều ngang viewport 390 và 1280 px. Đã xem ảnh form mobile và danh sách desktop. Chưa thao tác tương tác trên thiết bị cảm ứng thật hoặc kiểm tra đóng lớp có phân công thi khác 0.
- Chạy lại với schema test rỗng theo điều kiện VerifyRoles, V1–V4, DB_URL/DB_USER/DB_PASSWORD riêng, rồi dùng scripts/smoke-test.ps1 -TomcatHome <Tomcat-10.1> -Port <cổng trống> -OfferingMysql sau khi build WAR. Giữ nguyên trạng thái nghiệm thu 2B–2D.

## 2F · Quản lý ghi danh sinh viên — PARTIAL

EnrollmentDao thuộc dòng 2 của roadmap, sau OfferingDao. Bảng course_enrollments có khóa chính (offering_id, student_id); V2 không đặt sĩ số tối đa. Hợp đồng cho ADMIN ghi danh/rút/hoàn thành, TEACHER đọc danh sách lớp phụ trách, STUDENT đọc lịch sử của mình. Lớp hành chính của sinh viên không phải điều kiện ghi danh trong hợp đồng.

- GET /{admin,teacher,student}/enrollments: tìm theo mã/tên/email, lọc offering và status, sắp xếp và phân trang. GET .../enrollments/detail?offering=…&student=… kiểm tra phạm vi ngay trong SQL. GET/POST /admin/enrollments/new, POST /admin/enrollments/withdraw và /admin/enrollments/complete. POST dùng CSRF và 303 sau khi thành công.
- Tạo ENROLLED cho tài khoản ACTIVE có STUDENT, lớp và môn ACTIVE. Chỉ chuyển ENROLLED sang WITHDRAWN hoặc COMPLETED. Khóa chính ngăn cấp lại cặp đã có kể cả sau khi rút. Điều kiện UPDATE theo trạng thái hiện tại chặn form cũ. STUDENT vẫn thấy lịch sử WITHDRAWN ở module này; 2D/2E chỉ cấp quyền đọc môn/lớp theo ENROLLED hoặc COMPLETED.
- Transaction kiểm tra ADMIN và phiên, khóa user sinh viên rồi khóa offering/môn. Khóa offering phối hợp với 2E; khóa user cùng quy ước bắt đầu lượt thi. Mỗi thay đổi có audit cùng transaction, lưu cả hai ID. Kiểm tra phân công lớp của đề PUBLISHED đang mở trước khi kết thúc ghi danh, xét ưu tiên phân công cá nhân. Với đề liên kết đã có lượt của sinh viên, Service chặn thay đổi vì exam_attempts không ghi offering nguồn. Đây là cách bảo toàn lịch sử khi có nhiều đường cấp quyền.
- File mới: EnrollmentDao.java, EnrollmentManagementService.java, EnrollmentServlet.java, model/EnrollmentPage.java, WEB-INF/views/academic/enrollment-{list,form,detail}.jsp, scripts/VerifyEnrollments.java. Sửa account.jsp, scripts/smoke-test.ps1, scripts/VerifySubjects.java và tài liệu này.

| Nhóm kiểm tra | Kết quả |
|---|---|
| Maven Wrapper clean package | PASS, javac release 21, WAR. Các script dưới scripts chạy riêng. |
| Giả lập và hồi quy | PASS VerifyAuth, VerifyUserWrites, VerifyJdbcTransaction. VerifyUserDirectory đạt với SELECT trên dev. |
| MySQL 9.6 cô lập 13367 và Tomcat 10.1 cổng 18082 | PASS VerifyRoles, VerifySubjects, VerifyOfferings và VerifyEnrollments với fixture V1–V5 và phiên HTTP thật. Bao gồm đăng ký trùng đồng thời 303/409, đóng lớp song song ghi danh, phát hiện ghi danh đã kết thúc, rollback khi audit lỗi, bảo vệ quyền thi đang mở và lượt thi, từ chối đối tượng ngoài phạm vi, CSRF, escape HTML, kiểm tra bộ lọc và thứ tự sắp xếp. |
| Tích hợp 2D/2E | PASS WITHDRAWN vẫn có lịch sử nhưng mất quyền đọc môn/lớp từ ghi danh này, COMPLETED giữ quyền đọc. Đóng lớp có một phân công thi và ghi danh thực tế giữ bản ghi, audit ghi chính xác số lượng cả hai. Các trạng thái nghiệm thu trước giữ nguyên. |
| Bố cục | PASS Edge headless trên HTML thực từ Tomcat, đã loại CSRF, dùng CSS dự án. List, form và detail vừa chiều rộng 390 và 1280 px. Đã xem ảnh form mobile và danh sách desktop. |
| Nghiệm thu còn lại | NOT RUN thao tác trực tiếp trên thiết bị cảm ứng và fixture kiểm chứng riêng phân công cá nhân chồng cửa sổ phân công lớp khi đổi ghi danh. |

Chạy lại với schema fixture rỗng V1–V5 trên loopback 13367, tên role_test_<32 ký tự hex thường> theo điều kiện VerifyRoles, khai báo DB_URL/DB_USER/DB_PASSWORD riêng. Sau khi build, dùng scripts/smoke-test.ps1 -TomcatHome <Tomcat-10.1> -Port <cổng trống> -EnrollmentMysql. Script chạy cả các suite 2C/2D/2E trước 2F. MySQL test tách riêng khỏi online_exam_dev.

## 2G · Lớp hành chính và thành viên lớp

Hai module còn thiếu ở dòng 2 của crud-roadmap là SchoolClassDao và ClassStudentDao. 2G là mã tiểu giai đoạn dùng cho gói này. Ngân hàng câu hỏi thuộc dòng 3. Trạng thái riêng của 2B–2F giữ theo các mục trước.

| Module | Trạng thái | Bằng chứng nghiệm thu |
|---|---|---|
| Quản lý lớp hành chính | COMPLETED | MySQL và HTTP thật: thêm, sửa, mã trùng, trường bắt buộc, tra cứu, lọc/tìm, phân trang, quyền theo vai trò, hai cập nhật cạnh tranh, rollback audit, archive giữ thành viên và lớp học phần đang tham chiếu. |
| Quản lý thành viên lớp | COMPLETED | MySQL và HTTP thật: thêm, rời, tái gia nhập, trùng khóa kép, hai request thêm đồng thời, thao tác sai, ID thiếu bản ghi, giảng viên đọc đúng lớp, sinh viên chỉ thấy bản thân, lịch sử vẫn truy cập được và audit giữ thời điểm rời trước khi tái gia nhập. |

**Sử dụng.** Từ trang tài khoản chọn Lớp hành chính. GET /{admin,teacher,student}/classes tra cứu theo mã/tên, trạng thái, thứ tự mã và phân trang. GET .../classes/detail?id=… xem chi tiết và danh sách thành viên, lọc theo mã/tên, trạng thái, sắp theo tên và phân trang. GET/POST /admin/classes/new, GET/POST /admin/classes/edit, POST /admin/classes/archive. POST /admin/classes/membership nhận join, leave hoặc rejoin. Khi thêm dùng ID tài khoản sinh viên. Filter bảo vệ route và CSRF; sau POST thành công trả 303.

**Quan hệ và quyền.** ADMIN quản lý lớp và quan hệ thành viên; khi thêm hoặc tái gia nhập, cả lớp và tài khoản STUDENT phải ACTIVE. TEACHER đọc lớp có course_offerings.teacher_id của mình. STUDENT đọc lớp từng có quan hệ class_students và chỉ thấy tư cách cá nhân. Thay đổi thành viên lớp hành chính không tự ghi danh lớp học phần. Archive cập nhật classes.status. Khóa chính (class_id,student_id) giữ một quan hệ xuyên các lần rời/tái gia nhập. LEFT có left_at; lần tái gia nhập mới cập nhật joined_at và xóa left_at, với khoảng thời gian cũ được giữ trong audit_logs.detail_json.

**Transaction.** Service xác minh ADMIN và phiên trong transaction, khóa hàng lớp và so updated_at từ form. Thao tác thành viên khóa thêm user và cập nhật phiên bản lớp. Điều kiện trạng thái trên UPDATE, khóa kép, UNIQUE(class_code) và audit cùng transaction bảo vệ cả trường hợp retry và ghi đồng thời.

**File.** SchoolClassDao.java và ClassStudentDao.java phụ trách hai bảng. ClassManagementService.java, SchoolClassServlet.java và ClassDirectory.java dùng chung cho hai module. View gồm academic/class-list.jsp, class-form.jsp, class-detail.jsp và class-{filters,paging,token}.jspf. Đã cập nhật account.jsp và assets/css/subjects.css. Kiểm thử thêm scripts/VerifyClasses.java và tích hợp với scripts/VerifySubjects.java, scripts/smoke-test.ps1.

**Kiểm thử thực tế.** PASS Maven Wrapper clean package, Java release 21. PASS VerifyAuth, VerifyUserWrites, VerifyJdbcTransaction, VerifyUserDirectory và các suite VerifyRoles, VerifySubjects, VerifyOfferings, VerifyEnrollments, VerifyClasses. Suite tích hợp dùng Tomcat 10.1 cổng 18082, MySQL 9.6 mới tại 127.0.0.1:13367. Schema test lấy bằng SHOW CREATE TABLE; chỉ đọc metadata và SELECT trên dev. Không chạy migration hoặc seed. Để chạy lại trên schema fixture rỗng, cấu hình credential test qua môi trường theo VerifyRoles rồi chạy scripts/smoke-test.ps1 -TomcatHome <Tomcat-10.1> -Port <cổng trống> -ClassMysql.

PASS Edge headless trên HTML do Tomcat trả về với CSS dự án, đã bỏ trường CSRF: danh sách, chi tiết/thành viên và form ở 390 và 1280 px. Đã sửa encoding UTF-8 trong fragment bộ lọc và giữ bảng thành viên cuộn ngang trên mobile. NOT RUN thao tác trên thiết bị cảm ứng thật. Các bài thử câu hỏi/đáp án chưa áp dụng trong gói học vụ này.

## Giai đoạn 3 · Ngân hàng câu hỏi

Roadmap giao giai đoạn 3 cho Trung. 3A–3C là mã bàn giao cho ba module trong giai đoạn này.

| Module | Trạng thái | Phạm vi nghiệm thu |
|---|---|---|
| 3A · Chủ đề | COMPLETED | ADMIN quản lý cây; TEACHER tạo/sửa trong môn phụ trách. Khóa cây theo môn, kiểm tra cha cùng môn, chu trình và mã duy nhất trong môn. Giữ nguyên môn khi sửa. Chặn sửa chủ đề có câu đã đưa vào đề. V3 không có cột trạng thái để lưu trữ chủ đề. |
| 3B · Ngân hàng | PARTIAL | Giáo viên tạo/sửa ngân hàng sở hữu, đặt PRIVATE/SHARED; ADMIN tra cứu metadata. Số câu đếm theo bank_id. Tên ngân hàng có thể trùng theo V3. Chặn sửa ngân hàng đã có câu trong đề. Phạm vi đọc SHARED của giáo viên khác chờ quyết định trong roadmap. |
| 3C · Câu hỏi và đáp án | PARTIAL | Đã nghiệm thu tạo/sửa nháp trong ngân hàng sở hữu, lưu version và options cùng transaction, công bố và lưu trữ câu hỏi. Còn quyền đọc nội dung SHARED ngoài sở hữu và ca có tệp đính kèm thực tế. |

**Sử dụng.** Từ tài khoản ADMIN hoặc TEACHER chọn Chủ đề, Ngân hàng câu hỏi hoặc Câu hỏi. GET /{admin,teacher}/{topics,banks,questions} mở danh sách; GET /detail?id=… dưới mỗi đường dẫn mở chi tiết. GET/POST /new và /edit dùng cho vai trò có quyền ghi. POST /teacher/questions/publish và /teacher/questions/archive đổi trạng thái. Danh sách câu hỏi có bộ lọc môn, ngân hàng, chủ đề, loại, độ khó và trạng thái. ADMIN chỉ tra cứu metadata của câu hỏi. JSP hiển thị văn bản đã escape; STUDENT không có đường dẫn soạn thảo hoặc xem đáp án.

**Dữ liệu và lịch sử.** Chủ đề tùy chọn của câu phải cùng môn với ngân hàng. Chủ sở hữu và tác giả lấy từ phiên. Mỗi câu có ít nhất hai phương án khác nhau, không rỗng. SINGLE_CHOICE cần đúng một đáp án đúng; MULTIPLE_CHOICE cần ít nhất một. Sửa nháp tạo version_no mới dưới khóa question. Thay đổi question, version, options và audit dùng chung một connection. Đối chiếu dấu phiên bản từ audit để phát hiện form cũ. Khóa phiên và kiểm tra vai trò trong Service trước khi đọc/ghi. Câu đã công bố hoặc có version được đề tham chiếu bị chặn sửa. Sửa nháp có attachment trên phiên bản hiện hành bị chặn để bảo toàn liên kết tệp.

**File triển khai.** DAO riêng gồm TopicDao.java, QuestionBankDao.java, QuestionDao.java. Ba module dùng chung QuestionCatalogDao.java, model/QuestionCatalog.java, QuestionBankService.java và QuestionCatalogServlet.java. JSP nằm trong WEB-INF/views/question/{list.jsp,detail.jsp,form.jsp,common.jspf}. Đã thêm scripts/VerifyQuestionCatalog.java và cập nhật account.jsp, scripts/VerifySubjects.java, scripts/smoke-test.ps1 cùng tài liệu tiến độ.

**Kiểm thử thực tế.** Maven Wrapper clean package dùng javac release 21. Các suite nghiệm thu chạy độc lập với Surefire.

- PASS MySQL 9.6 cô lập (13367) và Tomcat 10.1 (18082), phiên ADMIN/TEACHER/STUDENT thật. VerifyQuestionCatalog kiểm tra phân trang/lọc, quan hệ cha/con, chủ đề sai môn, mã trùng, cạnh tranh cập nhật, quyền sở hữu, thiếu/sai CSRF, từ chối phiên cũ và bảo mật đáp án. Lỗi audit được chèn ở cả ba module; lỗi ghi phương án khi tạo và khi sửa đều rollback. Kiểm tra sau rollback gồm nội dung, lựa chọn, version và dấu phiên bản. Fixture exam_questions xác minh chặn sửa câu, ngân hàng và chủ đề đã dùng.
- PASS hồi quy VerifyAuth, VerifyUserWrites, VerifyJdbcTransaction, VerifyUserDirectory, VerifyRoles, VerifySubjects, VerifyOfferings, VerifyEnrollments và VerifyClasses. Tất cả kiểm thử ghi dùng schema thử riêng.
- PASS Edge headless trên HTML Tomcat đã loại trường CSRF: danh sách, chi tiết, form của cả ba module tại 390 và 1280 px (18 bố cục); kiểm tra tiếng Việt dài, bốn lựa chọn và thêm/bỏ phương án. NOT RUN thiết bị cảm ứng vật lý và fixture draft có attachment thực.
- Để chạy lại: dùng schema test rỗng V1–V5 và biến môi trường riêng theo điều kiện VerifyRoles (loopback 13367, role_test_<32 ký tự hex thường>), build WAR rồi chạy scripts/smoke-test.ps1 -TomcatHome <Tomcat-10.1> -Port <cổng trống> -CatalogMysql. Các trạng thái 2B–2G giữ theo biên bản trước.

## Giai đoạn 4 · Đề và phân công

Roadmap gộp quản lý đề, câu thuộc đề, phân công và mẫu chính sách trong một dòng. Trong gói này 4A, 4B, 4C là ba phần nghiệm thu. Chức năng mẫu chính sách chỉ gồm chọn mẫu hiện có và chụp cấu hình khi công bố. Việc quản lý danh mục mẫu thuộc phạm vi riêng.

| Phần | Cách sử dụng | Quy tắc |
|---|---|---|
| 4A · Cấu hình đề | TEACHER mở /teacher/exams để tìm kiếm, lọc trạng thái, đổi thứ tự, phân trang và tạo đề. Chọn tên đề để xem hoặc sửa. ADMIN tra cứu ở /admin/exams. | Chỉ chủ đề thi có quyền ghi. Lưu thời lượng, điểm đạt, giới hạn lượt, hướng dẫn, trộn câu/phương án, quy tắc lấy điểm giữa các lượt và chế độ công bố. Publish chụp cấu hình và các trường của mẫu giám sát. |
| 4B · Biên soạn | Trong chi tiết đề nháp, giáo viên lọc theo ngân hàng, chủ đề, độ khó, thêm/loại phiên bản, chỉnh thứ tự và điểm. Trang chi tiết hiển thị số câu và tổng điểm. | Kiểm tra phiên bản mới nhất, câu PUBLISHED, đúng môn, cả ngân hàng và câu thuộc giáo viên, phương án và số đáp án đúng hợp lệ. Phiên bản có tệp đính kèm bị loại khỏi danh sách và bị từ chối tại Service. Khi đã dùng trong đề, cơ chế giai đoạn 3 bảo vệ phiên bản. |
| 4C · Phân công và lịch | Trên đề PUBLISHED, giáo viên chọn lớp học phần hoặc cá nhân, nhập hai mốc UTC và số lượt ghi đè tùy chọn. Sinh viên xem /student/schedule. | Lớp phải cùng môn, đang mở, do giáo viên phụ trách và có ghi danh hợp lệ. Chọn cá nhân từ sinh viên đang học trong một lớp hợp lệ. Lịch cá nhân đã cấp được lưu độc lập. Chỉ sửa/thu hồi trước giờ mở và khi đề chưa có lượt thi. |

V4 dùng exams.max_attempts và max_attempts_override trên từng loại phân công. Giá trị hợp lệ 1–32767, override trống lưu SQL NULL. Lịch liệt kê rõ từng cửa sổ và giá trị cấu hình, không cộng hai giới hạn. Lượt thực tế sẽ được kiểm soát ở giai đoạn 5. CRUD contract mô tả ưu tiên override cá nhân, lớp hợp lệ có ID nhỏ nhất, rồi mặc định đề; roadmap vẫn yêu cầu chốt trường hợp nhiều lớp và lượt hủy. Phạm vi chia sẻ ngân hàng và công thức PARTIAL_CREDIT vẫn mở.

Ghi dữ liệu dùng POST/CSRF/PRG, xác minh phiên và vai trò trong transaction. Các thao tác trên cùng đề khóa hàng exams trước khi đọc dấu phiên bản từ audit_logs. Dấu phiên bản áp dụng chung cho cấu hình, câu và lịch. Xung đột trả HTTP 409. Audit và thay đổi dữ liệu commit cùng nhau. Trạng thái lần lượt DRAFT → PUBLISHED → CLOSED → ARCHIVED. Chặn đóng khi đang có lượt hoặc cửa sổ đang mở. ADMIN đọc metadata và lịch. Truy vấn lịch STUDENT chỉ lấy thông tin phân công, tiêu đề và giới hạn.

**File triển khai** gồm dao/ExamDao.java, dao/ExamQuestionDao.java, dao/ExamAssignmentDao.java, service/ExamService.java (gồm Config và Detail), controller/ExamServlet.java và WEB-INF/views/exam/{list.jsp,detail.jsp,schedule.jsp,header.jspf,keys.jspf,config.jspf,calendar.jspf}. account.jsp thêm liên kết theo vai trò. scripts/VerifyExams.java dùng chung fixture và session với VerifySubjects.java. scripts/smoke-test.ps1 thêm tùy chọn -ExamMysql.

Để nghiệm thu lại trên một schema fixture rỗng V1–V7 riêng, đặt DB_URL trỏ 127.0.0.1:13367/role_test_<32 hex>, khai báo credential thử và JAVA_HOME trong tiến trình, chạy Maven Wrapper clean package rồi chạy scripts/smoke-test.ps1 -TomcatHome <đường dẫn Tomcat 10.1> -Port <cổng trống> -ExamMysql. Suite tự kiểm tra tên schema, cổng và trạng thái fixture trước khi ghi.


### Nghiệm thu 21/11/2026

| Phần | Trạng thái | Bằng chứng và giới hạn |
|---|---|---|
| 4A · Quản lý đề | COMPLETED trong phạm vi quản lý đề | Đã tạo/sửa, kiểm tra thời lượng, giới hạn lượt 0 và vượt SMALLINT, lưu cấu hình và snapshot mẫu, từ chối công bố đề rỗng, chạy toàn bộ vòng đời trạng thái và bảo vệ cửa sổ đang mở/lượt đã bắt đầu. |
| 4B · Biên soạn | PARTIAL | Đã nghiệm thu câu thuộc chủ sở hữu, ghim phiên bản, đề sai môn, phiên bản thiếu, câu nháp/trùng, thêm/loại, tính tổng và giao dịch. Fixture có bản ghi attachment bị từ chối. Chia sẻ giữa giáo viên còn mở; tệp vật lý chưa được nghiệm thu. |
| 4C · Phân công và lịch | PARTIAL | Đã nghiệm thu lịch lớp/cá nhân, giới hạn ghi đè, khoảng thời gian sai, lớp thiếu, cá nhân không hợp lệ, phân công trùng, thu hồi và bảo vệ lịch sử. Hai lịch chồng nhau hiển thị được đồng thời. Chính sách áp dụng khi nhiều lớp cùng hợp lệ và cách tính lượt hủy còn cần chốt trước giai đoạn 5. |

- PASS Maven Wrapper clean package, javac release 21, WAR có đủ servlet và JSP giai đoạn 4. Surefire báo No tests to run. Các kết quả nghiệp vụ bên dưới đến từ script riêng.
- PASS MySQL 9.6 scratch tại 127.0.0.1:13367 và Tomcat 10.1 tại 127.0.0.1:18084. Các test ghi dùng schema riêng. Đã thử phiên ADMIN, hai phiên độc lập của giáo viên sở hữu, giáo viên khác, STUDENT và guest. Các thao tác đồng thời sửa cấu hình, thêm cùng câu và phân công cùng lớp đều cho một HTTP 303 và một HTTP 409. Lỗi audit cưỡng bức sau ghi của cả ba module trả 503 và rollback dữ liệu.
- PASS ca 2F: khi cửa sổ lớp đang mở và chưa có lịch cá nhân, rút ghi danh trả 409. Sau khi cấp lịch cá nhân trùng cửa sổ, rút ghi danh trả 303; lịch cá nhân vẫn xuất hiện. Sinh viên không nhận nội dung câu hỏi khi xem lịch. Không cấp hoặc cộng lượt trong kiểm thử này.
- PASS hồi quy VerifyAuth, VerifyUserWrites, VerifyJdbcTransaction, VerifyUserDirectory, VerifyRoles, VerifySubjects, VerifyOfferings, VerifyEnrollments, VerifyClasses, VerifyQuestionCatalog. VerifyUserDirectory đọc dữ liệu dev bằng SELECT; các bài thử ghi dùng fixture cô lập hoặc JDBC giả lập.
- PASS scripts/verify-exam-layout.ps1 trên Edge headless: danh sách, đề nháp, phân công, lịch sinh viên ở 390 và 1280 px (8/8 không tràn ngang). HTML dùng trong kiểm tra lấy từ Tomcat và bỏ giá trị CSRF trước khi lưu vào target. Lần chạy đầu phát hiện JSP fragment sai encoding; sau khi khai báo UTF-8 từng file, ca HTTP tiếng Việt và bộ hồi quy chạy thành công.
- NOT RUN thao tác cảm ứng trên thiết bị vật lý, phân phối tệp câu hỏi thật, kiểm tra tải lớn hoặc quá trình bắt đầu/chấm lượt thi. Trạng thái các giai đoạn 1–3 giữ theo biên bản trước.
