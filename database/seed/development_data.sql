-- Development only, run ONCE after V1..V7 and .local/development-accounts.sql.
-- No passwords or fixed account hashes. Missing DEV_* users causes NOT NULL failure.
-- Run in mysql batch mode WITHOUT --force; stop on error and ROLLBACK.
SET NAMES utf8mb4;
SET SESSION time_zone = '+00:00';
START TRANSACTION;

INSERT INTO roles (role_code, role_name, description) VALUES
('ADMIN', 'Quản trị viên', 'Quản trị tài khoản và hệ thống'),
('TEACHER', 'Giảng viên', 'Quản lý câu hỏi và đề thi'),
('STUDENT', 'Sinh viên', 'Tham gia thi và xem kết quả được phép');

SET @admin_id = (SELECT id FROM users WHERE user_code = 'DEV_ADMIN');
SET @teacher_id = (SELECT id FROM users WHERE user_code = 'DEV_TEACHER');
SET @student_id = (SELECT id FROM users WHERE user_code = 'DEV_STUDENT');
INSERT INTO user_roles (user_id, role_id, assigned_at, assigned_by) VALUES
(@admin_id, (SELECT id FROM roles WHERE role_code = 'ADMIN'), UTC_TIMESTAMP(3), NULL),
(@teacher_id, (SELECT id FROM roles WHERE role_code = 'TEACHER'), UTC_TIMESTAMP(3), @admin_id),
(@student_id, (SELECT id FROM roles WHERE role_code = 'STUDENT'), UTC_TIMESTAMP(3), @admin_id);

INSERT INTO subjects (subject_code, subject_name) VALUES ('WEB101', 'Công nghệ Web');
SET @subject_id = LAST_INSERT_ID();
INSERT INTO classes (class_code, class_name, academic_year) VALUES ('DEV_WEB', 'Lớp thử nghiệm Web', '2026-2027');
SET @class_id = LAST_INSERT_ID();
INSERT INTO class_students (class_id, student_id, joined_at) VALUES (@class_id, @student_id, UTC_TIMESTAMP(3));
INSERT INTO course_offerings (subject_id, class_id, teacher_id, academic_year, semester)
VALUES (@subject_id, @class_id, @teacher_id, '2026-2027', '1');
SET @offering_id = LAST_INSERT_ID();
INSERT INTO course_enrollments (offering_id, student_id, enrolled_at)
VALUES (@offering_id, @student_id, UTC_TIMESTAMP(3));

INSERT INTO topics (subject_id, topic_code, topic_name, display_order, created_at)
VALUES (@subject_id, 'HTTP', 'HTTP và Servlet', 1, UTC_TIMESTAMP(3));
SET @topic_id = LAST_INSERT_ID();
INSERT INTO question_banks (subject_id, owner_id, bank_name, visibility)
VALUES (@subject_id, @teacher_id, 'Ngân hàng câu hỏi Web mẫu', 'PRIVATE');
SET @bank_id = LAST_INSERT_ID();
INSERT INTO questions (bank_id, topic_id, author_id, question_type, difficulty, status)
VALUES (@bank_id, @topic_id, @teacher_id, 'SINGLE_CHOICE', 'EASY', 'PUBLISHED');
SET @question_id = LAST_INSERT_ID();
INSERT INTO question_versions (question_id, version_no, content, explanation, default_points, created_by)
VALUES (@question_id, 1, 'Phương thức HTTP nào dùng để đọc tài nguyên?', 'GET dùng để đọc tài nguyên.', 10.00, @teacher_id);
SET @version_id = LAST_INSERT_ID();
INSERT INTO question_version_options (question_version_id, option_content, is_correct, display_order) VALUES
(@version_id, 'GET', TRUE, 1), (@version_id, 'POST', FALSE, 2), (@version_id, 'DELETE', FALSE, 3);

INSERT INTO proctoring_policies (policy_name, require_fullscreen, warn_on_focus_loss, detect_paste,
    max_tab_switches, max_fullscreen_exits, max_parallel_sessions, require_camera, created_by)
VALUES ('Chính sách thử nghiệm', TRUE, TRUE, TRUE, 3, 2, 1, FALSE, @teacher_id);
SET @policy_id = LAST_INSERT_ID();
INSERT INTO exams (subject_id, created_by, title, instructions, duration_minutes, max_attempts,
    pass_percentage, shuffle_questions, shuffle_options, grading_policy, attempt_score_policy,
    result_release_mode, answer_release_mode, policy_id, policy_snapshot_json, status)
VALUES (@subject_id, @teacher_id, 'Đề thi Web mẫu', 'Chọn một đáp án đúng.', 15, 1,
    50.00, TRUE, TRUE, 'ALL_OR_NOTHING', 'HIGHEST', 'MANUAL', 'NEVER', @policy_id,
    JSON_OBJECT('require_fullscreen', TRUE, 'warn_on_focus_loss', TRUE, 'detect_paste', TRUE,
        'max_tab_switches', 3, 'max_fullscreen_exits', 2, 'max_parallel_sessions', 1, 'require_camera', FALSE), 'DRAFT');
SET @exam_id = LAST_INSERT_ID();
INSERT INTO exam_questions (exam_id, question_version_id, points, fixed_order)
VALUES (@exam_id, @version_id, 10.00, 1);
INSERT INTO exam_course_assignments (exam_id, offering_id, opens_at, closes_at, assigned_by)
VALUES (@exam_id, @offering_id, UTC_TIMESTAMP(3), DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 7 DAY), @teacher_id);
COMMIT;
-- Draft must be validated and published by the future Service before an attempt can start.
