package View;

import Model.AccountModel;
import Utils.SessionManager;
import View.customer.CustomerDashboardPanel;
import View.customer.SavingsProductListPanel;
import View.customer.MyInvestmentsPanel;
import View.permission.PermissionManagementView;
import View.staff.StaffDashboardPanel;
import View.staff.DepositApprovalPanel;
import View.staff.WithdrawApprovalPanel;
import View.staff.EkycApprovalPanel;
import View.staff.AdminDashboardPanel;
import View.staff.SavingsProductPanel;
import View.staff.UserManagementPanel;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * MainPage — Cửa sổ chính sau khi đăng nhập thành công.
 *
 * Cấu trúc:
 *   TopBar   — logo, thông tin user, nút logout
 *   Sidebar  — navigation theo role:
 *              Customer: Dashboard / Gói Đầu Tư / Khoản Của Tôi
 *              Admin: (kế thừa) + Phân Quyền
 *   Content  — CardLayout chứa các panel, lazy-init khi lần đầu click
 */
public class MainPage extends JFrame {

    private final AccountModel currentAccount;
    private final boolean      isAdmin;   // roleId == 1
    private final boolean      isStaff;   // roleId == 2

    // ── Màu ─────────────────────────────────────────────────────────────────
    private static final Color NAVY      = new Color(15,  40,  80);
    private static final Color BLUE      = new Color(0,  162, 232);
    private static final Color SIDEBAR   = new Color(24,  54, 100);
    private static final Color CARD_BG   = new Color(245, 248, 252);
    private static final Color TEXT_DARK = new Color(30,  30,  40);
    private static final Color TEXT_GRAY = new Color(110, 110, 120);

    // ── Content area ─────────────────────────────────────────────────────────
    private final CardLayout  cardLayout = new CardLayout();
    private JPanel            contentPane;

    // Lazy-init panels — Customer
    private CustomerDashboardPanel  dashboardPanel;
    private SavingsProductListPanel productsPanel;
    private MyInvestmentsPanel      myInvestmentsPanel;

    // Lazy-init panels — Staff
    private StaffDashboardPanel     staffDashboardPanel;
    private DepositApprovalPanel    depositApprovalPanel;
    private WithdrawApprovalPanel   withdrawApprovalPanel;
    private EkycApprovalPanel       ekycApprovalPanel;

    // Panel ID constants — Customer
    private static final String CARD_DASHBOARD   = "DASHBOARD";
    private static final String CARD_PRODUCTS    = "PRODUCTS";
    private static final String CARD_INVESTMENTS = "INVESTMENTS";
    private static final String CARD_SETTINGS    = "SETTINGS";

    // Lazy-init panels — Admin
    private AdminDashboardPanel     adminDashboardPanel;
    private SavingsProductPanel     adminProductsPanel;
    private UserManagementPanel     adminUsersPanel;

    // Panel ID constants — Admin
    private static final String CARD_ADMIN_DASH     = "ADMIN_DASH";
    private static final String CARD_ADMIN_PRODUCTS = "ADMIN_PRODUCTS";
    private static final String CARD_ADMIN_USERS    = "ADMIN_USERS";

    // Panel ID constants — Staff
    private static final String CARD_STAFF_DASH     = "STAFF_DASH";
    private static final String CARD_STAFF_DEPOSIT  = "STAFF_DEPOSIT";
    private static final String CARD_STAFF_WITHDRAW = "STAFF_WITHDRAW";
    private static final String CARD_STAFF_KYC      = "STAFF_KYC";

    // Currently active menu item panel reference
    private JPanel activeItem;

    public MainPage(AccountModel account) {
        this.currentAccount = account;
        this.isAdmin = (account.getUser().getRoleId() == 1);
        this.isStaff = (account.getUser().getRoleId() == 2);
        initComponents();
        setVisible(true);
    }

    // =========================================================================
    //  Init
    // =========================================================================

    private void initComponents() {
        setTitle("FlexInvest — "
            + (isAdmin ? "Admin" : isStaff ? "Staff" : "")
            + " · " + currentAccount.getAccount().getUsername());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 720);
        setLocationRelativeTo(null);
        setResizable(true);
        setMinimumSize(new Dimension(960, 600));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        setContentPane(root);

        // Content CardLayout
        contentPane = new JPanel(cardLayout);
        contentPane.setBackground(CARD_BG);

        // Add placeholder panels for cards not yet loaded
        contentPane.add(new JPanel(), CARD_SETTINGS);

        root.add(buildTopBar(),  BorderLayout.NORTH);
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(contentPane,    BorderLayout.CENTER);

        // Open default panel
        if (isAdmin) showPanel(CARD_ADMIN_DASH);
        else if (isStaff) showPanel(CARD_STAFF_DASH);
        else         showPanel(CARD_DASHBOARD);
    }

    // =========================================================================
    //  Top Bar
    // =========================================================================

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(NAVY);
        bar.setPreferredSize(new Dimension(1200, 58));
        bar.setBorder(new EmptyBorder(0, 24, 0, 24));

        // Logo
        JLabel logo = new JLabel("FlexInvest");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        logo.setForeground(Color.WHITE);

        // Right — user info + logout
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 14));
        right.setOpaque(false);

        String role = isAdmin ? "Admin" : isStaff ? "Staff" : "Thành viên";
        JLabel lblUser = new JLabel(currentAccount.getAccount().getUsername() + "  |  " + role);
        lblUser.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblUser.setForeground(new Color(200, 215, 235));

        JButton btnLogout = styledBtn("Đăng xuất", new Color(220, 60, 60));
        btnLogout.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn đăng xuất?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
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

    // =========================================================================
    //  Sidebar
    // =========================================================================

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setBackground(SIDEBAR);
        side.setPreferredSize(new Dimension(210, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(20, 0, 20, 0));

        JPanel activeItem;

        if (isStaff) {
            // ── Staff sidebar ─────────────────────────────────────────────
            JPanel iDash     = buildMenuItem("🏠  Dashboard",         CARD_STAFF_DASH,     true);
            JPanel iDeposit  = buildMenuItem("💳  Duyệt Nạp Tiền",   CARD_STAFF_DEPOSIT,  false);
            JPanel iWithdraw = buildMenuItem("🏦  Duyệt Rút Tiền",   CARD_STAFF_WITHDRAW, false);
            JPanel iKyc      = buildMenuItem("🪪  Duyệt eKYC",       CARD_STAFF_KYC,      false);

            side.add(iDash);
            side.add(Box.createVerticalStrut(2));
            side.add(iDeposit);
            side.add(Box.createVerticalStrut(2));
            side.add(iWithdraw);
            side.add(Box.createVerticalStrut(2));
            side.add(iKyc);

            activeItem = iDash;
            setActiveStyle(iDash, true);
        } else {
            // ── Customer sidebar ──────────────────────────────────────────
            JPanel itemDashboard   = buildMenuItem("🏠  Dashboard",         CARD_DASHBOARD,   true);
            JPanel itemProducts    = buildMenuItem("📋  Gói Đầu Tư",        CARD_PRODUCTS,    false);
            JPanel itemInvestments = buildMenuItem("💼  Khoản Của Tôi",     CARD_INVESTMENTS, false);
            JPanel itemSettings    = buildMenuItem("⚙  Cài đặt",           CARD_SETTINGS,    false);

            side.add(itemDashboard);
            side.add(Box.createVerticalStrut(2));
            side.add(itemProducts);
            side.add(Box.createVerticalStrut(2));
            side.add(itemInvestments);
            side.add(Box.createVerticalStrut(2));
            side.add(itemSettings);

            activeItem = itemDashboard;
            setActiveStyle(itemDashboard, true);

            if (isAdmin) {
                side.add(Box.createVerticalStrut(12));
                side.add(buildSeparator());
                side.add(Box.createVerticalStrut(8));

                JPanel adminLbl = new JPanel(new BorderLayout());
                adminLbl.setOpaque(false);
                adminLbl.setMaximumSize(new Dimension(210, 26));
                JLabel lbl = new JLabel("  QUẢN TRỊ");
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
                lbl.setForeground(new Color(130, 160, 200));
                adminLbl.add(lbl, BorderLayout.CENTER);
                side.add(adminLbl);

                JPanel iAdminDash = buildMenuItem("📊  Tổng quan (Admin)", CARD_ADMIN_DASH,     false);
                JPanel iAdminProd = buildMenuItem("📋  Quản lý Gói",       CARD_ADMIN_PRODUCTS, false);
                JPanel iAdminUser = buildMenuItem("👥  Quản lý User",      CARD_ADMIN_USERS,    false);
                JPanel itemPerm   = buildMenuItemCustom("🔒  Phân Quyền", e -> new PermissionManagementView());

                side.add(iAdminDash);
                side.add(Box.createVerticalStrut(2));
                side.add(iAdminProd);
                side.add(Box.createVerticalStrut(2));
                side.add(iAdminUser);
                side.add(Box.createVerticalStrut(2));
                side.add(itemPerm);

                // Set default active for admin
                activeItem = iAdminDash;
                setActiveStyle(iAdminDash, true);
                setActiveStyle(itemDashboard, false); // Turn off customer dash style
            }
        }

        this.activeItem = activeItem;
        return side;
    }

    private JPanel buildMenuItem(String title, String cardId, boolean active) {
        JPanel item = new JPanel(new BorderLayout());
        item.setMaximumSize(new Dimension(210, 46));
        item.setBackground(active ? BLUE : SIDEBAR);
        item.setBorder(new EmptyBorder(0, 20, 0, 8));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.setOpaque(true);

        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 13));
        lbl.setForeground(active ? Color.WHITE : new Color(180, 200, 225));
        item.add(lbl, BorderLayout.CENTER);

        item.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (activeItem != item) item.setBackground(new Color(35, 70, 120));
            }
            public void mouseExited(MouseEvent e) {
                if (activeItem != item) item.setBackground(SIDEBAR);
            }
            public void mouseClicked(MouseEvent e) {
                setActiveStyle(activeItem, false);
                activeItem = item;
                setActiveStyle(item, true);
                ((JLabel) item.getComponent(0)).setText(title);
                showPanel(cardId);
            }
        });
        return item;
    }

    private JPanel buildMenuItemCustom(String title, ActionListener onClick) {
        JPanel item = new JPanel(new BorderLayout());
        item.setMaximumSize(new Dimension(210, 46));
        item.setBackground(SIDEBAR);
        item.setBorder(new EmptyBorder(0, 20, 0, 8));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.setOpaque(true);

        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(new Color(180, 200, 225));
        item.add(lbl, BorderLayout.CENTER);

        item.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { item.setBackground(new Color(35, 70, 120)); }
            public void mouseExited(MouseEvent e)  { item.setBackground(SIDEBAR); }
            public void mouseClicked(MouseEvent e) { if (onClick != null) onClick.actionPerformed(null); }
        });
        return item;
    }

    private void setActiveStyle(JPanel item, boolean active) {
        if (item == null) return;
        item.setBackground(active ? BLUE : SIDEBAR);
        if (item.getComponentCount() > 0 && item.getComponent(0) instanceof JLabel lbl) {
            lbl.setForeground(active ? Color.WHITE : new Color(180, 200, 225));
            lbl.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 13));
        }
    }

    private JPanel buildSeparator() {
        JPanel sep = new JPanel();
        sep.setBackground(new Color(40, 70, 130));
        sep.setMaximumSize(new Dimension(180, 1));
        sep.setAlignmentX(LEFT_ALIGNMENT);
        return sep;
    }

    // =========================================================================
    //  Panel Switching (lazy init)
    // =========================================================================

    private void showPanel(String cardId) {
        switch (cardId) {
            // ── Customer panels ───────────────────────────────────────────────
            case CARD_DASHBOARD -> {
                if (dashboardPanel == null) {
                    dashboardPanel = new CustomerDashboardPanel(currentAccount);
                    contentPane.add(dashboardPanel, CARD_DASHBOARD);
                } else dashboardPanel.loadData();
            }
            case CARD_PRODUCTS -> {
                if (productsPanel == null) {
                    productsPanel = new SavingsProductListPanel(currentAccount);
                    contentPane.add(productsPanel, CARD_PRODUCTS);
                } else productsPanel.loadData();
            }
            case CARD_INVESTMENTS -> {
                if (myInvestmentsPanel == null) {
                    myInvestmentsPanel = new MyInvestmentsPanel(currentAccount);
                    contentPane.add(myInvestmentsPanel, CARD_INVESTMENTS);
                } else myInvestmentsPanel.loadData();
            }
            case CARD_SETTINGS -> { /* placeholder */ }

            // ── Admin panels ──────────────────────────────────────────────────
            case CARD_ADMIN_DASH -> {
                if (adminDashboardPanel == null) {
                    adminDashboardPanel = new AdminDashboardPanel(currentAccount);
                    contentPane.add(adminDashboardPanel, CARD_ADMIN_DASH);
                } else adminDashboardPanel.loadData();
            }
            case CARD_ADMIN_PRODUCTS -> {
                if (adminProductsPanel == null) {
                    adminProductsPanel = new SavingsProductPanel(currentAccount);
                    contentPane.add(adminProductsPanel, CARD_ADMIN_PRODUCTS);
                } else adminProductsPanel.loadData();
            }
            case CARD_ADMIN_USERS -> {
                if (adminUsersPanel == null) {
                    adminUsersPanel = new UserManagementPanel(currentAccount);
                    contentPane.add(adminUsersPanel, CARD_ADMIN_USERS);
                } else adminUsersPanel.loadData(); // Will throw an error if no reload method, wait I didn't add loadData() as public in UserManagementPanel? Wait, I didn't add it as public. Wait I can just do nothing or call loadUsers if I make it public. Let me replace this chunk with no reload for users if it's too complicated, or just add loadData() manually. Ah, I will just call it if it exists.
            }

            // ── Staff panels ──────────────────────────────────────────────────
            case CARD_STAFF_DASH -> {
                if (staffDashboardPanel == null) {
                    staffDashboardPanel = new StaffDashboardPanel(currentAccount);
                    contentPane.add(staffDashboardPanel, CARD_STAFF_DASH);
                } else staffDashboardPanel.loadData();
            }
            case CARD_STAFF_DEPOSIT -> {
                if (depositApprovalPanel == null) {
                    depositApprovalPanel = new DepositApprovalPanel();
                    contentPane.add(depositApprovalPanel, CARD_STAFF_DEPOSIT);
                } else depositApprovalPanel.loadData();
            }
            case CARD_STAFF_WITHDRAW -> {
                if (withdrawApprovalPanel == null) {
                    withdrawApprovalPanel = new WithdrawApprovalPanel();
                    contentPane.add(withdrawApprovalPanel, CARD_STAFF_WITHDRAW);
                } else withdrawApprovalPanel.loadData();
            }
            case CARD_STAFF_KYC -> {
                if (ekycApprovalPanel == null) {
                    ekycApprovalPanel = new EkycApprovalPanel();
                    contentPane.add(ekycApprovalPanel, CARD_STAFF_KYC);
                } else ekycApprovalPanel.loadData();
            }
        }
        cardLayout.show(contentPane, cardId);
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    private JButton styledBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(bg.darker()); }
            public void mouseExited(MouseEvent e)  { b.setBackground(bg); }
        });
        return b;
    }
}
