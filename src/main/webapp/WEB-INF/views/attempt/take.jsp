<%@ page pageEncoding="UTF-8" %>
<% request.setAttribute("pageCrumb", "Làm bài thi"); %>
<%@ include file="../exam/header.jspf" %>
<%@ page import="vn.edu.onlineexam.service.AttemptService.*" %>
<% var p=(Paper)request.getAttribute("paper"); %>

<% if("IN_PROGRESS".equals(p.state())){ %>
<div class="exam-bar">
  <div style="min-width:0">
    <h1 style="font-size:1.3rem;margin:0"><%=escape(p.title())%></h1>
    <span class="cell-sub"><%=p.items().size()%> câu hỏi · Tự động lưu khi chọn đáp án</span>
  </div>
  <div class="page-actions">
    <div class="timer-pill">
      <span>Còn</span>
      <time id="remaining" data-seconds="<%=p.seconds()%>"><%=p.seconds()%> giây</time>
    </div>
    <a class="btn btn-primary" id="confirm" href="?action=confirm&amp;id=<%=p.id()%>">Kiểm tra và nộp bài</a>
  </div>
</div>

<nav class="q-nav" aria-label="Đi tới câu hỏi">
  <% for(var item:p.items()){
     boolean navAnswered=item.options().stream().anyMatch(o->"1".equals(o.get("selected").toString())); %>
    <a class="q-pill <%=navAnswered?"is-answered":""%>" href="#q<%=item.question().get("id")%>">Câu <%=item.question().get("display_order")%></a>
  <% } %>
</nav>

<% for(var item:p.items()){
   var q=item.question();
   boolean answered=item.options().stream().anyMatch(o->"1".equals(o.get("selected").toString())); %>
<form class="answer" id="q<%=q.get("id")%>" method="post">
  <input type="hidden" name="action" value="save">
  <input type="hidden" name="csrf" value="${sessionScope.csrf}">
  <input type="hidden" name="id" value="<%=p.id()%>">
  <input type="hidden" name="question" value="<%=q.get("id")%>">
  <input type="hidden" name="revision" value="<%=q.get("revision")%>">
  <fieldset>
    <legend>Câu <%=q.get("display_order")%> · <%=q.get("points_snapshot")%> điểm</legend>
    <p class="question-stem"><%=escape(q.get("content"))%></p>
    <% for(var o:item.options()){ %>
    <div class="option-item">
      <label class="option-card">
        <input type="<%="SINGLE_CHOICE".equals(q.get("question_type"))?"radio":"checkbox"%>" name="option" value="<%=o.get("id")%>" <%="1".equals(o.get("selected").toString())?"checked":""%>>
        <span><%=escape(o.get("option_content"))%></span>
      </label>
    </div>
    <% } %>
    <div class="answer-foot">
      <button class="btn btn-sm">Lưu câu trả lời</button>
      <output aria-live="polite"><%=answered?"Đã trả lời":"Chưa trả lời"%></output>
    </div>
  </fieldset>
</form>
<% } %>

<form id="timeout" method="post" style="display:none">
  <input type="hidden" name="csrf" value="${sessionScope.csrf}">
  <input type="hidden" name="id" value="<%=p.id()%>">
  <input type="hidden" name="action" value="timeout">
</form>
<script src="../assets/js/attempt.js" defer></script>

<% } else if("EXPIRED".equals(p.state())){ %>
<div class="card form-card">
  <h1><%=escape(p.title())%></h1>
  <p class="alert alert-danger">Đã hết giờ. Các câu trả lời đã lưu được dùng để chấm.</p>
  <form method="post" id="expired">
    <input type="hidden" name="csrf" value="${sessionScope.csrf}">
    <input type="hidden" name="id" value="<%=p.id()%>">
    <button class="btn-primary" name="action" value="timeout">Hoàn tất lượt hết giờ</button>
  </form>
  <script>document.querySelector('#expired button').click()</script>
</div>
<% } else { %>
<div class="card form-card">
  <h1><%=escape(p.title())%></h1>
  <p class="alert alert-info">Lượt thi đã kết thúc.</p>
  <a class="btn btn-primary" href="attempts">Xem kết quả</a>
</div>
<% } %>

<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
