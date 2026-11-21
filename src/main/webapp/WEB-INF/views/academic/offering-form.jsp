<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.OfferingPage.*,vn.edu.onlineexam.util.Html" %>
<%
  var r=(Row)request.getAttribute("row");
  var c=(Choices)request.getAttribute("choices");
  boolean create=r==null;
  String title=create?"Tạo lớp học phần":"Phân công giảng viên";
  request.setAttribute("pageCrumb", title);
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title><%=title%> | Online Exam</title>
  <link rel="stylesheet" href="../../assets/css/app.css">
  <link rel="stylesheet" href="../../assets/css/subjects.css">
</head>
<body>
<%@ include file="../layout/app-shell-start.jspf" %>

<a class="back-link" href="../offerings">← Danh sách lớp học phần</a>
<div class="page-header">
  <div class="page-header-info"><h1><%=title%></h1></div>
</div>

<div class="card form-card">
  <form method="post" class="subject-form" action="<%=create?"new":"edit"%>">
    <input type="hidden" name="csrf" value="<%=Html.escape(session.getAttribute("csrf"))%>">
    <% if(create){ %>
      <div class="form-grid-2">
        <label>Môn học *
          <select name="subject" required>
            <option value="">Chọn môn</option>
            <% for(var o:c.subjects()){ %><option value="<%=o.id()%>"><%=Html.escape(o.label())%></option><% } %>
          </select>
        </label>
        <label>Lớp hành chính *
          <select name="schoolClass" required>
            <option value="">Chọn lớp</option>
            <% for(var o:c.classes()){ %><option value="<%=o.id()%>"><%=Html.escape(o.label())%></option><% } %>
          </select>
        </label>
      </div>
      <div class="form-grid-2">
        <label>Năm học *<input required maxlength="9" name="year" placeholder="2026-2027"></label>
        <label>Học kỳ *<input required maxlength="20" name="semester" placeholder="1"></label>
      </div>
    <% } else { %>
      <p class="alert alert-info"><%=Html.escape(r.subject())%> · <%=Html.escape(r.schoolClass())%></p>
      <input type="hidden" name="id" value="<%=r.id()%>">
      <input type="hidden" name="version" value="<%=r.version()%>">
    <% } %>

    <label>Giảng viên *
      <select name="teacher" required>
        <option value="">Chọn giảng viên</option>
        <% for(var o:c.teachers()){ %><option value="<%=o.id()%>" <%=!create && o.id()==r.teacherId()?"selected":""%>><%=Html.escape(o.label())%></option><% } %>
      </select>
    </label>

    <div class="form-actions">
      <button class="btn-primary">Lưu</button>
      <a class="btn" href="../offerings">Hủy</a>
    </div>
  </form>
</div>

<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
