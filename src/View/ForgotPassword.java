package View;

import Controller.ForgotPasswordController;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class ForgotPassword extends JFrame {

    // === Bước 1: nhập email ===
    private JPanel      pnlStep1;
    private JLabel      lblEmailTitle;
    private JLabel      lblEmailSub;
    private JLabel      lblEmail;
    private JTextField  txtEmail;
    private JButton     btnNext;
    private JLabel      lblBackToLogin1;

    // === Bước 2: nhập mật khẩu mới ===
    private JPanel         pnlStep2;
    private JLabel         lblResetTitle;
    private JLabel         lblResetSub;
    private JLabel         lblNewPass;
    private JPasswordField txtNewPass;
    private JLabel         lblConfirmPass;
    private JPasswordField txtConfirmPass;
    private JButton        btnReset;
    private JLabel         lblBackToLogin2;

    private JPanel     pnlContent;
    private CardLayout cardLayout;

    // === Màu sắc (đồng bộ LoginForm) ===
    private final Color BLUE     = new Color(0, 162, 232);
    private final Color GRAY_TXT = new Color(100, 100, 100);
    private final Color GRAY_BDR = new Color(200, 200, 200);
    private final Color PH_COLOR = new Color(180, 180, 180);

    private static final String PH_EMAIL    = "Nhập email đã đăng ký";
    private static final String PH_PASSWORD = "Nhập mật khẩu mới";
    private static final String PH_CONFIRM  = "Nhập lại mật khẩu mới";

    private final ForgotPasswordController controller = new ForgotPasswordController();

    // userId tìm được ở bước 1, dùng lại ở bước 2
    private String foundUserId;

    public ForgotPassword() {
        initComponents();
        styleComponents();
        addListeners();
        setVisible(true);
    }

    private void initComponents() {
        setTitle("FlexInvest - Quên mật khẩu");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(480, 420);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel pnlMain = new JPanel(new BorderLayout());
        pnlMain.setBackground(Color.WHITE);
        setContentPane(pnlMain);

        cardLayout = new CardLayout();
        pnlContent = new JPanel(cardLayout);
        pnlContent.setBackground(Color.WHITE);

        // ── Bước 1 ──────────────────────────────────────────────────────────
        pnlStep1 = new JPanel();
        pnlStep1.setBackground(Color.WHITE);
        pnlStep1.setLayout(new BoxLayout(pnlStep1, BoxLayout.Y_AXIS));
        pnlStep1.setBorder(new EmptyBorder(40, 50, 30, 50));

        lblEmailTitle = new JLabel("Quên mật khẩu?");
        lblEmailSub   = new JLabel("Nhập email đã đăng ký để đặt lại mật khẩu.");
        lblEmail      = new JLabel("Email");
        txtEmail      = new JTextField();
        btnNext       = new JButton("Tiếp theo");
        lblBackToLogin1 = new JLabel("<html><u>Quay lại đăng nhập</u></html>");

        pnlStep1.add(lblEmailTitle);
        pnlStep1.add(Box.createVerticalStrut(6));
        pnlStep1.add(lblEmailSub);
        pnlStep1.add(Box.createVerticalStrut(30));
        pnlStep1.add(lblEmail);
        pnlStep1.add(Box.createVerticalStrut(6));
        pnlStep1.add(txtEmail);
        pnlStep1.add(Box.createVerticalStrut(20));
        pnlStep1.add(btnNext);
        pnlStep1.add(Box.createVerticalStrut(16));
        pnlStep1.add(lblBackToLogin1);

        // ── Bước 2 ──────────────────────────────────────────────────────────
        pnlStep2 = new JPanel();
        pnlStep2.setBackground(Color.WHITE);
        pnlStep2.setLayout(new BoxLayout(pnlStep2, BoxLayout.Y_AXIS));
        pnlStep2.setBorder(new EmptyBorder(40, 50, 30, 50));

        lblResetTitle   = new JLabel("Đặt lại mật khẩu");
        lblResetSub     = new JLabel("Nhập mật khẩu mới cho tài khoản của bạn.");
        lblNewPass      = new JLabel("Mật khẩu mới");
        txtNewPass      = new JPasswordField();
        lblConfirmPass  = new JLabel("Xác nhận mật khẩu");
        txtConfirmPass  = new JPasswordField();
        btnReset        = new JButton("Đổi mật khẩu");
        lblBackToLogin2 = new JLabel("<html><u>Quay lại đăng nhập</u></html>");

        pnlStep2.add(lblResetTitle);
        pnlStep2.add(Box.createVerticalStrut(6));
        pnlStep2.add(lblResetSub);
        pnlStep2.add(Box.createVerticalStrut(24));
        pnlStep2.add(lblNewPass);
        pnlStep2.add(Box.createVerticalStrut(6));
        pnlStep2.add(txtNewPass);
        pnlStep2.add(Box.createVerticalStrut(16));
        pnlStep2.add(lblConfirmPass);
        pnlStep2.add(Box.createVerticalStrut(6));
        pnlStep2.add(txtConfirmPass);
        pnlStep2.add(Box.createVerticalStrut(20));
        pnlStep2.add(btnReset);
        pnlStep2.add(Box.createVerticalStrut(16));
        pnlStep2.add(lblBackToLogin2);

        pnlContent.add(pnlStep1, "STEP1");
        pnlContent.add(pnlStep2, "STEP2");
        pnlMain.add(pnlContent, BorderLayout.CENTER);
    }

    private void styleComponents() {
        Font titleFont = new Font("Segoe UI", Font.BOLD, 20);
        Font subFont   = new Font("Segoe UI", Font.PLAIN, 13);
        Font labelFont = new Font("Segoe UI", Font.PLAIN, 13);

        // Bước 1
        lblEmailTitle.setFont(titleFont);
        lblEmailTitle.setForeground(Color.BLACK);
        lblEmailTitle.setAlignmentX(LEFT_ALIGNMENT);

        lblEmailSub.setFont(subFont);
        lblEmailSub.setForeground(GRAY_TXT);
        lblEmailSub.setAlignmentX(LEFT_ALIGNMENT);

        styleLabel(lblEmail, labelFont);
        styleField(txtEmail);
        setPlaceholder(txtEmail, PH_EMAIL);
        styleButton(btnNext);
        styleBackLink(lblBackToLogin1);

        // Bước 2
        lblResetTitle.setFont(titleFont);
        lblResetTitle.setForeground(Color.BLACK);
        lblResetTitle.setAlignmentX(LEFT_ALIGNMENT);

        lblResetSub.setFont(subFont);
        lblResetSub.setForeground(GRAY_TXT);
        lblResetSub.setAlignmentX(LEFT_ALIGNMENT);

        styleLabel(lblNewPass, labelFont);
        styleField(txtNewPass);
        setPasswordPlaceholder(txtNewPass, PH_PASSWORD);

        styleLabel(lblConfirmPass, labelFont);
        styleField(txtConfirmPass);
        setPasswordPlaceholder(txtConfirmPass, PH_CONFIRM);

        styleButton(btnReset);
        styleBackLink(lblBackToLogin2);
    }

    private void styleLabel(JLabel l, Font f) {
        l.setFont(f);
        l.setForeground(GRAY_TXT);
        l.setAlignmentX(LEFT_ALIGNMENT);
    }

    private void styleField(JComponent f) {
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setAlignmentX(LEFT_ALIGNMENT);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        f.setPreferredSize(new Dimension(380, 44));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GRAY_BDR, 1, true),
            new EmptyBorder(0, 12, 0, 12)));
        f.setBackground(Color.WHITE);
    }

    private void styleButton(JButton b) {
        b.setBackground(BLUE);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        b.setPreferredSize(new Dimension(380, 48));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(BLUE.darker()); }
            public void mouseExited(MouseEvent e)  { b.setBackground(BLUE); }
        });
    }

    private void styleBackLink(JLabel l) {
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(GRAY_TXT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void setPlaceholder(JTextField f, String ph) {
        f.setText(ph);
        f.setForeground(PH_COLOR);
        f.addFocusListener(new FocusAdapter() {
            boolean showing = true;
            public void focusGained(FocusEvent e) {
                if (showing) { f.setText(""); f.setForeground(Color.DARK_GRAY); showing = false; }
            }
            public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) { f.setText(ph); f.setForeground(PH_COLOR); showing = true; }
            }
        });
    }

    private void setPasswordPlaceholder(JPasswordField f, String ph) {
        f.setEchoChar((char) 0);
        f.setText(ph);
        f.setForeground(PH_COLOR);
        f.addFocusListener(new FocusAdapter() {
            boolean showing = true;
            public void focusGained(FocusEvent e) {
                if (showing) { f.setText(""); f.setForeground(Color.DARK_GRAY); f.setEchoChar('●'); showing = false; }
            }
            public void focusLost(FocusEvent e) {
                if (new String(f.getPassword()).isEmpty()) {
                    f.setEchoChar((char) 0); f.setText(ph); f.setForeground(PH_COLOR); showing = true;
                }
            }
        });
    }

    private String getFieldValue(JTextField f, String placeholder) {
        String val = f.getText().trim();
        return val.equals(placeholder) ? "" : val;
    }

    private String getPasswordValue(JPasswordField f, String placeholder) {
        String val = new String(f.getPassword()).trim();
        return val.equals(placeholder) ? "" : val;
    }

    private void addListeners() {
        // Bước 1: xác minh email
        btnNext.addActionListener(e -> {
            String email = getFieldValue(txtEmail, PH_EMAIL);
            if (email.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập địa chỉ email!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            foundUserId = controller.verifyEmail(email);
            if (foundUserId == null) {
                JOptionPane.showMessageDialog(this, "Email không tồn tại trong hệ thống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Chuyển sang bước 2
            cardLayout.show(pnlContent, "STEP2");
            setSize(480, 460);
        });

        // Bước 2: đặt lại mật khẩu
        btnReset.addActionListener(e -> {
            String newPass  = getPasswordValue(txtNewPass,     PH_PASSWORD);
            String confirm  = getPasswordValue(txtConfirmPass, PH_CONFIRM);

            if (newPass.isEmpty() || confirm.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!newPass.equals(confirm)) {
                JOptionPane.showMessageDialog(this, "Mật khẩu xác nhận không khớp!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean ok = controller.resetPassword(foundUserId, newPass);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Đổi mật khẩu thất bại, vui lòng thử lại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Quay lại đăng nhập
        lblBackToLogin1.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { dispose(); }
        });
        lblBackToLogin2.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { dispose(); }
        });
    }
}
