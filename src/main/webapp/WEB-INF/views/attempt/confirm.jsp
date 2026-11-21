<%@ page pageEncoding="UTF-8" %><%@ include file="../exam/header.jspf" %><%@ page import="vn.edu.onlineexam.service.AttemptService.*" %>
<% var p=(Paper)request.getAttribute("paper"); %><h1>Xác nhận nộp bài</h1><p><%=escape(p.title())%></p>
<% if(Set.of("IN_PROGRESS","EXPIRED").contains(p.state())){ %><p>Hệ thống sẽ chấm các lựa chọn đã lưu. Sau khi nộp, bạn không thể sửa bài.</p>
<form method="post"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="<%=p.id()%>"><button name="action" value="submit">Xác nhận nộp bài</button></form>
<a href="?action=take&amp;id=<%=p.id()%>">Quay lại lượt thi</a><% } else { %><a href="attempts">Xem kết quả lượt thi</a><% } %></main></body></html>
