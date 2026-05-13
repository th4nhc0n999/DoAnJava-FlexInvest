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
 * WithdrawApprovalPanel — bảng duyệt lệnh rút tiền PENDING.
 *
 * Cột: ID / User ID / Số tiền / Phí / Ngân hàng / Số TK / Ngày tạo / Duyệt + Từ chối
 * - Hiển thị thêm thông tin tài khoản ngân hàng đích (từ BankAccountDAO)
 * - Nút Duyệt → dialog nhập ghi chú → WalletController.approveWithdraw()
 * - Nút Từ chối → dialog nhập lý do → WalletController.rejectWithdraw()
 */
public class WithdrawApprovalPanel extends JPanel {

    private static final Color NAVY      = new Color(15, 40, 80);
    private static final Color BLUE      = new Color(0, 162, 232);
    private static final Color GREEN     = new Color(16, 185, 129);
    private static final Color RED       = new Color(239, 68, 68);
    private static final Color ORANGE    = new Color(245, 158, 11);
    private static final Color BG        = new Color(238, 243, 250);
    private static final Color CARD_BG   = Color.WHITE;
    private static final Color TEXT_DARK = new Color(30, 30, 40);
    private static final Color TEXT_MUTED = new Color(110, 115, 130);
    private static final NumberFormat VND = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private final WalletController ctrl       = new WalletController();
    private final WalletDAO        walletDAO  = new WalletDAO();
    private final BankAccountDAO   bankDAO    = new BankAccountDAO();

    private List<Withdraw>     withdraws;
    private WithdrawTableModel tableModel;
    private JTable             table;
    private JLabel             lblStatus;
    private JButton            btnRefresh;

    /** Cache: withdrawId → amount (từ TRANSACTION) */
    private final Map<Integer, BigDecimal>  amountCache  = new HashMap<>();
    /** Cache: bankAccountId → BankAccount */
    private final Map<Integer, BankAccount> bankCache    = new HashMap<>();

    public WithdrawApprovalPanel() {
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
        JLabel title = new JLabel("Duyệt Rút Tiền");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(NAVY);
        JLabel sub = new JLabel("Danh sách lệnh rút đang chờ phê duyệt — số dư đã bị khóa");
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
        tableModel = new WithdrawTableModel();
        table = new JTable(tableModel);
        styleTable(table);

        table.getColumnModel().getColumn(7).setCellRenderer(new GreenBtnRenderer());
        table.getColumnModel().getColumn(7).setCellEditor(new ApproveEditor());
        table.getColumnModel().getColumn(8).setCellRenderer(new RedBtnRenderer());
        table.getColumnModel().getColumn(8).setCellEditor(new RejectEditor());

        int[] widths = {55, 70, 120, 80, 100, 130, 120, 75, 80};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(new Color(210, 220, 235), 1));
        sp.getViewport().setBackground(CARD_BG);
        inner.add(sp, BorderLayout.CENTER);

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
        new SwingWorker<List<Withdraw>, Void>() {
            @Override
            protected List<Withdraw> doInBackground() {
                List<Withdraw> list = ctrl.getPendingWithdraws();
                amountCache.clear();
                bankCache.clear();
                for (Withdraw w : list) {
                    // Load số tiền từ TRANSACTION
                    Transaction tx = walletDAO.getTransactionById(w.getTransactionId());
                    if (tx != null) amountCache.put(w.getWithdrawId(), tx.getAmount());
                    // Load thông tin TK ngân hàng
                    BankAccount ba = bankDAO.getById(w.getBankAccountId());
                    if (ba != null) bankCache.put(w.getWithdrawId(), ba);
                }
                return list;
            }
            @Override
            protected void done() {
                try {
                    withdraws = get();
                    tableModel.setData(withdraws, amountCache, bankCache);
                    lblStatus.setText(withdraws.size() + " lệnh PENDING — "
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
        if (withdraws == null || row >= withdraws.size()) return;
        Withdraw w = withdraws.get(row);
        BigDecimal amt = amountCache.getOrDefault(w.getWithdrawId(), BigDecimal.ZERO);
        BankAccount ba = bankCache.get(w.getWithdrawId());

        String bankInfo = ba != null
            ? ba.getBankName() + " — " + ba.getAccountNumber()
            : "Không rõ TK";

        int confirm = JOptionPane.showConfirmDialog(this,
            "<html><b>Xác nhận duyệt lệnh rút #" + w.getWithdrawId() + "?</b><br>"
            + "Số tiền: <b>" + VND.format(amt) + " đ</b><br>"
            + "Chuyển đến: <b>" + bankInfo + "</b></html>",
            "Duyệt rút tiền", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        String note = JOptionPane.showInputDialog(this, "Ghi chú (tùy chọn):", "");
        if (note == null) return;

        new SwingWorker<WalletController.Result, Void>() {
            @Override protected WalletController.Result doInBackground() {
                return ctrl.approveWithdraw(w.getWithdrawId(), note.isBlank() ? null : note);
            }
            @Override protected void done() {
                try {
                    WalletController.Result r = get();
                    if (r == WalletController.Result.SUCCESS) {
                        JOptionPane.showMessageDialog(WithdrawApprovalPanel.this,
                            "✅ Đã duyệt lệnh rút #" + w.getWithdrawId(),
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(WithdrawApprovalPanel.this,
                            "Lỗi: " + r, "Thất bại", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
                finally { loadData(); }
            }
        }.execute();
    }

    private void onReject(int row) {
        if (withdraws == null || row >= withdraws.size()) return;
        Withdraw w = withdraws.get(row);

        JPanel form = new JPanel(new BorderLayout(0, 8));
        form.add(new JLabel("Lý do từ chối lệnh rút #" + w.getWithdrawId() + ":"), BorderLayout.NORTH);
        JTextArea txt = new JTextArea(4, 30);
        txt.setLineWrap(true); txt.setWrapStyleWord(true);
        form.add(new JScrollPane(txt), BorderLayout.CENTER);

        int opt = JOptionPane.showConfirmDialog(this, form,
            "Từ chối lệnh rút", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;
        String reason = txt.getText().trim();

        new SwingWorker<WalletController.Result, Void>() {
            @Override protected WalletController.Result doInBackground() {
                return ctrl.rejectWithdraw(w.getWithdrawId(), reason.isBlank() ? "Không hợp lệ" : reason);
            }
            @Override protected void done() {
                try {
                    WalletController.Result r = get();
                    String msg = r == WalletController.Result.SUCCESS
                        ? "✅ Đã từ chối — tiền đã được hoàn về ví khách hàng."
                        : "Lỗi: " + r;
                    JOptionPane.showMessageDialog(WithdrawApprovalPanel.this, msg,
                        r == WalletController.Result.SUCCESS ? "Hoàn tất" : "Lỗi",
                        r == WalletController.Result.SUCCESS
                            ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) { ex.printStackTrace(); }
                finally { loadData(); }
            }
        }.execute();
    }

    // =========================================================================
    //  TableModel
    // =========================================================================

    static class WithdrawTableModel extends AbstractTableModel {
        private static final String[] COLS = {
            "ID", "User ID", "Số tiền", "Phí", "Ngân hàng", "Số TK", "Ngày tạo", "Duyệt", "Từ chối"
        };
        private List<Withdraw>            rows     = new ArrayList<>();
        private Map<Integer, BigDecimal>  amtMap   = new HashMap<>();
        private Map<Integer, BankAccount> bankMap  = new HashMap<>();

        void setData(List<Withdraw> d, Map<Integer, BigDecimal> amt, Map<Integer, BankAccount> bank) {
            this.rows    = d;
            this.amtMap  = amt;
            this.bankMap = bank;
            fireTableDataChanged();
        }

        @Override public int getRowCount()    { return rows.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }
        @Override public boolean isCellEditable(int r, int c) { return c == 7 || c == 8; }

        @Override
        public Object getValueAt(int r, int c) {
            Withdraw w = rows.get(r);
            BankAccount ba = bankMap.get(w.getWithdrawId());
            return switch (c) {
                case 0 -> w.getWithdrawId();
                case 1 -> ba != null ? ba.getUserId() : "—";
                case 2 -> { BigDecimal a = amtMap.get(w.getWithdrawId());
                            yield a != null ? VND.format(a) + " đ" : "—"; }
                case 3 -> w.getFee() != null ? VND.format(w.getFee()) + " đ" : "0 đ";
                case 4 -> ba != null ? ba.getBankName() : "—";
                case 5 -> ba != null ? ba.getAccountNumber() : "—";
                case 6 -> w.getCreatedAt() != null
                          ? w.getCreatedAt().toLocalDate().toString() : "—";
                case 7 -> "Duyệt";
                case 8 -> "Từ chối";
                default -> "";
            };
        }
    }

    // ── Button Renderers & Editors ───────────────────────────────────────────

    private static JButton makeActionBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 11));
        b.setForeground(Color.WHITE); b.setBackground(bg);
        b.setFocusPainted(false); b.setBorderPainted(false); b.setOpaque(true);
        return b;
    }

    private class GreenBtnRenderer extends JButton implements TableCellRenderer {
        GreenBtnRenderer() { super("Duyệt"); setFont(new Font("Segoe UI",Font.BOLD,11));
            setForeground(Color.WHITE); setBackground(GREEN); setFocusPainted(false); setBorderPainted(false); setOpaque(true); }
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c){return this;}
    }
    private class RedBtnRenderer extends JButton implements TableCellRenderer {
        RedBtnRenderer() { super("Từ chối"); setFont(new Font("Segoe UI",Font.BOLD,11));
            setForeground(Color.WHITE); setBackground(RED); setFocusPainted(false); setBorderPainted(false); setOpaque(true); }
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c){return this;}
    }

    private class ApproveEditor extends AbstractCellEditor implements TableCellEditor {
        private int cur;
        private final JButton btn = makeActionBtn("Duyệt", GREEN);
        ApproveEditor() { btn.addActionListener(e -> { fireEditingStopped(); onApprove(cur); }); }
        @Override public Component getTableCellEditorComponent(JTable t,Object v,boolean s,int r,int c){cur=r;return btn;}
        @Override public Object getCellEditorValue(){return "Duyệt";}
    }
    private class RejectEditor extends AbstractCellEditor implements TableCellEditor {
        private int cur;
        private final JButton btn = makeActionBtn("Từ chối", RED);
        RejectEditor() { btn.addActionListener(e -> { fireEditingStopped(); onReject(cur); }); }
        @Override public Component getTableCellEditorComponent(JTable t,Object v,boolean s,int r,int c){cur=r;return btn;}
        @Override public Object getCellEditorValue(){return "Từ chối";}
    }

    // ── Table styling & helper ────────────────────────────────────────────────

    private JButton makeRefreshBtn() {
        JButton b = new JButton("⟳  Làm mới");
        b.setFont(new Font("Segoe UI", Font.BOLD, 12)); b.setForeground(ORANGE);
        b.setBackground(Color.WHITE);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(245, 220, 180), 1, true), new EmptyBorder(6, 16, 6, 16)));
        b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void styleTable(JTable t) {
        t.setRowHeight(42); t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.getTableHeader().setBackground(new Color(240, 245, 255)); t.getTableHeader().setForeground(NAVY);
        t.setGridColor(new Color(230, 235, 245)); t.setShowVerticalLines(false);
        t.setSelectionBackground(new Color(255, 235, 200));
        t.setFillsViewportHeight(true); t.setBackground(CARD_BG);
    }
}
