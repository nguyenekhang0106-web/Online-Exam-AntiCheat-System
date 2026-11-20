<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.UserDirectory,vn.edu.onlineexam.util.Html" %>
<% var d=(UserDirectory)request.getAttribute("directory"); var q=d.query(); %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Quản lý người dùng</title><link rel="stylesheet" href="../assets/css/app.css">
<style>main{max-width:76rem}.filters{display:flex;flex-wrap:wrap;align-items:end;gap:.8rem}label{display:grid;gap:.3rem}
input,select,button{font:inherit;padding:.4rem}.results{overflow-x:auto}table{width:100%;border-collapse:collapse}
th,td{text-align:left;padding:.6rem;border-bottom:1px solid #ddd}caption{text-align:left;margin:1rem 0}</style></head>
<body><main><a href="account">Tài khoản quản trị</a><h1>Quản lý người dùng</h1>
<p><a href="users/new">Thêm tài khoản</a></p>
<% if(request.getAttribute("userSaved")!=null) { %><p role="status">Đã lưu tài khoản.</p><% } %>
<% if(request.getAttribute("statusMessage")!=null) { %><p role="status"><%=Html.escape(request.getAttribute("statusMessage"))%></p><% } %>
<form id="search" method="get" action="users"><div class="filters">
<label>Tên, email hoặc mã<input name="q" maxlength="254" value="<%=Html.escape(q.keyword())%>"></label>
<label>Vai trò<select name="role"><option value="">Tất cả</option>
<% for(String r:new String[]{"ADMIN","TEACHER","STUDENT"}) { %><option <%=r.equals(q.role())?"selected":""%>><%=r%></option><% } %></select></label>
<label>Trạng thái<select name="status"><option value="">Tất cả</option>
<% for(String s:new String[]{"ACTIVE","LOCKED","INACTIVE"}) { %><option <%=s.equals(q.status())?"selected":""%>><%=s%></option><% } %></select></label>
<label>Sắp xếp<select name="sort">
<% for(String[] s:new String[][]{{"created","Ngày tạo"},{"name","Họ tên"},{"email","Email"},{"code","Mã người dùng"}}) { %>
<option value="<%=s[0]%>" <%=s[0].equals(q.sort().name())?"selected":""%>><%=s[1]%></option><% } %></select></label>
<label>Thứ tự<select name="direction"><option value="ASC" <%=q.direction().name().equals("ASC")?"selected":""%>>Tăng dần</option>
<option value="DESC" <%=q.direction().name().equals("DESC")?"selected":""%>>Giảm dần</option></select></label>
<label>Số dòng/trang<input name="pageSize" type="number" min="1" max="100" value="<%=q.size()%>"></label>
<button>Tìm kiếm</button></div></form>
<div class="results"><table><caption><%=d.total()%> tài khoản phù hợp</caption>
<thead><tr><th>Mã người dùng</th><th>Họ tên</th><th>Email</th><th>Vai trò</th><th>Trạng thái</th><th>Ngày tạo (UTC)</th><th>Thao tác</th></tr></thead><tbody>
<% for(var row:d.rows()) { %><tr><td><%=Html.escape(row.code())%></td><td><%=Html.escape(row.name())%></td><td><%=Html.escape(row.email())%></td>
<td><%=Html.escape(row.roles())%></td><td><%=Html.escape(row.status())%></td><td><%=Html.escape(row.created())%></td><td><a href="users/edit?id=<%=row.id()%>">Sửa</a>
<a href="users/roles?id=<%=row.id()%>">Vai trò</a>
<% boolean lock="ACTIVE".equals(row.status());
   var actor=(vn.edu.onlineexam.model.AuthUser)request.getAttribute("authenticatedUser");
   if ((lock && actor.id()!=row.id()) || "LOCKED".equals(row.status())) { %>
<details><summary><%=lock?"Khóa":"Mở khóa"%></summary>
<form method="post" action="users/<%=lock?"lock":"unlock"%>">
<input type="hidden" name="csrf" value="<%=Html.escape(session.getAttribute("csrf"))%>">
<input type="hidden" name="id" value="<%=row.id()%>"><input type="hidden" name="version" value="<%=row.version()%>">
<% for(String key:new String[]{"q","role","status","page","pageSize","sort","direction"}) {
   String val=request.getParameter(key); if(val!=null) { %><input type="hidden" name="<%=key%>" value="<%=Html.escape(val)%>"><% }} %>
<p><%=lock?"Thu hồi mọi phiên của":"Cho phép đăng nhập lại với"%> <%=Html.escape(row.email())%>?</p>
<button>Xác nhận <%=lock?"khóa":"mở khóa"%></button></form></details><% } %>
</td></tr><% } %>
</tbody></table></div>
<% if(d.total()==0) { %><p>Không tìm thấy tài khoản phù hợp.</p><% } else { %>
<nav aria-label="Phân trang"><p>Trang <%=d.page()%> / <%=d.pages()%></p>
<% if(d.page()>1) { %><button form="search" name="page" value="<%=d.page()-1%>">Trang trước</button><% } %>
<% if(d.page()<d.pages()) { %><button form="search" name="page" value="<%=d.page()+1%>">Trang sau</button><% } %></nav><% } %>
</main></body></html>
