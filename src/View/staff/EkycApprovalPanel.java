package View.staff;

import Controller.EkycController;
import Model.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * EkycApprovalPanel — bảng duyệt hồ sơ eKYC PENDING.
 *
 * Cột: ID / Họ tên / CMND / Giới tính / Ngày sinh / Trạng thái / Xem ảnh / Duyệt / Từ chối
 * - Nút "Xem ảnh" → mở dialog hiển thị ảnh CCCD mặt trước/sau + ảnh selfie
 * - Nút Duyệt  → EkycController.approveEkyc()
 * - Nút Từ chối → dialog nhập lý do → EkycController.rejectEkyc()
 */
public class EkycApprovalPanel extends JPanel {

    private static final Color NAVY      = new Color(15, 40, 80);
    private static final Color BLUE      = new Color(0, 162, 232);
    private static final Color GREEN     = new Color(16, 185, 129);
    private static final Color RED       = new Color(239, 68, 68);
    private static final Color BG        = new Color(238, 243, 250);
    private static final Color CARD_BG   = Color.WHITE;
    private static final Color TEXT_DARK = new Color(30, 30, 40);
    private static final Color TEXT_MUTED = new Color(110, 115, 130);

    private final EkycController ctrl = new EkycController();

    private List<Ekyc>    kycList;
    private KycTableModel tableModel;
    private JTable        table;
    private JLabel        lblStatus;
    private JButton       btnRefresh;

    public EkycApprovalPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);
        build();
        loadData();
    }

    // =========================================================================
    //  Build UI
    // =========================================================================

    private void build() {
        JPanel inner = new JPanel(new BorderLayout(0, 14));
        inner.setBackground(BG);
        inner.setBorder(new EmptyBorder(24, 28, 24, 28));

        // ── Header ─────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Duyệt Hồ Sơ eKYC");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(NAVY);
        JLabel sub = new JLabel("Xem xét ảnh CCCD và selfie trước khi phê duyệt");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(TEXT_MUTED);
        titles.add(title);
        titles.add(sub);

        btnRefresh = makeRefreshBtn();
        btnRefresh.addActionListener(e -> loadData());
        header.add(titles, BorderLayout.WEST);
        header.add(btnRefresh, BorderLayout.EAST);
        inner.add(header, BorderLayout.NORTH);

        // ── Table ──────────────────────────────────────────────────────────
        tableModel = new KycTableModel();
        table = new JTable(tableModel);
        styleTable(table);

        // Column widths
        int[] widths = {55, 160, 120, 70, 100, 100, 80, 75, 80};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Renderers & editors
        table.getColumnModel().getColumn(6).setCellRenderer(new ViewImgRenderer());
        table.getColumnModel().getColumn(6).setCellEditor(new ViewImgEditor());
        table.getColumnModel().getColumn(7).setCellRenderer(new GreenBtnRenderer("Duyệt"));
        table.getColumnModel().getColumn(7).setCellEditor(new ApproveEditor());
        table.getColumnModel().getColumn(8).setCellRenderer(new RedBtnRenderer("Từ chối"));
        table.getColumnModel().getColumn(8).setCellEditor(new RejectEditor());

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(new Color(210, 220, 235), 1));
        sp.getViewport().setBackground(CARD_BG);
        inner.add(sp, BorderLayout.CENTER);

        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblStatus.setForeground(TEXT_MUTED);
        inner.add(lblStatus, BorderLayout.SOUTH);

        add(inner, BorderLayout.CENTER);
    }

    // =========================================================================
    //  Data Loading
    // =========================================================================

    public void loadData() {
        btnRefresh.setEnabled(false);
        btnRefresh.setText("⟳  Đang tải…");
        new SwingWorker<List<Ekyc>, Void>() {
            @Override protected List<Ekyc> doInBackground() {
                return ctrl.getPendingKyc();
            }
            @Override protected void done() {
                try {
                    kycList = get();
                    tableModel.setData(kycList);
                    lblStatus.setText(kycList.size() + " hồ sơ PENDING — "
                        + java.time.LocalTime.now().withNano(0));
                } catch (Exception ex) { ex.printStackTrace();
                } finally {
                    btnRefresh.setEnabled(true);
                    btnRefresh.setText("⟳  Làm mới");
                }
            }
        }.execute();
    }

    // =========================================================================
    //  Actions
    // =========================================================================

    /** Mở dialog xem ảnh CCCD mặt trước / sau + selfie từ đường dẫn file. */
    private void onViewImages(int row) {
        if (kycList == null || row >= kycList.size()) return;
        Ekyc kyc = kycList.get(row);

        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            "Ảnh KYC — " + kyc.getFullName(), true);
        dlg.setSize(860, 520);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(12, 12));

        // 3 panel ảnh: mặt trước, mặt sau, selfie
        JPanel imgRow = new JPanel(new GridLayout(1, 3, 12, 0));
        imgRow.setBorder(new EmptyBorder(16, 16, 8, 16));
        imgRow.setBackground(new Color(30, 35, 50));

        imgRow.add(buildImagePanel("Mặt trước CCCD", kyc.getFrontImageUrl()));
        imgRow.add(buildImagePanel("Mặt sau CCCD",   kyc.getBackImageUrl()));
        imgRow.add(buildImagePanel("Ảnh selfie",      kyc.getFaceImageUrl()));

        // Info footer
        JPanel info = new JPanel(new GridLayout(2, 4, 10, 6));
        info.setBackground(new Color(245, 248, 252));
        info.setBorder(new EmptyBorder(10, 16, 10, 16));
        info.add(infoCell("Họ tên:", kyc.getFullName()));
        info.add(infoCell("Số CMND:", kyc.getIdNumber()));
        info.add(infoCell("Ngày sinh:", kyc.getDateOfBirth() != null ? kyc.getDateOfBirth().toString() : "—"));
        info.add(infoCell("Giới tính:", kyc.getGender()));
        info.add(infoCell("Nơi thường trú:", kyc.getPlaceOfResidence()));
        info.add(infoCell("Nơi gốc:", kyc.getPlaceOfOrigin()));
        info.add(infoCell("Ngày cấp:", kyc.getIssueDate() != null ? kyc.getIssueDate().toString() : "—"));
        info.add(infoCell("Nơi cấp:", kyc.getIssuePlace()));

        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnClose.addActionListener(e -> dlg.dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnClose);

        dlg.add(imgRow, BorderLayout.CENTER);
        dlg.add(info,   BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private JPanel buildImagePanel(String label, String path) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(new Color(40, 44, 60));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(180, 200, 230));
        p.add(lbl, BorderLayout.NORTH);

        JLabel imgLbl;
        if (path != null && !path.isBlank()) {
            File f = new File(path);
            if (f.exists()) {
                ImageIcon raw = new ImageIcon(path);
                Image scaled = raw.getImage().getScaledInstance(240, 160, Image.SCALE_SMOOTH);
                imgLbl = new JLabel(new ImageIcon(scaled), SwingConstants.CENTER);
            } else {
                imgLbl = new JLabel("⚠ Không tìm thấy file:\n" + path, SwingConstants.CENTER);
                imgLbl.setForeground(new Color(239, 100, 100));
                imgLbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            }
        } else {
            imgLbl = new JLabel("Không có ảnh", SwingConstants.CENTER);
            imgLbl.setForeground(new Color(150, 155, 170));
        }
        p.add(imgLbl, BorderLayout.CENTER);
        return p;
    }

    private JPanel infoCell(String key, String val) {
        JPanel p = new JPanel(new BorderLayout(4, 0));
        p.setOpaque(false);
        JLabel k = new JLabel(key);
        k.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        k.setForeground(TEXT_MUTED);
        JLabel v = new JLabel(val != null ? val : "—");
        v.setFont(new Font("Segoe UI", Font.BOLD, 12));
        v.setForeground(TEXT_DARK);
        p.add(k, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    private void onApprove(int row) {
        if (kycList == null || row >= kycList.size()) return;
        Ekyc kyc = kycList.get(row);

        int c = JOptionPane.showConfirmDialog(this,
            "<html>Xác nhận phê duyệt eKYC cho <b>" + kyc.getFullName() + "</b>?</html>",
            "Duyệt eKYC", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (c != JOptionPane.YES_OPTION) return;

        new SwingWorker<EkycController.Result, Void>() {
            @Override protected EkycController.Result doInBackground() {
                return ctrl.approveEkyc(kyc.getKycId());
            }
            @Override protected void done() {
                try {
                    EkycController.Result r = get();
                    if (r == EkycController.Result.SUCCESS) {
                        JOptionPane.showMessageDialog(EkycApprovalPanel.this,
                            "✅ eKYC của " + kyc.getFullName() + " đã được phê duyệt.",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(EkycApprovalPanel.this,
                            "Lỗi: " + r, "Thất bại", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
                finally { loadData(); }
            }
        }.execute();
    }

    private void onReject(int row) {
        if (kycList == null || row >= kycList.size()) return;
        Ekyc kyc = kycList.get(row);

        JPanel form = new JPanel(new BorderLayout(0, 8));
        form.add(new JLabel("Lý do từ chối hồ sơ của " + kyc.getFullName() + ":"), BorderLayout.NORTH);
        JTextArea txt = new JTextArea(4, 32);
        txt.setLineWrap(true); txt.setWrapStyleWord(true);
        form.add(new JScrollPane(txt), BorderLayout.CENTER);

        int opt = JOptionPane.showConfirmDialog(this, form,
            "Từ chối eKYC", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;
        String reason = txt.getText().trim();

        new SwingWorker<EkycController.Result, Void>() {
            @Override protected EkycController.Result doInBackground() {
                return ctrl.rejectEkyc(kyc.getKycId(), reason.isBlank() ? "Thông tin không hợp lệ" : reason);
            }
            @Override protected void done() {
                try {
                    EkycController.Result r = get();
                    String msg = r == EkycController.Result.SUCCESS
                        ? "✅ Đã từ chối hồ sơ. Khách hàng sẽ nhận thông báo."
                        : "Lỗi: " + r;
                    JOptionPane.showMessageDialog(EkycApprovalPanel.this, msg,
                        r == EkycController.Result.SUCCESS ? "Hoàn tất" : "Lỗi",
                        r == EkycController.Result.SUCCESS
                            ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) { ex.printStackTrace(); }
                finally { loadData(); }
            }
        }.execute();
    }

    // =========================================================================
    //  TableModel
    // =========================================================================

    static class KycTableModel extends AbstractTableModel {
        private static final String[] COLS = {
            "ID", "Họ và tên", "Số CMND/CCCD", "Giới tính", "Ngày sinh",
            "Ngày nộp", "Xem ảnh", "Duyệt", "Từ chối"
        };
        private List<Ekyc> rows = new ArrayList<>();

        void setData(List<Ekyc> d) { this.rows = d; fireTableDataChanged(); }

        @Override public int getRowCount()    { return rows.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }
        @Override public boolean isCellEditable(int r, int c) { return c == 6 || c == 7 || c == 8; }

        @Override
        public Object getValueAt(int r, int c) {
            Ekyc k = rows.get(r);
            return switch (c) {
                case 0 -> k.getKycId();
                case 1 -> k.getFullName();
                case 2 -> k.getIdNumber();
                case 3 -> k.getGender() != null ? k.getGender() : "—";
                case 4 -> k.getDateOfBirth() != null ? k.getDateOfBirth().toString() : "—";
                case 5 -> k.getCreatedAt() != null
                          ? k.getCreatedAt().toLocalDateTime().toLocalDate().toString() : "—";
                case 6 -> "Xem ảnh";
                case 7 -> "Duyệt";
                case 8 -> "Từ chối";
                default -> "";
            };
        }
    }

    // ── Button Renderers & Editors ───────────────────────────────────────────

    private static JButton colBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 11));
        b.setForeground(Color.WHITE); b.setBackground(bg);
        b.setFocusPainted(false); b.setBorderPainted(false); b.setOpaque(true);
        return b;
    }

    private class ViewImgRenderer extends JButton implements TableCellRenderer {
        ViewImgRenderer() { super("🖼 Xem"); setFont(new Font("Segoe UI",Font.BOLD,11));
            setForeground(Color.WHITE); setBackground(BLUE); setFocusPainted(false); setBorderPainted(false); setOpaque(true); }
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c){return this;}
    }
    private class GreenBtnRenderer extends JButton implements TableCellRenderer {
        GreenBtnRenderer(String label) { super(label); setFont(new Font("Segoe UI",Font.BOLD,11));
            setForeground(Color.WHITE); setBackground(GREEN); setFocusPainted(false); setBorderPainted(false); setOpaque(true); }
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c){return this;}
    }
    private class RedBtnRenderer extends JButton implements TableCellRenderer {
        RedBtnRenderer(String label) { super(label); setFont(new Font("Segoe UI",Font.BOLD,11));
            setForeground(Color.WHITE); setBackground(RED); setFocusPainted(false); setBorderPainted(false); setOpaque(true); }
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c){return this;}
    }

    private class ViewImgEditor extends AbstractCellEditor implements TableCellEditor {
        private int cur;
        private final JButton btn = colBtn("🖼 Xem", BLUE);
        ViewImgEditor() { btn.addActionListener(e -> { fireEditingStopped(); onViewImages(cur); }); }
        @Override public Component getTableCellEditorComponent(JTable t,Object v,boolean s,int r,int c){cur=r;return btn;}
        @Override public Object getCellEditorValue(){return "Xem ảnh";}
    }
    private class ApproveEditor extends AbstractCellEditor implements TableCellEditor {
        private int cur;
        private final JButton btn = colBtn("Duyệt", GREEN);
        ApproveEditor() { btn.addActionListener(e -> { fireEditingStopped(); onApprove(cur); }); }
        @Override public Component getTableCellEditorComponent(JTable t,Object v,boolean s,int r,int c){cur=r;return btn;}
        @Override public Object getCellEditorValue(){return "Duyệt";}
    }
    private class RejectEditor extends AbstractCellEditor implements TableCellEditor {
        private int cur;
        private final JButton btn = colBtn("Từ chối", RED);
        RejectEditor() { btn.addActionListener(e -> { fireEditingStopped(); onReject(cur); }); }
        @Override public Component getTableCellEditorComponent(JTable t,Object v,boolean s,int r,int c){cur=r;return btn;}
        @Override public Object getCellEditorValue(){return "Từ chối";}
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private JButton makeRefreshBtn() {
        JButton b = new JButton("⟳  Làm mới");
        b.setFont(new Font("Segoe UI", Font.BOLD, 12)); b.setForeground(GREEN);
        b.setBackground(Color.WHITE);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 230, 210), 1, true), new EmptyBorder(6, 16, 6, 16)));
        b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void styleTable(JTable t) {
        t.setRowHeight(42); t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.getTableHeader().setBackground(new Color(240, 245, 255)); t.getTableHeader().setForeground(NAVY);
        t.setGridColor(new Color(230, 235, 245)); t.setShowVerticalLines(false);
        t.setSelectionBackground(new Color(200, 240, 220));
        t.setFillsViewportHeight(true); t.setBackground(CARD_BG);
    }
}
