package View;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import Controller.LoginController;
import Controller.RegisterController;
import DAO.AccountPermissionDAO;
import Model.AccountModel;
import Utils.SessionManager;
import View.customer.CustomerDashboardPanel;
import View.customer.CustomerFrame;

public class LoginForm extends JFrame {

    // ===== Panel ảnh bên trái =====
    private ImagePanel pnlImage;

    private JPanel pnlMain;
    private JPanel pnlTab;
    private JPanel pnlContent; // CardLayout chứa 2 trang
    private JLabel lblDangNhap;
    private JLabel lblDangKy;

    // === Form Đăng Nhập ===
    private JPanel pnlLogin;
    private JLabel lblUsername;
    private JTextField txtUsername;
    private JLabel lblPassword;
    private JPasswordField txtPassword;
    private JLabel lblForgot;
    private JButton btnZingPlay;

    // === Form Đăng Ký ===
    private JPanel pnlRegister;
    private JLabel lblRegUser;
    private JTextField txtRegUser;
    private JLabel lblRegEmail;
    private JTextField txtRegEmail;
    private JLabel lblRegPass;
    private JPasswordField txtRegPass;
    private JLabel lblRegConfirm;
    private JPasswordField txtRegConfirm;
    private JLabel lblRegReferral;
    private JTextField txtRegReferral;
    private JButton btnRegister;

    // === Màu sắc ===
    private final Color BLUE     = new Color(0, 162, 232);
    private final Color GRAY_TXT = new Color(100, 100, 100);
    private final Color GRAY_BDR = new Color(200, 200, 200);
    private final Color RED_LINE = new Color(220, 50, 50);
    private final Color PH_COLOR = new Color(180, 180, 180);

    // === Placeholder texts ===
    private static final String PH_USERNAME = "Nhập tên đăng nhập";
    private static final String PH_PASSWORD = "Nhập mật khẩu";
    private static final String PH_EMAIL    = "Nhập email của bạn";
    private static final String PH_CONFIRM  = "Nhập lại mật khẩu";
    private static final String PH_REFERRAL = "Nhập mã giới thiệu (nếu có)";

    // === Controllers ===
    private final LoginController    loginController    = new LoginController();
    private final RegisterController registerController = new RegisterController();

    private CardLayout cardLayout;

    public LoginForm() {
        initComponents();
        styleComponents();
        addListeners();
        setVisible(true);
    }

    private void initComponents() {
        setTitle("FlexInvest - Đăng Nhập / Đăng Ký");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 620);
        setLocationRelativeTo(null);
        setResizable(false);

        // Panel chính: split trái/phải
        pnlMain = new JPanel(new BorderLayout());
        pnlMain.setBackground(Color.WHITE);
        setContentPane(pnlMain);

        // ---- Panel ảnh bên trái ----
        pnlImage = new ImagePanel("/Resources/login_bg.jpg");
        pnlImage.setPreferredSize(new Dimension(400, 620));
        pnlMain.add(pnlImage, BorderLayout.WEST);

        // ---- Panel phải: tab + content ----
        JPanel pnlRight = new JPanel(new BorderLayout());
        pnlRight.setBackground(Color.WHITE);

        // ---- Tab header ----
        pnlTab = new JPanel(new GridLayout(1, 2));
        pnlTab.setBackground(Color.WHITE);
        pnlTab.setPreferredSize(new Dimension(500, 55));
        lblDangNhap = new JLabel("ĐĂNG NHẬP", SwingConstants.CENTER);
        lblDangKy   = new JLabel("ĐĂNG KÝ",   SwingConstants.CENTER);
        pnlTab.add(lblDangNhap);
        pnlTab.add(lblDangKy);
        pnlRight.add(pnlTab, BorderLayout.NORTH);

        // ---- CardLayout content ----
        cardLayout = new CardLayout();
        pnlContent = new JPanel(cardLayout);
        pnlContent.setBackground(Color.WHITE);

        // ---- Trang Đăng Nhập ----
        pnlLogin = new JPanel();
        pnlLogin.setBackground(Color.WHITE);
        pnlLogin.setLayout(new BoxLayout(pnlLogin, BoxLayout.Y_AXIS));
        pnlLogin.setBorder(new EmptyBorder(10, 50, 30, 50));

        lblUsername = new JLabel("Tên đăng nhập");
        txtUsername = new JTextField();
        lblPassword = new JLabel("Mật khẩu");
        txtPassword = new JPasswordField();
        lblForgot   = new JLabel("<html><u>Quên mật khẩu</u></html>");
        btnZingPlay = new JButton("Đăng nhập");

        pnlLogin.add(lblUsername);
        pnlLogin.add(Box.createVerticalStrut(6));
        pnlLogin.add(txtUsername);
        pnlLogin.add(Box.createVerticalStrut(16));
        pnlLogin.add(lblPassword);
        pnlLogin.add(Box.createVerticalStrut(6));
        pnlLogin.add(txtPassword);
        pnlLogin.add(Box.createVerticalStrut(10));
        pnlLogin.add(lblForgot);
        pnlLogin.add(Box.createVerticalStrut(16));
        pnlLogin.add(btnZingPlay);

        // ---- Trang Đăng Ký ----
        pnlRegister = new JPanel();
        pnlRegister.setBackground(Color.WHITE);
        pnlRegister.setLayout(new BoxLayout(pnlRegister, BoxLayout.Y_AXIS));
        pnlRegister.setBorder(new EmptyBorder(10, 50, 30, 50));

        lblRegUser    = new JLabel("Tên đăng nhập");
        txtRegUser    = new JTextField();
        lblRegEmail   = new JLabel("Email");
        txtRegEmail   = new JTextField();
        lblRegPass    = new JLabel("Mật khẩu");
        txtRegPass    = new JPasswordField();
        lblRegConfirm = new JLabel("Xác nhận mật khẩu");
        txtRegConfirm = new JPasswordField();
        lblRegReferral = new JLabel("Mã giới thiệu (không bắt buộc)");
        txtRegReferral = new JTextField();
        btnRegister   = new JButton("Tạo tài khoản");

        pnlRegister.add(lblRegUser);
        pnlRegister.add(Box.createVerticalStrut(6));
        pnlRegister.add(txtRegUser);
        pnlRegister.add(Box.createVerticalStrut(14));
        pnlRegister.add(lblRegEmail);
        pnlRegister.add(Box.createVerticalStrut(6));
        pnlRegister.add(txtRegEmail);
        pnlRegister.add(Box.createVerticalStrut(14));
        pnlRegister.add(lblRegPass);
        pnlRegister.add(Box.createVerticalStrut(6));
        pnlRegister.add(txtRegPass);
        pnlRegister.add(Box.createVerticalStrut(14));
        pnlRegister.add(lblRegConfirm);
        pnlRegister.add(Box.createVerticalStrut(6));
        pnlRegister.add(txtRegConfirm);
        pnlRegister.add(Box.createVerticalStrut(14));
        pnlRegister.add(lblRegReferral);
        pnlRegister.add(Box.createVerticalStrut(6));
        pnlRegister.add(txtRegReferral);
        pnlRegister.add(Box.createVerticalStrut(20));
        pnlRegister.add(btnRegister);

        // ---- Thêm vào CardLayout ----
        pnlContent.add(pnlLogin,    "LOGIN");
        pnlContent.add(pnlRegister, "REGISTER");

        pnlRight.add(pnlContent, BorderLayout.CENTER);
        pnlMain.add(pnlRight, BorderLayout.CENTER);
    }

    // ===== Inner class: Panel hiển thị ảnh co giãn theo kích thước =====
    static class ImagePanel extends JPanel {
        private BufferedImage image;

        public ImagePanel(String resourcePath) {
            setLayout(null);
            try {
                URL url = getClass().getResource(resourcePath);
                if (url != null) {
                    image = ImageIO.read(url);
                }
            } catch (IOException e) {
                System.err.println("Không thể tải ảnh: " + resourcePath);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                // Vẽ ảnh phủ đầy panel, giữ tỉ lệ cover (crop nếu cần)
                int pw = getWidth(), ph = getHeight();
                int iw = image.getWidth(), ih = image.getHeight();
                double scaleX = (double) pw / iw;
                double scaleY = (double) ph / ih;
                double scale  = Math.max(scaleX, scaleY);
                int drawW = (int)(iw * scale);
                int drawH = (int)(ih * scale);
                int x = (pw - drawW) / 2;
                int y = (ph - drawH) / 2;
                g2d.drawImage(image, x, y, drawW, drawH, null);

                // Overlay gradient mờ để chữ dễ đọc (tùy chọn)
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(0, 0, 0, 80),
                    0, ph, new Color(0, 0, 0, 160));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, pw, ph);
            } else {
                // Fallback: gradient xanh đẹp nếu chưa có ảnh
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(0, 120, 215),
                    0, getHeight(), new Color(0, 60, 150));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Vẽ text placeholder
                g2d.setColor(new Color(255, 255, 255, 180));
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 16));
                String msg = "Đặt ảnh login_bg.jpg";
                String msg2 = "vào src/Resources/";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(msg,  (getWidth() - fm.stringWidth(msg))  / 2, getHeight()/2 - 10);
                g2d.drawString(msg2, (getWidth() - fm.stringWidth(msg2)) / 2, getHeight()/2 + 20);
            }
        }
    }

    private void styleComponents() {
        Font tabFont = new Font("Segoe UI", Font.BOLD, 14);

        lblDangNhap.setFont(tabFont);
        lblDangNhap.setForeground(Color.BLACK);
        lblDangNhap.setBorder(new MatteBorder(0, 0, 3, 0, RED_LINE));
        lblDangNhap.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        lblDangKy.setFont(tabFont);
        lblDangKy.setForeground(GRAY_TXT);
        lblDangKy.setBorder(new EmptyBorder(0, 0, 3, 0));
        lblDangKy.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Labels đăng nhập
        Font lf = new Font("Segoe UI", Font.PLAIN, 13);
        styleLabel(lblUsername, lf);
        styleLabel(lblPassword, lf);
        lblForgot.setFont(lf); lblForgot.setForeground(GRAY_TXT);
        lblForgot.setAlignmentX(LEFT_ALIGNMENT);
        lblForgot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblForgot.setBorder(new EmptyBorder(0, 0, 6, 0));

        // Labels đăng ký
        styleLabel(lblRegUser, lf);
        styleLabel(lblRegEmail, lf);
        styleLabel(lblRegPass, lf);
        styleLabel(lblRegConfirm, lf);
        styleLabel(lblRegReferral, lf);

        // Fields đăng nhập
        styleField(txtUsername);
        setPlaceholder(txtUsername, PH_USERNAME);
        styleField(txtPassword);
        setPasswordPlaceholder(txtPassword, PH_PASSWORD);

        // Fields đăng ký
        styleField(txtRegUser);
        setPlaceholder(txtRegUser, PH_USERNAME);
        styleField(txtRegEmail);
        setPlaceholder(txtRegEmail, PH_EMAIL);
        styleField(txtRegPass);
        setPasswordPlaceholder(txtRegPass, PH_PASSWORD);
        styleField(txtRegConfirm);
        setPasswordPlaceholder(txtRegConfirm, PH_CONFIRM);
        styleField(txtRegReferral);
        setPlaceholder(txtRegReferral, PH_REFERRAL);

        // Buttons
        styleButton(btnZingPlay, BLUE);
        styleButton(btnRegister, BLUE);
    }

    private void styleLabel(JLabel l, Font f) {
        l.setFont(f); l.setForeground(GRAY_TXT); l.setAlignmentX(LEFT_ALIGNMENT);
    }

    private void styleField(JComponent f) {
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setAlignmentX(LEFT_ALIGNMENT);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        f.setPreferredSize(new Dimension(400, 44));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GRAY_BDR, 1, true),
            new EmptyBorder(0, 12, 0, 12)));
        f.setBackground(Color.WHITE);
    }

    private void setPlaceholder(JTextField f, String ph) {
        f.setText(ph); f.setForeground(PH_COLOR);
        f.addFocusListener(new FocusAdapter() {
            boolean s = true;
            public void focusGained(FocusEvent e) { if (s) { f.setText(""); f.setForeground(Color.DARK_GRAY); s = false; } }
            public void focusLost(FocusEvent e)   { if (f.getText().isEmpty()) { f.setText(ph); f.setForeground(PH_COLOR); s = true; } }
        });
    }

    private void setPasswordPlaceholder(JPasswordField f, String ph) {
        f.setEchoChar((char) 0); f.setText(ph); f.setForeground(PH_COLOR);
        f.addFocusListener(new FocusAdapter() {
            boolean s = true;
            public void focusGained(FocusEvent e) {
                if (s) { f.setText(""); f.setForeground(Color.DARK_GRAY); f.setEchoChar('●'); s = false; }
            }
            public void focusLost(FocusEvent e) {
                if (new String(f.getPassword()).isEmpty()) { f.setEchoChar((char)0); f.setText(ph); f.setForeground(PH_COLOR); s = true; }
            }
        });
    }

    private void styleButton(JButton b, Color bg) {
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        b.setPreferredSize(new Dimension(400, 48));
        b.setFocusPainted(false); b.setBorderPainted(false); b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(bg.darker()); }
            public void mouseExited(MouseEvent e)  { b.setBackground(bg); }
        });
    }

    private void switchTab(boolean showLogin) {
        if (showLogin) {
            cardLayout.show(pnlContent, "LOGIN");
            lblDangNhap.setForeground(Color.BLACK);
            lblDangNhap.setBorder(new MatteBorder(0, 0, 3, 0, RED_LINE));
            lblDangKy.setForeground(GRAY_TXT);
            lblDangKy.setBorder(new EmptyBorder(0, 0, 3, 0));
        } else {
            cardLayout.show(pnlContent, "REGISTER");
            lblDangKy.setForeground(Color.BLACK);
            lblDangKy.setBorder(new MatteBorder(0, 0, 3, 0, RED_LINE));
            lblDangNhap.setForeground(GRAY_TXT);
            lblDangNhap.setBorder(new EmptyBorder(0, 0, 3, 0));
        }
    }

    // Lấy giá trị thực của JTextField (trả về "" nếu đang hiển thị placeholder)
    private String getFieldValue(JTextField f, String placeholder) {
        String val = f.getText().trim();
        return val.equals(placeholder) ? "" : val;
    }

    // Lấy giá trị thực của JPasswordField (trả về "" nếu đang hiển thị placeholder)
    private String getPasswordValue(JPasswordField f, String placeholder) {
        String val = new String(f.getPassword()).trim();
        return val.equals(placeholder) ? "" : val;
    }

    private void addListeners() {
        // Chuyển tab
        lblDangNhap.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { switchTab(true); }
        });
        lblDangKy.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { switchTab(false); }
        });

        // Quên mật khẩu
        lblForgot.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { new ForgotPassword(); }
        });

        // Đăng nhập
        btnZingPlay.addActionListener(e -> {
            String user = getFieldValue(txtUsername, PH_USERNAME);
            String pass = getPasswordValue(txtPassword, PH_PASSWORD);

            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            AccountModel account = loginController.loginController(user, pass);
            if (account != null) {
                SessionManager.login(account);

                // Kiểm tra role
                if (account.user().getRoleId() == 1) {
                    new MainPage(account);
                } else {
                    new CustomerFrame(
                        account.user().getUserId(),
                        account.account().getUsername()
                    );
                }   
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Tên đăng nhập hoặc mật khẩu không đúng!", "Lỗi đăng nhập", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Đăng ký
        btnRegister.addActionListener(e -> {
            String user    = getFieldValue(txtRegUser, PH_USERNAME);
            String email   = getFieldValue(txtRegEmail, PH_EMAIL);
            String pass    = getPasswordValue(txtRegPass, PH_PASSWORD);
            String confirm = getPasswordValue(txtRegConfirm, PH_CONFIRM);
            String referral = getFieldValue(txtRegReferral, PH_REFERRAL);

            if (user.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!email.matches("^[\\w.+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
                JOptionPane.showMessageDialog(this, "Email không đúng định dạng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (pass.length() < 6) {
                JOptionPane.showMessageDialog(this, "Mật khẩu phải có ít nhất 6 ký tự!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!pass.equals(confirm)) {
                JOptionPane.showMessageDialog(this, "Mật khẩu xác nhận không khớp!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            switch (registerController.registerController(user, email, pass, referral)) {
                case "SUCCESS":
                    JOptionPane.showMessageDialog(this, "Đăng ký thành công! Vui lòng đăng nhập.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    switchTab(true);
                    break;
                case "USERNAME_EXISTS":
                    JOptionPane.showMessageDialog(this, "Tên đăng nhập đã tồn tại, vui lòng chọn tên khác!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    break;
                case "EMAIL_EXISTS":
                    JOptionPane.showMessageDialog(this, "Email đã được sử dụng, vui lòng dùng email khác!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    break;
                default:
                    JOptionPane.showMessageDialog(this, "Đăng ký thất bại, vui lòng thử lại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    break;
            }
        });
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(LoginForm::new);
    }
}