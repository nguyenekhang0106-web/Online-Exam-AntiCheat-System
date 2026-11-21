<%@ page pageEncoding="UTF-8" %>
<%@ include file="header.jspf" %><h1>Đề thi</h1>
<form><label>Tìm theo tên <input name="q" value="<%=escape(Objects.toString(request.getParameter("q"),""))%>"></label>
<label>Trạng thái <select name="status"><% for(String state:List.of("","DRAFT","PUBLISHED","CLOSED","ARCHIVED")){ %><option value="<%=state%>" <%=state.equals(request.getParameter("status"))?"selected":""%>><%=state.isEmpty()?"Tất cả":state%></option><% } %></select></label>
<label>Sắp xếp <select name="sort"><option value="latest">Mới nhất</option><option value="title" <%="title".equals(request.getParameter("sort"))?"selected":""%>>Tên đề</option></select></label>
<% int pg=(int)request.getAttribute("pageNumber"); var rows=(List<Map<String,Object>>)request.getAttribute("rows"); %>
<button name="page" value="1">Tìm</button><% if(pg>1){ %><button name="page" value="<%=pg-1%>">Trang trước</button><% } if(rows.size()>20){ %><button name="page" value="<%=pg+1%>">Trang sau</button><% } %></form>
<% if(rows.isEmpty()){ %><p>Không tìm thấy đề thi.</p><% } for(var row:rows.stream().limit(20).toList()){ %><article><a href="?id=<%=row.get("id")%>"><%=escape(row.get("title"))%></a> · <%=escape(row.get("subject_name"))%> · <%=escape(row.get("status"))%> · <%=row.get("max_attempts")%> lượt</article><% } %>
<% if(request.getAttribute("subjects")!=null){ Map<String,Object> e=Map.of(); %><h2>Tạo đề nháp</h2><form method="post"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><label>Môn học <select name="subject" required><% for(var subject:(List<Map<String,Object>>)request.getAttribute("subjects")){ %><option value="<%=subject.get("id")%>"><%=escape(subject.get("label"))%></option><% } %></select></label>
<%@ include file="config.jspf" %><button name="action" value="create">Tạo đề</button></form><% } %></main></body></html>
