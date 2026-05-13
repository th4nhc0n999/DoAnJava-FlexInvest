package View;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * OtpDialog — Popup nhập mã OTP 6 số.
 * Dùng lại cho nhiều flow: mua gói, rút sớm, đổi mật khẩu.
 *
 * Cách dùng:
 *   OtpDialog dlg = new OtpDialog(parentFrame, "Xác nhận mua gói");
 *   String otp = dlg.showAndGetOtp();
 *   if (otp != null) { ... }  // null = người dùng nhấn Hủy
 */
public class OtpDialog extends JDialog {

    private static final Color NAVY    = new Color(15, 40, 80);
    private static final Color BLUE    = new Color(0, 162, 232);
    private static final Color BG      = new Color(245, 248, 252);
    private static final Color BORDER  = new Color(210, 220, 235);

    private final JTextField[] cells = new JTextField[6];
    private String resultOtp = null;  // null = Hủy

    public OtpDialog(Frame parent, String actionTitle) {
        super(parent, "Xác nhận OTP — " + actionTitle, true);
        build();
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void build() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(Color.WHITE);
        setContentPane(root);

        // ── Header ───────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(new EmptyBorder(20, 28, 20, 28));

        JLabel title = new JLabel("Xác nhận giao dịch");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Nhập mã OTP 6 số được gửi đến điện thoại của bạn");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(180, 200, 235));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(title);
        titles.add(Box.createVerticalStrut(4));
        titles.add(sub);
        header.add(titles, BorderLayout.CENTER);
        root.add(header, BorderLayout.NORTH);

        // ── Body ─────────────────────────────────────────────────────────────
        JPanel body = new JPanel();
        body.setBackground(Color.WHITE);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(28, 32, 20, 32));

        // Hàng 6 ô OTP
        JPanel otpRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        otpRow.setOpaque(false);
        Font cellFont = new Font("Segoe UI", Font.BOLD, 22);
        for (int i = 0; i < 6; i++) {
            JTextField tf = new JTextField(1);
            tf.setFont(cellFont);
            tf.setHorizontalAlignment(JTextField.CENTER);
            tf.setPreferredSize(new Dimension(50, 56));
            tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 2, true),
                new EmptyBorder(0, 0, 0, 0)));
            tf.setBackground(BG);

            final int idx = i;
            tf.addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    char c = e.getKeyChar();
                    if (!Character.isDigit(c)) { e.consume(); return; }
                    tf.setText("");
                    // auto-advance
                    SwingUtilities.invokeLater(() -> {
                        if (idx < 5) cells[idx + 1].requestFocus();
                    });
                }
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && tf.getText().isEmpty() && idx > 0) {
                        cells[idx - 1].requestFocus();
                    }
                }
            });
            // highlight border on focus
            tf.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    tf.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BLUE, 2, true),
                        new EmptyBorder(0, 0, 0, 0)));
                }
                public void focusLost(FocusEvent e) {
                    tf.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER, 2, true),
                        new EmptyBorder(0, 0, 0, 0)));
                }
            });

            cells[i] = tf;
            otpRow.add(tf);
        }
        body.add(otpRow);
        body.add(Box.createVerticalStrut(8));

        // Hint mã demo
        JLabel hint = new JLabel("Demo: nhập bất kỳ 6 chữ số để xác nhận");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hint.setForeground(new Color(150, 155, 170));
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(hint);
        body.add(Box.createVerticalStrut(24));

        // ── Buttons ───────────────────────────────────────────────────────────
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 12, 0));
        btnRow.setOpaque(false);
        btnRow.setMaximumSize(new Dimension(360, 46));
        btnRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnCancel  = makeBtn("Hủy",     new Color(107, 114, 128), false);
        JButton btnConfirm = makeBtn("Xác nhận", BLUE, true);

        btnCancel.addActionListener(e -> { resultOtp = null; dispose(); });
        btnConfirm.addActionListener(e -> confirm());

        // Enter submits
        getRootPane().setDefaultButton(btnConfirm);
        btnRow.add(btnCancel);
        btnRow.add(btnConfirm);
        body.add(btnRow);

        root.add(body, BorderLayout.CENTER);
    }

    private void confirm() {
        StringBuilder sb = new StringBuilder();
        for (JTextField c : cells) sb.append(c.getText().trim());
        if (sb.length() < 6) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ 6 chữ số!", "Thiếu mã OTP", JOptionPane.WARNING_MESSAGE);
            cells[0].requestFocus();
            return;
        }
        resultOtp = sb.toString();
        dispose();
    }

    private JButton makeBtn(String text, Color bg, boolean primary) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setForeground(primary ? Color.WHITE : new Color(80, 85, 100));
        b.setBackground(primary ? bg : new Color(230, 234, 242));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(160, 42));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(bg.darker()); }
            public void mouseExited(MouseEvent e)  { b.setBackground(primary ? bg : new Color(230, 234, 242)); }
        });
        return b;
    }

    /**
     * Hiển thị dialog và trả về OTP đã nhập, hoặc null nếu người dùng hủy.
     */
    public String showAndGetOtp() {
        cells[0].requestFocusInWindow();
        setVisible(true);   // blocks until dispose()
        return resultOtp;
    }
}
