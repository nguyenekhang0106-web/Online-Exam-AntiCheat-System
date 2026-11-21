<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Online Exam | Hệ thống Thi Trắc Nghiệm Trực Tuyến</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css">
</head>
<body>
<div class="public-shell">
  <header class="public-nav">
    <div class="sidebar-brand" style="padding:0">
      <span class="brand-mark" aria-hidden="true">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2L3 7v10l9 5 9-5V7l-9-5z"/><path d="M9 12l2 2 4-4"/></svg>
      </span>
      <span class="brand-text">
        <span class="brand-name">Online Exam</span>
        <span class="brand-sub">Cổng thi trực tuyến</span>
      </span>
    </div>
    <a class="btn btn-primary" href="${pageContext.request.contextPath}/login">Đăng nhập</a>
  </header>

  <main class="public-main">
    <div class="hero-card">
      <div class="hero-top">
        <span class="badge badge-green">Thi trực tuyến</span>
        <span class="badge badge-purple">Ngân hàng câu hỏi</span>
        <span class="badge badge-pink">Chấm điểm tự động</span>
      </div>
      <h1 class="hero-title">Hệ thống Thi Trắc Nghiệm Trực Tuyến</h1>
      <p class="hero-lead">Nền tảng tổ chức kiểm tra và đánh giá học tập hiện đại dành cho nhà trường, giảng viên và sinh viên — quản lý lớp học, biên soạn đề thi linh hoạt và làm bài an toàn, ổn định.</p>

      <div class="page-actions">
        <a class="btn btn-primary" href="${pageContext.request.contextPath}/login">Đăng nhập hệ thống</a>
      </div>

      <div class="hero-features">
        <div class="hero-feature">
          <div class="task-card-tags"><span class="badge badge-purple">Quản trị &amp; Học vụ</span></div>
          <h3>Tổ chức đào tạo chặt chẽ</h3>
          <p>Quản lý tài khoản, phân quyền vai trò, môn học, lớp hành chính, lớp học phần và ghi danh sinh viên tập trung.</p>
        </div>
        <div class="hero-feature">
          <div class="task-card-tags"><span class="badge badge-green">Ngân hàng &amp; Đề thi</span></div>
          <h3>Biên soạn &amp; Rút đề linh hoạt</h3>
          <p>Hỗ trợ đề cố định hoặc rút câu ngẫu nhiên mỗi lượt thi, trộn câu hỏi/đáp án và xếp lịch theo lớp hoặc cá nhân.</p>
        </div>
        <div class="hero-feature">
          <div class="task-card-tags"><span class="badge badge-pink">Làm bài &amp; An toàn</span></div>
          <h3>Lưu bài tức thời &amp; Tự động chấm</h3>
          <p>Tự động lưu đáp án khi chọn, bảo toàn bài làm khi gián đoạn mạng, đồng hồ đếm ngược chính xác và công bố điểm rõ ràng.</p>
        </div>
      </div>

      <div class="hero-footer">
        <span>© 2026 Online Exam</span>
        <span>Cổng thi trắc nghiệm trực tuyến</span>
      </div>
    </div>
  </main>
</div>
</body>
</html>
