package View.staff;

import DAO.AccountDAO;
import DAO.UserDAO;
import DAO.WalletDAO;
import Model.Account;
import Model.AccountModel;
import Model.User;
import Model.Wallet;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.sql.Connection;
import ConnectDB.ConnectionOracle;

public class UserManagementPanel extends JPanel {

    private static final Color BG = new Color(238, 243, 250);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BLUE = new Color(0, 162, 232);
    private static final Color NAVY = new Color(15, 40, 80);

    private final AccountModel adminAccount;
    private final UserDAO userDAO = new UserDAO();
    private final AccountDAO accDAO = new AccountDAO();
    private final WalletDAO walletDAO = new WalletDAO();

    private DefaultTableModel tableModel;
    private JTable table;
    private JComboBox<String> cbRoleFilter;
    private JComboBox<String> cbKycFilter;

    private JPanel detailPanel;
    private JLabel lblDetailName, lblDetailEmail, lblDetailStatus, lblDetailKyc, lblDetailWallet;
    private DefaultTableModel historyTableModel;
    private User selectedUser;
    private JButton btnToggleStatus;

    public UserManagementPanel(AccountModel adminAccount) {
        this.adminAccount = adminAccount;
        setLayout(new BorderLayout(16, 0));
        setBackground(BG);
        setBorder(new EmptyBorder(24, 28, 24, 28));
        build();
        loadData();
    }

    private void build() {
        // Left Panel (Table + Filters)
        JPanel leftPanel = new JPanel(new BorderLayout(0, 16));
        leftPanel.setOpaque(false);

        // Filters
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setOpaque(false);
        filterPanel.add(new JLabel("Role:"));
        cbRoleFilter = new JComboBox<>(new String[]{"All", "Admin", "Staff", "Customer"});
        cbRoleFilter.addActionListener(e -> applyFilters());
        filterPanel.add(cbRoleFilter);

        filterPanel.add(Box.createHorizontalStrut(16));
        filterPanel.add(new JLabel("KYC:"));
        cbKycFilter = new JComboBox<>(new String[]{"All", "APPROVED", "PENDING", "REJECTED"});
        cbKycFilter.addActionListener(e -> applyFilters());
        filterPanel.add(cbKycFilter);

        leftPanel.add(filterPanel, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID", "Email", "Role", "Status", "KYC"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(36);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showUserDetails();
        });
        
        JScrollPane scroll = new JScrollPane(table);
        leftPanel.add(scroll, BorderLayout.CENTER);

        // Right Panel (Details)
        detailPanel = new JPanel(new BorderLayout(0, 16));
        detailPanel.setBackground(CARD_BG);
        detailPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 235)),
            new EmptyBorder(20, 20, 20, 20)
        ));
        detailPanel.setPreferredSize(new Dimension(350, 0));

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        lblDetailName = new JLabel("Chọn một user để xem");
        lblDetailName.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblDetailEmail = new JLabel("Email: --");
        lblDetailStatus = new JLabel("Status: --");
        lblDetailKyc = new JLabel("KYC: --");
        lblDetailWallet = new JLabel("Số dư: -- VNĐ");

        infoPanel.add(lblDetailName);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(lblDetailEmail);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(lblDetailStatus);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(lblDetailKyc);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(lblDetailWallet);
        infoPanel.add(Box.createVerticalStrut(20));

        btnToggleStatus = new JButton("Khóa / Mở Khóa");
        btnToggleStatus.addActionListener(e -> toggleUserStatus());
        btnToggleStatus.setEnabled(false);
        infoPanel.add(btnToggleStatus);

        detailPanel.add(infoPanel, BorderLayout.NORTH);

        // Recent Transactions
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setOpaque(false);
        historyPanel.add(new JLabel("Giao dịch gần đây"), BorderLayout.NORTH);
        
        historyTableModel = new DefaultTableModel(new String[]{"Ngày", "Loại", "Số tiền"}, 0);
        JTable historyTable = new JTable(historyTableModel);
        historyPanel.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        detailPanel.add(historyPanel, BorderLayout.CENTER);

        // Add to main panel
        add(leftPanel, BorderLayout.CENTER);
        add(detailPanel, BorderLayout.EAST);
    }

    private List<User> allUsers;

    public void loadData() {
        SwingWorker<List<User>, Void> worker = new SwingWorker<>() {
            @Override protected List<User> doInBackground() {
                return userDAO.getAllUsers();
            }
            @Override protected void done() {
                try {
                    allUsers = get();
                    applyFilters();
                } catch (Exception e) {}
            }
        };
        worker.execute();
    }

    private void applyFilters() {
        if (allUsers == null) return;
        tableModel.setRowCount(0);
        
        String roleFilter = (String) cbRoleFilter.getSelectedItem();
        String kycFilter = (String) cbKycFilter.getSelectedItem();

        for (User u : allUsers) {
            String roleName = u.getRoleId() == 1 ? "Admin" : u.getRoleId() == 2 ? "Staff" : "Customer";
            if (!"All".equals(roleFilter) && !roleName.equals(roleFilter)) continue;

            String kycStatus = getKycStatus(u.getUserId());
            if (!"All".equals(kycFilter) && !kycStatus.equals(kycFilter)) continue;

            tableModel.addRow(new Object[]{u.getUserId(), u.getEmail(), roleName, u.getStatus(), kycStatus});
        }
    }

    private String getKycStatus(int userId) {
        String status = "UNSUBMITTED";
        try (Connection con = ConnectionOracle.getOracleConnection();
             var ps = con.prepareStatement("SELECT verified_status FROM EKYC WHERE user_id=? ORDER BY created_at DESC")) {
            ps.setInt(1, userId);
            var rs = ps.executeQuery();
            if (rs.next()) status = rs.getString(1);
        } catch (Exception e) {}
        return status;
    }

    private void showUserDetails() {
        int r = table.getSelectedRow();
        if (r < 0) {
            btnToggleStatus.setEnabled(false);
            return;
        }

        int userId = (int) tableModel.getValueAt(r, 0);
        selectedUser = allUsers.stream().filter(u -> u.getUserId() == userId).findFirst().orElse(null);
        if (selectedUser == null) return;

        Account acc = accDAO.getByUserId(userId);
        lblDetailName.setText(acc != null ? acc.getUsername() : "Unknown");
        lblDetailEmail.setText("Email: " + selectedUser.getEmail());
        lblDetailStatus.setText("Status: " + selectedUser.getStatus());
        lblDetailKyc.setText("KYC: " + getKycStatus(userId));

        Wallet w = walletDAO.getByUserId(userId);
        lblDetailWallet.setText("Số dư ví: " + (w != null ? String.format("%,.0f VNĐ", w.getAvailableBalance()) : "0 VNĐ"));

        btnToggleStatus.setEnabled(true);
        btnToggleStatus.setText("ACTIVE".equals(selectedUser.getStatus()) ? "Khóa Tài Khoản" : "Mở Khóa");

        // Load recent transactions
        historyTableModel.setRowCount(0);
        if (w != null) {
            try (Connection con = ConnectionOracle.getOracleConnection();
                 var ps = con.prepareStatement("SELECT transaction_date, transaction_type, amount FROM TRANSACTION WHERE wallet_id=? ORDER BY transaction_date DESC FETCH FIRST 5 ROWS ONLY")) {
                ps.setInt(1, w.getWalletId());
                var rs = ps.executeQuery();
                while (rs.next()) {
                    historyTableModel.addRow(new Object[]{
                        rs.getDate(1), rs.getString(2), String.format("%,.0f", rs.getBigDecimal(3))
                    });
                }
            } catch (Exception e) {}
        }
    }

    private void toggleUserStatus() {
        if (selectedUser == null) return;
        String newStatus = "ACTIVE".equals(selectedUser.getStatus()) ? "LOCKED" : "ACTIVE";
        if (userDAO.updateStatus(selectedUser.getUserId(), newStatus)) {
            selectedUser.setStatus(newStatus);
            lblDetailStatus.setText("Status: " + newStatus);
            btnToggleStatus.setText("ACTIVE".equals(newStatus) ? "Khóa Tài Khoản" : "Mở Khóa");
            // Update table
            int r = table.getSelectedRow();
            if (r >= 0) tableModel.setValueAt(newStatus, r, 3);
            JOptionPane.showMessageDialog(this, "Đã cập nhật trạng thái tài khoản thành công!");
        } else {
            JOptionPane.showMessageDialog(this, "Có lỗi xảy ra khi cập nhật trạng thái.");
        }
    }
}
