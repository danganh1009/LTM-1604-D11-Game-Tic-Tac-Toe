<h2 align="center">
    <a href="https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin">
    🎓 Faculty of Information Technology (DaiNam University)
    </a>
</h2>
<h2 align="center">
   GAME TIK TAC TOE (CARO 3x3)
</h2>
<div align="center">
    <p align="center">
        <img src="docs/aiotlab_logo.png" alt="AIoTLab Logo" width="170"/>
        <img src="docs/fitdnu_logo.png" alt="AIoTLab Logo" width="180"/>
        <img src="docs/dnu_logo.png" alt="DaiNam University Logo" width="200"/>
    </p>


# <h2 align="center">
#     <a href="https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin">
#     🎓 Faculty of Information Technology (DaiNam University)
#     </a>
# </h2>
# <h2 align="center">
#    GAME TIK TAC TOE (CARO 3x3)
# </h2>
# <div align="center">
#     <p align="center">
#         <img src="docs/aiotlab_logo.png" alt="AIoTLab Logo" width="170"/>
#         <img src="docs/fitdnu_logo.png" alt="AIoTLab Logo" width="180"/>
#         <img src="docs/dnu_logo.png" alt="DaiNam University Logo" width="200"/>
#     </p>
#
# [![AIoTLab](https://img.shields.io/badge/AIoTLab-green?style=for-the-badge)](https://www.facebook.com/DNUAIoTLab)
# [![Faculty of Information Technology](https://img.shields.io/badge/Faculty%20of%20Information%20Technology-blue?style=for-the-badge)](https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin)
# [![DaiNam University](https://img.shields.io/badge/DaiNam%20University-orange?style=for-the-badge)](https://dainam.edu.vn)
#
# </div>
#
# ## 1. Giới thiệu hệ thống
#
# Ứng dụng Game Caro 3x3 sử dụng giao thức TCP cho phép nhiều người chơi thách đấu và thi đấu với nhau qua mạng.
#
# - Client: cung cấp giao diện chơi game và thách đấu.
# - Server: đóng vai trò trung tâm, quản lý kết nối, trận đấu và lịch sử người chơi.
# - Lưu trữ dữ liệu: lịch sử người chơi (thắng/thua) được lưu vào file văn bản (`game_history.txt`).
#
# Client có giao diện Java Swing, cho phép người dùng:
#
# - Đăng nhập/nhập tên người chơi.
# - Chơi cờ Caro 3x3 trực tuyến theo thời gian thực.
# - Xem thông báo khi thắng, thua hoặc hòa.
#
# Giao thức TCP được chọn vì tính đảm bảo truyền tin cậy:
#
# - Không mất gói dữ liệu (các nước đi được truyền đầy đủ, chính xác).
# - Duy trì kết nối liên tục cho đến khi trận đấu kết thúc.
#
# Chức năng chính:
#
# - Server
#   - Kết nối & Quản lý – Port `8000`, hỗ trợ đa luồng
#   - Quản lý trận đấu – Logic game Caro 3x3
#   - Theo dõi thống kê – Thắng/thua của người chơi
#   - Lưu trữ lịch sử – File `game_history.txt`
#   - Quản lý Client – Danh sách người chơi online
#
# - Client
#   - Kết nối đến Server – Giao tiếp qua TCP
#   - Giao diện đồ họa – Java Swing
#   - Thách đấu real-time – Chọn người chơi online
#   - Chơi game Caro – X màu xanh, O màu đỏ
#   - Xem lịch sử – Thống kê người chơi
#   - Làm mới – Reset bàn cờ và trạng thái
#
# Hệ thống:
#
# - Giao thức TCP – `ServerSocket` và `Socket`, đa luồng
# - Lưu trữ dữ liệu – File I/O cho lịch sử người chơi
# - Xử lý lỗi – Thông báo lỗi trong GUI, debug log
# - Logic game – Kiểm tra thắng thua 3x3
#
# Luật chơi tóm tắt:
#
# - Bàn cờ: 3x3 (9 ô)
# - Thắng: khi có 3 quân liên tiếp theo hàng, cột hoặc chéo
# - Hòa: khi bàn cờ đã đầy mà không có ai thắng
# - Ký hiệu: X = màu xanh, O = màu đỏ
#
#
# ## 2. Công nghệ sử dụng
#
# - Ngôn ngữ: Java (JDK 8+ / JDK 17 tested)
# - UI: Java Swing
# - Mạng: TCP sockets (`ServerSocket`, `Socket`)
# - Lưu trữ: File I/O (plain text `game_history.txt`)
# - Hệ điều hành: Cross-platform (Windows / macOS / Linux)
#
#
# ## 3. Hình ảnh các chức năng
#
# (Thay các ảnh thật vào thư mục `docs/` và cập nhật đường dẫn nếu cần)
#
# - Hình 1: Giao diện đăng nhập
# - Hình 2: Giao diện Cờ Caro (3x3)
# - Hình 3: Giao diện bạn thắng
# - Hình 4: Giao diện bạn thua
# - Hình 5: Giao diện lịch sử thắng hoặc thua
#
#
# ## 4. Các bước cài đặt & Chạy ứng dụng
#
# ### 4.1 Yêu cầu hệ thống
#
# - Java Development Kit (JDK): Phiên bản 8 trở lên (JDK 17 đã thử nghiệm)
# - Hệ điều hành: Windows, macOS, hoặc Linux
# - IDE: IntelliJ IDEA, Eclipse, hoặc đơn giản dùng terminal
#
#
# ### 4.2 Hướng dẫn biên dịch & chạy
#
# Mở terminal và điều hướng đến thư mục dự án (ví dụ `D:\LapTrinhMang\BTL`):
#
# ```powershell
# # Di chuyển vào thư mục dự án
# cd D:\LapTrinhMang\BTL
#
# # Biên dịch toàn bộ mã nguồn (nếu bạn chưa biên dịch ra bin/)
# javac -d bin src\\Server\\*.java src\\Client\\*.java
#
# # Hoặc biên dịch từng file nếu cần
# # javac -d bin src\\Server\\CaroServer.java src\\Client\\CaroClient.java
# ```
#
# Khởi động Server (mở một terminal mới):
#
# ```powershell
# # Chạy server (port mặc định: 8000)
# java -cp bin Server.CaroServer
# ```
#
# Khởi động Client (mỗi client mở 1 terminal hoặc chạy từ IDE):
#
# ```powershell
# # Chạy client
# java -cp bin Client.CaroClient
# ```
#
# ### Lưu ý vận hành
#
# - Mỗi client sẽ yêu cầu nhập tên khi kết nối. Nhập tên để tham gia.
# - Server lưu lịch sử trận đấu vào `game_history.txt` (nằm cùng thư mục project).
# - Để xem thống kê, sử dụng nút `Lịch sử` trong giao diện client — client sẽ gửi yêu cầu `get_stats` đến server và nhận trả về thống kê cá nhân.
#
#
# ### Thử nghiệm nhanh
#
# 1. Mở 1 terminal, chạy server.
# 2. Mở 2 terminal khác, chạy 2 client, nhập tên khác nhau.
# 3. Chơi 1 ván để tạo lịch sử.
# 4. Nhấn `Lịch sử` trên client để xem tổng số trận thắng/thua/hòa và tỷ lệ thắng.
#
#
# ---
#
# Nếu bạn muốn, tôi có thể:
#
# - Thêm một file `README` tiếng Anh.
# - Thêm ảnh mẫu vào thư mục `docs/` và nhúng vào README.
# - Tạo GitHub Actions để build và test project tự động khi push.
#
# Xin cho biết nếu muốn tôi thêm những phần này.

</div>