<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.UserDirectory.Membership,vn.edu.onlineexam.util.Html,vn.edu.onlineexam.service.UserManagementService" %>
<% var member = (Membership) request.getAttribute("membership"); %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Vai trò tài khoản</title><link rel="stylesheet" href="../../assets/css/app.css"></head>
<body><main><a href="../users">Danh sách người dùng</a>
<h1>Vai trò của <%=Html.escape(member.name())%></h1>
<p>Trạng thái tài khoản: <%=Html.escape(member.status())%></p>
<p>Sau khi đổi vai trò, người dùng cần đăng nhập lại.</p>
<% for (var role : member.choices()) { %>
<section><h2><%=Html.escape(role.name())%> (<%=Html.escape(role.code())%>)</h2>
<p><%=role.assigned() ? "Đã cấp" : "Chưa cấp"%></p>
<% if (UserManagementService.MANAGED_ROLES.contains(role.code())) { %>
<form method="post" action="roles">
<input type="hidden" name="csrf" value="<%=Html.escape(session.getAttribute("csrf"))%>">
<input type="hidden" name="id" value="<%=member.id()%>">
<input type="hidden" name="role" value="<%=Html.escape(role.code())%>">
<% if (role.assigned() && role.code().equals("ADMIN")) { %>
<label><input type="checkbox" required name="confirm" value="yes"> Tôi xác nhận thu hồi quyền quản trị.</label>
<% } %>
<button name="action" value="<%=role.assigned() ? "revoke" : "grant"%>"><%=role.assigned() ? "Thu hồi vai trò" : "Cấp vai trò"%></button>
</form><% } %></section><% } %>
</main></body></html>
