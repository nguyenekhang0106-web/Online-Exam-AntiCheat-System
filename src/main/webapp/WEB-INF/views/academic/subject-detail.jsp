<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.bean.academic.Subject,vn.edu.onlineexam.util.Html" %>
<%
  var s=(Subject)request.getAttribute("subject");
  request.setAttribute("pageCrumb", "Chi tiết môn học");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Chi tiết môn học | Online Exam</title>
  <link rel="stylesheet" href="../../assets/css/app.css">
  <link rel="stylesheet" href="../../assets/css/subjects.css">
</head>
<body>
<%@ include file="../layout/app-shell-start.jspf" %>

<a class="back-link" href="../subjects">← Danh sách môn học</a>
<div class="page-header">
  <div class="page-header-info">
    <h1><%=Html.escape(s.getSubjectName())%></h1>
    <div class="inline-meta">
      <span class="badge badge-purple"><%=Html.escape(s.getSubjectCode())%></span>
      <span class="<%=uiBadge(s.getStatus())%>"><%=Html.escape(s.getStatus())%></span>
    </div>
  </div>
  <% if("ADMIN".equals(request.getAttribute("roleLabel"))){ %>
  <div class="page-actions">
    <a class="btn btn-primary" href="edit?id=<%=s.getId()%>">Sửa môn học</a>
    <% if(!"ARCHIVED".equals(s.getStatus())){ %>
    <details class="confirm-box">
      <summary>Lưu trữ môn học</summary>
      <div class="confirm-panel">
        <form method="post" action="archive">
          <input type="hidden" name="csrf" value="<%=Html.escape(session.getAttribute("csrf"))%>">
          <input type="hidden" name="id" value="<%=s.getId()%>">
          <input type="hidden" name="version" value="<%=s.getUpdatedAt()%>">
          <p style="font-size:.85rem">Xác nhận chuyển môn học sang trạng thái lưu trữ?</p>
          <button class="btn btn-sm btn-danger">Xác nhận lưu trữ</button>
        </form>
      </div>
    </details>
    <% } %>
  </div>
  <% } %>
</div>

<% if(request.getAttribute("notice")!=null){ %><p role="status"><%=Html.escape(request.getAttribute("notice"))%></p><% } %>

<div class="card">
  <dl class="detail-grid">
    <div class="detail-item"><dt>Mã môn</dt><dd><%=Html.escape(s.getSubjectCode())%></dd></div>
    <div class="detail-item"><dt>Trạng thái</dt><dd><span class="<%=uiBadge(s.getStatus())%>"><%=Html.escape(s.getStatus())%></span></dd></div>
    <div class="detail-item" style="grid-column:1 / -1"><dt>Mô tả</dt><dd class="description"><%=Html.escape(s.getDescription())%></dd></div>
  </dl>
</div>

<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
