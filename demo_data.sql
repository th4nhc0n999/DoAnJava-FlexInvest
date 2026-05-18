-- =============================================================================
-- FlexInvest — Demo Data Script
-- Mục đích: Insert data mẫu cho buổi demo nội bộ
-- Ngày tạo: 2026-05-18
--
-- Tài khoản demo:
--   Admin:    username=admin   / password=admin123  / email=admin@flexinvest.vn
--   Staff:    username=staff   / password=staff123  / email=staff@flexinvest.vn
--   Customer: username=customer/ password=cus123    / email=customer@flexinvest.vn
--
-- HƯỚNG DẪN CHẠY:
--   1. Đảm bảo đã chạy script tạo bảng (schema.sql) trước
--   2. Chạy toàn bộ file này trên Oracle SQL*Plus / SQL Developer
--   3. Commit sau khi chạy xong
-- =============================================================================

-- =============================================================================
-- 0. Thêm cột payout_method và target_product_id vào INVESTMENT (nếu chưa có)
--    Dùng để lưu phương thức tất toán do user chọn trước ngày đáo hạn
-- =============================================================================
BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE INVESTMENT ADD (payout_method VARCHAR2(10), target_product_id NUMBER(10))';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -1430 THEN NULL; -- ORA-01430: column already exists → bỏ qua
        ELSE RAISE;
        END IF;
END;
/

-- Tắt constraints tạm thời nếu cần (Oracle)
-- ALTER TABLE ... DISABLE CONSTRAINT ...;

-- Xóa data cũ theo thứ tự phụ thuộc (safe cho demo environment)
DELETE FROM LEDGER         WHERE is_deleted IN (0,1);
DELETE FROM PAYOUT         WHERE is_deleted IN (0,1);
DELETE FROM INVESTMENT     WHERE is_deleted IN (0,1);
DELETE FROM DEPOSIT        WHERE is_deleted IN (0,1);
DELETE FROM WITHDRAW       WHERE is_deleted IN (0,1);
DELETE FROM TRANSACTION    WHERE is_deleted IN (0,1);
DELETE FROM EKYC           WHERE is_deleted IN (0,1);
DELETE FROM BANK_ACCOUNT   WHERE is_deleted IN (0,1);
DELETE FROM WALLET         WHERE is_deleted IN (0,1);
DELETE FROM ACCOUNT        WHERE is_deleted IN (0,1);
DELETE FROM USERS          WHERE is_deleted IN (0,1);
DELETE FROM SAVINGS_PRODUCT WHERE is_deleted IN (0,1);

-- Reset sequences nếu có (Oracle)
-- ALTER SEQUENCE users_seq RESTART START WITH 1;

-- =============================================================================
-- 1. USERS (role_id: 1=Admin, 2=Staff/Customer phân biệt qua account)
--    Lưu ý: PasswordUtils.hash() trong project trả về plaintext
--    → password lưu DB chính là plaintext: admin123, staff123, cus123
-- =============================================================================

INSERT INTO USERS (role_id, email, password_hash, status, referral_code, created_at, is_deleted)
VALUES (1, 'admin@flexinvest.vn', 'admin123', 'ACTIVE', 'ADMIN001', SYSDATE, 0);

INSERT INTO USERS (role_id, email, password_hash, status, referral_code, created_at, is_deleted)
VALUES (2, 'staff@flexinvest.vn', 'staff123', 'ACTIVE', 'STAFF001', SYSDATE, 0);

INSERT INTO USERS (role_id, email, password_hash, status, referral_code, created_at, is_deleted)
VALUES (3, 'customer@flexinvest.vn', 'cus123', 'ACTIVE', 'CUS001', SYSDATE, 0);

-- Lấy user_id vừa insert (Oracle dùng RETURNING hoặc tra sequence)
-- Giả sử user_id được sinh: admin=1, staff=2, customer=3
-- Nếu dùng sequence, thay &admin_id, &staff_id, &customer_id bằng giá trị thật

-- =============================================================================
-- 2. ACCOUNT (username/password để đăng nhập)
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
-- 3. WALLET (số dư ban đầu)
-- =============================================================================

INSERT INTO WALLET (user_id, available_balance, locked_balance, status, is_deleted)
SELECT user_id, 50000000, 0, 'ACTIVE', 0   -- Admin: 50 triệu
FROM USERS WHERE email = 'admin@flexinvest.vn';

INSERT INTO WALLET (user_id, available_balance, locked_balance, status, is_deleted)
SELECT user_id, 10000000, 0, 'ACTIVE', 0   -- Staff: 10 triệu
FROM USERS WHERE email = 'staff@flexinvest.vn';

INSERT INTO WALLET (user_id, available_balance, locked_balance, status, is_deleted)
SELECT user_id, 20000000, 0, 'ACTIVE', 0   -- Customer: 20 triệu
FROM USERS WHERE email = 'customer@flexinvest.vn';

-- =============================================================================
-- 4. SAVINGS_PRODUCT
-- =============================================================================

-- Gói 1: Flex-Safe (không kỳ hạn, lãi 2%/năm)
INSERT INTO SAVINGS_PRODUCT
  (product_name, interest_rate, term, min_investment_amount, max_investment_amount,
   fallback_interest_rate, min_holding_days, status, currency, is_deleted)
VALUES ('Flex-Safe Cơ Bản', 0.02, 0, 100000, NULL, 0, 0, 'ACTIVE', 'VNĐ', 0);

-- Gói 2: 1 tháng kỳ hạn (30 ngày, lãi 4%/năm, rút sớm 0%)
INSERT INTO SAVINGS_PRODUCT
  (product_name, interest_rate, term, min_investment_amount, max_investment_amount,
   fallback_interest_rate, min_holding_days, status, currency, is_deleted)
VALUES ('Tiết Kiệm 1 Tháng', 0.04, 30, 1000000, 500000000, 0, 15, 'ACTIVE', 'VNĐ', 0);

-- Gói 3: 3 tháng kỳ hạn (90 ngày, lãi 6%/năm, rút sớm 1%)
INSERT INTO SAVINGS_PRODUCT
  (product_name, interest_rate, term, min_investment_amount, max_investment_amount,
   fallback_interest_rate, min_holding_days, status, currency, is_deleted)
VALUES ('Tiết Kiệm 3 Tháng', 0.06, 90, 5000000, 1000000000, 0.01, 30, 'ACTIVE', 'VNĐ', 0);

-- Gói 4: 6 tháng kỳ hạn (180 ngày, lãi 7.5%/năm)
INSERT INTO SAVINGS_PRODUCT
  (product_name, interest_rate, term, min_investment_amount, max_investment_amount,
   fallback_interest_rate, min_holding_days, status, currency, is_deleted)
VALUES ('Tiết Kiệm 6 Tháng', 0.075, 180, 10000000, 2000000000, 0.02, 60, 'ACTIVE', 'VNĐ', 0);

-- Gói 5: VIP FlexToken (lãi 12%/năm, đặc biệt cho VIP)
INSERT INTO SAVINGS_PRODUCT
  (product_name, interest_rate, term, min_investment_amount, max_investment_amount,
   fallback_interest_rate, min_holding_days, status, currency, is_deleted)
VALUES ('Flex VIP 12 Tháng', 0.12, 365, 50000000, NULL, 0.03, 90, 'ACTIVE', 'FlexToken', 0);

-- =============================================================================
-- 5. INVESTMENT — 5 khoản ở các trạng thái khác nhau
-- =============================================================================

-- [INV-1] ACTIVE, Flex-Safe, mua 10 ngày trước — demo lãi tích lũy
INSERT INTO INVESTMENT
  (user_id, product_id, invested_amount, applied_interest_rate,
   deposit_date, maturity_date, status, payout_method, is_deleted)
SELECT
  (SELECT user_id FROM USERS WHERE email='customer@flexinvest.vn'),
  (SELECT product_id FROM SAVINGS_PRODUCT WHERE product_name='Flex-Safe Cơ Bản'),
  5000000, 0.02,
  SYSDATE - 10, NULL, 'ACTIVE', 'PT3', 0
FROM DUAL;

-- [INV-2] ACTIVE, 1-tháng, sắp đáo hạn 2 ngày nữa — demo batch processMaturity
INSERT INTO INVESTMENT
  (user_id, product_id, invested_amount, applied_interest_rate,
   deposit_date, maturity_date, status, payout_method, is_deleted)
SELECT
  (SELECT user_id FROM USERS WHERE email='customer@flexinvest.vn'),
  (SELECT product_id FROM SAVINGS_PRODUCT WHERE product_name='Tiết Kiệm 1 Tháng'),
  10000000, 0.04,
  SYSDATE - 28, SYSDATE + 2, 'ACTIVE', 'PT3', 0
FROM DUAL;

-- [INV-3] ACTIVE, 3-tháng, mua hôm qua (< 24h) — demo lãi = 0
INSERT INTO INVESTMENT
  (user_id, product_id, invested_amount, applied_interest_rate,
   deposit_date, maturity_date, status, payout_method, is_deleted)
SELECT
  (SELECT user_id FROM USERS WHERE email='customer@flexinvest.vn'),
  (SELECT product_id FROM SAVINGS_PRODUCT WHERE product_name='Tiết Kiệm 3 Tháng'),
  3000000, 0.06,
  SYSDATE - 0.5, SYSDATE + 89.5, 'ACTIVE', 'PT1', 0
FROM DUAL;

-- [INV-4] ACTIVE, 3-tháng, mua 35 ngày trước — demo lãi đã tích lũy 1 tháng
INSERT INTO INVESTMENT
  (user_id, product_id, invested_amount, applied_interest_rate,
   deposit_date, maturity_date, status, payout_method, is_deleted)
SELECT
  (SELECT user_id FROM USERS WHERE email='customer@flexinvest.vn'),
  (SELECT product_id FROM SAVINGS_PRODUCT WHERE product_name='Tiết Kiệm 3 Tháng'),
  8000000, 0.06,
  SYSDATE - 35, SYSDATE + 55, 'ACTIVE', 'PT2', 0
FROM DUAL;

-- [INV-5] COMPLETED, đã tất toán — demo lịch sử
INSERT INTO INVESTMENT
  (user_id, product_id, invested_amount, applied_interest_rate,
   deposit_date, maturity_date, status, payout_method, is_deleted)
SELECT
  (SELECT user_id FROM USERS WHERE email='customer@flexinvest.vn'),
  (SELECT product_id FROM SAVINGS_PRODUCT WHERE product_name='Tiết Kiệm 1 Tháng'),
  2000000, 0.04,
  SYSDATE - 60, SYSDATE - 30, 'COMPLETED', 'PT3', 0
FROM DUAL;

-- =============================================================================
-- 6. eKYC — 1 hồ sơ PENDING để Staff duyệt
-- =============================================================================

INSERT INTO EKYC
  (user_id, id_number, full_name, date_of_birth, gender,
   place_of_origin, place_of_residence,
   issue_date, expiry_date, issue_place,
   front_image_url, back_image_url, face_image_url,
   verified_status, created_at, is_deleted)
SELECT
  (SELECT user_id FROM USERS WHERE email='customer@flexinvest.vn'),
  '079204012345', 'Nguyễn Văn Demo', DATE '1998-06-15', 'Nam',
  'Hà Nội', 'TP. Hồ Chí Minh',
  DATE '2020-01-10', DATE '2030-01-10', 'Cục Cảnh sát QLHC về TTXH',
  '/demo/front.jpg', '/demo/back.jpg', '/demo/selfie.jpg',
  'PENDING', SYSDATE, 0
FROM DUAL;

-- =============================================================================
-- 7. BANK_ACCOUNT — tài khoản ngân hàng của customer
-- =============================================================================

INSERT INTO BANK_ACCOUNT (user_id, bank_name, account_number, is_linked, is_deleted)
SELECT user_id, 'Vietcombank', '1234567890', 1, 0
FROM USERS WHERE email = 'customer@flexinvest.vn';

-- =============================================================================
-- 8. TRANSACTION + DEPOSIT PENDING — 2 lệnh nạp chờ Staff duyệt
-- =============================================================================

-- Lệnh nạp PENDING #1 — 5 triệu
INSERT INTO TRANSACTION (wallet_id, type_code, amount, status, created_at, is_deleted)
SELECT w.wallet_id, 'DEPOSIT', 5000000, 'PENDING', SYSDATE - 0.1, 0
FROM WALLET w JOIN USERS u ON w.user_id = u.user_id
WHERE u.email = 'customer@flexinvest.vn';

INSERT INTO DEPOSIT (transaction_id, request_code, payment_gateway, receiving_account, is_deleted)
SELECT t.transaction_id,
       'DEP' || TO_CHAR(SYSDATE, 'YYYYMMDDSS') || '01',
       'BANKING', '9704000000000018', 0
FROM TRANSACTION t
JOIN WALLET w ON t.wallet_id = w.wallet_id
JOIN USERS u ON w.user_id = u.user_id
WHERE u.email = 'customer@flexinvest.vn'
  AND t.type_code = 'DEPOSIT' AND t.status = 'PENDING'
  AND ROWNUM = 1
ORDER BY t.created_at DESC;

-- Lệnh nạp PENDING #2 — 2 triệu
INSERT INTO TRANSACTION (wallet_id, type_code, amount, status, created_at, is_deleted)
SELECT w.wallet_id, 'DEPOSIT', 2000000, 'PENDING', SYSDATE - 0.05, 0
FROM WALLET w JOIN USERS u ON w.user_id = u.user_id
WHERE u.email = 'customer@flexinvest.vn';

INSERT INTO DEPOSIT (transaction_id, request_code, payment_gateway, receiving_account, is_deleted)
SELECT t.transaction_id,
       'DEP' || TO_CHAR(SYSDATE, 'YYYYMMDDSS') || '02',
       'MOMO', 'momo@flexinvest', 0
FROM (
  SELECT t.transaction_id, t.created_at
  FROM TRANSACTION t
  JOIN WALLET w ON t.wallet_id = w.wallet_id
  JOIN USERS u ON w.user_id = u.user_id
  WHERE u.email = 'customer@flexinvest.vn'
    AND t.type_code = 'DEPOSIT' AND t.status = 'PENDING'
  ORDER BY t.created_at DESC
) t WHERE ROWNUM = 1;

-- =============================================================================
-- COMMIT tất cả thay đổi
-- =============================================================================
COMMIT;

-- =============================================================================
-- KIỂM TRA KẾT QUẢ
-- =============================================================================
SELECT 'USERS'           AS tbl, COUNT(*) AS cnt FROM USERS           WHERE is_deleted=0
UNION ALL
SELECT 'ACCOUNT',              COUNT(*) FROM ACCOUNT          WHERE is_deleted=0
UNION ALL
SELECT 'WALLET',               COUNT(*) FROM WALLET           WHERE is_deleted=0
UNION ALL
SELECT 'SAVINGS_PRODUCT',      COUNT(*) FROM SAVINGS_PRODUCT  WHERE is_deleted=0
UNION ALL
SELECT 'INVESTMENT',           COUNT(*) FROM INVESTMENT        WHERE is_deleted=0
UNION ALL
SELECT 'EKYC',                 COUNT(*) FROM EKYC             WHERE is_deleted=0
UNION ALL
SELECT 'DEPOSIT_PENDING',      COUNT(*) FROM DEPOSIT d JOIN TRANSACTION t ON d.transaction_id=t.transaction_id WHERE t.status='PENDING' AND d.is_deleted=0
UNION ALL
SELECT 'BANK_ACCOUNT',         COUNT(*) FROM BANK_ACCOUNT     WHERE is_deleted=0;
