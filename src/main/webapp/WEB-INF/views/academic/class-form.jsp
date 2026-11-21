<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.bean.academic.SchoolClass,vn.edu.onlineexam.util.Html" %>
<%
  var k=(SchoolClass)request.getAttribute("schoolClass");
  String title=k==null?"Thêm lớp hành chính":"Sửa lớp hành chính";
  request.setAttribute("pageCrumb", title);
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title><%=title%> | Online Exam</title>
  <link rel="stylesheet" href="../../assets/css/app.css">
  <link rel="stylesheet" href="../../assets/css/subjects.css">
</head>
<body>
<%@ include file="../layout/app-shell-start.jspf" %>

<a class="back-link" href="../classes">← Danh sách lớp</a>
<div class="page-header">
  <div class="page-header-info"><h1><%=title%></h1></div>
</div>

<div class="card form-card">
  <form class="subject-form" method="post" action="<%=k==null?"new":"edit"%>">
    <input type="hidden" name="csrf" value="<%=Html.escape(session.getAttribute("csrf"))%>">
    <% if(k!=null){ %>
      <input type="hidden" name="id" value="<%=k.getId()%>">
      <input type="hidden" name="version" value="<%=k.getUpdatedAt()%>">
    <% } %>
    <div class="form-grid-2">
      <label>Mã lớp *
        <input name="code" required maxlength="30" placeholder="CNTT2026A" value="<%=k==null?"":Html.escape(k.getClassCode())%>">
      </label>
      <label>Năm học
        <input name="year" maxlength="9" placeholder="2026-2027" value="<%=k==null || k.getAcademicYear()==null?"":Html.escape(k.getAcademicYear())%>">
      </label>
    </div>
    <label>Tên lớp *
      <input name="name" required maxlength="150" placeholder="Công nghệ thông tin K2026" value="<%=k==null?"":Html.escape(k.getClassName())%>">
    </label>
    <div class="form-actions">
      <button class="btn-primary">Lưu lớp</button>
      <a class="btn" href="../classes">Hủy</a>
    </div>
  </form>
</div>

<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
