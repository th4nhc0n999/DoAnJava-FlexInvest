package View.permission;

import DAO.SysFunctionDAO;
import Model.SysFunction;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

/** CRUD cho bảng SYS_FUNCTION */
public class SysFunctionPanel extends JPanel {

    private static final Color NAVY    = new Color(15,  40,  80);
    private static final Color BLUE    = new Color(0,  162, 232);
    private static final Color BG      = new Color(238, 243, 250);
    private static final Color CARD_BG = new Color(245, 248, 252);
    private static final Color ROW_ALT = new Color(235, 243, 253);

    private final SysFunctionDAO dao = new SysFunctionDAO();

    private JTable            table;
    private DefaultTableModel tableModel;
    private List<SysFunction> functions;

    public SysFunctionPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);
        add(buildHeader(),  BorderLayout.NORTH);
        add(buildTable(),   BorderLayout.CENTER);
        add(buildToolbar(), BorderLayout.SOUTH);
        loadData();
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(NAVY);
        p.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel title = new JLabel("Quản lý Chức năng (SYS_FUNCTION)");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Khai báo các màn hình / tính năng để gán quyền");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(180, 200, 230));
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        text.add(title); text.add(sub);
        p.add(text, BorderLayout.WEST);
        return p;
    }

    private JScrollPane buildTable() {
        String[] cols = {"ID", "Tên chức năng", "Ngày tạo", "Ngày cập nhật"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(32);
        table.setGridColor(new Color(220, 228, 240));
        table.setSelectionBackground(new Color(198, 225, 255));
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(210, 225, 245));
        table.getTableHeader().setForeground(NAVY);
        table.getTableHeader().setReorderingAllowed(false);
        int[] widths = {60, 300, 150, 150};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) c.setBackground(row % 2 == 0 ? Color.WHITE : ROW_ALT);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return c;
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new EmptyBorder(8, 12, 0, 12));
        return scroll;
    }

    private JPanel buildToolbar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(4, 12, 8, 12));
        JButton btnAdd    = btn("+ Thêm mới",  new Color(0, 160, 100));
        JButton btnEdit   = btn("Chỉnh sửa",   BLUE);
        JButton btnDel    = btn("Xóa",         new Color(200, 50, 50));
        JButton btnRefresh= btn("Làm mới",     new Color(110, 110, 120));
        btnAdd.addActionListener(e -> openDialog(null));
        btnEdit.addActionListener(e -> { SysFunction f = selected(); if (f != null) openDialog(f); });
        btnDel.addActionListener(e -> deleteSelected());
        btnRefresh.addActionListener(e -> loadData());
        p.add(btnAdd); p.add(btnEdit); p.add(btnDel); p.add(btnRefresh);
        return p;
    }

    private void loadData() {
        functions = dao.getAll();
        tableModel.setRowCount(0);
        for (SysFunction f : functions) {
            tableModel.addRow(new Object[]{
                f.getFunctionId(),
                f.getNameFunction(),
                f.getCreatedAt() != null ? f.getCreatedAt().toString() : "",
                f.getUpdatedAt() != null ? f.getUpdatedAt().toString() : ""
            });
        }
    }

    private SysFunction selected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Chọn một dòng.", "Thông báo", JOptionPane.INFORMATION_MESSAGE); return null; }
        return functions.get(row);
    }

    private void openDialog(SysFunction existing) {
        boolean isNew = existing == null;
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                isNew ? "Thêm chức năng" : "Sửa chức năng", true);
        dlg.setSize(380, 180);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CARD_BG);
        form.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 4, 6, 4);
        g.fill   = GridBagConstraints.HORIZONTAL;

        JTextField txtName = new JTextField(existing != null ? existing.getNameFunction() : "", 25);
        txtName.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        g.gridx = 0; g.gridy = 0; g.weightx = 0; form.add(lbl("Tên chức năng *"), g);
        g.gridx = 1; g.weightx = 1; form.add(txtName, g);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btns.setBackground(CARD_BG);
        JButton save = btn("Lưu", BLUE), cancel = btn("Hủy", new Color(150, 150, 160));
        btns.add(cancel); btns.add(save);
        cancel.addActionListener(e -> dlg.dispose());
        save.addActionListener(e -> {
            String name = txtName.getText().trim();
            if (name.isEmpty()) { JOptionPane.showMessageDialog(dlg, "Tên không được trống.", "Lỗi", JOptionPane.ERROR_MESSAGE); return; }
            boolean ok = isNew ? dao.insert(new SysFunction(name))
                               : dao.update(new SysFunction(existing.getFunctionId(), name, null, null));
            if (ok) { loadData(); dlg.dispose(); }
            else JOptionPane.showMessageDialog(dlg, "Thao tác thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        });
        dlg.add(form, BorderLayout.CENTER);
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void deleteSelected() {
        SysFunction f = selected();
        if (f == null) return;
        int c = JOptionPane.showConfirmDialog(this,
                "Xóa chức năng \"" + f.getNameFunction() + "\"?\n(Sẽ thất bại nếu có SysRole đang dùng)",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (c == JOptionPane.YES_OPTION) {
            if (dao.softDelete(f.getFunctionId())) loadData();
            else JOptionPane.showMessageDialog(this, "Xóa thất bại — có thể đang được tham chiếu.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JButton btn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(120, 34));
        return b;
    }

    private JLabel lbl(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return l;
    }
}
