# Lộ trình MVC

Hiện tại welcome route chuyển đến HomeServlet ở /home. Servlet đặt request attribute rồi forward sang WEB-INF/views/home.jsp. Tomcat 10.1 cung cấp Servlet 6.0. Maven đóng WAR Java 21 với Connector/J 9.6.0. Package model.bean có 7 nhóm. DAO, Service và Filter mới có package-info. PasswordHasher đã có encoder/verifier. Các tên bên dưới là thành phần dự kiến, triển khai theo thứ tự dòng. Mỗi DAO phụ trách các bảng tương ứng trong ma trận.

| Giai đoạn và phụ trách | Controller dự kiến | Service | DAO | JSP dưới WEB-INF/views |
|---|---|---|---|---|
| 1 JDBC và xác thực · Khang | LoginServlet, LogoutServlet, ResetPasswordServlet | AuthService | UserDao, RoleDao, UserRoleDao, SessionDao, ResetTokenDao, AuditDao | auth/login.jsp, auth/reset.jsp |
| 2 Admin và đào tạo · Khang | AdminUserServlet, AcademicServlet, EnrollmentServlet | UserAdminService, AcademicService | SubjectDao, SchoolClassDao, ClassStudentDao, OfferingDao, EnrollmentDao và DAO giai đoạn 1 | admin/users.jsp, academic/classes.jsp, academic/roster.jsp |
| 3 Ngân hàng câu hỏi · Trung | TopicServlet, QuestionBankServlet, QuestionServlet, AttachmentServlet | QuestionBankService | TopicDao, QuestionBankDao, QuestionDao, QuestionVersionDao, OptionDao, AttachmentDao | question/banks.jsp, question/edit.jsp |
| 4 Đề và phân công · Trung, Cường phụ trách chính sách | ExamServlet, ExamAssignmentServlet, PolicyServlet | ExamService, AssignmentService, PolicyService | ExamDao, ExamQuestionDao, CourseAssignmentDao, StudentAssignmentDao, PolicyDao | exam/edit.jsp, exam/assign.jsp, exam/policies.jsp |
| 5 Làm bài · Trung | StartAttemptServlet, AttemptServlet, SaveAnswerServlet, SubmitAttemptServlet | AttemptService | AttemptDao, AttemptQuestionDao, OptionOrderDao, AnswerDao, ChoiceDao | attempt/list.jsp, attempt/take.jsp |
| 6 Chấm và công bố · Cường | ResultServlet, GradeAdjustmentServlet | GradingService, ResultService | GradeAdjustmentDao và DAO lượt thi | result/detail.jsp, result/grade.jsp |
| 7 Giám sát và review · Cường | AttemptSessionServlet, ProctoringEventServlet, EvidenceServlet, ViolationReviewServlet | ProctoringService, ReviewService | AttemptSessionDao, EventDao, EvidenceDao, ViolationReviewDao | proctoring/monitor.jsp, proctoring/review.jsp |
| 8 Phúc khảo, thông báo và báo cáo · Cường | AppealServlet, NotificationServlet, ReportServlet | AppealService, NotificationService, ReportService | AppealDao, NotificationDao, ReportDao đọc tổng hợp | common/appeals.jsp, common/inbox.jsp, report/summary.jsp |

Giai đoạn 1 chuẩn bị DataSource từ cấu hình ngoài WAR, Filter phiên/quyền và CSRF. Chạy integration test JDBC trên schema thử nghiệm khi được phép. Giai đoạn 5 và 6 phải tích hợp cùng luồng submit trước khi phát hành. Báo cáo đọc dữ liệu hiện có theo phạm vi giáo viên/quản trị. Xem điều kiện nghiệm thu tại [bảng tiến độ](implementation-progress.md).

## Kết quả đối chiếu và quyết định còn mở

- V1–V7 và Bean khớp 34 cặp bảng/lớp, 265 cột/thuộc tính và 60 FK theo kiểm tra tĩnh. ERD có 60 cạnh nhưng vẽ attempt_questions → attempt_answers là một–nhiều. V5 đặt UNIQUE(attempt_question_id), nên đúng là mỗi câu của lượt thi có **0 hoặc 1** answer. DAO cần trả Optional hoặc tương đương.
- Yêu cầu nêu MySQL 8.x. README ghi máy đang có MySQL 9.6 và tài liệu thiết kế chọn 8.4+. Dùng MySQL 8.4.x làm đích nghiệm thu dự kiến, ghi riêng phiên bản server và driver. Kiểm thử tĩnh chưa xác nhận migration chạy trên 8.4.
- Giá trị MANUAL xuất hiện trong chế độ công bố nhưng schema chưa có dấu mốc công bố thủ công. Giữ kết quả/đáp án ở trạng thái ẩn cho tới khi nhóm duyệt cách lưu quyết định và cách thu hồi.
- Kiểu câu hỏi nằm ở questions, còn đề tham chiếu question_versions. Khi đã publish, sửa loại trên bản gốc có thể làm đổi cách hiểu đề cũ. Service phải khóa loại câu hỏi và nội dung phiên bản đã sử dụng.
- course_offerings.teacher_id và exams.created_by cho biết người phụ trách. Schema không biểu diễn việc giao reviewer độc lập. Trước khi mở quyền review ngoài chủ đề thi, nhóm cần thống nhất phạm vi quản trị và cách xác minh người được giao.
- Chốt công thức PARTIAL_CREDIT, cách tính lượt bị hủy, ưu tiên phân công lớp đồng thời và quyền đọc ngân hàng SHARED trước khi viết use case tương ứng. Lưu lựa chọn đã duyệt trong hợp đồng.
