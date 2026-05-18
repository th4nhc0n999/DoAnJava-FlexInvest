package View;

import Controller.NotificationController;
import Model.Notification;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * NotificationDialog — JDialog dạng dropdown danh sách thông báo.
 * Mở bằng cách bấm icon chuông trong Sidebar.
 *
 * Tính năng:
 *  - Hiển thị danh sách thông báo gần nhất (tối đa 30)
 *  - Phân biệt đã đọc / chưa đọc bằng màu nền
 *  - Nút "Đánh dấu tất cả đã đọc"
 *  - Sau khi đóng, trả về callback để Sidebar cập nhật badge
 */
public class NotificationDialog extends JDialog {

    private static final Color BG       = new Color(245, 247, 252);
    private static final Color CARD_NEW = new Color(235, 245, 255);   // chưa đọc
    private static final Color CARD_OLD = Color.WHITE;                // đã đọc
    private static final Color NAVY     = new Color(15, 40, 80);
    private static final Color BLUE     = new Color(0, 162, 232);
    private static final Color MUTED    = new Color(110, 115, 130);
    private static final Color BADGE    = new Color(239, 68, 68);

    private final int                    userId;
    private final NotificationController notifCtrl;
    private final Runnable               onDismiss;   // callback cập nhật badge

    private JPanel listPanel;

    /**
     * @param parent    parent frame để định vị dialog
     * @param userId    ID user hiện tại
     * @param onDismiss Callback sau khi dialog đóng (để Sidebar cập nhật badge)
     */
    public NotificationDialog(Frame parent, int userId,
                              NotificationController notifCtrl, Runnable onDismiss) {
        super(parent, "Thông báo", false);   // false = non-modal để không block UI
        this.userId    = userId;
        this.notifCtrl = notifCtrl;
        this.onDismiss = onDismiss;

        setSize(420, 540);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                if (onDismiss != null) onDismiss.run();
            }
        });

        build();
        loadNotifications();
    }

    // =========================================================================
    //  Build
    // =========================================================================

    private void build() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG);

        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(new EmptyBorder(14, 18, 14, 18));

        JLabel title = new JLabel("🔔  Thông báo");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);

        JButton btnMarkAll = new JButton("✓  Đánh dấu tất cả đã đọc");
        btnMarkAll.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnMarkAll.setBackground(new Color(40, 80, 140));
        btnMarkAll.setForeground(Color.WHITE);
        btnMarkAll.setBorderPainted(false);
        btnMarkAll.setFocusPainted(false);
        btnMarkAll.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnMarkAll.addActionListener(e -> {
            notifCtrl.markAllRead(userId);
            loadNotifications();
            if (onDismiss != null) onDismiss.run();
        });

        header.add(title,      BorderLayout.WEST);
        header.add(btnMarkAll, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ── Scrollable list ───────────────────────────────────────────────────
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(BG);
        listPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane sp = new JScrollPane(listPanel);
        sp.setBorder(null);
        sp.getViewport().setBackground(BG);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(sp, BorderLayout.CENTER);
    }

    // =========================================================================
    //  Load
    // =========================================================================

    private void loadNotifications() {
        new SwingWorker<List<Notification>, Void>() {
            @Override protected List<Notification> doInBackground() {
                return notifCtrl.getRecent(userId, 30);
            }
            @Override protected void done() {
                try {
                    List<Notification> list = get();
                    renderList(list);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }.execute();
    }

    private void renderList(List<Notification> list) {
        listPanel.removeAll();

        if (list.isEmpty()) {
            JLabel empty = new JLabel("Bạn chưa có thông báo nào.", SwingConstants.CENTER);
            empty.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            empty.setForeground(MUTED);
            empty.setAlignmentX(CENTER_ALIGNMENT);
            listPanel.add(Box.createVerticalStrut(40));
            listPanel.add(empty);
        } else {
            for (Notification n : list) {
                listPanel.add(buildCard(n));
                listPanel.add(Box.createVerticalStrut(6));
            }
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel buildCard(Notification n) {
        boolean unread = n.isUnread();

        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(unread ? CARD_NEW : CARD_OLD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(
                unread ? new Color(180, 215, 250) : new Color(220, 225, 235), 1, true),
            new EmptyBorder(10, 14, 10, 14)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        card.setAlignmentX(LEFT_ALIGNMENT);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel lbTitle = new JLabel(n.getTitle() != null ? n.getTitle() : "(Không tiêu đề)");
        lbTitle.setFont(new Font("Segoe UI", unread ? Font.BOLD : Font.PLAIN, 13));
        lbTitle.setForeground(unread ? NAVY : new Color(60, 70, 90));

        JLabel lbTime = new JLabel(formatTime(n));
        lbTime.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lbTime.setForeground(MUTED);

        top.add(lbTitle, BorderLayout.CENTER);
        top.add(lbTime,  BorderLayout.EAST);

        JLabel lbBody = new JLabel();
        if (n.getBody() != null && !n.getBody().isBlank()) {
            String body = n.getBody().length() > 80
                ? n.getBody().substring(0, 77) + "..." : n.getBody();
            lbBody.setText("<html><span style='color:#6e7382;font-size:11px'>" + body + "</span></html>");
        }

        // Unread dot
        JLabel dot = new JLabel(unread ? "●" : "");
        dot.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        dot.setForeground(BLUE);
        dot.setPreferredSize(new Dimension(14, 14));

        card.add(dot,  BorderLayout.WEST);
        card.add(top,  BorderLayout.CENTER);
        if (n.getBody() != null && !n.getBody().isBlank())
            card.add(lbBody, BorderLayout.SOUTH);

        // Click → mark read
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (unread) {
                    notifCtrl.markRead(n.getNotificationId());
                    loadNotifications();
                    if (onDismiss != null) onDismiss.run();
                }
            }
        });

        return card;
    }

    private String formatTime(Notification n) {
        if (n.getSentAt() == null) return "";
        try {
            return n.getSentAt().toLocalDateTime()
                .format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
        } catch (Exception e) { return ""; }
    }

    // =========================================================================
    //  Static factory — mở dialog cạnh icon chuông
    // =========================================================================

    /**
     * Mở dialog cạnh component nguồn (icon chuông).
     * Dùng trong Sidebar: NotificationDialog.open(frame, bellBtn, userId, ctrl, onDismiss);
     */
    public static void open(Frame frame, Component anchor,
                            int userId, NotificationController ctrl,
                            Runnable onDismiss) {
        NotificationDialog dlg = new NotificationDialog(frame, userId, ctrl, onDismiss);

        // Định vị dialog sát icon chuông
        Point loc = anchor.getLocationOnScreen();
        int x = loc.x - dlg.getWidth() + anchor.getWidth();
        int y = loc.y + anchor.getHeight() + 4;

        // Đảm bảo không bị cắt ngoài màn hình
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        if (x + dlg.getWidth() > screen.width) x = screen.width - dlg.getWidth() - 8;
        if (y + dlg.getHeight() > screen.height) y = loc.y - dlg.getHeight() - 4;

        dlg.setLocation(x, y);
        dlg.setVisible(true);
    }
}
