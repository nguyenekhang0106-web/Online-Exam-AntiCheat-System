<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.ClassDirectory,vn.edu.onlineexam.model.bean.academic.SchoolClass,vn.edu.onlineexam.util.Html" %>
<% var d=(ClassDirectory<?>)request.getAttribute("result"); var q=d.query(); SchoolClass k=null; %>
<!DOCTYPE html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Lớp hành chính</title><link rel="stylesheet" href="../assets/css/app.css"><link rel="stylesheet" href="../assets/css/subjects.css"></head><body><main>
<a href="account">Tài khoản</a><h1>Lớp hành chính</h1>
<% if("ADMIN".equals(request.getAttribute("roleLabel"))){ %><p><a href="classes/new">Thêm lớp</a></p><% } %>
<%@ include file="class-filters.jspf" %>
<% if(d.rows().isEmpty()){ %><p>Không tìm thấy lớp.</p><% }else{ %><div class="results"><table><thead><tr><th>Mã lớp</th><th>Tên lớp</th><th>Năm học</th><th>Trạng thái</th></tr></thead><tbody>
<% for(Object item:d.rows()){var row=(SchoolClass)item; %><tr><td><%=Html.escape(row.getClassCode())%></td><td><a href="classes/detail?id=<%=row.getId()%>"><%=Html.escape(row.getClassName())%></a></td>
<td><%=Html.escape(row.getAcademicYear())%></td><td><%=Html.escape(row.getStatus())%></td></tr><% } %></tbody></table></div><% } %>
<%@ include file="class-paging.jspf" %></main></body></html>
