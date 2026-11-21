<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Đăng nhập | Online Exam</title>
  <link rel="stylesheet" href="assets/css/app.css">
</head>
<body>
<div class="public-shell">
  <header class="public-nav">
    <a class="sidebar-brand" href="${pageContext.request.contextPath}/home" style="padding:0;text-decoration:none">
      <span class="brand-mark" aria-hidden="true">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2L3 7v10l9 5 9-5V7l-9-5z"/><path d="M9 12l2 2 4-4"/></svg>
      </span>
      <span class="brand-text">
        <span class="brand-name">Online Exam</span>
        <span class="brand-sub">Cổng thi trực tuyến</span>
      </span>
    </a>
    <a class="btn btn-sm" href="${pageContext.request.contextPath}/home">Trang chủ</a>
  </header>

  <main class="public-main">
    <div class="auth-card">
      <div class="auth-header">
        <span class="brand-mark" aria-hidden="true">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2L3 7v10l9 5 9-5V7l-9-5z"/><path d="M9 12l2 2 4-4"/></svg>
        </span>
        <h1>Đăng nhập</h1>
        <p class="page-subtitle">Sử dụng tài khoản nhà trường cấp để tiếp tục</p>
      </div>

      <p role="alert">${invalid ? 'Không thể đăng nhập với thông tin đã cung cấp.' : ''}</p>

      <form class="auth-form" method="post" action="login">
        <input type="hidden" name="csrf" value="${sessionScope.csrf}">
        <label>Email
          <input type="email" name="email" required maxlength="254" autocomplete="username" placeholder="ten.nguoidung@truong.edu.vn">
        </label>
        <label>Mật khẩu
          <input type="password" name="password" required maxlength="1024" autocomplete="current-password" placeholder="Nhập mật khẩu">
        </label>
        <button class="btn-primary">Đăng nhập</button>
      </form>
    </div>
  </main>
</div>
</body>
</html>
