<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.EnrollmentPage.Row,vn.edu.onlineexam.util.Html" %>
<%
  var r=(Row)request.getAttribute("row");
  request.setAttribute("pageCrumb", "Chi tiết ghi danh");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Chi tiết ghi danh | Online Exam</title>
  <link rel="stylesheet" href="../../assets/css/app.css">
  <link rel="stylesheet" href="../../assets/css/subjects.css">
</head>
<body>
<%@ include file="../layout/app-shell-start.jspf" %>

<a class="back-link" href="../enrollments">← Danh sách ghi danh</a>
<div class="page-header">
  <div class="page-header-info">
    <h1>Chi tiết ghi danh</h1>
    <div class="inline-meta">
      <span class="<%=uiBadge(r.status())%>"><%=Html.escape(r.status())%></span>
    </div>
  </div>
</div>

<% if(request.getAttribute("notice")!=null){ %><p role="status"><%=Html.escape(request.getAttribute("notice"))%></p><% } %>

<div class="card">
  <dl class="detail-grid">
    <div class="detail-item"><dt>Sinh viên</dt><dd><%=Html.escape(r.name())%> · <%=Html.escape(r.code())%></dd></div>
    <div class="detail-item"><dt>Lớp học phần</dt><dd>#<%=r.offering()%> · <%=Html.escape(r.subject())%> · <%=Html.escape(r.schoolClass())%></dd></div>
    <div class="detail-item"><dt>Năm học / học kỳ</dt><dd><%=Html.escape(r.year())%> / <%=Html.escape(r.semester())%></dd></div>
    <div class="detail-item"><dt>Trạng thái</dt><dd><span class="<%=uiBadge(r.status())%>"><%=Html.escape(r.status())%></span></dd></div>
    <div class="detail-item"><dt>Ngày ghi danh (UTC)</dt><dd><%=Html.escape(r.enrolledAt())%></dd></div>
  </dl>

  <% if("ADMIN".equals(request.getAttribute("roleLabel")) && r.status().equals("ENROLLED")){ %>
  <form method="post" class="form-actions" style="margin-top:1.15rem;padding-top:1rem;border-top:1px solid var(--border-subtle)">
    <input type="hidden" name="csrf" value="<%=Html.escape(session.getAttribute("csrf"))%>">
    <input type="hidden" name="offering" value="<%=r.offering()%>">
    <input type="hidden" name="student" value="<%=r.student()%>">
    <button class="btn-danger" formaction="withdraw">Rút khỏi lớp</button>
    <button class="btn-success" formaction="complete">Đánh dấu hoàn thành</button>
  </form>
  <% } %>
</div>

<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
