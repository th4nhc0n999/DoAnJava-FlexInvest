package View;

import Controller.ForgotPasswordController;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * ForgotPassword — luồng 3 bước:
 *  Step 1: Nhập email → sinh OTP (giả lập hiện trong dialog)
 *  Step 2: Nhập OTP 6 chữ số (giao diện giống OtpDialog)
 *  Step 3: Nhập mật khẩu mới + xác nhận → lưu DB
 */
public class ForgotPassword extends JFrame {

    // ── Màu sắc ──────────────────────────────────────────────────────────────
    private final Color BLUE     = new Color(0, 162, 232);
    private final Color NAVY     = new Color(15, 40, 80);
    private final Color GRAY_TXT = new Color(100, 100, 100);
    private final Color GRAY_BDR = new Color(200, 200, 200);
    private final Color PH_COLOR = new Color(180, 180, 180);
    private final Color GREEN    = new Color(16, 185, 129);

    private static final String PH_EMAIL    = "Nhập email đã đăng ký";
    private static final String PH_PASSWORD = "Nhập mật khẩu mới";
    private static final String PH_CONFIRM  = "Nhập lại mật khẩu mới";

    // ── Controller ───────────────────────────────────────────────────────────
    private final ForgotPasswordController controller = new ForgotPasswordController();

    // ── Card layout ──────────────────────────────────────────────────────────
    private JPanel     pnlContent;
    private CardLayout cardLayout;

    // ── Step 1: Email ─────────────────────────────────────────────────────────
    private JTextField txtEmail;
    private JButton    btnSendOtp;

    // ── Step 2: OTP ───────────────────────────────────────────────────────────
    private JTextField[] otpCells = new JTextField[6];
    private JButton      btnVerifyOtp;
    private JButton      btnResendOtp;
    private JLabel       lblOtpHint;

    // ── Step 3: Mật khẩu mới ─────────────────────────────────────────────────
    private JPasswordField txtNewPass;
    private JPasswordField txtConfirmPass;
    private JButton        btnReset;

    public ForgotPassword() {
        initComponents();
        setVisible(true);
    }

    // =========================================================================
    //  Build UI
    // =========================================================================

    private void initComponents() {
        setTitle("FlexInvest — Quên mật khẩu");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(480, 440);
        setLocationRelativeTo(null);
        setResizable(false);

        cardLayout = new CardLayout();
        pnlContent = new JPanel(cardLayout);
        pnlContent.setBackground(Color.WHITE);

        pnlContent.add(buildStep1(), "STEP1");
        pnlContent.add(buildStep2(), "STEP2");
        pnlContent.add(buildStep3(), "STEP3");

        setContentPane(pnlContent);
    }

    // ── Step 1: nhập email ────────────────────────────────────────────────────
    private JPanel buildStep1() {
        JPanel p = stepPanel();

        // Header
        p.add(header("Quên mật khẩu?", "Nhập email để nhận mã OTP xác minh."));
        p.add(Box.createVerticalStrut(28));

        // Email field
        p.add(fieldLabel("Email"));
        p.add(Box.createVerticalStrut(6));
        txtEmail = new JTextField();
        styleField(txtEmail);
        setPlaceholder(txtEmail, PH_EMAIL);
        p.add(txtEmail);
        p.add(Box.createVerticalStrut(20));

        // Button
        btnSendOtp = makeButton("Gửi mã OTP");
        btnSendOtp.addActionListener(e -> onSendOtp());
        p.add(btnSendOtp);
        p.add(Box.createVerticalStrut(14));
        p.add(backLink("Quay lại đăng nhập", () -> dispose()));

        return p;
    }

    // ── Step 2: nhập OTP ─────────────────────────────────────────────────────
    private JPanel buildStep2() {
        JPanel p = stepPanel();

        p.add(header("Nhập mã OTP", "Mã 6 chữ số đã được hiển thị trong hộp thoại."));
        p.add(Box.createVerticalStrut(24));

        // 6 ô OTP
        JPanel otpRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        otpRow.setOpaque(false);
        otpRow.setAlignmentX(LEFT_ALIGNMENT);
        Font cellFont = new Font("Segoe UI", Font.BOLD, 20);
        for (int i = 0; i < 6; i++) {
            JTextField tf = new JTextField(1);
            tf.setFont(cellFont);
            tf.setHorizontalAlignment(JTextField.CENTER);
            tf.setPreferredSize(new Dimension(52, 58));
            tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GRAY_BDR, 2, true),
                new EmptyBorder(0, 0, 0, 0)));
            tf.setBackground(new Color(248, 250, 253));

            final int idx = i;
            tf.addKeyListener(new KeyAdapter() {
                @Override public void keyTyped(KeyEvent e) {
                    if (!Character.isDigit(e.getKeyChar())) { e.consume(); return; }
                    tf.setText("");
                    SwingUtilities.invokeLater(() -> {
                        if (idx < 5) otpCells[idx + 1].requestFocus();
                    });
                }
                @Override public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE
                            && tf.getText().isEmpty() && idx > 0) {
                        otpCells[idx - 1].requestFocus();
                    }
                }
            });
            tf.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    tf.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BLUE, 2, true),
                        new EmptyBorder(0, 0, 0, 0)));
                }
                public void focusLost(FocusEvent e) {
                    tf.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(GRAY_BDR, 2, true),
                        new EmptyBorder(0, 0, 0, 0)));
                }
            });
            otpCells[i] = tf;
            otpRow.add(tf);
        }
        p.add(otpRow);
        p.add(Box.createVerticalStrut(6));

        lblOtpHint = new JLabel("Demo: xem mã OTP trong hộp thoại vừa hiện.");
        lblOtpHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblOtpHint.setForeground(new Color(140, 145, 160));
        lblOtpHint.setAlignmentX(LEFT_ALIGNMENT);
        p.add(lblOtpHint);
        p.add(Box.createVerticalStrut(20));

        btnVerifyOtp = makeButton("Xác nhận OTP");
        btnVerifyOtp.addActionListener(e -> onVerifyOtp());
        p.add(btnVerifyOtp);
        p.add(Box.createVerticalStrut(10));

        btnResendOtp = new JButton("<html><u>Gửi lại mã OTP</u></html>");
        btnResendOtp.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnResendOtp.setForeground(BLUE);
        btnResendOtp.setBackground(Color.WHITE);
        btnResendOtp.setBorderPainted(false);
        btnResendOtp.setFocusPainted(false);
        btnResendOtp.setOpaque(false);
        btnResendOtp.setAlignmentX(LEFT_ALIGNMENT);
        btnResendOtp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnResendOtp.addActionListener(e -> onResendOtp());
        p.add(btnResendOtp);
        p.add(Box.createVerticalStrut(6));
        p.add(backLink("Quay lại", () -> cardLayout.show(pnlContent, "STEP1")));

        return p;
    }

    // ── Step 3: mật khẩu mới ─────────────────────────────────────────────────
    private JPanel buildStep3() {
        JPanel p = stepPanel();

        p.add(header("Đặt lại mật khẩu", "Nhập mật khẩu mới cho tài khoản của bạn."));
        p.add(Box.createVerticalStrut(24));

        p.add(fieldLabel("Mật khẩu mới"));
        p.add(Box.createVerticalStrut(6));
        txtNewPass = new JPasswordField();
        styleField(txtNewPass);
        setPasswordPlaceholder(txtNewPass, PH_PASSWORD);
        p.add(txtNewPass);
        p.add(Box.createVerticalStrut(14));

        p.add(fieldLabel("Xác nhận mật khẩu"));
        p.add(Box.createVerticalStrut(6));
        txtConfirmPass = new JPasswordField();
        styleField(txtConfirmPass);
        setPasswordPlaceholder(txtConfirmPass, PH_CONFIRM);
        p.add(txtConfirmPass);
        p.add(Box.createVerticalStrut(20));

        btnReset = makeButton("Đổi mật khẩu");
        btnReset.setBackground(GREEN);
        btnReset.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnReset.setBackground(GREEN.darker()); }
            public void mouseExited(MouseEvent e)  { btnReset.setBackground(GREEN); }
        });
        btnReset.addActionListener(e -> onReset());
        p.add(btnReset);
        p.add(Box.createVerticalStrut(14));
        p.add(backLink("Quay lại đăng nhập", () -> dispose()));

        return p;
    }

    // =========================================================================
    //  Actions
    // =========================================================================

    private void onSendOtp() {
        String email = getField(txtEmail, PH_EMAIL);
        if (email.isEmpty()) {
            warn("Vui lòng nhập địa chỉ email!");
            return;
        }

        btnSendOtp.setEnabled(false);
        btnSendOtp.setText("Đang xử lý...");
        SwingWorker<String, Void> w = new SwingWorker<>() {
            @Override protected String doInBackground() {
                return controller.sendOtp(email);
            }
            @Override protected void done() {
                try {
                    String otp = get();
                    if (otp == null) {
                        error("Email không tồn tại hoặc tài khoản bị khóa!");
                    } else {
                        // Giả lập gửi mail: hiện OTP trong dialog
                        JOptionPane.showMessageDialog(ForgotPassword.this,
                            "<html><b>📧 Mã OTP của bạn là:</b><br/><br/>"
                            + "<span style='font-size:24pt; color:#0066CC; letter-spacing:6px'><b>" + otp + "</b></span><br/><br/>"
                            + "<i>(Trong hệ thống thật, OTP sẽ gửi qua email)</i></html>",
                            "Mã OTP xác minh", JOptionPane.INFORMATION_MESSAGE);
                        // Chuyển sang bước 2
                        otpCells[0].requestFocus();
                        cardLayout.show(pnlContent, "STEP2");
                        setSize(480, 460);
                        setLocationRelativeTo(null);
                    }
                } catch (Exception ex) {
                    error("Lỗi hệ thống: " + ex.getMessage());
                } finally {
                    btnSendOtp.setEnabled(true);
                    btnSendOtp.setText("Gửi mã OTP");
                }
            }
        };
        w.execute();
    }

    private void onVerifyOtp() {
        StringBuilder sb = new StringBuilder();
        for (JTextField c : otpCells) sb.append(c.getText().trim());
        if (sb.length() < 6) {
            warn("Vui lòng nhập đủ 6 chữ số OTP!");
            otpCells[0].requestFocus();
            return;
        }

        if (!controller.verifyOtp(sb.toString())) {
            error("Mã OTP không đúng! Vui lòng kiểm tra lại.");
            // Xoá và focus lại ô đầu
            for (JTextField c : otpCells) c.setText("");
            otpCells[0].requestFocus();
            return;
        }

        // OTP đúng → chuyển sang bước 3
        cardLayout.show(pnlContent, "STEP3");
        setSize(480, 460);
        setLocationRelativeTo(null);
    }

    private void onResendOtp() {
        // Quay lại bước 1 để nhập lại email / gửi lại OTP
        for (JTextField c : otpCells) c.setText("");
        cardLayout.show(pnlContent, "STEP1");
        setSize(480, 440);
        setLocationRelativeTo(null);
        // Tự động trigger lại gửi OTP với email cũ
        String email = getField(txtEmail, PH_EMAIL);
        if (!email.isEmpty()) onSendOtp();
    }

    private void onReset() {
        String newPass = getPwd(txtNewPass, PH_PASSWORD);
        String confirm = getPwd(txtConfirmPass, PH_CONFIRM);

        if (newPass.isEmpty() || confirm.isEmpty()) {
            warn("Vui lòng nhập đầy đủ mật khẩu!");
            return;
        }
        if (newPass.length() < 6) {
            warn("Mật khẩu phải có ít nhất 6 ký tự!");
            return;
        }
        if (!newPass.equals(confirm)) {
            error("Mật khẩu xác nhận không khớp!");
            return;
        }

        btnReset.setEnabled(false);
        btnReset.setText("Đang lưu...");
        SwingWorker<Boolean, Void> w = new SwingWorker<>() {
            @Override protected Boolean doInBackground() {
                return controller.resetPassword(newPass);
            }
            @Override protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(ForgotPassword.this,
                            "✅ Đổi mật khẩu thành công!\nVui lòng đăng nhập lại.",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } else {
                        error("Đổi mật khẩu thất bại, vui lòng thử lại!");
                    }
                } catch (Exception ex) {
                    error("Lỗi hệ thống: " + ex.getMessage());
                } finally {
                    btnReset.setEnabled(true);
                    btnReset.setText("Đổi mật khẩu");
                }
            }
        };
        w.execute();
    }

    // =========================================================================
    //  Helpers — UI builders
    // =========================================================================

    private JPanel stepPanel() {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(36, 50, 28, 50));
        return p;
    }

    /** Header block: title + subtitle */
    private JPanel header(String title, String sub) {
        JPanel h = new JPanel();
        h.setOpaque(false);
        h.setLayout(new BoxLayout(h, BoxLayout.Y_AXIS));

        // Accent bar
        JPanel bar = new JPanel();
        bar.setBackground(BLUE);
        bar.setMaximumSize(new Dimension(40, 4));
        bar.setAlignmentX(LEFT_ALIGNMENT);
        h.add(bar);
        h.add(Box.createVerticalStrut(10));

        JLabel lTitle = new JLabel(title);
        lTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lTitle.setForeground(NAVY);
        lTitle.setAlignmentX(LEFT_ALIGNMENT);
        h.add(lTitle);

        JLabel lSub = new JLabel(sub);
        lSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lSub.setForeground(GRAY_TXT);
        lSub.setAlignmentX(LEFT_ALIGNMENT);
        h.add(Box.createVerticalStrut(4));
        h.add(lSub);
        return h;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(GRAY_TXT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
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

    private JButton makeButton(String text) {
        JButton b = new JButton(text);
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
            Color orig = b.getBackground();
            public void mouseEntered(MouseEvent e) { b.setBackground(orig.darker()); }
            public void mouseExited(MouseEvent e)  { b.setBackground(orig); }
        });
        return b;
    }

    private JLabel backLink(String text, Runnable action) {
        JLabel l = new JLabel("<html><u>" + text + "</u></html>");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(GRAY_TXT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { action.run(); }
            public void mouseEntered(MouseEvent e) { l.setForeground(BLUE); }
            public void mouseExited(MouseEvent e)  { l.setForeground(GRAY_TXT); }
        });
        return l;
    }

    // ── Placeholder helpers ────────────────────────────────────────────────

    private void setPlaceholder(JTextField f, String ph) {
        f.setText(ph); f.setForeground(PH_COLOR);
        f.addFocusListener(new FocusAdapter() {
            boolean on = true;
            public void focusGained(FocusEvent e) {
                if (on) { f.setText(""); f.setForeground(Color.DARK_GRAY); on = false; }
            }
            public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) { f.setText(ph); f.setForeground(PH_COLOR); on = true; }
            }
        });
    }

    private void setPasswordPlaceholder(JPasswordField f, String ph) {
        f.setEchoChar((char) 0); f.setText(ph); f.setForeground(PH_COLOR);
        f.addFocusListener(new FocusAdapter() {
            boolean on = true;
            public void focusGained(FocusEvent e) {
                if (on) { f.setText(""); f.setForeground(Color.DARK_GRAY); f.setEchoChar('●'); on = false; }
            }
            public void focusLost(FocusEvent e) {
                if (new String(f.getPassword()).isEmpty()) {
                    f.setEchoChar((char) 0); f.setText(ph); f.setForeground(PH_COLOR); on = true;
                }
            }
        });
    }

    // ── Value helpers ─────────────────────────────────────────────────────

    private String getField(JTextField f, String ph) {
        String v = f.getText().trim();
        return v.equals(ph) ? "" : v;
    }

    private String getPwd(JPasswordField f, String ph) {
        String v = new String(f.getPassword()).trim();
        return v.equals(ph) ? "" : v;
    }

    // ── Dialog shortcuts ──────────────────────────────────────────────────

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thông báo", JOptionPane.WARNING_MESSAGE);
    }
    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
