<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.ClassDirectory,vn.edu.onlineexam.model.bean.academic.SchoolClass,vn.edu.onlineexam.util.Html" %>
<%
  var d=(ClassDirectory<?>)request.getAttribute("result");
  var q=d.query();
  SchoolClass k=null;
  request.setAttribute("pageCrumb", "Lớp hành chính");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Lớp hành chính | Online Exam</title>
  <link rel="stylesheet" href="../assets/css/app.css">
  <link rel="stylesheet" href="../assets/css/subjects.css">
</head>
<body>
<%@ include file="../layout/app-shell-start.jspf" %>

<div class="page-header">
  <div class="page-header-info">
    <h1>Lớp hành chính</h1>
    <p class="page-subtitle">Tra cứu lớp hành chính và quản lý danh sách thành viên.</p>
  </div>
  <% if("ADMIN".equals(request.getAttribute("roleLabel"))){ %>
  <div class="page-actions">
    <a class="btn btn-primary" href="classes/new">+ Thêm lớp</a>
  </div>
  <% } %>
</div>

<div class="filter-card">
  <%@ include file="class-filters.jspf" %>
</div>

<% if(d.rows().isEmpty()){ %>
  <div class="empty-state">Không tìm thấy lớp.</div>
<% } else { %>
<div class="results">
  <table>
    <thead>
      <tr>
        <th>Mã lớp</th>
        <th>Tên lớp</th>
        <th>Năm học</th>
        <th>Trạng thái</th>
      </tr>
    </thead>
    <tbody>
    <% for(Object item:d.rows()){ var row=(SchoolClass)item; %>
      <tr>
        <td><strong><%=Html.escape(row.getClassCode())%></strong></td>
        <td class="cell-main"><a href="classes/detail?id=<%=row.getId()%>"><%=Html.escape(row.getClassName())%></a></td>
        <td><%=Html.escape(row.getAcademicYear())%></td>
        <td><span class="<%=uiBadge(row.getStatus())%>"><%=Html.escape(row.getStatus())%></span></td>
      </tr>
    <% } %>
    </tbody>
  </table>
</div>
<% } %>

<%@ include file="class-paging.jspf" %>
<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
