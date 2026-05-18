package View.customer;

import Controller.EkycController;
import Model.AccountModel;
import Model.Ekyc;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * EkycDialog — Popup cho Customer tự nộp hồ sơ eKYC.
 *
 * Trạng thái hiển thị:
 *  - APPROVED  → badge xanh "Đã xác minh ✔", form disabled
 *  - PENDING   → badge vàng "Đang chờ duyệt", form disabled
 *  - REJECTED  → badge đỏ "Đã bị từ chối — vui lòng nộp lại", form enabled
 *  - UNSUBMITTED → form trống, có thể nộp
 */
public class EkycDialog extends JDialog {

    // ── Màu sắc ──────────────────────────────────────────────────────────────
    private static final Color NAVY     = new Color(15, 40, 80);
    private static final Color BLUE     = new Color(0, 162, 232);
    private static final Color GREEN    = new Color(16, 185, 129);
    private static final Color ORANGE   = new Color(245, 158, 11);
    private static final Color RED      = new Color(239, 68, 68);
    private static final Color BG       = new Color(245, 248, 252);
    private static final Color GRAY_BDR = new Color(210, 220, 235);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── State ─────────────────────────────────────────────────────────────────
    private final EkycController controller = new EkycController();
    private final AccountModel   account;
    private Ekyc                 existingKyc;

    // ── Form fields ───────────────────────────────────────────────────────────
    private JTextField  txtFullName;
    private JTextField  txtIdNumber;
    private JTextField  txtDateOfBirth;   // dd/MM/yyyy
    private JComboBox<String> cbGender;
    private JTextField  txtPlaceOfOrigin;
    private JTextField  txtPlaceOfResidence;
    private JTextField  txtIssueDate;     // dd/MM/yyyy
    private JTextField  txtExpiryDate;    // dd/MM/yyyy
    private JTextField  txtIssuePlace;

    // ── Image paths ───────────────────────────────────────────────────────────
    private JTextField  txtFrontImg;
    private JTextField  txtBackImg;
    private JTextField  txtFaceImg;

    // ── Buttons ───────────────────────────────────────────────────────────────
    private JButton btnSubmit;
    private JPanel  statusBanner;
    private JLabel  lblStatus;

    public EkycDialog(Frame parent, AccountModel account) {
        super(parent, "Nộp hồ sơ eKYC", true);
        this.account = account;
        build();
        loadExistingKyc();
        pack();
        setLocationRelativeTo(parent);
    }

    // =========================================================================
    //  Build UI
    // =========================================================================

    private void build() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setMinimumSize(new Dimension(600, 600));

        // ── Header ───────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(new EmptyBorder(18, 24, 18, 24));
        JLabel lblTitle = new JLabel("Xác minh danh tính (eKYC)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitle.setForeground(Color.WHITE);
        JLabel lblSub = new JLabel("Điền đầy đủ thông tin CCCD/CMND và ảnh xác minh");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(180, 200, 235));
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(lblTitle);
        titles.add(Box.createVerticalStrut(3));
        titles.add(lblSub);
        header.add(titles, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // ── Status banner (ẩn mặc định) ──────────────────────────────────────
        statusBanner = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 10));
        statusBanner.setVisible(false);
        lblStatus = new JLabel();
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusBanner.add(lblStatus);
        add(statusBanner, BorderLayout.NORTH);  // sẽ bị replace bên dưới

        // ── Scroll form ───────────────────────────────────────────────────────
        JPanel form = buildForm();
        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // ── Center: status + form ─────────────────────────────────────────────
        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setBackground(BG);
        center.add(statusBanner, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // ── Footer (submit) ───────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 12));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GRAY_BDR));

        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnClose.addActionListener(e -> dispose());

        btnSubmit = new JButton("📤  Nộp hồ sơ KYC");
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSubmit.setBackground(BLUE);
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFocusPainted(false);
        btnSubmit.setBorderPainted(false);
        btnSubmit.setOpaque(true);
        btnSubmit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSubmit.setPreferredSize(new Dimension(180, 40));
        btnSubmit.addActionListener(e -> onSubmit());

        footer.add(btnClose);
        footer.add(btnSubmit);
        add(footer, BorderLayout.SOUTH);
    }

    private JPanel buildForm() {
        JPanel p = new JPanel();
        p.setBackground(BG);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(20, 24, 20, 24));

        // ── Thông tin cơ bản ─────────────────────────────────────────────────
        p.add(sectionTitle("Thông tin cơ bản"));
        p.add(Box.createVerticalStrut(10));

        JPanel grid1 = new JPanel(new GridLayout(0, 2, 12, 10));
        grid1.setOpaque(false);
        txtFullName         = addField(grid1, "Họ và tên *");
        txtIdNumber         = addField(grid1, "Số CCCD/CMND *");
        txtDateOfBirth      = addField(grid1, "Ngày sinh (dd/MM/yyyy) *");
        cbGender            = new JComboBox<>(new String[]{"Nam", "Nữ", "Khác"});
        cbGender.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addCombo(grid1, "Giới tính", cbGender);
        txtPlaceOfOrigin    = addField(grid1, "Quê quán");
        txtPlaceOfResidence = addField(grid1, "Thường trú");
        p.add(grid1);
        p.add(Box.createVerticalStrut(18));

        // ── Thông tin giấy tờ ────────────────────────────────────────────────
        p.add(sectionTitle("Thông tin giấy tờ"));
        p.add(Box.createVerticalStrut(10));

        JPanel grid2 = new JPanel(new GridLayout(0, 2, 12, 10));
        grid2.setOpaque(false);
        txtIssueDate  = addField(grid2, "Ngày cấp (dd/MM/yyyy) *");
        txtExpiryDate = addField(grid2, "Ngày hết hạn (dd/MM/yyyy) *");
        txtIssuePlace = addField(grid2, "Nơi cấp *");
        p.add(grid2);
        p.add(Box.createVerticalStrut(18));

        // ── Ảnh xác minh ─────────────────────────────────────────────────────
        p.add(sectionTitle("Ảnh xác minh"));
        p.add(Box.createVerticalStrut(10));

        JPanel imgPanel = new JPanel(new GridLayout(3, 1, 0, 10));
        imgPanel.setOpaque(false);
        txtFrontImg = addFilePicker(imgPanel, "📄 Ảnh mặt trước CCCD *");
        txtBackImg  = addFilePicker(imgPanel, "📄 Ảnh mặt sau CCCD *");
        txtFaceImg  = addFilePicker(imgPanel, "🤳 Ảnh selfie / khuôn mặt *");
        p.add(imgPanel);
        p.add(Box.createVerticalStrut(10));

        // Required note
        JLabel note = new JLabel("* Trường bắt buộc");
        note.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        note.setForeground(new Color(140, 145, 160));
        note.setAlignmentX(LEFT_ALIGNMENT);
        p.add(note);

        return p;
    }

    // =========================================================================
    //  Data Loading
    // =========================================================================

    private void loadExistingKyc() {
        existingKyc = controller.getKycByUser(account.getUser().getUserId());
        if (existingKyc == null) return;

        if (existingKyc.isApproved()) {
            showBanner("✔  eKYC đã được xác minh thành công. Bạn không cần nộp lại.", GREEN, false);
        } else if (existingKyc.isPending()) {
            showBanner("⏳  Hồ sơ eKYC đang chờ duyệt. Vui lòng chờ nhân viên xem xét.", ORANGE, false);
        } else if (existingKyc.isRejected()) {
            String reason = existingKyc.getNote() != null ? existingKyc.getNote() : "";
            showBanner("✖  Hồ sơ bị từ chối" + (reason.isBlank() ? "" : ": " + reason) + ". Vui lòng nộp lại.", RED, true);
        }
    }

    private void showBanner(String text, Color bg, boolean canResubmit) {
        statusBanner.setBackground(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 25));
        statusBanner.setBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, bg));
        lblStatus.setText(text);
        lblStatus.setForeground(bg.darker());
        statusBanner.setVisible(true);

        // Nếu không được nộp lại → disable toàn bộ form
        if (!canResubmit) {
            setFormEnabled(false);
            btnSubmit.setEnabled(false);
            btnSubmit.setText("Không thể nộp lại");
            btnSubmit.setBackground(new Color(180, 185, 195));
        }
    }

    private void setFormEnabled(boolean enabled) {
        for (JTextField f : new JTextField[]{txtFullName, txtIdNumber, txtDateOfBirth,
                txtPlaceOfOrigin, txtPlaceOfResidence, txtIssueDate, txtExpiryDate,
                txtIssuePlace, txtFrontImg, txtBackImg, txtFaceImg}) {
            if (f != null) f.setEnabled(enabled);
        }
        if (cbGender != null) cbGender.setEnabled(enabled);
    }

    // =========================================================================
    //  Submit
    // =========================================================================

    private void onSubmit() {
        // Validation
        String fullName = txtFullName.getText().trim();
        String idNumber = txtIdNumber.getText().trim();
        String dobStr   = txtDateOfBirth.getText().trim();
        String issueDateStr   = txtIssueDate.getText().trim();
        String expiryDateStr  = txtExpiryDate.getText().trim();
        String issuePlace     = txtIssuePlace.getText().trim();
        String frontImg = txtFrontImg.getText().trim();
        String backImg  = txtBackImg.getText().trim();
        String faceImg  = txtFaceImg.getText().trim();

        if (fullName.isEmpty() || idNumber.isEmpty() || dobStr.isEmpty()
                || issueDateStr.isEmpty() || expiryDateStr.isEmpty()
                || issuePlace.isEmpty() || frontImg.isEmpty()
                || backImg.isEmpty() || faceImg.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng điền đầy đủ tất cả các trường bắt buộc (*) và chọn ảnh xác minh!",
                "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Parse dates
        Date dob, issDate, expDate;
        try {
            dob     = Date.valueOf(LocalDate.parse(dobStr,       DATE_FMT));
            issDate = Date.valueOf(LocalDate.parse(issueDateStr, DATE_FMT));
            expDate = Date.valueOf(LocalDate.parse(expiryDateStr,DATE_FMT));
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                "Định dạng ngày không hợp lệ. Vui lòng nhập theo định dạng dd/MM/yyyy (ví dụ: 15/06/1998).",
                "Lỗi định dạng", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Build Ekyc object
        Ekyc ekyc = new Ekyc();
        ekyc.setUserId(account.getUser().getUserId());
        ekyc.setFullName(fullName);
        ekyc.setIdNumber(idNumber);
        ekyc.setDateOfBirth(dob);
        ekyc.setGender((String) cbGender.getSelectedItem());
        ekyc.setPlaceOfOrigin(txtPlaceOfOrigin.getText().trim());
        ekyc.setPlaceOfResidence(txtPlaceOfResidence.getText().trim());
        ekyc.setIssueDate(issDate);
        ekyc.setExpiryDate(expDate);
        ekyc.setIssuePlace(issuePlace);
        ekyc.setFrontImageUrl(frontImg);
        ekyc.setBackImageUrl(backImg);
        ekyc.setFaceImageUrl(faceImg);

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang nộp hồ sơ...");

        SwingWorker<EkycController.Result, Void> w = new SwingWorker<>() {
            @Override protected EkycController.Result doInBackground() {
                return controller.submitEkyc(ekyc);
            }
            @Override protected void done() {
                try {
                    EkycController.Result result = get();
                    switch (result) {
                        case SUCCESS -> {
                            JOptionPane.showMessageDialog(EkycDialog.this,
                                "✅ Nộp hồ sơ thành công!\nNhân viên sẽ xem xét trong vòng 24 giờ.",
                                "Nộp thành công", JOptionPane.INFORMATION_MESSAGE);
                            dispose();
                        }
                        case ALREADY_PROCESSED ->
                            JOptionPane.showMessageDialog(EkycDialog.this,
                                "Bạn đã có hồ sơ PENDING hoặc đã được xác minh. Không thể nộp lại.",
                                "Không thể nộp", JOptionPane.WARNING_MESSAGE);
                        default ->
                            JOptionPane.showMessageDialog(EkycDialog.this,
                                "Có lỗi khi nộp hồ sơ. Vui lòng thử lại sau.",
                                "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(EkycDialog.this,
                        "Lỗi hệ thống: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("📤  Nộp hồ sơ KYC");
                }
            }
        };
        w.execute();
    }

    // =========================================================================
    //  Form builder helpers
    // =========================================================================

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 14));
        l.setForeground(NAVY);
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 220, 235)));
        return l;
    }

    /** Thêm label + text field vào GridLayout panel, trả về text field. */
    private JTextField addField(JPanel grid, String label) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(new Color(80, 90, 110));

        JTextField tf = new JTextField();
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GRAY_BDR, 1, true),
            new EmptyBorder(4, 8, 4, 8)));
        tf.setBackground(Color.WHITE);

        JPanel cell = new JPanel(new BorderLayout(0, 4));
        cell.setOpaque(false);
        cell.add(lbl, BorderLayout.NORTH);
        cell.add(tf, BorderLayout.CENTER);
        grid.add(cell);
        return tf;
    }

    private void addCombo(JPanel grid, String label, JComboBox<String> cb) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(new Color(80, 90, 110));
        JPanel cell = new JPanel(new BorderLayout(0, 4));
        cell.setOpaque(false);
        cell.add(lbl, BorderLayout.NORTH);
        cell.add(cb, BorderLayout.CENTER);
        grid.add(cell);
    }

    /** Hàng file picker: label + text field (readonly) + nút Browse. */
    private JTextField addFilePicker(JPanel parent, String label) {
        JPanel row = new JPanel(new BorderLayout(8, 4));
        row.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(new Color(80, 90, 110));
        row.add(lbl, BorderLayout.NORTH);

        JPanel inputRow = new JPanel(new BorderLayout(6, 0));
        inputRow.setOpaque(false);

        JTextField tf = new JTextField();
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tf.setEditable(false);
        tf.setBackground(new Color(248, 250, 253));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GRAY_BDR, 1, true),
            new EmptyBorder(4, 8, 4, 8)));
        tf.setToolTipText("Nhấn nút Chọn file... để duyệt ảnh");

        JButton btnBrowse = new JButton("Chọn file...");
        btnBrowse.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnBrowse.setFocusPainted(false);
        btnBrowse.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBrowse.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Chọn ảnh xác minh");
            fc.setFileFilter(new FileNameExtensionFilter(
                "Ảnh (*.jpg, *.jpeg, *.png)", "jpg", "jpeg", "png"));
            fc.setAcceptAllFileFilterUsed(false);
            int res = fc.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                tf.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });

        inputRow.add(tf, BorderLayout.CENTER);
        inputRow.add(btnBrowse, BorderLayout.EAST);
        row.add(inputRow, BorderLayout.CENTER);
        parent.add(row);
        return tf;
    }

    // =========================================================================
    //  Public entry point
    // =========================================================================

    /** Mở dialog và trả về sau khi người dùng đóng. */
    public static void show(Frame parent, AccountModel account) {
        EkycDialog dlg = new EkycDialog(parent, account);
        dlg.setVisible(true);
    }
}
