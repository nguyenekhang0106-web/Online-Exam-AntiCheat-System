<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.OfferingPage.Row,vn.edu.onlineexam.util.Html" %>
<% var r=(Row)request.getAttribute("row"); %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Chi tiết lớp học phần</title><link rel="stylesheet" href="../../assets/css/app.css"><link rel="stylesheet" href="../../assets/css/subjects.css"></head><body><main>
<a href="../offerings">Danh sách lớp học phần</a><h1>Lớp học phần #<%=r.id()%></h1>
<% if(request.getAttribute("notice")!=null){ %><p role="status"><%=Html.escape(request.getAttribute("notice"))%></p><% } %>
<dl><dt>Môn học</dt><dd><a href="../subjects/detail?id=<%=r.subjectId()%>"><%=Html.escape(r.subject())%></a></dd>
<dt>Lớp hành chính</dt><dd><%=Html.escape(r.schoolClass())%></dd><dt>Giảng viên</dt><dd><%=Html.escape(r.teacher())%></dd>
<dt>Năm học / học kỳ</dt><dd><%=Html.escape(r.year())%> / <%=Html.escape(r.semester())%></dd><dt>Trạng thái</dt><dd><%=Html.escape(r.status())%></dd></dl>
<% if("ADMIN".equals(request.getAttribute("roleLabel")) && "ACTIVE".equals(r.status())){ %>
<p><a href="edit?id=<%=r.id()%>">Phân công giảng viên</a></p>
<details><summary>Đóng lớp học phần</summary><form method="post" action="close">
<input type="hidden" name="csrf" value="<%=Html.escape(session.getAttribute("csrf"))%>"><input type="hidden" name="id" value="<%=r.id()%>">
<input type="hidden" name="version" value="<%=r.version()%>"><button>Xác nhận đóng lớp</button></form></details><% } %></main></body></html>
