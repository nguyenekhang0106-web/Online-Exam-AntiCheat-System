<%@ page pageEncoding="UTF-8" %>
<% request.setAttribute("pageCrumb", "Xác nhận nộp bài"); %>
<%@ include file="../exam/header.jspf" %>
<%@ page import="vn.edu.onlineexam.service.AttemptService.*" %>
<% var p=(Paper)request.getAttribute("paper"); %>

<div class="card form-card">
  <div class="task-card-tags"><span class="badge badge-pink">Hoàn tất bài thi</span></div>
  <h1>Xác nhận nộp bài</h1>
  <p style="font-size:1rem;font-weight:600;color:var(--text-primary)"><%=escape(p.title())%></p>

  <% if(Set.of("IN_PROGRESS","EXPIRED").contains(p.state())){ %>
    <p class="alert alert-info">Hệ thống sẽ chấm các lựa chọn đã lưu. Sau khi nộp, bạn không thể sửa bài.</p>
    <div class="form-actions">
      <form method="post" style="margin:0">
        <input type="hidden" name="csrf" value="${sessionScope.csrf}">
        <input type="hidden" name="id" value="<%=p.id()%>">
        <button class="btn-primary" name="action" value="submit">Xác nhận nộp bài</button>
      </form>
      <a class="btn" href="?action=take&amp;id=<%=p.id()%>">Quay lại lượt thi</a>
    </div>
  <% } else { %>
    <a class="btn btn-primary" href="attempts">Xem kết quả lượt thi</a>
  <% } %>
</div>

<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
