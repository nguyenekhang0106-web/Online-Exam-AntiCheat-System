<%@ page pageEncoding="UTF-8" %>
<% request.setAttribute("pageCrumb", "Lịch thi của tôi"); %>
<%@ include file="header.jspf" %>

<div class="page-header">
  <div class="page-header-info">
    <h1>Lịch thi của tôi</h1>
    <p class="page-subtitle">Danh sách các kỳ thi đang mở hoặc sắp diễn ra dành cho bạn (giờ UTC).</p>
  </div>
  <div class="page-actions">
    <a class="btn" href="attempts">Lịch sử lượt thi</a>
  </div>
</div>

<%@ include file="calendar.jspf" %>

<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
