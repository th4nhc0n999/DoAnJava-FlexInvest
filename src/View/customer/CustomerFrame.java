package View.customer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import Model.AccountModel;

import java.awt.*;

/**
 * Main customer window.
 *
 * Navigation sidebar with:
 *   • Tổng quan (CustomerDashboardPanel)
 *   • Gói tiết kiệm (SavingsProductListPanel)
 *   • Lệnh của tôi (MyInvestmentsPanel)
 *
 * Entry point: CustomerFrame.open(userId, username)
 */
public class CustomerFrame extends JFrame {

    private final int userId;
    private final String username;

    // Lazy-created panels (created on first visit, reused after)
    private CustomerDashboardPanel dashboardPanel;
    private SavingsProductListPanel productsPanel;
    private MyInvestmentsPanel investmentsPanel;

    private JPanel contentArea;
    private CardLayout cardLayout;

    // Nav button references to toggle active state
    private JButton[] navButtons;
    private static final String[] NAV_IDS = {"DASHBOARD", "PRODUCTS", "INVESTMENTS"};

    public CustomerFrame(int userId, String username) {
        super("FlexInvest — " + username);
        this.userId = userId;
        this.username = username;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 580));
        setLocationRelativeTo(null);
        buildUI();

        setVisible(true);
    }


    private void buildUI() {
        getContentPane().setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout(0, 0));

        add(buildSidebar(), BorderLayout.WEST);

        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(UITheme.BG_DARK);
        add(contentArea, BorderLayout.CENTER);

        // Navigate to dashboard as first screen
        navigateTo("DASHBOARD");
    }

    // -----------------------------------------------------------------------
    // Sidebar
    // -----------------------------------------------------------------------

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(UITheme.BG_CARD);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(UITheme.BORDER_COLOR);
                g2.drawLine(getWidth()-1, 0, getWidth()-1, getHeight());
                g2.dispose();
            }
        };
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setLayout(new BorderLayout());
        sidebar.setOpaque(false);

        // ── Logo / brand area ──
        JPanel brand = new JPanel(new BorderLayout());
        brand.setOpaque(false);
        brand.setBorder(new EmptyBorder(24, 20, 24, 20));
        JLabel logo = UITheme.label("FlexInvest", UITheme.FONT_TITLE, UITheme.ACCENT_TEAL);
        JLabel tagline = UITheme.label("Đầu tư linh hoạt", UITheme.FONT_SMALL, UITheme.TEXT_MUTED);
        JPanel brandText = new JPanel(new GridLayout(2, 1, 0, 4));
        brandText.setOpaque(false);
        brandText.add(logo);
        brandText.add(tagline);
        brand.add(brandText, BorderLayout.CENTER);
        sidebar.add(brand, BorderLayout.NORTH);

        // ── Nav buttons ──
        JPanel nav = new JPanel();
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setOpaque(false);
        nav.setBorder(new EmptyBorder(8, 12, 8, 12));

        String[][] navItems = {
            {"DASHBOARD",   "🏠  Tổng quan"},
            {"PRODUCTS",    "📊  Gói tiết kiệm"},
            {"INVESTMENTS", "📁  Gói của tôi"},
        };

        navButtons = new JButton[navItems.length];
        for (int i = 0; i < navItems.length; i++) {
            final String id = navItems[i][0];
            JButton btn = buildNavButton(navItems[i][1]);
            btn.addActionListener(e -> navigateTo(id));
            navButtons[i] = btn;
            nav.add(btn);
            nav.add(Box.createVerticalStrut(4));
        }
        sidebar.add(nav, BorderLayout.CENTER);

        // ── Footer: user info + logout ──
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(16, 16, 20, 16));
        JLabel userLbl = UITheme.label("👤 " + username, UITheme.FONT_BODY, UITheme.TEXT_PRIMARY);
        JLabel idLbl = UITheme.label("ID: " + userId, UITheme.FONT_SMALL, UITheme.TEXT_MUTED);
        JPanel userInfo = new JPanel(new GridLayout(2, 1, 0, 2));
        userInfo.setOpaque(false);
        userInfo.add(userLbl);
        userInfo.add(idLbl);
        JButton logoutBtn = UITheme.ghostButton("Đăng xuất");
        logoutBtn.addActionListener(e -> logout());
        footer.add(userInfo, BorderLayout.CENTER);
        footer.add(logoutBtn, BorderLayout.SOUTH);
        sidebar.add(footer, BorderLayout.SOUTH);

        return sidebar;
    }

    private JButton buildNavButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (Boolean.TRUE.equals(getClientProperty("active"))) {
                    g2.setColor(new Color(0x00C4A7, true).darker());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                } else if (getModel().isRollover()) {
                    g2.setColor(UITheme.BG_CARD_HOVER);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(UITheme.FONT_BODY);
        btn.setForeground(UITheme.TEXT_PRIMARY);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 14, 10, 14));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        return btn;
    }

    // -----------------------------------------------------------------------
    // Navigation
    // -----------------------------------------------------------------------

    private void navigateTo(String id) {
        // Update active nav button
        for (int i = 0; i < NAV_IDS.length; i++) {
            navButtons[i].putClientProperty("active", NAV_IDS[i].equals(id));
            navButtons[i].repaint();
        }

        // Lazy-init panels on first visit
        if (!isPanelAdded(id)) {
            JPanel panel = createPanel(id);
            contentArea.add(panel, id);
        }

        cardLayout.show(contentArea, id);

        // Refresh data when switching to investments or dashboard
        if ("INVESTMENTS".equals(id) && investmentsPanel != null) investmentsPanel.refresh();
        if ("DASHBOARD".equals(id)   && dashboardPanel  != null) dashboardPanel.refresh();
    }

    private boolean isPanelAdded(String id) {
        return switch (id) {
            case "DASHBOARD"   -> dashboardPanel != null;
            case "PRODUCTS"    -> productsPanel  != null;
            case "INVESTMENTS" -> investmentsPanel != null;
            default            -> false;
        };
    }

    private JPanel createPanel(String id) {
        return switch (id) {
            case "DASHBOARD" -> {
                dashboardPanel = new CustomerDashboardPanel(userId, navId -> navigateTo(navId));
                yield dashboardPanel;
            }
            case "PRODUCTS" -> {
                productsPanel = new SavingsProductListPanel(userId, v -> {
                    // After purchase: refresh dashboard & navigate to investments
                    if (dashboardPanel  != null) dashboardPanel.refresh();
                    if (investmentsPanel != null) investmentsPanel.refresh();
                    navigateTo("INVESTMENTS");
                });
                yield productsPanel;
            }
            case "INVESTMENTS" -> {
                investmentsPanel = new MyInvestmentsPanel(userId, v -> {
                    if (dashboardPanel != null) dashboardPanel.refresh();
                });
                yield investmentsPanel;
            }
            default -> {
                JPanel p = new JPanel();
                p.setBackground(UITheme.BG_DARK);
                yield p;
            }
        };
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn đăng xuất?", "Đăng xuất",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            // Caller should re-show LoginForm; wire this via callback if needed
        }
    }

    // -----------------------------------------------------------------------
    // Static entry point — called from LoginController after successful login
    // -----------------------------------------------------------------------

    /**
     * Opens the customer dashboard window.
     *
     * @param userId   authenticated user's DB ID
     * @param username display name / email
     */
    public static void open(int userId, String username) {
        SwingUtilities.invokeLater(() -> {
            CustomerFrame frame = new CustomerFrame(userId, username);
            frame.setVisible(true);
        });
    }
}
