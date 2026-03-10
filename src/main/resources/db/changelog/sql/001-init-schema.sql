-- =============================================================================
-- FILE    : 001_initial_schema.sql
-- DESC    : Khởi tạo schema cho module x-user
--           Bao gồm: users, roles, permissions và các bảng trung gian
-- AUTHOR  : htv
-- VERSION : 001
-- =============================================================================

-- =============================================================================
-- PHẦN 1: PERMISSIONS
-- Lưu danh sách quyền hạn theo format RESOURCE:ACTION
-- Ví dụ: USER:READ, USER:WRITE, ROLE:DELETE
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.permissions
(
    -- PK
    id          UUID            NOT NULL,

    -- Thông tin quyền
    name        VARCHAR(100)    NOT NULL,   -- VD: USER:READ
    resource    VARCHAR(50)     NOT NULL,   -- VD: USER, ORDER, REPORT
    action      VARCHAR(50)     NOT NULL,   -- VD: READ, WRITE, DELETE, EXPORT
    description VARCHAR(255),

    -- Audit (BaseEntity)
    created_at  TIMESTAMPTZ     NOT NULL,
    updated_at  TIMESTAMPTZ     NOT NULL,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    is_deleted  BOOLEAN         NOT NULL    DEFAULT FALSE,
    version     INTEGER         NOT NULL    DEFAULT 0,

    CONSTRAINT pk_permissions           PRIMARY KEY (id),
    CONSTRAINT uq_permissions_name      UNIQUE      (name)
    );

-- Index tra cứu theo resource (VD: lấy tất cả quyền của USER)
CREATE INDEX IF NOT EXISTS idx_permissions_resource ON public.permissions (resource);

ALTER TABLE public.permissions OWNER TO htv;

COMMENT ON TABLE  public.permissions             IS 'Danh sách quyền hạn theo resource:action';
COMMENT ON COLUMN public.permissions.name        IS 'Tên quyền duy nhất, format: RESOURCE:ACTION — VD: USER:READ';
COMMENT ON COLUMN public.permissions.resource    IS 'Tài nguyên được quản lý — VD: USER, ORDER, REPORT';
COMMENT ON COLUMN public.permissions.action      IS 'Hành động được phép — VD: READ, WRITE, DELETE, EXPORT';
COMMENT ON COLUMN public.permissions.is_deleted  IS 'Soft delete — không xóa thật khỏi DB';
COMMENT ON COLUMN public.permissions.version     IS 'Optimistic locking — tránh ghi đè khi concurrent';


-- =============================================================================
-- PHẦN 2: ROLES
-- Nhóm quyền, gán vào user
-- Ví dụ: ROLE_ADMIN, ROLE_USER, ROLE_MODERATOR
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.roles
(
    -- PK
    id          UUID            NOT NULL,

    -- Thông tin role
    name        VARCHAR(50)     NOT NULL,   -- VD: ROLE_ADMIN
    description VARCHAR(255),
    is_system   BOOLEAN         NOT NULL    DEFAULT FALSE,  -- true = không cho xóa

-- Audit (BaseEntity)
    created_at  TIMESTAMPTZ     NOT NULL,
    updated_at  TIMESTAMPTZ     NOT NULL,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    is_deleted  BOOLEAN         NOT NULL    DEFAULT FALSE,
    version     INTEGER         NOT NULL    DEFAULT 0,

    CONSTRAINT pk_roles         PRIMARY KEY (id),
    CONSTRAINT uq_roles_name    UNIQUE      (name)
    );

ALTER TABLE public.roles OWNER TO htv;

COMMENT ON TABLE  public.roles              IS 'Nhóm quyền hạn — gắn vào user để phân quyền';
COMMENT ON COLUMN public.roles.name         IS 'Tên role duy nhất — nên có prefix ROLE_ theo convention Spring Security';
COMMENT ON COLUMN public.roles.is_system    IS 'TRUE = role mặc định của hệ thống, không cho phép xóa';
COMMENT ON COLUMN public.roles.is_deleted   IS 'Soft delete';
COMMENT ON COLUMN public.roles.version      IS 'Optimistic locking';


-- =============================================================================
-- PHẦN 3: ROLE_PERMISSIONS
-- Bảng trung gian: Role ↔ Permission (nhiều-nhiều)
-- Một role có nhiều permission, một permission thuộc nhiều role
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.role_permissions
(
    role_id         UUID    NOT NULL,
    permission_id   UUID    NOT NULL,

    CONSTRAINT pk_role_permissions
    PRIMARY KEY (role_id, permission_id),

    -- Xóa role → xóa luôn các dòng liên kết
    CONSTRAINT fk_rp_role
    FOREIGN KEY (role_id)
    REFERENCES  public.roles (id)
    ON DELETE CASCADE,

    -- Xóa permission → xóa luôn các dòng liên kết
    CONSTRAINT fk_rp_permission
    FOREIGN KEY (permission_id)
    REFERENCES  public.permissions (id)
    ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_role_permissions_role_id
    ON public.role_permissions (role_id);

CREATE INDEX IF NOT EXISTS idx_role_permissions_permission_id
    ON public.role_permissions (permission_id);

ALTER TABLE public.role_permissions OWNER TO htv;

COMMENT ON TABLE  public.role_permissions               IS 'Bảng trung gian: Role ↔ Permission (nhiều-nhiều)';
COMMENT ON COLUMN public.role_permissions.role_id       IS 'FK → roles.id';
COMMENT ON COLUMN public.role_permissions.permission_id IS 'FK → permissions.id';


-- =============================================================================
-- PHẦN 4: USERS
-- Tài khoản người dùng
-- Tính năng: MFA (TOTP / Email OTP), xác thực email,
--            quản lý mật khẩu, bảo mật đăng nhập
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.users
(
    -- ── PK ────────────────────────────────────────────────────────────────────
    id                              UUID            NOT NULL,

    -- ── Thông tin cơ bản ──────────────────────────────────────────────────────
    email                           VARCHAR(255)    NOT NULL,
    username                        VARCHAR(50)     NOT NULL,
    password_hash                   VARCHAR(255)    NOT NULL,
    first_name                      VARCHAR(100),
    last_name                       VARCHAR(100),
    phone_number                    VARCHAR(20),
    avatar_url                      VARCHAR(500),

    -- ── Trạng thái tài khoản ─────────────────────────────────────────────────
    -- PENDING   : mới đăng ký, chưa xác thực email
    -- ACTIVE    : hoạt động bình thường
    -- INACTIVE  : tự tắt hoặc admin vô hiệu hóa
    -- SUSPENDED : bị khóa tạm thời do vi phạm
    -- BANNED    : bị cấm vĩnh viễn
    status                          VARCHAR(20)     NOT NULL    DEFAULT 'PENDING',

    -- ── Xác thực email ────────────────────────────────────────────────────────
    email_verified                  BOOLEAN         NOT NULL    DEFAULT FALSE,
    email_verified_at               TIMESTAMPTZ,
    email_verify_token              VARCHAR(100),               -- UUID token gửi trong mail
    email_verify_token_expires_at   TIMESTAMPTZ,               -- hết hạn sau X giờ

-- ── MFA (Multi-Factor Authentication) ────────────────────────────────────
-- Hỗ trợ 2 loại:
--   TOTP      : Google Authenticator / Authy (scan QR)
--   EMAIL_OTP : OTP 6 số gửi qua email mỗi lần đăng nhập
    mfa_enabled                     BOOLEAN         NOT NULL    DEFAULT FALSE,
    mfa_type                        VARCHAR(20),               -- TOTP | EMAIL_OTP
    mfa_totp_secret                 VARCHAR(255),              -- secret key TOTP, cần mã hóa at-rest
    mfa_backup_codes                TEXT,                      -- JSON array các backup code đã hash
    mfa_otp_code                    VARCHAR(10),               -- OTP tạm thời (EMAIL_OTP)
    mfa_otp_expires_at              TIMESTAMPTZ,               -- thời gian hết hạn OTP
    mfa_otp_attempts                INTEGER         NOT NULL    DEFAULT 0, -- chống brute-force

-- ── Quản lý mật khẩu ─────────────────────────────────────────────────────
    password_reset_token            VARCHAR(100),              -- UUID token gửi trong mail
    password_reset_token_expires_at TIMESTAMPTZ,               -- hết hạn sau 1 giờ
    password_changed_at             TIMESTAMPTZ,               -- lần đổi mật khẩu gần nhất

-- ── Bảo mật đăng nhập ────────────────────────────────────────────────────
    last_login_at                   TIMESTAMPTZ,
    last_login_ip                   VARCHAR(45),               -- IPv4 (15) hoặc IPv6 (45)
    failed_login_attempts           INTEGER         NOT NULL    DEFAULT 0,
    locked_until                    TIMESTAMPTZ,               -- tạm khóa sau N lần sai liên tiếp

-- ── Audit (BaseEntity) ────────────────────────────────────────────────────
    created_at                      TIMESTAMPTZ     NOT NULL,
    updated_at                      TIMESTAMPTZ     NOT NULL,
    created_by                      VARCHAR(100),
    updated_by                      VARCHAR(100),
    is_deleted                      BOOLEAN         NOT NULL    DEFAULT FALSE,
    version                         INTEGER         NOT NULL    DEFAULT 0,

    CONSTRAINT pk_users             PRIMARY KEY (id),
    CONSTRAINT uq_users_email       UNIQUE      (email),
    CONSTRAINT uq_users_username    UNIQUE      (username),

    CONSTRAINT ck_users_status CHECK (
                                         status IN ('PENDING', 'ACTIVE', 'INACTIVE', 'SUSPENDED', 'BANNED')
    ),
    CONSTRAINT ck_users_mfa_type CHECK (
                                           mfa_type IS NULL OR mfa_type IN ('TOTP', 'EMAIL_OTP')
    )
    );

-- Index tra cứu thường dùng
CREATE INDEX IF NOT EXISTS idx_users_email
    ON public.users (email);

CREATE INDEX IF NOT EXISTS idx_users_username
    ON public.users (username);

CREATE INDEX IF NOT EXISTS idx_users_status
    ON public.users (status);

-- Index cho token lookup (verify email, reset password)
CREATE INDEX IF NOT EXISTS idx_users_email_verify_token
    ON public.users (email_verify_token)
    WHERE email_verify_token IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_users_password_reset_token
    ON public.users (password_reset_token)
    WHERE password_reset_token IS NOT NULL;

ALTER TABLE public.users OWNER TO htv;

COMMENT ON TABLE  public.users                              IS 'Tài khoản người dùng — hỗ trợ MFA, xác thực email, phân quyền';
COMMENT ON COLUMN public.users.email                        IS 'Email đăng nhập — duy nhất trong hệ thống';
COMMENT ON COLUMN public.users.username                     IS 'Tên đăng nhập — duy nhất, không phân biệt hoa thường';
COMMENT ON COLUMN public.users.password_hash                IS 'BCrypt hash của mật khẩu — KHÔNG lưu plain text';
COMMENT ON COLUMN public.users.status                       IS 'Trạng thái: PENDING|ACTIVE|INACTIVE|SUSPENDED|BANNED';
COMMENT ON COLUMN public.users.email_verified               IS 'TRUE sau khi user click link xác thực trong email';
COMMENT ON COLUMN public.users.email_verify_token           IS 'UUID token trong link xác thực — xóa sau khi dùng';
COMMENT ON COLUMN public.users.mfa_enabled                  IS 'TRUE = bắt buộc nhập mã MFA khi đăng nhập';
COMMENT ON COLUMN public.users.mfa_type                     IS 'TOTP: dùng app authenticator | EMAIL_OTP: nhận mã qua email';
COMMENT ON COLUMN public.users.mfa_totp_secret              IS 'Base32 secret key cho TOTP — phải mã hóa trước khi lưu';
COMMENT ON COLUMN public.users.mfa_backup_codes             IS 'JSON array ["hash1","hash2",...] — dùng khi mất thiết bị TOTP';
COMMENT ON COLUMN public.users.mfa_otp_attempts             IS 'Số lần nhập OTP sai — reset về 0 khi thành công';
COMMENT ON COLUMN public.users.failed_login_attempts        IS 'Số lần đăng nhập sai liên tiếp — reset về 0 khi thành công';
COMMENT ON COLUMN public.users.locked_until                 IS 'Tài khoản bị khóa tạm thời đến thời điểm này';
COMMENT ON COLUMN public.users.last_login_ip                IS 'IP lần đăng nhập gần nhất — hỗ trợ IPv4 và IPv6';
COMMENT ON COLUMN public.users.is_deleted                   IS 'Soft delete — không xóa thật khỏi DB';
COMMENT ON COLUMN public.users.version                      IS 'Optimistic locking';


-- =============================================================================
-- PHẦN 5: USER_ROLES
-- Bảng trung gian: User ↔ Role (nhiều-nhiều)
-- Một user có nhiều role, một role gán cho nhiều user
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.user_roles
(
    user_id     UUID    NOT NULL,
    role_id     UUID    NOT NULL,

    CONSTRAINT pk_user_roles
    PRIMARY KEY (user_id, role_id),

    -- Xóa user → xóa luôn các role gắn với user đó
    CONSTRAINT fk_ur_user
    FOREIGN KEY (user_id)
    REFERENCES  public.users (id)
    ON DELETE CASCADE,

    -- Không cho xóa role đang được gán cho user
    CONSTRAINT fk_ur_role
    FOREIGN KEY (role_id)
    REFERENCES  public.roles (id)
    ON DELETE RESTRICT
    );

CREATE INDEX IF NOT EXISTS idx_user_roles_user_id
    ON public.user_roles (user_id);

CREATE INDEX IF NOT EXISTS idx_user_roles_role_id
    ON public.user_roles (role_id);

ALTER TABLE public.user_roles OWNER TO htv;

COMMENT ON TABLE  public.user_roles             IS 'Bảng trung gian: User ↔ Role (nhiều-nhiều)';
COMMENT ON COLUMN public.user_roles.user_id     IS 'FK → users.id — CASCADE DELETE';
COMMENT ON COLUMN public.user_roles.role_id     IS 'FK → roles.id — RESTRICT DELETE (không xóa role đang dùng)';


-- =============================================================================
-- PHẦN 6: SEED DATA — Roles mặc định
-- is_system = TRUE: không cho phép xóa qua API
-- =============================================================================

INSERT INTO public.roles (id, name, description, is_system, created_at, updated_at, created_by, updated_by, is_deleted, version)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'ROLE_ADMIN',     'Quản trị viên — toàn quyền hệ thống',  TRUE, NOW(), NOW(), 'system', 'system', FALSE, 0),
    ('00000000-0000-0000-0000-000000000002', 'ROLE_USER',      'Người dùng thông thường',               TRUE, NOW(), NOW(), 'system', 'system', FALSE, 0),
    ('00000000-0000-0000-0000-000000000003', 'ROLE_MODERATOR', 'Kiểm duyệt nội dung',                  TRUE, NOW(), NOW(), 'system', 'system', FALSE, 0)
    ON CONFLICT (name) DO NOTHING;


-- =============================================================================
-- PHẦN 7: SEED DATA — Permissions mặc định
-- Format: RESOURCE:ACTION
-- =============================================================================

INSERT INTO public.permissions (id, name, resource, action, description, created_at, updated_at, created_by, updated_by, is_deleted, version)
VALUES
    -- USER
    ('10000000-0000-0000-0000-000000000001', 'USER:READ',       'USER', 'READ',   'Xem thông tin người dùng',         NOW(), NOW(), 'system', 'system', FALSE, 0),
    ('10000000-0000-0000-0000-000000000002', 'USER:WRITE',      'USER', 'WRITE',  'Tạo và cập nhật người dùng',       NOW(), NOW(), 'system', 'system', FALSE, 0),
    ('10000000-0000-0000-0000-000000000003', 'USER:DELETE',     'USER', 'DELETE', 'Xóa người dùng',                   NOW(), NOW(), 'system', 'system', FALSE, 0),

    -- ROLE
    ('10000000-0000-0000-0000-000000000004', 'ROLE:READ',       'ROLE', 'READ',   'Xem danh sách role',               NOW(), NOW(), 'system', 'system', FALSE, 0),
    ('10000000-0000-0000-0000-000000000005', 'ROLE:WRITE',      'ROLE', 'WRITE',  'Tạo và cập nhật role',             NOW(), NOW(), 'system', 'system', FALSE, 0),
    ('10000000-0000-0000-0000-000000000006', 'ROLE:DELETE',     'ROLE', 'DELETE', 'Xóa role',                         NOW(), NOW(), 'system', 'system', FALSE, 0),

    -- PERMISSION
    ('10000000-0000-0000-0000-000000000007', 'PERMISSION:READ', 'PERMISSION', 'READ',  'Xem danh sách permission',    NOW(), NOW(), 'system', 'system', FALSE, 0),
    ('10000000-0000-0000-0000-000000000008', 'PERMISSION:WRITE','PERMISSION', 'WRITE', 'Tạo và cập nhật permission',  NOW(), NOW(), 'system', 'system', FALSE, 0)
    ON CONFLICT (name) DO NOTHING;


-- =============================================================================
-- PHẦN 8: SEED DATA — Role ↔ Permission mapping
-- ROLE_ADMIN     : tất cả permissions
-- ROLE_USER      : USER:READ
-- ROLE_MODERATOR : USER:READ, USER:WRITE
-- =============================================================================

INSERT INTO public.role_permissions (role_id, permission_id)
VALUES
    -- ROLE_ADMIN → tất cả
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000003'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000004'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000005'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000006'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000007'),
    ('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000008'),

    -- ROLE_USER → chỉ đọc thông tin user
    ('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001'),

    -- ROLE_MODERATOR → xem và sửa user
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001'),
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002')
    ON CONFLICT DO NOTHING;