package View.customer;

import Controller.WalletController;
import Model.AccountModel;
import Model.Transaction;
import Model.Wallet;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * WalletPanel — Tổng quan ví + lịch sử giao dịch.
 *
 * Cấu trúc:
 *  - Top: số dư to rõ
 *  - Middle: thanh filter (loại giao dịch + từ ngày / đến ngày)
 *  - Bottom: bảng lịch sử với phân trang 20 dòng/trang
 */
public class WalletPanel extends JPanel {

    private static final Color NAVY    = new Color(15, 40, 80);
    private static final Color BLUE    = new Color(0, 162, 232);
    private static final Color GREEN   = new Color(16, 185, 129);
    private static final Color BG      = new Color(238, 243, 250);
    private static final Color CARD    = Color.WHITE;
    private static final Color MUTED   = new Color(110, 115, 130);
    private static final Color BORDER_C = new Color(210, 220, 235);
    private static final int PAGE_SIZE = 20;
    private static final NumberFormat VND = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AccountModel     account;
    private final int              userId;
    private final WalletController walletCtrl = new WalletController();

    // ── Header ────────────────────────────────────────────────────────────────
    private JLabel lblBalance;
    private JLabel lblLocked;
    private JButton btnRefresh;

    // ── Filter ────────────────────────────────────────────────────────────────
    private JComboBox<String> cbType;
    private JTextField        txtFrom;
    private JTextField        txtTo;

    // ── Table ─────────────────────────────────────────────────────────────────
    private JTable                table;
    private TxTableModel          tableModel;
    private List<Transaction>     allTx     = new ArrayList<>();
    private List<Transaction>     filteredTx = new ArrayList<>();

    // ── Pagination ───────────────────────────────────────────────────────────
    private int    currentPage = 0;
    private JLabel lblPage;
    private JButton btnPrev, btnNext;

    public WalletPanel(AccountModel account) {
        this.account = account;
        this.userId  = account.getUser().getUserId();
        setLayout(new BorderLayout(0, 0));
        setBackground(BG);
        build();
        loadData();
    }

    // =========================================================================
    //  Build
    // =========================================================================

    private void build() {
        JPanel inner = new JPanel(new BorderLayout(0, 16));
        inner.setBackground(BG);
        inner.setBorder(new EmptyBorder(24, 28, 24, 28));

        inner.add(buildHeader(),    BorderLayout.NORTH);
        inner.add(buildContent(),   BorderLayout.CENTER);

        add(inner, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        // Card số dư
        JPanel card = new JPanel(new BorderLayout(16, 0));
        card.setBackground(NAVY);
        card.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel lbl1 = new JLabel("Số dư khả dụng");
        lbl1.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl1.setForeground(new Color(180, 200, 235));

        lblBalance = new JLabel("Đang tải...");
        lblBalance.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblBalance.setForeground(Color.WHITE);

        lblLocked = new JLabel("Đang giữ: —");
        lblLocked.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblLocked.setForeground(new Color(160, 185, 220));

        left.add(lbl1);
        left.add(Box.createVerticalStrut(4));
        left.add(lblBalance);
        left.add(Box.createVerticalStrut(4));
        left.add(lblLocked);

        btnRefresh = new JButton("⟳  Làm mới");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.setBackground(new Color(40, 80, 140));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setBorderPainted(false);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRefresh.addActionListener(e -> loadData());

        card.add(left,       BorderLayout.CENTER);
        card.add(btnRefresh, BorderLayout.EAST);
        return card;
    }

    private JPanel buildContent() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(BG);

        // Filter bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        filterBar.setBackground(CARD);
        filterBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_C, 1),
            new EmptyBorder(0, 8, 0, 8)));

        filterBar.add(new JLabel("Loại:"));
        cbType = new JComboBox<>(new String[]{
            "Tất cả", "DEPOSIT", "WITHDRAW", "INVEST", "PAYOUT", "BONUS"
        });
        cbType.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbType.setPreferredSize(new Dimension(140, 30));
        filterBar.add(cbType);

        filterBar.add(new JLabel("  Từ ngày:"));
        txtFrom = new JTextField(10);
        txtFrom.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtFrom.setToolTipText("dd/MM/yyyy");
        txtFrom.setText("01/01/" + LocalDate.now().getYear());
        filterBar.add(txtFrom);

        filterBar.add(new JLabel("Đến:"));
        txtTo = new JTextField(10);
        txtTo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtTo.setToolTipText("dd/MM/yyyy");
        txtTo.setText(LocalDate.now().format(DATE_FMT));
        filterBar.add(txtTo);

        JButton btnFilter = new JButton("🔍  Lọc");
        btnFilter.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnFilter.setBackground(BLUE);
        btnFilter.setForeground(Color.WHITE);
        btnFilter.setBorderPainted(false);
        btnFilter.setFocusPainted(false);
        btnFilter.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnFilter.addActionListener(e -> applyFilter());
        filterBar.add(btnFilter);

        JButton btnClear = new JButton("✕  Xóa lọc");
        btnClear.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClear.setForeground(MUTED);
        btnClear.setBorderPainted(false);
        btnClear.setFocusPainted(false);
        btnClear.setBackground(CARD);
        btnClear.addActionListener(e -> {
            cbType.setSelectedIndex(0);
            txtFrom.setText("01/01/" + LocalDate.now().getYear());
            txtTo.setText(LocalDate.now().format(DATE_FMT));
            applyFilter();
        });
        filterBar.add(btnClear);

        p.add(filterBar, BorderLayout.NORTH);

        // Table
        tableModel = new TxTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(36);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(240, 245, 255));
        table.getTableHeader().setForeground(NAVY);
        table.setGridColor(new Color(230, 235, 245));
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(12, 0));
        table.setSelectionBackground(new Color(210, 230, 255));
        table.setFillsViewportHeight(true);
        table.setBackground(CARD);

        // Column widths
        int[] widths = {115, 110, 140, 140, 200};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Color renderer for amount
        table.getColumnModel().getColumn(2).setCellRenderer(new AmountRenderer());

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_C, 1));
        sp.getViewport().setBackground(CARD);
        p.add(sp, BorderLayout.CENTER);

        // Pagination
        JPanel pagination = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        pagination.setBackground(CARD);
        pagination.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_C));

        btnPrev = new JButton("◀  Trước");
        btnPrev.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnPrev.setFocusPainted(false);
        btnPrev.addActionListener(e -> { if (currentPage > 0) { currentPage--; showPage(); } });

        lblPage = new JLabel("Trang 1 / 1");
        lblPage.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblPage.setForeground(MUTED);

        btnNext = new JButton("Tiếp  ▶");
        btnNext.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnNext.setFocusPainted(false);
        btnNext.addActionListener(e -> {
            int total = (int) Math.ceil((double) filteredTx.size() / PAGE_SIZE);
            if (currentPage < total - 1) { currentPage++; showPage(); }
        });

        pagination.add(btnPrev);
        pagination.add(lblPage);
        pagination.add(btnNext);
        p.add(pagination, BorderLayout.SOUTH);

        return p;
    }

    // =========================================================================
    //  Data
    // =========================================================================

    public void loadData() {
        btnRefresh.setEnabled(false);
        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() {
                Wallet w = walletCtrl.getBalance(userId);
                List<Transaction> txList = walletCtrl.getTransactionHistory(userId);
                return new Object[]{w, txList};
            }
            @Override protected void done() {
                try {
                    Object[] data = get();
                    Wallet w = (Wallet) data[0];
                    if (w != null) {
                        lblBalance.setText(VND.format(w.getAvailableBalance()) + " VNĐ");
                        lblLocked.setText("Đang giữ: " + VND.format(w.getLockedBalance()) + " VNĐ");
                    }
                    allTx = (List<Transaction>) data[1];
                    applyFilter();
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    btnRefresh.setEnabled(true);
                }
            }
        }.execute();
    }

    private void applyFilter() {
        String typeFilter = (String) cbType.getSelectedItem();
        LocalDate from = parseDate(txtFrom.getText(), LocalDate.of(2020, 1, 1));
        LocalDate to   = parseDate(txtTo.getText(), LocalDate.now());

        filteredTx = allTx.stream().filter(tx -> {
            // Type filter
            if (!"Tất cả".equals(typeFilter) && !typeFilter.equals(tx.getTypeCode()))
                return false;
            // Date filter
            if (tx.getCreatedAt() != null) {
                LocalDate txDate = tx.getCreatedAt().toLocalDate();
                if (txDate.isBefore(from) || txDate.isAfter(to)) return false;
            }
            return true;
        }).toList();

        currentPage = 0;
        showPage();
    }

    private void showPage() {
        int total = filteredTx.isEmpty() ? 1 : (int) Math.ceil((double) filteredTx.size() / PAGE_SIZE);
        int start = currentPage * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, filteredTx.size());
        List<Transaction> pageData = filteredTx.subList(start, end);

        tableModel.setData(pageData);
        lblPage.setText("Trang " + (currentPage + 1) + " / " + total
            + "  (" + filteredTx.size() + " giao dịch)");
        btnPrev.setEnabled(currentPage > 0);
        btnNext.setEnabled(currentPage < total - 1);
    }

    private LocalDate parseDate(String s, LocalDate fallback) {
        try { return LocalDate.parse(s.trim(), DATE_FMT); }
        catch (Exception e) { return fallback; }
    }

    // =========================================================================
    //  TableModel
    // =========================================================================

    static class TxTableModel extends AbstractTableModel {
        private static final String[] COLS = {
            "Ngày giờ", "Loại", "Số tiền (VNĐ)", "Trạng thái", "Ghi chú"
        };
        private List<Transaction> rows = new ArrayList<>();

        void setData(List<Transaction> data) {
            rows = data;
            fireTableDataChanged();
        }

        @Override public int getRowCount()    { return rows.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }
        @Override public boolean isCellEditable(int r, int c) { return false; }

        @Override public Object getValueAt(int r, int c) {
            Transaction t = rows.get(r);
            return switch (c) {
                case 0 -> t.getCreatedAt() != null
                    ? t.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—";
                case 1 -> typeLabel(t.getTypeCode());
                case 2 -> t.getAmount();   // AmountRenderer xử lý format
                case 3 -> t.getStatus() != null ? t.getStatus() : "—";
                case 4 -> "";  // note chưa có trong model — để trống
                default -> "";
            };
        }

        private String typeLabel(String code) {
            if (code == null) return "—";
            return switch (code) {
                case "DEPOSIT"  -> "💳 Nạp tiền";
                case "WITHDRAW" -> "🏦 Rút tiền";
                case "INVEST"   -> "📈 Đầu tư";
                case "PAYOUT"   -> "💰 Nhận lãi";
                case "BONUS"    -> "🎁 Thưởng";
                default -> code;
            };
        }
    }

    // ── Amount renderer: credit = xanh, debit = đỏ ────────────────────────
    private static class AmountRenderer extends DefaultTableCellRenderer {
        private static final NumberFormat FMT = NumberFormat.getNumberInstance(new Locale("vi","VN"));
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, val, sel, foc, row, col);
            setHorizontalAlignment(RIGHT);
            if (val instanceof BigDecimal bd) {
                String typeCode = (String) t.getModel().getValueAt(row, 1);
                boolean isCredit = typeCode.contains("Nạp") || typeCode.contains("lãi") || typeCode.contains("Thưởng");
                setText((isCredit ? "+ " : "- ") + FMT.format(bd));
                setForeground(isCredit ? new Color(16, 185, 129) : new Color(239, 68, 68));
            }
            return this;
        }
    }
}
