<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Đăng nhập | Online Exam</title>
<link rel="stylesheet" href="assets/css/app.css"></head>
<body><main><h1>Đăng nhập</h1>
<form method="post" action="login">
    <input type="hidden" name="csrf" value="${sessionScope.csrf}">
    <p><label>Email <input type="email" name="email" required maxlength="254" autocomplete="username"></label></p>
    <p><label>Mật khẩu <input type="password" name="password" required maxlength="1024" autocomplete="current-password"></label></p>
    <p role="alert">${invalid ? 'Không thể đăng nhập với thông tin đã cung cấp.' : ''}</p>
    <button>Đăng nhập</button>
</form></main></body></html>
