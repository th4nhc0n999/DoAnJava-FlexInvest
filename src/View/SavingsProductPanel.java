package View;

import Controller.SavingsProductController;
import Model.SavingsProduct;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel quản lý danh sách gói sản phẩm tích lũy.
 *
 * Chức năng:
 *  - Bảng danh sách tất cả gói (kể cả INACTIVE)
 *  - Nút Thêm → mở dialog tạo gói mới
 *  - Nút Sửa → mở dialog chỉnh sửa (không cho sửa nếu có Investment ACTIVE)
 *  - Nút Bật/Tắt → toggle trạng thái ACTIVE ↔ INACTIVE
 */
public class SavingsProductPanel extends JPanel {

    private static final Color NAVY    = new Color(15, 40, 80);
    private static final Color BLUE    = new Color(0, 162, 232);
    private static final Color BG      = new Color(238, 243, 250);
    private static final Color CARD_BG = new Color(245, 248, 252);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SavingsProductController controller = new SavingsProductController();

    private JTable table;
    private DefaultTableModel tableModel;
    private List<SavingsProduct> products;

    public SavingsProductPanel() {
        initUI();
        loadData();
    }

    // ── Build UI ─────────────────────────────────────────────────────────────

    private void initUI() {
        setBackground(BG);
        setLayout(new BorderLayout(0, 16));
        setBorder(new EmptyBorder(24, 24, 24, 24));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildToolbar(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JLabel title = new JLabel("Quản lý gói sản phẩm");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(30, 30, 40));

        JButton btnAdd = buildButton("+ Thêm gói", BLUE);
        btnAdd.addActionListener(e -> openProductDialog(null));

        p.add(title, BorderLayout.WEST);
        p.add(btnAdd, BorderLayout.EAST);
        return p;
    }

    private JScrollPane buildTable() {
        String[] cols = {"ID", "Tên gói", "Lãi suất (%)", "Kỳ hạn (ngày)",
                         "Tối thiểu (VNĐ)", "Tối đa (VNĐ)", "Trạng thái", "Hôm nay mở?"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        table.setRowHeight(34);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(NAVY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(220, 228, 240));

        // Renderer màu dòng xen kẽ + highlight INACTIVE
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                String status = (String) tableModel.getValueAt(row, 6);
                if (sel) {
                    setBackground(new Color(200, 220, 255));
                } else if ("INACTIVE".equals(status)) {
                    setBackground(new Color(245, 235, 235));
                } else {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 251, 255));
                }
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        });

        // Cột hẹp
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(7).setMaxWidth(100);

        return new JScrollPane(table);
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);

        JButton btnEdit   = buildButton("✎  Sửa", NAVY);
        JButton btnToggle = buildButton("⏻  Bật/Tắt", new Color(120, 120, 130));

        btnEdit.addActionListener(e -> {
            SavingsProduct p = getSelected();
            if (p != null) openProductDialog(p);
        });

        btnToggle.addActionListener(e -> {
            SavingsProduct p = getSelected();
            if (p == null) return;
            boolean activate = "INACTIVE".equals(p.getStatus());
            String action = activate ? "bật" : "tắt";
            int ok = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn " + action + " gói \"" + p.getProductName() + "\"?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                boolean result = controller.toggleActive(p.getProductId(), activate);
                if (result) {
                    JOptionPane.showMessageDialog(this,
                        "Đã " + action + " gói thành công.", "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);
                    loadData();
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Không thể " + action + " gói — đang có khoản đầu tư ACTIVE.",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        bar.add(btnEdit);
        bar.add(btnToggle);
        return bar;
    }

    // ── Load data ────────────────────────────────────────────────────────────

    private void loadData() {
        products = controller.getAllProducts();
        tableModel.setRowCount(0);
        for (SavingsProduct p : products) {
            tableModel.addRow(new Object[]{
                p.getProductId(),
                p.getProductName(),
                p.getInterestRate().multiply(BigDecimal.valueOf(100)).setScale(2),
                p.getTerm() == 0 ? "Không KH" : String.valueOf(p.getTerm()),
                formatMoney(p.getMinInvestmentAmount()),
                p.getMaxInvestmentAmount() != null ? formatMoney(p.getMaxInvestmentAmount()) : "—",
                p.getStatus(),
                controller.isOpenToday(p) ? "✔ Mở" : "✘ Đóng"
            });
        }
    }

    // ── Dialog Thêm/Sửa ──────────────────────────────────────────────────────

    private void openProductDialog(SavingsProduct existing) {
        boolean isNew = (existing == null);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                isNew ? "Thêm gói mới" : "Sửa gói", true);
        dialog.setSize(480, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        // Form fields
        JTextField tfName        = new JTextField(20);
        JTextField tfRate        = new JTextField(10);
        JTextField tfTerm        = new JTextField(10);
        JTextField tfMin         = new JTextField(15);
        JTextField tfMax         = new JTextField(15);
        JTextField tfPenalty     = new JTextField(10);
        JTextField tfFallback    = new JTextField(10);
        JTextField tfMinHold     = new JTextField(10);
        JTextField tfStartDate   = new JTextField(12);
        JTextField tfEndDate     = new JTextField(12);

        if (!isNew) {
            tfName.setText(existing.getProductName());
            tfRate.setText(existing.getInterestRate().toPlainString());
            tfTerm.setText(String.valueOf(existing.getTerm()));
            tfMin.setText(existing.getMinInvestmentAmount().toPlainString());
            tfMax.setText(existing.getMaxInvestmentAmount() != null
                    ? existing.getMaxInvestmentAmount().toPlainString() : "");
            tfPenalty.setText(existing.getPenaltyRate() != null
                    ? existing.getPenaltyRate().toPlainString() : "0");
            tfFallback.setText(existing.getFallbackInterestRate() != null
                    ? existing.getFallbackInterestRate().toPlainString() : "0");
            tfMinHold.setText(String.valueOf(existing.getMinHoldingDays()));
            tfStartDate.setText(existing.getStartDate() != null
                    ? existing.getStartDate().format(DATE_FMT) : "");
            tfEndDate.setText(existing.getEndDate() != null
                    ? existing.getEndDate().format(DATE_FMT) : "");
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 20, 8, 20));
        form.setBackground(Color.WHITE);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.WEST;

        Object[][] rows = {
            {"Tên gói *",             tfName},
            {"Lãi suất (0.05 = 5%) *",tfRate},
            {"Kỳ hạn (ngày, 0=FlexSafe)*", tfTerm},
            {"Số tiền tối thiểu *",   tfMin},
            {"Số tiền tối đa",        tfMax},
            {"Phạt rút sớm",          tfPenalty},
            {"Lãi dự phòng",          tfFallback},
            {"Ngày giữ tối thiểu",    tfMinHold},
            {"Ngày mở (dd/MM/yyyy)",  tfStartDate},
            {"Ngày đóng (dd/MM/yyyy)",tfEndDate},
        };

        for (int i = 0; i < rows.length; i++) {
            c.gridx = 0; c.gridy = i;
            form.add(new JLabel((String) rows[i][0]), c);
            c.gridx = 1;
            form.add((JComponent) rows[i][1], c);
        }

        JButton btnSave = buildButton(isNew ? "Tạo gói" : "Lưu thay đổi", BLUE);
        btnSave.addActionListener(e -> {
            try {
                SavingsProduct p = isNew ? new SavingsProduct() : existing;
                p.setProductName(tfName.getText().trim());
                p.setInterestRate(new BigDecimal(tfRate.getText().trim()));
                p.setTerm(Integer.parseInt(tfTerm.getText().trim()));
                p.setMinInvestmentAmount(new BigDecimal(tfMin.getText().trim()));
                p.setMaxInvestmentAmount(tfMax.getText().isBlank() ? null
                        : new BigDecimal(tfMax.getText().trim()));
                p.setPenaltyRate(tfPenalty.getText().isBlank() ? BigDecimal.ZERO
                        : new BigDecimal(tfPenalty.getText().trim()));
                p.setFallbackInterestRate(tfFallback.getText().isBlank() ? BigDecimal.ZERO
                        : new BigDecimal(tfFallback.getText().trim()));
                p.setMinHoldingDays(tfMinHold.getText().isBlank() ? 0
                        : Integer.parseInt(tfMinHold.getText().trim()));
                p.setStartDate(tfStartDate.getText().isBlank() ? null
                        : LocalDate.parse(tfStartDate.getText().trim(), DATE_FMT));
                p.setEndDate(tfEndDate.getText().isBlank() ? null
                        : LocalDate.parse(tfEndDate.getText().trim(), DATE_FMT));
                p.setCurrency("VND");

                boolean ok;
                if (isNew) {
                    ok = controller.createProduct(p) > 0;
                } else {
                    ok = controller.updateProduct(p);
                }

                if (ok) {
                    JOptionPane.showMessageDialog(dialog,
                            isNew ? "Tạo gói thành công!" : "Cập nhật thành công!",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    loadData();
                } else {
                    JOptionPane.showMessageDialog(dialog,
                            "Thao tác thất bại. Kiểm tra lại dữ liệu.",
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Dữ liệu không hợp lệ: " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(Color.WHITE);
        footer.add(btnSave);

        dialog.add(new JScrollPane(form), BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private SavingsProduct getSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một gói.",
                    "Chưa chọn", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return products.get(row);
    }

    private JButton buildButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        return btn;
    }

    private String formatMoney(BigDecimal val) {
        if (val == null) return "—";
        return String.format("%,.0f", val.doubleValue());
    }
}
