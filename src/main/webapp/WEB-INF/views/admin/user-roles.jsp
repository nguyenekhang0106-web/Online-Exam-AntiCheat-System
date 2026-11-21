<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.UserDirectory.Membership,vn.edu.onlineexam.util.Html,vn.edu.onlineexam.service.UserManagementService" %>
<%
  var member = (Membership) request.getAttribute("membership");
  request.setAttribute("pageCrumb", "Vai trò tài khoản");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Vai trò tài khoản | Online Exam</title>
  <link rel="stylesheet" href="../../assets/css/app.css">
</head>
<body>
<%@ include file="../layout/app-shell-start.jspf" %>

<a class="back-link" href="../users">← Danh sách người dùng</a>
<div class="page-header">
  <div class="page-header-info">
    <h1>Vai trò của <%=Html.escape(member.name())%></h1>
    <div class="inline-meta">
      <span>Trạng thái tài khoản:</span>
      <span class="<%=uiBadge(member.status())%>"><%=Html.escape(member.status())%></span>
    </div>
  </div>
</div>

<p class="alert alert-info">Sau khi đổi vai trò, các phiên hiện tại của tài khoản sẽ bị thu hồi và người dùng cần đăng nhập lại.</p>

<div class="role-grid">
<% for (var role : member.choices()) { %>
  <section class="role-card">
    <div class="role-card-head">
      <h2><%=Html.escape(role.name())%></h2>
      <span class="<%=role.assigned() ? "badge badge-green" : "badge badge-neutral"%>"><%=role.assigned() ? "Đã cấp" : "Chưa cấp"%></span>
    </div>
    <div class="inline-meta">
      <span class="<%=uiBadge(role.code())%>"><%=Html.escape(role.code())%></span>
    </div>
    <% if (UserManagementService.MANAGED_ROLES.contains(role.code())) { %>
    <form class="stack-form" method="post" action="roles">
      <input type="hidden" name="csrf" value="<%=Html.escape(session.getAttribute("csrf"))%>">
      <input type="hidden" name="id" value="<%=member.id()%>">
      <input type="hidden" name="role" value="<%=Html.escape(role.code())%>">
      <% if (role.assigned() && role.code().equals("ADMIN")) { %>
      <label class="check-label"><input type="checkbox" required name="confirm" value="yes"> <span>Tôi xác nhận thu hồi quyền quản trị.</span></label>
      <% } %>
      <button class="<%=role.assigned() ? "btn-danger" : "btn-primary"%>" name="action" value="<%=role.assigned() ? "revoke" : "grant"%>"><%=role.assigned() ? "Thu hồi vai trò" : "Cấp vai trò"%></button>
    </form>
    <% } %>
  </section>
<% } %>
</div>

<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
