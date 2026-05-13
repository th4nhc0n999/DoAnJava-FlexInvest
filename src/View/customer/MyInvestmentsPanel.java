package View.customer;

import Controller.InvestmentController;
import DAO.InvestmentDAO;
import Model.*;
import Utils.DateUtils;
import Utils.InterestCalculator;
import View.OtpDialog;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.Locale;

/**
 * MyInvestmentsPanel — Quản lý các khoản đầu tư đang chạy của user.
 *
 * Cột: Gói / Gốc / Lãi ước tính đến hôm nay / Ngày đáo hạn / Trạng thái / Phương thức / Rút sớm
 * - Lãi ước tính gọi calculateInterestForDeposit() với redeemDate=hôm nay (KHÔNG tất toán thật)
 * - Highlight màu vàng nếu gói đáo hạn ≤ 3 ngày
 * - Nút "Rút sớm" → confirm dialog → OTP → InvestmentController.redeemEarly()
 * - Dropdown phương thức tất toán PT1/PT2/PT3 lưu trong Map<investmentId, method>
 */
public class MyInvestmentsPanel extends JPanel {

    private static final Color NAVY       = new Color(15, 40, 80);
    private static final Color BLUE       = new Color(0, 162, 232);
    private static final Color GREEN      = new Color(16, 185, 129);
    private static final Color YELLOW_ROW = new Color(255, 245, 210);
    private static final Color RED        = new Color(239, 68, 68);
    private static final Color BG         = new Color(238, 243, 250);
    private static final Color CARD_BG    = Color.WHITE;
    private static final Color TEXT_DARK  = new Color(30, 30, 40);
    private static final Color TEXT_MUTED = new Color(110, 115, 130);
    private static final NumberFormat VND = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private final AccountModel         account;
    private final int                  userId;
    private final InvestmentDAO        investDAO   = new InvestmentDAO();
    private final InvestmentController controller  = new InvestmentController();

    /** Lưu phương thức tất toán được chọn cho từng investmentId (chỉ trong bộ nhớ). */
    private final Map<Integer, String> payoutMethods = new HashMap<>();

    private List<Investment>    investments;
    private List<SavingsProduct> products;
    private InvestmentTableModel tableModel;
    private JTable               table;
    private JLabel               lblStatus;
    private JButton              btnRefresh;

    public MyInvestmentsPanel(AccountModel account) {
        this.account = account;
        this.userId  = account.getUser().getUserId();
        setLayout(new BorderLayout());
        setBackground(BG);
        build();
        loadData();
    }

    // =========================================================================
    //  Build UI
    // =========================================================================

    private void build() {
        JPanel inner = new JPanel(new BorderLayout(0, 16));
        inner.setBackground(BG);
        inner.setBorder(new EmptyBorder(24, 28, 24, 28));

        // ── Header ─────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Khoản Đầu Tư Của Tôi");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(NAVY);
        JLabel sub = new JLabel("Theo dõi lãi ước tính và quản lý tất toán");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(TEXT_MUTED);
        left.add(title);
        left.add(sub);

        btnRefresh = new JButton("⟳  Làm mới");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.setForeground(BLUE);
        btnRefresh.setBackground(Color.WHITE);
        btnRefresh.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 225, 245), 1, true),
            new EmptyBorder(6, 16, 6, 16)));
        btnRefresh.setFocusPainted(false);
        btnRefresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRefresh.addActionListener(e -> loadData());
        header.add(left, BorderLayout.WEST);
        header.add(btnRefresh, BorderLayout.EAST);
        inner.add(header, BorderLayout.NORTH);

        // ── Legend ─────────────────────────────────────────────────────────
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        legend.setOpaque(false);
        JPanel yellowBox = new JPanel(); yellowBox.setBackground(YELLOW_ROW);
        yellowBox.setPreferredSize(new Dimension(14, 14));
        legend.add(yellowBox);
        JLabel legendLbl = new JLabel("Sắp đáo hạn ≤ 3 ngày");
        legendLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        legendLbl.setForeground(TEXT_MUTED);
        legend.add(legendLbl);
        inner.add(legend, BorderLayout.CENTER); // temporary placeholder

        // ── Table ──────────────────────────────────────────────────────────
        tableModel = new InvestmentTableModel();
        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    boolean warn = isNearMaturity(row);
                    c.setBackground(warn ? YELLOW_ROW : CARD_BG);
                    c.setForeground(TEXT_DARK);
                }
                return c;
            }
        };
        styleTable(table);

        // Column renderers
        table.getColumnModel().getColumn(5).setCellRenderer(new PayoutComboRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(new PayoutComboEditor());
        table.getColumnModel().getColumn(6).setCellRenderer(new ActionButtonRenderer());
        table.getColumnModel().getColumn(6).setCellEditor(new ActionButtonEditor());

        // Column widths
        int[] widths = {150, 130, 130, 110, 95, 115, 80};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Wrap legend + table in a panel
        JPanel tableSection = new JPanel(new BorderLayout(0, 6));
        tableSection.setOpaque(false);
        tableSection.add(legend, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(new Color(210, 220, 235), 1));
        sp.getViewport().setBackground(CARD_BG);
        tableSection.add(sp, BorderLayout.CENTER);

        // Reset layout
        inner.remove(legend);   // was in CENTER
        inner.add(tableSection, BorderLayout.CENTER);

        // ── Status ─────────────────────────────────────────────────────────
        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblStatus.setForeground(TEXT_MUTED);
        inner.add(lblStatus, BorderLayout.SOUTH);

        add(inner, BorderLayout.CENTER);
    }

    // =========================================================================
    //  Data
    // =========================================================================

    public void loadData() {
        btnRefresh.setEnabled(false);
        btnRefresh.setText("⟳  Đang tải...");
        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() {
                List<Investment>    invs  = investDAO.getActiveByUserId(userId);
                List<SavingsProduct> prods = investDAO.getAllActiveProducts();
                // Tính lãi ước tính cho từng khoản
                Map<Integer, BigDecimal> estMap = new HashMap<>();
                for (Investment inv : invs) {
                    try {
                        SavingsProduct prod = findProduct(prods, inv.getProductId());
                        if (prod == null) continue;
                        LocalDate start    = DateUtils.toLocalDate(inv.getStartDate());
                        LocalDate today    = LocalDate.now();
                        LocalDate matDate  = DateUtils.toLocalDate(inv.getMaturityDate());
                        InterestCalculator.InterestResult res = InterestCalculator.calculate(
                            inv.getInvestedAmount(), start, today, prod, matDate, inv.getAppliedInterestRate());
                        estMap.put(inv.getInvestmentId(), res.interestAmount);
                    } catch (Exception ignored) {}
                }
                return new Object[]{invs, prods, estMap};
            }

            @SuppressWarnings("unchecked")
            @Override protected void done() {
                try {
                    Object[] data = get();
                    investments = (List<Investment>)        data[0];
                    products    = (List<SavingsProduct>)    data[1];
                    Map<Integer, BigDecimal> estMap = (Map<Integer, BigDecimal>) data[2];
                    tableModel.setData(investments, products, estMap, payoutMethods);
                    lblStatus.setText(investments.size() + " khoản đang ACTIVE — cập nhật lúc " + java.time.LocalTime.now().withNano(0));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    lblStatus.setText("Lỗi tải dữ liệu: " + ex.getMessage());
                } finally {
                    btnRefresh.setEnabled(true);
                    btnRefresh.setText("⟳  Làm mới");
                }
            }
        }.execute();
    }

    private boolean isNearMaturity(int row) {
        if (investments == null || row >= investments.size()) return false;
        Investment inv = investments.get(row);
        LocalDate mat = DateUtils.toLocalDate(inv.getMaturityDate());
        if (mat == null) return false;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), mat);
        return days >= 0 && days <= 3;
    }

    private static SavingsProduct findProduct(List<SavingsProduct> list, int productId) {
        return list == null ? null : list.stream()
            .filter(p -> p.getProductId() == productId).findFirst().orElse(null);
    }

    // =========================================================================
    //  Early Redemption Flow
    // =========================================================================

    private void onRedeemEarly(int row) {
        if (investments == null || row >= investments.size()) return;
        Investment inv = investments.get(row);

        int confirm = JOptionPane.showConfirmDialog(this,
            "<html><b>Xác nhận rút sớm?</b><br>" +
            "Gốc: " + VND.format(inv.getInvestedAmount()) + " đ<br>" +
            "Rút sớm sẽ áp dụng lãi suất phạt (fallback rate).<br>" +
            "Bạn sẽ nhận về ít hơn nếu rút trước hạn.</html>",
            "Rút sớm — Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        // OTP
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        OtpDialog otp = new OtpDialog(parentFrame, "Rút sớm khoản đầu tư #" + inv.getInvestmentId());
        String code = otp.showAndGetOtp();
        if (code == null) return;

        btnRefresh.setEnabled(false);
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                return controller.redeemEarly(inv.getInvestmentId());
            }
            @Override protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        JOptionPane.showMessageDialog(MyInvestmentsPanel.this,
                            "Rút sớm thành công!\nTiền đã được hoàn vào ví của bạn.",
                            "Thành công ✅", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(MyInvestmentsPanel.this,
                            "Rút sớm thất bại. Vui lòng kiểm tra lại trạng thái khoản đầu tư.",
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    btnRefresh.setEnabled(true);
                    loadData();
                }
            }
        }.execute();
    }

    // =========================================================================
    //  TableModel
    // =========================================================================

    static class InvestmentTableModel extends AbstractTableModel {
        private static final String[] COLS = {
            "Tên gói", "Gốc (đ)", "Lãi ước tính", "Đáo hạn", "Trạng thái", "Phương thức", "Rút sớm"
        };
        private List<Investment>    rows     = new ArrayList<>();
        private Map<Integer, SavingsProduct> prodMap  = new HashMap<>();
        private Map<Integer, BigDecimal>     estMap   = new HashMap<>();
        private Map<Integer, String>         payMap   = new HashMap<>();

        void setData(List<Investment> invs, List<SavingsProduct> prods,
                     Map<Integer, BigDecimal> estMap, Map<Integer, String> payMap) {
            this.rows    = invs;
            this.prodMap.clear();
            if (prods != null) prods.forEach(p -> prodMap.put(p.getProductId(), p));
            this.estMap  = estMap;
            this.payMap  = payMap;
            fireTableDataChanged();
        }

        @Override public int getRowCount()    { return rows.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }
        @Override public boolean isCellEditable(int r, int c) { return c == 5 || c == 6; }

        @Override
        public Object getValueAt(int r, int c) {
            Investment inv = rows.get(r);
            SavingsProduct p = prodMap.get(inv.getProductId());
            return switch (c) {
                case 0 -> p != null ? p.getProductName() : "Gói #" + inv.getProductId();
                case 1 -> VND.format(inv.getInvestedAmount()) + " đ";
                case 2 -> {
                    BigDecimal est = estMap.get(inv.getInvestmentId());
                    yield est != null ? "+ " + VND.format(est) + " đ" : "—";
                }
                case 3 -> {
                    LocalDate mat = DateUtils.toLocalDate(inv.getMaturityDate());
                    if (mat == null) yield "Không kỳ hạn";
                    long days = ChronoUnit.DAYS.between(LocalDate.now(), mat);
                    yield mat + (days <= 3 && days >= 0 ? " ⚡" : "");
                }
                case 4 -> inv.getStatus();
                case 5 -> payMap.getOrDefault(inv.getInvestmentId(), "PT1");
                case 6 -> "Rút sớm";
                default -> "";
            };
        }

        @Override
        public void setValueAt(Object val, int r, int c) {
            if (c == 5 && r < rows.size()) {
                payMap.put(rows.get(r).getInvestmentId(), String.valueOf(val));
            }
        }

        Investment getInvestment(int row) { return rows.get(row); }
    }

    // =========================================================================
    //  Payout Method Combo Renderer / Editor
    // =========================================================================

    private static final String[] PAYOUT_OPTIONS = {
        "PT1 — Rollover gốc+lãi",
        "PT2 — Rollover gốc, rút lãi",
        "PT3 — Rút toàn bộ"
    };
    private static final String[] PAYOUT_CODES = {"PT1", "PT2", "PT3"};

    private class PayoutComboRenderer extends JComboBox<String> implements TableCellRenderer {
        PayoutComboRenderer() {
            for (String s : PAYOUT_OPTIONS) addItem(s);
            setFont(new Font("Segoe UI", Font.PLAIN, 12));
            setBorder(BorderFactory.createEmptyBorder());
        }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean foc, int row, int col) {
            String code = String.valueOf(val);
            int idx = 0;
            for (int i = 0; i < PAYOUT_CODES.length; i++) if (PAYOUT_CODES[i].equals(code)) { idx = i; break; }
            setSelectedIndex(idx);
            setBackground(isNearMaturity(row) ? YELLOW_ROW : (sel ? new Color(210, 230, 255) : CARD_BG));
            return this;
        }
    }

    private class PayoutComboEditor extends DefaultCellEditor {
        private final JComboBox<String> combo = new JComboBox<>(PAYOUT_OPTIONS);
        private int currentRow;

        PayoutComboEditor() {
            super(new JCheckBox());
            combo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            combo.addActionListener(e -> {
                int idx = combo.getSelectedIndex();
                if (idx >= 0 && currentRow < investments.size()) {
                    int invId = investments.get(currentRow).getInvestmentId();
                    payoutMethods.put(invId, PAYOUT_CODES[idx]);
                    tableModel.setValueAt(PAYOUT_CODES[idx], currentRow, 5);
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object val,
                boolean sel, int row, int col) {
            currentRow = row;
            String code = String.valueOf(val);
            for (int i = 0; i < PAYOUT_CODES.length; i++)
                if (PAYOUT_CODES[i].equals(code)) { combo.setSelectedIndex(i); break; }
            return combo;
        }
        @Override public Object getCellEditorValue() {
            int idx = combo.getSelectedIndex();
            return idx >= 0 ? PAYOUT_CODES[idx] : "PT1";
        }
    }

    // =========================================================================
    //  Action Button Renderer / Editor
    // =========================================================================

    private static class ActionButtonRenderer extends JButton implements TableCellRenderer {
        ActionButtonRenderer() {
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setForeground(Color.WHITE);
            setBackground(new Color(239, 68, 68));
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(true);
        }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean foc, int row, int col) {
            setText("Rút sớm");
            setBackground(new Color(239, 68, 68));
            return this;
        }
    }

    private class ActionButtonEditor extends DefaultCellEditor {
        private int currentRow;
        private final JButton btn;

        ActionButtonEditor() {
            super(new JCheckBox());
            btn = new JButton("Rút sớm");
            btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btn.setForeground(Color.WHITE);
            btn.setBackground(new Color(239, 68, 68));
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.addActionListener(e -> {
                fireEditingStopped();
                onRedeemEarly(currentRow);
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object val,
                boolean sel, int row, int col) {
            currentRow = row;
            return btn;
        }
        @Override public Object getCellEditorValue() { return "Rút sớm"; }
    }

    // =========================================================================
    //  Table Styling
    // =========================================================================

    private void styleTable(JTable t) {
        t.setRowHeight(44);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.getTableHeader().setBackground(new Color(240, 245, 255));
        t.getTableHeader().setForeground(NAVY);
        t.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 220, 235)));
        t.setGridColor(new Color(230, 235, 245));
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(12, 0));
        t.setSelectionBackground(new Color(210, 230, 255));
        t.setSelectionForeground(TEXT_DARK);
        t.setFillsViewportHeight(true);
        t.setBackground(CARD_BG);
    }
}
