<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.UserDirectory.Profile,vn.edu.onlineexam.util.Html" %>
<%! String value(Object o) { return o==null ? "" : Html.escape(o); } %>
<%
  boolean create=request.getAttribute("jakarta.servlet.forward.servlet_path").equals("/admin/users/new");
  Profile p=(Profile)request.getAttribute("profile"); if(p==null) p=new Profile(null,null,null,null);
  String title=create?"Thêm tài khoản":"Chỉnh sửa tài khoản";
  request.setAttribute("pageCrumb", title);
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title><%=title%> | Online Exam</title>
  <link rel="stylesheet" href="../../assets/css/app.css">
</head>
<body>
<%@ include file="../layout/app-shell-start.jspf" %>

<a class="back-link" href="../users">← Quay lại danh sách người dùng</a>
<div class="page-header">
  <div class="page-header-info">
    <h1><%=title%></h1>
    <p class="page-subtitle"><%=create ? "Tạo tài khoản mới và gán vai trò ban đầu." : "Cập nhật thông tin hồ sơ tài khoản."%></p>
  </div>
</div>

<% if(request.getAttribute("error")!=null){ %><p role="alert"><%=value(request.getAttribute("error"))%></p><% } %>

<div class="card form-card">
  <form class="stack-form" method="post" action="<%=create?"new":"edit"%>">
    <input type="hidden" name="csrf" value="<%=value(session.getAttribute("csrf"))%>">
    <% if(!create){ %>
      <input type="hidden" name="id" value="<%=value(request.getParameter("id"))%>">
      <input type="hidden" name="version" value="<%=value(request.getAttribute("version"))%>">
    <% } %>

    <div class="form-grid-2">
      <label>Họ tên *
        <input required maxlength="150" name="name" value="<%=value(p.name())%>" placeholder="Nguyễn Văn A">
      </label>
      <label>Email *
        <input required maxlength="254" type="email" name="email" value="<%=value(p.email())%>" placeholder="email@truong.edu.vn">
      </label>
    </div>

    <div class="form-grid-2">
      <label>Mã người dùng
        <input maxlength="30" name="code" value="<%=value(p.code())%>" placeholder="SV2026001">
      </label>
      <label>Điện thoại
        <input maxlength="20" name="phone" value="<%=value(p.phone())%>" placeholder="09xxxxxxxx">
      </label>
    </div>

    <% if(create){ %>
      <label>Vai trò ban đầu *
        <select name="role" required>
          <option value="">Chọn vai trò</option>
          <% for(String role:new String[]{"ADMIN","TEACHER","STUDENT"}){ %><option <%=role.equals(request.getParameter("role"))?"selected":""%>><%=role%></option><% } %>
        </select>
      </label>
      <div class="form-grid-2">
        <label>Mật khẩu * (ít nhất 12 ký tự)
          <input type="password" required minlength="12" maxlength="1024" name="password" autocomplete="new-password">
        </label>
        <label>Xác nhận mật khẩu *
          <input type="password" required name="confirm" autocomplete="new-password">
        </label>
      </div>
    <% } %>

    <div class="form-actions">
      <button class="btn-primary">Lưu tài khoản</button>
      <a class="btn" href="../users">Hủy</a>
    </div>
  </form>
</div>

<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
