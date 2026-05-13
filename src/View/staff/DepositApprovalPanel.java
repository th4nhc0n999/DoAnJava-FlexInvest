package View.staff;

import Controller.WalletController;
import DAO.BankAccountDAO;
import DAO.WalletDAO;
import Model.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.Locale;

/**
 * DepositApprovalPanel — bảng duyệt lệnh nạp tiền PENDING.
 *
 * Cột: Mã lệnh / Khách hàng / Số tiền / Cổng TT / Tài khoản / Nút Duyệt + Từ chối
 * - Nút Duyệt → dialog nhập bank_trans_ref → WalletController.approveDeposit()
 * - Nút Từ chối → dialog nhập lý do → WalletController.rejectDeposit()
 */
public class DepositApprovalPanel extends JPanel {

    // ── Màu ─────────────────────────────────────────────────────────────────
    private static final Color NAVY      = new Color(15, 40, 80);
    private static final Color BLUE      = new Color(0, 162, 232);
    private static final Color GREEN     = new Color(16, 185, 129);
    private static final Color RED       = new Color(239, 68, 68);
    private static final Color BG        = new Color(238, 243, 250);
    private static final Color CARD_BG   = Color.WHITE;
    private static final Color TEXT_DARK = new Color(30, 30, 40);
    private static final Color TEXT_MUTED = new Color(110, 115, 130);
    private static final NumberFormat VND = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private final WalletController ctrl      = new WalletController();
    private final WalletDAO        walletDAO = new WalletDAO();

    private List<Deposit>      deposits;
    private DepositTableModel  tableModel;
    private JTable             table;
    private JLabel             lblStatus;
    private JButton            btnRefresh;

    /** amount cache: transactionId → amount (cần query riêng vì Deposit không mang amount) */
    private final Map<Integer, BigDecimal> amountCache  = new HashMap<>();
    /** walletId → userId cache */
    private final Map<Integer, Integer>    userIdCache  = new HashMap<>();

    public DepositApprovalPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);
        build();
        loadData();
    }

    // =========================================================================
    //  Build UI
    // =========================================================================

    private void build() {
        JPanel inner = new JPanel(new BorderLayout(0, 14));
        inner.setBackground(BG);
        inner.setBorder(new EmptyBorder(24, 28, 24, 28));

        // ── Header ─────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Duyệt Nạp Tiền");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(NAVY);
        JLabel sub = new JLabel("Danh sách lệnh nạp đang chờ phê duyệt");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(TEXT_MUTED);
        titles.add(title);
        titles.add(sub);

        btnRefresh = makeRefreshBtn();
        btnRefresh.addActionListener(e -> loadData());
        header.add(titles, BorderLayout.WEST);
        header.add(btnRefresh, BorderLayout.EAST);
        inner.add(header, BorderLayout.NORTH);

        // ── Table ──────────────────────────────────────────────────────────
        tableModel = new DepositTableModel();
        table = new JTable(tableModel);
        styleTable(table);

        // Action column renderers
        table.getColumnModel().getColumn(6).setCellRenderer(new ApproveButtonRenderer());
        table.getColumnModel().getColumn(6).setCellEditor(new ApproveButtonEditor());
        table.getColumnModel().getColumn(7).setCellRenderer(new RejectButtonRenderer());
        table.getColumnModel().getColumn(7).setCellEditor(new RejectButtonEditor());

        int[] widths = {60, 100, 130, 100, 100, 130, 80, 80};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(new Color(210, 220, 235), 1));
        sp.getViewport().setBackground(CARD_BG);
        inner.add(sp, BorderLayout.CENTER);

        // ── Status ─────────────────────────────────────────────────────────
        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblStatus.setForeground(TEXT_MUTED);
        inner.add(lblStatus, BorderLayout.SOUTH);

        add(inner, BorderLayout.CENTER);
    }

    // =========================================================================
    //  Data Loading
    // =========================================================================

    public void loadData() {
        btnRefresh.setEnabled(false);
        btnRefresh.setText("⟳  Đang tải…");
        new SwingWorker<List<Deposit>, Void>() {
            @Override
            protected List<Deposit> doInBackground() {
                List<Deposit> list = ctrl.getPendingDeposits();
                // Pre-load amount từ TRANSACTION (WalletDAO.getTransactionById)
                amountCache.clear();
                userIdCache.clear();
                for (Deposit d : list) {
                    Transaction tx = walletDAO.getTransactionById(d.getTransactionId());
                    if (tx != null) {
                        amountCache.put(d.getDepositId(), tx.getAmount());
                        Wallet w = walletDAO.getById(tx.getWalletId());
                        if (w != null) userIdCache.put(d.getDepositId(), w.getUserId());
                    }
                }
                return list;
            }
            @Override
            protected void done() {
                try {
                    deposits = get();
                    tableModel.setData(deposits, amountCache, userIdCache);
                    lblStatus.setText(deposits.size() + " lệnh PENDING — "
                        + java.time.LocalTime.now().withNano(0));
                } catch (Exception ex) { ex.printStackTrace();
                } finally {
                    btnRefresh.setEnabled(true);
                    btnRefresh.setText("⟳  Làm mới");
                }
            }
        }.execute();
    }

    // =========================================================================
    //  Actions
    // =========================================================================

    private void onApprove(int row) {
        if (deposits == null || row >= deposits.size()) return;
        Deposit d = deposits.get(row);

        String ref = JOptionPane.showInputDialog(this,
            "Nhập mã tham chiếu ngân hàng (bank_trans_ref):\n(Bỏ trống nếu không có)",
            "Duyệt lệnh nạp #" + d.getDepositId(), JOptionPane.QUESTION_MESSAGE);
        if (ref == null) return;  // cancel

        new SwingWorker<WalletController.Result, Void>() {
            @Override protected WalletController.Result doInBackground() {
                return ctrl.approveDeposit(d.getDepositId(),
                    ref.isBlank() ? null : ref, null);
            }
            @Override protected void done() {
                try {
                    WalletController.Result r = get();
                    if (r == WalletController.Result.SUCCESS) {
                        JOptionPane.showMessageDialog(DepositApprovalPanel.this,
                            "✅ Đã duyệt lệnh nạp #" + d.getDepositId()
                            + "\nTiền đã được cộng vào ví khách hàng.",
                            "Duyệt thành công", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(DepositApprovalPanel.this,
                            "Lỗi: " + r, "Duyệt thất bại", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
                finally { loadData(); }
            }
        }.execute();
    }

    private void onReject(int row) {
        if (deposits == null || row >= deposits.size()) return;
        Deposit d = deposits.get(row);

        JPanel form = new JPanel(new BorderLayout(0, 8));
        form.add(new JLabel("Lý do từ chối lệnh nạp #" + d.getDepositId() + ":"), BorderLayout.NORTH);
        JTextArea txt = new JTextArea(4, 30);
        txt.setLineWrap(true);
        txt.setWrapStyleWord(true);
        form.add(new JScrollPane(txt), BorderLayout.CENTER);

        int opt = JOptionPane.showConfirmDialog(this, form,
            "Từ chối lệnh nạp", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;

        String reason = txt.getText().trim();
        new SwingWorker<WalletController.Result, Void>() {
            @Override protected WalletController.Result doInBackground() {
                return ctrl.rejectDeposit(d.getDepositId(), reason.isBlank() ? "Không hợp lệ" : reason);
            }
            @Override protected void done() {
                try {
                    WalletController.Result r = get();
                    String msg = r == WalletController.Result.SUCCESS
                        ? "✅ Đã từ chối lệnh nạp #" + d.getDepositId()
                        : "Lỗi: " + r;
                    JOptionPane.showMessageDialog(DepositApprovalPanel.this, msg,
                        r == WalletController.Result.SUCCESS ? "Hoàn tất" : "Lỗi",
                        r == WalletController.Result.SUCCESS ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) { ex.printStackTrace(); }
                finally { loadData(); }
            }
        }.execute();
    }

    // =========================================================================
    //  TableModel
    // =========================================================================

    static class DepositTableModel extends AbstractTableModel {
        private static final String[] COLS = {
            "ID", "User ID", "Số tiền", "Cổng TT", "Tài khoản TH", "Mã lệnh", "Duyệt", "Từ chối"
        };
        private List<Deposit>          rows      = new ArrayList<>();
        private Map<Integer, BigDecimal> amtMap  = new HashMap<>();
        private Map<Integer, Integer>    uidMap  = new HashMap<>();

        void setData(List<Deposit> d, Map<Integer, BigDecimal> amt, Map<Integer, Integer> uid) {
            this.rows   = d;
            this.amtMap = amt;
            this.uidMap = uid;
            fireTableDataChanged();
        }

        @Override public int getRowCount()    { return rows.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }
        @Override public boolean isCellEditable(int r, int c) { return c == 6 || c == 7; }

        @Override
        public Object getValueAt(int r, int c) {
            Deposit d = rows.get(r);
            return switch (c) {
                case 0 -> d.getDepositId();
                case 1 -> uidMap.getOrDefault(d.getDepositId(), 0);
                case 2 -> {
                    BigDecimal a = amtMap.get(d.getDepositId());
                    yield a != null ? VND.format(a) + " đ" : "—";
                }
                case 3 -> d.getPaymentGateway() != null ? d.getPaymentGateway() : "—";
                case 4 -> d.getReceivingAccount() != null ? d.getReceivingAccount() : "—";
                case 5 -> d.getRequestCode();
                case 6 -> "Duyệt";
                case 7 -> "Từ chối";
                default -> "";
            };
        }
    }

    // ── Button Renderers & Editors ───────────────────────────────────────────

    private static JButton actionBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 11));
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        return b;
    }

    private class ApproveButtonRenderer extends JButton implements TableCellRenderer {
        ApproveButtonRenderer() { super("Duyệt"); setFont(new Font("Segoe UI", Font.BOLD, 11));
            setForeground(Color.WHITE); setBackground(GREEN); setFocusPainted(false); setBorderPainted(false); setOpaque(true); }
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) { return this; }
    }
    private class RejectButtonRenderer extends JButton implements TableCellRenderer {
        RejectButtonRenderer() { super("Từ chối"); setFont(new Font("Segoe UI", Font.BOLD, 11));
            setForeground(Color.WHITE); setBackground(RED); setFocusPainted(false); setBorderPainted(false); setOpaque(true); }
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) { return this; }
    }

    private class ApproveButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private int cur;
        private final JButton btn = actionBtn("Duyệt", GREEN);
        ApproveButtonEditor() {
            btn.addActionListener(e -> { fireEditingStopped(); onApprove(cur); });
        }
        @Override public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int r, int c) {
            cur = r; return btn;
        }
        @Override public Object getCellEditorValue() { return "Duyệt"; }
    }
    private class RejectButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private int cur;
        private final JButton btn = actionBtn("Từ chối", RED);
        RejectButtonEditor() {
            btn.addActionListener(e -> { fireEditingStopped(); onReject(cur); });
        }
        @Override public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int r, int c) {
            cur = r; return btn;
        }
        @Override public Object getCellEditorValue() { return "Từ chối"; }
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    private JButton makeRefreshBtn() {
        JButton b = new JButton("⟳  Làm mới");
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(BLUE);
        b.setBackground(Color.WHITE);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 225, 245), 1, true),
            new EmptyBorder(6, 16, 6, 16)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void styleTable(JTable t) {
        t.setRowHeight(42); t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.getTableHeader().setBackground(new Color(240, 245, 255));
        t.getTableHeader().setForeground(NAVY);
        t.setGridColor(new Color(230, 235, 245)); t.setShowVerticalLines(false);
        t.setSelectionBackground(new Color(210, 230, 255));
        t.setFillsViewportHeight(true); t.setBackground(CARD_BG);
    }
}
