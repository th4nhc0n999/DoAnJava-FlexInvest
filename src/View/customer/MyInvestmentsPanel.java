package View.customer;

import Controller.InvestmentController;
import DAO.InvestmentDAO;
import DAO.SavingsProductDAO;
import Model.Investment;
import Model.SavingsProduct;
import Utils.InterestCalculator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Panel showing the user's active and pending investments.
 *
 * Columns: Gói / Gốc / Lãi ước tính / Ngày đáo hạn / Trạng thái / Phương thức TT / Rút sớm
 *
 * Rules:
 *  • Lãi ước tính = InterestCalculator.calculateInterestForDeposit(inv, product) — display only
 *  • Rows with maturity within 3 days are highlighted yellow
 *  • Dropdown "Phương thức tất toán" (PT1/PT2/PT3) persists to DB
 *  • "Rút sớm" → confirm dialog → OTP → redeemEarly()
 */
public class MyInvestmentsPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat VND_FMT;
    static {
        VND_FMT = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        VND_FMT.setMaximumFractionDigits(0);
    }

    private static final String[] REDEMPTION_METHODS = {"—", "PT1", "PT2", "PT3"};
    private static final int WARN_DAYS = 3;

    private final int userId;
    private final InvestmentDAO investmentDAO = new InvestmentDAO();
    private final SavingsProductDAO productDAO = new SavingsProductDAO();
    private final InvestmentController controller = new InvestmentController();
    private final Consumer<Void> onRedeemComplete;

    private List<Investment> investments;
    private Map<Integer, SavingsProduct> productMap;  // productId → product
    private InvestmentsTableModel tableModel;
    private JTable table;

    public MyInvestmentsPanel(int userId, Consumer<Void> onRedeemComplete) {
        this.userId = userId;
        this.onRedeemComplete = onRedeemComplete;
        setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
        refresh();
    }

    private void buildUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(24, 28, 12, 28));
        JLabel title = UITheme.label("Lệnh đầu tư của tôi", UITheme.FONT_TITLE, UITheme.TEXT_PRIMARY);
        JButton refreshBtn = UITheme.ghostButton("↻ Làm mới");
        refreshBtn.addActionListener(e -> refresh());
        header.add(title, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Table
        tableModel = new InvestmentsTableModel();
        table = new JTable(tableModel) {
            @Override public boolean isCellEditable(int r, int c) {
                // Col 5 = method dropdown, col 6 = button editor
                return c == 5 || c == 6;
            }

            @Override public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component comp = super.prepareRenderer(renderer, row, col);
                if (investments != null && row < investments.size()) {
                    Investment inv = investments.get(row);
                    if (!isRowSelected(row)) {
                        if (inv.isDueWithinDays(WARN_DAYS)) {
                            comp.setBackground(new Color(0x3D2E00)); // warm dark yellow highlight
                        } else {
                            comp.setBackground(row % 2 == 0 ? UITheme.BG_CARD : UITheme.BG_DARK);
                        }
                    }
                }
                return comp;
            }
        };
        UITheme.styleTable(table);
        table.setRowHeight(48);

        // Column widths
        int[] widths = {200, 130, 130, 110, 90, 100, 90};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Custom renderers
        table.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(new MethodRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(new MethodEditor());
        table.getColumnModel().getColumn(6).setCellRenderer(new RedeemButtonRenderer());
        table.getColumnModel().getColumn(6).setCellEditor(new RedeemButtonEditor(new JCheckBox()));

        JScrollPane sp = UITheme.darkScroll(table);
        sp.setBorder(new EmptyBorder(0, 28, 24, 28));
        add(sp, BorderLayout.CENTER);

        // Legend
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        legend.setOpaque(false);
        legend.setBorder(new EmptyBorder(0, 28, 12, 28));
        JLabel l = UITheme.label("⚠  Nền vàng: đáo hạn trong " + WARN_DAYS + " ngày tới",
                UITheme.FONT_SMALL, UITheme.ACCENT_GOLD);
        legend.add(l);
        add(legend, BorderLayout.SOUTH);
    }

    public void refresh() {
        SwingWorker<Void, Void> w = new SwingWorker<>() {
            @Override protected Void doInBackground() {
                investments = investmentDAO.getActiveByUserId(userId);
                productMap = investments.stream()
                        .collect(Collectors.toMap(
                                Investment::getProductId,
                                inv -> {
                                    SavingsProduct p = productDAO.getById(inv.getProductId());
                                    return p != null ? p : new SavingsProduct();
                                },
                                (a, b) -> a
                        ));
                return null;
            }

            @Override protected void done() {
                tableModel.fireTableDataChanged();
            }
        };
        w.execute();
    }

    // -----------------------------------------------------------------------
    // Redeem Early flow
    // -----------------------------------------------------------------------

    private void doRedeemEarly(int investmentId) {
        Investment inv = investments.stream()
                .filter(i -> i.getInvestmentId() == investmentId).findFirst().orElse(null);
        if (inv == null) return;

        SavingsProduct product = productMap.get(inv.getProductId());
        BigDecimal estInterest = InterestCalculator.calculateInterestForDeposit(inv, product);
        BigDecimal estPayout = inv.getInvestedAmount().add(estInterest);

        String msg = String.format(
                "<html><body style='width:300px'>"
                + "<b>Xác nhận tất toán sớm</b><br><br>"
                + "Gói: %s<br>"
                + "Gốc: %s VND<br>"
                + "Lãi ước tính (đến hôm nay): %s VND<br>"
                + "Dự kiến nhận: %s VND<br><br>"
                + "<font color='#F5A623'>Lưu ý: Áp dụng phí phạt tất toán sớm theo quy định của gói.</font>"
                + "</body></html>",
                inv.getProductName(),
                vnd(inv.getInvestedAmount()),
                vnd(estInterest),
                vnd(estPayout)
        );

        int choice = JOptionPane.showConfirmDialog(
                SwingUtilities.getWindowAncestor(this), msg,
                "Tất toán sớm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        String otp = OtpDialog.showDialog(owner,
                "Xác nhận tất toán sớm",
                "Nhập mã OTP 6 số để xác nhận giao dịch");
        if (otp == null) return;

        boolean success = controller.redeemEarly(investmentId);
        if (success) {
            JOptionPane.showMessageDialog(this,
                    "Tất toán sớm thành công.",
                    "Thành công ✅",
                    JOptionPane.INFORMATION_MESSAGE);
            refresh();
            if (onRedeemComplete != null) onRedeemComplete.accept(null);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Không thể tất toán sớm. Vui lòng thử lại.",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // -----------------------------------------------------------------------
    // Table model
    // -----------------------------------------------------------------------

    private class InvestmentsTableModel extends AbstractTableModel {
        private final String[] COLS = {"Gói", "Gốc (VND)", "Lãi ước tính", "Ngày đáo hạn", "Trạng thái", "Phương thức TT", "Rút sớm"};

        @Override public int getRowCount() { return investments == null ? 0 : investments.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }

        @Override public Object getValueAt(int r, int c) {
            Investment inv = investments.get(r);
            SavingsProduct product = productMap != null ? productMap.get(inv.getProductId()) : null;
            return switch (c) {
                case 0 -> inv.getProductName() != null ? inv.getProductName() : "Gói #" + inv.getProductId();
                case 1 -> vnd(inv.getInvestedAmount());
                case 2 -> vnd(InterestCalculator.calculateInterestForDeposit(inv, product));
                case 3 -> inv.getMaturityDate() != null
                        ? inv.getMaturityDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FMT)
                        : "—";
                case 4 -> inv.getStatus();
                case 5 -> inv;   // method dropdown
                case 6 -> inv;   // redeem button
                default -> "";
            };
        }

        @Override public void setValueAt(Object val, int r, int c) {
            // Called by MethodEditor to persist selection
            if (c == 5 && val instanceof String) {
                investmentDAO.updateRedemptionMethod(investments.get(r).getInvestmentId(), (String) val);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Renderers and editors
    // -----------------------------------------------------------------------

    private class StatusRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean focus, int row, int col) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, row, col);
            String status = val == null ? "" : val.toString();
            Color c = switch (status) {
                case "ACTIVE"   -> UITheme.SUCCESS_GREEN;
                case "PENDING"  -> UITheme.ACCENT_GOLD;
                case "REDEEMED" -> UITheme.TEXT_MUTED;
                default         -> UITheme.TEXT_PRIMARY;
            };
            lbl.setForeground(c);
            lbl.setFont(UITheme.FONT_BODY);
            lbl.setHorizontalAlignment(CENTER);
            return lbl;
        }
    }

    private class MethodRenderer implements TableCellRenderer {
        private final JComboBox<String> combo = new JComboBox<>(REDEMPTION_METHODS);
        { combo.setBackground(UITheme.BG_CARD); combo.setForeground(UITheme.TEXT_PRIMARY); combo.setFont(UITheme.FONT_BODY); }

        @Override public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean focus, int row, int col) {
            if (val instanceof Investment) {
                String method = ((Investment) val).getRedemptionMethod();
                combo.setSelectedItem(method != null ? method : "—");
            }
            return combo;
        }
    }

    private class MethodEditor extends DefaultCellEditor {
        private final JComboBox<String> combo;
        private int editingRow;

        MethodEditor() {
            super(new JComboBox<>(REDEMPTION_METHODS));
            combo = (JComboBox<String>) editorComponent;
            combo.setBackground(UITheme.BG_CARD);
            combo.setForeground(UITheme.ACCENT_TEAL);
            combo.setFont(UITheme.FONT_BODY);
            combo.addActionListener(e -> {
                if (investments != null && editingRow < investments.size()) {
                    String selected = (String) combo.getSelectedItem();
                    tableModel.setValueAt(selected, editingRow, 5);
                }
            });
        }

        @Override public Component getTableCellEditorComponent(JTable t, Object val,
                boolean sel, int row, int col) {
            editingRow = row;
            if (val instanceof Investment) {
                String m = ((Investment) val).getRedemptionMethod();
                combo.setSelectedItem(m != null ? m : "—");
            }
            return combo;
        }

        @Override public Object getCellEditorValue() { return combo.getSelectedItem(); }
    }

    private class RedeemButtonRenderer implements TableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean focus, int row, int col) {
            Investment inv = val instanceof Investment ? (Investment) val : null;
            JButton btn = UITheme.dangerButton("Rút sớm");
            btn.setBackground(UITheme.ACCENT_RED);
            btn.setForeground(Color.WHITE);
            boolean canRedeem = inv != null && ("ACTIVE".equals(inv.getStatus()) || "PENDING".equals(inv.getStatus()));
            btn.setEnabled(canRedeem);
            JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
            wrap.setBackground(row % 2 == 0 ? UITheme.BG_CARD : UITheme.BG_DARK);
            wrap.add(btn);
            return wrap;
        }
    }

    private class RedeemButtonEditor extends DefaultCellEditor {
        private Investment currentInv;

        RedeemButtonEditor(JCheckBox cb) { super(cb); }

        @Override public Component getTableCellEditorComponent(JTable t, Object val,
                boolean sel, int row, int col) {
            currentInv = val instanceof Investment ? (Investment) val : null;
            JButton btn = UITheme.dangerButton("Rút sớm");
            btn.setBackground(UITheme.ACCENT_RED);
            btn.setForeground(Color.WHITE);
            btn.addActionListener(e -> {
                fireEditingStopped();
                if (currentInv != null) doRedeemEarly(currentInv.getInvestmentId());
            });
            JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
            wrap.setBackground(t.getSelectionBackground());
            wrap.add(btn);
            return wrap;
        }

        @Override public Object getCellEditorValue() { return currentInv; }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String vnd(BigDecimal v) {
        if (v == null) return "0";
        return VND_FMT.format(v);
    }
}
