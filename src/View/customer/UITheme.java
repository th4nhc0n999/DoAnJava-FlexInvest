package View.customer;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Centralised design tokens for the customer-facing UI.
 * Inspired by a clean fintech aesthetic: deep navy background,
 * teal accent, warm gold for highlights.
 */
public final class UITheme {

    private UITheme() {}

    // Palette
    public static final Color BG_DARK       = new Color(0x0F1923);   // main background
    public static final Color BG_CARD       = new Color(0x192534);   // card/panel bg
    public static final Color BG_CARD_HOVER = new Color(0x1F3048);   // hover
    public static final Color ACCENT_TEAL   = new Color(0x00C4A7);   // primary action
    public static final Color ACCENT_GOLD   = new Color(0xF5A623);   // highlight / warning
    public static final Color ACCENT_RED    = new Color(0xE8404A);   // danger
    public static final Color TEXT_PRIMARY  = new Color(0xF0F4F8);   // main text
    public static final Color TEXT_MUTED    = new Color(0x7A92A9);   // secondary text
    public static final Color BORDER_COLOR  = new Color(0x243447);   // subtle borders
    public static final Color SUCCESS_GREEN = new Color(0x27AE60);

    // Badge colors
    public static final Color BADGE_BG      = new Color(0xF5A623);
    public static final Color BADGE_FG      = new Color(0x0F1923);

    // Fonts
    public static final Font FONT_TITLE  = new Font("SansSerif", Font.BOLD, 22);
    public static final Font FONT_HEAD   = new Font("SansSerif", Font.BOLD, 15);
    public static final Font FONT_BODY   = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_SMALL  = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_MONO   = new Font("Monospaced", Font.PLAIN, 13);
    public static final Font FONT_BADGE  = new Font("SansSerif", Font.BOLD, 10);

    // ---- factory helpers ----

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        l.setOpaque(false);
        return l;
    }

    public static JButton primaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) g2.setColor(ACCENT_TEAL.darker());
                else if (getModel().isRollover()) g2.setColor(ACCENT_TEAL.brighter());
                else g2.setColor(ACCENT_TEAL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_HEAD);
        btn.setForeground(BG_DARK);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 22, 8, 22));
        btn.setOpaque(false);
        return btn;
    }

    public static JButton dangerButton(String text) {
        JButton btn = primaryButton(text);
        // Override paint to use red
        btn.setForeground(Color.WHITE);
        btn.setBackground(ACCENT_RED);  // used as fallback
        return btn;
    }

    public static JButton ghostButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY);
        btn.setForeground(ACCENT_TEAL);
        btn.setBackground(new Color(0, 0, 0, 0));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_TEAL, 1, true),
                new EmptyBorder(6, 16, 6, 16)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** Dark card panel with rounded feel (paint rounded rect). */
    public static JPanel cardPanel() {
        return new JPanel() {
            { setOpaque(false); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g2.dispose();
            }
        };
    }

    /** Apply dark styling to a JTable. */
    public static void styleTable(JTable table) {
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_PRIMARY);
        table.setFont(FONT_BODY);
        table.setRowHeight(36);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(BG_CARD_HOVER);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.getTableHeader().setBackground(BG_DARK);
        table.getTableHeader().setForeground(TEXT_MUTED);
        table.getTableHeader().setFont(FONT_SMALL);
        table.getTableHeader().setBorder(BorderFactory.createEmptyBorder());
    }

    /** Style a JScrollPane to match dark theme. */
    public static JScrollPane darkScroll(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(BG_CARD);
        sp.setBackground(BG_CARD);
        return sp;
    }
}
