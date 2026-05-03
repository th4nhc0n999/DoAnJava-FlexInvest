package View.permission;

import DAO.AccountDAO;
import DAO.AccountPermissionDAO;
import DAO.RoleGroupDAO;
import DAO.SysRoleDAO;
import Model.Account;
import Model.RoleGroup;
import Model.SysRole;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Gán quyền cho tài khoản (ACCOUNT):
 *   - Tab "Trực tiếp": gán SysRole trực tiếp vào account
 *   - Tab "Qua nhóm" : gán RoleGroup vào account
 */
public class AccountPermissionPanel extends JPanel {

    private static final Color NAVY    = new Color(15,  40,  80);
    private static final Color BLUE    = new Color(0,  162, 232);
    private static final Color BG      = new Color(238, 243, 250);
    private static final Color ROW_ALT = new Color(235, 243, 253);

    private final AccountDAO           accountDAO = new AccountDAO();
    private final AccountPermissionDAO permDAO    = new AccountPermissionDAO();
    private final SysRoleDAO           roleDAO    = new SysRoleDAO();
    private final RoleGroupDAO         rgDAO      = new RoleGroupDAO();

    private JTable            accountTable;
    private DefaultTableModel accountModel;
    private JTextField        txtSearch;
    private JLabel            lblAccount;

    private List<Account>  allAccounts     = new ArrayList<>();
    private List<Account>  filteredAccounts = new ArrayList<>();

    // Direct roles
    private JPanel           directCheckPanel;
    private final List<JCheckBox> directCheckboxes = new ArrayList<>();

    // Group roles
    private JPanel           groupCheckPanel;
    private final List<JCheckBox> groupCheckboxes = new ArrayList<>();

    public AccountPermissionPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);
        add(buildHeader(), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildAccountPanel(), buildRightPanel());
        split.setDividerLocation(380);
        split.setDividerSize(6);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
        loadAccounts();
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(NAVY);
        p.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel title = new JLabel("Gán quyền cho Tài khoản (ACCOUNT)");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Gán SysRole trực tiếp hoặc qua RoleGroup cho từng tài khoản");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(180, 200, 230));
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        text.add(title); text.add(sub);
        p.add(text, BorderLayout.WEST);
        return p;
    }

    // ── Panel trái: danh sách Account ────────────────────────────────────────
    private JPanel buildAccountPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(210, 220, 235)));

        JLabel hdr = new JLabel("  Tài khoản");
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 13));
        hdr.setOpaque(true);
        hdr.setBackground(new Color(210, 225, 245));
        hdr.setForeground(NAVY);
        hdr.setPreferredSize(new Dimension(0, 36));

        JPanel searchBar = new JPanel(new BorderLayout(6, 0));
        searchBar.setBackground(Color.WHITE);
        searchBar.setBorder(new EmptyBorder(8, 10, 6, 10));
        txtSearch = new JTextField();
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtSearch.setToolTipText("Tìm theo username");
        JButton btnSearch = new JButton("Tìm");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnSearch.setBackground(BLUE);
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setBorderPainted(false);
        btnSearch.setFocusPainted(false);
        btnSearch.setPreferredSize(new Dimension(50, 28));
        txtSearch.addActionListener(e -> filterAccounts());
        btnSearch.addActionListener(e -> filterAccounts());
        searchBar.add(txtSearch, BorderLayout.CENTER);
        searchBar.add(btnSearch, BorderLayout.EAST);

        String[] cols = {"ID", "Username", "Trạng thái"};
        accountModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        accountTable = new JTable(accountModel);
        accountTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        accountTable.setRowHeight(32);
        accountTable.setGridColor(new Color(220, 228, 240));
        accountTable.setSelectionBackground(new Color(198, 225, 255));
        accountTable.setFillsViewportHeight(true);
        accountTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        accountTable.getTableHeader().setBackground(new Color(210, 225, 245));
        accountTable.getTableHeader().setForeground(NAVY);
        accountTable.getTableHeader().setReorderingAllowed(false);
        accountTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        accountTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        accountTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        accountTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) c.setBackground(row % 2 == 0 ? Color.WHITE : ROW_ALT);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return c;
            }
        });
        accountTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadPermissionsForSelected();
        });

        JPanel north = new JPanel(new BorderLayout());
        north.setBackground(Color.WHITE);
        north.add(hdr,       BorderLayout.NORTH);
        north.add(searchBar, BorderLayout.SOUTH);
        p.add(north,                          BorderLayout.NORTH);
        p.add(new JScrollPane(accountTable),  BorderLayout.CENTER);

        JButton btnRefresh = new JButton("Làm mới");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnRefresh.setFocusPainted(false);
        btnRefresh.addActionListener(e -> { txtSearch.setText(""); loadAccounts(); });
        JPanel bot = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
        bot.setBackground(Color.WHITE);
        bot.add(btnRefresh);
        p.add(bot, BorderLayout.SOUTH);
        return p;
    }

    // ── Panel phải: 2 tab gán quyền ──────────────────────────────────────────
    private JPanel buildRightPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG);

        lblAccount = new JLabel("  Chọn tài khoản để gán quyền");
        lblAccount.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblAccount.setForeground(NAVY);
        lblAccount.setOpaque(true);
        lblAccount.setBackground(new Color(230, 238, 250));
        lblAccount.setBorder(new EmptyBorder(10, 16, 10, 16));
        lblAccount.setPreferredSize(new Dimension(0, 42));
        outer.add(lblAccount, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Tab 1: Direct SysRole
        directCheckPanel = new JPanel();
        directCheckPanel.setLayout(new BoxLayout(directCheckPanel, BoxLayout.Y_AXIS));
        directCheckPanel.setBackground(Color.WHITE);
        directCheckPanel.setBorder(new EmptyBorder(8, 12, 8, 12));
        tabs.addTab("Quyền trực tiếp (SysRole)", new JScrollPane(directCheckPanel));

        // Tab 2: RoleGroup
        groupCheckPanel = new JPanel();
        groupCheckPanel.setLayout(new BoxLayout(groupCheckPanel, BoxLayout.Y_AXIS));
        groupCheckPanel.setBackground(Color.WHITE);
        groupCheckPanel.setBorder(new EmptyBorder(8, 12, 8, 12));
        tabs.addTab("Nhóm vai trò (RoleGroup)", new JScrollPane(groupCheckPanel));

        outer.add(tabs, BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        toolbar.setBackground(BG);
        toolbar.setBorder(new EmptyBorder(4, 10, 8, 10));
        JButton btnSave = btn("Lưu quyền", BLUE);
        btnSave.addActionListener(e -> savePermissions());
        toolbar.add(btnSave);
        outer.add(toolbar, BorderLayout.SOUTH);
        return outer;
    }

    // ── Tải dữ liệu ──────────────────────────────────────────────────────────
    private void loadAccounts() {
        allAccounts = accountDAO.getAll();
        filteredAccounts = new ArrayList<>(allAccounts);
        refreshAccountTable();
    }

    private void filterAccounts() {
        String kw = txtSearch.getText().trim().toLowerCase();
        filteredAccounts = kw.isEmpty() ? new ArrayList<>(allAccounts) : new ArrayList<>();
        if (!kw.isEmpty())
            for (Account a : allAccounts)
                if (a.getUsername().toLowerCase().contains(kw)) filteredAccounts.add(a);
        refreshAccountTable();
        lblAccount.setText("  Chọn tài khoản để gán quyền");
    }

    private void refreshAccountTable() {
        accountModel.setRowCount(0);
        for (Account a : filteredAccounts)
            accountModel.addRow(new Object[]{a.getAccountId(), a.getUsername(), a.getStatus()});
    }

    private void loadPermissionsForSelected() {
        Account acc = selectedAccount();
        if (acc == null) return;
        lblAccount.setText("  Tài khoản: " + acc.getUsername() + "  (ID=" + acc.getAccountId() + ")");

        // Direct SysRoles
        List<Integer> directIds = permDAO.getDirectRoleIdsByAccountId(acc.getAccountId());
        Set<Integer> directSet = new HashSet<>(directIds);
        List<SysRole> allRoles = roleDAO.getAll();
        rebuildCheckboxes(directCheckPanel, directCheckboxes, allRoles, directSet, true);

        // RoleGroups
        List<Integer> groupIds = permDAO.getRoleGroupIdsByAccountId(acc.getAccountId());
        Set<Integer> groupSet = new HashSet<>(groupIds);
        List<RoleGroup> allGroups = rgDAO.getAll();
        rebuildGroupCheckboxes(groupSet, allGroups);
    }

    private void rebuildCheckboxes(JPanel panel, List<JCheckBox> cbs,
                                   List<SysRole> roles, Set<Integer> assigned,
                                   boolean isDirect) {
        cbs.clear(); panel.removeAll();
        for (SysRole sr : roles) {
            String fn = sr.getFunctionName() != null ? sr.getFunctionName() : "Fn#" + sr.getFunctionId();
            JCheckBox cb = new JCheckBox(
                "Role#" + sr.getRoleId() + "  [" + fn + "]"
                + "  V=" + sr.getViewPerm() + " A=" + sr.getAddPerm()
                + " E=" + sr.getEditPerm() + " D=" + sr.getDeletePerm()
                + " DL=" + sr.getDownloadPerm());
            cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            cb.setBackground(Color.WHITE);
            cb.setSelected(assigned.contains(sr.getRoleId()));
            cb.putClientProperty("id", sr.getRoleId());
            cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            cb.setBorder(new EmptyBorder(2, 4, 2, 4));
            cbs.add(cb); panel.add(cb);
        }
        if (roles.isEmpty()) {
            JLabel empty = new JLabel("Chưa có SysRole. Tạo ở tab Quyền.");
            empty.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            empty.setForeground(new Color(150, 150, 160));
            panel.add(empty);
        }
        panel.revalidate(); panel.repaint();
    }

    private void rebuildGroupCheckboxes(Set<Integer> assigned, List<RoleGroup> groups) {
        groupCheckboxes.clear(); groupCheckPanel.removeAll();
        for (RoleGroup rg : groups) {
            JCheckBox cb = new JCheckBox("Nhóm#" + rg.getRoleGroupId() + "  " + rg.getNameRoleGroup());
            cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            cb.setBackground(Color.WHITE);
            cb.setSelected(assigned.contains(rg.getRoleGroupId()));
            cb.putClientProperty("id", rg.getRoleGroupId());
            cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            cb.setBorder(new EmptyBorder(4, 4, 4, 4));
            groupCheckboxes.add(cb); groupCheckPanel.add(cb);
        }
        if (groups.isEmpty()) {
            JLabel empty = new JLabel("Chưa có RoleGroup. Tạo ở tab Nhóm vai trò.");
            empty.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            empty.setForeground(new Color(150, 150, 160));
            groupCheckPanel.add(empty);
        }
        groupCheckPanel.revalidate(); groupCheckPanel.repaint();
    }

    private void savePermissions() {
        Account acc = selectedAccount();
        if (acc == null) { JOptionPane.showMessageDialog(this, "Chọn tài khoản.", "Thông báo", JOptionPane.INFORMATION_MESSAGE); return; }

        List<Integer> directIds = new ArrayList<>();
        for (JCheckBox cb : directCheckboxes) if (cb.isSelected()) directIds.add((Integer) cb.getClientProperty("id"));

        List<Integer> groupIds = new ArrayList<>();
        for (JCheckBox cb : groupCheckboxes) if (cb.isSelected()) groupIds.add((Integer) cb.getClientProperty("id"));

        boolean ok1 = permDAO.saveDirectRoles(acc.getAccountId(), directIds);
        boolean ok2 = permDAO.saveRoleGroups(acc.getAccountId(),  groupIds);

        if (ok1 && ok2)
            JOptionPane.showMessageDialog(this,
                "Đã lưu " + directIds.size() + " quyền trực tiếp + " + groupIds.size() + " nhóm cho \"" + acc.getUsername() + "\".",
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
        else
            JOptionPane.showMessageDialog(this, "Lưu thất bại (một phần hoặc toàn bộ).", "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private Account selectedAccount() {
        int row = accountTable.getSelectedRow();
        return row >= 0 ? filteredAccounts.get(row) : null;
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
}
