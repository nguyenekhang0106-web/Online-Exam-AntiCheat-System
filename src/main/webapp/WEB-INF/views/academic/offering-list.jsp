<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.OfferingPage,vn.edu.onlineexam.util.Html" %>
<% var result=(OfferingPage)request.getAttribute("result"); var q=result.query(); %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Lớp học phần</title><link rel="stylesheet" href="../assets/css/app.css"><link rel="stylesheet" href="../assets/css/subjects.css"></head><body><main>
<a href="account">Tài khoản</a><h1>Lớp học phần</h1>
<% if("ADMIN".equals(request.getAttribute("roleLabel"))){ %><p><a href="offerings/new">Tạo lớp học phần</a></p><% } %>
<form id="filters" class="filters" method="get"><label>Môn, lớp hoặc giảng viên<input name="q" maxlength="200" value="<%=Html.escape(q.keyword())%>"></label>
<label>ID môn học<input type="number" min="0" name="subject" value="<%=q.subjectId()%>"><small>0 để xem tất cả</small></label>
<label>Trạng thái<select name="status"><option value="">Tất cả</option><% for(String s:new String[]{"ACTIVE","CLOSED","ARCHIVED"}){ %>
<option <%=q.status().equals(s)?"selected":""%>><%=s%></option><% } %></select></label>
<label>Sắp xếp<select name="sort"><% for(String[] s:new String[][]{{"id","ID lớp học phần"},{"subject","Môn học"},{"schoolClass","Lớp hành chính"},{"teacher","Giảng viên"},{"year","Năm học"}}){ %>
<option value="<%=s[0]%>" <%=q.sort().name().equals(s[0])?"selected":""%>><%=s[1]%></option><% } %></select></label>
<label>Thứ tự<select name="direction"><option value="ASC">Tăng</option><option value="DESC" <%=q.descending()?"selected":""%>>Giảm</option></select></label>
<label>Số dòng<input name="size" type="number" min="1" max="100" value="<%=q.size()%>"></label><button>Tìm kiếm</button></form>
<p><%=result.total()%> lớp học phần phù hợp</p><% if(result.total()>0){ %>
<div class="results"><table><thead><tr><th>Môn học</th><th>Lớp hành chính</th><th>Giảng viên</th><th>Năm học / kỳ</th><th>Trạng thái</th></tr></thead><tbody>
<% for(var r:result.rows()){ %><tr><td><a href="offerings/detail?id=<%=r.id()%>"><%=Html.escape(r.subject())%> (#<%=r.id()%>)</a></td>
<td><%=Html.escape(r.schoolClass())%></td><td><%=Html.escape(r.teacher())%></td><td><%=Html.escape(r.year())%> / <%=Html.escape(r.semester())%></td><td><%=Html.escape(r.status())%></td></tr><% } %></tbody></table></div>
<nav><p>Trang <%=result.page()%> / <%=result.pages()%></p><% if(result.page()>1){ %><button form="filters" name="page" value="<%=result.page()-1%>">Trang trước</button><% } %>
<% if(result.page()<result.pages()){ %><button form="filters" name="page" value="<%=result.page()+1%>">Trang sau</button><% } %></nav><% } %></main></body></html>
