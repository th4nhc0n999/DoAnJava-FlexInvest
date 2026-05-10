package View;

import DAO.EkycDAO;
import DAO.RequestDAO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class StaffDashboardPanel extends JPanel {
    private JLabel lblPendingDeposit;
    private JLabel lblPendingWithdraw;
    private JLabel lblPendingEkyc;

    private RequestDAO requestDAO;
    private EkycDAO ekycDAO;

    public StaffDashboardPanel() {
        requestDAO = new RequestDAO();
        ekycDAO = new EkycDAO();

        initComponents();
        
        // Refresh when panel is shown
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                refreshData();
            }
        });
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250)); // Light modern background

        // Title
        JLabel lblTitle = new JLabel("Staff Dashboard - Pending Approvals", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        add(lblTitle, BorderLayout.NORTH);

        // Content panel
        JPanel contentPanel = new JPanel(new GridLayout(1, 3, 20, 20));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 40, 40));

        // Create cards
        JPanel pnlDeposit = createCard("Pending Deposits", new Color(41, 128, 185));
        lblPendingDeposit = createValueLabel();
        pnlDeposit.add(lblPendingDeposit, BorderLayout.CENTER);

        JPanel pnlWithdraw = createCard("Pending Withdrawals", new Color(192, 57, 43));
        lblPendingWithdraw = createValueLabel();
        pnlWithdraw.add(lblPendingWithdraw, BorderLayout.CENTER);

        JPanel pnlEkyc = createCard("Pending eKYC", new Color(39, 174, 96));
        lblPendingEkyc = createValueLabel();
        pnlEkyc.add(lblPendingEkyc, BorderLayout.CENTER);

        contentPanel.add(pnlDeposit);
        contentPanel.add(pnlWithdraw);
        contentPanel.add(pnlEkyc);

        add(contentPanel, BorderLayout.CENTER);

        // Refresh button
        JButton btnRefresh = new JButton("Refresh Data");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRefresh.setFocusPainted(false);
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.addActionListener(e -> refreshData());
        
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 30, 10));
        bottomPanel.add(btnRefresh);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createCard(String title, Color bgColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1, true),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel lblCardTitle = new JLabel(title, SwingConstants.CENTER);
        lblCardTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblCardTitle.setForeground(Color.WHITE);
        card.add(lblCardTitle, BorderLayout.NORTH);

        return card;
    }

    private JLabel createValueLabel() {
        JLabel lblValue = new JLabel("0", SwingConstants.CENTER);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 48));
        lblValue.setForeground(Color.WHITE);
        return lblValue;
    }

    public void refreshData() {
        // Run in background to avoid freezing UI
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            int pendingDeposits = 0;
            int pendingWithdraws = 0;
            int pendingEkyc = 0;

            @Override
            protected Void doInBackground() throws Exception {
                pendingDeposits = requestDAO.countPendingDeposits();
                pendingWithdraws = requestDAO.countPendingWithdraws();
                pendingEkyc = ekycDAO.countPending();
                return null;
            }

            @Override
            protected void done() {
                lblPendingDeposit.setText(String.valueOf(pendingDeposits));
                lblPendingWithdraw.setText(String.valueOf(pendingWithdraws));
                lblPendingEkyc.setText(String.valueOf(pendingEkyc));
            }
        };
        worker.execute();
    }
}
