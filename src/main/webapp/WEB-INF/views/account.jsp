<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
  request.setAttribute("pageCrumb", "Bảng điều khiển");
  String role = String.valueOf(request.getAttribute("roleLabel"));
  boolean isAdmin = "ADMIN".equals(role);
  boolean isTeacher = "TEACHER".equals(role);
  boolean isStudent = "STUDENT".equals(role);
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Tài khoản | Online Exam</title>
  <link rel="stylesheet" href="../assets/css/app.css">
</head>
<body>
<%@ include file="layout/app-shell-start.jspf" %>

<div class="page-header">
  <div class="page-header-info">
    <h1>Bảng điều khiển</h1>
    <p class="page-subtitle">Xin chào! Bạn đang đăng nhập với vai trò <span class="<%=uiBadge(role)%>"><%=uiRoleVi%></span>.</p>
  </div>
</div>

<div class="board-columns">
  <% if (isStudent) { %>
  <section class="board-col">
    <div class="board-col-head"><h2 class="board-col-title">Kỳ thi của tôi</h2><span class="badge badge-pink">Sinh viên</span></div>
    <a class="task-card" href="schedule">
      <div class="task-card-tags"><span class="badge badge-green">Sẵn sàng</span><span class="badge badge-purple">Lịch thi</span></div>
      <div class="task-card-title">Lịch thi của tôi</div>
      <p class="task-card-desc">Xem danh sách bài thi đã được phân công, thời gian mở/đóng cửa sổ và kiểm tra quyền vào thi.</p>
      <div class="task-card-foot"><span>Mở lịch thi</span><span>→</span></div>
    </a>
    <a class="task-card" href="attempts">
      <div class="task-card-tags"><span class="badge badge-purple">Kết quả</span><span class="badge badge-pink">Lịch sử</span></div>
      <div class="task-card-title">Lượt thi và kết quả</div>
      <p class="task-card-desc">Tiếp tục lượt thi đang mở hoặc xem điểm số, phần trăm đạt của các lượt đã nộp.</p>
      <div class="task-card-foot"><span>Xem lượt thi</span><span>→</span></div>
    </a>
  </section>
  <% } %>

  <% if (isTeacher || isAdmin) { %>
  <section class="board-col">
    <div class="board-col-head"><h2 class="board-col-title">Ngân hàng câu hỏi</h2><span class="badge badge-purple">Nội dung</span></div>
    <a class="task-card" href="topics">
      <div class="task-card-tags"><span class="badge badge-green">Cấu trúc</span><span class="badge badge-purple">Chủ đề</span></div>
      <div class="task-card-title">Chủ đề môn học</div>
      <p class="task-card-desc">Tổ chức cây chủ đề theo từng môn học để phân loại câu hỏi rõ ràng.</p>
      <div class="task-card-foot"><span>Quản lý chủ đề</span><span>→</span></div>
    </a>
    <a class="task-card" href="banks">
      <div class="task-card-tags"><span class="badge badge-purple">Kho đề</span><span class="badge badge-pink">Phân quyền</span></div>
      <div class="task-card-title">Ngân hàng câu hỏi</div>
      <p class="task-card-desc">Tạo và quản lý các bộ ngân hàng câu hỏi riêng tư hoặc chia sẻ theo môn học.</p>
      <div class="task-card-foot"><span>Mở ngân hàng</span><span>→</span></div>
    </a>
    <a class="task-card" href="questions">
      <div class="task-card-tags"><span class="badge badge-green">Phiên bản</span><span class="badge badge-pink">Trắc nghiệm</span></div>
      <div class="task-card-title">Câu hỏi &amp; Đáp án</div>
      <p class="task-card-desc">Soạn thảo câu hỏi một/nhiều đáp án, công bố bản chính thức và lưu trữ lịch sử phiên bản.</p>
      <div class="task-card-foot"><span>Quản lý câu hỏi</span><span>→</span></div>
    </a>
  </section>

  <section class="board-col">
    <div class="board-col-head"><h2 class="board-col-title">Tổ chức thi &amp; Kết quả</h2><span class="badge badge-green">Kỳ thi</span></div>
    <a class="task-card" href="exams">
      <div class="task-card-tags"><span class="badge badge-green">Đề thi</span><span class="badge badge-purple">Phân công</span></div>
      <div class="task-card-title">Đề và phân công thi</div>
      <p class="task-card-desc">Cấu hình đề cố định/rút ngẫu nhiên, biên soạn câu hỏi, công bố đề và xếp lịch cho lớp hoặc cá nhân.</p>
      <div class="task-card-foot"><span>Quản lý đề thi</span><span>→</span></div>
    </a>
    <a class="task-card" href="attempts">
      <div class="task-card-tags"><span class="badge badge-purple">Chấm điểm</span><span class="badge badge-pink">Theo dõi</span></div>
      <div class="task-card-title">Lượt thi và kết quả</div>
      <p class="task-card-desc">Tra cứu danh sách lượt làm bài, trạng thái nộp và điểm kết quả cuối cùng của thí sinh.</p>
      <div class="task-card-foot"><span>Xem kết quả</span><span>→</span></div>
    </a>
  </section>
  <% } %>

  <section class="board-col">
    <div class="board-col-head"><h2 class="board-col-title">Đào tạo &amp; Học vụ</h2><span class="badge badge-green">Học vụ</span></div>
    <a class="task-card" href="subjects">
      <div class="task-card-tags"><span class="badge badge-green">Danh mục</span></div>
      <div class="task-card-title">Môn học</div>
      <p class="task-card-desc">Tra cứu danh sách môn học <%=isAdmin ? "và tạo/chỉnh sửa/lưu trữ môn học" : "thuộc phạm vi phụ trách hoặc đã đăng ký"%>.</p>
      <div class="task-card-foot"><span>Mở môn học</span><span>→</span></div>
    </a>
    <a class="task-card" href="classes">
      <div class="task-card-tags"><span class="badge badge-purple">Lớp</span></div>
      <div class="task-card-title">Lớp hành chính</div>
      <p class="task-card-desc">Xem thông tin lớp hành chính và danh sách thành viên sinh viên.</p>
      <div class="task-card-foot"><span>Mở lớp hành chính</span><span>→</span></div>
    </a>
    <a class="task-card" href="offerings">
      <div class="task-card-tags"><span class="badge badge-pink">Học kỳ</span></div>
      <div class="task-card-title">Lớp học phần</div>
      <p class="task-card-desc">Theo dõi lớp học phần theo môn, lớp, giảng viên phụ trách và học kỳ.</p>
      <div class="task-card-foot"><span>Mở lớp học phần</span><span>→</span></div>
    </a>
    <a class="task-card" href="enrollments">
      <div class="task-card-tags"><span class="badge badge-green">Ghi danh</span></div>
      <div class="task-card-title">Ghi danh sinh viên</div>
      <p class="task-card-desc">Xem trạng thái ghi danh, rút lớp hoặc hoàn thành học phần của sinh viên.</p>
      <div class="task-card-foot"><span>Mở ghi danh</span><span>→</span></div>
    </a>
  </section>

  <% if (isAdmin) { %>
  <section class="board-col">
    <div class="board-col-head"><h2 class="board-col-title">Quản trị hệ thống</h2><span class="badge badge-purple">Quản trị</span></div>
    <a class="task-card" href="users">
      <div class="task-card-tags"><span class="badge badge-purple">Người dùng</span><span class="badge badge-pink">Phân quyền</span></div>
      <div class="task-card-title">Quản lý người dùng</div>
      <p class="task-card-desc">Tạo tài khoản, chỉnh sửa hồ sơ, cấp/thu hồi vai trò và khóa/mở khóa phiên truy cập.</p>
      <div class="task-card-foot"><span>Mở quản lý người dùng</span><span>→</span></div>
    </a>
  </section>
  <% } %>
</div>

<%@ include file="layout/app-shell-end.jspf" %>
</body>
</html>
