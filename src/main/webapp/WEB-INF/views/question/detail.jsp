<%@ page contentType="text/html; charset=UTF-8" %><%@ include file="common.jspf" %>
<% var detail=(Detail)request.getAttribute("detail");var row=detail.row();boolean draft="DRAFT".equals(row.get("status")); %>
<!DOCTYPE html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title><%=title%></title><link rel="stylesheet" href="<%=root%>/assets/css/app.css"><link rel="stylesheet" href="<%=root%>/assets/css/subjects.css"></head><body><main>
<a href="<%=base%>">Danh sách <%=title.toLowerCase(java.util.Locale.ROOT)%></a><h1><%=title%> #<%=h(row.get("id"))%></h1>
<% if(request.getAttribute("notice")!=null){ %><p role="status"><%=h(request.getAttribute("notice"))%></p><% } %>
<p class="description"><%=h(row.get("label"))%></p><p>Môn học: <%=h(row.get("subject_code"))%></p>
<% if(topics){ %><p>Mã: <%=h(row.get("topic_code"))%> · Thứ tự: <%=h(row.get("display_order"))%> · Chủ đề cha: <%=h(row.get("parent_topic_id"))%></p><% } %>
<% if(banks){ %><p class="description"><%=h(row.get("description"))%></p><p><%=h(row.get("visibility"))%> · <a href="<%=base.substring(0,base.lastIndexOf('/'))%>/questions?bank=<%=h(row.get("id"))%>"><%=h(row.get("question_count"))%> câu hỏi</a></p><% } %>
<% if(questions){ %><p><a href="<%=base.substring(0,base.lastIndexOf('/'))%>/banks/detail?id=<%=h(row.get("bank_id"))%>"><%=h(row.get("bank_name"))%></a> · <%=h(row.get("question_type"))%> · <%=h(row.get("difficulty"))%> · <%=h(row.get("status"))%></p>
<% if(manage){ %><p>Phiên bản <%=h(row.get("version_no"))%> · <%=h(row.get("default_points"))%> điểm</p><ol><% for(var a:detail.options()){ %><li class="description"><%=h(a.text())%><%=a.correct()?" (đúng)":""%></li><% } %></ol><p class="description"><%=h(row.get("explanation"))%></p><% }} %>
<% if(manage && (!questions || draft)){ %><p><a href="<%=base%>/edit?id=<%=h(row.get("id"))%>">Chỉnh sửa</a></p><% } %>
<% if(questions && manage && !"ARCHIVED".equals(row.get("status"))){ %><form method="post"><input type="hidden" name="csrf" value="<%=h(session.getAttribute("csrf"))%>"><input type="hidden" name="id" value="<%=h(row.get("id"))%>"><input type="hidden" name="stamp" value="<%=h(row.get("stamp"))%>">
<% if(draft){ %><button formaction="<%=base%>/publish">Công bố câu hỏi</button><% } %> <button formaction="<%=base%>/archive">Lưu trữ câu hỏi</button></form><% } %></main></body></html>
