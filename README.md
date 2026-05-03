[FLEXINVEST - Ứng dựng đầu tư sinh lời]
Một ứng dụng Desktop được phát triển bằng ngôn ngữ Java và thư viện đồ họa Swing, phục vụ cho việc quản lý tiền đầu tư sinh lời.

📌 Tổng quan dự án
Dự án được xây dựng dựa trên kiến trúc MVC (Model-View-Controller) giúp tách biệt logic xử lý, giao diện và dữ liệu, dễ dàng bảo trì và mở rộng.

Ngôn ngữ: Java.

Giao diện: Java Swing.

Kiến trúc: MVC Pattern + DAO (Data Access Object).

Cơ sở dữ liệu: [Oracle].

Công cụ phát triển: NetBeans (có nbproject và build.xml).

📂 Cấu trúc thư mục (Source Code)
Dựa trên cấu trúc dự án của bạn:

ConnectDB/: Chứa các lớp thiết lập kết nối với Cơ sở dữ liệu.

Model/: Định nghĩa các đối tượng dữ liệu (Entity classes).

View/: Các giao diện người dùng (JFrame, JPanel).

Controller/: Điều khiển luồng dữ liệu giữa View và Model, xử lý sự kiện.

DAO/: Thực hiện các câu lệnh truy vấn dữ liệu (CRUD).

Utils/: Các công cụ hỗ trợ (Định dạng ngày tháng, kiểm tra dữ liệu, v.v.).

Resources/: Chứa hình ảnh, icons, file cấu hình.

doanjava/: Package chính chứa file thực thi khởi đầu dự án.

🛠 Hướng dẫn cài đặt
Để chạy dự án này trên máy tính cá nhân, bạn thực hiện các bước sau:

Yêu cầu hệ thống:

Java JDK 8 trở lên.

Hệ quản trị CSDL [Oracle].

IDE: NetBeans hoặc IntelliJ IDEA / VS Code (có cài Java Extension).

Thiết lập CSDL:

Thêm ojdbc11-21.5.0.0.jar vào libraries

Import file .sql (nếu có) vào hệ quản trị CSDL của bạn.

Cấu hình thông tin kết nối (User, Password, Port) trong package ConnectDB.

Chạy ứng dụng:

Mở project bằng NetBeans.

Nhấn Clean and Build.

Tìm file có chứa hàm main (thường nằm trong doanjava) và nhấn Run.
