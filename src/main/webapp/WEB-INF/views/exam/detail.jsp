<%@ page pageEncoding="UTF-8" %>
<% request.setAttribute("pageCrumb", "Chi tiết đề thi"); %>
<%@ include file="header.jspf" %>
<%
  var detail=(ExamService.Detail)request.getAttribute("detail");
  var e=detail.exam();
  boolean teacher="TEACHER".equals(request.getAttribute("roleLabel"));
  boolean draft=teacher && "DRAFT".equals(e.get("status"));
  boolean published=teacher && "PUBLISHED".equals(e.get("status"));
%>

<a class="back-link" href="exams">← Danh sách đề</a>
<div class="page-header">
  <div class="page-header-info">
    <h1><%=escape(e.get("title"))%></h1>
    <div class="inline-meta">
      <span class="badge badge-purple"><%=escape(e.get("subject_name"))%></span>
      <span class="<%=uiBadge(e.get("status"))%>"><%=escape(e.get("status"))%></span>
      <span class="badge badge-neutral"><%=e.get("duration_minutes")%> phút</span>
      <span class="badge badge-neutral"><%=e.get("max_attempts")%> lượt</span>
      <span class="badge badge-green">Điểm đạt <%=e.get("pass_percentage")%>%</span>
    </div>
  </div>
  <% if(teacher && !"ARCHIVED".equals(e.get("status"))){ %>
  <div class="page-actions">
    <form method="post" style="margin:0">
      <%@ include file="keys.jspf" %>
      <button class="<%=draft?"btn-success":published?"btn-danger":"btn"%>" name="action" value="<%=draft?"PUBLISHED":published?"CLOSED":"ARCHIVED"%>"><%=draft?"Công bố đề":published?"Đóng đề":"Lưu trữ đề"%></button>
    </form>
  </div>
  <% } %>
</div>

<% if(draft){ %>
<div class="card">
  <form method="post">
    <fieldset style="margin:0;border:none;padding:0;background:transparent">
      <legend style="padding:0;margin-bottom:1rem;font-size:1.05rem">Sửa cấu hình</legend>
      <%@ include file="keys.jspf" %>
      <%@ include file="config.jspf" %>
      <div class="form-actions" style="margin-top:1rem">
        <button class="btn-primary" name="action" value="edit">Lưu cấu hình</button>
      </div>
    </fieldset>
  </form>
</div>
<% } %>

<% if(teacher){ BigDecimal total=BigDecimal.ZERO; %>
<div class="card">
  <div class="card-header">
    <h2 class="card-title">Nội dung đề</h2>
    <span class="badge badge-purple"><%=detail.items().size()%> câu nguồn</span>
  </div>

  <div class="item-list">
  <% for(var item:detail.items()){ total=total.add((BigDecimal)item.get("points")); %>
    <article>
      <div class="inline-meta" style="margin-top:0">
        <strong>Câu ở vị trí <%=item.get("fixed_order")%></strong>
        <span class="badge badge-neutral">phiên bản <%=item.get("version_no")%></span>
        <span class="badge badge-green"><%=item.get("points")%> điểm</span>
      </div>
      <p class="description" style="margin-bottom:0"><%=escape(item.get("content"))%></p>
      <% if(draft){ %>
      <form class="exam-item-form" method="post">
        <%@ include file="keys.jspf" %>
        <input type="hidden" name="version" value="<%=item.get("question_version_id")%>">
        <label>Thứ tự <input type="number" name="position" min="0" required value="<%=item.get("fixed_order")%>"></label>
        <label>Điểm <input type="number" step=".01" min="0" max="99999.99" required name="points" value="<%=item.get("points")%>"></label>
        <button class="btn btn-sm btn-primary" name="action" value="revise">Lưu câu</button>
        <button class="btn btn-sm btn-danger" name="action" value="remove" formnovalidate>Loại khỏi đề</button>
      </form>
      <% } %>
    </article>
  <% } %>
  </div>

  <p class="alert alert-info" style="margin-bottom:0">
    Tập nguồn: <strong><%=detail.items().size()%></strong> câu · Tổng điểm nguồn: <strong><%=total%></strong>.
    Số câu mỗi lượt: <strong><%="RANDOM_PER_ATTEMPT".equals(e.get("selection_mode"))?e.get("question_count"):detail.items().size()%></strong>.
  </p>

  <% if(draft){ %>
  <div style="margin-top:1.25rem;padding-top:1.15rem;border-top:1px solid var(--border-subtle)">
    <h3 style="margin-bottom:.75rem">Chọn câu đã công bố</h3>
    <form class="filters" style="margin-bottom:1rem">
      <input type="hidden" name="id" value="<%=e.get("id")%>">
      <label>Ngân hàng <input name="bank" value="<%=escape(Objects.toString(request.getParameter("bank"),""))%>"></label>
      <label>Chủ đề <input name="topic" value="<%=escape(Objects.toString(request.getParameter("topic"),""))%>"></label>
      <label>Độ khó
        <select name="difficulty">
          <option value="">Tất cả</option>
          <option <%="EASY".equals(request.getParameter("difficulty"))?"selected":""%>>EASY</option>
          <option <%="MEDIUM".equals(request.getParameter("difficulty"))?"selected":""%>>MEDIUM</option>
          <option <%="HARD".equals(request.getParameter("difficulty"))?"selected":""%>>HARD</option>
        </select>
      </label>
      <button class="btn">Lọc</button>
    </form>

    <form class="filters" method="post">
      <%@ include file="keys.jspf" %>
      <label style="flex:2 1 260px">Câu hỏi
        <select required name="version">
          <% for(var v:detail.choices()){ %><option value="<%=v.get("id")%>"><%=escape(v.get("bank_name"))%> · <%=escape(v.get("content"))%></option><% } %>
        </select>
      </label>
      <label style="flex:0 1 120px">Thứ tự <input required type="number" min="0" name="position"></label>
      <label style="flex:0 1 120px">Điểm <input required type="number" min="0" max="99999.99" step=".01" name="points"></label>
      <button class="btn-primary" name="action" value="add">+ Thêm vào đề</button>
    </form>

    <% if(total.signum()==0){ %>
      <p class="alert alert-danger" style="margin-top:.85rem;margin-bottom:0">Để công bố, đề cần có câu hỏi hợp lệ và tổng điểm lớn hơn 0.</p>
    <% } %>
  </div>
  <% } %>
</div>
<% } %>

<div class="card">
  <div class="card-header">
    <h2 class="card-title">Phân công thi</h2>
  </div>
  <%@ include file="calendar.jspf" %>

  <% if(published){ %>
  <div class="form-grid-2" style="margin-top:1rem">
    <% for(var scope:List.of("COURSE","STUDENT")){ var targets=scope.equals("COURSE")?detail.courses():detail.students(); %>
    <form method="post">
      <fieldset>
        <legend><%=scope.equals("COURSE")?"Phân công lớp học phần":"Phân công cá nhân"%></legend>
        <%@ include file="keys.jspf" %>
        <input type="hidden" name="scope" value="<%=scope%>">
        <div class="stack-form">
          <label>Đối tượng
            <select name="target" required>
              <% for(var target:targets){ %><option value="<%=target.get("id")%>"><%=escape(target.get("label"))%> (#<%=target.get("id")%>)</option><% } %>
            </select>
          </label>
          <label>Mở (UTC) <input name="opens" type="datetime-local" required></label>
          <label>Đóng (UTC) <input name="closes" type="datetime-local" required></label>
          <label>Ghi đè số lượt <input type="number" min="1" max="32767" name="limit" placeholder="Bỏ trống nếu theo đề"></label>
          <div class="form-actions">
            <button class="btn-primary btn-sm" name="action" value="assign">Tạo lịch</button>
            <button class="btn btn-sm" name="action" value="reschedule">Cập nhật lịch</button>
          </div>
        </div>
      </fieldset>
    </form>
    <% } %>
  </div>

  <% if(!detail.schedule().isEmpty()){ %>
  <div class="form-actions" style="margin-top:.75rem;padding-top:.75rem;border-top:1px solid var(--border-subtle)">
    <% for(var slot:detail.schedule()){ %>
    <form method="post" style="margin:0">
      <%@ include file="keys.jspf" %>
      <input type="hidden" name="scope" value="<%=slot.get("scope")%>">
      <input type="hidden" name="target" value="<%=slot.get("target")%>">
      <button class="btn btn-sm btn-danger" name="action" value="cancel">Thu hồi lịch <%=slot.get("scope")%> #<%=slot.get("target")%></button>
    </form>
    <% } %>
  </div>
  <% } %>
  <% } %>
</div>

<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
