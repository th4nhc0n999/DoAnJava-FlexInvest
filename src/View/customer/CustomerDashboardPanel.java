package View.customer;

import DAO.EkycDAO;
import DAO.InvestmentDAO;
import DAO.WalletDAO;
import Model.AccountModel;
import Model.Ekyc;
import Model.Investment;
import Model.Wallet;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Main dashboard panel shown immediately after customer login.
 *
 * Sections:
 *  ① Số dư ví (available + locked) — live from WalletDAO
 *  ② Tổng đang tích lũy — sum of active invested amounts from InvestmentDAO
 *  ③ Gói sắp đáo hạn trong 7 ngày — list from InvestmentDAO.getDueSoon()
 *  ④ Trạng thái eKYC — from EkycDAO; shows "Nộp hồ sơ" button if not verified
 */
public class CustomerDashboardPanel extends JPanel {

    private static final NumberFormat CURRENCY_FMT;
    static {
        CURRENCY_FMT = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        CURRENCY_FMT.setMaximumFractionDigits(0);
    }

    private int userId;
    private final WalletDAO walletDAO = new WalletDAO();
    private final InvestmentDAO investmentDAO = new InvestmentDAO();
    private final EkycDAO ekycDAO = new EkycDAO();

    // Callback to navigate to KYC panel
    private final Consumer<String> navigate;

    // Live labels updated on refresh
    private JLabel lblAvailable, lblLocked, lblTotalAccumulating;
    private JPanel dueSoonContainer;
    private JPanel kycStatusPanel;

    public CustomerDashboardPanel(int userId, Consumer<String> navigate) {
        this.userId = userId;
        this.navigate = navigate;
        setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout(0, 0));
        buildUI();
        refresh();
    }

    // -----------------------------------------------------------------------
    // Build static layout
    // -----------------------------------------------------------------------
    private AccountModel account;
    public CustomerDashboardPanel(AccountModel account) {
        this.account = account;
        this.userId = account.user().getUserId();

        this.account = account;

        if (account != null && account.user() != null) {
            this.userId = account.user().getUserId();
        }

        this.navigate = null;
        setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout(0, 0));
        buildUI();  
        refresh();
    }

    private void buildUI() {
        JPanel scrollContent = new JPanel();
        scrollContent.setBackground(UITheme.BG_DARK);
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.setBorder(new EmptyBorder(24, 28, 24, 28));

        // ── Header ──────────────────────────────────────────────────────────
        JLabel header = UITheme.label("Tổng quan tài khoản", UITheme.FONT_TITLE, UITheme.TEXT_PRIMARY);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));
        scrollContent.add(header);

        // ── Row 1: wallet + accumulating ───────────────────────────────────
        JPanel statsRow = new JPanel(new GridLayout(1, 2, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        statsRow.add(buildWalletCard());
        statsRow.add(buildAccumulatingCard());
        scrollContent.add(statsRow);
        scrollContent.add(Box.createVerticalStrut(20));

        // ── Row 2: due soon + KYC ──────────────────────────────────────────
        JPanel row2 = new JPanel(new GridLayout(1, 2, 16, 0));
        row2.setOpaque(false);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        row2.add(buildDueSoonCard());
        row2.add(buildKycCard());
        scrollContent.add(row2);

        // Refresh button
        scrollContent.add(Box.createVerticalStrut(16));
        JButton refreshBtn = UITheme.ghostButton("↻  Làm mới");
        refreshBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        refreshBtn.addActionListener(e -> refresh());
        scrollContent.add(refreshBtn);

        JScrollPane sp = UITheme.darkScroll(scrollContent);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(sp, BorderLayout.CENTER);
    }

    // ── Wallet card ─────────────────────────────────────────────────────────

    private JPanel buildWalletCard() {
        JPanel card = UITheme.cardPanel();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = UITheme.label("💳  Số dư ví", UITheme.FONT_HEAD, UITheme.TEXT_MUTED);
        lblAvailable = UITheme.label("—", UITheme.FONT_TITLE, UITheme.ACCENT_TEAL);
        JPanel sub = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        sub.setOpaque(false);
        JLabel lockedLbl = UITheme.label("Đang khóa: ", UITheme.FONT_SMALL, UITheme.TEXT_MUTED);
        lblLocked = UITheme.label("—", UITheme.FONT_SMALL, UITheme.ACCENT_GOLD);
        sub.add(lockedLbl);
        sub.add(lblLocked);

        card.add(title, BorderLayout.NORTH);
        card.add(lblAvailable, BorderLayout.CENTER);
        card.add(sub, BorderLayout.SOUTH);
        return card;
    }

    // ── Accumulating card ───────────────────────────────────────────────────

    private JPanel buildAccumulatingCard() {
        JPanel card = UITheme.cardPanel();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = UITheme.label("📈  Tổng đang tích lũy", UITheme.FONT_HEAD, UITheme.TEXT_MUTED);
        lblTotalAccumulating = UITheme.label("—", UITheme.FONT_TITLE, UITheme.SUCCESS_GREEN);
        JLabel note = UITheme.label("Gộp tất cả gói ACTIVE + PENDING", UITheme.FONT_SMALL, UITheme.TEXT_MUTED);

        card.add(title, BorderLayout.NORTH);
        card.add(lblTotalAccumulating, BorderLayout.CENTER);
        card.add(note, BorderLayout.SOUTH);
        return card;
    }

    // ── Due-soon card ────────────────────────────────────────────────────────

    private JPanel buildDueSoonCard() {
        JPanel card = UITheme.cardPanel();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = UITheme.label("⏰  Đáo hạn trong 7 ngày", UITheme.FONT_HEAD, UITheme.TEXT_MUTED);
        card.add(title, BorderLayout.NORTH);

        dueSoonContainer = new JPanel();
        dueSoonContainer.setOpaque(false);
        dueSoonContainer.setLayout(new BoxLayout(dueSoonContainer, BoxLayout.Y_AXIS));
        card.add(dueSoonContainer, BorderLayout.CENTER);
        return card;
    }

    // ── KYC card ─────────────────────────────────────────────────────────────

    private JPanel buildKycCard() {
        JPanel card = UITheme.cardPanel();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = UITheme.label("🪪  Xác minh danh tính (eKYC)", UITheme.FONT_HEAD, UITheme.TEXT_MUTED);
        card.add(title, BorderLayout.NORTH);

        kycStatusPanel = new JPanel(new BorderLayout(0, 10));
        kycStatusPanel.setOpaque(false);
        card.add(kycStatusPanel, BorderLayout.CENTER);
        return card;
    }

    // -----------------------------------------------------------------------
    // Data refresh (call after any mutation too)
    // -----------------------------------------------------------------------

    public void refresh() {
        SwingWorker<DashboardData, Void> worker = new SwingWorker<>() {
            @Override protected DashboardData doInBackground() throws Exception {
                DashboardData d = new DashboardData();
                d.wallet = walletDAO.getWalletByUserId(userId);
                d.totalAccumulating = investmentDAO.getTotalActiveInvestedAmount(userId);
                d.dueSoon = investmentDAO.getDueSoon(userId, 7);
                d.kyc = ekycDAO.getLatestByUserId(userId);
                return d;
            }

            @Override protected void done() {
                try {
                    DashboardData d = get();
                    updateWallet(d.wallet);
                    updateAccumulating(d.totalAccumulating);
                    updateDueSoon(d.dueSoon);
                    updateKyc(d.kyc);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void updateWallet(Wallet w) {
        if (w == null) {
            lblAvailable.setText("Chưa có ví");
            lblLocked.setText("—");
        } else {
            lblAvailable.setText(vnd(w.getAvailableBalance()));
            lblLocked.setText(vnd(w.getLockedBalance()) + " VND");
        }
    }

    private void updateAccumulating(BigDecimal total) {
        lblTotalAccumulating.setText(vnd(total) + " VND");
    }

    private void updateDueSoon(List<Investment> list) {
        dueSoonContainer.removeAll();
        if (list.isEmpty()) {
            dueSoonContainer.add(UITheme.label("Không có gói nào sắp đáo hạn.", UITheme.FONT_BODY, UITheme.TEXT_MUTED));
        } else {
            for (Investment inv : list) {
                JPanel row = new JPanel(new BorderLayout(8, 0));
                row.setOpaque(false);
                row.setBorder(new EmptyBorder(4, 0, 4, 0));

                String text = String.format("• %s — %s VND", inv.getProductName(), vnd(inv.getInvestedAmount()));
                JLabel nameLbl = UITheme.label(text, UITheme.FONT_BODY, UITheme.TEXT_PRIMARY);

                // String dateStr = inv.getMaturityDate() != null
                //         ? inv.getMaturityDate().format(DATE_FMT) : "N/A";
                String dateStr = inv.getMaturityDate() != null
                            ? new SimpleDateFormat("dd/MM/yyyy").format(inv.getMaturityDate())
                            : "N/A";
                    
                JLabel dateLbl = UITheme.label(dateStr, UITheme.FONT_SMALL, UITheme.ACCENT_GOLD);

                row.add(nameLbl, BorderLayout.CENTER);
                row.add(dateLbl, BorderLayout.EAST);
                dueSoonContainer.add(row);
            }
        }
        dueSoonContainer.revalidate();
        dueSoonContainer.repaint();
    }

    private void updateKyc(Ekyc kyc) {
        kycStatusPanel.removeAll();
        if (kyc == null) {
            // Never submitted
            JLabel statusLbl = UITheme.label("Chưa xác minh danh tính", UITheme.FONT_BODY, UITheme.ACCENT_GOLD);
            JButton kycBtn = UITheme.primaryButton("Nộp hồ sơ eKYC");
            kycBtn.addActionListener(e -> {
                if (navigate != null) navigate.accept("KYC");
                else JOptionPane.showMessageDialog(this, "Vui lòng hoàn thành eKYC để đầu tư.");
            });
            kycStatusPanel.add(statusLbl, BorderLayout.NORTH);
            kycStatusPanel.add(kycBtn, BorderLayout.CENTER);
        } else {
            String status = kyc.getVerifiedStatus();
            Color col;
            String icon;
            switch (status) {
                case "VERIFIED": col = UITheme.SUCCESS_GREEN; icon = "✅ "; break;
                case "REJECTED": col = UITheme.ACCENT_RED;   icon = "❌ "; break;
                default:         col = UITheme.ACCENT_GOLD;  icon = "⏳ "; break;
            }
            JLabel statusLbl = UITheme.label(icon + mapKycStatus(status), UITheme.FONT_BODY, col);
            kycStatusPanel.add(statusLbl, BorderLayout.NORTH);
            if ("REJECTED".equals(status)) {
                JButton resubmit = UITheme.primaryButton("Nộp lại hồ sơ");
                resubmit.addActionListener(e -> { if (navigate != null) navigate.accept("KYC"); });
                kycStatusPanel.add(resubmit, BorderLayout.CENTER);
            }
            if (kyc.getNote() != null && !kyc.getNote().isBlank()) {
                JLabel noteLbl = UITheme.label("Ghi chú: " + kyc.getNote(),
                        UITheme.FONT_SMALL, UITheme.TEXT_MUTED);
                kycStatusPanel.add(noteLbl, BorderLayout.SOUTH);
            }
        }
        kycStatusPanel.revalidate();
        kycStatusPanel.repaint();
    }

    private String mapKycStatus(String s) {
        return switch (s) {
            case "VERIFIED" -> "Đã xác minh";
            case "REJECTED" -> "Bị từ chối — vui lòng nộp lại";
            default         -> "Đang xử lý";
        };
    }

    private String vnd(BigDecimal v) {
        if (v == null) return "0";
        return CURRENCY_FMT.format(v);
    }

    // -----------------------------------------------------------------------
    // Inner data holder
    // -----------------------------------------------------------------------

    private static class DashboardData {
        Wallet wallet;
        BigDecimal totalAccumulating;
        List<Investment> dueSoon;
        Ekyc kyc;
    }
}
