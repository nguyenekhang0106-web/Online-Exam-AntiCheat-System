<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.EnrollmentPage.Row,vn.edu.onlineexam.util.Html" %>
<% var r=(Row)request.getAttribute("row"); %>
<!DOCTYPE html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Chi tiết ghi danh</title><link rel="stylesheet" href="../../assets/css/app.css"><link rel="stylesheet" href="../../assets/css/subjects.css"></head>
<body><main><a href="../enrollments">Danh sách ghi danh</a><h1>Chi tiết ghi danh</h1>
<% if(request.getAttribute("notice")!=null){ %><p role="status"><%=Html.escape(request.getAttribute("notice"))%></p><% } %>
<dl><dt>Sinh viên</dt><dd><%=Html.escape(r.name())%> · <%=Html.escape(r.code())%></dd><dt>Lớp học phần</dt>
<dd>#<%=r.offering()%> · <%=Html.escape(r.subject())%> · <%=Html.escape(r.schoolClass())%></dd><dt>Năm học / học kỳ</dt><dd><%=Html.escape(r.year())%> / <%=Html.escape(r.semester())%></dd>
<dt>Trạng thái</dt><dd><%=Html.escape(r.status())%></dd><dt>Ngày ghi danh (UTC)</dt><dd><%=Html.escape(r.enrolledAt())%></dd></dl>
<% if("ADMIN".equals(request.getAttribute("roleLabel")) && r.status().equals("ENROLLED")){ %>
<form method="post"><input type="hidden" name="csrf" value="<%=Html.escape(session.getAttribute("csrf"))%>">
<input type="hidden" name="offering" value="<%=r.offering()%>"><input type="hidden" name="student" value="<%=r.student()%>">
<button formaction="withdraw">Rút khỏi lớp</button> <button formaction="complete">Đánh dấu hoàn thành</button></form><% } %></main></body></html>
