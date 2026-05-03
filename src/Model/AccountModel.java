package Model;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Map;

/**
 * DTO session sau khi đăng nhập thành công.
 * Chứa: Account, User, token, thời gian login, và map Permission theo functionId.
 */
public class AccountModel {
    private final Account              account;
    private final User                 user;
    private final String               token;
    private final Timestamp            loginTime;
    private final Map<Integer, Permission> permissions;   // functionId → Permission

    /** Constructor đầy đủ (dùng sau khi load permission). */
    public AccountModel(Account account, User user, String token,
                        Timestamp loginTime, Map<Integer, Permission> permissions) {
        this.account     = account;
        this.user        = user;
        this.token       = token;
        this.loginTime   = loginTime;
        this.permissions = permissions != null ? permissions : Collections.emptyMap();
    }

    /** Constructor rút gọn (backward-compatible, không có permission). */
    public AccountModel(Account account, User user, String token, Timestamp loginTime) {
        this(account, user, token, loginTime, Collections.emptyMap());
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Account   getAccount()   { return account; }
    public User      getUser()      { return user; }
    public String    getToken()     { return token; }
    public Timestamp getLoginTime() { return loginTime; }

    /** Trả về toàn bộ map permission (unmodifiable). */
    public Map<Integer, Permission> getPermissions() {
        return Collections.unmodifiableMap(permissions);
    }

    /**
     * Lấy Permission của account trên một function cụ thể.
     *
     * @param functionId SYS_FUNCTION.FUNCTION_ID
     * @return Permission đã merge, hoặc Permission rỗng (không có quyền gì) nếu chưa load.
     */
    public Permission getPermission(int functionId) {
        return permissions.getOrDefault(functionId,
               new Permission(account.getAccountId(), functionId));
    }

    /** Shortcut: kiểm tra quyền xem một chức năng. */
    public boolean canView(int functionId)     { return getPermission(functionId).isViewPerm(); }
    /** Shortcut: kiểm tra quyền thêm. */
    public boolean canAdd(int functionId)      { return getPermission(functionId).isAddPerm(); }
    /** Shortcut: kiểm tra quyền sửa. */
    public boolean canEdit(int functionId)     { return getPermission(functionId).isEditPerm(); }
    /** Shortcut: kiểm tra quyền xóa. */
    public boolean canDelete(int functionId)   { return getPermission(functionId).isDeletePerm(); }
    /** Shortcut: kiểm tra quyền tải xuống. */
    public boolean canDownload(int functionId) { return getPermission(functionId).isDownloadPerm(); }
}
