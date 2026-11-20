# Hợp đồng MVC và quyền thao tác

Áp dụng cho các module trong `vn.edu.onlineexam`. V1–V7 là chuẩn tên và kiểu dữ liệu. Mọi đề nghị đổi schema hoặc Bean phải ghi lỗi, tác động, phương án chuyển dữ liệu và được nhóm duyệt trước khi sửa.

## Ranh giới trách nhiệm

Servlet nhận HTTP, kiểm tra định dạng, lấy danh tính từ session, gọi Service, chuyển DTO vào JSP dưới WEB-INF/views. GET chỉ đọc. POST thay đổi dữ liệu phải kiểm tra CSRF. Filter kiểm tra session và vai trò ở mức route. Service kiểm tra quyền trên chính đối tượng trong mỗi lần đọc/ghi. ID gửi từ form chỉ giúp tìm bản ghi. JSP escape dữ liệu và chỉ hiển thị trường được phép.

Service sở hữu transaction. Một use case dùng một JDBC Connection cho mọi DAO liên quan, commit một lần, rollback khi lỗi. DAO nhận Connection, dùng PreparedStatement, đóng statement/result set, trả Bean hoặc projection. Không commit trong DAO. Không giữ Connection hay dữ liệu request trong field Servlet. Dùng UTC trên connection, LocalDateTime chính xác mili giây, BigDecimal cho điểm, kiểu đối tượng cho NULL. Không log hash, token hoặc nội dung bằng chứng. Lưu bí mật kết nối ngoài WAR.

## Ma trận đề xuất

Mỗi dòng mô tả thao tác nghiệp vụ được phép. “Phụ trách” nghĩa là đề do giáo viên tạo hoặc lớp học phần được phân công, xác minh bằng JOIN. “Cá nhân” luôn lấy ID từ session. Dấu — nghĩa là không có chức năng trực tiếp. ADMIN không mặc nhiên có quyền thi hộ hoặc sửa điểm. Cột cuối nêu đối tượng cần kiểm tra hoặc ghi cùng transaction. Các bảng có CRUD đầy đủ **có điều kiện** là options và exam_questions ở bản nháp chưa sử dụng. Các thao tác xóa khác bên dưới chỉ là thu hồi liên kết hoặc loại lựa chọn trong một use case.

| Bảng | ADMIN | TEACHER | STUDENT | Quan hệ, vòng đời và transaction |
|---|---|---|---|---|
| users | Tạo, đọc, sửa hồ sơ, khóa/mở | Đọc/sửa hồ sơ cá nhân | Như giáo viên | Khóa tài khoản và thu hồi session cùng transaction |
| roles | Đọc danh mục | — | — | Ba mã dùng khi cấp quyền |
| user_roles | Cấp/thu hồi và tra cứu | — | — | Kiểm tra user, role, ghi actor và audit nguyên tử |
| password_reset_tokens | Yêu cầu/dùng cho cá nhân | Như ADMIN | Như ADMIN | Backend tạo hash, khóa token, kiểm tra hạn, đánh dấu dùng và đổi mật khẩu một lần |
| login_sessions | Xem/thu hồi phiên, đăng nhập | Đăng nhập, đăng xuất cá nhân | Như giáo viên | Backend tạo, cập nhật last_seen, revoke theo user |
| audit_logs | Đọc phục vụ kiểm toán | — | — | Backend chỉ thêm cùng thao tác được ghi nhận |
| subjects | Tạo/sửa, archive | Đọc môn phụ trách | Đọc môn đã đăng ký | Kiểm tra offering trước thay trạng thái |
| classes | Tạo/sửa, archive | Đọc lớp phụ trách | Đọc lớp cá nhân | Giữ lịch sử class_students |
| class_students | Thêm, đánh dấu rời/tái gia nhập | Đọc danh sách phụ trách | Đọc tư cách cá nhân | Khóa cặp lớp–sinh viên, đồng bộ status và left_at |
| course_offerings | Tạo, phân công, đóng lớp | Đọc lớp được giao | Đọc lớp đã đăng ký | Kiểm tra môn, lớp, vai trò giáo viên |
| course_enrollments | Ghi danh, rút, hoàn thành | Đọc danh sách phụ trách | Đọc cá nhân | Khóa cặp offering–student, kiểm tra quyền thi đang hiệu lực |
| topics | Quản lý cây môn | Tạo/sửa trong môn phụ trách | — | Khóa cây theo môn, kiểm tra cha cùng môn và chu trình |
| question_banks | Tra cứu quản trị | Tạo/sửa ngân hàng sở hữu, đọc ngân hàng chia sẻ | — | Kiểm tra subject, owner và visibility |
| questions | Tra cứu quản trị | Tạo, sửa nháp, publish/archive | — | Khóa question trước khi cấp version_no |
| question_versions | — | Thêm phiên bản và đọc theo quyền bank | Chỉ nội dung đã giao qua DTO | Ghi version và options cùng transaction, bất biến khi publish |
| question_version_options | — | CRUD bản nháp | Đọc lựa chọn của lượt thi | Chặn sửa khi version đã dùng, ẩn is_correct |
| question_attachments | — | Đính/gỡ nháp và đọc | Tải tệp thuộc câu được giao | Kiểm tra option cùng version, phối hợp kho tệp và metadata |
| proctoring_policies | Quản lý mẫu | Tạo/đọc mẫu được phép | Đọc quy định áp dụng | Snapshot khi publish, bảo toàn lịch sử đề |
| exams | Tra cứu | Tạo/sửa nháp, publish/close/archive đề sở hữu | Đọc đề đã phân công | Publish cùng kiểm tra câu hỏi, điểm, chính sách |
| exam_questions | — | CRUD trong đề nháp | Đọc qua lượt thi | Khóa exam, kiểm tra version cùng môn và thứ tự |
| exam_course_assignments | Tra cứu | Gán/sửa/thu hồi trước khi thi | Đọc lịch hợp lệ | Kiểm tra exam và offering cùng môn, cửa sổ và override |
| exam_student_assignments | Tra cứu | Gán/sửa/thu hồi trước khi thi | Đọc lịch cá nhân | Khóa phân công, kiểm tra student và đề |
| exam_attempts | Tra cứu theo nhiệm vụ | Đọc kết quả đề phụ trách | Bắt đầu, tiếp tục, nộp lượt cá nhân | Backend quản lý trạng thái, điểm và deadline |
| attempt_questions | — | Đọc khi chấm | Đọc câu của lượt cá nhân | Backend chỉ thêm snapshot lúc bắt đầu, kiểm tra exam |
| attempt_option_orders | — | Đọc khi chấm | Đọc thứ tự cá nhân | Backend chỉ thêm, option phải thuộc version |
| attempt_answers | — | Đọc khi chấm | Lưu câu trả lời trong hạn | Khóa attempt, upsert theo attempt_question, backend chấm |
| attempt_answer_choices | — | Đọc khi chấm | Chọn/bỏ chọn trong hạn | Thay tập lựa chọn cùng answer, kiểm tra version và loại câu |
| grade_adjustments | Đọc audit | Điều chỉnh có lý do cho đề phụ trách | Đọc kết quả được công bố | Chỉ thêm lịch sử, cập nhật answer và tổng cùng transaction |
| attempt_sessions | Tra cứu sự cố | Đọc phiên đề phụ trách | Mở/đóng phiên cá nhân | Backend khóa attempt, kiểm tra số phiên mở |
| proctoring_events | Đọc theo nhiệm vụ | Đọc đề phụ trách | Gửi tín hiệu của phiên cá nhân | Backend chỉ thêm, đối chiếu session và chống trùng client_event_id |
| proctoring_evidence | Đọc theo nhiệm vụ | Xem khi review được giao | Gửi theo sự kiện được phép | Backend tạo metadata, hết hạn xóa object và đánh dấu deleted_at |
| violation_reviews | Review khi được giao | Review đề phụ trách | — | Chỉ thêm quyết định, invalidation và audit nguyên tử |
| notifications | Đọc/đánh dấu cá nhân | Như ADMIN | Như ADMIN | Backend tạo theo recipient, chỉ người nhận đánh dấu đã đọc |
| exam_appeals | Theo dõi và phân xử | Xử lý đề phụ trách | Tạo, đọc đơn của mình | Kiểm tra chủ attempt, khóa đơn trước khi chuyển trạng thái |

## Luồng dễ sai

- Bắt đầu thi phải khóa hàng sinh viên ổn định trước khi đếm lượt, kiểm tra đề đã publish, enrollment và cửa sổ phân công. Chọn override cá nhân, rồi lớp hợp lệ có ID phân công nhỏ nhất, rồi mặc định đề. Đếm cả lượt bị hủy/vô hiệu theo đề xuất hiện tại. Resume lượt đang mở. Lưu attempt, thứ tự câu và đáp án cùng transaction.
- Autosave và submit khóa cùng hàng attempt. Chỉ chấp nhận thay đổi trong hạn theo đồng hồ server. Chấm từ đáp án server và điểm snapshot. Submit lặp trả kết quả cũ. Chỉ trả lời giải và điểm khi thỏa quy tắc công bố. MANUAL cần quyết định riêng ở roadmap.
- Dấu hiệu giám sát cần người có thẩm quyền xem xét. Bảo vệ đường tải evidence bằng kiểm tra quyền trên attempt. Chỉ việc review mới có thể dẫn đến quyết định vô hiệu hóa.
- Khi FK RESTRICT chặn xóa, báo xung đột nghiệp vụ. Không tự xóa bản ghi con để vượt ràng buộc. Với đối tượng không có cột trạng thái, chỉ cung cấp các thao tác đã nêu trong ma trận.
