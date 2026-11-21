<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.EnrollmentPage.Choices,vn.edu.onlineexam.util.Html" %>
<% var c=(Choices)request.getAttribute("choices"); %>
<!DOCTYPE html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Ghi danh sinh viên</title><link rel="stylesheet" href="../../assets/css/app.css"><link rel="stylesheet" href="../../assets/css/subjects.css"></head>
<body><main><a href="../enrollments">Danh sách ghi danh</a><h1>Ghi danh sinh viên</h1>
<form method="post" action="new" class="subject-form"><input type="hidden" name="csrf" value="<%=Html.escape(session.getAttribute("csrf"))%>">
<label>Lớp học phần<select name="offering" required><option value="">Chọn lớp đang mở</option>
<% for(var o:c.offerings()){ %><option value="<%=o.id()%>"><%=Html.escape(o.label())%></option><% } %></select></label>
<label>Sinh viên<select name="student" required><option value="">Chọn sinh viên</option>
<% for(var o:c.students()){ %><option value="<%=o.id()%>"><%=Html.escape(o.label())%></option><% } %></select></label>
<button>Ghi danh</button></form></main></body></html>
