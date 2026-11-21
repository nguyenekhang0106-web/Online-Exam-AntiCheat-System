<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Tài khoản | Online Exam</title>
<link rel="stylesheet" href="../assets/css/app.css"></head>
<body><main><h1>Tài khoản</h1>
<p>Bạn đang đăng nhập với vai trò ${roleLabel}.</p>
<p><a href="subjects">Môn học</a></p>
<p><a href="classes">Lớp hành chính</a></p>
<p><a href="offerings">Lớp học phần</a></p>
<p><a href="enrollments">Ghi danh sinh viên</a></p>
<% if ("TEACHER".equals(request.getAttribute("roleLabel")) || "ADMIN".equals(request.getAttribute("roleLabel"))) { %>
<nav aria-label="Ngân hàng câu hỏi"><a href="topics">Chủ đề</a> · <a href="banks">Ngân hàng câu hỏi</a> · <a href="questions">Câu hỏi</a></nav>
<% } %>
<% if ("ADMIN".equals(request.getAttribute("roleLabel"))) { %><p><a href="users">Quản lý người dùng</a></p><% } %>
<% if ("STUDENT".equals(request.getAttribute("roleLabel"))) { %><p><a href="schedule">Lịch thi của tôi</a></p><% } else { %><p><a href="exams">Đề và phân công thi</a></p><% } %>
<p><a href="attempts">Lượt thi và kết quả</a></p><% if (!"STUDENT".equals(request.getAttribute("roleLabel"))) { %><p><a href="proctoring/events">Lịch sử giám sát</a></p><% } %><form method="post" action="../logout">
    <input type="hidden" name="csrf" value="${sessionScope.csrf}">
    <button>Đăng xuất</button>
</form></main></body></html>
