<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.UserDirectory.Profile,vn.edu.onlineexam.util.Html" %>
<%! String value(Object o) { return o==null ? "" : Html.escape(o); } %>
<% boolean create=request.getAttribute("jakarta.servlet.forward.servlet_path").equals("/admin/users/new");
   Profile p=(Profile)request.getAttribute("profile"); if(p==null) p=new Profile(null,null,null,null);
   String title=create?"Thêm tài khoản":"Chỉnh sửa tài khoản"; %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title><%=title%></title><link rel="stylesheet" href="../../assets/css/app.css">
<style>form{display:grid;gap:.8rem;max-width:32rem}label{display:grid;gap:.2rem}input,select,button{font:inherit;padding:.45rem;min-width:0}</style>
</head><body><main><a href="../users">Quay lại danh sách</a><h1><%=title%></h1>
<% if(request.getAttribute("error")!=null){ %><p role="alert"><%=value(request.getAttribute("error"))%></p><% } %>
<form method="post" action="<%=create?"new":"edit"%>">
<input type="hidden" name="csrf" value="<%=value(session.getAttribute("csrf"))%>">
<% if(!create){ %><input type="hidden" name="id" value="<%=value(request.getParameter("id"))%>">
<input type="hidden" name="version" value="<%=value(request.getAttribute("version"))%>"><% } %>
<label>Họ tên *<input required maxlength="150" name="name" value="<%=value(p.name())%>"></label>
<label>Email *<input required maxlength="254" type="email" name="email" value="<%=value(p.email())%>"></label>
<label>Mã người dùng<input maxlength="30" name="code" value="<%=value(p.code())%>"></label>
<label>Điện thoại<input maxlength="20" name="phone" value="<%=value(p.phone())%>"></label>
<% if(create){ %><label>Vai trò ban đầu *<select name="role" required><option value="">Chọn vai trò</option>
<% for(String role:new String[]{"ADMIN","TEACHER","STUDENT"}){ %><option <%=role.equals(request.getParameter("role"))?"selected":""%>><%=role%></option><% } %></select></label>
<label>Mật khẩu * (ít nhất 12 ký tự)<input type="password" required minlength="12" maxlength="1024" name="password" autocomplete="new-password"></label>
<label>Xác nhận mật khẩu *<input type="password" required name="confirm" autocomplete="new-password"></label><% } %>
<button>Lưu tài khoản</button></form></main></body></html>
