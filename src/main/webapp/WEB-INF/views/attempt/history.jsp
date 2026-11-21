<%@ page pageEncoding="UTF-8" %><%@ include file="../exam/header.jspf" %><%@ page import="vn.edu.onlineexam.service.AttemptService.*" %>
<h1>Lịch sử lượt thi</h1><% var history=(List<Map<String,Object>>)request.getAttribute("history");boolean student="STUDENT".equals(request.getAttribute("roleLabel"));if(history.isEmpty()){ %><p>Chưa có lượt thi.</p><% } for(var a:history){ %>
<article><h2><%=escape(a.get("title"))%></h2><p>Lượt <%=a.get("attempt_no")%> · <%=escape(a.get("status"))%>
<% if(!student){ %> · Sinh viên #<%=a.get("student_id")%><% } %></p>
<% if(a.get("score")!=null){ %><p>Điểm <%=a.get("score")%> / <%=a.get("maximum")%> · <%=a.get("percentage")%>% · <%=Set.of("true","1").contains(a.get("passed").toString())?"Đạt":"Chưa đạt"%>
<% if("1".equals(a.get("chosen").toString())){ %> · Kết quả được chọn<% } %></p><% } else if(student && "IN_PROGRESS".equals(a.get("status"))){ %><a href="?action=take&amp;id=<%=a.get("id")%>">Mở lượt thi</a><% } else { %><p>Chưa có điểm được phép hiển thị.</p><% } %></article><% } %></main></body></html>
