<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8" import="vn.edu.onlineexam.dao.ProctoringEventDao.Page,static vn.edu.onlineexam.util.Html.escape" %>
<!doctype html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Lịch sử giám sát</title><link rel="stylesheet" href="../../assets/css/app.css"></head>
<body><main><a href="../account">Tài khoản</a><h1>Lịch sử giám sát</h1>
<form><label>Lượt thi <input name="attempt" type="number" min="1"></label> <label>Sinh viên <input name="student" type="number" min="1"></label> <label>Trang <input name="page" type="number" min="1" value="1"></label> <button>Lọc</button></form>
<% var result=(Page)request.getAttribute("result"); %><p><%=result.count()%> sự kiện phù hợp.</p>
<% for(var event:result.rows()){ %><article style="overflow-wrap:anywhere"><h2><%=escape(event.get("event_code"))%></h2><p><%=escape(event.get("title"))%> · Lượt #<%=event.get("attempt_id")%> · Sinh viên #<%=event.get("student_id")%></p>
<p><%=java.util.Map.of(
"TAB_HIDDEN", "Trang thi chuyển sang trạng thái ẩn.",
"WINDOW_BLUR", "Cửa sổ thi mất focus trong khi trang còn hiển thị.",
"FULLSCREEN_EXIT", "Trang thi vừa thoát chế độ toàn màn hình.",
"COPY_ATTEMPT", "Trình duyệt phát hiện thao tác sao chép trong câu hỏi.",
"PASTE_ATTEMPT", "Trình duyệt phát hiện thao tác dán trong câu hỏi.",
"CONNECTION_LOST", "Trình duyệt báo mất kết nối.",
"CONNECTION_RESTORED", "Trình duyệt báo kết nối trở lại."
).getOrDefault(String.valueOf(event.get("event_code")), "Tín hiệu do trình duyệt báo cáo.")%></p>
<p>Server nhận: <%=escape(event.get("server_received_at"))%> UTC · <%=escape(event.get("severity"))%></p>
<p>Nguồn: <%=escape(event.get("source"))%> · Thời gian client khai báo (UTC): <%=escape(event.get("client_occurred_at"))%> · <%=java.util.Set.of("true","1").contains(String.valueOf(event.get("reviewed")))?"Đã xem xét":"Chưa xem xét"%></p></article><% } %></main></body></html>
