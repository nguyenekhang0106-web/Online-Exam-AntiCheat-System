<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.OfferingPage.Row,vn.edu.onlineexam.util.Html" %>
<%
  var r=(Row)request.getAttribute("row");
  request.setAttribute("pageCrumb", "Chi tiết lớp học phần");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Chi tiết lớp học phần | Online Exam</title>
  <link rel="stylesheet" href="../../assets/css/app.css">
  <link rel="stylesheet" href="../../assets/css/subjects.css">
</head>
<body>
<%@ include file="../layout/app-shell-start.jspf" %>

<a class="back-link" href="../offerings">← Danh sách lớp học phần</a>
<div class="page-header">
  <div class="page-header-info">
    <h1>Lớp học phần #<%=r.id()%></h1>
    <div class="inline-meta">
      <span class="badge badge-purple"><%=Html.escape(r.year())%> / <%=Html.escape(r.semester())%></span>
      <span class="<%=uiBadge(r.status())%>"><%=Html.escape(r.status())%></span>
    </div>
  </div>
  <% if("ADMIN".equals(request.getAttribute("roleLabel")) && "ACTIVE".equals(r.status())){ %>
  <div class="page-actions">
    <a class="btn btn-primary" href="edit?id=<%=r.id()%>">Phân công giảng viên</a>
    <details class="confirm-box">
      <summary>Đóng lớp học phần</summary>
      <div class="confirm-panel">
        <form method="post" action="close">
          <input type="hidden" name="csrf" value="<%=Html.escape(session.getAttribute("csrf"))%>">
          <input type="hidden" name="id" value="<%=r.id()%>">
          <input type="hidden" name="version" value="<%=r.version()%>">
          <p style="font-size:.85rem">Xác nhận đóng lớp học phần này?</p>
          <button class="btn btn-sm btn-danger">Xác nhận đóng lớp</button>
        </form>
      </div>
    </details>
  </div>
  <% } %>
</div>

<% if(request.getAttribute("notice")!=null){ %><p role="status"><%=Html.escape(request.getAttribute("notice"))%></p><% } %>

<div class="card">
  <dl class="detail-grid">
    <div class="detail-item"><dt>Môn học</dt><dd><a href="../subjects/detail?id=<%=r.subjectId()%>"><%=Html.escape(r.subject())%></a></dd></div>
    <div class="detail-item"><dt>Lớp hành chính</dt><dd><%=Html.escape(r.schoolClass())%></dd></div>
    <div class="detail-item"><dt>Giảng viên</dt><dd><%=Html.escape(r.teacher())%></dd></div>
    <div class="detail-item"><dt>Năm học / học kỳ</dt><dd><%=Html.escape(r.year())%> / <%=Html.escape(r.semester())%></dd></div>
    <div class="detail-item"><dt>Trạng thái</dt><dd><span class="<%=uiBadge(r.status())%>"><%=Html.escape(r.status())%></span></dd></div>
  </dl>
</div>

<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
