-- MySQL 8.4+ / 9.6; execute once, in V1..V7 order, on an explicitly selected EMPTY database.
-- No CREATE DATABASE, USE, DROP, or automatic migration runner. DDL auto-commits.
SET NAMES utf8mb4;
SET SESSION time_zone = '+00:00';

-- Notification: notifications
CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    link_path VARCHAR(500) NULL,
    read_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX ix_notifications_1 (recipient_id, read_at, created_at),
    CONSTRAINT fk_notifications_1 FOREIGN KEY (recipient_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ExamAppeal: exam_appeals
CREATE TABLE exam_appeals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    attempt_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewer_id BIGINT NULL,
    response TEXT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    resolved_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    INDEX ix_exam_appeals_1 (student_id, status, created_at),
    INDEX ix_exam_appeals_2 (attempt_id, student_id),
    CONSTRAINT fk_exam_appeals_1 FOREIGN KEY (attempt_id, student_id)
        REFERENCES exam_attempts (id, student_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_exam_appeals_2 FOREIGN KEY (student_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_exam_appeals_3 (reviewer_id),
    CONSTRAINT fk_exam_appeals_3 FOREIGN KEY (reviewer_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_exam_appeals_1 CHECK (status IN ('PENDING','REVIEWING','RESOLVED','REJECTED')),
    CONSTRAINT ck_exam_appeals_2 CHECK (resolved_at IS NULL OR resolved_at >= created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
