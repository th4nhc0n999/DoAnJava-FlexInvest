package View;

import ConnectDB.ConnectionOracle;
import Controller.InvestmentController;
import Controller.NotificationController;
import Model.AccountModel;
import Model.SavingsProduct;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Panel Dashboard dành cho Admin.
 *
 * Hiển thị 4 số liệu tổng quan:
 *  1. Tổng người dùng
 *  2. Tổng tiền đang tích lũy
 *  3. Số lệnh chờ duyệt hôm nay
 *  4. Tổng lãi đã trả
 *
 * Nút "Chạy batch hôm nay":
 *  → Gọi InvestmentController.dailyFlexSafeAccrual()
 *  → Gọi InvestmentController.processMaturity() cho tất cả gói đáo hạn hôm nay
 *  → Gửi notification cho từng user có gói vừa tất toán
 */
public class AdminDashboardPanel extends JPanel {

    // ── Màu sắc theo theme MainPage ──────────────────────────────────────────
    private static final Color NAVY      = new Color(15,  40,  80);
    private static final Color BLUE      = new Color(0,  162, 232);
    private static final Color CARD_BG   = new Color(245, 248, 252);
    private static final Color TEXT_DARK = new Color(30,  30,  40);
    private static final Color TEXT_GRAY = new Color(110, 110, 120);
    private static final Color BG        = new Color(238, 243, 250);

    private final AccountModel currentAccount;
    private final InvestmentController investmentController;
    private final NotificationController notifController = new NotificationController();

    // Labels số liệu — cập nhật lại khi refresh
    private JLabel lblTotalUsers;
    private JLabel lblTotalInvested;
    private JLabel lblPending;
    private JLabel lblTotalInterest;
    private JButton btnBatch;

    public AdminDashboardPanel(AccountModel account) {
        this.currentAccount       = account;
        this.investmentController = new InvestmentController();
        initUI();
        loadStats();
    }

    // ── Build UI ─────────────────────────────────────────────────────────────

    private void initUI() {
        setBackground(BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(28, 28, 28, 28));

        // Tiêu đề
        JLabel title = new JLabel("Admin Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_DARK);
        title.setAlignmentX(LEFT_ALIGNMENT);
        add(title);

        JLabel sub = new JLabel("Tổng quan hệ thống FlexInvest");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(TEXT_GRAY);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        add(sub);
        add(Box.createVerticalStrut(24));

        // Hàng 4 cards số liệu
        add(buildStatsRow());
        add(Box.createVerticalStrut(24));

        // Nút batch
        add(buildBatchSection());
        add(Box.createVerticalStrut(16));

        // Nút refresh
        JButton btnRefresh = new JButton("🔄  Làm mới số liệu");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnRefresh.setFocusPainted(false);
        btnRefresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRefresh.setAlignmentX(LEFT_ALIGNMENT);
        btnRefresh.addActionListener(e -> loadStats());
        add(btnRefresh);
    }

    private JPanel buildStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        row.setAlignmentX(LEFT_ALIGNMENT);

        lblTotalUsers    = new JLabel("—");
        lblTotalInvested = new JLabel("—");
        lblPending       = new JLabel("—");
        lblTotalInterest = new JLabel("—");

        row.add(buildStatCard("Tổng người dùng",      lblTotalUsers,    new Color(0, 180, 120)));
        row.add(buildStatCard("Đang tích lũy (VNĐ)",  lblTotalInvested, BLUE));
        row.add(buildStatCard("Lệnh chờ duyệt",       lblPending,       new Color(230, 140, 0)));
        row.add(buildStatCard("Tổng lãi đã trả (VNĐ)",lblTotalInterest, new Color(160, 100, 220)));
        return row;
    }

    private JPanel buildStatCard(String title, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
            new EmptyBorder(16, 16, 16, 16)));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTitle.setForeground(TEXT_GRAY);
        lblTitle.setAlignmentX(LEFT_ALIGNMENT);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        valueLabel.setForeground(TEXT_DARK);
        valueLabel.setAlignmentX(LEFT_ALIGNMENT);

        card.add(lblTitle);
        card.add(Box.createVerticalStrut(8));
        card.add(valueLabel);
        return card;
    }

    private JPanel buildBatchSection() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD_BG);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 220, 235), 1, true),
            new EmptyBorder(20, 24, 20, 24)));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        panel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel lbl = new JLabel("Xử lý batch cuối ngày");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(TEXT_DARK);
        lbl.setAlignmentX(LEFT_ALIGNMENT);

        JLabel hint = new JLabel("Cộng lãi Flex-Safe hàng ngày và tất toán các gói đáo hạn hôm nay.");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hint.setForeground(TEXT_GRAY);
        hint.setAlignmentX(LEFT_ALIGNMENT);

        btnBatch = new JButton("▶  Chạy batch hôm nay");
        btnBatch.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBatch.setForeground(Color.WHITE);
        btnBatch.setBackground(NAVY);
        btnBatch.setBorderPainted(false);
        btnBatch.setFocusPainted(false);
        btnBatch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBatch.setAlignmentX(LEFT_ALIGNMENT);
        btnBatch.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btnBatch.setBackground(BLUE); }
            public void mouseExited (java.awt.event.MouseEvent e) { btnBatch.setBackground(NAVY); }
        });
        btnBatch.addActionListener(e -> runBatch());

        panel.add(lbl);
        panel.add(Box.createVerticalStrut(4));
        panel.add(hint);
        panel.add(Box.createVerticalStrut(12));
        panel.add(btnBatch);
        return panel;
    }

    // ── Load số liệu từ DB ───────────────────────────────────────────────────

    private void loadStats() {
        SwingUtilities.invokeLater(() -> {
            lblTotalUsers.setText(String.valueOf(queryCount(
                "SELECT COUNT(*) FROM USERS WHERE is_deleted = 0")));

            lblTotalInvested.setText(formatMoney(querySum(
                "SELECT NVL(SUM(invested_amount), 0) FROM INVESTMENT " +
                "WHERE status = 'ACTIVE' AND is_deleted = 0")));

            lblPending.setText(String.valueOf(queryCount(
                "SELECT COUNT(*) FROM DEPOSIT " +
                "WHERE status = 'PENDING' AND is_deleted = 0 " +
                "UNION ALL " +
                "SELECT COUNT(*) FROM WITHDRAW " +
                "WHERE status = 'PENDING' AND is_deleted = 0")));

            lblTotalInterest.setText(formatMoney(querySum(
                "SELECT NVL(SUM(interest_amount), 0) FROM PAYOUT WHERE is_deleted = 0")));
        });
    }

    // ── Chạy batch ───────────────────────────────────────────────────────────

    private void runBatch() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Xác nhận chạy batch hôm nay?\n" +
            "Thao tác này sẽ:\n" +
            "  • Cộng lãi ngày cho tất cả gói Flex-Safe\n" +
            "  • Tự động tất toán các gói đáo hạn hôm nay",
            "Xác nhận batch", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        btnBatch.setEnabled(false);
        btnBatch.setText("Đang xử lý...");

        // Chạy trên background thread để không đơ UI
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                StringBuilder result = new StringBuilder();

                // ── Bước 1: Cộng lãi Flex-Safe ───────────────────────────────
                int flexSafeProductId = getFlexSafeProductId();
                if (flexSafeProductId > 0) {
                    int accrued = investmentController.dailyFlexSafeAccrual(flexSafeProductId);
                    result.append("✔ Cộng lãi Flex-Safe: ").append(accrued).append(" khoản\n");
                } else {
                    result.append("⚠ Không tìm thấy gói Flex-Safe\n");
                }

                // ── Bước 2: Tất toán gói đáo hạn hôm nay ────────────────────
                List<Integer> maturedIds = getMaturedInvestmentIds();
                int maturedCount = 0;
                for (int invId : maturedIds) {
                    boolean ok = investmentController.processMaturity(invId, null, null);
                    if (ok) maturedCount++;
                }
                result.append("✔ Tất toán đáo hạn: ").append(maturedCount)
                      .append("/").append(maturedIds.size()).append(" khoản\n");

                // ── Bước 3: Gửi notification ─────────────────────────────────
                // NotificationController đã gửi trong processMaturity()
                // Gửi thêm broadcast nếu cần
                if (maturedCount > 0) {
                    result.append("✔ Đã gửi thông báo cho các user đáo hạn\n");
                }

                return result.toString();
            }

            @Override
            protected void done() {
                try {
                    String msg = get();
                    JOptionPane.showMessageDialog(AdminDashboardPanel.this,
                        msg, "Batch hoàn thành", JOptionPane.INFORMATION_MESSAGE);
                    loadStats(); // Refresh số liệu sau batch
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AdminDashboardPanel.this,
                        "Lỗi khi chạy batch: " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnBatch.setEnabled(true);
                    btnBatch.setText("▶  Chạy batch hôm nay");
                }
            }
        };
        worker.execute();
    }

    // ── Helpers DB ───────────────────────────────────────────────────────────

    private int queryCount(String sql) {
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            System.err.println("[AdminDashboardPanel.queryCount] " + e.getMessage());
        }
        return 0;
    }

    private double querySum(String sql) {
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (Exception e) {
            System.err.println("[AdminDashboardPanel.querySum] " + e.getMessage());
        }
        return 0;
    }

    /** Lấy product_id của gói Flex-Safe (term = 0) */
    private int getFlexSafeProductId() {
        String sql = "SELECT product_id FROM SAVINGS_PRODUCT " +
                     "WHERE term = 0 AND status = 'ACTIVE' AND is_deleted = 0 AND ROWNUM = 1";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            System.err.println("[getFlexSafeProductId] " + e.getMessage());
        }
        return -1;
    }

    /** Lấy danh sách investment_id đáo hạn hôm nay */
    private List<Integer> getMaturedInvestmentIds() {
        List<Integer> ids = new java.util.ArrayList<>();
        String sql = "SELECT investment_id FROM INVESTMENT " +
                     "WHERE status = 'ACTIVE' AND is_deleted = 0 " +
                     "AND maturity_date <= TRUNC(SYSDATE)";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) ids.add(rs.getInt(1));
        } catch (Exception e) {
            System.err.println("[getMaturedInvestmentIds] " + e.getMessage());
        }
        return ids;
    }

    private String formatMoney(double amount) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN"))
                .format((long) amount);
    }
}
