<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.EnrollmentPage.Choices,vn.edu.onlineexam.util.Html" %>
<%
  var c=(Choices)request.getAttribute("choices");
  request.setAttribute("pageCrumb", "Ghi danh sinh viên");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Ghi danh sinh viên | Online Exam</title>
  <link rel="stylesheet" href="../../assets/css/app.css">
  <link rel="stylesheet" href="../../assets/css/subjects.css">
</head>
<body>
<%@ include file="../layout/app-shell-start.jspf" %>

<a class="back-link" href="../enrollments">← Danh sách ghi danh</a>
<div class="page-header">
  <div class="page-header-info"><h1>Ghi danh sinh viên</h1></div>
</div>

<div class="card form-card">
  <form method="post" action="new" class="subject-form">
    <input type="hidden" name="csrf" value="<%=Html.escape(session.getAttribute("csrf"))%>">
    <label>Lớp học phần *
      <select name="offering" required>
        <option value="">Chọn lớp đang mở</option>
        <% for(var o:c.offerings()){ %><option value="<%=o.id()%>"><%=Html.escape(o.label())%></option><% } %>
      </select>
    </label>
    <label>Sinh viên *
      <select name="student" required>
        <option value="">Chọn sinh viên</option>
        <% for(var o:c.students()){ %><option value="<%=o.id()%>"><%=Html.escape(o.label())%></option><% } %>
      </select>
    </label>
    <div class="form-actions">
      <button class="btn-primary">Ghi danh</button>
      <a class="btn" href="../enrollments">Hủy</a>
    </div>
  </form>
</div>

<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
