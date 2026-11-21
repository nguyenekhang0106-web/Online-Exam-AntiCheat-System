<%@ page pageEncoding="UTF-8" %>
<% request.setAttribute("pageCrumb", "Đề thi"); %>
<%@ include file="header.jspf" %>

<div class="page-header">
  <div class="page-header-info">
    <h1>Đề thi</h1>
    <p class="page-subtitle">Tìm kiếm, biên soạn, công bố và phân công lịch thi.</p>
  </div>
  <% if(request.getAttribute("subjects")!=null){ %>
  <div class="page-actions">
    <a class="btn btn-primary" href="#create-exam">+ Tạo đề nháp</a>
  </div>
  <% } %>
</div>

<div class="filter-card">
  <form class="filters">
    <label>Tìm theo tên
      <input name="q" placeholder="Nhập tên đề thi…" value="<%=escape(Objects.toString(request.getParameter("q"),""))%>">
    </label>
    <label>Trạng thái
      <select name="status">
        <% for(String state:List.of("","DRAFT","PUBLISHED","CLOSED","ARCHIVED")){ %><option value="<%=state%>" <%=state.equals(request.getParameter("status"))?"selected":""%>><%=state.isEmpty()?"Tất cả":state%></option><% } %>
      </select>
    </label>
    <label>Sắp xếp
      <select name="sort">
        <option value="latest">Mới nhất</option>
        <option value="title" <%="title".equals(request.getParameter("sort"))?"selected":""%>>Tên đề</option>
      </select>
    </label>
    <% int pg=(int)request.getAttribute("pageNumber"); var rows=(List<Map<String,Object>>)request.getAttribute("rows"); %>
    <button class="btn-primary" name="page" value="1">Tìm</button>
    <% if(pg>1){ %><button class="btn" name="page" value="<%=pg-1%>">← Trang trước</button><% } %>
    <% if(rows.size()>20){ %><button class="btn" name="page" value="<%=pg+1%>">Trang sau →</button><% } %>
  </form>
</div>

<% if(rows.isEmpty()){ %>
  <div class="empty-state">Không tìm thấy đề thi.</div>
<% } else { %>
<div class="schedule-list">
  <% for(var row:rows.stream().limit(20).toList()){ %>
  <article>
    <div class="task-card-tags">
      <span class="<%=uiBadge(row.get("status"))%>"><%=escape(row.get("status"))%></span>
      <span class="badge badge-purple"><%=escape(row.get("subject_name"))%></span>
    </div>
    <h3 style="margin-bottom:.45rem"><a href="?id=<%=row.get("id")%>"><%=escape(row.get("title"))%></a></h3>
    <div class="task-card-foot">
      <span>Tối đa <%=row.get("max_attempts")%> lượt</span>
      <a class="btn btn-sm btn-soft" href="?id=<%=row.get("id")%>">Mở chi tiết →</a>
    </div>
  </article>
  <% } %>
</div>
<% } %>

<% if(request.getAttribute("subjects")!=null){ Map<String,Object> e=Map.of(); %>
<div class="card" id="create-exam">
  <div class="card-header">
    <h2 class="card-title">Tạo đề nháp</h2>
    <span class="badge badge-pink">DRAFT</span>
  </div>
  <form method="post" class="stack-form" style="max-width:100%">
    <input type="hidden" name="csrf" value="${sessionScope.csrf}">
    <label>Môn học *
      <select name="subject" required>
        <% for(var subject:(List<Map<String,Object>>)request.getAttribute("subjects")){ %><option value="<%=subject.get("id")%>"><%=escape(subject.get("label"))%></option><% } %>
      </select>
    </label>
    <%@ include file="config.jspf" %>
    <div class="form-actions">
      <button class="btn-primary" name="action" value="create">Tạo đề</button>
    </div>
  </form>
</div>
<% } %>

<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
