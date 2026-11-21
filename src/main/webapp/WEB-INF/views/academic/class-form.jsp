<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.bean.academic.SchoolClass,vn.edu.onlineexam.util.Html" %>
<% var k=(SchoolClass)request.getAttribute("schoolClass");String title=k==null?"Thêm lớp hành chính":"Sửa lớp hành chính"; %>
<!DOCTYPE html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title><%=title%></title><link rel="stylesheet" href="../../assets/css/app.css"><link rel="stylesheet" href="../../assets/css/subjects.css"></head><body><main>
<a href="../classes">Danh sách lớp</a><h1><%=title%></h1>
<form class="subject-form" method="post" action="<%=k==null?"new":"edit"%>"><input type="hidden" name="csrf" value="<%=Html.escape(session.getAttribute("csrf"))%>">
<% if(k!=null){ %><input type="hidden" name="id" value="<%=k.getId()%>"><input type="hidden" name="version" value="<%=k.getUpdatedAt()%>"><% } %>
<label>Mã lớp<input name="code" required maxlength="30" value="<%=k==null?"":Html.escape(k.getClassCode())%>"></label>
<label>Tên lớp<input name="name" required maxlength="150" value="<%=k==null?"":Html.escape(k.getClassName())%>"></label>
<label>Năm học<input name="year" maxlength="9" value="<%=k==null || k.getAcademicYear()==null?"":Html.escape(k.getAcademicYear())%>"></label>
<button>Lưu lớp</button></form></main></body></html>
