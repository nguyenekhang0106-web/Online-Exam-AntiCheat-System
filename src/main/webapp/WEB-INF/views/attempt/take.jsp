<%@ page pageEncoding="UTF-8" %><%@ include file="../exam/header.jspf" %><%@ page import="vn.edu.onlineexam.service.AttemptService.*" %>
<% var p=(Paper)request.getAttribute("paper"); %><h1><%=escape(p.title())%></h1>
<% if("IN_PROGRESS".equals(p.state())){ %>
<p>Còn <time id="remaining" data-seconds="<%=p.seconds()%>"><%=p.seconds()%> giây</time></p>
<nav aria-label="Đi tới câu hỏi"><% for(var item:p.items()){ %><a href="#q<%=item.question().get("id")%>">Câu <%=item.question().get("display_order")%></a> <% } %></nav>
<% for(var item:p.items()){var q=item.question();boolean answered=item.options().stream().anyMatch(o->"1".equals(o.get("selected").toString())); %>
<form class="answer" id="q<%=q.get("id")%>" method="post">
<input type="hidden" name="action" value="save"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="<%=p.id()%>"><input type="hidden" name="question" value="<%=q.get("id")%>"><input type="hidden" name="revision" value="<%=q.get("revision")%>">
<fieldset><legend>Câu <%=q.get("display_order")%> · <%=q.get("points_snapshot")%> điểm</legend><p style="white-space:pre-wrap;overflow-wrap:anywhere"><%=escape(q.get("content"))%></p>
<% for(var o:item.options()){ %><div><label style="overflow-wrap:anywhere"><input type="<%="SINGLE_CHOICE".equals(q.get("question_type"))?"radio":"checkbox"%>" name="option" value="<%=o.get("id")%>" <%="1".equals(o.get("selected").toString())?"checked":""%>> <%=escape(o.get("option_content"))%></label></div><% } %>
<button>Lưu câu trả lời</button><output aria-live="polite"><%=answered?"Đã trả lời":"Chưa trả lời"%></output></fieldset></form><% } %>
<a id="confirm" href="?action=confirm&amp;id=<%=p.id()%>">Kiểm tra và nộp bài</a>
<form id="timeout" method="post"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="<%=p.id()%>"><input type="hidden" name="action" value="timeout"></form>
<script src="../assets/js/attempt.js" defer></script>
<% } else if("EXPIRED".equals(p.state())){ %><p>Đã hết giờ. Các câu trả lời đã lưu được dùng để chấm.</p><form method="post" id="expired"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="<%=p.id()%>"><button name="action" value="timeout">Hoàn tất lượt hết giờ</button></form><script>document.querySelector('#expired button').click()</script>
<% } else { %><p>Lượt thi đã kết thúc.</p><a href="attempts">Xem kết quả</a><% } %></main></body></html>
