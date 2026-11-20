<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.SubjectPage.Input,vn.edu.onlineexam.util.Html" %>
<%! String field(String s){return s==null?"":Html.escape(s);} %>
<% boolean create="/admin/subjects/new".equals(request.getAttribute("jakarta.servlet.forward.servlet_path"));
   Input in=(Input)request.getAttribute("input"); if(in==null) in=new Input("","","");
   String title=create?"Thêm môn học":"Sửa môn học"; %>
<!DOCTYPE html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title><%=title%></title><link rel="stylesheet" href="../../assets/css/app.css"><link rel="stylesheet" href="../../assets/css/subjects.css"></head>
<body><main><a href="../subjects">Danh sách môn học</a><h1><%=title%></h1>
<% if(request.getAttribute("error")!=null){ %><p role="alert"><%=Html.escape(request.getAttribute("error"))%></p><% } %>
<form class="subject-form" method="post" action="<%=create?"new":"edit"%>">
<input type="hidden" name="csrf" value="<%=Html.escape(session.getAttribute("csrf"))%>">
<% if(!create){ %><input type="hidden" name="id" value="<%=Html.escape(request.getParameter("id"))%>"><input type="hidden" name="version" value="<%=Html.escape(request.getAttribute("version"))%>"><% } %>
<label>Mã môn<input required name="code" maxlength="30" value="<%=field(in.code())%>"></label>
<label>Tên môn<input required name="name" maxlength="200" value="<%=field(in.name())%>"></label>
<label>Mô tả<textarea name="description" rows="5"><%=field(in.description())%></textarea></label><button>Lưu môn học</button></form></main></body></html>
