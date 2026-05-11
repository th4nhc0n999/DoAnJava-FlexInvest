package Controller;

import DAO.AccountDAO;
import DAO.UserDAO;
import Model.Account;
import Model.AccountModel;
import Model.Permission;
import Model.User;
import Utils.PasswordUtils;
import Utils.PermissionUtils;
import Utils.TokenUtils;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * Xử lý đăng nhập và xác thực người dùng.
 *
 * Sau khi xác thực thành công, controller:
 *   1. Tạo session token.
 *   2. Load tất cả Permission của account vào AccountModel
 *      (dùng PermissionUtils — gộp từ SysRole trực tiếp + qua RoleGroup).
 */
public class LoginController {

    /**
     * Danh sách FUNCTION_ID cần load permission khi login.
     * Thêm ID tương ứng với các màn hình trong SYS_FUNCTION vào đây.
     * Ví dụ: 1 = Quản lý User, 2 = Giao dịch, 3 = Báo cáo, ...
     */
    private static final int[] FUNCTION_IDS = {1, 2, 3, 4, 5};

    private final AccountDAO accountDAO = new AccountDAO();
    private final UserDAO    userDAO    = new UserDAO();

    /**
     * Xác thực đăng nhập.
     *
     * @param username tên đăng nhập
     * @param password mật khẩu plaintext
     * @return AccountModel (chứa account, user, token, loginTime, permissions)
     *         hoặc null nếu xác thực thất bại
     */
    public AccountModel loginController(String username, String password) {
        // ── 1. Lấy account theo username ─────────────────────────────────────
        Account account = accountDAO.getByUsername(username);

        if (account == null)                              return null;
        if (account.getIsDeleted() == 1)                  return null;
        if (!"ACTIVE".equals(account.getStatus()))        return null;
        if (!account.getPasswordHash().equals(PasswordUtils.hash(password))) return null;

        // ── 2. Lấy thông tin user ─────────────────────────────────────────────
        User user = userDAO.getUserById(account.getUserId());
        System.out.println("User: " + user);
        if (user == null) return null;

        // ── 3. Tạo token ──────────────────────────────────────────────────────
        String token = TokenUtils.generate(account.getAccountId());
        Timestamp loginTime = new Timestamp(System.currentTimeMillis());

        // ── 4. Load Permission cho tất cả function ────────────────────────────
        Map<Integer, Permission> permissions = loadPermissions(account.getAccountId());

        // ── 5. Trả về AccountModel ────────────────────────────────────────────
        return new AccountModel(account, user, token, loginTime, permissions);
    }

    /**
     * Load và cache Permission của account cho từng FUNCTION_ID.
     *
     * @param accountId ACCOUNT.ACCOUNT_ID
     * @return Map<functionId, Permission>
     */
    private Map<Integer, Permission> loadPermissions(int accountId) {
        Map<Integer, Permission> map = new HashMap<>();
        for (int fnId : FUNCTION_IDS) {
            Permission perm = PermissionUtils.getMergedPermission(accountId, fnId);
            map.put(fnId, perm);
        }
        return map;
    }
}
