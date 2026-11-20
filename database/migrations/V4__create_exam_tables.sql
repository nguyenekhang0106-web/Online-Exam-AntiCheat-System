-- MySQL 8.4+ / 9.6; execute once, in V1..V7 order, on an explicitly selected EMPTY database.
-- No CREATE DATABASE, USE, DROP, or automatic migration runner. DDL auto-commits.
SET NAMES utf8mb4;
SET SESSION time_zone = '+00:00';

-- ProctoringPolicy: proctoring_policies
CREATE TABLE proctoring_policies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    policy_name VARCHAR(150) NOT NULL,
    require_fullscreen BOOLEAN NOT NULL DEFAULT FALSE,
    warn_on_focus_loss BOOLEAN NOT NULL DEFAULT FALSE,
    detect_paste BOOLEAN NOT NULL DEFAULT FALSE,
    max_tab_switches INT NOT NULL,
    max_fullscreen_exits INT NOT NULL,
    max_parallel_sessions SMALLINT NOT NULL,
    require_camera BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX ix_proctoring_policies_1 (created_by),
    CONSTRAINT fk_proctoring_policies_1 FOREIGN KEY (created_by)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_proctoring_policies_1 CHECK (max_parallel_sessions >= 1),
    CONSTRAINT ck_proctoring_policies_2 CHECK (require_fullscreen IN (0, 1)),
    CONSTRAINT ck_proctoring_policies_3 CHECK (warn_on_focus_loss IN (0, 1)),
    CONSTRAINT ck_proctoring_policies_4 CHECK (detect_paste IN (0, 1)),
    CONSTRAINT ck_proctoring_policies_5 CHECK (max_tab_switches >= 0),
    CONSTRAINT ck_proctoring_policies_6 CHECK (max_fullscreen_exits >= 0),
    CONSTRAINT ck_proctoring_policies_7 CHECK (require_camera IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Exam: exams
CREATE TABLE exams (
    id BIGINT NOT NULL AUTO_INCREMENT,
    subject_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    instructions TEXT NULL,
    duration_minutes SMALLINT NOT NULL,
    max_attempts SMALLINT NOT NULL DEFAULT 1,
    pass_percentage DECIMAL(5,2) NOT NULL,
    shuffle_questions BOOLEAN NOT NULL DEFAULT FALSE,
    shuffle_options BOOLEAN NOT NULL DEFAULT FALSE,
    grading_policy VARCHAR(30) NOT NULL DEFAULT 'ALL_OR_NOTHING',
    attempt_score_policy VARCHAR(20) NOT NULL DEFAULT 'HIGHEST',
    result_release_mode VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    answer_release_mode VARCHAR(30) NOT NULL DEFAULT 'NEVER',
    policy_id BIGINT NULL,
    policy_snapshot_json JSON NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    published_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    INDEX ix_exams_1 (subject_id),
    CONSTRAINT fk_exams_1 FOREIGN KEY (subject_id)
        REFERENCES subjects (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_exams_2 (created_by),
    CONSTRAINT fk_exams_2 FOREIGN KEY (created_by)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_exams_3 (policy_id),
    CONSTRAINT fk_exams_3 FOREIGN KEY (policy_id)
        REFERENCES proctoring_policies (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_exams_1 CHECK (duration_minutes >= 1),
    CONSTRAINT ck_exams_2 CHECK (max_attempts >= 1),
    CONSTRAINT ck_exams_3 CHECK (attempt_score_policy IN ('HIGHEST','LATEST','FIRST')),
    CONSTRAINT ck_exams_4 CHECK (grading_policy IN ('ALL_OR_NOTHING','PARTIAL_CREDIT')),
    CONSTRAINT ck_exams_5 CHECK (status IN ('DRAFT','PUBLISHED','CLOSED','ARCHIVED')),
    CONSTRAINT ck_exams_6 CHECK (result_release_mode IN ('IMMEDIATE','AFTER_CLOSE','MANUAL')),
    CONSTRAINT ck_exams_7 CHECK (answer_release_mode IN ('NEVER','AFTER_CLOSE','MANUAL')),
    CONSTRAINT ck_exams_8 CHECK (published_at IS NULL OR published_at >= created_at),
    CONSTRAINT ck_exams_9 CHECK (pass_percentage BETWEEN 0 AND 100),
    CONSTRAINT ck_exams_10 CHECK (shuffle_questions IN (0, 1)),
    CONSTRAINT ck_exams_11 CHECK (shuffle_options IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ExamQuestion: exam_questions
CREATE TABLE exam_questions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    question_version_id BIGINT NOT NULL,
    points DECIMAL(7,2) NOT NULL,
    fixed_order INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_exam_questions_1 UNIQUE (exam_id, question_version_id),
    CONSTRAINT uq_exam_questions_2 UNIQUE (exam_id, fixed_order),
    CONSTRAINT fk_exam_questions_1 FOREIGN KEY (exam_id)
        REFERENCES exams (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_exam_questions_1 (question_version_id),
    CONSTRAINT fk_exam_questions_2 FOREIGN KEY (question_version_id)
        REFERENCES question_versions (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_exam_questions_1 CHECK (points >= 0),
    CONSTRAINT ck_exam_questions_2 CHECK (fixed_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ExamCourseAssignment: exam_course_assignments
CREATE TABLE exam_course_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    offering_id BIGINT NOT NULL,
    opens_at DATETIME(3) NOT NULL,
    closes_at DATETIME(3) NOT NULL,
    max_attempts_override SMALLINT NULL,
    assigned_by BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uq_exam_course_assignments_1 UNIQUE (exam_id, offering_id),
    INDEX ix_exam_course_assignments_1 (offering_id, opens_at, closes_at),
    CONSTRAINT fk_exam_course_assignments_1 FOREIGN KEY (exam_id)
        REFERENCES exams (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_exam_course_assignments_2 FOREIGN KEY (offering_id)
        REFERENCES course_offerings (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_exam_course_assignments_2 (assigned_by),
    CONSTRAINT fk_exam_course_assignments_3 FOREIGN KEY (assigned_by)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_exam_course_assignments_1 CHECK (closes_at > opens_at),
    CONSTRAINT ck_exam_course_assignments_2 CHECK (max_attempts_override IS NULL OR max_attempts_override >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ExamStudentAssignment: exam_student_assignments
CREATE TABLE exam_student_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    opens_at DATETIME(3) NOT NULL,
    closes_at DATETIME(3) NOT NULL,
    max_attempts_override SMALLINT NULL,
    assigned_by BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uq_exam_student_assignments_1 UNIQUE (exam_id, student_id),
    INDEX ix_exam_student_assignments_1 (student_id, opens_at, closes_at),
    CONSTRAINT fk_exam_student_assignments_1 FOREIGN KEY (exam_id)
        REFERENCES exams (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_exam_student_assignments_2 FOREIGN KEY (student_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_exam_student_assignments_2 (assigned_by),
    CONSTRAINT fk_exam_student_assignments_3 FOREIGN KEY (assigned_by)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_exam_student_assignments_1 CHECK (closes_at > opens_at),
    CONSTRAINT ck_exam_student_assignments_2 CHECK (max_attempts_override IS NULL OR max_attempts_override >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
