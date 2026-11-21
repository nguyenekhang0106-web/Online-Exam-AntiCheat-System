<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.EnrollmentPage,vn.edu.onlineexam.util.Html" %>
<% var d=(EnrollmentPage)request.getAttribute("result");var q=d.query(); %>
<!DOCTYPE html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Ghi danh sinh viên</title><link rel="stylesheet" href="../assets/css/app.css"><link rel="stylesheet" href="../assets/css/subjects.css"></head>
<body><main><a href="account">Tài khoản</a><h1>Ghi danh sinh viên</h1>
<% if("ADMIN".equals(request.getAttribute("roleLabel"))){ %><p><a href="enrollments/new">Ghi danh sinh viên</a></p><% } %>
<form id="filters" method="get" class="filters"><label>Mã, tên hoặc email sinh viên<input name="q" maxlength="254" value="<%=Html.escape(q.keyword())%>"></label>
<label>ID lớp học phần (0 = tất cả)<input name="offering" type="number" min="0" value="<%=q.offering()%>"></label>
<label>Trạng thái<select name="status"><option value="">Tất cả</option><% for(String s:new String[]{"ENROLLED","WITHDRAWN","COMPLETED"}){ %>
<option <%=q.status().equals(s)?"selected":""%>><%=s%></option><% } %></select></label>
<label>Sắp xếp<select name="sort"><% for(String[] s:new String[][]{{"name","Tên sinh viên"},{"code","Mã sinh viên"},{"offering","Lớp học phần"},{"enrolled","Ngày ghi danh"}}){ %>
<option value="<%=s[0]%>" <%=q.sort().name().equals(s[0])?"selected":""%>><%=s[1]%></option><% } %></select></label>
<label>Thứ tự<select name="direction"><option value="ASC">Tăng</option><option value="DESC" <%=q.descending()?"selected":""%>>Giảm</option></select></label>
<label>Số dòng<input type="number" min="1" max="100" name="size" value="<%=q.size()%>"></label><button>Tìm kiếm</button></form>
<p><%=d.total()%> ghi danh phù hợp</p><% if(d.total()>0){ %><div class="results"><table><thead><tr><th>Sinh viên</th><th>Lớp học phần</th><th>Trạng thái</th></tr></thead><tbody>
<% for(var r:d.rows()){ %><tr><td><a href="enrollments/detail?offering=<%=r.offering()%>&amp;student=<%=r.student()%>"><%=Html.escape(r.name())%></a> · <%=Html.escape(r.code())%></td>
<td>#<%=r.offering()%> · <%=Html.escape(r.subject())%> · <%=Html.escape(r.schoolClass())%></td><td><%=Html.escape(r.status())%></td></tr><% } %></tbody></table></div>
<nav><p>Trang <%=d.page()%> / <%=d.pages()%></p><% if(d.page()>1){ %><button form="filters" name="page" value="<%=d.page()-1%>">Trang trước</button><% } %>
<% if(d.page()<d.pages()){ %><button form="filters" name="page" value="<%=d.page()+1%>">Trang sau</button><% } %></nav><% } %></main></body></html>
