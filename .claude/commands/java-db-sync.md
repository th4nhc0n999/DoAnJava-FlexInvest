# Java-DB Sync — FlexInvest

Skill này đồng bộ các file Java (Model/DAO) với schema gốc tại **D:/HQTCSDL/FlexInvest.sql**.

## Cách dùng
Gõ `/java-db-sync` để:
- Đọc D:/HQTCSDL/FlexInvest.sql làm nguồn sự thật
- Kiểm tra từng bảng có Model/DAO tương ứng chưa
- Tạo hoặc cập nhật các file còn thiếu/sai

---

## Quy tắc ánh xạ kiểu dữ liệu

| Oracle SQL                              | Java type            |
|-----------------------------------------|----------------------|
| `NUMBER GENERATED ALWAYS AS IDENTITY`   | `int` / `long`       |
| `NUMBER(10)` (khóa ngoại / ID)          | `int`                |
| `NUMBER(18,2)` (tiền tệ)               | `java.math.BigDecimal` |
| `NUMBER(6,4)` (lãi suất)               | `java.math.BigDecimal` |
| `NUMBER(18,4)` (token)                 | `java.math.BigDecimal` |
| `NUMBER(1)` (cờ boolean)               | `int`  (0/1), hoặc `boolean` nếu hiển thị |
| `VARCHAR2(n)`                           | `String`             |
| `CLOB`                                  | `String`             |
| `TIMESTAMP`                             | `java.sql.Timestamp` |
| `DATE`                                  | `java.sql.Date`      |

---

## Cấu trúc package

```
src/
├── Model/          ← POJO ánh xạ 1-1 với bảng
├── DAO/            ← CRUD dùng PreparedStatement, try-with-resources
├── Controller/     ← Business logic, không chứa SQL
├── View/           ← Swing UI (JFrame / JPanel)
│   └── permission/ ← Các màn hình quản lý quyền
├── Utils/          ← Tiện ích: PermissionUtils, SessionManager, ...
├── ConnectDB/      ← ConnectionOracle, ConnectionUtils
└── Resources/      ← db.properties, *.sql, assets
```

---

## Nguồn sự thật: FlexInvest.sql

Đường dẫn tuyệt đối: `D:/HQTCSDL/FlexInvest.sql`

### Bảng → Model → DAO

| # | Bảng SQL               | Model Java           | DAO Java                   | Ghi chú |
|---|------------------------|----------------------|----------------------------|---------|
| 1 | `ROLES`                | `Role.java`          | `RoleDAO.java`             | role_id(int), role_name, description, created_at, **is_deleted** |
| 2 | `USERS`                | `User.java`          | `UserDAO.java`             | user_id(**long** — IDENTITY), role_id(int), email, password_hash, status, referral_code, is_deleted |
| 3 | `EKYC`                 | `Ekyc.java`          | `EkycDAO.java`             | |
| 4 | `AUTH_SESSION`         | `AuthSession.java`   | `AuthSessionDAO.java`      | access_token, refresh_token, expired_at |
| 5 | `REFERRAL`             | `Referral.java`      | `ReferralDAO.java`         | |
| 6 | `WALLET`               | `Wallet.java`        | `WalletDAO.java`           | available_balance(BigDecimal), locked_balance(BigDecimal) |
| 7 | `BANK_ACCOUNT`         | `BankAccount.java`   | `BankAccountDAO.java`      | |
| 8 | `TRANSACTION_TYPE`     | `TransactionType.java`| `TransactionTypeDAO.java` | type_code là VARCHAR2 PK |
| 9 | `TRANSACTION`          | `Transaction.java`   | `TransactionDAO.java`      | amount(BigDecimal) |
|10 | `DEPOSIT`              | `Deposit.java`       | `DepositDAO.java`          | |
|11 | `WITHDRAW`             | `Withdraw.java`      | `WithdrawDAO.java`         | fee(BigDecimal) |
|12 | `LEDGER`               | `Ledger.java`        | `LedgerDAO.java`           | debit/credit/balance_after(BigDecimal) |
|13 | `SAVINGS_PRODUCT`      | `SavingsProduct.java`| `SavingsProductDAO.java`   | interest_rate(BigDecimal), min/max_investment(BigDecimal) |
|14 | `INVESTMENT`           | `Investment.java`    | `InvestmentDAO.java`       | invested_amount(BigDecimal), applied_interest_rate(BigDecimal) |
|15 | `PAYOUT`               | `Payout.java`        | `PayoutDAO.java`           | payout_amount(BigDecimal) |
|16 | `EARLY_REDEMPTION_EVENT`| `EarlyRedemptionEvent.java`| `EarlyRedemptionEventDAO.java` | |
|17 | `INTEREST_RATE`        | `InterestRate.java`  | `InterestRateDAO.java`     | old_rate/new_rate(BigDecimal) |
|18 | `TOKEN`                | `Token.java`         | `TokenDAO.java`            | balance/total_earned(BigDecimal) |
|19 | `TOKEN_TRANSACTION`    | `TokenTransaction.java`| `TokenTransactionDAO.java`| amount/balance_after(BigDecimal) |
|20 | `EXCHANGE_MONEY`       | `ExchangeMoney.java` | `ExchangeMoneyDAO.java`    | |
|21 | `MISSIONS`             | `Mission.java`       | `MissionDAO.java`          | reward_token(BigDecimal), is_active(int) |
|22 | `USER_MISSION`         | `UserMission.java`   | `UserMissionDAO.java`      | |
|23 | `AUDIT_LOG`            | `AuditLog.java`      | `AuditLogDAO.java`         | old_value/new_value → String (từ CLOB) |
|24 | `SUPPORT_TICKET`       | `SupportTicket.java` | `SupportTicketDAO.java`    | description → String (CLOB) |
|25 | `SUPPORT_TICKET_MESSAGE`| `SupportTicketMessage.java`| `SupportTicketMessageDAO.java` | content → String (CLOB) |
|26 | `NOTIFICATION`         | `Notification.java`  | `NotificationDAO.java`     | body → String (CLOB) |
|27 | `ACCOUNT`              | `Account.java`       | `AccountDAO.java`          | **Bảng đăng nhập**: account_id(int), user_id(int), username, password_hash, status, is_deleted |
|28 | `SYS_FUNCTION`         | `SysFunction.java`   | `SysFunctionDAO.java`      | function_id(**int**), name_function |
|29 | `SYS_ROLE`             | `SysRole.java`       | `SysRoleDAO.java`          | role_id(int), function_id(int), **view_perm, add_perm, edit_perm, delete_perm, download_perm** (int 0/1), is_deleted |
|30 | `ROLE_GROUP`           | `RoleGroup.java`     | `RoleGroupDAO.java`        | role_group_id(int), name_role_group, is_deleted |
|31 | `ROLE_GROUP_ASSIGN_ROLE`| *(pivot)*           | `RoleGroupDAO.java`        | Quản lý qua RoleGroupDAO |
|32 | `ACCOUNT_ASSIGN_ROLE_GROUP`| *(pivot)*        | `AccountPermissionDAO.java`| |
|33 | `ACCOUNT_ASSIGN_ROLE`  | *(pivot)*            | `AccountPermissionDAO.java`| |

---

## Schema phân quyền (bảng 27-33) — chi tiết

```
ACCOUNT ──────────────────────────────────────────────────────────┐
  account_id   NUMBER(10) IDENTITY PK                             │
  user_id      NUMBER(10) FK → USERS(user_id)                     │
  username     VARCHAR2(50) UNIQUE                                 │
  password_hash VARCHAR2(200)                                      │
  status       VARCHAR2(20) DEFAULT 'ACTIVE'                       │
  is_deleted   NUMBER(1)  DEFAULT 0                               │
                                                                   │
ACCOUNT_ASSIGN_ROLE ←──────────────────────────────── FK(account_id)
  account_id   NUMBER(10) FK → ACCOUNT
  role_id      NUMBER(10) FK → SYS_ROLE
  is_deleted   NUMBER(1)

ACCOUNT_ASSIGN_ROLE_GROUP ←────────────────────────── FK(account_id)
  account_id     NUMBER(10) FK → ACCOUNT
  role_group_id  NUMBER(10) FK → ROLE_GROUP
  is_deleted     NUMBER(1)

ROLE_GROUP
  role_group_id   NUMBER(10) IDENTITY PK
  name_role_group VARCHAR2(100)
  is_deleted      NUMBER(1)

ROLE_GROUP_ASSIGN_ROLE
  role_group_id   NUMBER(10) FK → ROLE_GROUP
  role_id         NUMBER(10) FK → SYS_ROLE
  is_deleted      NUMBER(1)

SYS_ROLE
  role_id       NUMBER(10) IDENTITY PK
  function_id   NUMBER(10) FK → SYS_FUNCTION
  add_perm      NUMBER(1)   ← Quyền Thêm
  edit_perm     NUMBER(1)   ← Quyền Sửa
  delete_perm   NUMBER(1)   ← Quyền Xóa
  download_perm NUMBER(1)   ← Quyền Tải
  view_perm     NUMBER(1)   ← Quyền Xem
  is_deleted    NUMBER(1)

SYS_FUNCTION
  function_id   NUMBER(10) IDENTITY PK
  name_function VARCHAR2(100)
  created_at    DATE
  updated_at    DATE
```

---

## Common Function — PermissionUtils

Signature:
```java
// Trả về tất cả SysRole của account cho một function cụ thể
List<SysRole> PermissionUtils.getAccountSysRoles(int accountId, int functionId)

// Trả về Permission đã Merge (OR) của account trên function đó
Permission PermissionUtils.getMergedPermission(int accountId, int functionId)
```

**Luồng merge:**
1. Lấy SysRole qua trực tiếp: `ACCOUNT_ASSIGN_ROLE → SYS_ROLE` WHERE `function_id = ?` AND `is_deleted = 0`
2. Lấy SysRole qua nhóm: `ACCOUNT_ASSIGN_ROLE_GROUP → ROLE_GROUP_ASSIGN_ROLE → SYS_ROLE` WHERE `function_id = ?` AND `is_deleted = 0` ở mọi bảng
3. Merge bằng OR tất cả 5 quyền: view_perm, add_perm, edit_perm, delete_perm, download_perm

**Permission object:**
```java
public class Permission {
    int accountId, functionId;
    boolean viewPerm, addPerm, editPerm, deletePerm, downloadPerm;
    List<SysRole> sourceRoles;  // các role đóng góp vào kết quả
}
```

---

## Quy ước viết DAO

```java
public class XxxDAO {
    private Xxx mapRow(ResultSet rs) throws SQLException { ... }

    public List<Xxx> getAll()                // SELECT, is_deleted = 0
    public Xxx getById(int id)               // SELECT WHERE id = ?
    public boolean insert(Xxx obj)           // INSERT
    public boolean update(Xxx obj)           // UPDATE WHERE id = ?
    public boolean softDelete(int id)        // UPDATE SET is_deleted = 1
}
```

- Luôn dùng `try-with-resources` cho Connection, PreparedStatement, ResultSet
- Dùng `con.setAutoCommit(false)` + `con.commit()` cho thao tác nhiều câu lệnh
- Dùng `System.err.println("DaoName.method: " + e)` để log lỗi

---

## Checklist khi tạo file Java mới

Khi tạo file Java liên quan đến database, hãy:
1. ✅ Tra cứu tên bảng và cột chính xác trong bảng trên
2. ✅ Dùng đúng kiểu Java theo bảng ánh xạ
3. ✅ Đặt tên trường Java theo camelCase của tên cột SQL (ví dụ `name_function` → `nameFunction`)
4. ✅ Model có đủ constructor, getter, setter
5. ✅ DAO có `mapRow()` private để tái sử dụng
6. ✅ Không hardcode SQL string ngoài DAO
7. ✅ Mọi `is_deleted = 1` dùng soft-delete, không DELETE thật
8. ✅ `SYS_ROLE` có 5 quyền: VIEW, ADD, EDIT, DELETE, DOWNLOAD (không phải 4!)
9. ✅ `ACCOUNT` khác `USERS` — login dùng `ACCOUNT.USERNAME`, profile dùng `USERS`
10. ✅ `USERS.user_id` là **NUMBER** (long), không phải VARCHAR2
