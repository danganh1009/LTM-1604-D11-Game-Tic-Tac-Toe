 <h2 align="center">
    <a href="https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin">
    🎓 Faculty of Information Technology (DaiNam University)
    </a>
</h2>
<h2 align="center">
   GAME TIC TAC TOE (CARO 3x3) SỬ DỤNG GIAO THỨC TCP
</h2>
<div align="center">
    <p align="center">
        <img src="docs/aiotlab_logo.png" alt="AIoTLab Logo" width="170"/>
        <img src="docs/fitdnu_logo.png" alt="AIoTLab Logo" width="180"/>
        <img src="docs/dnu_logo.png" alt="DaiNam University Logo" width="200"/>
    </p>

[![AIoTLab](https://img.shields.io/badge/AIoTLab-green?style=for-the-badge)](https://www.facebook.com/DNUAIoTLab)
[![Faculty of Information Technology](https://img.shields.io/badge/Faculty%20of%20Information%20Technology-blue?style=for-the-badge)](https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin)
[![DaiNam University](https://img.shields.io/badge/DaiNam%20University-orange?style=for-the-badge)](https://dainam.edu.vn)


</div>
---

## 💡1. Tổng quan về hệ thống
Ứng dụng "Game Caro 3x3" là một trò chơi cờ Caro cổ điển, được phát triển để người chơi có thể thách đấu và thi đấu trực tuyến. Hệ thống được xây dựng theo mô hình **client-server** sử dụng giao thức **TCP**, đảm bảo trải nghiệm chơi mượt mà và đáng tin cậy.

### 💻 Thành phần chính

#### Server
* **Lắng nghe kết nối:** Chạy trên cổng mặc định `8000`.
* **Hỗ trợ đa luồng:** Xử lý đồng thời nhiều kết nối client.
* **Quản lý trận đấu:** Ghép cặp người chơi, quản lý lượt đi và xác định kết quả (thắng/thua/hòa).
* **Lưu trữ dữ liệu:** Ghi nhận và cập nhật lịch sử người chơi (thống kê thắng/thua) vào file `player_history.txt`.
* **Quản lý người chơi:** Duy trì danh sách người chơi online và điều phối lời mời thách đấu.

#### Client
* **Giao diện:** Xây dựng bằng Java Swing, thân thiện và dễ sử dụng.
* **Tính năng:**
    * Đăng nhập bằng tên.
    * Xem danh sách người chơi trực tuyến.
    * Gửi và nhận lời mời thách đấu.
    * Hiển thị bàn cờ 3x3 và cho phép chơi theo thời gian thực.
    * Nhận thông báo kết quả ngay lập tức trên màn hình.
    * Xem lịch sử thi đấu và làm mới bàn cờ.

---

### 🌐 Giao thức & Kết nối

* Sử dụng giao thức **TCP** thông qua `ServerSocket` (server) và `Socket` (client).
* **Lý do chọn TCP:**
    * **Đáng tin cậy:** Đảm bảo mọi nước đi đều được truyền chính xác và không bị mất gói tin.
    * **Duy trì kết nối:** Giữ kết nối liên tục để đồng bộ hóa trạng thái trận đấu giữa hai người chơi.

---

### 💾 Lưu trữ dữ liệu

* **Dữ liệu:** Lịch sử người chơi (số trận thắng và thua) được lưu trữ trong file văn bản `player_history.txt`.
* **Cập nhật:** Server chịu trách nhiệm cập nhật file này sau mỗi trận đấu kết thúc.

---

### ♟️ Luật chơi (Tóm tắt)

* **Bàn cờ:** Kích thước 3x3.
* **Người chơi:** Lần lượt đánh dấu các ô trống bằng **X** (màu xanh 🟢) và **O** (màu đỏ 🔴).
* **Chiến thắng:** Đặt được 3 ký hiệu liên tiếp trên một hàng, cột, hoặc đường chéo.
* **Hòa:** Khi tất cả 9 ô đều đã được đánh dấu mà không có ai thắng.


### 📌 Ví dụ bàn cờ thắng:
*<img width="480" height="609" alt="image" src="https://github.com/user-attachments/assets/aff2ffe9-cee5-49e4-a152-fb89c0488096" />
## 🔧 2. Công nghệ sử dụng
[![Java](https://img.shields.io/badge/Java-17-orange)](https://www.oracle.com/java/)
[![JDK](https://img.shields.io/badge/JDK-17-blueviolet)](https://adoptium.net/)
[![Language](https://img.shields.io/badge/Language-Java-green)](https://www.java.com/)
[![TCP](https://img.shields.io/badge/Protocol-TCP-9cf)](https://en.wikipedia.org/wiki/Transmission_Control_Protocol)
[![Socket](https://img.shields.io/badge/Socket-Server/Socket-blue)](https://docs.oracle.com/en/java/)
[![Swing](https://img.shields.io/badge/UI-Swing-orange)](https://docs.oracle.com/en/java/)

- Ngôn ngữ: Java 17
- GUI: Java Swing (`javax.swing`, `java.awt`)
- Giao tiếp mạng: TCP sockets (`ServerSocket`, `Socket`)
- Port mặc định: `8000`
- Lưu trữ lịch sử: file văn bản `game_history.txt` (được ghi bởi server)

## 📸 3. Hình ảnh các chức năng

## 📸 Hình ảnh các chức năng

### 1\. Giao diện Đăng nhập
<img src="https://github.com/user-attachments/assets/030c3d36-b49c-4a04-9a0a-b1209917c216" alt="Giao diện Đăng nhập" width="450" />

### 2\. Giao diện Bàn cờ 3x3
<img src="https://github.com/user-attachments/assets/b207f28e-d40f-44c0-a936-3b85c669e780" alt="Giao diện Bàn cờ 3x3" width="700" />

### 3\. Giao diện Chiến thắng
<img src="https://github.com/user-attachments/assets/c615efd8-5e20-462f-9323-635efc064acd" alt="Giao diện Thắng" width="450" />

### 4\. Giao diện Thất bại
<img src="https://github.com/user-attachments/assets/6a893418-d768-47d7-95bc-2321d239fd01" alt="Giao diện Thua" width="450" />

### 5\. Giao diện Thống kê
<img src="https://github.com/user-attachments/assets/0a46bfec-8f87-4d59-a593-093b41d5619a" alt="Giao diện Thống kê" width="450" />
## ⚙️ 4. Các bước cài đặt & Chạy ứng dụng

### 🛠️ 4.1. Yêu cầu hệ thống

* **Java Development Kit (JDK):** Phiên bản **Java 9 trở lên** (khuyến nghị **Java 17 LTS**).
    * *Lưu ý:* Dự án sử dụng `module-info.java`, do đó cần JDK 9+ để biên dịch và chạy.
* **Môi trường phát triển:** Eclipse IDE.
* **Hệ điều hành:** Windows, macOS, hoặc Linux.

---

### 📥 4.2. Thiết lập dự án trong Eclipse

1.  **Mở Eclipse và Import dự án:**
    * Mở Eclipse IDE.
    * Trên thanh menu, chọn **File > Import...**
    * Trong cửa sổ mới, chọn **General > Existing Projects into Workspace** rồi nhấn **Next**.
    * Chọn **Browse...** để tìm đến thư mục gốc của dự án và nhấn **Finish**.

2.  **Kiểm tra cấu hình JDK:**
    * Đảm bảo dự án đã được cấu hình với **JDK 9 trở lên**.
    * Nhấp chuột phải vào dự án trong **Package Explorer**, chọn **Properties**.
    * Kiểm tra trong mục **Java Build Path** hoặc **Java Compiler** để đảm bảo đúng phiên bản JDK được sử dụng.

---

### ▶️ 4.3. Chạy ứng dụng

Khi chạy bằng Eclipse, IDE sẽ tự động biên dịch mã nguồn cho bạn. Bạn chỉ cần chạy các lớp chính theo thứ tự.

1.  **Khởi động Server:**
    * Trong cửa sổ **Package Explorer**, tìm đến file `GameServer.java` (nằm trong thư mục `src/server`).
    * Nhấp chuột phải vào file này và chọn **Run As > Java Application**.
    * Server sẽ khởi động và thông báo sẽ xuất hiện trong cửa sổ **Console** của Eclipse.

2.  **Khởi động Client:**
    * Trong cửa sổ **Package Explorer**, tìm đến file `GameClient.java` (nằm trong thư mục `src/client`).
    * Nhấp chuột phải vào file này và chọn **Run As > Java Application**.
    * Một cửa sổ giao diện của client sẽ bật lên.
    * Để chơi với nhiều người, bạn có thể lặp lại bước này để mở thêm các cửa sổ client mới.

---

### 🚀 4.4. Cách chơi (tóm tắt)

* **Đăng nhập:** Nhập tên người chơi khi khởi động client.
* **Trận đấu:**
    * **X** = Người chơi 1 (màu xanh 🔵)
    * **O** = Người chơi 2 (màu đỏ 🔴)
* **Kết quả:**
    * 🏆 **Thắng:** Khi có 3 ô liên tiếp trên một hàng, cột hoặc đường chéo.
    * 🤝 **Hòa:** Bàn cờ đầy mà không có ai thắng.
* **Lịch sử:** Dữ liệu thắng/thua được ghi vào file `player_history.txt`. Client có nút "Thống kê" để xem.

---

### ⚠️ Lưu ý vận hành

* **Port:** Port mặc định là `8000`. Nếu gặp lỗi kết nối, hãy kiểm tra cài đặt tường lửa (firewall).
* **Quyền ghi:** Đảm bảo thư mục chạy server có quyền ghi để tạo và cập nhật file `player_history.txt`.


### 📞 5. Liên hệ
 * ## Email: Nguyenhaidangtb2004.tb@gmail.com
 * ## GitHub: Danganh1009






