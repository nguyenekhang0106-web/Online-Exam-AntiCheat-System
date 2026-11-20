# Thiết kế Database First và Java Bean

## Phạm vi và trạng thái

Đã viết 7 migration, 34 bảng, 60 FK, 34 Java Bean và helper dữ liệu phát triển.
Chưa thực thi SQL lên MySQL. Không có DAO/Service/Controller nghiệp vụ mới, không tích hợp Flyway,
không tự chạy migration khi deploy WAR. Giữ `pom.xml`, Java release 21, Maven Wrapper và Git remote.

Đích triển khai: MySQL 8.4+ (máy hiện có MySQL 9.6.0), InnoDB, `utf8mb4_0900_ai_ci`.
Không dùng MariaDB/MySQL 5.7; CHECK phải thực sự được thực thi, và cần functional index.
Các tệp V1–V7 không có CREATE DATABASE/USE/DROP để tránh tác động nhầm database.
Tên database ứng dụng dự kiến là `online_exam_db`; kiểm thử phải dùng database mới, riêng biệt sau khi được cho phép.

## Migration và dữ liệu mẫu

Thứ tự bắt buộc: V1 identity → V2 academic → V3 question → V4 exam → V5 attempt → V6 proctoring → V7 common.
Mỗi tệp chỉ chạy một lần. Không dùng IF NOT EXISTS để che giấu schema lệch; không có rollback phá hủy dữ liệu.
MySQL DDL auto-commit: nếu một migration lỗi giữa chừng, không chạy lại toàn bộ mù quáng.
Ghi lại migration đã chạy; trên database thử nghiệm chỉ tạo lại database mới khi có phép.
Không chạy bằng tài khoản ứng dụng có quyền DDL ở môi trường thật.

Sau khi được phép thử nghiệm trên database trống, quản trị viên tạo database bằng:

```sql
CREATE DATABASE online_exam_db_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

Tại CMD, từ repository, dùng tài khoản của bạn và nhập mật khẩu tại prompt:

```bat
mysql -u <db_user> -p --default-character-set=utf8mb4 online_exam_db_test < database\migrations\V1__create_identity_tables.sql
```

Lặp lại chính xác thứ tự V2–V7, dừng ngay nếu có lỗi; không dùng `--force`.
PowerShell không hỗ trợ cú pháp `<` như CMD; có thể dùng MySQL CLI tương tác với `source` và đường dẫn `/`,
nhưng phải dừng/ROLLBACK khi seed lỗi, không tiếp tục COMMIT sau lỗi.
Không đưa mật khẩu vào command line, source, URL JDBC hoặc Git.

Seed phát triển có 3 vai trò, 3 tài khoản được tạo riêng bằng helper, môn WEB101, lớp DEV_WEB,
lớp học phần, enrollment, topic, ngân hàng, câu hỏi một đáp án đúng, chính sách và đề DRAFT có một câu.
Đề phải qua kiểm tra/publish ở Service trước khi thi; cửa sổ phân công mẫu kéo dài 7 ngày từ lúc seed.

```powershell
.\mvnw.cmd clean package
java -cp target/classes scripts/SeedAccounts.java
```

Helper yêu cầu terminal tương tác, nhập và xác nhận 3 mật khẩu riêng (không hiện trên màn hình).
Helper chỉ ghi `.local/development-accounts.sql` đã ignore, không kết nối DB và không ghi đè file đã tồn tại.
Mật khẩu dùng PBKDF2-HMAC-SHA256, 600.000 vòng, salt ngẫu nhiên 16 byte, khóa 32 byte;
định dạng `pbkdf2-sha256$iterations$base64Salt$base64Hash` có verifier tại `PasswordHasher`.
Auth Service sau này phải gọi verifier này; hiện chưa có màn hình/API đăng nhập.
Không commit file SQL sinh ra dù chỉ chứa hash, không dùng các tài khoản mẫu ở production.

Sau khi có phép thử DB, chạy tại CMD:

```bat
mysql -u <db_user> -p --default-character-set=utf8mb4 online_exam_db_test < .local\development-accounts.sql
mysql -u <db_user> -p --default-character-set=utf8mb4 online_exam_db_test < database\seed\development_data.sql
```

Seed là one-shot trong transaction; chạy lại sẽ lỗi unique thay vì âm thầm đổi dữ liệu.
Nếu thiếu DEV_ADMIN/DEV_TEACHER/DEV_STUDENT, user_roles vi phạm NOT NULL và batch dừng;
đóng kết nối sẽ rollback transaction chưa commit. Seed không chứa mật khẩu rõ hay hash giả.
Vai trò production có thể được tạo riêng bằng ba bản ghi roles ở đầu seed; không chạy toàn bộ seed phát triển vào production.

## Kiểu dữ liệu, UTC và Bean

| SQL | Java |
| --- | --- |
| BIGINT | Long |
| SMALLINT | Short |
| INT | Integer |
| BOOLEAN | Boolean |
| DECIMAL | BigDecimal |
| DATETIME(3) | LocalDateTime, quy ước UTC |
| VARCHAR/CHAR/TEXT/LONGTEXT/JSON | String |

Tất cả field dùng object, kể cả NOT NULL để biểu diễn Bean chưa được gán hoặc ID chưa insert.
NOT NULL được đảm bảo ở DB và cần validation Service; Bean không tự điền default của DB.
DAO khi muốn dùng DEFAULT phải bỏ cột khỏi INSERT, không bind null vào cột NOT NULL.
Mỗi Bean có constructor rỗng/full, getter/setter, chỉ giữ FK ID, không Lombok/ORM/SQL/logic nghiệp vụ.
Không override toString; không tự đưa Bean chứa passwordHash/isCorrect/explanation/token hash ra JSP hay JSON.
DTO cho sinh viên phải whitelist field và tôn trọng chế độ công bố kết quả/đáp án.
`is_correct` ánh xạ `isCorrect` với `getIsCorrect()/setIsCorrect(Boolean)` đúng JavaBeans nullable.
Không thêm enum Java: các VARCHAR giữ String đúng đặc tả; giá trị hợp lệ được CHECK và liệt kê dưới đây.

DATETIME không tự mang timezone. Mỗi connection DAO cần `SET SESSION time_zone = '+00:00'`
trước khi dùng DEFAULT CURRENT_TIMESTAMP, và ứng dụng tạo LocalDateTime theo UTC.
Đọc/ghi JDBC 4.2 bằng getObject(..., LocalDateTime.class)/setObject; không chuyển qua timezone mặc định máy.
UI chuyển UTC sang múi giờ hiển thị. DECIMAL dùng getBigDecimal/setBigDecimal, không double.
Nullable số/Boolean đọc bằng getObject hoặc wasNull, không getLong/getBoolean đơn lẻ làm mất NULL.
JSON là chuỗi JSON hợp lệ; MySQL kiểm tra cú pháp nhưng Service kiểm tra cấu trúc/schema JSON.

## Ràng buộc và các quyết định bổ sung

- PK đúng đặc tả: roles.id là SMALLINT AI, các id đơn còn lại BIGINT AI; 5 bảng có PK ghép,
  gồm user_roles, class_students, course_enrollments, attempt_option_orders, attempt_answer_choices.
- Không thêm/xóa/đổi tên cột nghiệp vụ, không đổi NULLability. CHAR hash/UUID dùng ascii_bin;
  chuẩn hóa hash hex và UUID lowercase tại Service. Cột văn bản khác dùng collation không phân biệt hoa/dấu;
  email/code unique cũng theo quy tắc này, cần chuẩn hóa và thống nhất trước khi nhập dữ liệu thật.
- Tất cả FK dùng RESTRICT khi delete/update, kể cả bảng nối để ưu tiên giữ lịch sử.
  Không cascade. Khóa tài khoản/archive đề bằng trạng thái; không hard delete dữ liệu đã được tham chiếu.
- UNIQUE theo đặc tả đầy đủ. Thêm UNIQUE (id, parent_id) chỉ để làm đích FK ghép hợp chuẩn MySQL 8.4/9.6;
  không coi khóa tham chiếu không unique là hợp lệ. Không thêm index đơn nếu đã có prefix index phù hợp.
- Năm FK ghép thay thế FK đơn tương ứng: attachment.option_id + question_version_id;
  grade_adjustment.attempt_question_id + attempt_id; event.attempt_session_id + attempt_id;
  review.related_event_id + attempt_id; appeal.attempt_id + student_id.
  Nhờ đó không thể gắn nhầm phiên bản, lượt thi hoặc chủ lượt thi ở các quan hệ này.
  FK nullable vẫn giữ FK độc lập cho cột bắt buộc khi cột tùy chọn NULL.
- UNIQUE functional index `uq_exam_attempts_active` trên exam_id và CASE status=IN_PROGRESS THEN student_id
  chặn hai lượt đang hoạt động của cùng sinh viên/đề, kể cả request đồng thời. Các trạng thái khác trả NULL
  nên vẫn giữ nhiều lượt lịch sử. Không thêm cột generated hiển thị hoặc bảng thứ 35.
- max_attempts mặc định 1, >=1; override NULL hoặc >=1; attempt_no >=1; duration_minutes >=1;
  max_parallel_sessions >=1; points/size/order/ngưỡng giám sát >=0; tỷ lệ 0..100; total_score <= max_score khi cả hai có giá trị.
- BOOLEAN có CHECK IN(0,1), vì BOOLEAN MySQL là TINYINT và tự nó không giới hạn 0/1.
  Boolean nullable vẫn cho NULL. Boolean bắt buộc mặc định FALSE để không bật chính sách/trộn đề ngoài ý muốn.
- created_at/updated_at mặc định CURRENT_TIMESTAMP(3), updated_at ON UPDATE CURRENT_TIMESTAMP(3).
  Các thời điểm nghiệp vụ như started_at, assigned_at, received_at phải được caller truyền rõ.
  Cửa sổ thi phải closes_at > opens_at; deadline > started_at; các thời điểm kết thúc có CHECK sau mốc bắt đầu.

Các domain chưa được đặc tả đầy đủ được chọn và ràng buộc rõ như sau (cần migration mới nếu nhóm muốn đổi):

| Cột | Giá trị; mặc định nếu có |
| --- | --- |
| users.status | ACTIVE (default), INACTIVE, LOCKED |
| subjects/classes.status | ACTIVE (default), INACTIVE, ARCHIVED |
| class_students.status | ACTIVE (default), LEFT |
| course_offerings.status | ACTIVE (default), CLOSED, ARCHIVED |
| course_enrollments.status | ENROLLED (default), WITHDRAWN, COMPLETED |
| question_banks.visibility | PRIVATE (default), SHARED |
| questions.status | DRAFT (default), PUBLISHED, ARCHIVED |
| questions.question_type | SINGLE_CHOICE, MULTIPLE_CHOICE |
| questions.difficulty | EASY, MEDIUM, HARD |
| exams.status | DRAFT (default), PUBLISHED, CLOSED, ARCHIVED |
| exams.grading_policy | ALL_OR_NOTHING (default), PARTIAL_CREDIT |
| exams.attempt_score_policy | HIGHEST (default), LATEST, FIRST |
| exams.result_release_mode | IMMEDIATE, AFTER_CLOSE, MANUAL (default) |
| exams.answer_release_mode | NEVER (default), AFTER_CLOSE, MANUAL |
| exam_attempts.status | IN_PROGRESS (default), SUBMITTED, AUTO_SUBMITTED, CANCELLED, INVALIDATED |
| proctoring_events.severity | INFO, LOW, MEDIUM, HIGH, CRITICAL |
| violation_reviews.decision | VALID_EVENT, FALSE_POSITIVE, WARNING, INVALIDATE_ATTEMPT |
| exam_appeals.status | PENDING (default), REVIEWING, RESOLVED, REJECTED |

roles.role_code chỉ ADMIN/TEACHER/STUDENT. Các action_code/event_code/notification.type/submit_reason/
close_reason để mở, vì đặc tả chỉ nêu ví dụ hoặc chưa liệt kê hết; Service cần allowlist theo API.
Các ràng buộc trạng thái–timestamp phức tạp (ví dụ đã graded phải đủ điểm, CLOSED phải kết thúc)
phải được Service xử lý nguyên tử; CHECK hiện tại chỉ bảo vệ các bất biến đơn hàng được nêu.

## Quy tắc liên bảng và đồng thời cho Service

### Bắt đầu/resume lượt thi

1. Xác thực STUDENT từ session server, bỏ qua studentId do client tự khai. Begin transaction.
2. Lock hàng users của sinh viên bằng SELECT ... FOR UPDATE làm khóa ổn định kể cả chưa có attempt.
   Sau đó lock exam/phân công theo cùng một thứ tự trong mọi luồng; giữ cấu hình đề/phân công bất biến sau publish.
   Quy tắc này làm serialize thao tác bắt đầu thi của cùng sinh viên; có thể tối ưu sau bằng cơ chế lock chuyên biệt.
3. Đề phải PUBLISHED. Nếu có phân công cá nhân thì cửa sổ cá nhân quyết định quyền vào thi;
   ngoài cửa sổ này không fallback sang lớp để vượt giới hạn. Nếu không có phân công cá nhân,
   phải có course_enrollment ENROLLED và phân công lớp đang mở.
4. Trong các phân công lớp hợp lệ, chọn ID exam_course_assignments nhỏ nhất (deterministic),
   không cộng lượt, không lấy override lớn nhất. NULL override cá nhân fallback sang override của lớp được chọn,
   rồi exams.max_attempts. Nếu có cá nhân nhưng không có lớp hợp lệ thì dùng mặc định đề.
   Không đổi phân công/enrollment ảnh hưởng quyền thi khi đã có lượt; nếu cần đổi phải có quy trình riêng có audit.
5. Kiểm tra attempt IN_PROGRESS: resume cùng lượt; nếu deadline đã qua thì chuyển AUTO_SUBMITTED/chấm trong transaction
   trước khi xét lượt mới. Không tạo lại attempt chỉ vì client mất mạng.
6. Đếm tất cả lượt đã tạo, kể cả CANCELLED/INVALIDATED để chống xin hủy nhằm lấy thêm lượt.
   So với max hợp lệ; attempt_no = max(attempt_no)+1, không tái sử dụng số cũ. UNIQUE là hàng rào cuối chống race.
   Request start lặp lại khi lượt còn mở trả lại lượt đó; client không được dùng retry start để tự mở lượt tiếp theo sau khi đã nộp.
   Đặc tả chưa có idempotency key start nên muốn đảm bảo retry sau khi hoàn tất phải thêm cơ chế idempotency có lưu bền trong giai đoạn sau.
7. deadline_at = min(started_at + duration_minutes, closes_at phân công được chọn).
   Tạo attempt_questions/attempt_option_orders đầy đủ, điểm snapshot, thứ tự shuffle ở backend trong cùng transaction.
   Commit rồi mới trả dữ liệu an toàn cho client. Bắt duplicate/deadlock và retry có giới hạn, không tăng số lượt ngoài transaction.

### Câu hỏi, đáp án, chấm và nộp bài

- topics.parent_topic_id phải cùng subject, không tạo chu trình; questions.topic_id phải cùng môn với bank;
  question_version trong đề phải cùng subject của exam; offering và exam phải cùng môn.
  Những liên kết này thiếu cột chung để FK ghép mà không đổi đặc tả, nên Service phải kiểm tra.
- Không UPDATE/DELETE question_versions/options/attachments đã xuất bản hoặc đã được tham chiếu;
  tạo version_no mới dưới lock question. SINGLE_CHOICE đúng một is_correct, MULTIPLE_CHOICE ít nhất một;
  mỗi câu đủ lựa chọn. questions.question_type là thuộc tính ở câu hỏi gốc, phải bất biến sau publish,
  vì đổi loại sẽ làm thay đổi cách hiểu các phiên bản cũ.
- Đề publish phải có câu hỏi, tổng điểm >0, snapshot đầy đủ chính sách; khóa exam_questions và cấu hình chấm/thi
  sau publish. Schema không lưu lịch sử tất cả cấu hình đề, nên việc sửa trực tiếp sẽ phá lịch sử dù câu hỏi có version.
- attempt_questions phải tham chiếu exam_question của đúng exam của attempt.
  attempt_option_orders.option_id phải thuộc question_version của exam_question đó.
  attempt_answer_choices.option_id phải nằm trong tập option của đúng attempt_question và version đó.
  FK đơn chỉ xác nhận ID tồn tại, không chứng minh toàn bộ đường JOIN; Service phải kiểm tra bằng JOIN trong transaction.
  Mỗi SINGLE_CHOICE chỉ có tối đa một lựa chọn (PK ghép của choices không tự giới hạn số hàng).
- autosave/submit đều lock cùng exam_attempt trước, rồi mới sửa answers/choices; chỉ cho ghi khi IN_PROGRESS
  và còn hạn theo clock server. Request autosave lặp lại dùng UPSERT theo attempt_question_id và thay tập lựa chọn
  trong transaction; không insert hai answer. Chặn autosave đến muộn sau submit.
- submit dùng chuyển trạng thái có điều kiện `WHERE status='IN_PROGRESS'`, kiểm tra affected rows;
  request trùng trả kết quả đã có, không chấm/cộng điểm lần hai. Deadline do server quyết định; không tin thời gian client.
- Không nhận total_score, awarded_points, is_correct, passed từ trình duyệt. Backend tính lại từ đáp án đã lưu.
  ALL_OR_NOTHING: tập chọn phải đúng toàn bộ tập đáp án đúng.
  Đề xuất PARTIAL_CREDIT: points * max(0, correctSelected/correctCount - wrongSelected/wrongCount),
  phần wrong bằng 0 nếu không có lựa chọn sai; làm tròn HALF_UP 2 chữ số mỗi câu.
  Clamp 0..points_snapshot; cộng điểm, percentage=total/max*100 (2 chữ số), passed so với pass_percentage.
  Công thức là quyết định cần nhóm chốt trước khi code Service, chưa có trong Bean/SQL.
- grade_adjustments phải cùng attempt (FK ghép đã chặn), giới hạn new_points theo points_snapshot,
  lock attempt, ghi audit old/new rồi cập nhật answer và tổng điểm nguyên tử. Không ghi đè lịch sử điều chỉnh.
- HIGHEST/LATEST/FIRST chỉ chọn lượt đã nộp và graded, không CANCELLED/INVALIDATED;
  tie-break HIGHEST bằng attempt_no nhỏ nhất. Không có bảng kết quả tổng hợp riêng nên tính qua query Service/DAO.

### Giám sát, công bố và phúc khảo

- max_parallel_sessions phải kiểm tra dưới lock attempt khi mở/đóng phiên; FK không giới hạn số hàng active.
  session_token_hash lưu digest token ngẫu nhiên, không raw token; password dùng password KDF riêng.
- Event có client_event_id thì UNIQUE(attempt_id, client_event_id) hỗ trợ retry idempotent;
  NULL được phép nhiều lần cho sự kiện server. Retry cùng ID không ghi đè nội dung cũ; kiểm tra xung đột payload.
- Client event/time không đáng tin; server_received_at do backend đặt. Dấu hiệu tab/window/paste không phải kết luận gian lận.
  Cần reviewer có quyền, lý do và audit để INVALIDATE_ATTEMPT; Bean không quyết định điều này.
- Evidence lưu storage_key, không public URL; kiểm tra MIME/kích thước/hash tại upload, áp quyền tải,
  worker hết hạn xóa object rồi cập nhật deleted_at, vẫn giữ metadata phục vụ audit.
- AFTER_CLOSE phải xét cửa sổ phân công hợp lệ của chính sinh viên; không công bố đáp án khi còn cửa sổ/lượt thi
  cần bảo vệ. MANUAL chưa có cột release timestamp/flag trong đặc tả: cần bổ sung migration hoặc audit event
  được định nghĩa rõ trước khi triển khai; không tự coi MANUAL là đã công bố.
- exam_appeals FK ghép bảo đảm student đúng chủ attempt. Service kiểm tra thời hạn, quyền reviewer,
  nội dung, trạng thái và số phúc khảo được phép. Schema hiện cho phép nhiều phúc khảo một lượt.
- audit_logs.target_type/target_id là tham chiếu đa hình, không FK được tới nhiều bảng; Service xác thực đích.
  append-only log phải được bảo vệ bằng quyền DB/DAO, FK RESTRICT không ngăn UPDATE/DELETE trực tiếp chính hàng log.

## Kiểm thử và bước tiếp theo

```powershell
.\mvnw.cmd clean package
node scripts/verify-database.cjs
java -cp target/classes scripts/VerifyBeans.java
```

Node chỉ phục vụ kiểm tra tĩnh, không cần cho build/chạy Java; script không cần npm install.
Chạy checker sau clean/package vì contract tạm được ghi trong target.
Checker đọc SQL, đối chiếu tên/kiểu/thứ tự field Bean, PK/FK và thứ tự tạo bảng, đích UNIQUE của FK.
VerifyBeans dùng reflection và JavaBeans Introspector để kiểm tra constructor/accessor/NULL và password verifier.
Đây không phải kiểm chứng SQL engine. Chưa có quyền chạy database trống nên không kết luận migration/seed đã chạy thành công.

Khi được phép thử MySQL: kiểm tra đủ 34 BASE TABLE, insert seed, thử lỗi duplicate email/FK sai/điểm âm,
max_attempts=0, override=0, percentage>100, Boolean=2, cửa sổ ngược; thử FK ghép sai attempt/version/student.
Dùng hai connection cạnh tranh tạo IN_PROGRESS để xác nhận chỉ một thành công; đổi lượt cũ sang SUBMITTED rồi thử lượt mới.
Kiểm tra hai event cùng client_event_id và cùng attempt bị chặn, nhiều NULL vẫn hợp lệ.
Kiểm tra DELETE user/question/exam đang có lịch sử bị RESTRICT. Không thực hiện những test này trên DB có dữ liệu thật.

Tiếp theo: chốt domain trạng thái và công thức điểm; cho phép thử DB riêng; viết JDBC connection factory cấu hình ngoài repo,
UTC/transaction helper; DAO mapping; Auth Service dùng PasswordHasher; sau đó ExamAttemptService với locking,
DTO an toàn và integration tests concurrency. Thống nhất quy trình versioning migration trước khi cả nhóm cùng sửa schema.

Tham khảo: [MySQL functional indexes](https://dev.mysql.com/doc/refman/8.4/en/create-index.html),
[MySQL CHECK constraints](https://dev.mysql.com/doc/refman/8.4/en/create-table-check-constraints.html),
[OWASP password storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html).

## Danh mục bảng và khóa ngoại

Danh mục chi tiết được liệt kê bên dưới; ERD đầy đủ tại [online_exam_erd.md](../database/erd/online_exam_erd.md).

| Bảng | Bean | Mục đích | PK | UNIQUE nghiệp vụ |
| --- | --- | --- | --- | --- |
| users | User | Tài khoản, danh tính và trạng thái truy cập | id | user_code; email |
| roles | Role | Danh mục ba vai trò hệ thống | id | role_code |
| user_roles | UserRole | Vai trò của người dùng và người gán | user_id, role_id | — |
| password_reset_tokens | PasswordResetToken | Token khôi phục mật khẩu đã băm, hạn và thời điểm dùng | id | token_hash |
| login_sessions | LoginSession | Phiên đăng nhập và thu hồi | id | session_hash |
| audit_logs | AuditLog | Nhật ký hành động và đích đa hình | id | — |
| subjects | Subject | Danh mục môn học | id | subject_code |
| classes | SchoolClass | Lớp hành chính | id | class_code |
| class_students | ClassStudent | Quan hệ thành viên lớp | class_id, student_id | — |
| course_offerings | CourseOffering | Lớp học phần theo môn, lớp, giáo viên, kỳ | id | — |
| course_enrollments | CourseEnrollment | Sinh viên đăng ký lớp học phần | offering_id, student_id | — |
| topics | Topic | Cây chủ đề trong môn | id | subject_id, topic_code |
| question_banks | QuestionBank | Ngân hàng câu hỏi theo chủ sở hữu | id | — |
| questions | Question | Danh tính câu hỏi, loại và độ khó | id | — |
| question_versions | QuestionVersion | Nội dung câu hỏi có phiên bản | id | question_id, version_no |
| question_version_options | QuestionVersionOption | Các lựa chọn của một phiên bản | id | question_version_id, display_order |
| question_attachments | QuestionAttachment | Tệp đính kèm phiên bản hoặc lựa chọn | id | — |
| proctoring_policies | ProctoringPolicy | Cấu hình giám sát để snapshot vào đề | id | — |
| exams | Exam | Cấu hình đề thi và quy tắc công bố/chấm | id | — |
| exam_questions | ExamQuestion | Một phiên bản câu hỏi trong một đề | id | exam_id, question_version_id; exam_id, fixed_order |
| exam_course_assignments | ExamCourseAssignment | Phân công thi cho lớp học phần | id | exam_id, offering_id |
| exam_student_assignments | ExamStudentAssignment | Phân công riêng cho sinh viên | id | exam_id, student_id |
| exam_attempts | ExamAttempt | Một lượt thi, trạng thái và kết quả | id | exam_id, student_id, attempt_no |
| attempt_questions | AttemptQuestion | Câu hỏi/thứ tự/điểm snapshot cho lượt thi | id | attempt_id, exam_question_id; attempt_id, display_order |
| attempt_option_orders | AttemptOptionOrder | Thứ tự lựa chọn sau khi trộn | attempt_question_id, option_id | attempt_question_id, display_order |
| attempt_answers | AttemptAnswer | Câu trả lời tổng cho một câu trong lượt thi | id | attempt_question_id |
| attempt_answer_choices | AttemptAnswerChoice | Tập lựa chọn sinh viên đã đánh dấu | answer_id, option_id | — |
| grade_adjustments | GradeAdjustment | Lịch sử điều chỉnh điểm có người chấm/lý do | id | — |
| attempt_sessions | AttemptSession | Phiên thiết bị trong một lượt thi | id | session_token_hash |
| proctoring_events | ProctoringEvent | Dấu hiệu giám sát có khóa chống gửi trùng | id | attempt_id, client_event_id |
| proctoring_evidence | ProctoringEvidence | Metadata bằng chứng, thời hạn và xóa vật lý | id | — |
| violation_reviews | ViolationReview | Kết quả đánh giá dấu hiệu bởi người có quyền | id | — |
| notifications | Notification | Thông báo theo người nhận | id | — |
| exam_appeals | ExamAppeal | Đơn phúc khảo của chủ lượt thi | id | — |

### Toàn bộ 60 FK

Tất cả ON DELETE/UPDATE RESTRICT. Các cặp cột ghi cùng một ô tạo thành một FK ghép.

| Bảng con | Cột FK | Bảng cha | Cột đích |
| --- | --- | --- | --- |
| user_roles | user_id | users | id |
| user_roles | role_id | roles | id |
| user_roles | assigned_by | users | id |
| password_reset_tokens | user_id | users | id |
| login_sessions | user_id | users | id |
| audit_logs | actor_user_id | users | id |
| class_students | class_id | classes | id |
| class_students | student_id | users | id |
| course_offerings | subject_id | subjects | id |
| course_offerings | class_id | classes | id |
| course_offerings | teacher_id | users | id |
| course_enrollments | offering_id | course_offerings | id |
| course_enrollments | student_id | users | id |
| topics | subject_id | subjects | id |
| topics | parent_topic_id | topics | id |
| question_banks | subject_id | subjects | id |
| question_banks | owner_id | users | id |
| questions | bank_id | question_banks | id |
| questions | topic_id | topics | id |
| questions | author_id | users | id |
| question_versions | question_id | questions | id |
| question_versions | created_by | users | id |
| question_version_options | question_version_id | question_versions | id |
| question_attachments | question_version_id | question_versions | id |
| question_attachments | option_id, question_version_id | question_version_options | id, question_version_id |
| proctoring_policies | created_by | users | id |
| exams | subject_id | subjects | id |
| exams | created_by | users | id |
| exams | policy_id | proctoring_policies | id |
| exam_questions | exam_id | exams | id |
| exam_questions | question_version_id | question_versions | id |
| exam_course_assignments | exam_id | exams | id |
| exam_course_assignments | offering_id | course_offerings | id |
| exam_course_assignments | assigned_by | users | id |
| exam_student_assignments | exam_id | exams | id |
| exam_student_assignments | student_id | users | id |
| exam_student_assignments | assigned_by | users | id |
| exam_attempts | exam_id | exams | id |
| exam_attempts | student_id | users | id |
| attempt_questions | attempt_id | exam_attempts | id |
| attempt_questions | exam_question_id | exam_questions | id |
| attempt_option_orders | attempt_question_id | attempt_questions | id |
| attempt_option_orders | option_id | question_version_options | id |
| attempt_answers | attempt_question_id | attempt_questions | id |
| attempt_answer_choices | answer_id | attempt_answers | id |
| attempt_answer_choices | option_id | question_version_options | id |
| grade_adjustments | attempt_id | exam_attempts | id |
| grade_adjustments | attempt_question_id, attempt_id | attempt_questions | id, attempt_id |
| grade_adjustments | grader_id | users | id |
| attempt_sessions | attempt_id | exam_attempts | id |
| proctoring_events | attempt_id | exam_attempts | id |
| proctoring_events | attempt_session_id, attempt_id | attempt_sessions | id, attempt_id |
| proctoring_evidence | event_id | proctoring_events | id |
| violation_reviews | attempt_id | exam_attempts | id |
| violation_reviews | related_event_id, attempt_id | proctoring_events | id, attempt_id |
| violation_reviews | reviewer_id | users | id |
| notifications | recipient_id | users | id |
| exam_appeals | attempt_id, student_id | exam_attempts | id, student_id |
| exam_appeals | student_id | users | id |
| exam_appeals | reviewer_id | users | id |
