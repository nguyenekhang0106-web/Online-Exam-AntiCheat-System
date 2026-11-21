<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.SubjectPage.Input,vn.edu.onlineexam.util.Html" %>
<%! String field(String s){return s==null?"":Html.escape(s);} %>
<%
  boolean create="/admin/subjects/new".equals(request.getAttribute("jakarta.servlet.forward.servlet_path"));
  Input in=(Input)request.getAttribute("input"); if(in==null) in=new Input("","","");
  String title=create?"Thêm môn học":"Sửa môn học";
  request.setAttribute("pageCrumb", title);
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title><%=title%> | Online Exam</title>
  <link rel="stylesheet" href="../../assets/css/app.css">
  <link rel="stylesheet" href="../../assets/css/subjects.css">
</head>
<body>
<%@ include file="../layout/app-shell-start.jspf" %>

<a class="back-link" href="../subjects">← Danh sách môn học</a>
<div class="page-header">
  <div class="page-header-info">
    <h1><%=title%></h1>
  </div>
</div>

<% if(request.getAttribute("error")!=null){ %><p role="alert"><%=Html.escape(request.getAttribute("error"))%></p><% } %>

<div class="card form-card">
  <form class="subject-form" method="post" action="<%=create?"new":"edit"%>">
    <input type="hidden" name="csrf" value="<%=Html.escape(session.getAttribute("csrf"))%>">
    <% if(!create){ %>
      <input type="hidden" name="id" value="<%=Html.escape(request.getParameter("id"))%>">
      <input type="hidden" name="version" value="<%=Html.escape(request.getAttribute("version"))%>">
    <% } %>
    <div class="form-grid-2">
      <label>Mã môn *
        <input required name="code" maxlength="30" placeholder="WEB101" value="<%=field(in.code())%>">
      </label>
      <label>Tên môn *
        <input required name="name" maxlength="200" placeholder="Công nghệ Web" value="<%=field(in.name())%>">
      </label>
    </div>
    <label>Mô tả
      <textarea name="description" rows="5" placeholder="Nhập mô tả môn học…"><%=field(in.description())%></textarea>
    </label>
    <div class="form-actions">
      <button class="btn-primary">Lưu môn học</button>
      <a class="btn" href="../subjects">Hủy</a>
    </div>
  </form>
</div>

<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
