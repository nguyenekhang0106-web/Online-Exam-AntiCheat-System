-- MySQL 8.4+ / 9.6; execute once, in V1..V7 order, on an explicitly selected EMPTY database.
-- No CREATE DATABASE, USE, DROP, or automatic migration runner. DDL auto-commits.
SET NAMES utf8mb4;
SET SESSION time_zone = '+00:00';

-- ExamAttempt: exam_attempts
CREATE TABLE exam_attempts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    attempt_no SMALLINT NOT NULL,
    started_at DATETIME(3) NOT NULL,
    deadline_at DATETIME(3) NOT NULL,
    submitted_at DATETIME(3) NULL,
    last_saved_at DATETIME(3) NULL,
    status VARCHAR(25) NOT NULL DEFAULT 'IN_PROGRESS',
    submit_reason VARCHAR(30) NULL,
    total_score DECIMAL(9,2) NULL,
    max_score DECIMAL(9,2) NULL,
    percentage DECIMAL(5,2) NULL,
    passed BOOLEAN NULL,
    graded_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_exam_attempts_1 UNIQUE (exam_id, student_id, attempt_no),
    CONSTRAINT uq_exam_attempts_2 UNIQUE (id, student_id),
    INDEX ix_exam_attempts_1 (student_id, status, started_at),
    CONSTRAINT fk_exam_attempts_1 FOREIGN KEY (exam_id)
        REFERENCES exams (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_exam_attempts_2 FOREIGN KEY (student_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_exam_attempts_1 CHECK (attempt_no >= 1),
    CONSTRAINT ck_exam_attempts_2 CHECK (deadline_at > started_at),
    CONSTRAINT ck_exam_attempts_3 CHECK (submitted_at IS NULL OR submitted_at >= started_at),
    CONSTRAINT ck_exam_attempts_4 CHECK (last_saved_at IS NULL OR last_saved_at >= started_at),
    CONSTRAINT ck_exam_attempts_5 CHECK (graded_at IS NULL OR (submitted_at IS NOT NULL AND graded_at >= submitted_at)),
    CONSTRAINT ck_exam_attempts_6 CHECK (status IN ('IN_PROGRESS','SUBMITTED','AUTO_SUBMITTED','CANCELLED','INVALIDATED')),
    CONSTRAINT ck_exam_attempts_7 CHECK (total_score IS NULL OR max_score IS NULL OR total_score <= max_score),
    CONSTRAINT ck_exam_attempts_8 CHECK (total_score >= 0),
    CONSTRAINT ck_exam_attempts_9 CHECK (max_score >= 0),
    CONSTRAINT ck_exam_attempts_10 CHECK (percentage BETWEEN 0 AND 100),
    CONSTRAINT ck_exam_attempts_11 CHECK (passed IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- NULL keys allow historical attempts; only IN_PROGRESS occupies the unique slot.
CREATE UNIQUE INDEX uq_exam_attempts_active ON exam_attempts
    (exam_id, (CASE WHEN status = 'IN_PROGRESS' THEN student_id ELSE NULL END));

-- AttemptQuestion: attempt_questions
CREATE TABLE attempt_questions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    attempt_id BIGINT NOT NULL,
    exam_question_id BIGINT NOT NULL,
    display_order INT NOT NULL,
    points_snapshot DECIMAL(7,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_attempt_questions_1 UNIQUE (attempt_id, exam_question_id),
    CONSTRAINT uq_attempt_questions_2 UNIQUE (attempt_id, display_order),
    CONSTRAINT uq_attempt_questions_3 UNIQUE (id, attempt_id),
    CONSTRAINT fk_attempt_questions_1 FOREIGN KEY (attempt_id)
        REFERENCES exam_attempts (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_attempt_questions_1 (exam_question_id),
    CONSTRAINT fk_attempt_questions_2 FOREIGN KEY (exam_question_id)
        REFERENCES exam_questions (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_attempt_questions_1 CHECK (display_order >= 0),
    CONSTRAINT ck_attempt_questions_2 CHECK (points_snapshot >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- AttemptOptionOrder: attempt_option_orders
CREATE TABLE attempt_option_orders (
    attempt_question_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    display_order SMALLINT NOT NULL,
    PRIMARY KEY (attempt_question_id, option_id),
    CONSTRAINT uq_attempt_option_orders_1 UNIQUE (attempt_question_id, display_order),
    CONSTRAINT fk_attempt_option_orders_1 FOREIGN KEY (attempt_question_id)
        REFERENCES attempt_questions (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_attempt_option_orders_1 (option_id),
    CONSTRAINT fk_attempt_option_orders_2 FOREIGN KEY (option_id)
        REFERENCES question_version_options (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_attempt_option_orders_1 CHECK (display_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- AttemptAnswer: attempt_answers
CREATE TABLE attempt_answers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    attempt_question_id BIGINT NOT NULL,
    last_saved_at DATETIME(3) NOT NULL,
    graded_at DATETIME(3) NULL,
    awarded_points DECIMAL(7,2) NULL,
    is_correct BOOLEAN NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uq_attempt_answers_1 UNIQUE (attempt_question_id),
    CONSTRAINT fk_attempt_answers_1 FOREIGN KEY (attempt_question_id)
        REFERENCES attempt_questions (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_attempt_answers_1 CHECK (graded_at IS NULL OR graded_at >= last_saved_at),
    CONSTRAINT ck_attempt_answers_2 CHECK (awarded_points >= 0),
    CONSTRAINT ck_attempt_answers_3 CHECK (is_correct IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- AttemptAnswerChoice: attempt_answer_choices
CREATE TABLE attempt_answer_choices (
    answer_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    selected_at DATETIME(3) NOT NULL,
    PRIMARY KEY (answer_id, option_id),
    CONSTRAINT fk_attempt_answer_choices_1 FOREIGN KEY (answer_id)
        REFERENCES attempt_answers (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_attempt_answer_choices_1 (option_id),
    CONSTRAINT fk_attempt_answer_choices_2 FOREIGN KEY (option_id)
        REFERENCES question_version_options (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- GradeAdjustment: grade_adjustments
CREATE TABLE grade_adjustments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    attempt_id BIGINT NOT NULL,
    attempt_question_id BIGINT NOT NULL,
    grader_id BIGINT NOT NULL,
    old_points DECIMAL(9,2) NOT NULL,
    new_points DECIMAL(9,2) NOT NULL,
    reason TEXT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX ix_grade_adjustments_1 (attempt_id),
    CONSTRAINT fk_grade_adjustments_1 FOREIGN KEY (attempt_id)
        REFERENCES exam_attempts (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_grade_adjustments_2 (attempt_question_id, attempt_id),
    CONSTRAINT fk_grade_adjustments_2 FOREIGN KEY (attempt_question_id, attempt_id)
        REFERENCES attempt_questions (id, attempt_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_grade_adjustments_3 (grader_id),
    CONSTRAINT fk_grade_adjustments_3 FOREIGN KEY (grader_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_grade_adjustments_1 CHECK (old_points >= 0),
    CONSTRAINT ck_grade_adjustments_2 CHECK (new_points >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
