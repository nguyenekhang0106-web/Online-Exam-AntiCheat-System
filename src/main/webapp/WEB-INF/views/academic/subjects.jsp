<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.SubjectPage,vn.edu.onlineexam.util.Html" %>
<%
  var d=(SubjectPage)request.getAttribute("directory");
  var q=d.query();
  request.setAttribute("pageCrumb", "Môn học");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Môn học | Online Exam</title>
  <link rel="stylesheet" href="../assets/css/app.css">
  <link rel="stylesheet" href="../assets/css/subjects.css">
</head>
<body>
<%@ include file="../layout/app-shell-start.jspf" %>

<div class="page-header">
  <div class="page-header-info">
    <h1>Môn học</h1>
    <p class="page-subtitle">Danh mục môn học trong phạm vi quyền truy cập của bạn.</p>
  </div>
  <% if("ADMIN".equals(request.getAttribute("roleLabel"))){ %>
  <div class="page-actions">
    <a class="btn btn-primary" href="subjects/new">+ Thêm môn học</a>
  </div>
  <% } %>
</div>

<div class="filter-card">
  <form id="filters" method="get" class="filters">
    <label>Mã hoặc tên môn
      <input name="q" maxlength="200" placeholder="Ví dụ: WEB101…" value="<%=Html.escape(q.keyword())%>">
    </label>
    <label>Trạng thái
      <select name="status">
        <option value="">Tất cả</option>
        <% for(String s:new String[]{"ACTIVE","INACTIVE","ARCHIVED"}){ %><option <%=s.equals(q.status())?"selected":""%>><%=s%></option><% } %>
      </select>
    </label>
    <label>Sắp xếp
      <select name="sort">
        <% for(String[] s:new String[][]{{"code","Mã môn"},{"name","Tên môn"},{"updated","Ngày cập nhật"}}){ %>
        <option value="<%=s[0]%>" <%=q.sort().name().equals(s[0])?"selected":""%>><%=s[1]%></option><% } %>
      </select>
    </label>
    <label>Thứ tự
      <select name="direction">
        <option value="ASC">Tăng dần</option>
        <option value="DESC" <%=q.descending()?"selected":""%>>Giảm dần</option>
      </select>
    </label>
    <label>Số môn mỗi trang
      <input name="size" type="number" min="1" max="100" value="<%=q.size()%>">
    </label>
    <button class="btn-primary">Tìm kiếm</button>
  </form>
</div>

<div class="results-meta"><span><%=d.total()%> môn học phù hợp</span></div>

<% if(!d.rows().isEmpty()){ %>
<div class="results">
  <table>
    <thead>
      <tr>
        <th>Mã môn</th>
        <th>Tên môn</th>
        <th>Trạng thái</th>
      </tr>
    </thead>
    <tbody>
    <% for(var s:d.rows()){ %>
      <tr>
        <td><strong><%=Html.escape(s.getSubjectCode())%></strong></td>
        <td class="cell-main"><a href="subjects/detail?id=<%=s.getId()%>"><%=Html.escape(s.getSubjectName())%></a></td>
        <td><span class="<%=uiBadge(s.getStatus())%>"><%=Html.escape(s.getStatus())%></span></td>
      </tr>
    <% } %>
    </tbody>
  </table>
</div>
<nav class="pagination" aria-label="Phân trang">
  <p>Trang <%=d.page()%> / <%=d.pages()%></p>
  <div class="pagination-btns">
    <% if(d.page()>1){ %><button class="btn btn-sm" form="filters" name="page" value="<%=d.page()-1%>">← Trang trước</button><% } %>
    <% if(d.page()<d.pages()){ %><button class="btn btn-sm" form="filters" name="page" value="<%=d.page()+1%>">Trang sau →</button><% } %>
  </div>
</nav>
<% } else { %>
<div class="empty-state">Không tìm thấy môn học phù hợp.</div>
<% } %>

<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
