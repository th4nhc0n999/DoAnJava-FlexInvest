-- =============================================================================
-- FlexInvest — Demo Data Script (FIXED)
-- Mục đích: Insert data mẫu cho buổi demo
--
-- Tài khoản demo:
--   Admin:    username=admin    / password=admin123  / email=admin@flexinvest.vn
--   Staff:    username=staff    / password=staff123  / email=staff@flexinvest.vn
--   Customer: username=customer / password=cus123    / email=customer@flexinvest.vn
--
-- HƯỚNG DẪN CHẠY:
--   1. Chạy FlexInvest_fixed.sql trước (tạo bảng + seed roles/functions)
--   2. Chạy file này trên Oracle SQL Developer / SQL*Plus
--   3. Script tự COMMIT ở cuối
-- =============================================================================

-- =============================================================================
-- 0. Thêm cột payout_method và target_product_id vào INVESTMENT nếu chưa có
--    (phòng trường hợp chạy schema cũ chưa có 2 cột này)
-- =============================================================================
BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE INVESTMENT ADD (payout_method VARCHAR2(10), target_product_id NUMBER(10))';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -1430 THEN NULL; -- ORA-01430: column already exists
        ELSE RAISE;
        END IF;
END;
/

-- =============================================================================
-- 1. Xóa data cũ theo thứ tự FK (safe cho môi trường demo)
-- =============================================================================
DELETE FROM LEDGER              WHERE is_deleted IN (0,1);
DELETE FROM PAYOUT              WHERE is_deleted IN (0,1);
DELETE FROM EARLY_REDEMPTION_EVENT WHERE is_deleted IN (0,1);
DELETE FROM INVESTMENT          WHERE is_deleted IN (0,1);
DELETE FROM DEPOSIT             WHERE is_deleted IN (0,1);
DELETE FROM WITHDRAW            WHERE is_deleted IN (0,1);
DELETE FROM "TRANSACTION"       WHERE is_deleted IN (0,1);
DELETE FROM EKYC                WHERE is_deleted IN (0,1);
DELETE FROM BANK_ACCOUNT        WHERE is_deleted IN (0,1);
DELETE FROM WALLET              WHERE is_deleted IN (0,1);
DELETE FROM ACCOUNT             WHERE is_deleted IN (0,1);
DELETE FROM USERS               WHERE is_deleted IN (0,1);
DELETE FROM SAVINGS_PRODUCT     WHERE is_deleted IN (0,1);
DELETE FROM MISSIONS            WHERE is_deleted IN (0,1);
COMMIT;

-- =============================================================================
-- 2. USERS
--    role_id: 1=Admin, 2=Staff, 3=Customer (khớp với RegisterController role_id=3)
--    password lưu plaintext vì PasswordUtils.hash() trả về plaintext
-- =============================================================================
INSERT INTO USERS (role_id, email, password_hash, status, referral_code, created_at, is_deleted)
VALUES (1, 'admin@flexinvest.vn', 'admin123', 'ACTIVE', 'ADMIN001', SYSDATE, 0);

INSERT INTO USERS (role_id, email, password_hash, status, referral_code, created_at, is_deleted)
VALUES (2, 'staff@flexinvest.vn', 'staff123', 'ACTIVE', 'STAFF001', SYSDATE, 0);

INSERT INTO USERS (role_id, email, password_hash, status, referral_code, created_at, is_deleted)
VALUES (3, 'customer@flexinvest.vn', 'cus123', 'ACTIVE', 'CUS001', SYSDATE, 0);

-- =============================================================================
-- 3. ACCOUNT (username/password để đăng nhập — khớp với AccountDAO)
-- =============================================================================
INSERT INTO ACCOUNT (user_id, username, password_hash, status, created_at, updated_at, is_deleted)
SELECT user_id, 'admin', 'admin123', 'ACTIVE', SYSDATE, SYSDATE, 0
FROM USERS WHERE email = 'admin@flexinvest.vn';

INSERT INTO ACCOUNT (user_id, username, password_hash, status, created_at, updated_at, is_deleted)
SELECT user_id, 'staff', 'staff123', 'ACTIVE', SYSDATE, SYSDATE, 0
FROM USERS WHERE email = 'staff@flexinvest.vn';

INSERT INTO ACCOUNT (user_id, username, password_hash, status, created_at, updated_at, is_deleted)
SELECT user_id, 'customer', 'cus123', 'ACTIVE', SYSDATE, SYSDATE, 0
FROM USERS WHERE email = 'customer@flexinvest.vn';

-- =============================================================================
-- 4. WALLET (số dư ban đầu — khớp với WalletDAO.insertWithConnection)
-- =============================================================================
INSERT INTO WALLET (user_id, available_balance, locked_balance, status, is_deleted)
SELECT user_id, 50000000, 0, 'ACTIVE', 0
FROM USERS WHERE email = 'admin@flexinvest.vn';

INSERT INTO WALLET (user_id, available_balance, locked_balance, status, is_deleted)
SELECT user_id, 10000000, 0, 'ACTIVE', 0
FROM USERS WHERE email = 'staff@flexinvest.vn';

INSERT INTO WALLET (user_id, available_balance, locked_balance, status, is_deleted)
SELECT user_id, 20000000, 0, 'ACTIVE', 0
FROM USERS WHERE email = 'customer@flexinvest.vn';

-- =============================================================================
-- 5. SAVINGS_PRODUCT (khớp InvestmentDAO.mapProduct: term=0 là Flex-Safe)
-- =============================================================================
INSERT INTO SAVINGS_PRODUCT
  (product_name, interest_rate, term, min_investment_amount, max_investment_amount,
   penalty_rate, fallback_interest_rate, min_holding_days, status, currency, is_deleted)
VALUES ('Flex-Safe Cơ Bản', 0.02, 0, 100000, NULL, 0, 0, 0, 'ACTIVE', 'VND', 0);

INSERT INTO SAVINGS_PRODUCT
  (product_name, interest_rate, term, min_investment_amount, max_investment_amount,
   penalty_rate, fallback_interest_rate, min_holding_days, status, currency, is_deleted)
VALUES ('Tiết Kiệm 1 Tháng', 0.04, 30, 1000000, 500000000, 0, 0, 15, 'ACTIVE', 'VND', 0);

INSERT INTO SAVINGS_PRODUCT
  (product_name, interest_rate, term, min_investment_amount, max_investment_amount,
   penalty_rate, fallback_interest_rate, min_holding_days, status, currency, is_deleted)
VALUES ('Tiết Kiệm 3 Tháng', 0.06, 90, 5000000, 1000000000, 0.01, 0, 30, 'ACTIVE', 'VND', 0);

INSERT INTO SAVINGS_PRODUCT
  (product_name, interest_rate, term, min_investment_amount, max_investment_amount,
   penalty_rate, fallback_interest_rate, min_holding_days, status, currency, is_deleted)
VALUES ('Tiết Kiệm 6 Tháng', 0.075, 180, 10000000, 2000000000, 0.02, 0, 60, 'ACTIVE', 'VND', 0);

INSERT INTO SAVINGS_PRODUCT
  (product_name, interest_rate, term, min_investment_amount, max_investment_amount,
   penalty_rate, fallback_interest_rate, min_holding_days, status, currency, is_deleted)
VALUES ('Flex VIP 12 Tháng', 0.12, 365, 50000000, NULL, 0.03, 0, 90, 'ACTIVE', 'VND', 0);

-- =============================================================================
-- 6. INVESTMENT
--    Dùng start_date (khớp InvestmentDAO.mapInvestment + insert SQL)
--    Dùng payout_method (khớp InvestmentDAO.setPayoutMethod/getPayoutMethod)
-- =============================================================================

-- [INV-1] ACTIVE, Flex-Safe, mua 10 ngày trước
INSERT INTO INVESTMENT
  (user_id, product_id, invested_amount, applied_interest_rate,
   start_date, maturity_date, status, payout_method, is_deleted)
SELECT
  (SELECT user_id FROM USERS WHERE email = 'customer@flexinvest.vn'),
  (SELECT product_id FROM SAVINGS_PRODUCT WHERE product_name = 'Flex-Safe Cơ Bản'),
  5000000, 0.02,
  SYSDATE - 10, NULL, 'ACTIVE', 'PT3', 0
FROM DUAL;

-- [INV-2] ACTIVE, 1 tháng, sắp đáo hạn 2 ngày nữa
INSERT INTO INVESTMENT
  (user_id, product_id, invested_amount, applied_interest_rate,
   start_date, maturity_date, status, payout_method, is_deleted)
SELECT
  (SELECT user_id FROM USERS WHERE email = 'customer@flexinvest.vn'),
  (SELECT product_id FROM SAVINGS_PRODUCT WHERE product_name = 'Tiết Kiệm 1 Tháng'),
  10000000, 0.04,
  SYSDATE - 28, SYSDATE + 2, 'ACTIVE', 'PT3', 0
FROM DUAL;

-- [INV-3] ACTIVE, 3 tháng, mới mua hôm qua
INSERT INTO INVESTMENT
  (user_id, product_id, invested_amount, applied_interest_rate,
   start_date, maturity_date, status, payout_method, is_deleted)
SELECT
  (SELECT user_id FROM USERS WHERE email = 'customer@flexinvest.vn'),
  (SELECT product_id FROM SAVINGS_PRODUCT WHERE product_name = 'Tiết Kiệm 3 Tháng'),
  3000000, 0.06,
  SYSDATE - 1, SYSDATE + 89, 'ACTIVE', 'PT1', 0
FROM DUAL;

-- [INV-4] ACTIVE, 3 tháng, mua 35 ngày trước
INSERT INTO INVESTMENT
  (user_id, product_id, invested_amount, applied_interest_rate,
   start_date, maturity_date, status, payout_method, is_deleted)
SELECT
  (SELECT user_id FROM USERS WHERE email = 'customer@flexinvest.vn'),
  (SELECT product_id FROM SAVINGS_PRODUCT WHERE product_name = 'Tiết Kiệm 3 Tháng'),
  8000000, 0.06,
  SYSDATE - 35, SYSDATE + 55, 'ACTIVE', 'PT2', 0
FROM DUAL;

-- [INV-5] COMPLETED, đã tất toán — demo lịch sử
INSERT INTO INVESTMENT
  (user_id, product_id, invested_amount, applied_interest_rate,
   start_date, maturity_date, status, payout_method, is_deleted)
SELECT
  (SELECT user_id FROM USERS WHERE email = 'customer@flexinvest.vn'),
  (SELECT product_id FROM SAVINGS_PRODUCT WHERE product_name = 'Tiết Kiệm 1 Tháng'),
  2000000, 0.04,
  SYSDATE - 60, SYSDATE - 30, 'COMPLETED', 'PT3', 0
FROM DUAL;

-- =============================================================================
-- 7. eKYC — 1 hồ sơ PENDING để Staff duyệt
-- =============================================================================
INSERT INTO EKYC
  (user_id, id_number, full_name, date_of_birth, gender,
   place_of_origin, place_of_residence,
   issue_date, expiry_date, issue_place,
   front_image_url, back_image_url, face_image_url,
   verified_status, created_at, updated_at, is_deleted)
SELECT
  (SELECT user_id FROM USERS WHERE email = 'customer@flexinvest.vn'),
  '079204012345', 'Nguyễn Văn Demo', DATE '1998-06-15', 'Nam',
  'Hà Nội', 'TP. Hồ Chí Minh',
  DATE '2020-01-10', DATE '2030-01-10', 'Cục Cảnh sát QLHC về TTXH',
  '/demo/front.jpg', '/demo/back.jpg', '/demo/selfie.jpg',
  'PENDING', SYSDATE, SYSDATE, 0
FROM DUAL;

-- =============================================================================
-- 8. BANK_ACCOUNT
-- =============================================================================
INSERT INTO BANK_ACCOUNT (user_id, bank_name, account_number, is_linked, is_deleted)
SELECT user_id, 'Vietcombank', '1234567890', 1, 0
FROM USERS WHERE email = 'customer@flexinvest.vn';

INSERT INTO BANK_ACCOUNT (user_id, bank_name, account_number, is_linked, is_deleted)
SELECT user_id, 'Techcombank', '0987654321', 1, 0
FROM USERS WHERE email = 'staff@flexinvest.vn';

-- =============================================================================
-- 9. TRANSACTION + DEPOSIT PENDING
--    Dùng "TRANSACTION" (có ngoặc kép) vì là reserved word Oracle
-- =============================================================================

-- Lệnh nạp PENDING #1 — 5 triệu
INSERT INTO "TRANSACTION" (wallet_id, type_code, amount, status, created_at, is_deleted)
SELECT w.wallet_id, 'DEPOSIT', 5000000, 'PENDING', SYSDATE - 0.1, 0
FROM WALLET w
JOIN USERS u ON w.user_id = u.user_id
WHERE u.email = 'customer@flexinvest.vn';

INSERT INTO DEPOSIT (transaction_id, request_code, payment_gateway, receiving_account, is_deleted)
SELECT t.transaction_id,
       'DEP' || TO_CHAR(SYSDATE, 'YYYYMMDD') || '001',
       'BANKING', '9704000000000018', 0
FROM "TRANSACTION" t
JOIN WALLET w ON t.wallet_id = w.wallet_id
JOIN USERS u  ON w.user_id = u.user_id
WHERE u.email = 'customer@flexinvest.vn'
  AND t.type_code = 'DEPOSIT'
  AND t.status = 'PENDING'
  AND ROWNUM = 1
ORDER BY t.created_at DESC;

-- Lệnh nạp PENDING #2 — 2 triệu
INSERT INTO "TRANSACTION" (wallet_id, type_code, amount, status, created_at, is_deleted)
SELECT w.wallet_id, 'DEPOSIT', 2000000, 'PENDING', SYSDATE - 0.05, 0
FROM WALLET w
JOIN USERS u ON w.user_id = u.user_id
WHERE u.email = 'customer@flexinvest.vn';

INSERT INTO DEPOSIT (transaction_id, request_code, payment_gateway, receiving_account, is_deleted)
SELECT t.transaction_id,
       'DEP' || TO_CHAR(SYSDATE, 'YYYYMMDD') || '002',
       'MOMO', 'momo@flexinvest', 0
FROM (
  SELECT t.transaction_id, t.created_at
  FROM "TRANSACTION" t
  JOIN WALLET w ON t.wallet_id = w.wallet_id
  JOIN USERS u  ON w.user_id = u.user_id
  WHERE u.email = 'customer@flexinvest.vn'
    AND t.type_code = 'DEPOSIT'
    AND t.status = 'PENDING'
  ORDER BY t.created_at DESC
) t WHERE ROWNUM = 1;

-- =============================================================================
-- 10. MISSIONS — vài nhiệm vụ mẫu
-- =============================================================================
INSERT INTO MISSIONS
  (title, description, mission_type, action_type, target_value, reward_token,
   is_active, sort_order, created_at, is_deleted)
VALUES ('Đầu tư lần đầu', 'Thực hiện khoản đầu tư đầu tiên', 'ONE_TIME', 'INVEST',
        1, 10, 1, 1, SYSDATE, 0);

INSERT INTO MISSIONS
  (title, description, mission_type, action_type, target_value, reward_token,
   is_active, sort_order, created_at, is_deleted)
VALUES ('Nạp tiền 5 triệu', 'Nạp tổng cộng 5,000,000 VND', 'ONE_TIME', 'DEPOSIT',
        5000000, 5, 1, 2, SYSDATE, 0);

INSERT INTO MISSIONS
  (title, description, mission_type, action_type, target_value, reward_token,
   is_active, sort_order, created_at, is_deleted)
VALUES ('Giới thiệu bạn bè', 'Giới thiệu 1 người dùng mới', 'ONE_TIME', 'REFERRAL',
        1, 20, 1, 3, SYSDATE, 0);

-- =============================================================================
-- COMMIT tất cả
-- =============================================================================
COMMIT;

-- =============================================================================
-- KIỂM TRA KẾT QUẢ
-- =============================================================================
SELECT 'USERS'          AS tbl, COUNT(*) AS cnt FROM USERS           WHERE is_deleted = 0
UNION ALL SELECT 'ACCOUNT',           COUNT(*) FROM ACCOUNT          WHERE is_deleted = 0
UNION ALL SELECT 'WALLET',            COUNT(*) FROM WALLET           WHERE is_deleted = 0
UNION ALL SELECT 'SAVINGS_PRODUCT',   COUNT(*) FROM SAVINGS_PRODUCT  WHERE is_deleted = 0
UNION ALL SELECT 'INVESTMENT',        COUNT(*) FROM INVESTMENT        WHERE is_deleted = 0
UNION ALL SELECT 'EKYC',              COUNT(*) FROM EKYC             WHERE is_deleted = 0
UNION ALL SELECT 'BANK_ACCOUNT',      COUNT(*) FROM BANK_ACCOUNT     WHERE is_deleted = 0
UNION ALL SELECT 'DEPOSIT_PENDING',   COUNT(*) FROM DEPOSIT d
          JOIN "TRANSACTION" t ON d.transaction_id = t.transaction_id
          WHERE t.status = 'PENDING' AND d.is_deleted = 0
UNION ALL SELECT 'MISSIONS',          COUNT(*) FROM MISSIONS         WHERE is_deleted = 0;
