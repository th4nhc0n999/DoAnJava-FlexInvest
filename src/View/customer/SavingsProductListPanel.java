package View.customer;

import Controller.InvestmentController;
import Controller.InvestmentController.BuyResult;
import DAO.SavingsProductDAO;
import Model.SavingsProduct;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.EventObject;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Panel listing all ACTIVE savings products.
 * Columns: Tên / Kỳ hạn / Lãi suất / Min / Max / Mua
 *
 * Badge "Chỉ hôm nay" shown for Flex-Sale and Flex-Holiday products.
 * "Mua" button disabled if the product is not open today.
 */
public class SavingsProductListPanel extends JPanel {

    private static final NumberFormat VND_FMT;
    static {
        VND_FMT = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        VND_FMT.setMaximumFractionDigits(0);
    }

    private final int userId;
    private final SavingsProductDAO productDAO = new SavingsProductDAO();
    private final InvestmentController controller = new InvestmentController();
    private final Consumer<Void> onPurchaseComplete; // refresh dashboard/investments

    private List<SavingsProduct> products;
    private ProductTableModel tableModel;
    private JTable table;

    public SavingsProductListPanel(int userId, Consumer<Void> onPurchaseComplete) {
        this.userId = userId;
        this.onPurchaseComplete = onPurchaseComplete;
        setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout(0, 0));
        buildUI();
        loadData();
    }

    private void buildUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(24, 28, 12, 28));
        JLabel title = UITheme.label("Danh sách gói tiết kiệm", UITheme.FONT_TITLE, UITheme.TEXT_PRIMARY);
        JButton refreshBtn = UITheme.ghostButton("↻ Làm mới");
        refreshBtn.addActionListener(e -> loadData());
        header.add(title, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Table
        tableModel = new ProductTableModel();
        table = new JTable(tableModel) {
            @Override public boolean isCellEditable(int r, int c) { return c == 5; } // Allow edit only for button column
            @Override public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? UITheme.BG_CARD : UITheme.BG_DARK);
                }
                return c;
            }
        };
        UITheme.styleTable(table);

        // Custom renderers
        table.getColumnModel().getColumn(0).setCellRenderer(new NameWithBadgeRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(new ButtonEditor());

        // Column widths
        int[] widths = {220, 80, 90, 130, 130, 100};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.setRowHeight(48);

        JScrollPane sp = UITheme.darkScroll(table);
        sp.setBorder(new EmptyBorder(0, 28, 24, 28));
        add(sp, BorderLayout.CENTER);
    }

    private void loadData() {
        SwingWorker<List<SavingsProduct>, Void> w = new SwingWorker<>() {
            @Override protected List<SavingsProduct> doInBackground() {
                return productDAO.getAllActive();
            }
            @Override protected void done() {
                try {
                    products = get();
                    tableModel.fireTableDataChanged();
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        };
        w.execute();
    }

    // -----------------------------------------------------------------------
    // Show buy dialog
    // -----------------------------------------------------------------------

    private void showBuyDialog(SavingsProduct product) {
        // Build dialog content
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UITheme.BG_CARD);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Product info labels
        addRow(panel, gbc, 0, "Gói:", product.getProductName());
        addRow(panel, gbc, 1, "Kỳ hạn:", product.getTerm() + " ngày");
        addRow(panel, gbc, 2, "Lãi suất:", formatRate(product.getInterestRate()) + " / năm");
        addRow(panel, gbc, 3, "Tối thiểu:", vnd(product.getMinInvestmentAmount()) + " VND");
        if (product.getMaxInvestmentAmount() != null) {
            addRow(panel, gbc, 4, "Tối đa:", vnd(product.getMaxInvestmentAmount()) + " VND");
        }

        // Amount field
        gbc.gridy = 5;
        gbc.gridx = 0;
        panel.add(UITheme.label("Số tiền đầu tư (VND):", UITheme.FONT_BODY, UITheme.TEXT_MUTED), gbc);
        gbc.gridx = 1;
        JTextField amountField = new JTextField(16);
        amountField.setBackground(UITheme.BG_DARK);
        amountField.setForeground(UITheme.TEXT_PRIMARY);
        amountField.setCaretColor(UITheme.ACCENT_TEAL);
        amountField.setFont(UITheme.FONT_MONO);
        amountField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
                new EmptyBorder(6, 8, 6, 8)
        ));
        panel.add(amountField, gbc);

        // Show amount input dialog
        int confirm = JOptionPane.showConfirmDialog(
                SwingUtilities.getWindowAncestor(this),
                panel, "Mua gói: " + product.getProductName(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );
        if (confirm != JOptionPane.OK_OPTION) return;

        String amtText = amountField.getText().replaceAll("[,. ]", "").trim();
        BigDecimal amount;
        try {
            amount = new BigDecimal(amtText);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Số tiền không hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validate conditions first
        SwingWorker<BuyResult, Void> validateWorker = new SwingWorker<>() {
            @Override protected BuyResult doInBackground() {
                return controller.buyProduct(userId, product.getProductId(), amount);
            }
            @Override protected void done() {
                try {
                    BuyResult result = get();
                    
                    // If validation failed, show error and return
                    if (result != BuyResult.SUCCESS) {
                        String message = switch (result) {
                            case KYC_NOT_APPROVED -> "eKYC chưa được phê duyệt.";
                            case PRODUCT_NOT_OPEN -> "Gói này không mở bán hôm nay.";
                            case AMOUNT_BELOW_MIN -> "Số tiền dưới mức tối thiểu.";
                            case AMOUNT_ABOVE_MAX -> "Số tiền vượt quá mức tối đa.";
                            case INSUFFICIENT_BALANCE -> "Số dư ví không đủ.";
                            case PRODUCT_NOT_FOUND -> "Gói không tồn tại.";
                            case DB_ERROR -> "Lỗi hệ thống, vui lòng thử lại.";
                            default -> "Lỗi không xác định.";
                        };
                        JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(SavingsProductListPanel.this),
                                message, "Lỗi", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    // All validation passed, now show OTP confirmation
                    Frame owner = (Frame) SwingUtilities.getWindowAncestor(SavingsProductListPanel.this);
                    String otp = OtpDialog.showDialog(owner,
                            "Xác nhận giao dịch",
                            "Nhập mã OTP 6 số được gửi đến điện thoại của bạn");
                    if (otp == null) return; // user cancelled
                    
                    // Purchase confirmed
                    JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(SavingsProductListPanel.this),
                            "Mua gói thành công!", "Kết quả", JOptionPane.INFORMATION_MESSAGE);
                    loadData();
                    onPurchaseComplete.accept(null);
                    
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(SavingsProductListPanel.this),
                            "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        validateWorker.execute();
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, String value) {
        gbc.gridy = row;
        gbc.gridx = 0;
        p.add(UITheme.label(label, UITheme.FONT_BODY, UITheme.TEXT_MUTED), gbc);
        gbc.gridx = 1;
        p.add(UITheme.label(value, UITheme.FONT_BODY, UITheme.TEXT_PRIMARY), gbc);
    }

    // -----------------------------------------------------------------------
    // Table model
    // -----------------------------------------------------------------------

    private class ProductTableModel extends AbstractTableModel {
        private final String[] COLS = {"Tên gói", "Kỳ hạn", "Lãi suất", "Tối thiểu", "Tối đa", "Hành động"};

        @Override public int getRowCount() { return products == null ? 0 : products.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }

        @Override public Object getValueAt(int r, int c) {
            SavingsProduct p = products.get(r);
            return switch (c) {
                case 0 -> p;                            // NameWithBadgeRenderer handles this
                case 1 -> p.getTerm() + " ngày";
                case 2 -> formatRate(p.getInterestRate()) + "%";
                case 3 -> vnd(p.getMinInvestmentAmount());
                case 4 -> p.getMaxInvestmentAmount() != null ? vnd(p.getMaxInvestmentAmount()) : "—";
                case 5 -> p;                            // ButtonRenderer handles this
                default -> "";
            };
        }

        @Override public Class<?> getColumnClass(int c) {
            return (c == 0 || c == 5) ? SavingsProduct.class : String.class;
        }
    }

    // -----------------------------------------------------------------------
    // Cell renderers & editor
    // -----------------------------------------------------------------------

    /** Renders product name + optional "Chỉ hôm nay" badge. */
    private class NameWithBadgeRenderer implements TableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean focus, int row, int col) {
            SavingsProduct p = (SavingsProduct) val;
            JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            cell.setBackground(row % 2 == 0 ? UITheme.BG_CARD : UITheme.BG_DARK);
            cell.setBorder(new EmptyBorder(0, 8, 0, 8));

            JLabel name = UITheme.label(p.getProductName(), UITheme.FONT_BODY, UITheme.TEXT_PRIMARY);
            cell.add(name);

            if ((p.isFlexSale() || p.isFlexHoliday()) && p.isOpenToday()) {
                JLabel badge = new JLabel("Chỉ hôm nay") {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(UITheme.BADGE_BG);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                badge.setFont(UITheme.FONT_BADGE);
                badge.setForeground(UITheme.BADGE_FG);
                badge.setBorder(new EmptyBorder(2, 6, 2, 6));
                badge.setOpaque(false);
                cell.add(badge);
            }
            return cell;
        }
    }

    /** Renders the Mua button (disabled if not open today). */
    private class ButtonRenderer implements TableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean focus, int row, int col) {
            SavingsProduct p = (SavingsProduct) val;
            JButton btn = UITheme.primaryButton("Mua");
            btn.setEnabled(p.isOpenToday());
            if (!p.isOpenToday()) {
                btn.setForeground(UITheme.TEXT_MUTED);
                btn.setToolTipText("Gói này không mở cửa hôm nay");
            }
            JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
            wrap.setBackground(row % 2 == 0 ? UITheme.BG_CARD : UITheme.BG_DARK);
            wrap.add(btn);
            return wrap;
        }
    }

    /** Handles click on Mua button — opens buy dialog. */
    private class ButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private SavingsProduct currentProduct;
        private JButton button;
        private JPanel panel;

        ButtonEditor() {
            button = UITheme.primaryButton("Mua");
            button.addActionListener(e -> {
                fireEditingStopped();
                if (currentProduct != null && currentProduct.isOpenToday()) {
                    showBuyDialog(currentProduct);
                }
            });
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
            panel.add(button);
        }

        @Override public Component getTableCellEditorComponent(JTable t, Object val,
                boolean sel, int row, int col) {
            currentProduct = (SavingsProduct) val;
            button.setEnabled(currentProduct.isOpenToday());
            panel.setBackground(t.getSelectionBackground());
            return panel;
        }

        @Override public Object getCellEditorValue() { return currentProduct; }
        
        @Override public boolean isCellEditable(EventObject e) {
            if (e instanceof MouseEvent) {
                return ((MouseEvent) e).getClickCount() >= 1;
            }
            return true;
        }
    }

    // -----------------------------------------------------------------------
    // Formatting helpers
    // -----------------------------------------------------------------------

    private String vnd(BigDecimal v) {
        if (v == null) return "—";
        return VND_FMT.format(v);
    }

    private String formatRate(BigDecimal rate) {
        if (rate == null) return "—";
        // rate is stored as fraction (0.075 = 7.5%) — multiply by 100
        return String.format("%.2f", rate.doubleValue() * 100);
    }
}
