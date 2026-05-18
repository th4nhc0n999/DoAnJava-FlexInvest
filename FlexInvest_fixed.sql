-- ============================================================
-- FlexInvest — DDL Schema Script (FIXED)
-- Oracle Database
-- ============================================================
-- Fixes:
--   1. "TRANSACTION" dùng dấu ngoặc kép (Oracle reserved word)
--   2. ROLES seed: 3 roles (Admin=1, Staff=2, Customer=3)
--   3. INVESTMENT có cột payout_method, target_product_id
--   4. LEDGER columns khớp với WalletDAO.java
--   5. Admin seed dùng email nhất quán: admin@flexinvest.vn
-- ============================================================

ALTER SESSION SET CURRENT_SCHEMA = FlexInvest;

-- 1. ROLES
CREATE TABLE ROLES (
    role_id     NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_name   VARCHAR2(100)  NOT NULL,
    description VARCHAR2(500),
    created_at  TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    is_deleted  NUMBER(1)      DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1))
);

-- 2. USERS
CREATE TABLE USERS (
    user_id       NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_id       NUMBER         NOT NULL,
    email         VARCHAR2(255)  NOT NULL UNIQUE,
    password_hash VARCHAR2(512)  NOT NULL,
    created_at    TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    status        VARCHAR2(50)   DEFAULT 'ACTIVE' NOT NULL,
    referral_code VARCHAR2(50)   UNIQUE,
    is_deleted    NUMBER(1)      DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES ROLES(role_id)
);

-- 3. EKYC
CREATE TABLE EKYC (
    kyc_id               NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id              NUMBER         NOT NULL,
    id_number            VARCHAR2(50)   NOT NULL,
    full_name            VARCHAR2(255)  NOT NULL,
    date_of_birth        DATE           NOT NULL,
    gender               VARCHAR2(10),
    place_of_origin      VARCHAR2(500),
    place_of_residence   VARCHAR2(500),
    issue_date           DATE,
    expiry_date          DATE,
    issue_place          VARCHAR2(255),
    front_image_url      VARCHAR2(1000),
    back_image_url       VARCHAR2(1000),
    face_image_url       VARCHAR2(1000),
    match_score          NUMBER(5,2),
    verified_status      VARCHAR2(50)   DEFAULT 'PENDING' NOT NULL,
    note                 VARCHAR2(1000),
    verified_at          TIMESTAMP,
    created_at           TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at           TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    is_deleted           NUMBER(1)      DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)),
    CONSTRAINT fk_ekyc_user FOREIGN KEY (user_id) REFERENCES USERS(user_id)
);

-- 4. AUTH_SESSION
CREATE TABLE AUTH_SESSION (
    session_id    NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       NUMBER         NOT NULL,
    access_token  VARCHAR2(2000) NOT NULL,
    refresh_token VARCHAR2(2000) NOT NULL,
    expired_at    TIMESTAMP      NOT NULL,
    device_info   VARCHAR2(1000),
    created_at    TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    last_used_at  TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    is_deleted    NUMBER(1)      DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)),
    CONSTRAINT fk_auth_user FOREIGN KEY (user_id) REFERENCES USERS(user_id)
);

-- 5. REFERRAL
CREATE TABLE REFERRAL (
    referral_id   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    referrer_id   NUMBER         NOT NULL,
    referee_id    NUMBER         NOT NULL,
    status        VARCHAR2(50)   DEFAULT 'PENDING' NOT NULL,
    reward_amount NUMBER(18,2)   DEFAULT 0,
    created_at    TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    is_deleted    NUMBER(1)      DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)),
    CONSTRAINT fk_referral_referrer FOREIGN KEY (referrer_id) REFERENCES USERS(user_id),
    CONSTRAINT fk_referral_referee  FOREIGN KEY (referee_id)  REFERENCES USERS(user_id)
);

-- 6. WALLET
CREATE TABLE WALLET (
    wallet_id         NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           NUMBER       NOT NULL UNIQUE,
    available_balance NUMBER(18,2) DEFAULT 0 NOT NULL,
    locked_balance    NUMBER(18,2) DEFAULT 0 NOT NULL,
    status            VARCHAR2(50) DEFAULT 'ACTIVE' NOT NULL,
    is_deleted        NUMBER(1)    DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)),
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES USERS(user_id)
);

-- 7. BANK_ACCOUNT
CREATE TABLE BANK_ACCOUNT (
    bank_account_id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         NUMBER         NOT NULL,
    bank_name       VARCHAR2(255)  NOT NULL,
    account_number  VARCHAR2(50)   NOT NULL,
    is_linked       NUMBER(1)      DEFAULT 0 NOT NULL CHECK (is_linked IN (0,1)),
    is_deleted      NUMBER(1)      DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)),
    CONSTRAINT fk_bank_user FOREIGN KEY (user_id) REFERENCES USERS(user_id)
);

-- 8. TRANSACTION_TYPE
CREATE TABLE TRANSACTION_TYPE (
    type_code   VARCHAR2(50)  PRIMARY KEY,
    type_name   VARCHAR2(200) NOT NULL,
    description VARCHAR2(500),
    is_deleted  NUMBER(1)     DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1))
);

-- 9. TRANSACTION (dùng ngoặc kép vì là reserved word trong Oracle)
CREATE TABLE "TRANSACTION" (
    transaction_id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    wallet_id      NUMBER         NOT NULL,
    type_code      VARCHAR2(50)   NOT NULL,
    amount         NUMBER(18,2)   NOT NULL,
    status         VARCHAR2(50)   DEFAULT 'PENDING' NOT NULL,
    created_at     TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    is_deleted     NUMBER(1)      DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)),
    CONSTRAINT fk_txn_wallet FOREIGN KEY (wallet_id)  REFERENCES WALLET(wallet_id),
    CONSTRAINT fk_txn_type   FOREIGN KEY (type_code)  REFERENCES TRANSACTION_TYPE(type_code)
);

-- 10. DEPOSIT
CREATE TABLE DEPOSIT (
    deposit_id        NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id    NUMBER         NOT NULL UNIQUE,
    request_code      VARCHAR2(100)  NOT NULL,
    payment_gateway   VARCHAR2(100),
    receiving_account VARCHAR2(100),
    qr_string         VARCHAR2(2000),
    bank_trans_ref    VARCHAR2(255),
    expired_at        TIMESTAMP,
    is_deleted        NUMBER(1)      DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)),
    CONSTRAINT fk_deposit_txn FOREIGN KEY (transaction_id) REFERENCES "TRANSACTION"(transaction_id)
);

-- 11. WITHDRAW
CREATE TABLE WITHDRAW (
    withdraw_id     NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id  NUMBER         NOT NULL UNIQUE,
    bank_account_id NUMBER         NOT NULL,
    fee             NUMBER(18,2)   DEFAULT 0,
    status          VARCHAR2(50)   DEFAULT 'PENDING' NOT NULL,
    created_at      TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    processed_at    TIMESTAMP,
    note            VARCHAR2(1000),
    is_deleted      NUMBER(1)      DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)),
    CONSTRAINT fk_withdraw_txn  FOREIGN KEY (transaction_id)  REFERENCES "TRANSACTION"(transaction_id),
    CONSTRAINT fk_withdraw_bank FOREIGN KEY (bank_account_id) REFERENCES BANK_ACCOUNT(bank_account_id)
);

-- 12. LEDGER (khớp với WalletDAO.java: debit, credit, balance_after, wallet_id)
CREATE TABLE LEDGER (
    ledger_id      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id NUMBER       NOT NULL,
    wallet_id      NUMBER       NOT NULL,
    debit          NUMBER(18,2) DEFAULT 0 NOT NULL,
    credit         NUMBER(18,2) DEFAULT 0 NOT NULL,
    balance_after  NUMBER(18,2) NOT NULL,
    created_at     TIMESTAMP    DEFAULT SYSTIMESTAMP NOT NULL,
    is_deleted     NUMBER(1)    DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)),
    CONSTRAINT fk_ledger_txn    FOREIGN KEY (transaction_id) REFERENCES "TRANSACTION"(transaction_id),
    CONSTRAINT fk_ledger_wallet FOREIGN KEY (wallet_id)      REFERENCES WALLET(wallet_id)
);

-- 13. SAVINGS_PRODUCT
CREATE TABLE SAVINGS_PRODUCT (
    product_id              NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_name            VARCHAR2(255)  NOT NULL,
    interest_rate           NUMBER(6,4)    NOT NULL,
    term                    NUMBER         NOT NULL,
    min_investment_amount   NUMBER(18,2)   NOT NULL,
    max_investment_amount   NUMBER(18,2),
    penalty_rate            NUMBER(6,4)    DEFAULT 0,
    fallback_interest_rate  NUMBER(6,4)    DEFAULT 0,
    min_holding_days        NUMBER         DEFAULT 0,
    status                  VARCHAR2(50)   DEFAULT 'ACTIVE' NOT NULL,
    currency                VARCHAR2(10)   DEFAULT 'VND' NOT NULL,
    start_date              DATE,
    end_date                DATE,
    is_deleted              NUMBER(1)      DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1))
);

-- 14. INVESTMENT (khớp với InvestmentDAO.java: start_date, payout_method, target_product_id)
CREATE TABLE INVESTMENT (
    investment_id         NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id               NUMBER         NOT NULL,
    product_id            NUMBER         NOT NULL,
    invested_amount       NUMBER(18,2)   NOT NULL,
    applied_interest_rate NUMBER(6,4)    NOT NULL,
    start_date            DATE           NOT NULL,
    maturity_date         DATE,
    status                VARCHAR2(50)   DEFAULT 'ACTIVE' NOT NULL,
    payout_method         VARCHAR2(10),
    target_product_id     NUMBER(10),
    is_deleted            NUMBER(1)      DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)),
    CONSTRAINT fk_inv_user    FOREIGN KEY (user_id)    REFERENCES USERS(user_id),
    CONSTRAINT fk_inv_product FOREIGN KEY (product_id) REFERENCES SAVINGS_PRODUCT(product_id)
);

-- 15. PAYOUT
CREATE TABLE PAYOUT (
    payout_id      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    investment_id  NUMBER         NOT NULL,
    transaction_id NUMBER         NOT NULL,
    payout_type    VARCHAR2(50)   NOT NULL,
    payout_date    DATE           NOT NULL,
    payout_amount  NUMBER(18,2)   NOT NULL,
    is_deleted     NUMBER(1)      DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)),
    CONSTRAINT fk_payout_inv FOREIGN KEY (investment_id)  REFERENCES INVESTMENT(investment_id),
    CONSTRAINT fk_payout_txn FOREIGN KEY (transaction_id) REFERENCES "TRANSACTION"(transaction_id)
);

-- 16. EARLY_REDEMPTION_EVENT
CREATE TABLE EARLY_REDEMPTION_EVENT (
    event_id        NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    investment_id   NUMBER       NOT NULL,
    policy_id       NUMBER,
    redemption_date DATE         NOT NULL,
    original_amount NUMBER(18,2) NOT NULL,
    penalty_amount  NUMBER(18,2) DEFAULT 0 NOT NULL,
    actual_payout   NUMBER(18,2) NOT NULL,
    transaction_id  NUMBER       NOT NULL,
    created_at      TIMESTAMP    DEFAULT SYSTIMESTAMP NOT NULL,
    is_deleted      NUMBER(1)    DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)),
    CONSTRAINT fk_ere_inv FOREIGN KEY (investment_id)  REFERENCES INVESTMENT(investment_id),
    CONSTRAINT fk_ere_txn FOREIGN KEY (transaction_id) REFERENCES "TRANSACTION"(transaction_id)
);

-- 17. INTEREST_RATE
CREATE TABLE INTEREST_RATE (
    history_id     NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id     NUMBER       NOT NULL,
    old_rate       NUMBER(6,4)  NOT NULL,
    new_rate       NUMBER(6,4)  NOT NULL,
    effective_date DATE         NOT NULL,
    created_at     TIMESTAMP    DEFAULT SYSTIMESTAMP NOT NULL,
    changed_by     NUMBER       NOT NULL,
    is_deleted     NUMBER(1)    DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)),
    CONSTRAINT fk_ir_product    FOREIGN KEY (product_id) REFERENCES SAVINGS_PRODUCT(product_id),
    CONSTRAINT fk_ir_changed_by FOREIGN KEY (changed_by) REFERENCES USERS(user_id)
);

-- 18. TOKEN
CREATE TABLE TOKEN (
    token_wallet_id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         NUMBER       NOT NULL UNIQUE,
    balance         NUMBER(18,4) DEFAULT 0 NOT NULL,
    total_earned    NUMBER(18,4) DEFAULT 0 NOT NULL,
    status          VARCHAR2(50) DEFAULT 'ACTIVE' NOT NULL,
    updated_at      TIMESTAMP    DEFAULT SYSTIMESTAMP NOT NULL,
    is_deleted      NUMBER(1)    DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)),
    CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES USERS(user_id)
);

-- 19. TOKEN_TRANSACTION
CREATE TABLE TOKEN_TRANSACTION (
    token_tx_id   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       NUMBER        NOT NULL,
    type          VARCHAR2(50)  NOT NULL,
    amount        NUMBER(18,4)  NOT NULL,
    source_type   VARCHAR2(100),
    source_id     NUMBER,
    balance_after NUMBER(18,4)  NOT NULL,
    created_at    TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    is_deleted    NUMBER(1)     DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)),
    CONSTRAINT fk_token_tx_user FOREIGN KEY (user_id) REFERENCES USERS(user_id)
);

-- 20. EXCHANGE_MONEY
CREATE TABLE EXCHANGE_MONEY (
    exm_id         NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        NUMBER       NOT NULL,
    transaction_id NUMBER       NOT NULL,
    token_spent    NUMBER(18,4) NOT NULL,
    money_received NUMBER(18,2) NOT NULL,
    exchange_rate  NUMBER(18,6) NOT NULL,
    status         VARCHAR2(50) DEFAULT 'PENDING' NOT NULL,
    created_at     TIMESTAMP    DEFAULT SYSTIMESTAMP NOT NULL,
    is_deleted     NUMBER(1)    DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)),
    CONSTRAINT fk_exm_user FOREIGN KEY (user_id)        REFERENCES USERS(user_id),
    CONSTRAINT fk_exm_txn  FOREIGN KEY (transaction_id) REFERENCES "TRANSACTION"(transaction_id)
);

-- 21. MISSIONS
CREATE TABLE MISSIONS (
    mission_id   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title        VARCHAR2(255)  NOT NULL,
    description  VARCHAR2(1000),
    mission_type VARCHAR2(100)  NOT NULL,
    action_type  VARCHAR2(100)  NOT NULL,
    target_value NUMBER         NOT NULL,
    reward_token NUMBER(18,4)   NOT NULL,
    is_active    NUMBER(1)      DEFAULT 1 NOT NULL CHECK (is_active IN (0,1)),
    start_date   DATE,
    end_date     DATE,
    sort_order   NUMBER         DEFAULT 0,
    created_at   TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    is_deleted   NUMBER(1)      DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1))
);

-- 22. USER_MISSION
CREATE TABLE USER_MISSION (
    user_mission_id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         NUMBER       NOT NULL,
    mission_id      NUMBER       NOT NULL,
    status          VARCHAR2(50) DEFAULT 'IN_PROGRESS' NOT NULL,
    progress        NUMBER       DEFAULT 0 NOT NULL,
    completed_at    TIMESTAMP,
    claimed_at      TIMESTAMP,
    is_deleted      NUMBER(1)    DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)),
    CONSTRAINT uq_user_mission UNIQUE (user_id, mission_id),
    CONSTRAINT fk_um_user      FOREIGN KEY (user_id)    REFERENCES USERS(user_id),
    CONSTRAINT fk_um_mission   FOREIGN KEY (mission_id) REFERENCES MISSIONS(mission_id)
);

-- 23. AUDIT_LOG
CREATE TABLE AUDIT_LOG (
    log_id      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     NUMBER,
    action      VARCHAR2(255)  NOT NULL,
    entity_name VARCHAR2(255),
    entity_id   VARCHAR2(255),
    old_value   CLOB,
    new_value   CLOB,
    ip_address  VARCHAR2(50),
    created_at  TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    is_deleted  NUMBER(1)      DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)),
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES USERS(user_id)
);

-- 24. SUPPORT_TICKET
CREATE TABLE SUPPORT_TICKET (
    ticket_id   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     NUMBER         NOT NULL,
    category    VARCHAR2(100),
    subject     VARCHAR2(500)  NOT NULL,
    description CLOB,
    priority    VARCHAR2(50)   DEFAULT 'NORMAL' NOT NULL,
    status      VARCHAR2(50)   DEFAULT 'OPEN' NOT NULL,
    assigned_to NUMBER,
    created_at  TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at  TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    resolved_at TIMESTAMP,
    is_deleted  NUMBER(1)      DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)),
    CONSTRAINT fk_ticket_user     FOREIGN KEY (user_id)     REFERENCES USERS(user_id),
    CONSTRAINT fk_ticket_assigned FOREIGN KEY (assigned_to) REFERENCES USERS(user_id)
);

-- 25. SUPPORT_TICKET_MESSAGE
CREATE TABLE SUPPORT_TICKET_MESSAGE (
    message_id       NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id        NUMBER        NOT NULL,
    sender_id        NUMBER        NOT NULL,
    sender_type      VARCHAR2(50)  NOT NULL,
    content          CLOB          NOT NULL,
    created_at       TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    is_internal_note NUMBER(1)     DEFAULT 0 NOT NULL CHECK (is_internal_note IN (0,1)),
    is_deleted       NUMBER(1)     DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)),
    CONSTRAINT fk_stm_ticket FOREIGN KEY (ticket_id) REFERENCES SUPPORT_TICKET(ticket_id),
    CONSTRAINT fk_stm_sender FOREIGN KEY (sender_id) REFERENCES USERS(user_id)
);

-- 26. NOTIFICATION
CREATE TABLE NOTIFICATION (
    notification_id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         NUMBER        NOT NULL,
    title           VARCHAR2(500) NOT NULL,
    body            CLOB,
    type            VARCHAR2(100),
    is_read         NUMBER(1)     DEFAULT 0 NOT NULL CHECK (is_read IN (0,1)),
    sent_at         TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    read_at         TIMESTAMP,
    is_deleted      NUMBER(1)     DEFAULT 0 NOT NULL CHECK (is_deleted IN (0,1)),
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES USERS(user_id)
);

-- ============================================================
-- PHÂN QUYỀN HỆ THỐNG
-- ============================================================

-- 27. ACCOUNT
CREATE TABLE ACCOUNT (
    ACCOUNT_ID    NUMBER(10) GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    USER_ID       NUMBER(10)    NOT NULL,
    USERNAME      VARCHAR2(50)  UNIQUE NOT NULL,
    PASSWORD_HASH VARCHAR2(200) NOT NULL,
    STATUS        VARCHAR2(20)  DEFAULT 'ACTIVE',
    CREATED_AT    DATE          DEFAULT SYSDATE,
    UPDATED_AT    DATE          DEFAULT SYSDATE,
    IS_DELETED    NUMBER(1)     DEFAULT 0 CHECK (IS_DELETED IN (0,1)),
    CONSTRAINT FK_ACCOUNT_USER FOREIGN KEY (USER_ID) REFERENCES USERS(USER_ID)
);

-- 28. SYS_FUNCTION
CREATE TABLE SYS_FUNCTION (
    FUNCTION_ID   NUMBER(10)    GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    NAME_FUNCTION VARCHAR2(100) NOT NULL,
    CREATED_AT    DATE          DEFAULT SYSDATE,
    UPDATED_AT    DATE          DEFAULT SYSDATE
);

-- 29. SYS_ROLE
CREATE TABLE SYS_ROLE (
    ROLE_ID       NUMBER(10) GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    FUNCTION_ID   NUMBER(10) NOT NULL,
    ADD_PERM      NUMBER(1)  DEFAULT 0 CHECK (ADD_PERM IN (0,1)),
    EDIT_PERM     NUMBER(1)  DEFAULT 0 CHECK (EDIT_PERM IN (0,1)),
    DELETE_PERM   NUMBER(1)  DEFAULT 0 CHECK (DELETE_PERM IN (0,1)),
    DOWNLOAD_PERM NUMBER(1)  DEFAULT 0 CHECK (DOWNLOAD_PERM IN (0,1)),
    VIEW_PERM     NUMBER(1)  DEFAULT 0 CHECK (VIEW_PERM IN (0,1)),
    CREATED_AT    DATE       DEFAULT SYSDATE,
    UPDATED_AT    DATE       DEFAULT SYSDATE,
    IS_DELETED    NUMBER(1)  DEFAULT 0 CHECK (IS_DELETED IN (0,1)),
    CONSTRAINT FK_ROLE_FUNCTION FOREIGN KEY (FUNCTION_ID) REFERENCES SYS_FUNCTION(FUNCTION_ID)
);

-- 30. ROLE_GROUP
CREATE TABLE ROLE_GROUP (
    ROLE_GROUP_ID   NUMBER(10)    GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    NAME_ROLE_GROUP VARCHAR2(100) NOT NULL,
    CREATED_AT      DATE          DEFAULT SYSDATE,
    UPDATED_AT      DATE          DEFAULT SYSDATE,
    IS_DELETED      NUMBER(1)     DEFAULT 0 CHECK (IS_DELETED IN (0,1))
);

-- 31. ROLE_GROUP_ASSIGN_ROLE
CREATE TABLE ROLE_GROUP_ASSIGN_ROLE (
    ROLE_GROUP_ID NUMBER(10) NOT NULL,
    ROLE_ID       NUMBER(10) NOT NULL,
    CREATED_AT    DATE       DEFAULT SYSDATE,
    UPDATED_AT    DATE       DEFAULT SYSDATE,
    IS_DELETED    NUMBER(1)  DEFAULT 0 CHECK (IS_DELETED IN (0,1)),
    CONSTRAINT PK_ROLE_GROUP_ASSIGN_ROLE PRIMARY KEY (ROLE_GROUP_ID, ROLE_ID),
    CONSTRAINT FK_RGAR_ROLE_GROUP FOREIGN KEY (ROLE_GROUP_ID) REFERENCES ROLE_GROUP(ROLE_GROUP_ID),
    CONSTRAINT FK_RGAR_ROLE       FOREIGN KEY (ROLE_ID)       REFERENCES SYS_ROLE(ROLE_ID)
);

-- 32. ACCOUNT_ASSIGN_ROLE_GROUP
CREATE TABLE ACCOUNT_ASSIGN_ROLE_GROUP (
    ACCOUNT_ID    NUMBER(10) NOT NULL,
    ROLE_GROUP_ID NUMBER(10) NOT NULL,
    CREATED_AT    DATE       DEFAULT SYSDATE,
    UPDATED_AT    DATE       DEFAULT SYSDATE,
    IS_DELETED    NUMBER(1)  DEFAULT 0 CHECK (IS_DELETED IN (0,1)),
    CONSTRAINT PK_ACCOUNT_ASSIGN_ROLE_GROUP PRIMARY KEY (ACCOUNT_ID, ROLE_GROUP_ID),
    CONSTRAINT FK_AARG_ACCOUNT    FOREIGN KEY (ACCOUNT_ID)    REFERENCES ACCOUNT(ACCOUNT_ID),
    CONSTRAINT FK_AARG_ROLE_GROUP FOREIGN KEY (ROLE_GROUP_ID) REFERENCES ROLE_GROUP(ROLE_GROUP_ID)
);

-- 33. ACCOUNT_ASSIGN_ROLE
CREATE TABLE ACCOUNT_ASSIGN_ROLE (
    ACCOUNT_ID NUMBER(10) NOT NULL,
    ROLE_ID    NUMBER(10) NOT NULL,
    CREATED_AT DATE       DEFAULT SYSDATE,
    UPDATED_AT DATE       DEFAULT SYSDATE,
    IS_DELETED NUMBER(1)  DEFAULT 0 CHECK (IS_DELETED IN (0,1)),
    CONSTRAINT PK_ACCOUNT_ASSIGN_ROLE PRIMARY KEY (ACCOUNT_ID, ROLE_ID),
    CONSTRAINT FK_AAR_ACCOUNT FOREIGN KEY (ACCOUNT_ID) REFERENCES ACCOUNT(ACCOUNT_ID),
    CONSTRAINT FK_AAR_ROLE    FOREIGN KEY (ROLE_ID)    REFERENCES SYS_ROLE(ROLE_ID)
);

COMMIT;

-- ============================================================
-- SEED DATA
-- ============================================================

-- ROLES: 3 roles để khớp với RegisterController (role_id=3 = Customer)
INSERT INTO ROLES (role_name, description) VALUES ('Admin',    'Quản trị viên hệ thống');
INSERT INTO ROLES (role_name, description) VALUES ('Staff',    'Nhân viên xử lý');
INSERT INTO ROLES (role_name, description) VALUES ('Customer', 'Khách hàng đầu tư');
COMMIT;

-- TRANSACTION_TYPE
INSERT INTO TRANSACTION_TYPE (type_code, type_name, description) VALUES ('DEPOSIT',    'Nạp tiền',     'Giao dịch nạp tiền vào ví');
INSERT INTO TRANSACTION_TYPE (type_code, type_name, description) VALUES ('WITHDRAW',   'Rút tiền',     'Giao dịch rút tiền khỏi ví');
INSERT INTO TRANSACTION_TYPE (type_code, type_name, description) VALUES ('INVEST',     'Đầu tư',       'Khóa tiền vào sản phẩm đầu tư');
INSERT INTO TRANSACTION_TYPE (type_code, type_name, description) VALUES ('PAYOUT',     'Tất toán',     'Nhận lãi / gốc khi đáo hạn');
INSERT INTO TRANSACTION_TYPE (type_code, type_name, description) VALUES ('EARLY_REDM', 'Tất toán sớm', 'Tất toán trước hạn, áp phạt');
INSERT INTO TRANSACTION_TYPE (type_code, type_name, description) VALUES ('BONUS',      'Thưởng',       'Thưởng từ giới thiệu / nhiệm vụ');
COMMIT;

-- SYS_FUNCTION (FUNCTION_ID 1-5, khớp LoginController.FUNCTION_IDS = {1,2,3,4,5})
INSERT INTO SYS_FUNCTION (FUNCTION_ID, NAME_FUNCTION) VALUES (1, 'Quản lý Người dùng');
INSERT INTO SYS_FUNCTION (FUNCTION_ID, NAME_FUNCTION) VALUES (2, 'Quản lý Giao dịch');
INSERT INTO SYS_FUNCTION (FUNCTION_ID, NAME_FUNCTION) VALUES (3, 'Quản lý Đầu tư');
INSERT INTO SYS_FUNCTION (FUNCTION_ID, NAME_FUNCTION) VALUES (4, 'Quản lý Nhiệm vụ');
INSERT INTO SYS_FUNCTION (FUNCTION_ID, NAME_FUNCTION) VALUES (5, 'Báo cáo & Thống kê');
COMMIT;

-- ============================================================
-- Admin user seed
-- Thứ tự: USERS → ACCOUNT → WALLET
-- ============================================================
INSERT INTO USERS (role_id, email, password_hash, status, referral_code, is_deleted)
VALUES (1, 'admin@flexinvest.vn', 'admin123', 'ACTIVE', 'ADMIN001', 0);

INSERT INTO ACCOUNT (USER_ID, USERNAME, PASSWORD_HASH, STATUS, CREATED_AT, UPDATED_AT, IS_DELETED)
SELECT user_id, 'admin', 'admin123', 'ACTIVE', SYSDATE, SYSDATE, 0
  FROM USERS WHERE email = 'admin@flexinvest.vn';

INSERT INTO WALLET (USER_ID, AVAILABLE_BALANCE, LOCKED_BALANCE, STATUS, IS_DELETED)
SELECT user_id, 0, 0, 'ACTIVE', 0
  FROM USERS WHERE email = 'admin@flexinvest.vn';

COMMIT;
