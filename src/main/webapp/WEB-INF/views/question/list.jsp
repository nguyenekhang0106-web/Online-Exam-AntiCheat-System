<%@ page contentType="text/html; charset=UTF-8" %><%@ include file="common.jspf" %>
<%
  var result=(Page)request.getAttribute("result");
  var f=result.filter();
  request.setAttribute("pageCrumb", title);
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title><%=title%> | Online Exam</title>
  <link rel="stylesheet" href="<%=root%>/assets/css/app.css">
  <link rel="stylesheet" href="<%=root%>/assets/css/subjects.css">
</head>
<body>
<%@ include file="../layout/app-shell-start.jspf" %>

<div class="page-header">
  <div class="page-header-info">
    <h1><%=title%></h1>
    <p class="page-subtitle">Quản lý và tra cứu <%=title.toLowerCase(java.util.Locale.ROOT)%> theo môn học.</p>
  </div>
  <% if(manage){ %>
  <div class="page-actions">
    <a class="btn btn-primary" href="<%=base%>/new">+ Tạo mới</a>
  </div>
  <% } %>
</div>

<div class="filter-card">
  <form class="filters" id="filters" method="get">
    <label>Tìm kiếm
      <input maxlength="254" name="q" value="<%=h(f.term())%>">
    </label>
    <label>ID môn
      <input min="1" type="number" name="subject" value="<%=f.subject()>0?f.subject():""%>">
    </label>
    <% if(questions){ %>
      <label>ID ngân hàng<input min="1" type="number" name="bank" value="<%=f.bank()>0?f.bank():""%>"></label>
      <label>ID chủ đề<input min="1" type="number" name="topic" value="<%=f.topic()>0?f.topic():""%>"></label>
    <% } %>
    <% for(String key:questions?List.of("state","type","difficulty"):banks?List.of("state"):List.<String>of()){
       String[] vals=key.equals("type")?new String[]{"SINGLE_CHOICE","MULTIPLE_CHOICE"}:key.equals("difficulty")?new String[]{"EASY","MEDIUM","HARD"}:banks?new String[]{"PRIVATE","SHARED"}:new String[]{"DRAFT","PUBLISHED","ARCHIVED"};
       String current=key.equals("type")?f.type():key.equals("difficulty")?f.difficulty():f.state(); %>
      <label><%=key.equals("type")?"Loại câu":key.equals("difficulty")?"Độ khó":banks?"Hiển thị":"Trạng thái"%>
        <select name="<%=key%>">
          <option value="">Tất cả</option>
          <% for(String val:vals){ %><option <%=val.equals(current)?"selected":""%>><%=val%></option><% } %>
        </select>
      </label>
    <% } %>
    <label>Sắp theo
      <select name="sort">
        <option value="id">ID</option>
        <option value="name" <%=f.sort().equals("name")?"selected":""%>><%=questions?"Tên ngân hàng":"Tên"%></option>
      </select>
    </label>
    <label>Thứ tự
      <select name="direction">
        <option value="ASC">Tăng</option>
        <option value="DESC" <%=f.descending()?"selected":""%>>Giảm</option>
      </select>
    </label>
    <label>Số dòng
      <input name="size" type="number" min="1" max="100" value="<%=f.size()%>">
    </label>
    <button class="btn-primary">Tìm</button>
  </form>
</div>

<% if(result.rows().isEmpty()){ %>
  <div class="empty-state">Không tìm thấy dữ liệu.</div>
<% } else { %>
<div class="results">
  <table>
    <thead>
      <tr>
        <th>ID</th>
        <th><%=title%></th>
        <th>Môn</th>
        <th><%=topics?"Mã chủ đề":banks?"Số câu":"Trạng thái"%></th>
      </tr>
    </thead>
    <tbody>
    <% for(var row:result.rows()){ %>
      <tr>
        <td>#<%=h(row.get("id"))%></td>
        <td class="cell-main"><a href="<%=base%>/detail?id=<%=h(row.get("id"))%>"><%=h(row.get("label"))%></a></td>
        <td><span class="badge badge-purple"><%=h(row.get("subject_code"))%></span></td>
        <td>
          <% Object colVal = row.get(topics?"topic_code":banks?"question_count":"status"); %>
          <% if(questions){ %><span class="<%=uiBadge(colVal)%>"><%=h(colVal)%></span>
          <% } else if(banks){ %><span class="badge badge-neutral"><%=h(colVal)%> câu</span>
          <% } else { %><strong><%=h(colVal)%></strong><% } %>
        </td>
      </tr>
    <% } %>
    </tbody>
  </table>
</div>
<% } %>

<nav class="pagination" aria-label="Phân trang">
  <p>Trang <%=f.page()%></p>
  <div class="pagination-btns">
    <% if(f.page()>1){ %><button class="btn btn-sm" form="filters" name="page" value="<%=f.page()-1%>">← Trang trước</button><% } %>
    <% if(result.more()){ %><button class="btn btn-sm" form="filters" name="page" value="<%=f.page()+1%>">Trang sau →</button><% } %>
  </div>
</nav>

<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
