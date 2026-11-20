-- MySQL 8.4+ / 9.6; execute once, in V1..V7 order, on an explicitly selected EMPTY database.
-- No CREATE DATABASE, USE, DROP, or automatic migration runner. DDL auto-commits.
SET NAMES utf8mb4;
SET SESSION time_zone = '+00:00';

-- User: users
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_code VARCHAR(30) NULL,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NULL,
    avatar_url VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    email_verified_at DATETIME(3) NULL,
    last_login_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uq_users_1 UNIQUE (user_code),
    CONSTRAINT uq_users_2 UNIQUE (email),
    CONSTRAINT ck_users_1 CHECK (status IN ('ACTIVE','INACTIVE','LOCKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Role: roles
CREATE TABLE roles (
    id SMALLINT NOT NULL AUTO_INCREMENT,
    role_code VARCHAR(30) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_roles_1 UNIQUE (role_code),
    CONSTRAINT ck_roles_1 CHECK (role_code IN ('ADMIN','TEACHER','STUDENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- UserRole: user_roles
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id SMALLINT NOT NULL,
    assigned_at DATETIME(3) NOT NULL,
    assigned_by BIGINT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_1 FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_user_roles_1 (role_id),
    CONSTRAINT fk_user_roles_2 FOREIGN KEY (role_id)
        REFERENCES roles (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_user_roles_2 (assigned_by),
    CONSTRAINT fk_user_roles_3 FOREIGN KEY (assigned_by)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- PasswordResetToken: password_reset_tokens
CREATE TABLE password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    used_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uq_password_reset_tokens_1 UNIQUE (token_hash),
    INDEX ix_password_reset_tokens_1 (user_id, expires_at),
    CONSTRAINT fk_password_reset_tokens_1 FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_password_reset_tokens_1 CHECK (expires_at > created_at),
    CONSTRAINT ck_password_reset_tokens_2 CHECK (used_at IS NULL OR used_at >= created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- LoginSession: login_sessions
CREATE TABLE login_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    session_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(512) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_seen_at DATETIME(3) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    revoked_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_login_sessions_1 UNIQUE (session_hash),
    INDEX ix_login_sessions_1 (user_id, revoked_at, expires_at),
    CONSTRAINT fk_login_sessions_1 FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_login_sessions_1 CHECK (expires_at > created_at),
    CONSTRAINT ck_login_sessions_2 CHECK (last_seen_at >= created_at),
    CONSTRAINT ck_login_sessions_3 CHECK (revoked_at IS NULL OR revoked_at >= created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- AuditLog: audit_logs
CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_user_id BIGINT NULL,
    action_code VARCHAR(60) NOT NULL,
    target_type VARCHAR(60) NOT NULL,
    target_id BIGINT NULL,
    ip_address VARCHAR(45) NULL,
    detail_json JSON NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX ix_audit_logs_1 (actor_user_id, created_at),
    INDEX ix_audit_logs_2 (target_type, target_id, created_at),
    CONSTRAINT fk_audit_logs_1 FOREIGN KEY (actor_user_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
