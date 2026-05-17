package View.customer;

import Controller.InvestmentController;
import DAO.InvestmentDAO;
import DAO.WalletDAO;
import Model.*;
import View.OtpDialog;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * SavingsProductListPanel — Danh sách gói tiết kiệm đang ACTIVE.
 *
 * Cột: Tên / Kỳ hạn / Lãi suất / Min / Max / Trạng thái / Mua
 * - Badge "Chỉ hôm nay" nếu gói có date-window và isOpenToday() = true
 * - Nút Mua bị disable nếu gói không mở hôm nay (isOpenToday() = false)
 * - Nút Mua mở dialog nhập số tiền → OTP → InvestmentController.buyProduct()
 */
public class SavingsProductListPanel extends JPanel {

    private static final Color NAVY       = new Color(15, 40, 80);
    private static final Color BLUE       = new Color(0, 162, 232);
    private static final Color GREEN      = new Color(16, 185, 129);
    private static final Color ORANGE     = new Color(249, 115, 22);
    private static final Color BG         = new Color(238, 243, 250);
    private static final Color CARD_BG    = Color.WHITE;
    private static final Color TEXT_DARK  = new Color(30, 30, 40);
    private static final Color TEXT_MUTED = new Color(110, 115, 130);
    private static final NumberFormat VND = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private final AccountModel       account;
    private final int                userId;
    private final InvestmentDAO      investDAO  = new InvestmentDAO();
    private final WalletDAO          walletDAO  = new WalletDAO();
    private final InvestmentController controller = new InvestmentController();

    private List<SavingsProduct> products;
    private JTable               table;
    private ProductTableModel    tableModel;
    private JLabel               lblStatus;
    private JButton              btnRefresh;

    public SavingsProductListPanel(AccountModel account) {
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
        JLabel title = new JLabel("Gói Đầu Tư");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(NAVY);
        JLabel sub = new JLabel("Chọn gói phù hợp và bắt đầu tích lũy");
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

        // ── Table ──────────────────────────────────────────────────────────
        tableModel = new ProductTableModel();
        table = new JTable(tableModel);
        styleTable(table);

        // Custom renderers
        table.getColumnModel().getColumn(0).setCellRenderer(new NameRenderer());    // Tên + badge
        table.getColumnModel().getColumn(5).setCellRenderer(new StatusRenderer());  // Trạng thái
        table.getColumnModel().getColumn(6).setCellRenderer(new BuyButtonRenderer());
        table.getColumnModel().getColumn(6).setCellEditor(new BuyButtonEditor());

        // Column widths
        int[] widths = {220, 90, 90, 110, 120, 100, 90};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(new Color(210, 220, 235), 1));
        sp.setBackground(CARD_BG);
        sp.getViewport().setBackground(CARD_BG);
        inner.add(sp, BorderLayout.CENTER);

        // ── Status bar ─────────────────────────────────────────────────────
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
        new SwingWorker<List<SavingsProduct>, Void>() {
            @Override protected List<SavingsProduct> doInBackground() {
                return investDAO.getAllActiveProducts();
            }
            @Override protected void done() {
                try {
                    products = get();
                    tableModel.setProducts(products);
                    lblStatus.setText("Hiển thị " + products.size() + " gói — cập nhật lúc " + java.time.LocalTime.now().withNano(0));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    lblStatus.setText("Lỗi khi tải dữ liệu: " + ex.getMessage());
                } finally {
                    btnRefresh.setEnabled(true);
                    btnRefresh.setText("⟳  Làm mới");
                }
            }
        }.execute();
    }

    // =========================================================================
    //  Buy Flow
    // =========================================================================

    private void onBuyClicked(int row) {
        if (products == null || row >= products.size()) return;
        SavingsProduct p = products.get(row);

        if (!p.isOpenToday()) {
            JOptionPane.showMessageDialog(this,
                "Gói " + p.getProductName() + " hiện không trong thời gian mở bán.",
                "Không thể mua", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // ── Dialog nhập số tiền ─────────────────────────────────────────────
        String minStr = VND.format(p.getMinInvestmentAmount());
        String maxStr = p.getMaxInvestmentAmount() != null ? VND.format(p.getMaxInvestmentAmount()) : "không giới hạn";

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 10));
        form.setBorder(new EmptyBorder(8, 0, 8, 0));
        form.add(new JLabel("Gói:"));        form.add(new JLabel(p.getProductName()));
        form.add(new JLabel("Lãi suất:"));   form.add(new JLabel(p.getInterestRate().multiply(BigDecimal.valueOf(100)).toPlainString() + "%/năm"));
        form.add(new JLabel("Số tiền (đ):")); JTextField txtAmount = new JTextField("0"); form.add(txtAmount);

        int opt = JOptionPane.showConfirmDialog(this, form,
            "Mua gói — " + p.getProductName(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;

        BigDecimal amount;
        try {
            String raw = txtAmount.getText().trim().replace(",", "").replace(".", "");
            amount = new BigDecimal(raw);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Số tiền không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validate min/max client-side trước khi gọi OTP
        if (amount.compareTo(p.getMinInvestmentAmount()) < 0) {
            JOptionPane.showMessageDialog(this,
                "Số tiền tối thiểu là " + minStr + " đ", "Không đủ tối thiểu", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (p.getMaxInvestmentAmount() != null && amount.compareTo(p.getMaxInvestmentAmount()) > 0) {
            JOptionPane.showMessageDialog(this,
                "Số tiền tối đa là " + maxStr + " đ", "Vượt tối đa", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Kiểm tra số dư ví trước OTP
        Wallet wallet = walletDAO.getByUserId(userId);
        if (wallet == null || wallet.getAvailableBalance().compareTo(amount) < 0) {
            JOptionPane.showMessageDialog(this,
                "Số dư ví không đủ! Hiện tại: " +
                (wallet != null ? VND.format(wallet.getAvailableBalance()) : "0") + " đ",
                "Số dư không đủ", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // ── OTP ────────────────────────────────────────────────────────────
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        OtpDialog otp = new OtpDialog(parentFrame, "Mua gói " + p.getProductName());
        String code = otp.showAndGetOtp();
        if (code == null) return;   // người dùng hủy

        // ── Gọi controller ─────────────────────────────────────────────────
        final BigDecimal finalAmount = amount;
        btnRefresh.setEnabled(false);
        new SwingWorker<InvestmentController.BuyResult, Void>() {
            @Override protected InvestmentController.BuyResult doInBackground() {
                return controller.buyProduct(userId, p.getProductId(), finalAmount);
            }
            @Override protected void done() {
                try {
                    InvestmentController.BuyResult result = get();
                    if (result == InvestmentController.BuyResult.SUCCESS) {
                        JOptionPane.showMessageDialog(SavingsProductListPanel.this,
                            "Mua gói thành công!\nSố tiền: " + VND.format(finalAmount) + " đ\nGói: " + p.getProductName(),
                            "Thành công ✅", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        String msg = switch (result) {
                            case KYC_NOT_APPROVED     -> "Tài khoản chưa được xác minh eKYC.";
                            case INSUFFICIENT_BALANCE -> "Số dư ví không đủ.";
                            case AMOUNT_BELOW_MIN     -> "Số tiền dưới mức tối thiểu " + minStr + " đ.";
                            case AMOUNT_ABOVE_MAX     -> "Số tiền vượt mức tối đa " + maxStr + " đ.";
                            case PRODUCT_NOT_FOUND    -> "Không tìm thấy gói đầu tư.";
                            case PRODUCT_NOT_OPEN     -> "Gói không trong thời gian mở bán.";
                            case DB_ERROR             -> "Lỗi hệ thống khi xử lý giao dịch.";
                            default                    -> "Lỗi không xác định: " + result;
                        };
                        JOptionPane.showMessageDialog(SavingsProductListPanel.this,
                            msg, "Mua thất bại", JOptionPane.ERROR_MESSAGE);
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

    private static class ProductTableModel extends AbstractTableModel {
        private static final String[] COLS = {"Tên gói", "Kỳ hạn", "Lãi suất", "Tối thiểu", "Tối đa", "Trạng thái", "Mua"};
        private List<SavingsProduct> data = new java.util.ArrayList<>();

        void setProducts(List<SavingsProduct> list) { this.data = list; fireTableDataChanged(); }

        @Override public int getRowCount()    { return data.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }
        @Override public boolean isCellEditable(int r, int c) { return c == 6; }

        @Override
        public Object getValueAt(int r, int c) {
            SavingsProduct p = data.get(r);
            return switch (c) {
                case 0 -> p;                          // renderer lấy obj để vẽ badge
                case 1 -> p.getTerm() == 0 ? "Không kỳ hạn" : p.getTerm() + " ngày";
                case 2 -> p.getInterestRate().multiply(BigDecimal.valueOf(100)).toPlainString() + "%/năm";
                case 3 -> VND.format(p.getMinInvestmentAmount()) + " đ";
                case 4 -> p.getMaxInvestmentAmount() != null ? VND.format(p.getMaxInvestmentAmount()) + " đ" : "—";
                case 5 -> p.isOpenToday() ? "Đang mở" : "Chưa mở";
                case 6 -> "Mua";
                default -> "";
            };
        }
    }

    // =========================================================================
    //  Renderers & Editors
    // =========================================================================

    private class NameRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean foc, int row, int col) {
            JPanel cell = new JPanel(new BorderLayout(6, 0));
            cell.setOpaque(true);
            cell.setBackground(sel ? new Color(210, 230, 255) : CARD_BG);
            cell.setBorder(new EmptyBorder(0, 8, 0, 8));

            if (val instanceof SavingsProduct p) {
                JLabel name = new JLabel(p.getProductName());
                name.setFont(new Font("Segoe UI", Font.BOLD, 13));
                name.setForeground(TEXT_DARK);
                cell.add(name, BorderLayout.CENTER);

                if (p.hasDateWindow() && p.isOpenToday()) {
                    JLabel badge = new JLabel("Chỉ hôm nay");
                    badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
                    badge.setForeground(Color.WHITE);
                    badge.setBackground(ORANGE);
                    badge.setOpaque(true);
                    badge.setBorder(new EmptyBorder(2, 6, 2, 6));
                    cell.add(badge, BorderLayout.EAST);
                }
            }
            return cell;
        }
    }

    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean foc, int row, int col) {
            JLabel lbl = new JLabel(String.valueOf(val));
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setOpaque(true);
            boolean open = "Đang mở".equals(val);
            lbl.setForeground(open ? GREEN : new Color(150, 155, 170));
            lbl.setBackground(sel ? new Color(210, 230, 255) : CARD_BG);
            return lbl;
        }
    }

    private static class BuyButtonRenderer extends JButton implements TableCellRenderer {
        BuyButtonRenderer() {
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(true);
        }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean foc, int row, int col) {
            // Check if product is open
            Object nameObj = t.getValueAt(row, 0);
            boolean open = true;
            if (nameObj instanceof SavingsProduct p) open = p.isOpenToday();

            setText("Mua");
            setEnabled(open);
            setForeground(Color.WHITE);
            setBackground(open ? BLUE : new Color(180, 185, 200));
            return this;
        }
    }

    private class BuyButtonEditor extends DefaultCellEditor {
        private int currentRow;
        private final JButton btn;

        BuyButtonEditor() {
            super(new JCheckBox());
            btn = new JButton("Mua");
            btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btn.setForeground(Color.WHITE);
            btn.setBackground(BLUE);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.addActionListener(e -> {
                fireEditingStopped();
                onBuyClicked(currentRow);
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object val,
                boolean sel, int row, int col) {
            currentRow = row;
            Object nameObj = t.getValueAt(row, 0);
            boolean open = !(nameObj instanceof SavingsProduct p) || p.isOpenToday();
            btn.setEnabled(open);
            btn.setBackground(open ? BLUE : new Color(180, 185, 200));
            return btn;
        }

        @Override public Object getCellEditorValue() { return "Mua"; }
    }

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
