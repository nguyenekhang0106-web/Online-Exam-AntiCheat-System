-- MySQL 8.4+ / 9.6; execute once, in V1..V7 order, on an explicitly selected EMPTY database.
-- No CREATE DATABASE, USE, DROP, or automatic migration runner. DDL auto-commits.
SET NAMES utf8mb4;
SET SESSION time_zone = '+00:00';

-- AttemptSession: attempt_sessions
CREATE TABLE attempt_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    attempt_id BIGINT NOT NULL,
    session_token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    device_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(512) NULL,
    opened_at DATETIME(3) NOT NULL,
    last_seen_at DATETIME(3) NOT NULL,
    closed_at DATETIME(3) NULL,
    close_reason VARCHAR(50) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_attempt_sessions_1 UNIQUE (session_token_hash),
    CONSTRAINT uq_attempt_sessions_2 UNIQUE (id, attempt_id),
    INDEX ix_attempt_sessions_1 (attempt_id, closed_at),
    CONSTRAINT fk_attempt_sessions_1 FOREIGN KEY (attempt_id)
        REFERENCES exam_attempts (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_attempt_sessions_1 CHECK (last_seen_at >= opened_at),
    CONSTRAINT ck_attempt_sessions_2 CHECK (closed_at IS NULL OR closed_at >= opened_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ProctoringEvent: proctoring_events
CREATE TABLE proctoring_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    attempt_id BIGINT NOT NULL,
    attempt_session_id BIGINT NULL,
    event_code VARCHAR(60) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    client_occurred_at DATETIME(3) NULL,
    server_received_at DATETIME(3) NOT NULL,
    client_event_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    detail_json JSON NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_proctoring_events_1 UNIQUE (attempt_id, client_event_id),
    CONSTRAINT uq_proctoring_events_2 UNIQUE (id, attempt_id),
    INDEX ix_proctoring_events_1 (attempt_id, server_received_at),
    CONSTRAINT fk_proctoring_events_1 FOREIGN KEY (attempt_id)
        REFERENCES exam_attempts (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_proctoring_events_2 (attempt_session_id, attempt_id),
    CONSTRAINT fk_proctoring_events_2 FOREIGN KEY (attempt_session_id, attempt_id)
        REFERENCES attempt_sessions (id, attempt_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_proctoring_events_1 CHECK (severity IN ('INFO','LOW','MEDIUM','HIGH','CRITICAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ProctoringEvidence: proctoring_evidence
CREATE TABLE proctoring_evidence (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    media_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    captured_at DATETIME(3) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    deleted_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    INDEX ix_proctoring_evidence_1 (expires_at, deleted_at),
    INDEX ix_proctoring_evidence_2 (event_id),
    CONSTRAINT fk_proctoring_evidence_1 FOREIGN KEY (event_id)
        REFERENCES proctoring_events (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_proctoring_evidence_1 CHECK (expires_at > captured_at),
    CONSTRAINT ck_proctoring_evidence_2 CHECK (deleted_at IS NULL OR deleted_at >= captured_at),
    CONSTRAINT ck_proctoring_evidence_3 CHECK (size_bytes >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ViolationReview: violation_reviews
CREATE TABLE violation_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    attempt_id BIGINT NOT NULL,
    related_event_id BIGINT NULL,
    reviewer_id BIGINT NOT NULL,
    decision VARCHAR(30) NOT NULL,
    reason TEXT NULL,
    reviewed_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    INDEX ix_violation_reviews_1 (attempt_id),
    CONSTRAINT fk_violation_reviews_1 FOREIGN KEY (attempt_id)
        REFERENCES exam_attempts (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_violation_reviews_2 (related_event_id, attempt_id),
    CONSTRAINT fk_violation_reviews_2 FOREIGN KEY (related_event_id, attempt_id)
        REFERENCES proctoring_events (id, attempt_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_violation_reviews_3 (reviewer_id),
    CONSTRAINT fk_violation_reviews_3 FOREIGN KEY (reviewer_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_violation_reviews_1 CHECK (decision IN ('VALID_EVENT','FALSE_POSITIVE','WARNING','INVALIDATE_ATTEMPT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
