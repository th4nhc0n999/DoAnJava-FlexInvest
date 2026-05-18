package View.staff;

import DAO.MissionDAO;
import DAO.TokenDAO;
import Model.Mission;
import Model.UserMission;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * MissionManagementPanel — Admin panel quản lý nhiệm vụ.
 *
 * Tab 1: Danh sách nhiệm vụ (sửa reward token, bật/tắt)
 * Tab 2: Leaderboard token tháng này (top 20)
 */
public class MissionManagementPanel extends JPanel {

    private static final Color NAVY    = new Color(15, 40, 80);
    private static final Color BLUE    = new Color(0, 162, 232);
    private static final Color GREEN   = new Color(16, 185, 129);
    private static final Color PURPLE  = new Color(139, 92, 246);
    private static final Color BG      = new Color(238, 243, 250);
    private static final Color CARD    = Color.WHITE;
    private static final Color MUTED   = new Color(110, 115, 130);
    private static final Color BORDER_C = new Color(210, 220, 235);
    private static final NumberFormat VND = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private final MissionDAO missionDAO = new MissionDAO();
    private final TokenDAO   tokenDAO   = new TokenDAO();

    // ── Tab 1: Missions ─────────────────────────────────────────────────────
    private JTable          missionTable;
    private MissionTableModel missionModel;
    private List<Mission>   missions = new ArrayList<>();

    // ── Tab 2: Leaderboard ──────────────────────────────────────────────────
    private JTable          lbTable;
    private LeaderboardModel lbModel;

    public MissionManagementPanel() {
        setLayout(new BorderLayout());
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
        inner.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header
        JLabel title = new JLabel("🎯  Quản lý Nhiệm vụ & Token");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(NAVY);

        JButton btnRefresh = actionBtn("⟳  Làm mới", BLUE);
        btnRefresh.addActionListener(e -> loadData());

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.WEST);
        header.add(btnRefresh, BorderLayout.EAST);
        inner.add(header, BorderLayout.NORTH);

        // Tabs
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.addTab("📋  Danh sách nhiệm vụ", buildMissionTab());
        tabs.addTab("🏆  Leaderboard Token", buildLeaderboardTab());
        inner.add(tabs, BorderLayout.CENTER);

        add(inner, BorderLayout.CENTER);
    }

    // ── Tab 1: Missions ─────────────────────────────────────────────────────
    private JPanel buildMissionTab() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(12, 0, 0, 0));

        // Action buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRow.setBackground(BG);

        JButton btnEdit = actionBtn("✏  Sửa thưởng", BLUE);
        btnEdit.addActionListener(e -> onEditReward());

        JButton btnToggle = actionBtn("⏸  Bật / Tắt", new Color(245, 158, 11));
        btnToggle.addActionListener(e -> onToggleMission());

        btnRow.add(btnEdit);
        btnRow.add(btnToggle);
        p.add(btnRow, BorderLayout.NORTH);

        // Table
        missionModel = new MissionTableModel();
        missionTable = new JTable(missionModel);
        styleTable(missionTable);
        int[] widths = {50, 200, 120, 90, 80, 90, 80};
        for (int i = 0; i < widths.length && i < missionTable.getColumnCount(); i++)
            missionTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane sp = new JScrollPane(missionTable);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_C));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // ── Tab 2: Leaderboard ──────────────────────────────────────────────────
    private JPanel buildLeaderboardTab() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(12, 0, 0, 0));

        JLabel info = new JLabel("Top 20 user có số dư FlexToken cao nhất hiện tại");
        info.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        info.setForeground(MUTED);
        info.setBorder(new EmptyBorder(0, 0, 8, 0));
        p.add(info, BorderLayout.NORTH);

        lbModel = new LeaderboardModel();
        lbTable = new JTable(lbModel);
        styleTable(lbTable);
        // Rank column center + color
        lbTable.getColumnModel().getColumn(0).setCellRenderer(new RankRenderer());
        lbTable.getColumnModel().getColumn(0).setMaxWidth(50);
        lbTable.getColumnModel().getColumn(2).setPreferredWidth(150);

        JScrollPane sp = new JScrollPane(lbTable);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_C));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // =========================================================================
    //  Data
    // =========================================================================

    public void loadData() {
        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() {
                List<Mission> ms = missionDAO.getAllActive();
                List<int[]>   lb = queryTokenLeaderboard(20);
                return new Object[]{ms, lb};
            }
            @Override protected void done() {
                try {
                    Object[] data = get();
                    missions = (List<Mission>) data[0];
                    missionModel.setData(missions);
                    lbModel.setData((List<int[]>) data[1]);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }.execute();
    }

    /** Query top N user theo TOKEN.balance DESC. */
    private List<int[]> queryTokenLeaderboard(int limit) {
        List<int[]> result = new ArrayList<>();
        String sql = """
            SELECT t.user_id, t.balance, t.total_earned
            FROM TOKEN t
            WHERE t.is_deleted = 0 AND t.status = 'ACTIVE'
            ORDER BY t.balance DESC
            FETCH FIRST ? ROWS ONLY
            """;
        try (var con = ConnectDB.ConnectionOracle.getOracleConnection();
             var ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new int[]{
                        rs.getInt("user_id"),
                        rs.getBigDecimal("balance").intValue(),
                        rs.getBigDecimal("total_earned").intValue()
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("[MissionManagementPanel.queryTokenLeaderboard] " + e.getMessage());
        }
        return result;
    }

    // =========================================================================
    //  Actions
    // =========================================================================

    private void onEditReward() {
        int row = missionTable.getSelectedRow();
        if (row < 0) { warn("Chọn nhiệm vụ cần sửa!"); return; }
        Mission m = missions.get(row);

        String input = JOptionPane.showInputDialog(this,
            "Nhập số token thưởng mới cho: \"" + m.getTitle() + "\"",
            m.getRewardToken() != null ? m.getRewardToken().toPlainString() : "0");
        if (input == null || input.isBlank()) return;

        BigDecimal newReward;
        try { newReward = new BigDecimal(input.trim()); }
        catch (NumberFormatException e) { warn("Số không hợp lệ!"); return; }

        // Update trực tiếp qua SQL (cần thêm method vào MissionDAO trong thực tế)
        // Tạm thời dùng raw JDBC
        boolean ok = updateMissionReward(m.getMissionId(), newReward);
        if (ok) {
            m.setRewardToken(newReward);
            missionModel.fireTableDataChanged();
            JOptionPane.showMessageDialog(this, "✅ Đã cập nhật thưởng thành " + newReward + " Token!");
        } else {
            JOptionPane.showMessageDialog(this, "❌ Cập nhật thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean updateMissionReward(int missionId, BigDecimal reward) {
        String sql = "UPDATE MISSIONS SET reward_token = ? WHERE mission_id = ?";
        try (var con = ConnectDB.ConnectionOracle.getOracleConnection();
             var ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, reward);
            ps.setInt(2, missionId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[MissionManagementPanel.updateMissionReward] " + e.getMessage());
            return false;
        }
    }

    private void onToggleMission() {
        int row = missionTable.getSelectedRow();
        if (row < 0) { warn("Chọn nhiệm vụ cần bật/tắt!"); return; }
        Mission m = missions.get(row);

        int newState = m.getIsActive() == 1 ? 0 : 1;
        String label = newState == 1 ? "kích hoạt" : "tắt";
        int confirm = JOptionPane.showConfirmDialog(this,
            "Bạn muốn " + label + " nhiệm vụ \"" + m.getTitle() + "\"?",
            "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        boolean ok = toggleMissionActive(m.getMissionId(), newState);
        if (ok) {
            m.setIsActive(newState);
            missionModel.fireTableDataChanged();
        } else {
            JOptionPane.showMessageDialog(this, "Cập nhật thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean toggleMissionActive(int missionId, int isActive) {
        String sql = "UPDATE MISSIONS SET is_active = ? WHERE mission_id = ?";
        try (var con = ConnectDB.ConnectionOracle.getOracleConnection();
             var ps = con.prepareStatement(sql)) {
            ps.setInt(1, isActive);
            ps.setInt(2, missionId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[MissionManagementPanel.toggleMissionActive] " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    //  TableModels
    // =========================================================================

    static class MissionTableModel extends AbstractTableModel {
        private static final String[] COLS = {"ID", "Tiêu đề", "Loại", "Thưởng (Token)", "Mục tiêu", "Trạng thái", "Hành động"};
        private List<Mission> rows = new ArrayList<>();

        void setData(List<Mission> data) { rows = data; fireTableDataChanged(); }

        @Override public int getRowCount()    { return rows.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }
        @Override public boolean isCellEditable(int r, int c) { return false; }

        @Override public Object getValueAt(int r, int c) {
            Mission m = rows.get(r);
            return switch (c) {
                case 0 -> m.getMissionId();
                case 1 -> m.getTitle() != null ? m.getTitle() : "—";
                case 2 -> typeLabel(m.getMissionType());
                case 3 -> m.getRewardToken() != null ? m.getRewardToken().toPlainString() : "0";
                case 4 -> m.getTargetValue();
                case 5 -> m.getIsActive() == 1 ? "✅ Active" : "⏸ Inactive";
                case 6 -> m.getActionType() != null ? m.getActionType() : "—";
                default -> "";
            };
        }

        private String typeLabel(String t) {
            if (t == null) return "—";
            return switch (t) {
                case "DAILY"   -> "📅 Hàng ngày";
                case "WEEKLY"  -> "📋 Hàng tuần";
                case "MONTHLY" -> "🗓 Hàng tháng";
                default -> t;
            };
        }
    }

    static class LeaderboardModel extends AbstractTableModel {
        private static final String[] COLS = {"#", "User ID", "Số dư Token", "Tổng đã nhận"};
        private List<int[]> rows = new ArrayList<>();
        private static final NumberFormat FMT = NumberFormat.getNumberInstance(new Locale("vi","VN"));

        void setData(List<int[]> data) { rows = data; fireTableDataChanged(); }

        @Override public int getRowCount()    { return rows.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }
        @Override public boolean isCellEditable(int r, int c) { return false; }

        @Override public Object getValueAt(int r, int c) {
            int[] row = rows.get(r);
            return switch (c) {
                case 0 -> r + 1;
                case 1 -> row[0];
                case 2 -> FMT.format(row[1]);
                case 3 -> FMT.format(row[2]);
                default -> "";
            };
        }
    }

    // ── Rank renderer: top 3 có màu đặc biệt ────────────────────────────────
    private static class RankRenderer extends DefaultTableCellRenderer {
        private static final Color[] RANK_COLORS = {
            new Color(255, 215, 0),   // 🥇 Vàng
            new Color(192, 192, 192), // 🥈 Bạc
            new Color(205, 127, 50),  // 🥉 Đồng
        };
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, val, sel, foc, row, col);
            setHorizontalAlignment(CENTER);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            if (row < 3) {
                setText(new String[]{"🥇", "🥈", "🥉"}[row]);
                setForeground(RANK_COLORS[row]);
            } else {
                setText(String.valueOf(row + 1));
                setForeground(Color.DARK_GRAY);
            }
            return this;
        }
    }

    // =========================================================================
    //  UI helpers
    // =========================================================================

    private void styleTable(JTable t) {
        t.setRowHeight(34);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.getTableHeader().setBackground(new Color(240, 245, 255));
        t.getTableHeader().setForeground(NAVY);
        t.setGridColor(new Color(230, 235, 245));
        t.setShowVerticalLines(false);
        t.setSelectionBackground(new Color(210, 230, 255));
        t.setFillsViewportHeight(true);
        t.setBackground(CARD);
    }

    private JButton actionBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thông báo", JOptionPane.WARNING_MESSAGE);
    }
}
