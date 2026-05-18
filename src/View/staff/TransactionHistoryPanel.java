package View.staff;

import DAO.WalletDAO;
import Model.Transaction;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * TransactionHistoryPanel — Admin panel toàn bộ giao dịch hệ thống.
 *
 * Tính năng:
 *  - Bảng tất cả TRANSACTION (wallet_id, type, amount, status, thời gian)
 *  - Filter theo ngày + loại giao dịch
 *  - Export ra CSV bằng FileWriter
 */
public class TransactionHistoryPanel extends JPanel {

    private static final Color NAVY    = new Color(15, 40, 80);
    private static final Color BLUE    = new Color(0, 162, 232);
    private static final Color BG      = new Color(238, 243, 250);
    private static final Color CARD    = Color.WHITE;
    private static final Color MUTED   = new Color(110, 115, 130);
    private static final Color BORDER_C = new Color(210, 220, 235);
    private static final NumberFormat VND = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final WalletDAO walletDAO = new WalletDAO();

    private JTable            table;
    private TxTableModel      tableModel;
    private List<Transaction> allTx = new ArrayList<>();

    // Filter controls
    private JComboBox<String> cbType;
    private JTextField        txtFrom;
    private JTextField        txtTo;
    private JLabel            lblCount;

    public TransactionHistoryPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(BG);
        build();
        loadData();
    }

    // =========================================================================
    //  Build
    // =========================================================================

    private void build() {
        JPanel inner = new JPanel(new BorderLayout(0, 12));
        inner.setBackground(BG);
        inner.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("📊  Lịch sử giao dịch hệ thống");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(NAVY);
        JButton btnRefresh = actionBtn("⟳  Làm mới", BLUE);
        btnRefresh.addActionListener(e -> loadData());
        header.add(title,      BorderLayout.WEST);
        header.add(btnRefresh, BorderLayout.EAST);
        inner.add(header, BorderLayout.NORTH);

        // Filter bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filterBar.setBackground(CARD);
        filterBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_C, 1),
            new EmptyBorder(0, 8, 0, 8)));

        filterBar.add(new JLabel("Loại:"));
        cbType = new JComboBox<>(new String[]{
            "Tất cả", "DEPOSIT", "WITHDRAW", "INVEST", "PAYOUT", "BONUS"
        });
        cbType.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterBar.add(cbType);

        filterBar.add(new JLabel("  Từ:"));
        txtFrom = new JTextField("01/01/" + LocalDate.now().getYear(), 10);
        txtFrom.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterBar.add(txtFrom);

        filterBar.add(new JLabel("Đến:"));
        txtTo = new JTextField(LocalDate.now().format(DATE_FMT), 10);
        txtTo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterBar.add(txtTo);

        JButton btnFilter = actionBtn("🔍  Lọc", BLUE);
        btnFilter.addActionListener(e -> applyFilter());
        filterBar.add(btnFilter);

        JButton btnExport = actionBtn("📤  Export CSV", new Color(16, 185, 129));
        btnExport.addActionListener(e -> exportCsv());
        filterBar.add(btnExport);

        lblCount = new JLabel("0 giao dịch");
        lblCount.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblCount.setForeground(MUTED);
        filterBar.add(lblCount);

        inner.add(filterBar, BorderLayout.CENTER);

        // Table
        tableModel = new TxTableModel();
        table = new JTable(tableModel);
        styleTable(table);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_C));
        sp.getViewport().setBackground(CARD);

        JPanel tableWrap = new JPanel(new BorderLayout());
        tableWrap.setBackground(BG);
        tableWrap.add(sp, BorderLayout.CENTER);
        inner.add(tableWrap, BorderLayout.SOUTH);

        // Re-layout: header North, filter+table in CENTER column
        setLayout(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setBackground(BG);
        top.setBorder(new EmptyBorder(20, 24, 0, 24));
        top.add(header,    BorderLayout.NORTH);
        top.add(filterBar, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(BG);
        tablePanel.setBorder(new EmptyBorder(0, 24, 20, 24));
        tablePanel.add(sp, BorderLayout.CENTER);
        add(tablePanel, BorderLayout.CENTER);
    }

    // =========================================================================
    //  Data
    // =========================================================================

    public void loadData() {
        new SwingWorker<List<Transaction>, Void>() {
            @Override protected List<Transaction> doInBackground() {
                return loadAllTransactions();
            }
            @Override protected void done() {
                try {
                    allTx = get();
                    applyFilter();
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }.execute();
    }

    /** Query toàn bộ TRANSACTION từ DB (không lọc theo user). */
    private List<Transaction> loadAllTransactions() {
        List<Transaction> list = new ArrayList<>();
        String sql = """
            SELECT t.transaction_id, t.wallet_id, t.type_code, t.amount,
                   t.status, t.created_at, t.is_deleted,
                   w.user_id
            FROM TRANSACTION t
            JOIN WALLET w ON t.wallet_id = w.wallet_id
            WHERE t.is_deleted = 0
            ORDER BY t.created_at DESC
            """;
        try (var con = ConnectDB.ConnectionOracle.getOracleConnection();
             var ps = con.prepareStatement(sql);
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                Transaction t = new Transaction();
                t.setTransactionId(rs.getInt("transaction_id"));
                t.setWalletId(rs.getInt("wallet_id"));
                t.setTypeCode(rs.getString("type_code"));
                t.setAmount(rs.getBigDecimal("amount"));
                t.setStatus(rs.getString("status"));
                java.sql.Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) t.setCreatedAt(ts.toLocalDateTime());
                t.setIsDeleted(rs.getInt("is_deleted"));
                list.add(t);
            }
        } catch (Exception e) {
            System.err.println("[TransactionHistoryPanel] " + e.getMessage());
        }
        return list;
    }

    private void applyFilter() {
        String type = (String) cbType.getSelectedItem();
        LocalDate from = parseDate(txtFrom.getText(), LocalDate.of(2020, 1, 1));
        LocalDate to   = parseDate(txtTo.getText(), LocalDate.now());

        List<Transaction> filtered = allTx.stream().filter(t -> {
            if (!"Tất cả".equals(type) && !type.equals(t.getTypeCode())) return false;
            if (t.getCreatedAt() != null) {
                LocalDate d = t.getCreatedAt().toLocalDate();
                if (d.isBefore(from) || d.isAfter(to)) return false;
            }
            return true;
        }).toList();

        tableModel.setData(filtered);
        lblCount.setText(filtered.size() + " giao dịch");
    }

    // =========================================================================
    //  Export CSV
    // =========================================================================

    private void exportCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("transactions_" + LocalDate.now() + ".csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try (FileWriter fw = new FileWriter(fc.getSelectedFile())) {
            // Header
            fw.write("ID,WalletID,Loai,SoTien,TrangThai,ThoiGian\n");
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                fw.write(
                    tableModel.getValueAt(r, 0) + "," +
                    tableModel.getValueAt(r, 1) + "," +
                    tableModel.getValueAt(r, 2) + "," +
                    tableModel.getValueAt(r, 3) + "," +
                    tableModel.getValueAt(r, 4) + "," +
                    tableModel.getValueAt(r, 5) + "\n"
                );
            }
            JOptionPane.showMessageDialog(this, "✅ Export CSV thành công!\n" + fc.getSelectedFile().getPath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "❌ Lỗi khi xuất file: " + ex.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    //  TableModel
    // =========================================================================

    static class TxTableModel extends AbstractTableModel {
        private static final String[] COLS = {
            "ID", "WalletID", "Loại", "Số tiền (VNĐ)", "Trạng thái", "Thời gian"
        };
        private List<Transaction> rows = new ArrayList<>();
        private static final NumberFormat FMT = NumberFormat.getNumberInstance(new Locale("vi","VN"));
        private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        void setData(List<Transaction> data) { rows = data; fireTableDataChanged(); }

        @Override public int getRowCount()    { return rows.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }
        @Override public boolean isCellEditable(int r, int c) { return false; }

        @Override public Object getValueAt(int r, int c) {
            Transaction t = rows.get(r);
            return switch (c) {
                case 0 -> t.getTransactionId();
                case 1 -> t.getWalletId();
                case 2 -> typeLabel(t.getTypeCode());
                case 3 -> t.getAmount() != null ? FMT.format(t.getAmount()) : "—";
                case 4 -> t.getStatus() != null ? t.getStatus() : "—";
                case 5 -> t.getCreatedAt() != null ? t.getCreatedAt().format(DT) : "—";
                default -> "";
            };
        }

        private String typeLabel(String code) {
            if (code == null) return "—";
            return switch (code) {
                case "DEPOSIT"  -> "💳 Nạp";
                case "WITHDRAW" -> "🏦 Rút";
                case "INVEST"   -> "📈 Đầu tư";
                case "PAYOUT"   -> "💰 Lãi";
                case "BONUS"    -> "🎁 Thưởng";
                default -> code;
            };
        }
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    private LocalDate parseDate(String s, LocalDate fallback) {
        try { return LocalDate.parse(s.trim(), DATE_FMT); }
        catch (Exception e) { return fallback; }
    }

    private void styleTable(JTable t) {
        t.setRowHeight(32);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.getTableHeader().setBackground(new Color(240, 245, 255));
        t.getTableHeader().setForeground(NAVY);
        t.setGridColor(new Color(230, 235, 245));
        t.setShowVerticalLines(false);
        t.setSelectionBackground(new Color(210, 230, 255));
        t.setFillsViewportHeight(true);
        t.setBackground(CARD);
        int[] widths = {55, 70, 100, 120, 90, 160};
        for (int i = 0; i < widths.length && i < t.getColumnCount(); i++)
            t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
    }

    private JButton actionBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
