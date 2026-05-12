package View;

import Model.AccountModel;
import Utils.SessionManager;
import View.permission.PermissionManagementView;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class MainPage extends JFrame {

    private final AccountModel currentAccount;

    private static final Color NAVY      = new Color(15,  40,  80);
    private static final Color BLUE      = new Color(0,  162, 232);
    private static final Color SIDEBAR   = new Color(24,  54, 100);
    private static final Color CARD_BG   = new Color(245, 248, 252);
    private static final Color TEXT_DARK = new Color(30,  30,  40);
    private static final Color TEXT_GRAY = new Color(110, 110, 120);

    public MainPage(AccountModel account) {
        this.currentAccount = account;
        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        setTitle("FlexInvest - Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 680);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        setContentPane(root);

        root.add(buildTopBar(),   BorderLayout.NORTH);
        root.add(buildSidebar(),  BorderLayout.WEST);
        root.add(buildContent(),  BorderLayout.CENTER);
    }

    // ── Thanh trên ──────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(NAVY);
        bar.setPreferredSize(new Dimension(1100, 58));
        bar.setBorder(new EmptyBorder(0, 24, 0, 24));

        JLabel logo = new JLabel("FlexInvest");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        logo.setForeground(Color.WHITE);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        right.setOpaque(false);

        String role = currentAccount.user().getRoleId() == 1 ? "Admin" : "Thành viên";
        JLabel lblUser = new JLabel(currentAccount.account().getUsername() + "  |  " + role);
        lblUser.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblUser.setForeground(new Color(200, 215, 235));

        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setBackground(new Color(220, 60, 60));
        btnLogout.setBorderPainted(false);
        btnLogout.setFocusPainted(false);
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogout.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnLogout.setBackground(new Color(190, 40, 40)); }
            public void mouseExited(MouseEvent e)  { btnLogout.setBackground(new Color(220, 60, 60)); }
        });
        btnLogout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn đăng xuất?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                SessionManager.logout();
                new LoginForm();
                dispose();
            }
        });

        right.add(lblUser);
        right.add(btnLogout);

        bar.add(logo, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ── Sidebar ─────────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setBackground(SIDEBAR);
        side.setPreferredSize(new Dimension(200, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(20, 0, 20, 0));

        String[] menus = {"Dashboard", "Danh mục", "Giao dịch", "Báo cáo", "Cài đặt"};
        for (int i = 0; i < menus.length; i++) {
            side.add(buildMenuItem(menus[i], i == 0, null));
        }

        if (currentAccount.user().getRoleId() == 1) {
            side.add(Box.createVerticalStrut(8));
            side.add(buildSeparator());
            side.add(buildMenuItem("Phân Quyền", false, e -> new PermissionManagementView()));
        }

        return side;
    }

    private JPanel buildSeparator() {
        JPanel sep = new JPanel();
        sep.setBackground(new Color(40, 70, 130));
        sep.setMaximumSize(new Dimension(180, 1));
        sep.setAlignmentX(LEFT_ALIGNMENT);
        return sep;
    }

    private JPanel buildMenuItem(String title, boolean active, java.awt.event.ActionListener onClick) {
        JPanel item = new JPanel(new BorderLayout());
        item.setMaximumSize(new Dimension(200, 46));
        item.setBackground(active ? BLUE : SIDEBAR);
        item.setBorder(new EmptyBorder(0, 20, 0, 0));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 13));
        lbl.setForeground(active ? Color.WHITE : new Color(180, 200, 225));

        item.add(lbl, BorderLayout.CENTER);

        if (!active) {
            item.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { item.setBackground(new Color(35, 70, 120)); }
                public void mouseExited(MouseEvent e)  { item.setBackground(SIDEBAR); }
                public void mouseClicked(MouseEvent e) {
                    if (onClick != null) onClick.actionPerformed(null);
                }
            });
        }

        return item;
    }

    // ── Nội dung chính ──────────────────────────────────────────────────────
    private JPanel buildContent() {
        JPanel content = new JPanel();
        content.setBackground(new Color(238, 243, 250));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(28, 28, 28, 28));

        // Welcome
        JLabel welcome = new JLabel("Xin chào, " + currentAccount.account().getUsername() + "!");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 22));
        welcome.setForeground(TEXT_DARK);
        welcome.setAlignmentX(LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Đây là tổng quan tài khoản của bạn.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(TEXT_GRAY);
        sub.setAlignmentX(LEFT_ALIGNMENT);

        content.add(welcome);
        content.add(Box.createVerticalStrut(4));
        content.add(sub);
        content.add(Box.createVerticalStrut(24));

        // Hàng cards thông tin
        JPanel cardRow = new JPanel(new GridLayout(1, 3, 16, 0));
        cardRow.setOpaque(false);
        cardRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        cardRow.setAlignmentX(LEFT_ALIGNMENT);
        cardRow.add(buildInfoCard("Trạng thái",  currentAccount.user().getStatus(),       new Color(0, 180, 120)));
        cardRow.add(buildInfoCard("Email",        currentAccount.user().getEmail(),        BLUE));
        cardRow.add(buildInfoCard("Thành viên từ",
                currentAccount.user().getCreatedAt() != null
                    ? currentAccount.user().getCreatedAt().toString().substring(0, 10)
                    : "—",
                new Color(160, 100, 220)));
        content.add(cardRow);
        content.add(Box.createVerticalStrut(24));

        // Khu vực placeholder
        JPanel placeholder = new JPanel(new BorderLayout());
        placeholder.setBackground(CARD_BG);
        placeholder.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 220, 235), 1, true),
            new EmptyBorder(32, 32, 32, 32)));
        placeholder.setAlignmentX(LEFT_ALIGNMENT);
        placeholder.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel ph = new JLabel("Nội dung sẽ được bổ sung...", SwingConstants.CENTER);
        ph.setFont(new Font("Segoe UI", Font.ITALIC, 15));
        ph.setForeground(new Color(180, 185, 200));
        placeholder.add(ph, BorderLayout.CENTER);

        content.add(placeholder);
        return content;
    }

    private JPanel buildInfoCard(String title, String value, Color accent) {
        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
            new EmptyBorder(16, 16, 16, 16)));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTitle.setForeground(TEXT_GRAY);
        lblTitle.setAlignmentX(LEFT_ALIGNMENT);

        JLabel lblValue = new JLabel(value != null ? value : "—");
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblValue.setForeground(TEXT_DARK);
        lblValue.setAlignmentX(LEFT_ALIGNMENT);

        card.add(lblTitle);
        card.add(Box.createVerticalStrut(8));
        card.add(lblValue);
        return card;
    }
}
