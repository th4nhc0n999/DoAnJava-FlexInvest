package View.permission;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Container JFrame cho toàn bộ màn hình quản lý quyền.
 * Đồng bộ với FlexInvest.sql — dùng SYS_FUNCTION, SYS_ROLE, ROLE_GROUP, ACCOUNT.
 */
public class PermissionManagementView extends JFrame {

    private static final Color NAVY = new Color(15, 40, 80);
    private static final Color BLUE = new Color(0, 162, 232);

    public PermissionManagementView() {
        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        setTitle("FlexInvest — Quản lý Phân quyền");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1200, 740);
        setLocationRelativeTo(null);
        setResizable(true);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(NAVY);
        setContentPane(root);

        // ── Top bar ──────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(NAVY);
        topBar.setBorder(new EmptyBorder(0, 20, 0, 20));
        topBar.setPreferredSize(new Dimension(0, 48));

        JLabel logo = new JLabel("FlexInvest  /  Quản lý Phân quyền");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        logo.setForeground(Color.WHITE);
        topBar.add(logo, BorderLayout.WEST);

        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClose.setForeground(Color.WHITE);
        btnClose.setBackground(new Color(180, 50, 50));
        btnClose.setBorderPainted(false);
        btnClose.setFocusPainted(false);
        btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dispose());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 9));
        right.setOpaque(false);
        right.add(btnClose);
        topBar.add(right, BorderLayout.EAST);
        root.add(topBar, BorderLayout.NORTH);

        // ── Tabs ─────────────────────────────────────────────────────────────
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabs.addTab(null, new SysFunctionPanel());
        tabs.addTab(null, new SysRolePanel());
        tabs.addTab(null, new RoleGroupPanel());
        tabs.addTab(null, new AccountPermissionPanel());

        tabs.setTabComponentAt(0, tabLabel("Chức năng",    new Color(0,  160, 100)));
        tabs.setTabComponentAt(1, tabLabel("Quyền",        BLUE));
        tabs.setTabComponentAt(2, tabLabel("Nhóm vai trò", new Color(160, 80, 200)));
        tabs.setTabComponentAt(3, tabLabel("Tài khoản",    new Color(200, 120, 0)));

        root.add(tabs, BorderLayout.CENTER);
    }

    private JLabel tabLabel(String text, Color accent) {
        JLabel l = new JLabel("  " + text + "  ");
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(new Color(40, 50, 80));
        l.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, accent));
        return l;
    }
}
