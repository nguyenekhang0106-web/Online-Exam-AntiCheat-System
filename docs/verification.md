# Kết quả kiểm thử nền tảng

Ngày 19/11/2026, Windows 11, JDK 25.0.2, Maven Wrapper 3.3.4 / Maven 3.9.11.

## Build thực tế

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.2'
$env:MAVEN_USER_HOME = "$PWD\.cache\maven-user"
.\mvnw.cmd -B -ntp '-Dmaven.repo.local=.cache/m2' clean verify
```

- BUILD SUCCESS, tạo `target/online-exam.war`.
- Compiler sử dụng `release 21`; javap xác nhận HomeServlet có major version 65.
- WAR có HomeServlet, JSP, web.xml, CSS và MySQL Connector/J 9.6.0 (kèm protobuf dependency bắc cầu).
- WAR không chứa jakarta.servlet-api: API do Tomcat cung cấp.
- Chưa có unit test nghiệp vụ; Maven báo `No tests to run`.
- JDK 25 phát cảnh báo deprecated Unsafe từ Guice của Maven; không làm build thất bại.

## Runtime thực tế

```powershell
.\scripts\smoke-test.ps1 -TomcatHome 'C:\Program Files\Apache Software Foundation\Tomcat 10.1'
```

Tomcat 10.1.61, CATALINA_BASE riêng trong target, bind 127.0.0.1:18080:

| Kiểm tra | Kết quả |
| --- | --- |
| GET /online-exam/home | 200; JSP biên dịch, request attribute hiển thị, tiếng Việt đúng |
| GET /online-exam/ | 200; welcome mapping hoạt động |
| GET /online-exam/assets/css/app.css | 200; nội dung CSS đúng |
| GET /online-exam/WEB-INF/views/home.jsp | 404; view không được truy cập trực tiếp |

Script dừng tiến trình đã tạo trong finally. Không khởi động hoặc sửa Windows service Tomcat10.
Log nằm trong `target/tomcat-smoke/logs` và được Git ignore.
Script dùng UTF-8 BOM để tương thích Windows PowerShell 5.1.

## Giới hạn và quyền thực thi

Lần chạy Maven trong sandbox bị chặn network; chạy lại với quyền được cấp đã tải và build thành công.
Đọc/chạy Tomcat trong Program Files cũng cần quyền thực thi phù hợp.
Không cài phần mềm hệ thống, không sửa JAVA_HOME/PATH lâu dài.
MySQL client 9.6.0 và service MySQL96 đang chạy được xác nhận, nhưng chưa xác thực DB,
chưa tạo schema và chưa kiểm thử JDBC vì giai đoạn này không có tài khoản hay nghiệp vụ DB.
Đã kiểm tra ignore cho target, cache, .env, cấu hình local và VS Code settings cá nhân.
Không commit, push, thay remote hoặc thay Git index; giữ nguyên thay đổi .gitignore có trước.
