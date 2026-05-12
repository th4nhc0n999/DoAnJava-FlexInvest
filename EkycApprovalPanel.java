package View;

import Controller.EkycController;
import Model.Ekyc;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.net.URL;
import java.util.List;
import javax.imageio.ImageIO;

public class EkycApprovalPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private EkycController ekycController;
    private List<Ekyc> currentList;

    // Detail Panel Components
    private JLabel lblFullNameValue;
    private JLabel lblIdNumberValue;
    private JLabel lblDobValue;
    private JLabel lblFrontImage;
    private JLabel lblBackImage;
    private JLabel lblFaceImage;

    private JButton btnApprove;
    private JButton btnReject;

    private int selectedKycId = -1;

    public EkycApprovalPanel() {
        ekycController = new EkycController();
        initComponents();
        
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
        JLabel lblTitle = new JLabel("Duyệt Hồ Sơ eKYC");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        add(lblTitle, BorderLayout.NORTH);

        // Split Pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.4);
        splitPane.setDividerSize(5);

        // Left: Table
        JPanel leftPanel = new JPanel(new BorderLayout());
        String[] columns = {"ID", "Họ Tên", "Số CCCD", "Ngày gửi"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showDetails();
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        leftPanel.add(scrollPane, BorderLayout.CENTER);
        
        JButton btnRefresh = new JButton("Làm mới danh sách");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRefresh.addActionListener(e -> loadData());
        leftPanel.add(btnRefresh, BorderLayout.SOUTH);

        splitPane.setLeftComponent(leftPanel);

        // Right: Details
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JPanel infoPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        infoPanel.add(new JLabel("Họ Tên:"));
        lblFullNameValue = new JLabel("-");
        lblFullNameValue.setFont(new Font("Segoe UI", Font.BOLD, 14));
        infoPanel.add(lblFullNameValue);

        infoPanel.add(new JLabel("Số CCCD:"));
        lblIdNumberValue = new JLabel("-");
        lblIdNumberValue.setFont(new Font("Segoe UI", Font.BOLD, 14));
        infoPanel.add(lblIdNumberValue);

        infoPanel.add(new JLabel("Ngày sinh:"));
        lblDobValue = new JLabel("-");
        lblDobValue.setFont(new Font("Segoe UI", Font.BOLD, 14));
        infoPanel.add(lblDobValue);

        rightPanel.add(infoPanel, BorderLayout.NORTH);

        // Images Panel
        JPanel imagesPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        imagesPanel.setBorder(BorderFactory.createTitledBorder("Ảnh tải lên (Mặt trước / Mặt sau / Selfie)"));
        
        lblFrontImage = createImageLabel("Mặt trước");
        lblBackImage = createImageLabel("Mặt sau");
        lblFaceImage = createImageLabel("Selfie");

        imagesPanel.add(lblFrontImage);
        imagesPanel.add(lblBackImage);
        imagesPanel.add(lblFaceImage);

        rightPanel.add(imagesPanel, BorderLayout.CENTER);

        // Action Buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnApprove = new JButton("Duyệt (Approve)");
        btnReject = new JButton("Từ chối (Reject)");
        btnApprove.setEnabled(false);
        btnReject.setEnabled(false);

        btnApprove.setBackground(new Color(39, 174, 96));
        btnApprove.setForeground(Color.WHITE);
        btnApprove.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnApprove.setFocusPainted(false);
        btnApprove.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnReject.setBackground(new Color(192, 57, 43));
        btnReject.setForeground(Color.WHITE);
        btnReject.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnReject.setFocusPainted(false);
        btnReject.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnApprove.addActionListener(e -> approve());
        btnReject.addActionListener(e -> reject());

        actionPanel.add(btnApprove);
        actionPanel.add(btnReject);

        rightPanel.add(actionPanel, BorderLayout.SOUTH);

        splitPane.setRightComponent(rightPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private JLabel createImageLabel(String text) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        lbl.setPreferredSize(new Dimension(200, 150));
        return lbl;
    }

    private void loadData() {
        SwingWorker<List<Ekyc>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Ekyc> doInBackground() throws Exception {
                return ekycController.getPendingList();
            }

            @Override
            protected void done() {
                try {
                    currentList = get();
                    tableModel.setRowCount(0);
                    for (Ekyc e : currentList) {
                        tableModel.addRow(new Object[]{
                                e.getKycId(),
                                e.getFullName(),
                                e.getIdNumber(),
                                e.getCreatedAt()
                        });
                    }
                    clearDetails();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(EkycApprovalPanel.this, "Lỗi tải dữ liệu", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void clearDetails() {
        selectedKycId = -1;
        lblFullNameValue.setText("-");
        lblIdNumberValue.setText("-");
        lblDobValue.setText("-");
        lblFrontImage.setIcon(null);
        lblFrontImage.setText("Mặt trước");
        lblBackImage.setIcon(null);
        lblBackImage.setText("Mặt sau");
        lblFaceImage.setIcon(null);
        lblFaceImage.setText("Selfie");
        btnApprove.setEnabled(false);
        btnReject.setEnabled(false);
    }

    private void showDetails() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            clearDetails();
            return;
        }

        Ekyc ekyc = currentList.get(selectedRow);
        selectedKycId = ekyc.getKycId();

        lblFullNameValue.setText(ekyc.getFullName());
        lblIdNumberValue.setText(ekyc.getIdNumber());
        lblDobValue.setText(ekyc.getDateOfBirth() != null ? ekyc.getDateOfBirth().toString() : "-");

        loadImageAsync(ekyc.getFrontImageUrl(), lblFrontImage);
        loadImageAsync(ekyc.getBackImageUrl(), lblBackImage);
        loadImageAsync(ekyc.getFaceImageUrl(), lblFaceImage);

        btnApprove.setEnabled(true);
        btnReject.setEnabled(true);
    }

    private void loadImageAsync(String path, JLabel label) {
        if (path == null || path.isEmpty()) {
            label.setIcon(null);
            label.setText("Không có ảnh");
            return;
        }

        label.setText("Đang tải...");
        label.setIcon(null);

        SwingWorker<ImageIcon, Void> worker = new SwingWorker<>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                try {
                    Image img;
                    if (path.startsWith("http://") || path.startsWith("https://")) {
                        img = ImageIO.read(new java.net.URI(path).toURL());
                    } else {
                        img = ImageIO.read(new File(path));
                    }
                    if (img != null) {
                        int w = label.getWidth() > 0 ? label.getWidth() : 200;
                        int h = label.getHeight() > 0 ? label.getHeight() : 150;
                        Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                        return new ImageIcon(scaled);
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi tải ảnh: " + path);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        label.setText(null);
                        label.setIcon(icon);
                    } else {
                        label.setText("Lỗi ảnh");
                    }
                } catch (Exception e) {
                    label.setText("Lỗi ảnh");
                }
            }
        };
        worker.execute();
    }

    private void approve() {
        if (selectedKycId == -1) return;
        
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn chắc chắn muốn duyệt hồ sơ eKYC này?", "Xác nhận duyệt", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            EkycController.EkycResult res = ekycController.approveEkyc(selectedKycId);
            if (res == EkycController.EkycResult.SUCCESS) {
                JOptionPane.showMessageDialog(this, "Duyệt hồ sơ eKYC thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                loadData();
            } else {
                JOptionPane.showMessageDialog(this, "Duyệt thất bại: " + res, "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void reject() {
        if (selectedKycId == -1) return;

        String reason = JOptionPane.showInputDialog(this, "Nhập lý do từ chối hồ sơ (Bắt buộc):", "Từ chối eKYC", JOptionPane.WARNING_MESSAGE);
        if (reason != null) {
            if (reason.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Lý do từ chối không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            EkycController.EkycResult res = ekycController.rejectEkyc(selectedKycId, reason);
            if (res == EkycController.EkycResult.SUCCESS) {
                JOptionPane.showMessageDialog(this, "Đã từ chối hồ sơ eKYC!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                loadData();
            } else {
                JOptionPane.showMessageDialog(this, "Từ chối thất bại: " + res, "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
