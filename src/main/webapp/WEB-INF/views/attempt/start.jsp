<%@ page pageEncoding="UTF-8" %>
<% request.setAttribute("pageCrumb", "Kiểm tra quyền vào thi"); %>
<%@ include file="../exam/header.jspf" %>
<%@ page import="vn.edu.onlineexam.service.AttemptService.*" %>
<% var p=(Admission)request.getAttribute("admission"); %>

<a class="back-link" href="schedule">← Quay lại lịch thi</a>

<div class="card form-card">
  <div class="task-card-tags">
    <span class="badge badge-purple">Chuẩn bị làm bài</span>
    <% if(p.open()!=0){ %><span class="badge badge-pink">Đang có lượt mở</span><% } %>
  </div>
  <h1 style="margin-bottom:.75rem"><%=escape(p.title())%></h1>

  <dl class="detail-grid" style="margin-bottom:1.15rem">
    <div class="detail-item"><dt>Lượt đã ghi nhận</dt><dd><%=p.used()%></dd></div>
    <% if(p.limit()!=null){ %>
      <div class="detail-item"><dt>Giới hạn lượt</dt><dd><%=p.limit()%></dd></div>
      <div class="detail-item"><dt>Còn có thể bắt đầu</dt><dd><%=Math.max(0,p.limit()-p.used())%></dd></div>
    <% } %>
  </dl>

  <% if(!p.message().isEmpty()){ %>
    <p class="alert alert-danger"><%=escape(p.message())%></p>
  <% } %>

  <div class="form-actions">
    <% if(p.open()!=0){ %>
      <a class="btn btn-primary" href="?action=take&amp;id=<%=p.open()%>">Mở lượt hiện tại</a>
    <% } else if(p.message().isEmpty()){ %>
      <form method="post" style="margin:0">
        <input type="hidden" name="csrf" value="${sessionScope.csrf}">
        <input type="hidden" name="exam" value="${exam}">
        <input type="hidden" name="previous" value="<%=p.previous()%>">
        <button class="btn-primary" name="action" value="start">Bắt đầu lượt thi</button>
      </form>
    <% } %>
    <a class="btn" href="schedule">Lịch thi</a>
    <a class="btn" href="attempts">Lịch sử lượt thi</a>
  </div>
</div>

<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
