package View;

import DAO.RequestDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

public class WithdrawApprovalPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private RequestDAO requestDAO;

    public WithdrawApprovalPanel() {
        requestDAO = new RequestDAO();
        initComponents();
        
        // Refresh when panel is shown
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadData();
            }
        });
        
        loadData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(new Color(245, 247, 250));

        // Title
        JLabel lblTitle = new JLabel("Duyệt Lệnh Rút Tiền");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        add(lblTitle, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Khách hàng (Email)", "Số tiền", "Tài khoản nhận", "Ngày tạo", "Ghi chú / Gói"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // read-only
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        // Buttons Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        
        JButton btnRefresh = new JButton("Làm mới");
        JButton btnApprove = new JButton("Duyệt (Approve)");
        JButton btnReject = new JButton("Từ chối (Reject)");

        styleButton(btnRefresh, new Color(41, 128, 185));
        styleButton(btnApprove, new Color(39, 174, 96));
        styleButton(btnReject, new Color(192, 57, 43));

        btnRefresh.addActionListener(e -> loadData());
        btnApprove.addActionListener(e -> approveWithdraw());
        btnReject.addActionListener(e -> rejectWithdraw());

        btnPanel.add(btnRefresh);
        btnPanel.add(btnApprove);
        btnPanel.add(btnReject);

        add(btnPanel, BorderLayout.SOUTH);
    }

    private void styleButton(JButton btn, Color color) {
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void loadData() {
        SwingWorker<List<Object[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Object[]> doInBackground() throws Exception {
                return requestDAO.findPendingWithdrawDetails();
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> data = get();
                    tableModel.setRowCount(0);
                    for (Object[] row : data) {
                        tableModel.addRow(row);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(WithdrawApprovalPanel.this, "Lỗi khi tải dữ liệu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void approveWithdraw() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một lệnh rút để duyệt!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int withdrawId = (int) tableModel.getValueAt(selectedRow, 0);
        String note = JOptionPane.showInputDialog(this, "Nhập ghi chú duyệt lệnh (tùy chọn):", "Duyệt lệnh rút", JOptionPane.QUESTION_MESSAGE);
        
        if (note != null) {
            boolean success = requestDAO.approveWithdraw(withdrawId, note.trim().isEmpty() ? "Đã duyệt" : note.trim());
            if (success) {
                JOptionPane.showMessageDialog(this, "Duyệt lệnh rút thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                loadData();
            } else {
                JOptionPane.showMessageDialog(this, "Duyệt thất bại. Vui lòng thử lại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void rejectWithdraw() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một lệnh rút để từ chối!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int withdrawId = (int) tableModel.getValueAt(selectedRow, 0);
        String reason = JOptionPane.showInputDialog(this, "Nhập lý do từ chối (bắt buộc):", "Từ chối lệnh rút", JOptionPane.WARNING_MESSAGE);
        
        if (reason != null) {
            if (reason.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Lý do từ chối không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            boolean success = requestDAO.rejectWithdraw(withdrawId, reason);
            if (success) {
                JOptionPane.showMessageDialog(this, "Đã từ chối lệnh rút!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                loadData();
            } else {
                JOptionPane.showMessageDialog(this, "Xử lý thất bại. Vui lòng thử lại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
