<%@ page contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.model.UserDirectory,vn.edu.onlineexam.util.Html" %>
<%
  var d=(UserDirectory)request.getAttribute("directory");
  var q=d.query();
  request.setAttribute("pageCrumb", "Quản lý người dùng");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Quản lý người dùng | Online Exam</title>
  <link rel="stylesheet" href="../assets/css/app.css">
</head>
<body>
<%@ include file="../layout/app-shell-start.jspf" %>

<div class="page-header">
  <div class="page-header-info">
    <h1>Quản lý người dùng</h1>
    <p class="page-subtitle">Tìm kiếm, phân quyền, khóa/mở khóa tài khoản trong hệ thống.</p>
  </div>
  <div class="page-actions">
    <a class="btn btn-primary" href="users/new">+ Thêm tài khoản</a>
  </div>
</div>

<% if(request.getAttribute("userSaved")!=null) { %><p role="status">Đã lưu tài khoản.</p><% } %>
<% if(request.getAttribute("statusMessage")!=null) { %><p role="status"><%=Html.escape(request.getAttribute("statusMessage"))%></p><% } %>

<div class="filter-card">
  <form id="search" method="get" action="users">
    <div class="filters">
      <label>Tên, email hoặc mã
        <input name="q" maxlength="254" placeholder="Nhập từ khóa…" value="<%=Html.escape(q.keyword())%>">
      </label>
      <label>Vai trò
        <select name="role">
          <option value="">Tất cả</option>
          <% for(String r:new String[]{"ADMIN","TEACHER","STUDENT"}) { %><option <%=r.equals(q.role())?"selected":""%>><%=r%></option><% } %>
        </select>
      </label>
      <label>Trạng thái
        <select name="status">
          <option value="">Tất cả</option>
          <% for(String s:new String[]{"ACTIVE","LOCKED","INACTIVE"}) { %><option <%=s.equals(q.status())?"selected":""%>><%=s%></option><% } %>
        </select>
      </label>
      <label>Sắp xếp
        <select name="sort">
          <% for(String[] s:new String[][]{{"created","Ngày tạo"},{"name","Họ tên"},{"email","Email"},{"code","Mã người dùng"}}) { %>
          <option value="<%=s[0]%>" <%=s[0].equals(q.sort().name())?"selected":""%>><%=s[1]%></option><% } %>
        </select>
      </label>
      <label>Thứ tự
        <select name="direction">
          <option value="ASC" <%=q.direction().name().equals("ASC")?"selected":""%>>Tăng dần</option>
          <option value="DESC" <%=q.direction().name().equals("DESC")?"selected":""%>>Giảm dần</option>
        </select>
      </label>
      <label>Số dòng/trang
        <input name="pageSize" type="number" min="1" max="100" value="<%=q.size()%>">
      </label>
      <button class="btn-primary">Tìm kiếm</button>
    </div>
  </form>
</div>

<div class="results">
  <table>
    <caption><%=d.total()%> tài khoản phù hợp</caption>
    <thead>
      <tr>
        <th>Mã người dùng</th>
        <th>Họ tên</th>
        <th>Email</th>
        <th>Vai trò</th>
        <th>Trạng thái</th>
        <th>Ngày tạo (UTC)</th>
        <th>Thao tác</th>
      </tr>
    </thead>
    <tbody>
    <% for(var row:d.rows()) { %>
      <tr>
        <td><strong><%=Html.escape(row.code())%></strong></td>
        <td class="cell-main"><%=Html.escape(row.name())%></td>
        <td><%=Html.escape(row.email())%></td>
        <td>
          <% for(String roleTag : String.valueOf(row.roles()).split(",")) { String rt = roleTag.trim(); if (!rt.isEmpty()) { %>
            <span class="<%=uiBadge(rt)%>"><%=Html.escape(rt)%></span>
          <% } } %>
        </td>
        <td><span class="<%=uiBadge(row.status())%>"><%=Html.escape(row.status())%></span></td>
        <td class="cell-sub"><%=Html.escape(row.created())%></td>
        <td>
          <div class="cell-actions">
            <a class="btn btn-sm" href="users/edit?id=<%=row.id()%>">Sửa</a>
            <a class="btn btn-sm btn-soft" href="users/roles?id=<%=row.id()%>">Vai trò</a>
            <% boolean lock="ACTIVE".equals(row.status());
               var actor=(vn.edu.onlineexam.model.AuthUser)request.getAttribute("authenticatedUser");
               if ((lock && actor.id()!=row.id()) || "LOCKED".equals(row.status())) { %>
            <details class="confirm-box <%=lock?"":"is-positive"%>">
              <summary><%=lock?"Khóa":"Mở khóa"%></summary>
              <div class="confirm-panel">
                <form method="post" action="users/<%=lock?"lock":"unlock"%>">
                  <input type="hidden" name="csrf" value="<%=Html.escape(session.getAttribute("csrf"))%>">
                  <input type="hidden" name="id" value="<%=row.id()%>">
                  <input type="hidden" name="version" value="<%=row.version()%>">
                  <% for(String key:new String[]{"q","role","status","page","pageSize","sort","direction"}) {
                     String val=request.getParameter(key); if(val!=null) { %><input type="hidden" name="<%=key%>" value="<%=Html.escape(val)%>"><% }} %>
                  <p style="font-size:.85rem"><%=lock?"Thu hồi mọi phiên của":"Cho phép đăng nhập lại với"%> <strong><%=Html.escape(row.email())%></strong>?</p>
                  <button class="btn btn-sm <%=lock?"btn-danger":"btn-success"%>">Xác nhận <%=lock?"khóa":"mở khóa"%></button>
                </form>
              </div>
            </details>
            <% } %>
          </div>
        </td>
      </tr>
    <% } %>
    </tbody>
  </table>
</div>

<% if(d.total()==0) { %>
  <div class="empty-state">Không tìm thấy tài khoản phù hợp.</div>
<% } else { %>
  <nav class="pagination" aria-label="Phân trang">
    <p>Trang <%=d.page()%> / <%=d.pages()%></p>
    <div class="pagination-btns">
      <% if(d.page()>1) { %><button class="btn btn-sm" form="search" name="page" value="<%=d.page()-1%>">← Trang trước</button><% } %>
      <% if(d.page()<d.pages()) { %><button class="btn btn-sm" form="search" name="page" value="<%=d.page()+1%>">Trang sau →</button><% } %>
    </div>
  </nav>
<% } %>

<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
