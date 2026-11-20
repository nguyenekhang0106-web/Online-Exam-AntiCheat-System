<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.SubjectPage,vn.edu.onlineexam.util.Html" %>
<% var d=(SubjectPage)request.getAttribute("directory"); var q=d.query(); %>
<!DOCTYPE html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Môn học</title><link rel="stylesheet" href="../assets/css/app.css"><link rel="stylesheet" href="../assets/css/subjects.css"></head>
<body><main><a href="account">Tài khoản</a><h1>Môn học</h1>
<% if("ADMIN".equals(request.getAttribute("roleLabel"))){ %><p><a href="subjects/new">Thêm môn học</a></p><% } %>
<form id="filters" method="get" class="filters"><label>Mã hoặc tên môn<input name="q" maxlength="200" value="<%=Html.escape(q.keyword())%>"></label>
<label>Trạng thái<select name="status"><option value="">Tất cả</option><% for(String s:new String[]{"ACTIVE","INACTIVE","ARCHIVED"}){ %>
<option <%=s.equals(q.status())?"selected":""%>><%=s%></option><% } %></select></label>
<label>Sắp xếp<select name="sort"><% for(String[] s:new String[][]{{"code","Mã môn"},{"name","Tên môn"},{"updated","Ngày cập nhật"}}){ %>
<option value="<%=s[0]%>" <%=q.sort().name().equals(s[0])?"selected":""%>><%=s[1]%></option><% } %></select></label>
<label>Thứ tự<select name="direction"><option value="ASC">Tăng dần</option><option value="DESC" <%=q.descending()?"selected":""%>>Giảm dần</option></select></label>
<label>Số môn mỗi trang<input name="size" type="number" min="1" max="100" value="<%=q.size()%>"></label><button>Tìm kiếm</button></form>
<p><%=d.total()%> môn học phù hợp</p>
<% if(!d.rows().isEmpty()){ %><div class="results"><table><thead><tr><th>Mã môn</th><th>Tên môn</th><th>Trạng thái</th></tr></thead><tbody>
<% for(var s:d.rows()){ %><tr><td><%=Html.escape(s.getSubjectCode())%></td><td><a href="subjects/detail?id=<%=s.getId()%>"><%=Html.escape(s.getSubjectName())%></a></td><td><%=Html.escape(s.getStatus())%></td></tr><% } %>
</tbody></table></div><nav aria-label="Phân trang"><p>Trang <%=d.page()%> / <%=d.pages()%></p>
<% if(d.page()>1){ %><button form="filters" name="page" value="<%=d.page()-1%>">Trang trước</button><% } %>
<% if(d.page()<d.pages()){ %><button form="filters" name="page" value="<%=d.page()+1%>">Trang sau</button><% } %></nav><% } %>
</main></body></html>
