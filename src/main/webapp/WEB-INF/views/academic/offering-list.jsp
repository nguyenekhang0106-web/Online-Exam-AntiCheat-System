<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.OfferingPage,vn.edu.onlineexam.util.Html" %>
<%
  var result=(OfferingPage)request.getAttribute("result");
  var q=result.query();
  request.setAttribute("pageCrumb", "Lớp học phần");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Lớp học phần | Online Exam</title>
  <link rel="stylesheet" href="../assets/css/app.css">
  <link rel="stylesheet" href="../assets/css/subjects.css">
</head>
<body>
<%@ include file="../layout/app-shell-start.jspf" %>

<div class="page-header">
  <div class="page-header-info">
    <h1>Lớp học phần</h1>
    <p class="page-subtitle">Quản lý lớp học phần theo môn học, lớp hành chính, giảng viên và học kỳ.</p>
  </div>
  <% if("ADMIN".equals(request.getAttribute("roleLabel"))){ %>
  <div class="page-actions">
    <a class="btn btn-primary" href="offerings/new">+ Tạo lớp học phần</a>
  </div>
  <% } %>
</div>

<div class="filter-card">
  <form id="filters" class="filters" method="get">
    <label>Môn, lớp hoặc giảng viên
      <input name="q" maxlength="200" value="<%=Html.escape(q.keyword())%>">
    </label>
    <label>ID môn học <small>0 để xem tất cả</small>
      <input type="number" min="0" name="subject" value="<%=q.subjectId()%>">
    </label>
    <label>Trạng thái
      <select name="status">
        <option value="">Tất cả</option>
        <% for(String s:new String[]{"ACTIVE","CLOSED","ARCHIVED"}){ %><option <%=q.status().equals(s)?"selected":""%>><%=s%></option><% } %>
      </select>
    </label>
    <label>Sắp xếp
      <select name="sort">
        <% for(String[] s:new String[][]{{"id","ID lớp học phần"},{"subject","Môn học"},{"schoolClass","Lớp hành chính"},{"teacher","Giảng viên"},{"year","Năm học"}}){ %>
        <option value="<%=s[0]%>" <%=q.sort().name().equals(s[0])?"selected":""%>><%=s[1]%></option><% } %>
      </select>
    </label>
    <label>Thứ tự
      <select name="direction">
        <option value="ASC">Tăng</option>
        <option value="DESC" <%=q.descending()?"selected":""%>>Giảm</option>
      </select>
    </label>
    <label>Số dòng
      <input name="size" type="number" min="1" max="100" value="<%=q.size()%>">
    </label>
    <button class="btn-primary">Tìm kiếm</button>
  </form>
</div>

<div class="results-meta"><p style="margin:0"><%=result.total()%> lớp học phần phù hợp</p></div>

<% if(result.total()>0){ %>
<div class="results">
  <table>
    <thead>
      <tr>
        <th>Môn học</th>
        <th>Lớp hành chính</th>
        <th>Giảng viên</th>
        <th>Năm học / kỳ</th>
        <th>Trạng thái</th>
      </tr>
    </thead>
    <tbody>
    <% for(var r:result.rows()){ %>
      <tr>
        <td class="cell-main"><a href="offerings/detail?id=<%=r.id()%>"><%=Html.escape(r.subject())%> (#<%=r.id()%>)</a></td>
        <td><%=Html.escape(r.schoolClass())%></td>
        <td><%=Html.escape(r.teacher())%></td>
        <td><%=Html.escape(r.year())%> / <%=Html.escape(r.semester())%></td>
        <td><span class="<%=uiBadge(r.status())%>"><%=Html.escape(r.status())%></span></td>
      </tr>
    <% } %>
    </tbody>
  </table>
</div>
<nav class="pagination" aria-label="Phân trang">
  <p>Trang <%=result.page()%> / <%=result.pages()%></p>
  <div class="pagination-btns">
    <% if(result.page()>1){ %><button class="btn btn-sm" form="filters" name="page" value="<%=result.page()-1%>">← Trang trước</button><% } %>
    <% if(result.page()<result.pages()){ %><button class="btn btn-sm" form="filters" name="page" value="<%=result.page()+1%>">Trang sau →</button><% } %>
  </div>
</nav>
<% } else { %>
<div class="empty-state">Không tìm thấy lớp học phần phù hợp.</div>
<% } %>

<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
