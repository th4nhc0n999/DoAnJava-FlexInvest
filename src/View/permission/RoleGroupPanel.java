package View.permission;

import DAO.RoleGroupDAO;
import DAO.SysRoleDAO;
import Model.RoleGroup;
import Model.SysRole;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Quản lý ROLE_GROUP và gán SysRole vào nhóm.
 * Trái: danh sách nhóm. Phải: danh sách SysRole với checkbox.
 */
public class RoleGroupPanel extends JPanel {

    private static final Color NAVY    = new Color(15,  40,  80);
    private static final Color BLUE    = new Color(0,  162, 232);
    private static final Color BG      = new Color(238, 243, 250);
    private static final Color ROW_ALT = new Color(235, 243, 253);

    private final RoleGroupDAO rgDAO   = new RoleGroupDAO();
    private final SysRoleDAO   roleDAO = new SysRoleDAO();

    private JTable            groupTable;
    private DefaultTableModel groupModel;
    private List<RoleGroup>   groups;

    private JLabel             lblGroupName;
    private JPanel             checkPanel;
    private final List<JCheckBox> checkboxes = new ArrayList<>();
    private List<SysRole>      allSysRoles;

    public RoleGroupPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);
        add(buildHeader(), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildGroupPanel(), buildAssignPanel());
        split.setDividerLocation(300);
        split.setDividerSize(6);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
        allSysRoles = roleDAO.getAll();
        loadGroups();
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(NAVY);
        p.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel title = new JLabel("Quản lý Nhóm Vai trò (ROLE_GROUP)");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Tạo nhóm, gán SysRole vào nhóm để dễ gán cho tài khoản");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(180, 200, 230));
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        text.add(title); text.add(sub);
        p.add(text, BorderLayout.WEST);
        return p;
    }

    // ── Panel trái: danh sách ROLE_GROUP ─────────────────────────────────────
    private JPanel buildGroupPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(210, 220, 235)));

        JLabel hdr = new JLabel("  Nhóm vai trò");
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 13));
        hdr.setOpaque(true);
        hdr.setBackground(new Color(210, 225, 245));
        hdr.setForeground(NAVY);
        hdr.setPreferredSize(new Dimension(0, 36));

        String[] cols = {"ID", "Tên nhóm"};
        groupModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        groupTable = new JTable(groupModel);
        groupTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        groupTable.setRowHeight(32);
        groupTable.setGridColor(new Color(220, 228, 240));
        groupTable.setSelectionBackground(new Color(198, 225, 255));
        groupTable.setFillsViewportHeight(true);
        groupTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        groupTable.getTableHeader().setBackground(new Color(210, 225, 245));
        groupTable.getTableHeader().setForeground(NAVY);
        groupTable.getTableHeader().setReorderingAllowed(false);
        groupTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        groupTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        groupTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) c.setBackground(row % 2 == 0 ? Color.WHITE : ROW_ALT);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return c;
            }
        });
        groupTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadRolesForSelectedGroup();
        });

        // Toolbar nhóm
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(Color.WHITE);
        JButton btnAdd = btnSmall("+ Thêm", new Color(0, 160, 100));
        JButton btnEdit = btnSmall("Sửa", BLUE);
        JButton btnDel  = btnSmall("Xóa", new Color(200, 50, 50));
        btnAdd.addActionListener(e -> openGroupDialog(null));
        btnEdit.addActionListener(e -> { RoleGroup g = selectedGroup(); if (g != null) openGroupDialog(g); });
        btnDel.addActionListener(e -> deleteGroup());
        toolbar.add(btnAdd); toolbar.add(btnEdit); toolbar.add(btnDel);

        p.add(hdr,                           BorderLayout.NORTH);
        p.add(new JScrollPane(groupTable),   BorderLayout.CENTER);
        p.add(toolbar,                       BorderLayout.SOUTH);
        return p;
    }

    // ── Panel phải: gán SysRole vào nhóm ─────────────────────────────────────
    private JPanel buildAssignPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG);

        lblGroupName = new JLabel("  Chọn nhóm để gán SysRole");
        lblGroupName.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblGroupName.setForeground(NAVY);
        lblGroupName.setOpaque(true);
        lblGroupName.setBackground(new Color(230, 238, 250));
        lblGroupName.setBorder(new EmptyBorder(10, 16, 10, 16));
        lblGroupName.setPreferredSize(new Dimension(0, 42));
        outer.add(lblGroupName, BorderLayout.NORTH);

        checkPanel = new JPanel();
        checkPanel.setLayout(new BoxLayout(checkPanel, BoxLayout.Y_AXIS));
        checkPanel.setBackground(Color.WHITE);
        checkPanel.setBorder(new EmptyBorder(10, 14, 10, 14));
        outer.add(new JScrollPane(checkPanel), BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        toolbar.setBackground(BG);
        toolbar.setBorder(new EmptyBorder(4, 10, 8, 10));
        JButton btnAll   = btnSmall("Chọn tất cả",    new Color(0, 160, 100));
        JButton btnNone  = btnSmall("Bỏ chọn",        new Color(180, 60, 60));
        JButton btnSave  = btn("Lưu gán quyền",       BLUE);
        btnAll.addActionListener(e  -> checkboxes.forEach(cb -> cb.setSelected(true)));
        btnNone.addActionListener(e -> checkboxes.forEach(cb -> cb.setSelected(false)));
        btnSave.addActionListener(e -> saveAssignment());
        toolbar.add(btnAll); toolbar.add(btnNone); toolbar.add(btnSave);
        outer.add(toolbar, BorderLayout.SOUTH);
        return outer;
    }

    // ── Load dữ liệu ─────────────────────────────────────────────────────────
    private void loadGroups() {
        groups = rgDAO.getAll();
        groupModel.setRowCount(0);
        for (RoleGroup rg : groups)
            groupModel.addRow(new Object[]{rg.getRoleGroupId(), rg.getNameRoleGroup()});
    }

    private void loadRolesForSelectedGroup() {
        RoleGroup rg = selectedGroup();
        if (rg == null) return;
        lblGroupName.setText("  Nhóm: " + rg.getNameRoleGroup());
        List<Integer> assigned = rgDAO.getRoleIdsByGroupId(rg.getRoleGroupId());
        Set<Integer> assignedSet = new HashSet<>(assigned);
        rebuildCheckboxes(assignedSet);
    }

    private void rebuildCheckboxes(Set<Integer> assigned) {
        checkboxes.clear();
        checkPanel.removeAll();
        for (SysRole sr : allSysRoles) {
            String label = "Role#" + sr.getRoleId()
                    + "  [" + (sr.getFunctionName() != null ? sr.getFunctionName() : "Fn#" + sr.getFunctionId()) + "]"
                    + "  V=" + sr.getViewPerm() + " A=" + sr.getAddPerm()
                    + " E=" + sr.getEditPerm() + " D=" + sr.getDeletePerm()
                    + " DL=" + sr.getDownloadPerm();
            JCheckBox cb = new JCheckBox(label);
            cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            cb.setBackground(Color.WHITE);
            cb.setSelected(assigned.contains(sr.getRoleId()));
            cb.putClientProperty("roleId", sr.getRoleId());
            cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            cb.setBorder(new EmptyBorder(2, 4, 2, 4));
            checkboxes.add(cb);
            checkPanel.add(cb);
        }
        if (allSysRoles.isEmpty()) {
            JLabel empty = new JLabel("Chưa có SysRole nào. Hãy tạo ở tab Quyền.");
            empty.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            empty.setForeground(new Color(150, 150, 160));
            checkPanel.add(empty);
        }
        checkPanel.revalidate();
        checkPanel.repaint();
    }

    private void saveAssignment() {
        RoleGroup rg = selectedGroup();
        if (rg == null) { JOptionPane.showMessageDialog(this, "Chọn nhóm.", "Thông báo", JOptionPane.INFORMATION_MESSAGE); return; }
        List<Integer> ids = new ArrayList<>();
        for (JCheckBox cb : checkboxes) if (cb.isSelected()) ids.add((Integer) cb.getClientProperty("roleId"));
        if (rgDAO.saveRoles(rg.getRoleGroupId(), ids))
            JOptionPane.showMessageDialog(this, "Đã lưu " + ids.size() + " quyền cho nhóm \"" + rg.getNameRoleGroup() + "\".", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        else JOptionPane.showMessageDialog(this, "Lưu thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    // ── Dialog CRUD nhóm ─────────────────────────────────────────────────────
    private void openGroupDialog(RoleGroup existing) {
        boolean isNew = existing == null;
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                isNew ? "Thêm nhóm" : "Sửa nhóm", true);
        dlg.setSize(360, 160);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(245, 248, 252));
        form.setBorder(new EmptyBorder(16, 20, 8, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 4, 6, 4); g.fill = GridBagConstraints.HORIZONTAL;
        JTextField txtName = new JTextField(existing != null ? existing.getNameRoleGroup() : "", 20);
        txtName.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        g.gridx = 0; g.gridy = 0; g.weightx = 0; form.add(lbl("Tên nhóm *"), g);
        g.gridx = 1; g.weightx = 1; form.add(txtName, g);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btns.setBackground(new Color(245, 248, 252));
        JButton save = btn("Lưu", BLUE), cancel = btn("Hủy", new Color(150, 150, 160));
        btns.add(cancel); btns.add(save);
        cancel.addActionListener(e -> dlg.dispose());
        save.addActionListener(e -> {
            String name = txtName.getText().trim();
            if (name.isEmpty()) { JOptionPane.showMessageDialog(dlg, "Tên không trống.", "Lỗi", JOptionPane.ERROR_MESSAGE); return; }
            boolean ok = isNew ? rgDAO.insert(new RoleGroup(name))
                               : rgDAO.update(new RoleGroup(existing.getRoleGroupId(), name, null, null, 0));
            if (ok) { loadGroups(); dlg.dispose(); }
            else JOptionPane.showMessageDialog(dlg, "Thao tác thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        });
        dlg.add(form, BorderLayout.CENTER);
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void deleteGroup() {
        RoleGroup rg = selectedGroup();
        if (rg == null) return;
        int c = JOptionPane.showConfirmDialog(this, "Xóa nhóm \"" + rg.getNameRoleGroup() + "\"?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (c == JOptionPane.YES_OPTION) {
            if (rgDAO.softDelete(rg.getRoleGroupId())) { loadGroups(); rebuildCheckboxes(Collections.emptySet()); lblGroupName.setText("  Chọn nhóm để gán SysRole"); }
            else JOptionPane.showMessageDialog(this, "Xóa thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private RoleGroup selectedGroup() {
        int row = groupTable.getSelectedRow();
        if (row < 0) return null;
        return groups.get(row);
    }

    private JButton btn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(140, 34));
        return b;
    }

    private JButton btnSmall(String text, Color bg) {
        JButton b = btn(text, bg);
        b.setPreferredSize(new Dimension(90, 28));
        b.setFont(b.getFont().deriveFont(11f));
        return b;
    }

    private JLabel lbl(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return l;
    }
}
