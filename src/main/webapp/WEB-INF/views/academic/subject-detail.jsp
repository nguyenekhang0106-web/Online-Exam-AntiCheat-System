<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.bean.academic.Subject,vn.edu.onlineexam.util.Html" %>
<% var s=(Subject)request.getAttribute("subject"); %>
<!DOCTYPE html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Chi tiết môn học</title><link rel="stylesheet" href="../../assets/css/app.css"><link rel="stylesheet" href="../../assets/css/subjects.css"></head>
<body><main><a href="../subjects">Danh sách môn học</a><h1><%=Html.escape(s.getSubjectName())%></h1>
<% if(request.getAttribute("notice")!=null){ %><p role="status"><%=Html.escape(request.getAttribute("notice"))%></p><% } %>
<dl><dt>Mã môn</dt><dd><%=Html.escape(s.getSubjectCode())%></dd><dt>Trạng thái</dt><dd><%=Html.escape(s.getStatus())%></dd>
<dt>Mô tả</dt><dd class="description"><%=Html.escape(s.getDescription())%></dd></dl>
<% if("ADMIN".equals(request.getAttribute("roleLabel"))){ %><p><a href="edit?id=<%=s.getId()%>">Sửa môn học</a></p>
<% if(!"ARCHIVED".equals(s.getStatus())){ %><details><summary>Lưu trữ môn học</summary><form method="post" action="archive">
<input type="hidden" name="csrf" value="<%=Html.escape(session.getAttribute("csrf"))%>"><input type="hidden" name="id" value="<%=s.getId()%>">
<input type="hidden" name="version" value="<%=s.getUpdatedAt()%>"><button>Xác nhận lưu trữ</button></form></details><% }} %>
</main></body></html>
