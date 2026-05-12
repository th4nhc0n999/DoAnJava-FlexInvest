package View;

import ConnectDB.ConnectionOracle;
import Controller.NotificationController;
import Model.Transaction;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Panel quản lý người dùng (dành cho Admin).
 *
 * Chức năng:
 *  - Bảng danh sách users, có filter theo Role và kycStatus
 *  - Click dòng → panel bên phải hiển thị thông tin chi tiết
 *    + lịch sử giao dịch gần đây (5 giao dịch)
 *  - Nút Khoá / Mở khoá tài khoản
 */
public class UserManagementPanel extends JPanel {

    private static final Color NAVY    = new Color(15,  40,  80);
    private static final Color BLUE    = new Color(0,  162, 232);
    private static final Color BG      = new Color(238, 243, 250);
    private static final Color CARD_BG = new Color(245, 248, 252);
    private static final Color RED     = new Color(220, 60, 60);
    private static final Color GREEN   = new Color(0,  160, 100);

    private JTable table;
    private DefaultTableModel tableModel;
    private List<UserRow> userRows = new ArrayList<>();

    // Filter
    private JComboBox<String> cbRole;
    private JComboBox<String> cbKyc;
    private JTextField tfSearch;

    // Detail panel bên phải
    private JPanel detailPanel;
    private JLabel lblDetailName, lblDetailEmail, lblDetailRole;
    private JLabel lblDetailStatus, lblDetailKyc, lblDetailCreated;
    private JTextArea taHistory;
    private JButton btnLockUnlock;

    private final NotificationController notifController = new NotificationController();

    public UserManagementPanel() {
        initUI();
        loadUsers(null, null, null);
    }

    // ── Build UI ─────────────────────────────────────────────────────────────

    private void initUI() {
        setBackground(BG);
        setLayout(new BorderLayout(16, 0));
        setBorder(new EmptyBorder(24, 24, 24, 24));

        add(buildLeft(),   BorderLayout.CENTER);
        add(buildRight(),  BorderLayout.EAST);
    }

    // ── Panel trái: filter + bảng ─────────────────────────────────────────

    private JPanel buildLeft() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);

        JLabel title = new JLabel("Quản lý người dùng");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(30, 30, 40));

        p.add(title, BorderLayout.NORTH);
        p.add(buildFilterBar(), BorderLayout.CENTER);

        // Bảng
        String[] cols = {"ID", "Username", "Email", "Role", "KYC", "Trạng thái", "Ngày tạo"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(34);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(NAVY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(220, 228, 240));

        // Renderer màu dòng + highlight LOCKED
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                String status = (String) tableModel.getValueAt(row, 5);
                if (sel) {
                    setBackground(new Color(200, 220, 255));
                } else if ("LOCKED".equals(status) || "INACTIVE".equals(status)) {
                    setBackground(new Color(255, 240, 240));
                } else {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 251, 255));
                }
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        });

        table.getColumnModel().getColumn(0).setMaxWidth(50);

        // Click dòng → hiển thị detail
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showDetail(table.getSelectedRow());
        });

        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setOpaque(false);
        tableWrapper.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(buildFilterBar(), BorderLayout.NORTH);
        content.add(tableWrapper,     BorderLayout.CENTER);

        p.add(content, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setOpaque(false);

        tfSearch = new JTextField(14);
        tfSearch.putClientProperty("JTextField.placeholderText", "Tìm theo email / username...");

        cbRole = new JComboBox<>(new String[]{"Tất cả role", "Admin", "Staff", "Customer"});
        cbKyc  = new JComboBox<>(new String[]{"Tất cả KYC", "APPROVED", "PENDING", "REJECTED", "NONE"});

        JButton btnFilter = buildButton("Lọc", NAVY);
        btnFilter.addActionListener(e -> applyFilter());

        JButton btnReset = buildButton("Reset", new Color(130, 130, 140));
        btnReset.addActionListener(e -> {
            tfSearch.setText("");
            cbRole.setSelectedIndex(0);
            cbKyc.setSelectedIndex(0);
            loadUsers(null, null, null);
        });

        bar.add(new JLabel("Tìm:"));
        bar.add(tfSearch);
        bar.add(new JLabel("Role:"));
        bar.add(cbRole);
        bar.add(new JLabel("KYC:"));
        bar.add(cbKyc);
        bar.add(btnFilter);
        bar.add(btnReset);
        return bar;
    }

    // ── Panel phải: thông tin chi tiết ────────────────────────────────────

    private JPanel buildRight() {
        detailPanel = new JPanel();
        detailPanel.setBackground(CARD_BG);
        detailPanel.setPreferredSize(new Dimension(280, 0));
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
        detailPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 220, 235), 1, true),
            new EmptyBorder(20, 18, 20, 18)));

        JLabel title = new JLabel("Chi tiết người dùng");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(NAVY);
        title.setAlignmentX(LEFT_ALIGNMENT);

        lblDetailName    = detailLabel("—");
        lblDetailEmail   = detailLabel("—");
        lblDetailRole    = detailLabel("—");
        lblDetailStatus  = detailLabel("—");
        lblDetailKyc     = detailLabel("—");
        lblDetailCreated = detailLabel("—");

        taHistory = new JTextArea(6, 20);
        taHistory.setEditable(false);
        taHistory.setFont(new Font("Monospaced", Font.PLAIN, 11));
        taHistory.setBackground(new Color(245, 248, 252));
        taHistory.setText("Chọn một user để xem lịch sử");

        btnLockUnlock = buildButton("Khoá tài khoản", RED);
        btnLockUnlock.setEnabled(false);
        btnLockUnlock.setAlignmentX(LEFT_ALIGNMENT);
        btnLockUnlock.addActionListener(e -> toggleLock());

        detailPanel.add(title);
        detailPanel.add(Box.createVerticalStrut(14));
        detailPanel.add(fieldRow("Họ tên:",   lblDetailName));
        detailPanel.add(fieldRow("Email:",    lblDetailEmail));
        detailPanel.add(fieldRow("Role:",     lblDetailRole));
        detailPanel.add(fieldRow("Trạng thái:", lblDetailStatus));
        detailPanel.add(fieldRow("KYC:",      lblDetailKyc));
        detailPanel.add(fieldRow("Tạo lúc:",  lblDetailCreated));
        detailPanel.add(Box.createVerticalStrut(12));
        JLabel histTitle = new JLabel("5 giao dịch gần nhất:");
        histTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        histTitle.setAlignmentX(LEFT_ALIGNMENT);
        detailPanel.add(histTitle);
        detailPanel.add(Box.createVerticalStrut(4));
        JScrollPane scroll = new JScrollPane(taHistory);
        scroll.setAlignmentX(LEFT_ALIGNMENT);
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        detailPanel.add(scroll);
        detailPanel.add(Box.createVerticalStrut(14));
        detailPanel.add(btnLockUnlock);

        return detailPanel;
    }

    private JPanel fieldRow(String labelText, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(new Color(110, 110, 120));
        lbl.setPreferredSize(new Dimension(90, 20));
        row.add(lbl, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);
        return row;
    }

    private JLabel detailLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(30, 30, 40));
        return l;
    }

    // ── Load & filter ─────────────────────────────────────────────────────

    private void applyFilter() {
        String search = tfSearch.getText().trim();
        String role   = cbRole.getSelectedIndex() == 0 ? null
                : switch (cbRole.getSelectedIndex()) {
                    case 1 -> "1"; // Admin roleId
                    case 2 -> "2"; // Staff
                    case 3 -> "3"; // Customer
                    default -> null;
                };
        String kyc = cbKyc.getSelectedIndex() == 0 ? null
                : (String) cbKyc.getSelectedItem();
        loadUsers(search.isEmpty() ? null : search, role, kyc);
    }

    private void loadUsers(String search, String roleId, String kycStatus) {
        userRows.clear();
        tableModel.setRowCount(0);

        StringBuilder sql = new StringBuilder(
            "SELECT u.user_id, a.username, u.email, u.role_id, " +
            "NVL(e.verified_status, 'NONE') AS kyc_status, " +
            "a.status, a.account_id, u.created_at " +
            "FROM USERS u " +
            "JOIN ACCOUNT a ON u.user_id = a.user_id " +
            "LEFT JOIN EKYC e ON u.user_id = e.user_id AND e.is_deleted = 0 " +
            "WHERE u.is_deleted = 0 AND a.is_deleted = 0 ");

        if (search != null)
            sql.append("AND (LOWER(u.email) LIKE LOWER('%").append(search).append("%') ")
               .append("OR LOWER(a.username) LIKE LOWER('%").append(search).append("%')) ");
        if (roleId != null)
            sql.append("AND u.role_id = ").append(roleId).append(" ");
        if (kycStatus != null)
            sql.append("AND NVL(e.verified_status, 'NONE') = '").append(kycStatus).append("' ");

        sql.append("ORDER BY u.user_id DESC");

        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String roleLabel = switch (rs.getInt("role_id")) {
                    case 1 -> "Admin";
                    case 2 -> "Staff";
                    default -> "Customer";
                };
                UserRow row = new UserRow(
                    rs.getInt("user_id"),
                    rs.getInt("account_id"),
                    rs.getString("username"),
                    rs.getString("email"),
                    roleLabel,
                    rs.getString("kyc_status"),
                    rs.getString("status"),
                    rs.getString("created_at") != null
                        ? rs.getTimestamp("created_at").toString().substring(0, 10) : "—"
                );
                userRows.add(row);
                tableModel.addRow(new Object[]{
                    row.userId, row.username, row.email,
                    row.role, row.kycStatus, row.status, row.createdAt
                });
            }
        } catch (Exception e) {
            System.err.println("[UserManagementPanel.loadUsers] " + e.getMessage());
        }
    }

    // ── Hiển thị chi tiết ────────────────────────────────────────────────

    private void showDetail(int row) {
        if (row < 0 || row >= userRows.size()) return;
        UserRow u = userRows.get(row);

        lblDetailName.setText(u.username);
        lblDetailEmail.setText(u.email);
        lblDetailRole.setText(u.role);
        lblDetailCreated.setText(u.createdAt);

        // Status màu
        lblDetailStatus.setText(u.status);
        lblDetailStatus.setForeground(
            "ACTIVE".equals(u.status) ? GREEN :
            "LOCKED".equals(u.status) ? RED   : new Color(130, 130, 130));

        // KYC màu
        lblDetailKyc.setText(u.kycStatus);
        lblDetailKyc.setForeground(
            "APPROVED".equals(u.kycStatus)  ? GREEN :
            "REJECTED".equals(u.kycStatus)  ? RED   :
            "PENDING".equals(u.kycStatus)   ? new Color(200, 120, 0) :
            new Color(130, 130, 130));

        // Lịch sử giao dịch
        loadRecentTransactions(u.userId);

        // Nút khoá/mở
        btnLockUnlock.setEnabled(true);
        if ("LOCKED".equals(u.status) || "INACTIVE".equals(u.status)) {
            btnLockUnlock.setText("Mở khoá tài khoản");
            btnLockUnlock.setBackground(GREEN);
        } else {
            btnLockUnlock.setText("Khoá tài khoản");
            btnLockUnlock.setBackground(RED);
        }
    }

    private void loadRecentTransactions(int userId) {
        StringBuilder sb = new StringBuilder();
        String sql = "SELECT t.type_code, t.amount, t.status, t.created_at " +
                     "FROM TRANSACTION t " +
                     "JOIN WALLET w ON t.wallet_id = w.wallet_id " +
                     "WHERE w.user_id = ? AND t.is_deleted = 0 " +
                     "ORDER BY t.created_at DESC FETCH FIRST 5 ROWS ONLY";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    sb.append(String.format("[%s] %s — %,.0f VNĐ (%s)%n",
                        rs.getTimestamp("created_at").toString().substring(0, 10),
                        rs.getString("type_code"),
                        rs.getBigDecimal("amount").doubleValue(),
                        rs.getString("status")));
                }
                if (count == 0) sb.append("Chưa có giao dịch nào.");
            }
        } catch (Exception e) {
            sb.append("Không thể tải lịch sử.");
        }
        taHistory.setText(sb.toString());
    }

    // ── Khoá / Mở khoá ───────────────────────────────────────────────────

    private void toggleLock() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        UserRow u = userRows.get(row);

        boolean isLocked = "LOCKED".equals(u.status) || "INACTIVE".equals(u.status);
        String action = isLocked ? "mở khoá" : "khoá";

        int confirm = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc muốn " + action + " tài khoản \"" + u.username + "\"?",
            "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String newStatus = isLocked ? "ACTIVE" : "LOCKED";
        String sql = "UPDATE ACCOUNT SET status = ? WHERE account_id = ?";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, u.accountId);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                // Gửi notification cho user
                String notifMsg = isLocked
                    ? "Tài khoản của bạn đã được mở khoá."
                    : "Tài khoản của bạn đã bị khoá. Liên hệ hỗ trợ để biết thêm.";
                notifController.sendToUser(u.userId, notifMsg, "SYSTEM");

                JOptionPane.showMessageDialog(this,
                    "Đã " + action + " tài khoản thành công.",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
                loadUsers(null, null, null);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Lỗi: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private JButton buildButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 14, 7, 14));
        return btn;
    }

    // ── Inner record ──────────────────────────────────────────────────────

    private static class UserRow {
        final int    userId, accountId;
        final String username, email, role, kycStatus, status, createdAt;

        UserRow(int userId, int accountId, String username, String email,
                String role, String kycStatus, String status, String createdAt) {
            this.userId    = userId;
            this.accountId = accountId;
            this.username  = username;
            this.email     = email;
            this.role      = role;
            this.kycStatus = kycStatus;
            this.status    = status;
            this.createdAt = createdAt;
        }
    }
}
