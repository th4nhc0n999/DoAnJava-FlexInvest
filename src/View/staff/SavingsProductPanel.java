package View.staff;

import Controller.SavingsProductController;
import Model.AccountModel;
import Model.SavingsProduct;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class SavingsProductPanel extends JPanel {

    private static final Color BG = new Color(238, 243, 250);
    private static final Color NAVY = new Color(15, 40, 80);

    private final AccountModel account;
    private final SavingsProductController spController = new SavingsProductController();

    private DefaultTableModel tableModel;
    private JTable table;
    private List<SavingsProduct> products;

    public SavingsProductPanel(AccountModel account) {
        this.account = account;
        setLayout(new BorderLayout());
        setBackground(BG);
        build();
        loadData();
    }

    private void build() {
        JPanel inner = new JPanel(new BorderLayout(0, 16));
        inner.setBackground(BG);
        inner.setBorder(new EmptyBorder(24, 28, 24, 28));
        inner.setOpaque(true);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Quản lý Gói Đầu Tư/Tiết Kiệm");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        header.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton btnAdd = new JButton("+ Thêm Gói Mới");
        btnAdd.setBackground(NAVY);
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFocusPainted(false);
        btnAdd.addActionListener(e -> showProductDialog(null));

        JButton btnEdit = new JButton("✎ Sửa");
        btnEdit.addActionListener(e -> editSelected());

        JButton btnToggle = new JButton("Bật/Tắt");
        btnToggle.addActionListener(e -> toggleSelected());

        actions.add(btnAdd);
        actions.add(btnEdit);
        actions.add(btnToggle);
        header.add(actions, BorderLayout.EAST);
        inner.add(header, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID", "Tên Gói", "Lãi suất", "Kỳ hạn", "Min/Max", "Loại", "Trạng thái"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(36);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        // VIP marker
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                String type = (String) tableModel.getValueAt(r, 5);
                if (type.contains("VIP")) {
                    comp.setForeground(new Color(212, 175, 55)); // Gold
                    comp.setFont(comp.getFont().deriveFont(Font.BOLD));
                } else {
                    comp.setForeground(s ? t.getSelectionForeground() : t.getForeground());
                    comp.setFont(t.getFont());
                }
                return comp;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        inner.add(scroll, BorderLayout.CENTER);

        add(inner, BorderLayout.CENTER);
    }

    public void loadData() {
        SwingWorker<List<SavingsProduct>, Void> worker = new SwingWorker<>() {
            @Override protected List<SavingsProduct> doInBackground() {
                // FIX: dùng controller đã có sẵn thay vì raw SQL trong View
                return spController.getAllProducts();
            }

            @Override protected void done() {
                try {
                    products = get();
                    tableModel.setRowCount(0);
                    for (SavingsProduct p : products) {
                        String minMax = String.format("%,.0f - %s",
                            p.getMinInvestmentAmount(),
                            p.getMaxInvestmentAmount() != null
                                ? String.format("%,.0f", p.getMaxInvestmentAmount()) : "∞");
                        String type = "FlexToken".equalsIgnoreCase(p.getCurrency()) ? "VIP (FlexToken)" : "Thường";
                        tableModel.addRow(new Object[]{
                            p.getProductId(), p.getProductName(),
                            p.getInterestRate() + "%",
                            p.getTerm() == 0 ? "Flex-Safe" : p.getTerm() + " ngày",
                            minMax, type, p.getStatus()
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void editSelected() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một gói để sửa.");
            return;
        }
        showProductDialog(products.get(r));
    }

    private void toggleSelected() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một gói để bật/tắt.");
            return;
        }
        SavingsProduct p = products.get(r);
        // FIX: toggleActive nhận boolean (true = ACTIVE), không phải String
        boolean currentlyActive = "ACTIVE".equalsIgnoreCase(p.getStatus());
        if (spController.toggleActive(p.getProductId(), !currentlyActive)) {
            loadData();
        } else {
            JOptionPane.showMessageDialog(this, "Lỗi khi cập nhật trạng thái (có thể gói đang có người dùng ACTIVE).");
        }
    }

    private void showProductDialog(SavingsProduct p) {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), p == null ? "Thêm Gói Mới" : "Sửa Gói", true);
        d.setLayout(new BorderLayout());
        
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBorder(new EmptyBorder(20, 20, 20, 20));

        JTextField txtName = new JTextField(p != null ? p.getProductName() : "");
        JTextField txtRate = new JTextField(p != null ? p.getInterestRate().toString() : "0.0");
        JTextField txtTerm = new JTextField(p != null ? String.valueOf(p.getTerm()) : "0");
        JTextField txtMin = new JTextField(p != null ? p.getMinInvestmentAmount().toString() : "100000");
        JTextField txtMax = new JTextField(p != null && p.getMaxInvestmentAmount() != null ? p.getMaxInvestmentAmount().toString() : "");
        JComboBox<String> cbCurrency = new JComboBox<>(new String[]{"VNĐ", "FlexToken"});
        if (p != null && "FlexToken".equalsIgnoreCase(p.getCurrency())) cbCurrency.setSelectedIndex(1);

        form.add(new JLabel("Tên Gói:")); form.add(txtName);
        form.add(new JLabel("Lãi suất (%/năm):")); form.add(txtRate);
        form.add(new JLabel("Kỳ hạn (ngày, 0=Flex-Safe):")); form.add(txtTerm);
        form.add(new JLabel("Tối thiểu:")); form.add(txtMin);
        form.add(new JLabel("Tối đa (để trống = vô hạn):")); form.add(txtMax);
        form.add(new JLabel("Loại tiền (FlexToken = VIP):")); form.add(cbCurrency);

        JButton btnSave = new JButton("Lưu");
        btnSave.addActionListener(e -> {
            try {
                SavingsProduct np = p == null ? new SavingsProduct() : p;
                np.setProductName(txtName.getText().trim());
                np.setInterestRate(new BigDecimal(txtRate.getText().trim()));
                np.setTerm(Integer.parseInt(txtTerm.getText().trim()));
                np.setMinInvestmentAmount(new BigDecimal(txtMin.getText().trim()));
                String maxStr = txtMax.getText().trim();
                np.setMaxInvestmentAmount(maxStr.isEmpty() ? null : new BigDecimal(maxStr));
                np.setCurrency((String) cbCurrency.getSelectedItem());
                
                // Defaults for missing fields in simple form
                if (p == null) {
                    np.setPenaltyRate(BigDecimal.ZERO);
                    np.setFallbackInterestRate(BigDecimal.ZERO);
                    np.setMinHoldingDays(0);
                    np.setStatus("ACTIVE");
                }

                boolean ok = p == null
                    ? spController.createProduct(np) > 0
                    : spController.updateProduct(np);
                if (ok) {
                    loadData();
                    d.dispose();
                } else {
                    JOptionPane.showMessageDialog(d, "Lỗi khi lưu (có thể gói đang có người dùng ACTIVE).", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(d, "Dữ liệu không hợp lệ: " + ex.getMessage());
            }
        });

        d.add(form, BorderLayout.CENTER);
        JPanel bottom = new JPanel();
        bottom.add(btnSave);
        d.add(bottom, BorderLayout.SOUTH);
        d.pack();
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }
}
