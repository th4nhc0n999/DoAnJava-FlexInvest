package View.customer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Reusable OTP dialog — shows 6 individual digit boxes.
 * Call showDialog() which blocks and returns the entered code,
 * or null if the user cancelled.
 */
public class OtpDialog extends JDialog {

    private final JTextField[] digitFields = new JTextField[6];
    private String result = null;

    private OtpDialog(Frame owner, String title, String subtitle) {
        super(owner, title, true);
        buildUI(subtitle);
        setSize(420, 300);
        setLocationRelativeTo(owner);
        setResizable(false);
        getContentPane().setBackground(UITheme.BG_DARK);
    }

    private void buildUI(String subtitle) {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(UITheme.BG_DARK);
        root.setBorder(new EmptyBorder(28, 32, 28, 32));
        setContentPane(root);

        // Title area
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 0, 6));
        topPanel.setOpaque(false);
        JLabel titleLbl = UITheme.label(getTitle(), UITheme.FONT_HEAD, UITheme.TEXT_PRIMARY);
        JLabel subLbl = UITheme.label(subtitle, UITheme.FONT_BODY, UITheme.TEXT_MUTED);
        topPanel.add(titleLbl);
        topPanel.add(subLbl);
        root.add(topPanel, BorderLayout.NORTH);

        // Digit input row
        JPanel digitsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        digitsPanel.setOpaque(false);
        for (int i = 0; i < 6; i++) {
            JTextField tf = createDigitField();
            digitFields[i] = tf;
            final int idx = i;
            tf.addKeyListener(new KeyAdapter() {
                @Override public void keyTyped(KeyEvent e) {
                    char c = e.getKeyChar();
                    if (!Character.isDigit(c)) { e.consume(); return; }
                    tf.setText(String.valueOf(c));
                    e.consume();
                    if (idx < 5) digitFields[idx + 1].requestFocus();
                    else confirmIfComplete();
                }
                @Override public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                        if (tf.getText().isEmpty() && idx > 0) {
                            digitFields[idx - 1].requestFocus();
                            digitFields[idx - 1].setText("");
                        } else {
                            tf.setText("");
                        }
                    }
                }
            });
            digitsPanel.add(tf);
        }

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.add(digitsPanel);
        root.add(centerWrapper, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        btnPanel.setOpaque(false);

        JButton confirmBtn = UITheme.primaryButton("Xác nhận");
        confirmBtn.addActionListener(e -> confirmIfComplete());

        JButton cancelBtn = UITheme.ghostButton("Hủy");
        cancelBtn.addActionListener(e -> { result = null; dispose(); });

        btnPanel.add(cancelBtn);
        btnPanel.add(confirmBtn);
        root.add(btnPanel, BorderLayout.SOUTH);

        // ESC closes
        getRootPane().registerKeyboardAction(
                e -> { result = null; dispose(); },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private JTextField createDigitField() {
        JTextField tf = new JTextField(1) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(isFocusOwner() ? UITheme.ACCENT_TEAL : UITheme.BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tf.setHorizontalAlignment(JTextField.CENTER);
        tf.setFont(new Font("Monospaced", Font.BOLD, 24));
        tf.setForeground(UITheme.TEXT_PRIMARY);
        tf.setCaretColor(UITheme.ACCENT_TEAL);
        tf.setOpaque(false);
        tf.setBorder(new EmptyBorder(4, 4, 4, 4));
        tf.setPreferredSize(new Dimension(46, 56));
        return tf;
    }

    private void confirmIfComplete() {
        StringBuilder sb = new StringBuilder();
        for (JTextField df : digitFields) sb.append(df.getText().trim());
        if (sb.length() == 6 && sb.toString().matches("\\d{6}")) {
            result = sb.toString();
            dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập đủ 6 chữ số.", "Thông báo",
                    JOptionPane.WARNING_MESSAGE);
            digitFields[0].requestFocus();
        }
    }

    // ---- Public API ----

    /**
     * Shows the OTP dialog and blocks until the user confirms or cancels.
     *
     * @param owner    parent frame
     * @param title    dialog title
     * @param subtitle instruction text shown below the title
     * @return the 6-digit string if confirmed, or null if cancelled
     */
    public static String showDialog(Frame owner, String title, String subtitle) {
        OtpDialog dlg = new OtpDialog(owner, title, subtitle);
        dlg.setVisible(true);   // blocks
        return dlg.result;
    }
}
