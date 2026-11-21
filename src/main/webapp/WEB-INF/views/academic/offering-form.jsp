<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.OfferingPage.*,vn.edu.onlineexam.util.Html" %>
<% var r=(Row)request.getAttribute("row"); var c=(Choices)request.getAttribute("choices"); boolean create=r==null;
String title=create?"Tạo lớp học phần":"Phân công giảng viên"; %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title><%=title%></title><link rel="stylesheet" href="../../assets/css/app.css"><link rel="stylesheet" href="../../assets/css/subjects.css"></head><body><main>
<a href="../offerings">Danh sách lớp học phần</a><h1><%=title%></h1>
<form method="post" class="subject-form" action="<%=create?"new":"edit"%>"><input type="hidden" name="csrf" value="<%=Html.escape(session.getAttribute("csrf"))%>">
<% if(create){ %>
<label>Môn học<select name="subject" required><option value="">Chọn môn</option><% for(var o:c.subjects()){ %><option value="<%=o.id()%>"><%=Html.escape(o.label())%></option><% } %></select></label>
<label>Lớp hành chính<select name="schoolClass" required><option value="">Chọn lớp</option><% for(var o:c.classes()){ %><option value="<%=o.id()%>"><%=Html.escape(o.label())%></option><% } %></select></label>
<label>Năm học<input required maxlength="9" name="year"></label><label>Học kỳ<input required maxlength="20" name="semester"></label>
<% }else{ %><p><%=Html.escape(r.subject())%> · <%=Html.escape(r.schoolClass())%></p><input type="hidden" name="id" value="<%=r.id()%>">
<input type="hidden" name="version" value="<%=r.version()%>"><% } %>
<label>Giảng viên<select name="teacher" required><option value="">Chọn giảng viên</option><% for(var o:c.teachers()){ %>
<option value="<%=o.id()%>" <%=!create && o.id()==r.teacherId()?"selected":""%>><%=Html.escape(o.label())%></option><% } %></select></label>
<button>Lưu</button></form></main></body></html>
