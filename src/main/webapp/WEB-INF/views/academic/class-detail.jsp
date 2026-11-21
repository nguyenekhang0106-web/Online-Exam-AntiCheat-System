<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.ClassDirectory,vn.edu.onlineexam.model.ClassDirectory.Member,vn.edu.onlineexam.model.bean.academic.SchoolClass,vn.edu.onlineexam.util.Html" %>
<% var k=(SchoolClass)request.getAttribute("schoolClass");var d=(ClassDirectory<?>)request.getAttribute("result");var q=d.query();
boolean admin="ADMIN".equals(request.getAttribute("roleLabel")),active=k.getStatus().equals("ACTIVE"); %>
<!DOCTYPE html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Thành viên lớp</title><link rel="stylesheet" href="../../assets/css/app.css"><link rel="stylesheet" href="../../assets/css/subjects.css"></head><body><main>
<a href="../classes">Danh sách lớp</a><h1><%=Html.escape(k.getClassName())%></h1>
<p><%=Html.escape(k.getClassCode())%> · <%=Html.escape(k.getAcademicYear())%> · <%=Html.escape(k.getStatus())%></p>
<% if(request.getAttribute("notice")!=null){ %><p role="status"><%=Html.escape(request.getAttribute("notice"))%></p><% } %>
<% if(admin){ %><p><a href="edit?id=<%=k.getId()%>">Sửa lớp</a></p>
<% if(!k.getStatus().equals("ARCHIVED")){ %><details><summary>Lưu trữ lớp</summary><form method="post" action="archive">
<%@ include file="class-token.jspf" %><button>Xác nhận lưu trữ</button></form></details><% }} %>
<h2>Thành viên lớp</h2><% if(admin && active){ %><form class="filters" method="post" action="membership">
<%@ include file="class-token.jspf" %><label>ID tài khoản sinh viên<input required type="number" min="1" name="student"></label><button name="action" value="join">Thêm thành viên</button></form><% } %>
<%@ include file="class-filters.jspf" %>
<% if(d.rows().isEmpty()){ %><p>Không tìm thấy thành viên.</p><% }else{ %><div class="results"><table class="class-roster"><thead><tr><th>Sinh viên</th><th>Trạng thái</th><th>Gia nhập (UTC)</th><th>Rời lớp (UTC)</th><% if(admin){ %><th>Thao tác</th><% } %></tr></thead><tbody>
<% for(Object item:d.rows()){var m=(Member)item;boolean leave=m.status().equals("ACTIVE"); %>
<tr><td><%=Html.escape(m.name())%> · <%=Html.escape(m.code())%></td><td><%=Html.escape(m.status())%></td><td><%=Html.escape(m.joined())%></td><td><%=Html.escape(m.left())%></td>
<% if(admin){ %><td><% if(leave || active){ %><form method="post" action="membership"><%@ include file="class-token.jspf" %><input type="hidden" name="student" value="<%=m.studentId()%>">
<button name="action" value="<%=leave?"leave":"rejoin"%>"><%=leave?"Đánh dấu rời lớp":"Tái gia nhập"%></button></form><% } %></td><% } %></tr><% } %></tbody></table></div><% } %>
<%@ include file="class-paging.jspf" %></main></body></html>
