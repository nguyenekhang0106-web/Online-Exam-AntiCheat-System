<%@ page pageEncoding="UTF-8" %>
<% request.setAttribute("pageCrumb", "Lịch sử lượt thi"); %>
<%@ include file="../exam/header.jspf" %>
<%@ page import="vn.edu.onlineexam.service.AttemptService.*" %>

<div class="page-header">
  <div class="page-header-info">
    <h1>Lịch sử lượt thi</h1>
    <p class="page-subtitle">Theo dõi trạng thái các lượt làm bài và điểm số đã công bố.</p>
  </div>
  <% if("STUDENT".equals(request.getAttribute("roleLabel"))){ %>
  <div class="page-actions">
    <a class="btn btn-primary" href="schedule">Xem lịch thi</a>
  </div>
  <% } %>
</div>

<%
  var history=(List<Map<String,Object>>)request.getAttribute("history");
  boolean student="STUDENT".equals(request.getAttribute("roleLabel"));
  if(history.isEmpty()){ %>
  <div class="empty-state">Chưa có lượt thi.</div>
<% } else { %>
<div class="attempt-list">
<% for(var a:history){ %>
  <article>
    <div class="task-card-tags">
      <span class="<%=uiBadge(a.get("status"))%>"><%=escape(a.get("status"))%></span>
      <% if(a.get("score")!=null){
         boolean passed=Set.of("true","1").contains(a.get("passed").toString()); %>
        <span class="<%=passed?"badge badge-green":"badge badge-red"%>"><%=passed?"Đạt":"Chưa đạt"%></span>
        <% if("1".equals(a.get("chosen").toString())){ %><span class="badge badge-purple">Kết quả được chọn</span><% } %>
      <% } %>
    </div>
    <h2 style="font-size:1.05rem;margin-bottom:.35rem"><%=escape(a.get("title"))%></h2>
    <p class="cell-sub" style="margin-bottom:.65rem">
      Lượt <%=a.get("attempt_no")%> · <%=escape(a.get("status"))%><% if(!student){ %> · Sinh viên #<%=a.get("student_id")%><% } %>
    </p>
    <% if(a.get("score")!=null){ %>
      <p style="font-size:.95rem;font-weight:600;margin-bottom:0">
        Điểm <%=a.get("score")%> / <%=a.get("maximum")%> · <%=a.get("percentage")%>% · <%=Set.of("true","1").contains(a.get("passed").toString())?"Đạt":"Chưa đạt"%><% if("1".equals(a.get("chosen").toString())){ %> · Kết quả được chọn<% } %>
      </p>
    <% } else if(student && "IN_PROGRESS".equals(a.get("status"))){ %>
      <a class="btn btn-primary btn-sm" href="?action=take&amp;id=<%=a.get("id")%>">Mở lượt thi</a>
    <% } else { %>
      <p class="cell-sub" style="margin-bottom:0">Chưa có điểm được phép hiển thị.</p>
    <% } %>
  </article>
<% } %>
</div>
<% } %>

<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
