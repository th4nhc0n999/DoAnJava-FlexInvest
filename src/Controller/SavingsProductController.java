package Controller;

import ConnectDB.ConnectionOracle;
import DAO.InvestmentDAO;
import Model.SavingsProduct;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller quản lý gói sản phẩm tiết kiệm.
 *
 * Phân quyền: chỉ Admin mới được gọi createProduct() / updateProduct() /
 * toggleActive(). isOpenToday() dùng chung cho mọi role.
 */
public class SavingsProductController {

    private final InvestmentDAO productDAO = new InvestmentDAO();

    // ── 1. Tạo gói mới ──────────────────────────────────────────────────────

    /**
     * Tạo gói sản phẩm mới với đầy đủ validation.
     *
     * @return productId vừa tạo, hoặc -1 nếu thất bại
     */
    public int createProduct(SavingsProduct p) {
        String err = validate(p, true);
        if (err != null) {
            System.err.println("[SavingsProductController.createProduct] " + err);
            return -1;
        }
        // Mặc định status ACTIVE, is_deleted = 0
        p.setStatus("ACTIVE");
        p.setIsDeleted(0);
        int newId = productDAO.insertProduct(p);
        if (newId > 0) {
            System.out.printf("[createProduct] Tạo gói '%s' thành công (id=%d)%n",
                    p.getProductName(), newId);
        }
        return newId;
    }

    // ── 2. Cập nhật gói ─────────────────────────────────────────────────────

    /**
     * Cập nhật thông tin gói.
     * Không cho sửa nếu đang có Investment ACTIVE dùng gói đó.
     *
     * @return true nếu cập nhật thành công
     */
    public boolean updateProduct(SavingsProduct p) {
        String err = validate(p, false);
        if (err != null) {
            System.err.println("[SavingsProductController.updateProduct] " + err);
            return false;
        }

        if (hasActiveInvestment(p.getProductId())) {
            System.err.println("[updateProduct] Không thể sửa — đang có khoản đầu tư ACTIVE dùng gói này.");
            return false;
        }

        boolean ok = productDAO.updateProduct(p);
        if (ok) System.out.printf("[updateProduct] Cập nhật gói id=%d thành công.%n", p.getProductId());
        return ok;
    }

    // ── 3. Bật / Tắt gói ────────────────────────────────────────────────────

    /**
     * Bật hoặc tắt gói sản phẩm.
     * Không cho tắt nếu đang có Investment ACTIVE dùng gói đó.
     *
     * @param productId ID gói
     * @param active    true = bật (ACTIVE), false = tắt (INACTIVE)
     * @return true nếu thành công
     */
    public boolean toggleActive(int productId, boolean active) {
        if (!active && hasActiveInvestment(productId)) {
            System.err.println("[toggleActive] Không thể tắt — đang có khoản đầu tư ACTIVE.");
            return false;
        }
        String newStatus = active ? "ACTIVE" : "INACTIVE";
        boolean ok = productDAO.toggleProductStatus(productId, newStatus);
        if (ok) System.out.printf("[toggleActive] Gói id=%d → %s%n", productId, newStatus);
        return ok;
    }

    // ── 4. Xóa mềm ──────────────────────────────────────────────────────────

    /**
     * Xóa mềm gói sản phẩm.
     * Không cho xóa nếu đang có Investment ACTIVE.
     */
    public boolean deleteProduct(int productId) {
        if (hasActiveInvestment(productId)) {
            System.err.println("[deleteProduct] Không thể xóa — đang có khoản đầu tư ACTIVE.");
            return false;
        }
        // soft delete qua toggleProductStatus (đặt INACTIVE)
        return productDAO.toggleProductStatus(productId, "INACTIVE");
    }

    // ── 5. isOpenToday ───────────────────────────────────────────────────────

    /**
     * Kiểm tra gói có đang mở bán hôm nay không.
     * Delegate sang SavingsProductDAO.isOpenToday() đã xử lý logic
     * Flex-Sale (ngày đôi) và Flex-Holiday (ngày lễ).
     */
    public boolean isOpenToday(int productId) {
        SavingsProduct p = productDAO.getProductById(productId);
        return p != null && p.isOpenToday();
    }

    public boolean isOpenToday(SavingsProduct p) {
        return p != null && p.isOpenToday();
    }

    // ── 6. Query helper ──────────────────────────────────────────────────────

    public List<SavingsProduct> getAllProducts() {
        return productDAO.getAllActiveProducts();
    }

    public List<SavingsProduct> getActiveProducts() {
        return productDAO.getAllActiveProducts();
    }

    public List<SavingsProduct> getOpenTodayProducts() {
        return productDAO.getAllActiveProducts(); // filter thêm tại View nếu cần
    }

    public SavingsProduct getById(int productId) {
        return productDAO.getProductById(productId);
    }

    // ── 7. Validation ────────────────────────────────────────────────────────

    /**
     * Validate dữ liệu gói trước khi insert/update.
     *
     * @param isNew true nếu là tạo mới (bỏ qua check productId)
     * @return chuỗi lỗi đầu tiên gặp phải, hoặc null nếu hợp lệ
     */
    private String validate(SavingsProduct p, boolean isNew) {
        if (p == null)                               return "Dữ liệu gói không được null.";
        if (!isNew && p.getProductId() <= 0)         return "productId không hợp lệ.";

        if (p.getProductName() == null || p.getProductName().isBlank())
            return "Tên gói không được để trống.";

        if (p.getInterestRate() == null || p.getInterestRate().compareTo(BigDecimal.ZERO) < 0)
            return "Lãi suất phải >= 0.";

        if (p.getTerm() < 0)
            return "Kỳ hạn không được âm (0 = không kỳ hạn).";

        if (p.getMinInvestmentAmount() == null || p.getMinInvestmentAmount().compareTo(BigDecimal.ZERO) <= 0)
            return "Số tiền đầu tư tối thiểu phải > 0.";

        if (p.getMaxInvestmentAmount() != null
                && p.getMaxInvestmentAmount().compareTo(p.getMinInvestmentAmount()) < 0)
            return "Số tiền tối đa phải >= tối thiểu.";

        if (p.getStartDate() != null && p.getEndDate() != null
                && p.getEndDate().isBefore(p.getStartDate()))
            return "Ngày kết thúc phải sau ngày bắt đầu.";

        return null; // hợp lệ
    }

    // ── 8. Kiểm tra Investment ACTIVE ────────────────────────────────────────

    /**
     * Kiểm tra có Investment đang ACTIVE dùng gói này không.
     * Query trực tiếp — không cần InvestmentDAO đầy đủ.
     */
    private boolean hasActiveInvestment(int productId) {
        String sql = "SELECT COUNT(*) FROM INVESTMENT " +
                     "WHERE product_id = ? AND status = 'ACTIVE' AND is_deleted = 0";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            System.err.println("[hasActiveInvestment] Lỗi: " + e.getMessage());
        }
        return false;
    }
}
