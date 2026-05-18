package View.customer;

import DAO.MissionDAO;
import DAO.TokenDAO;
import DAO.WalletDAO;
import Model.AccountModel;
import Model.Mission;
import Model.UserMission;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * MissionPanel — 3 tab: Điểm danh / Nhiệm vụ tuần / Nhiệm vụ tháng.
 * Phần FlexToken: hiển thị số dư + nút "Chuyển đổi token".
 *
 * Phụ thuộc vào MissionDAO (N4 đã implement đầy đủ).
 */
public class MissionPanel extends JPanel {

    private static final Color NAVY    = new Color(15, 40, 80);
    private static final Color BLUE    = new Color(0, 162, 232);
    private static final Color GREEN   = new Color(16, 185, 129);
    private static final Color ORANGE  = new Color(245, 158, 11);
    private static final Color PURPLE  = new Color(139, 92, 246);
    private static final Color BG      = new Color(238, 243, 250);
    private static final Color CARD    = Color.WHITE;
    private static final Color MUTED   = new Color(110, 115, 130);
    private static final NumberFormat VND = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private final AccountModel account;
    private final int          userId;
    private final MissionDAO   missionDAO = new MissionDAO();
    private final TokenDAO     tokenDAO   = new TokenDAO();
    private final WalletDAO    walletDAO  = new WalletDAO();

    // ── Token header ─────────────────────────────────────────────────────────
    private JLabel lblTokenBalance;
    private JButton btnConvert;

    // ── Check-in cells (7 ô) ─────────────────────────────────────────────────
    private JPanel[] checkInCells = new JPanel[7];
    private JLabel[] checkInLabels = new JLabel[7];
    private JButton  btnCheckIn;

    public MissionPanel(AccountModel account) {
        this.account = account;
        this.userId  = account.getUser().getUserId();
        setLayout(new BorderLayout(0, 16));
        setBackground(BG);
        build();
        loadData();
    }

    // =========================================================================
    //  Build
    // =========================================================================

    private void build() {
        JPanel inner = new JPanel(new BorderLayout(0, 16));
        inner.setBackground(BG);
        inner.setBorder(new EmptyBorder(24, 28, 24, 28));

        inner.add(buildTokenHeader(),  BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.setBackground(CARD);
        tabs.addTab("📅  Điểm danh",     buildCheckInTab());
        tabs.addTab("📋  Nhiệm vụ tuần", buildMissionTab("WEEKLY"));
        tabs.addTab("🗓  Nhiệm vụ tháng", buildMissionTab("MONTHLY"));
        inner.add(tabs, BorderLayout.CENTER);

        add(inner, BorderLayout.CENTER);
    }

    // ── Token header card ────────────────────────────────────────────────────
    private JPanel buildTokenHeader() {
        JPanel card = new JPanel(new BorderLayout(16, 0));
        card.setBackground(PURPLE);
        card.setBorder(new EmptyBorder(18, 24, 18, 24));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel lbl = new JLabel("FlexToken của bạn");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(new Color(210, 195, 255));

        lblTokenBalance = new JLabel("...");
        lblTokenBalance.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTokenBalance.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Token hợp lệ trong năm dương lịch hiện tại");
        sub.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        sub.setForeground(new Color(200, 185, 255));

        left.add(lbl);
        left.add(Box.createVerticalStrut(3));
        left.add(lblTokenBalance);
        left.add(Box.createVerticalStrut(2));
        left.add(sub);

        btnConvert = new JButton("🔄  Chuyển đổi Token");
        btnConvert.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnConvert.setBackground(new Color(100, 60, 200));
        btnConvert.setForeground(Color.WHITE);
        btnConvert.setBorderPainted(false);
        btnConvert.setFocusPainted(false);
        btnConvert.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnConvert.addActionListener(e -> showConvertDialog());

        card.add(left,       BorderLayout.CENTER);
        card.add(btnConvert, BorderLayout.EAST);
        return card;
    }

    // ── Tab Điểm danh ────────────────────────────────────────────────────────
    private JPanel buildCheckInTab() {
        JPanel p = new JPanel(new BorderLayout(0, 20));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Title
        JLabel title = new JLabel("Chuỗi điểm danh liên tiếp 7 ngày");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(NAVY);
        p.add(title, BorderLayout.NORTH);

        // 7 ô điểm danh
        JPanel grid = new JPanel(new GridLayout(1, 7, 10, 0));
        grid.setBackground(BG);
        String[] dayNames = {"Ngày 1", "Ngày 2", "Ngày 3", "Ngày 4", "Ngày 5", "Ngày 6", "Ngày 7"};
        for (int i = 0; i < 7; i++) {
            JPanel cell = new JPanel(new BorderLayout());
            cell.setBackground(CARD);
            cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 220, 235), 2, true),
                new EmptyBorder(12, 8, 12, 8)));

            JLabel top = new JLabel(dayNames[i], SwingConstants.CENTER);
            top.setFont(new Font("Segoe UI", Font.BOLD, 12));
            top.setForeground(MUTED);

            JLabel icon = new JLabel("○", SwingConstants.CENTER);
            icon.setFont(new Font("Segoe UI", Font.PLAIN, 28));
            icon.setForeground(new Color(200, 210, 225));

            cell.add(top,  BorderLayout.NORTH);
            cell.add(icon, BorderLayout.CENTER);
            checkInCells[i]  = cell;
            checkInLabels[i] = icon;
            grid.add(cell);
        }
        p.add(grid, BorderLayout.CENTER);

        // Nút điểm danh
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 12));
        bottom.setBackground(BG);

        btnCheckIn = new JButton("✅  Điểm danh ngay");
        btnCheckIn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCheckIn.setBackground(GREEN);
        btnCheckIn.setForeground(Color.WHITE);
        btnCheckIn.setBorderPainted(false);
        btnCheckIn.setFocusPainted(false);
        btnCheckIn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCheckIn.setPreferredSize(new Dimension(220, 46));
        btnCheckIn.addActionListener(e -> onCheckIn());
        bottom.add(btnCheckIn);
        p.add(bottom, BorderLayout.SOUTH);

        return p;
    }

    // ── Tab nhiệm vụ (dùng chung cho WEEKLY và MONTHLY) ─────────────────────
    private JPanel buildMissionTab(String type) {
        // Panel placeholder — nội dung được điền động khi loadData()
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setName(type);  // đánh dấu để loadData() tìm đúng tab
        p.add(new JLabel("Đang tải...", SwingConstants.CENTER), BorderLayout.CENTER);
        return p;
    }

    // =========================================================================
    //  Data loading
    // =========================================================================

    public void loadData() {
        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() {
                BigDecimal tokenBal  = tokenDAO.getBalance(userId);
                int streak           = missionDAO.getCheckInStreak(userId);
                List<UserMission> all = missionDAO.getUserMissions(userId);
                return new Object[]{tokenBal, streak, all};
            }
            @Override protected void done() {
                try {
                    Object[] data = get();
                    BigDecimal tokenBal   = (BigDecimal) data[0];
                    int streak            = (Integer)    data[1];
                    List<UserMission> all = (List<UserMission>) data[2];

                    lblTokenBalance.setText(VND.format(tokenBal) + " Token");
                    updateCheckInUI(streak);
                    updateMissionTabs(all);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }.execute();
    }

    private void updateCheckInUI(int streak) {
        for (int i = 0; i < 7; i++) {
            boolean done = i < streak;
            boolean today = i == streak && streak < 7;

            checkInCells[i].setBackground(done ? new Color(220, 250, 235) : CARD);
            checkInCells[i].setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    done  ? GREEN :
                    today ? ORANGE :
                    new Color(210, 220, 235), 2, true),
                new EmptyBorder(12, 8, 12, 8)));

            checkInLabels[i].setText(done ? "✔" : (today ? "☆" : "○"));
            checkInLabels[i].setForeground(
                done  ? GREEN  :
                today ? ORANGE :
                new Color(200, 210, 225));
        }

        boolean checkedToday = streak > 0 && alreadyCheckedInToday();
        btnCheckIn.setEnabled(!checkedToday);
        btnCheckIn.setText(checkedToday ? "✅  Đã điểm danh hôm nay" : "✅  Điểm danh ngay");
        btnCheckIn.setBackground(checkedToday ? new Color(100, 190, 140) : GREEN);
    }

    private boolean alreadyCheckedInToday() {
        // Dùng getCheckInStreak để gián tiếp kiểm tra —
        // nếu claimedAt hôm nay thì MissionDAO.checkIn() sẽ trả về false.
        // Đây là best-effort UI check, không cần query thêm.
        return false; // sẽ cập nhật sau khi gọi checkIn
    }

    private void updateMissionTabs(List<UserMission> allMissions) {
        JTabbedPane tabs = findTabbedPane();
        if (tabs == null) return;

        // Rebuild tab 1 (WEEKLY) và tab 2 (MONTHLY)
        for (int t = 1; t <= 2; t++) {
            String type = t == 1 ? "WEEKLY" : "MONTHLY";
            List<UserMission> filtered = allMissions.stream()
                .filter(um -> um.getMission() != null
                           && type.equals(um.getMission().getMissionType()))
                .collect(Collectors.toList());
            tabs.setComponentAt(t, buildMissionListPanel(filtered));
        }
    }

    private JPanel buildMissionListPanel(List<UserMission> missions) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        if (missions.isEmpty()) {
            JLabel empty = new JLabel("Chưa có nhiệm vụ nào. Hãy đăng ký từ Admin!", SwingConstants.CENTER);
            empty.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            empty.setForeground(MUTED);
            empty.setAlignmentX(CENTER_ALIGNMENT);
            p.add(empty);
            return p;
        }

        for (UserMission um : missions) {
            p.add(buildMissionCard(um));
            p.add(Box.createVerticalStrut(12));
        }

        JScrollPane sp = new JScrollPane(p);
        sp.setBorder(null);
        sp.getViewport().setBackground(BG);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.add(sp, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildMissionCard(UserMission um) {
        Mission m = um.getMission();
        if (m == null) return new JPanel();

        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 220, 235), 1, true),
            new EmptyBorder(14, 16, 14, 16)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        card.setAlignmentX(LEFT_ALIGNMENT);

        // Left: title + description
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel lbTitle = new JLabel(m.getTitle() != null ? m.getTitle() : "Nhiệm vụ #" + m.getMissionId());
        lbTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbTitle.setForeground(NAVY);

        JLabel lbDesc = new JLabel(m.getDescription() != null ? m.getDescription() : "");
        lbDesc.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbDesc.setForeground(MUTED);

        // Progress bar
        int progress = um.getProgress();
        int target   = m.getTargetValue();
        JProgressBar bar = new JProgressBar(0, Math.max(target, 1));
        bar.setValue(Math.min(progress, target));
        bar.setString(progress + " / " + target);
        bar.setStringPainted(true);
        bar.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        bar.setForeground(um.isCompleted() || um.isClaimed() ? GREEN : BLUE);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        left.add(lbTitle);
        left.add(Box.createVerticalStrut(3));
        left.add(lbDesc);
        left.add(Box.createVerticalStrut(6));
        left.add(bar);

        // Right: reward + claim button
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        JLabel lbReward = new JLabel("+" + VND.format(m.getRewardToken()) + " Token");
        lbReward.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbReward.setForeground(PURPLE);
        lbReward.setAlignmentX(CENTER_ALIGNMENT);

        if (um.isCompleted()) {
            JButton btnClaim = new JButton("🎁  Nhận thưởng");
            btnClaim.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnClaim.setBackground(ORANGE);
            btnClaim.setForeground(Color.WHITE);
            btnClaim.setBorderPainted(false);
            btnClaim.setFocusPainted(false);
            btnClaim.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnClaim.addActionListener(e -> onClaim(um.getMissionId(), btnClaim));
            right.add(lbReward);
            right.add(Box.createVerticalStrut(6));
            right.add(btnClaim);
        } else if (um.isClaimed()) {
            JLabel lbClaimed = new JLabel("✅  Đã nhận");
            lbClaimed.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lbClaimed.setForeground(GREEN);
            right.add(lbReward);
            right.add(lbClaimed);
        } else {
            right.add(lbReward);
        }

        card.add(left,  BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    // =========================================================================
    //  Actions
    // =========================================================================

    private void onCheckIn() {
        btnCheckIn.setEnabled(false);
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                return missionDAO.checkIn(userId);
            }
            @Override protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        int streak = missionDAO.getCheckInStreak(userId);
                        BigDecimal bal = tokenDAO.getBalance(userId);
                        lblTokenBalance.setText(VND.format(bal) + " Token");
                        updateCheckInUI(streak);
                        btnCheckIn.setEnabled(false);
                        btnCheckIn.setText("✅  Đã điểm danh hôm nay");
                        btnCheckIn.setBackground(new Color(100, 190, 140));
                        JOptionPane.showMessageDialog(MissionPanel.this,
                            "Điểm danh thành công! Streak hiện tại: " + streak + " ngày.",
                            "Điểm danh ✅", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(MissionPanel.this,
                            "Bạn đã điểm danh hôm nay rồi!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                        btnCheckIn.setEnabled(false);
                        btnCheckIn.setText("✅  Đã điểm danh hôm nay");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    btnCheckIn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void onClaim(int missionId, JButton btn) {
        btn.setEnabled(false);
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                return missionDAO.claimReward(userId, missionId);
            }
            @Override protected void done() {
                try {
                    if (get()) {
                        BigDecimal bal = tokenDAO.getBalance(userId);
                        lblTokenBalance.setText(VND.format(bal) + " Token");
                        btn.setText("✅  Đã nhận");
                        btn.setBackground(GREEN);
                        loadData(); // refresh toàn panel
                    } else {
                        JOptionPane.showMessageDialog(MissionPanel.this,
                            "Nhận thưởng thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        btn.setEnabled(true);
                    }
                } catch (Exception ex) { ex.printStackTrace(); btn.setEnabled(true); }
            }
        }.execute();
    }

    // ── Dialog chuyển đổi token ───────────────────────────────────────────────
    private void showConvertDialog() {
        BigDecimal balance = tokenDAO.getBalance(userId);

        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            "Chuyển đổi FlexToken", true);
        dlg.setSize(440, 340);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(24, 28, 24, 28));
        content.setBackground(CARD);

        JLabel lblBal = new JLabel("Số dư hiện tại: " + VND.format(balance) + " Token");
        lblBal.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblBal.setForeground(PURPLE);
        lblBal.setAlignmentX(LEFT_ALIGNMENT);
        content.add(lblBal);
        content.add(Box.createVerticalStrut(20));

        // PT1: chuyển sang VNĐ
        JPanel opt1 = optionCard(
            "💰  Phương thức 1 — Đổi sang VNĐ",
            "Tỉ lệ: 1 Token = 10 VNĐ\nSẽ nhận: " + VND.format(balance.multiply(BigDecimal.TEN)) + " VNĐ",
            BLUE);
        opt1.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        opt1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                dlg.dispose();
                onConvertToVnd(balance);
            }
        });
        content.add(opt1);
        content.add(Box.createVerticalStrut(12));

        // PT2: mua gói VIP
        JPanel opt2 = optionCard(
            "🏆  Phương thức 2 — Mua gói VIP bằng Token",
            "Dùng token để đăng ký gói FlexToken VIP 12 tháng (lãi 12%/năm).",
            PURPLE);
        opt2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        opt2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                dlg.dispose();
                onConvertToVip();
            }
        });
        content.add(opt2);

        dlg.add(content, BorderLayout.CENTER);
        dlg.setVisible(true);
    }

    private void onConvertToVnd(BigDecimal tokenBalance) {
        BigDecimal vnd = tokenBalance.multiply(BigDecimal.TEN);
        int confirm = JOptionPane.showConfirmDialog(this,
            String.format("<html>Bạn sẽ đổi <b>%s Token</b> → <b>%s VNĐ</b>?<br/>" +
                "Token sẽ bị trừ toàn bộ sau khi xác nhận.</html>",
                VND.format(tokenBalance), VND.format(vnd)),
            "Xác nhận đổi Token", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                // 1. Trừ token
                boolean deducted = tokenDAO.deductToken(userId, tokenBalance);
                if (!deducted) return false;
                // 2. Credit VNĐ vào ví
                var wallet = walletDAO.getByUserId(userId);
                if (wallet == null) return false;
                int txId = walletDAO.credit(
                    wallet.getWalletId(), "BONUS", vnd,
                    "Đổi " + tokenBalance.toPlainString() + " FlexToken sang VNĐ");
                return txId > 0;
            }
            @Override protected void done() {
                try {
                    if (get()) {
                        BigDecimal bal = tokenDAO.getBalance(userId);
                        lblTokenBalance.setText(VND.format(bal) + " Token");
                        JOptionPane.showMessageDialog(MissionPanel.this,
                            "✅ Đổi Token thành công!\nĐã nhận " + VND.format(vnd) + " VNĐ vào ví.",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(MissionPanel.this, "Đổi Token thất bại!",
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }.execute();
    }

    private void onConvertToVip() {
        JOptionPane.showMessageDialog(this,
            "<html>Chức năng mua gói VIP bằng Token sẽ do<br/>" +
            "<b>TokenController</b> xử lý.<br/>" +
            "Vui lòng liên hệ nhân viên hoặc chờ tính năng được triển khai.</html>",
            "PT2 — Mua gói VIP", JOptionPane.INFORMATION_MESSAGE);
    }

    // =========================================================================
    //  Helper finders
    // =========================================================================

    private JTabbedPane findTabbedPane() {
        for (Component c : getComponents()) {
            if (c instanceof JPanel outer) {
                for (Component cc : outer.getComponents()) {
                    if (cc instanceof JTabbedPane tp) return tp;
                }
            }
        }
        return null;
    }

    // =========================================================================
    //  UI builders
    // =========================================================================

    private JPanel optionCard(String title, String desc, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 15));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accent, 2, true),
            new EmptyBorder(14, 16, 14, 16)));
        card.setAlignmentX(LEFT_ALIGNMENT);

        JLabel lTitle = new JLabel(title);
        lTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lTitle.setForeground(accent.darker());

        JLabel lDesc = new JLabel("<html>" + desc.replace("\n", "<br/>") + "</html>");
        lDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lDesc.setForeground(MUTED);

        card.add(lTitle, BorderLayout.NORTH);
        card.add(lDesc,  BorderLayout.CENTER);
        return card;
    }
}
