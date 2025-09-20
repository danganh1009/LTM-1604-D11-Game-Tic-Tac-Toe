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

## 💡1. Giới thiệu về hệ thống
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

###  Giao diện Đăng nhập
<img src="https://github.com/user-attachments/assets/030c3d36-b49c-4a04-9a0a-b1209917c216" alt="Giao diện Đăng nhập" width="450" />

###  Giao diện Bàn cờ 3x3
<img src="https://github.com/user-attachments/assets/b207f28e-d40f-44c0-a936-3b85c669e780" alt="Giao diện Bàn cờ 3x3" width="700" />

###  Giao diện Chiến thắng
<img src="https://github.com/user-attachments/assets/c615efd8-5e20-462f-9323-635efc064acd" alt="Giao diện Thắng" width="450" />

###  Giao diện Thất bại
<img src="https://github.com/user-attachments/assets/6a893418-d768-47d7-95bc-2321d239fd01" alt="Giao diện Thua" width="450" />

###  Giao diện Thống kê
<img src="https://github.com/user-attachments/assets/0a46bfec-8f87-4d59-a593-093b41d5619a" alt="Giao diện Thống kê" width="450" />

# ⚙️ 4. Các bước cài đặt & Chạy ứng dụng

## 🛠️ 4.1. Yêu cầu hệ thống
- **Java Development Kit (JDK):** Phiên bản 8 trở lên (khuyến nghị JDK 17/21).  
- **Hệ điều hành:** Windows, macOS, hoặc Linux.  
- **Môi trường phát triển:** IDE (IntelliJ IDEA, Eclipse).  
- **Bộ nhớ:** Tối thiểu 512GB ổ cứng và 16GB RAM.  

---

## 📥 4.2. Các bước cài đặt

### 🧰 Bước 1: Chuẩn bị môi trường
1. **Cài đặt Java**  
   - Dự án yêu cầu **JDK 8+** (JDK 21 cũng chạy được).  
   - Kiểm tra bằng lệnh:  
     ```bash
     java -version
     javac -version
     ```
   - Đảm bảo cả hai lệnh hiển thị phiên bản **>= 8**.  

2. **Cấu trúc thư mục dự án**  
BTLTicTacToe/
└── src/
├── client/
├── server/


### 🏗 Bước 2: Biên dịch mã nguồn
Mở terminal và điều hướng đến thư mục dự án:  
```bash
cd D:\Download\BTLTicTacToe>
```
Biên dịch tất cả file Java:

```bash

javac -d bin src/**/*.java
```
▶️ Bước 3: Chạy ứng dụng
Khởi động Server:

```bash
java -cp bin server.CaroServer
```
Server chạy trên port mặc định (8000) (có thể thay đổi).

Giao diện server hiển thị, sẵn sàng nhận kết nối từ client.

Khởi động Client:

```bash

java -cp bin client.CaroClient
```
Mở terminal mới cho mỗi client muốn tham gia.

Nhập tên người dùng khi được yêu cầu (ví dụ: Phóng, Trường, Long).

Client kết nối tới server và hiển thị giao diện Cờ Caro 3x3.

🚀 Cách Chơi
Đăng nhập: nhập tên người chơi khi mở client.

Xem danh sách online: chọn người chơi khác và bấm Thách Đấu.

Chơi game:

🟩 X = Xanh lá (người chơi 1).

🟥 O = Đỏ (người chơi 2).

🏆 Thắng: khi có 3 ô liên tiếp (ngang / dọc / chéo).

🤝 Hòa: khi bàn cờ đầy mà không ai thắng.

Lịch sử người chơi: mở cửa sổ Thống kê để xem số trận thắng/thua.

Kết thúc: đóng cửa sổ để thoát.




### 📞 5. Liên hệ
 * ## Email: Nguyenhaidangtb2004.tb@gmail.com
 * ## GitHub: Danganh1009













