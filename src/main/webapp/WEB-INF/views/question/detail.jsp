<%@ page contentType="text/html; charset=UTF-8" %><%@ include file="common.jspf" %>
<%
  var detail=(Detail)request.getAttribute("detail");
  var row=detail.row();
  boolean draft="DRAFT".equals(row.get("status"));
  request.setAttribute("pageCrumb", "Chi tiết " + title.toLowerCase(java.util.Locale.ROOT));
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title><%=title%> #<%=h(row.get("id"))%> | Online Exam</title>
  <link rel="stylesheet" href="<%=root%>/assets/css/app.css">
  <link rel="stylesheet" href="<%=root%>/assets/css/subjects.css">
</head>
<body>
<%@ include file="../layout/app-shell-start.jspf" %>

<a class="back-link" href="<%=base%>">← Danh sách <%=title.toLowerCase(java.util.Locale.ROOT)%></a>
<div class="page-header">
  <div class="page-header-info">
    <h1><%=title%> #<%=h(row.get("id"))%></h1>
    <div class="inline-meta">
      <span class="badge badge-purple"><%=h(row.get("subject_code"))%></span>
      <% if(banks){ %><span class="<%=uiBadge(row.get("visibility"))%>"><%=h(row.get("visibility"))%></span><% } %>
      <% if(questions){ %>
        <span class="<%=uiBadge(row.get("status"))%>"><%=h(row.get("status"))%></span>
        <span class="<%=uiBadge(row.get("difficulty"))%>"><%=h(row.get("difficulty"))%></span>
        <span class="badge badge-neutral"><%=h(row.get("question_type"))%></span>
      <% } %>
    </div>
  </div>
  <div class="page-actions">
    <% if(manage && (!questions || draft)){ %><a class="btn btn-primary" href="<%=base%>/edit?id=<%=h(row.get("id"))%>">Chỉnh sửa</a><% } %>
    <% if(questions && manage && !"ARCHIVED".equals(row.get("status"))){ %>
    <form method="post" style="display:flex;flex-wrap:wrap;gap:.5rem;margin:0">
      <input type="hidden" name="csrf" value="<%=h(session.getAttribute("csrf"))%>">
      <input type="hidden" name="id" value="<%=h(row.get("id"))%>">
      <input type="hidden" name="stamp" value="<%=h(row.get("stamp"))%>">
      <% if(draft){ %><button class="btn-success" formaction="<%=base%>/publish">Công bố câu hỏi</button><% } %>
      <button class="btn-danger" formaction="<%=base%>/archive">Lưu trữ câu hỏi</button>
    </form>
    <% } %>
  </div>
</div>

<% if(request.getAttribute("notice")!=null){ %><p role="status"><%=h(request.getAttribute("notice"))%></p><% } %>

<div class="card">
  <div class="card-header">
    <h2 class="card-title">Thông tin chi tiết</h2>
  </div>
  <p class="description" style="font-size:1rem;font-weight:500"><%=h(row.get("label"))%></p>

  <dl class="detail-grid" style="margin-top:1rem">
    <div class="detail-item"><dt>Môn học</dt><dd><%=h(row.get("subject_code"))%></dd></div>
    <% if(topics){ %>
      <div class="detail-item"><dt>Mã chủ đề</dt><dd><%=h(row.get("topic_code"))%></dd></div>
      <div class="detail-item"><dt>Thứ tự</dt><dd><%=h(row.get("display_order"))%></dd></div>
      <div class="detail-item"><dt>Chủ đề cha</dt><dd><%=h(row.get("parent_topic_id"))%></dd></div>
    <% } %>
    <% if(banks){ %>
      <div class="detail-item"><dt>Hiển thị</dt><dd><span class="<%=uiBadge(row.get("visibility"))%>"><%=h(row.get("visibility"))%></span></dd></div>
      <div class="detail-item"><dt>Số câu hỏi</dt><dd><a href="<%=base.substring(0,base.lastIndexOf('/'))%>/questions?bank=<%=h(row.get("id"))%>"><%=h(row.get("question_count"))%> câu hỏi</a></dd></div>
      <div class="detail-item" style="grid-column:1 / -1"><dt>Mô tả</dt><dd class="description"><%=h(row.get("description"))%></dd></div>
    <% } %>
    <% if(questions){ %>
      <div class="detail-item"><dt>Ngân hàng</dt><dd><a href="<%=base.substring(0,base.lastIndexOf('/'))%>/banks/detail?id=<%=h(row.get("bank_id"))%>"><%=h(row.get("bank_name"))%></a></dd></div>
      <% if(manage){ %>
        <div class="detail-item"><dt>Phiên bản</dt><dd>v<%=h(row.get("version_no"))%></dd></div>
        <div class="detail-item"><dt>Điểm mặc định</dt><dd><%=h(row.get("default_points"))%> điểm</dd></div>
      <% } %>
    <% } %>
  </dl>
</div>

<% if(questions && manage){ %>
<div class="card">
  <div class="card-header">
    <h2 class="card-title">Danh sách phương án</h2>
    <span class="badge badge-purple">Phiên bản <%=h(row.get("version_no"))%> · <%=h(row.get("default_points"))%> điểm</span>
  </div>
  <ol style="margin:0;padding-left:1.3rem;display:grid;gap:.55rem">
  <% for(var a:detail.options()){ %>
    <li class="description" style="padding:.55rem .75rem;border-radius:8px;border:1px solid <%=a.correct()?"#B4E1A6":"var(--border-subtle)"%>;background:<%=a.correct()?"var(--green-bg)":"var(--bg-subtle)"%>">
      <%=h(a.text())%><%=a.correct()?" (đúng)":""%>
    </li>
  <% } %>
  </ol>
  <% if(row.get("explanation")!=null && !row.get("explanation").toString().isBlank()){ %>
    <div style="margin-top:1rem;padding-top:.85rem;border-top:1px solid var(--border-subtle)">
      <div style="font-size:.78rem;font-weight:700;text-transform:uppercase;color:var(--text-muted);margin-bottom:.3rem">Giải thích</div>
      <p class="description" style="margin:0"><%=h(row.get("explanation"))%></p>
    </div>
  <% } %>
</div>
<% } %>

<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
