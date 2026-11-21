<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.ClassDirectory,vn.edu.onlineexam.model.ClassDirectory.Member,vn.edu.onlineexam.model.bean.academic.SchoolClass,vn.edu.onlineexam.util.Html" %>
<%
  var k=(SchoolClass)request.getAttribute("schoolClass");
  var d=(ClassDirectory<?>)request.getAttribute("result");
  var q=d.query();
  boolean admin="ADMIN".equals(request.getAttribute("roleLabel")), active=k.getStatus().equals("ACTIVE");
  request.setAttribute("pageCrumb", "Chi tiết lớp hành chính");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Thành viên lớp | Online Exam</title>
  <link rel="stylesheet" href="../../assets/css/app.css">
  <link rel="stylesheet" href="../../assets/css/subjects.css">
</head>
<body>
<%@ include file="../layout/app-shell-start.jspf" %>

<a class="back-link" href="../classes">← Danh sách lớp</a>
<div class="page-header">
  <div class="page-header-info">
    <h1><%=Html.escape(k.getClassName())%></h1>
    <div class="inline-meta">
      <span class="badge badge-purple"><%=Html.escape(k.getClassCode())%></span>
      <span><%=Html.escape(k.getAcademicYear())%></span>
      <span class="<%=uiBadge(k.getStatus())%>"><%=Html.escape(k.getStatus())%></span>
    </div>
  </div>
  <% if(admin){ %>
  <div class="page-actions">
    <a class="btn btn-primary" href="edit?id=<%=k.getId()%>">Sửa lớp</a>
    <% if(!k.getStatus().equals("ARCHIVED")){ %>
    <details class="confirm-box">
      <summary>Lưu trữ lớp</summary>
      <div class="confirm-panel">
        <form method="post" action="archive">
          <%@ include file="class-token.jspf" %>
          <p style="font-size:.85rem">Xác nhận lưu trữ lớp hành chính này?</p>
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
  <div class="card-header">
    <h2 class="card-title">Thành viên lớp</h2>
  </div>

  <% if(admin && active){ %>
  <form class="filters" method="post" action="membership" style="margin-bottom:1rem;padding-bottom:1rem;border-bottom:1px solid var(--border-subtle)">
    <%@ include file="class-token.jspf" %>
    <label>ID tài khoản sinh viên
      <input required type="number" min="1" name="student" placeholder="Nhập ID sinh viên">
    </label>
    <button class="btn-primary" name="action" value="join">+ Thêm thành viên</button>
  </form>
  <% } %>

  <%@ include file="class-filters.jspf" %>
</div>

<% if(d.rows().isEmpty()){ %>
  <div class="empty-state">Không tìm thấy thành viên.</div>
<% } else { %>
<div class="results">
  <table class="class-roster">
    <thead>
      <tr>
        <th>Sinh viên</th>
        <th>Trạng thái</th>
        <th>Gia nhập (UTC)</th>
        <th>Rời lớp (UTC)</th>
        <% if(admin){ %><th>Thao tác</th><% } %>
      </tr>
    </thead>
    <tbody>
    <% for(Object item:d.rows()){ var m=(Member)item; boolean leave=m.status().equals("ACTIVE"); %>
      <tr>
        <td><span class="cell-main"><%=Html.escape(m.name())%></span> <span class="cell-sub">· <%=Html.escape(m.code())%></span></td>
        <td><span class="<%=uiBadge(m.status())%>"><%=Html.escape(m.status())%></span></td>
        <td class="cell-sub"><%=Html.escape(m.joined())%></td>
        <td class="cell-sub"><%=Html.escape(m.left())%></td>
        <% if(admin){ %>
        <td>
          <% if(leave || active){ %>
          <form method="post" action="membership" style="margin:0">
            <%@ include file="class-token.jspf" %>
            <input type="hidden" name="student" value="<%=m.studentId()%>">
            <button class="btn btn-sm <%=leave?"btn-danger":"btn-success"%>" name="action" value="<%=leave?"leave":"rejoin"%>"><%=leave?"Đánh dấu rời lớp":"Tái gia nhập"%></button>
          </form>
          <% } %>
        </td>
        <% } %>
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
