<%@ page pageEncoding="UTF-8" %><%@ include file="../exam/header.jspf" %><%@ page import="vn.edu.onlineexam.service.AttemptService.*" %>
<% var p=(Admission)request.getAttribute("admission"); %>
<h1><%=escape(p.title())%></h1><p>Lượt đã ghi nhận: <%=p.used()%>.
<% if(p.limit()!=null){ %> Giới hạn: <%=p.limit()%>. Còn có thể bắt đầu: <%=Math.max(0,p.limit()-p.used())%>.<% } %></p>
<% if(!p.message().isEmpty()){ %><p><%=escape(p.message())%></p><% } if(p.open()!=0){ %><a href="?action=take&amp;id=<%=p.open()%>">Mở lượt hiện tại</a><% } else if(p.message().isEmpty()){ %>
<form method="post"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="exam" value="${exam}"><input type="hidden" name="previous" value="<%=p.previous()%>"><button name="action" value="start">Bắt đầu lượt thi</button></form><% } %>
<p><a href="schedule">Lịch thi</a> · <a href="attempts">Lịch sử lượt thi</a></p></main></body></html>
