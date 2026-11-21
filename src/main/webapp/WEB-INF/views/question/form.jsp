<%@ page contentType="text/html; charset=UTF-8" %><%@ include file="common.jspf" %>
<%
  var detail=(Detail)request.getAttribute("detail");
  boolean create=detail==null;
  Map<String,Object> row=create?Map.of():detail.row();
  var choices=(List<Map<String,Object>>)request.getAttribute("choices");
  String relation=questions?"bank":"subject";
  String heading=(create?"Tạo":"Sửa")+" "+title.toLowerCase(java.util.Locale.ROOT);
  request.setAttribute("pageCrumb", heading);
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title><%=heading%> | Online Exam</title>
  <link rel="stylesheet" href="<%=root%>/assets/css/app.css">
  <link rel="stylesheet" href="<%=root%>/assets/css/subjects.css">
</head>
<body>
<%@ include file="../layout/app-shell-start.jspf" %>

<a class="back-link" href="<%=base%>">← Danh sách <%=title.toLowerCase(java.util.Locale.ROOT)%></a>
<div class="page-header">
  <div class="page-header-info"><h1><%=heading%></h1></div>
</div>

<div class="card form-card">
  <form class="subject-form" method="post" action="<%=base%>/<%=create?"new":"edit"%>">
    <input type="hidden" name="csrf" value="<%=h(session.getAttribute("csrf"))%>">
    <% if(!create){ %>
      <input type="hidden" name="id" value="<%=h(row.get("id"))%>">
      <input type="hidden" name="stamp" value="<%=h(row.get("stamp"))%>">
      <input type="hidden" name="<%=relation%>" value="<%=h(row.get(relation+"_id"))%>">
      <p class="alert alert-info"><%=questions?"Ngân hàng":"Môn học"%>: <strong><%=h(row.get(questions?"bank_name":"subject_code"))%></strong></p>
    <% } else { %>
      <label><%=questions?"Ngân hàng":"Môn học"%> *
        <select required name="<%=relation%>">
          <option value="">Chọn</option>
          <% for(var o:choices){ %><option value="<%=h(o.get("id"))%>"><%=h(o.get("label"))%></option><% } %>
        </select>
      </label>
    <% } %>

    <% if(!questions){ %>
      <label>Tên <%=title.toLowerCase(java.util.Locale.ROOT)%> *
        <input required maxlength="<%=topics?150:200%>" name="name" value="<%=h(row.get("label"))%>">
      </label>
    <% } %>

    <% if(topics){ %>
      <div class="form-grid-2">
        <label>Mã chủ đề *<input required maxlength="30" name="code" value="<%=h(row.get("topic_code"))%>"></label>
        <label>ID chủ đề cha (tùy chọn)<input type="number" min="1" name="parent" value="<%=h(row.get("parent_topic_id"))%>"></label>
      </div>
      <label>Thứ tự *<input required type="number" min="0" name="order" value="<%=create?"0":h(row.get("display_order"))%>"></label>
    <% } %>

    <% if(banks){ %>
      <label>Mô tả<textarea name="description"><%=h(row.get("description"))%></textarea></label>
      <label>Hiển thị
        <select name="visibility">
          <option>PRIVATE</option>
          <option <%="SHARED".equals(row.get("visibility"))?"selected":""%>>SHARED</option>
        </select>
      </label>
    <% } %>

    <% if(questions){ %>
      <div class="form-grid-2">
        <label>ID chủ đề (tùy chọn)<input type="number" min="1" name="topic" value="<%=h(row.get("topic_id"))%>"></label>
        <label>Điểm mặc định *<input required type="number" min="0" max="99999.99" step="0.01" name="points" value="<%=create?"1":h(row.get("default_points"))%>"></label>
      </div>
      <div class="form-grid-2">
        <label>Loại câu
          <select name="type">
            <option value="SINGLE_CHOICE">Một đáp án đúng</option>
            <option value="MULTIPLE_CHOICE" <%="MULTIPLE_CHOICE".equals(row.get("question_type"))?"selected":""%>>Nhiều đáp án đúng</option>
          </select>
        </label>
        <label>Độ khó
          <select name="difficulty">
            <% for(String d:List.of("EASY","MEDIUM","HARD")){ %><option <%=d.equals(row.get("difficulty"))?"selected":""%>><%=d%></option><% } %>
          </select>
        </label>
      </div>
      <label>Nội dung *<textarea required name="content" rows="5"><%=h(row.get("label"))%></textarea></label>
      <label>Giải thích<textarea name="explanation" rows="3"><%=h(row.get("explanation"))%></textarea></label>

      <div id="answers">
      <% var answers=new ArrayList<Answer>(); if(!create) answers.addAll(detail.options());
         while(answers.size()<2) answers.add(new Answer("",false));
         for(int i=0;i<answers.size();i++){ var a=answers.get(i); %>
        <fieldset>
          <legend>Phương án</legend>
          <label>Nội dung<textarea required name="option"><%=h(a.text())%></textarea></label>
          <div class="answer-editor-row">
            <label class="check-label"><input type="checkbox" name="correct" value="<%=i%>" <%=a.correct()?"checked":""%>> <span>Đáp án đúng</span></label>
            <button type="button" class="btn btn-sm btn-danger remove">Bỏ phương án</button>
          </div>
        </fieldset>
      <% } %>
      </div>
      <div>
        <button type="button" class="btn btn-soft" id="add">+ Thêm phương án</button>
      </div>
      <script>
      const answers=document.getElementById('answers');
      const template=answers.firstElementChild?.cloneNode(true);
      function numberAnswers(){Array.from(answers.children).forEach((f,i)=>{f.querySelector('legend').textContent='Phương án '+(i+1);f.querySelector('[name=correct]').value=i;f.querySelector('.remove').disabled=answers.children.length<=2;});}
      document.getElementById('add').onclick=()=>{if(!template)return;const f=template.cloneNode(true);f.querySelector('textarea').value='';f.querySelector('[name=correct]').checked=false;answers.append(f);numberAnswers();};
      answers.onclick=e=>{if(e.target.matches('.remove') && answers.children.length>2){e.target.closest('fieldset').remove();numberAnswers();}};numberAnswers();
      </script>
    <% } %>

    <div class="form-actions">
      <button class="btn-primary">Lưu</button>
      <a class="btn" href="<%=base%>">Hủy</a>
    </div>
  </form>
</div>

<%@ include file="../layout/app-shell-end.jspf" %>
</body>
</html>
