package View.staff;

import Controller.InvestmentController;
import DAO.AccountDAO;
import DAO.InvestmentDAO;
import DAO.RequestDAO;
import Model.AccountModel;
import Model.Investment;
import Model.SavingsProduct;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import ConnectDB.ConnectionOracle;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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

    // ── Tab 2: Investment Overview ────────────────────────────────────────────
    private JTable            invTable;
    private InvTableModel     invModel;
    private List<Investment>  allInvestments = new ArrayList<>();
    private JComboBox<String> cbInvStatus;
    private JComboBox<String> cbInvProduct;
    private List<SavingsProduct> products = new ArrayList<>();

    public AdminDashboardPanel(AccountModel account) {
        this.account = account;
        setLayout(new BorderLayout());
        setBackground(BG);
        build();
        loadData();
    }

    private void build() {
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.addTab("📊  Tổng quan",         buildDashTab());
        tabs.addTab("💼  Khoản Đầu Tư",      buildInvestmentTab());
        add(tabs, BorderLayout.CENTER);
    }

    // =========================================================================
    //  Tab 1: Dashboard
    // =========================================================================

    private JPanel buildDashTab() {
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(BG);
        inner.setOpaque(true);
        inner.setBorder(new EmptyBorder(24, 28, 24, 28));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("ổng quan Hệ Thống (Admin)");
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

        lblTotalUsers       = new JLabel("0",       SwingConstants.LEFT);
        lblTotalInvested    = new JLabel("0 VNĐ",   SwingConstants.LEFT);
        lblPendingRequests  = new JLabel("0",       SwingConstants.LEFT);
        lblTotalInterest    = new JLabel("0 VNĐ",   SwingConstants.LEFT);

        cards.add(buildCard("👥 Tổng User",             lblTotalUsers,      BLUE));
        cards.add(buildCard("💰 Tổng Đầu Tư (ACTIVE)", lblTotalInvested,   GREEN));
        cards.add(buildCard("⏱ Lệnh Chờ Duyệt",      lblPendingRequests, YELLOW));
        cards.add(buildCard("📈 Tổng Lãi Đã Trả",       lblTotalInterest,   PURPLE));

        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        inner.add(cards);
        inner.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        return scroll;
    }

    // =========================================================================
    //  Tab 2: Investment Overview
    // =========================================================================

    private JPanel buildInvestmentTab() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Filter bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filterBar.setBackground(Color.WHITE);
        filterBar.setBorder(BorderFactory.createLineBorder(new Color(210, 220, 235)));

        filterBar.add(new JLabel("Trạng thái:"));
        cbInvStatus = new JComboBox<>(new String[]{"Tất cả", "ACTIVE", "COMPLETED", "REDEEMED"});
        cbInvStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterBar.add(cbInvStatus);

        filterBar.add(new JLabel("  Gói:"));
        cbInvProduct = new JComboBox<>();
        cbInvProduct.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbInvProduct.setPreferredSize(new Dimension(180, 28));
        filterBar.add(cbInvProduct);

        JButton btnFilter = new JButton("🔍  Lọc");
        btnFilter.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnFilter.setBackground(BLUE);
        btnFilter.setForeground(Color.WHITE);
        btnFilter.setBorderPainted(false);
        btnFilter.setFocusPainted(false);
        btnFilter.addActionListener(e -> applyInvFilter());
        filterBar.add(btnFilter);

        p.add(filterBar, BorderLayout.NORTH);

        // Table
        invModel = new InvTableModel();
        invTable = new JTable(invModel);
        invTable.setRowHeight(32);
        invTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        invTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        invTable.getTableHeader().setBackground(new Color(240, 245, 255));
        invTable.setGridColor(new Color(230, 235, 245));
        invTable.setShowVerticalLines(false);
        invTable.setSelectionBackground(new Color(210, 230, 255));
        invTable.setFillsViewportHeight(true);
        int[] ws = {60, 70, 130, 120, 80, 100, 100, 90};
        for (int i = 0; i < ws.length && i < invTable.getColumnCount(); i++)
            invTable.getColumnModel().getColumn(i).setPreferredWidth(ws[i]);

        JScrollPane sp = new JScrollPane(invTable);
        sp.setBorder(BorderFactory.createLineBorder(new Color(210, 220, 235)));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private void applyInvFilter() {
        String status  = (String) cbInvStatus.getSelectedItem();
        int    prdIdx  = cbInvProduct.getSelectedIndex();
        int    prdId   = (prdIdx > 0 && prdIdx <= products.size())
                         ? products.get(prdIdx - 1).getProductId() : -1;

        List<Investment> filtered = allInvestments.stream().filter(inv -> {
            if (!"Tất cả".equals(status) && !status.equals(inv.getStatus())) return false;
            if (prdId > 0 && inv.getProductId() != prdId) return false;
            return true;
        }).toList();
        invModel.setData(filtered);
    }

    // =========================================================================
    //  InvestmentTableModel
    // =========================================================================

    static class InvTableModel extends AbstractTableModel {
        private static final String[] COLS = {
            "ID", "UserID", "Gói (ID)", "Số tiền (VNĐ)", "Lãi suất", "Ngày bắt đầu", "Đáo hạn", "Trạng thái"
        };
        private List<Investment> rows = new ArrayList<>();
        private static final NumberFormat FMT = NumberFormat.getNumberInstance(new Locale("vi","VN"));
        private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        void setData(List<Investment> d) { rows = d; fireTableDataChanged(); }

        @Override public int getRowCount()    { return rows.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }
        @Override public boolean isCellEditable(int r, int c) { return false; }

        @Override public Object getValueAt(int r, int c) {
            Investment inv = rows.get(r);
            return switch (c) {
                case 0 -> inv.getInvestmentId();
                case 1 -> inv.getUserId();
                case 2 -> inv.getProductId();
                case 3 -> inv.getInvestedAmount() != null ? FMT.format(inv.getInvestedAmount()) : "—";
                case 4 -> inv.getAppliedInterestRate() != null
                          ? inv.getAppliedInterestRate().toPlainString() + "%" : "—";
                case 5 -> inv.getStartDate() != null
                          ? inv.getStartDate().toLocalDate().format(DF) : "—";
                case 6 -> inv.getMaturityDate() != null
                          ? inv.getMaturityDate().toLocalDate().format(DF) : "Không giới hạn";
                case 7 -> statusLabel(inv.getStatus());
                default -> "";
            };
        }

        private String statusLabel(String s) {
            if (s == null) return "—";
            return switch (s) {
                case "ACTIVE"    -> "✅ Đang chạy";
                case "COMPLETED" -> "🏁 Đã tất toán";
                case "REDEEMED" -> "🔄 Đã rút trước hạn";
                default -> s;
            };
        }
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
            List<Investment> investments = new ArrayList<>();
            List<SavingsProduct> prods = new ArrayList<>();

            @Override
            protected Void doInBackground() {
                try (Connection con = ConnectionOracle.getOracleConnection()) {
                    try (var ps = con.prepareStatement("SELECT COUNT(*) FROM USERS WHERE is_deleted=0");
                         var rs = ps.executeQuery()) { if (rs.next()) users = rs.getInt(1); }
                    try (var ps = con.prepareStatement("SELECT NVL(SUM(invested_amount), 0) FROM INVESTMENT WHERE status='ACTIVE' AND is_deleted=0");
                         var rs = ps.executeQuery()) { if (rs.next()) invested = rs.getBigDecimal(1); }
                    try (var ps = con.prepareStatement(
                             "SELECT " +
                             "  (SELECT COUNT(*) FROM DEPOSIT d JOIN TRANSACTION t ON d.transaction_id=t.transaction_id WHERE t.status='PENDING' AND d.is_deleted=0)" +
                             "+ (SELECT COUNT(*) FROM WITHDRAW WHERE status='PENDING' AND is_deleted=0)" +
                             " FROM DUAL");
                         var rs = ps.executeQuery()) { if (rs.next()) pending = rs.getInt(1); }
                    try (var ps = con.prepareStatement("SELECT NVL(SUM(payout_amount), 0) FROM PAYOUT WHERE payout_type='INTEREST' AND is_deleted=0");
                         var rs = ps.executeQuery()) { if (rs.next()) interest = rs.getBigDecimal(1); }
                } catch (Exception e) { e.printStackTrace(); }
                investments = invDAO.getAllInvestments();
                prods       = invDAO.getAllActiveProducts();
                return null;
            }

            @Override
            protected void done() {
                lblTotalUsers.setText(String.format("%,d", users));
                lblTotalInvested.setText(VND.format(invested) + " VNĐ");
                lblPendingRequests.setText(String.format("%,d", pending));
                lblTotalInterest.setText(VND.format(interest) + " VNĐ");

                // Investment tab
                allInvestments = investments;
                products = prods;
                cbInvProduct.removeAllItems();
                cbInvProduct.addItem("Tất cả gói");
                for (SavingsProduct p : products)
                    cbInvProduct.addItem(p.getProductName());
                invModel.setData(allInvestments);
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
