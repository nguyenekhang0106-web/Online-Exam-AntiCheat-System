# Online-Exam-AntiCheat-System
Hệ thống thi trắc nghiệm trực tuyến tích hợp giám sát gian lận và bảo mật, phát triển bằng Java JSP/Servlet theo mô hình MVC.

## Phạm vi nền tảng

Java release 21, Maven WAR, Jakarta Servlet 6.0, JSP, JDBC, MySQL và Tomcat 10.1.
Hiện có trang kiểm tra MVC, migration cho 34 bảng và 34 Java Bean; chưa triển khai đăng nhập, đề thi, giám sát hay kết nối DB.
Không sử dụng Spring. Mở chính thư mục repository bằng VS Code, không tạo project con.

## Môi trường đã kiểm tra (19/11/2026)

| Thành phần | Kết quả trên máy hiện tại |
| --- | --- |
| JDK | Oracle JDK 25.0.2, `C:\Program Files\Java\jdk-25.0.2` |
| JAVA_HOME | Chưa được đặt trong môi trường ban đầu |
| Git | 2.53.0.windows.2 |
| Maven | IntelliJ tích hợp 3.9.11; `mvn` chưa có trong PATH |
| MySQL | Server/client 9.6.0, service `MySQL96` đang chạy; client chưa có trong PATH |
| Tomcat | 10.1.61, service `Tomcat10` đang dừng; thư mục Program Files cần quyền truy cập phù hợp |
| VS Code | Đã có Java Extension Pack, Red Hat Java, Debugger, Test Runner, Maven và Project Manager |

Không cần cài thêm để build. JDK 25 có thể biên dịch `--release 21`;
WAR yêu cầu Java 21 trở lên khi chạy, dù riêng Tomcat 10.1 có thể chạy từ Java 11.
Nhóm có thể thống nhất dùng JDK 21 LTS để đồng nhất build/runtime. Wrapper cố định
Maven 3.9.11 (cùng phiên bản Maven IntelliJ đã kiểm tra), không cố định JDK.

## Cấu hình Windows

PowerShell, áp dụng cho terminal hiện tại (không đổi cấu hình hệ thống):

```powershell
cd D:\Online-Exam-AntiCheat-System
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.2'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
javac -version
.\mvnw.cmd -version
.\mvnw.cmd clean verify
```

CMD tương đương:

```bat
set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.2"
set "PATH=%JAVA_HOME%\bin;%PATH%"
mvnw.cmd clean verify
```

Lần đầu Wrapper tải Maven; Maven tải dependency từ Maven Central nên cần Internet.
Mặc định cache ở `%USERPROFILE%\.m2`. Nếu muốn cache chỉ trong repository:

```powershell
$env:MAVEN_USER_HOME = "$PWD\.cache\maven-user"
.\mvnw.cmd -B -ntp '-Dmaven.repo.local=.cache/m2' clean verify
```

Để đặt lâu dài: mở **Edit environment variables for your account**, thêm `JAVA_HOME`
trỏ tới thư mục JDK (không phải `bin`), thêm `%JAVA_HOME%\bin` vào User PATH,
rồi mở lại VS Code/terminal. Không ghi đè toàn bộ PATH.

Máy mới của thành viên, chỉ cài khi chủ máy đồng ý:

1. Cài JDK 21 từ [Adoptium](https://adoptium.net/temurin/releases/?version=21), đặt JAVA_HOME như trên.
2. Cài [Git for Windows](https://git-scm.com/downloads/win) và [VS Code](https://code.visualstudio.com/).
   Cài Extension Pack for Java (`vscjava.vscode-java-pack`); repository có recommendation.
3. Không bắt buộc cài Maven riêng. Nếu muốn dùng `mvn`, tải binary ZIP từ
   [Apache Maven](https://maven.apache.org/download.cgi), giải nén chẳng hạn `C:\Tools\apache-maven-3.9.11`,
   thêm thư mục `bin` tương ứng vào User PATH và kiểm tra `mvn -version`.
   Build nhóm vẫn dùng Wrapper để tránh khác phiên bản.
4. Tải [Tomcat 10.1](https://tomcat.apache.org/download-10.cgi) bản ZIP, giải nén vào thư mục có quyền ghi
   như `C:\Tools\apache-tomcat-10.1.x`. Đặt `CATALINA_HOME` tới thư mục đó nếu muốn dùng các lệnh bên dưới.
   Không dùng Tomcat 9 (`javax.servlet`) cho source `jakarta.servlet` này.
5. Cài [MySQL Community Server](https://dev.mysql.com/downloads/mysql/), nhóm nên thống nhất phiên bản
   (có thể chọn 8.4 LTS trên máy mới), cấu hình Windows service và mật khẩu riêng qua trình cài.
   Thêm thư mục MySQL `bin` vào User PATH nếu cần `mysql` trong terminal.
   Workbench là tùy chọn. Kiểm tra `mysql --version`, `Get-Service *mysql*` và
   `mysql -u <ten-tai-khoan> -p` (nhập mật khẩu tại prompt, không đưa vào câu lệnh).

Trong VS Code, mở thư mục gốc, chờ Maven import. Nếu JDK chưa được nhận diện, dùng
**Java: Configure Java Runtime** để chọn JDK; cấu hình đường dẫn máy cá nhân ở User Settings.
Live Server không chạy được JSP/Servlet; phải dùng Tomcat.

## Chạy WAR trên Tomcat

Build tạo `target/online-exam.war`. Với Tomcat ZIP thuộc quyền sở hữu của bạn:

```powershell
$env:CATALINA_HOME = 'C:\Tools\apache-tomcat-10.1.x' # thay bằng đường dẫn thật
Copy-Item .\target\online-exam.war "$env:CATALINA_HOME\webapps\online-exam.war"
& "$env:CATALINA_HOME\bin\catalina.bat" run
```

Mở `http://localhost:8080/online-exam/` hoặc `/online-exam/home`.
Dừng bằng Ctrl+C. Trước khi thay WAR đang chạy, dừng Tomcat. Nếu port 8080 bận,
chọn port HTTP khác trong `conf/server.xml`. Không khởi động đồng thời service và bản chạy tay trên cùng port.
Với bản cài Windows service trong Program Files, cần quyền phù hợp khi triển khai;
Java của service được cấu hình riêng, không tự đổi theo JAVA_HOME của terminal.

Có thể kiểm thử WAR mà không sửa bản Tomcat hệ thống:

```powershell
.\scripts\smoke-test.ps1 -TomcatHome 'C:\Program Files\Apache Software Foundation\Tomcat 10.1'
```

Script dùng thư viện Tomcat có sẵn, tạo CATALINA_BASE riêng tại `target/tomcat-smoke`,
chỉ bind `127.0.0.1:18080`, kiểm tra HTTP rồi dừng đúng tiến trình đã tạo.
Cần quyền đọc Tomcat; có thể chỉ định bản ZIP ở thư mục bạn sở hữu nếu Program Files từ chối truy cập.
Log tại `target/tomcat-smoke/logs`. Có thể đổi port bằng `-Port 18081`.
`clean` sẽ xóa target, vì vậy chỉ chạy clean sau khi server thử nghiệm đã dừng.

## Cấu trúc và giải thích thiết kế

```text
pom.xml                       # WAR, Java release 21, dependency/plugin versions
mvnw / mvnw.cmd / .mvn/        # Maven Wrapper cho cả nhóm
src/main/java/vn/edu/onlineexam/
  controller/HomeServlet.java # nhận GET /home, truyền dữ liệu sang view
  service/                    # nghiệp vụ và ranh giới transaction
  dao/                        # JDBC, PreparedStatement, đóng tài nguyên
  model/                      # đối tượng dữ liệu/domain
  filter/                     # xác thực/phân quyền và xử lý HTTP dùng chung
  util/                       # tiện ích hạ tầng
src/main/resources/           # cấu hình công khai, không chứa secret
src/main/webapp/
  WEB-INF/web.xml             # Servlet 6.0, UTF-8, welcome route
  WEB-INF/views/home.jsp      # view, không truy cập trực tiếp từ trình duyệt
  assets/css/app.css          # tài nguyên tĩnh
scripts/smoke-test.ps1         # kiểm tra HTTP trên Tomcat có sẵn
```

Luồng mở rộng: Browser → Filter → Controller → Service → DAO → MySQL;
Controller đặt request attribute rồi forward tới JSP. Không đặt SQL trong Servlet/JSP,
không để DAO phụ thuộc HTTP, không giữ dữ liệu từng request trong field của Servlet.
Các package trống dùng `package-info.java` để Git giữ cấu trúc và mô tả trách nhiệm,
chưa tạo lớp nghiệp vụ giả.

`jakarta.servlet-api:6.0.0` có scope `provided`: Tomcat cung cấp lúc chạy, tránh đóng gói API trùng vào WAR.
`mysql-connector-j:9.6.0` có scope `runtime`: source DAO sau này dùng API `java.sql` có sẵn trong JDK.
Chưa thêm JSTL, thư viện authentication hoặc framework khi chưa có nhu cầu.
JSP hiện chỉ hiển thị chuỗi cố định từ server; khi hiển thị dữ liệu người dùng phải escape HTML
(ví dụ JSTL `c:out` sau khi thêm JSTL), vì EL không tự escape.

## Database và secret

Trang kiểm tra không cần database. Đã viết SQL migration và Bean nhưng chưa thực thi tạo schema/user hoặc kiểm thử đăng nhập MySQL.
Giai đoạn sau thống nhất `DB_URL`, `DB_USER`, `DB_PASSWORD` qua biến môi trường hoặc
JNDI DataSource của Tomcat. Hiện chưa có code đọc các biến này; file `.env` không tự được Java nạp.
Để file cấu hình cá nhân ngoài source, ví dụ `config/local/` đã ignore.
Không đặt mật khẩu vào README, JDBC URL, pom.xml, JSP hay src/main/resources.
Gitignore không ngăn Maven đóng gói file nhạy cảm nằm trong source và không bỏ theo dõi file đã tracked.

Thiết kế 34 bảng, hướng dẫn migration/seed và các quy tắc transaction/locking:
[docs/database-design.md](docs/database-design.md).
ERD: [database/erd/online_exam_erd.md](database/erd/online_exam_erd.md).
Migration nằm trong `database/migrations/V1__...` đến `V7__...`, không tự chạy khi build/deploy.
Chỉ chạy SQL trên database thử nghiệm trống sau khi được cho phép.
Helper `scripts/SeedAccounts.java` sinh SQL tài khoản có hash riêng trong `.local/`, không kết nối DB;
`PasswordHasher` cung cấp encoder/verifier dùng chung cho Auth Service sau này.

Kiểm tra cấu trúc và Bean sau build (Node.js chỉ cần cho checker, không cần cho ứng dụng Java):

```powershell
node scripts/verify-database.cjs
java -cp target/classes scripts/VerifyBeans.java
```

## Phân công và quy ước tích hợp

| Thành viên | Phụ trách |
| --- | --- |
| Khang | Database, Authentication, Admin |
| Trung | Câu hỏi, đề thi, làm bài |
| Cường | Giám sát gian lận, bảo mật, chấm điểm, báo cáo |

Mỗi chức năng đi qua các lớp tương ứng, không chia thành ba project độc lập.
Trước khi triển khai cần thống nhất schema, khóa/ID, model User/Exam/Attempt, hợp đồng Service,
session/role và quy tắc transaction. Dùng nhánh chức năng, review thay đổi pom/schema chung,
build bằng Wrapper trước khi tích hợp. Chưa triển khai nghiệp vụ trong giai đoạn này.

## Kiểm thử

`mvnw.cmd clean verify` kiểm tra biên dịch/đóng gói, chưa có unit test nghiệp vụ.
JSP chỉ được Tomcat biên dịch lúc chạy, nên build thành công chưa đủ chứng minh view hoạt động.
Script smoke kiểm tra `/home`, welcome route, CSS và HTTP 404 khi truy cập trực tiếp JSP trong WEB-INF.
Kết quả thực tế được ghi tại [docs/verification.md](docs/verification.md).

Tài liệu tham khảo: [Tomcat và Servlet versions](https://tomcat.apache.org/whichversion.html),
[Maven Wrapper](https://maven.apache.org/wrapper/),
[Java trên VS Code](https://code.visualstudio.com/docs/java/java-tutorial),
[MySQL Connector/J](https://dev.mysql.com/doc/connector-j/en/).
