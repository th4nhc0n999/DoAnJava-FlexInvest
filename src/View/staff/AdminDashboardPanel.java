package View.staff;

import Controller.InvestmentController;
import DAO.AccountDAO;
import DAO.InvestmentDAO;
import DAO.RequestDAO;
import Model.AccountModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import ConnectDB.ConnectionOracle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class AdminDashboardPanel extends JPanel {

    private static final Color BG = new Color(238, 243, 250);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BLUE = new Color(0, 162, 232);
    private static final Color GREEN = new Color(16, 185, 129);
    private static final Color YELLOW = new Color(245, 158, 11);
    private static final Color PURPLE = new Color(139, 92, 246);
    private static final NumberFormat VND = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private final AccountModel account;
    private final InvestmentController invController = new InvestmentController();
    private final InvestmentDAO invDAO = new InvestmentDAO();
    private final RequestDAO reqDAO = new RequestDAO();
    private final AccountDAO accDAO = new AccountDAO();

    private JLabel lblTotalUsers;
    private JLabel lblTotalInvested;
    private JLabel lblPendingRequests;
    private JLabel lblTotalInterest;
    private JButton btnRunBatch;

    public AdminDashboardPanel(AccountModel account) {
        this.account = account;
        setLayout(new BorderLayout());
        setBackground(BG);
        build();
        loadData();
    }

    private void build() {
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(BG);
        inner.setOpaque(true);
        inner.setBorder(new EmptyBorder(24, 28, 24, 28));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Tổng quan Hệ Thống (Admin)");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        header.add(title, BorderLayout.WEST);

        btnRunBatch = new JButton("▶ Chạy batch hôm nay");
        btnRunBatch.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRunBatch.setBackground(new Color(220, 38, 38));
        btnRunBatch.setForeground(Color.WHITE);
        btnRunBatch.setFocusPainted(false);
        btnRunBatch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRunBatch.addActionListener(e -> runBatch());
        header.add(btnRunBatch, BorderLayout.EAST);

        inner.add(header);
        inner.add(Box.createVerticalStrut(24));

        // Summary Cards
        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));
        cards.setOpaque(false);

        lblTotalUsers = new JLabel("0", SwingConstants.LEFT);
        lblTotalInvested = new JLabel("0 VNĐ", SwingConstants.LEFT);
        lblPendingRequests = new JLabel("0", SwingConstants.LEFT);
        lblTotalInterest = new JLabel("0 VNĐ", SwingConstants.LEFT);

        cards.add(buildCard("👥 Tổng User", lblTotalUsers, BLUE));
        cards.add(buildCard("💰 Tổng Đầu Tư (ACTIVE)", lblTotalInvested, GREEN));
        cards.add(buildCard("⏱ Lệnh Chờ Duyệt", lblPendingRequests, YELLOW));
        cards.add(buildCard("📈 Tổng Lãi Đã Trả", lblTotalInterest, PURPLE));

        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        inner.add(cards);
        
        inner.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.setOpaque(true);
        scroll.getViewport().setOpaque(true);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildCard(String title, JLabel valueLabel, Color accent) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 235)),
                new EmptyBorder(16, 20, 16, 20)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(accent);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(new Color(30, 30, 40));

        p.add(lblTitle, BorderLayout.NORTH);
        p.add(valueLabel, BorderLayout.CENTER);
        return p;
    }

    public void loadData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            int users = 0;
            BigDecimal invested = BigDecimal.ZERO;
            int pending = 0;
            BigDecimal interest = BigDecimal.ZERO;

            @Override
            protected Void doInBackground() {
                try (Connection con = ConnectionOracle.getOracleConnection()) {
                    // Total users
                    try (var ps = con.prepareStatement("SELECT COUNT(*) FROM USERS WHERE is_deleted=0");
                         var rs = ps.executeQuery()) {
                        if (rs.next()) users = rs.getInt(1);
                    }
                    // Total invested active
                    try (var ps = con.prepareStatement("SELECT NVL(SUM(invested_amount), 0) FROM INVESTMENT WHERE status='ACTIVE' AND is_deleted=0");
                         var rs = ps.executeQuery()) {
                        if (rs.next()) invested = rs.getBigDecimal(1);
                    }
                    // Pending requests
                    try (var ps = con.prepareStatement("SELECT COUNT(*) FROM APPROVAL_REQUEST WHERE status='PENDING' AND is_deleted=0");
                         var rs = ps.executeQuery()) {
                        if (rs.next()) pending = rs.getInt(1);
                    }
                    // Total interest paid
                    try (var ps = con.prepareStatement("SELECT NVL(SUM(payout_amount), 0) FROM PAYOUT WHERE payout_type='INTEREST' AND is_deleted=0");
                         var rs = ps.executeQuery()) {
                        if (rs.next()) interest = rs.getBigDecimal(1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                lblTotalUsers.setText(String.format("%,d", users));
                lblTotalInvested.setText(VND.format(invested) + " VNĐ");
                lblPendingRequests.setText(String.format("%,d", pending));
                lblTotalInterest.setText(VND.format(interest) + " VNĐ");
            }
        };
        worker.execute();
    }

    private void runBatch() {
        int confirm = JOptionPane.showConfirmDialog(this, 
                "Chạy batch tính lãi tự động và tất toán cho ngày hôm nay?", 
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        btnRunBatch.setEnabled(false);
        btnRunBatch.setText("Đang xử lý...");

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return invController.runDailyBatch();
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    JOptionPane.showMessageDialog(AdminDashboardPanel.this, 
                        result.isEmpty() ? "Hoàn thành batch." : result, 
                        "Kết quả chạy Batch", JOptionPane.INFORMATION_MESSAGE);
                    loadData();
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(AdminDashboardPanel.this, "Lỗi chạy batch: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnRunBatch.setEnabled(true);
                    btnRunBatch.setText("▶ Chạy batch hôm nay");
                }
            }
        };
        worker.execute();
    }
}
