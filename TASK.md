# Bài tập lớn Lập trình nâng cao: Phát triển hệ thống đấu giá trực tuyến

## 1. Giới thiệu bài tập lớn

Bài tập lớn này nhằm mục đích củng cố và mở rộng kiến thức về lập trình nâng cao thông qua việc phát triển một hệ thống đấu giá trực tuyến. Sinh viên sẽ áp dụng các nguyên lý lập trình hướng đối tượng kết hợp với các kỹ thuật nâng cao để xây dựng một hệ thống có cấu trúc rõ ràng, dễ bảo trì và mở rộng.

Trong quá trình thực hiện, sinh viên được phép tự do thiết kế hệ thống, bổ sung các lớp, phương thức và cấu trúc chương trình phù hợp với ý tưởng của nhóm, miễn là đáp ứng đầy đủ các yêu cầu chức năng. Có thể sử dụng thêm các thư viện Java bên ngoài nếu cần thiết. Khuyến khích áp dụng các công nghệ và kỹ thuật nâng cao nhằm cải thiện chất lượng hệ thống.

**Các quy định chung của bài tập lớn:**
* **Làm việc nhóm:** Sinh viên làm việc theo nhóm 3–4 người/nhóm.
* **Chấm điểm:** Chấm điểm theo nhóm. Sau khi có điểm tổng, các thành viên tự phân chia điểm theo mức độ đóng góp, với điều kiện tổng điểm cá nhân bằng điểm chung của nhóm. Vì vậy, nhóm cần **phân công công việc rõ ràng và minh bạch** về đóng góp của từng thành viên.
* **Sử dụng công cụ:** Sinh viên được phép sử dụng tất cả các công cụ hỗ trợ (Google, GitHub, AI, v.v.) trong quá trình làm bài. Tuy nhiên, bài nộp là sản phẩm chung và cả nhóm chịu trách nhiệm hoàn toàn về nội dung bài nộp.
* **Quản lý mã nguồn:** Sinh viên phải **commit mã nguồn thường xuyên lên GitHub** để chứng minh tiến độ làm bài; không chấp nhận trường hợp chỉ có một commit duy nhất vào thời điểm cuối.
* **Kỷ luật:** Nếu bất kỳ thành viên nào **không hiểu hoặc không thể giải thích bất kỳ phần mã nguồn nào**, toàn bộ nhóm sẽ bị chấm 0 điểm.

---

## 2. Mô tả hệ thống

Hệ thống bidding (đấu giá trực tuyến) là một nền tảng phần mềm cho phép nhiều người dùng cùng tham gia cạnh tranh giá để mua một sản phẩm hoặc dịch vụ trong một khoảng thời gian xác định. Thay vì bán với giá cố định, người bán đưa sản phẩm lên hệ thống và giá bán cuối cùng được xác định thông qua quá trình đấu giá giữa các người mua (bidder). Tham khảo *eBay Auctions*.

---

## 3. Các yêu cầu cụ thể

### 3.1 Chức năng bắt buộc
Nhóm chức năng bắt buộc bao gồm các chức năng cốt lõi cần có để hệ thống đấu giá trực tuyến có thể vận hành đầy đủ. Các chức năng này phục vụ trực tiếp cho người dùng thông qua giao diện đồ họa và đảm bảo quy trình đấu giá diễn ra đúng logic nghiệp vụ.

#### 3.1.1 Quản lý người dùng
Hệ thống cần hỗ trợ quản lý người dùng với các vai trò khác nhau, mỗi vai trò có quyền hạn và chức năng riêng phù hợp với nghiệp vụ đấu giá trực tuyến.
* Đăng ký / đăng nhập tài khoản.
* Các vai trò:
    * **Bidder:** tham gia đấu giá.
    * **Seller:** đăng sản phẩm đấu giá.
    * **Admin:** quản lý hệ thống.

#### 3.1.2 Quản lý sản phẩm đấu giá
Hệ thống phải cho phép người bán quản lý thông tin các sản phẩm được đưa ra đấu giá, đảm bảo thông tin sản phẩm đầy đủ, chính xác và nhất quán trong suốt phiên đấu giá.
* Thêm / sửa / xóa sản phẩm.
* Thông tin sản phẩm:
    * Tên, mô tả, giá khởi điểm.
    * Giá hiện tại cao nhất.
    * Thời gian bắt đầu & kết thúc.

#### 3.1.3 Tham gia đấu giá
Chức năng tham gia đấu giá cho phép người dùng đặt giá và theo dõi diễn biến của phiên đấu giá theo thời gian thực, đồng thời đảm bảo mọi giá đấu đều tuân thủ các quy tắc hợp lệ.
* Người dùng đặt giá cao hơn giá hiện tại.
* Kiểm tra tính hợp lệ của giá đấu.
* Cập nhật người dẫn đầu phiên đấu giá.

#### 3.1.4 Kết thúc phiên đấu giá
Hệ thống cần tự động xử lý việc kết thúc phiên đấu giá dựa trên thời gian đã thiết lập, đồng thời xác định chính xác kết quả và trạng thái cuối cùng của phiên đấu giá.
* Tự động đóng phiên khi hết thời gian.
* Xác định người thắng cuộc.
* Chuyển trạng thái phiên đấu giá: `OPEN` → `RUNNING` → `FINISHED` → `PAID / CANCELED`.

#### 3.1.5 Xử lý lỗi & ngoại lệ
Hệ thống phải có cơ chế phát hiện và xử lý các lỗi và tình huống ngoại lệ phát sinh trong quá trình đấu giá nhằm đảm bảo tính ổn định và trải nghiệm người dùng.
* Đặt giá thấp hơn giá hiện tại.
* Đấu giá khi phiên đã đóng.
* Lỗi dữ liệu, lỗi kết nối.

#### 3.1.6 Giao diện người dùng (GUI)
* Sử dụng JavaFX (khuyến nghị) hoặc Swing.
* Các màn hình chính:
    * Danh sách phiên đấu giá.
    * Chi tiết sản phẩm.
    * Màn hình đấu giá trực tiếp (realtime bidding).
    * Quản lý sản phẩm (dành cho Seller).

### 3.2 Chức năng nâng cao
Ngoài các chức năng bắt buộc, hệ thống có thể được mở rộng với các chức năng nâng cao nhằm tăng tính cạnh tranh, cải thiện trải nghiệm người dùng và thể hiện khả năng phân tích, thiết kế thuật toán của sinh viên. Một số ví dụ như sau:

#### 3.2.1 Auto-Bidding (Đấu giá tự động)
* Người dùng đặt:
    * Giá tối đa (maxBid).
    * Bước giá (increment).
* Hệ thống tự động trả giá thay người dùng khi có bid mới từ đối thủ.
* Logic:
    * So sánh nhiều auto-bid cùng lúc.
    * Không vượt quá maxBid.
    * Ưu tiên theo thời điểm đăng ký auto-bid.
    * Xử lý xung đột bid đồng thời.

#### 3.2.2 Xử lý đấu giá đồng thời (Concurrent Bidding)
* Nhiều bidder đặt giá cùng thời điểm.
* Không được xảy ra:
    * Lost update.
    * Giá bị rollback.
    * Hai người cùng thắng.

#### 3.2.3 Gia hạn phiên đấu giá (Anti-sniping Algorithm)
* Nếu có bid mới trong **X** giây cuối → tự động gia hạn thêm **Y** giây.
* Ví dụ:
    * Kết thúc dự kiến: 20:00:00
    * 19:59:50 có bid → kéo dài đến 20:01:00

#### 3.2.4 Realtime Update (Observer nâng cao)
* Khi có bid mới: toàn bộ client đang xem phiên được cập nhật ngay lập tức.
* Không sử dụng polling liên tục.
* Gợi ý sử dụng:
    * Observer Pattern.
    * Socket / Event-based communication.
    * Thread-safe notify.

#### 3.2.5 Bid History Visualization – Realtime Price Curve
* Hiển thị biểu đồ đường (line chart) giá đấu cao nhất theo thời gian thực.
* Trục X: Thời gian (timestamp).
* Trục Y: Giá đấu hiện tại.
* Mỗi bid hợp lệ → biểu đồ tự động cập nhật (không cần refresh).

### 3.3 Thiết kế hướng đối tượng (OOP)
Hệ thống cần được thiết kế theo hướng đối tượng nhằm đảm bảo tính mô-đun, khả năng mở rộng và dễ bảo trì trong quá trình phát triển và nâng cấp.

#### 3.3.1 Xác định các lớp chính
Gợi ý cấu trúc lớp:
* `Entity` (Abstract Class/Interface): lớp cơ sở.
* `Item` (Abstract, kế thừa Entity): sản phẩm đấu giá.
    * `Electronics`, `Art`, `Vehicle` (kế thừa Item).
* `User` (Abstract, kế thừa Entity).
    * `Bidder`, `Seller`, `Admin` (kế thừa User).
* `Auction`: quản lý trung tâm.
* `BidTransaction`: giao dịch đặt giá.

#### 3.3.2 Áp dụng các nguyên tắc OOP
* **Encapsulation:** private/protected + getter/setter.
* **Inheritance:** phân cấp rõ ràng (`Item` → `Electronics`/`Art` /...).
* **Polymorphism:** override phương thức (ví dụ: `printInfo()`).
* **Abstraction:** abstract class / interface cho hành vi chung.

### 3.4 Thiết kế kiến trúc hệ thống (Networking & MVC)
Kiến trúc hệ thống được thiết kế theo mô hình phân tầng nhằm tách biệt rõ ràng giữa giao diện, xử lý nghiệp vụ và dữ liệu, đồng thời hỗ trợ giao tiếp qua mạng.
* Kiến trúc Client–Server.
* Giao tiếp: REST API hoặc Socket (dữ liệu JSON).
* Phía Client: MVC (JavaFX khuyến nghị + `.fxml`).
* Phía Server: MVC (Controller → Model → DAO/Database).
* Chỉ Server truy cập database.

### 3.5 Tích hợp và triển khai
Quá trình tích hợp và triển khai nhằm đảm bảo hệ thống có thể được xây dựng, kiểm thử và vận hành một cách ổn định trong môi trường thực tế.
* **Build tool:** Maven hoặc Gradle.
* **Chất lượng mã:** refactoring, loại bỏ code trùng, đặt tên tốt.
* **Coding convention:** Google Java Style Guide.
* **Unit Test:** JUnit (logic quan trọng).
* **Git:** commit rõ ràng, thường xuyên (Conventional Commits khuyến nghị).
* **CI/CD:** GitHub Actions + JUnit (khuyến khích).

### 3.6 Design Pattern áp dụng
Việc áp dụng các mẫu thiết kế giúp chuẩn hóa cách giải quyết các bài toán phổ biến trong hệ thống, đồng thời nâng cao chất lượng và khả năng mở rộng của mã nguồn.
* **Singleton:** quản lý kết nối / auction manager.
* **Factory Method:** tạo các loại Item (`Electronics`, `Art`, ...).
* **Observer:** realtime update giá / bid.
* **Strategy / Command** (tuỳ chọn): xử lý các loại bid khác nhau.

---

## Bảng chấm điểm

| Nội dung đánh giá | Điểm | Mức |
| :--- | :---: | :---: |
| **Thiết kế lớp và cây kế thừa** | | |
| Xác định và triển khai các lớp chính (`User`, `Bidder`/`Seller`/`Admin`, `Item`, `Auction`, `BidTransaction`,...) | 0.5 | Bắt buộc |
| Áp dụng đúng các nguyên tắc OOP (Encapsulation, Inheritance, Polymorphism, Abstraction) | 1 | Bắt buộc |
| Áp dụng design pattern phù hợp | 1 | Bắt buộc |
| **Chức năng chính** | | |
| Quản lý người dùng, sản phẩm | 1 | Bắt buộc |
| Chức năng đấu giá | 1 | Bắt buộc |
| Xử lý lỗi & ngoại lệ | 1 | Bắt buộc |
| **Kỹ thuật quan trọng & concurrency** | | |
| Xử lý đấu giá đồng thời an toàn (tránh lost update, rollback, race condition) | 1 | Bắt buộc |
| Realtime update (Observer/Socket): thông báo bid mới cho tất cả client | 0.5 | Bắt buộc |
| **Tích hợp, kiến trúc & chất lượng mã** | | |
| Thiết kế kiến trúc Client–Server rõ ràng | 0.5 | Bắt buộc |
| Áp dụng MVC (JavaFX + FXML cho client, Controller-Model-DAO cho server) | 0.5 | Bắt buộc |
| Sử dụng Maven/Gradle, coding convention tốt, mã nguồn sạch | 0.5 | Bắt buộc |
| Unit Test (JUnit) cho logic quan trọng | 0.5 | Bắt buộc |
| Thiết lập CI/CD cơ bản (GitHub Actions + test tự động) | 0.5 | Bắt buộc |
| **Chức năng nâng cao (tối đa 1.5đ)** | | |
| Auto-Bidding (đấu giá tự động với maxBid, increment, PriorityQueue) | 0.5 | Tùy chọn |
| Gia hạn phiên đấu giá (Anti-sniping) khi bid cuối | 0.5 | Tùy chọn |
| Bid History Visualization: biểu đồ đường giá realtime (line chart) | 0.5 | Tùy chọn |
| Hoặc các tính năng khác sinh viên tự sáng tạo | 0.5 | Tùy chọn |
| **Tổng điểm** | **10+1** | |

---

## 4. Lựa chọn sáng tạo và nâng cao của nhóm (Creative Features)

Để tối ưu hóa trải nghiệm người dùng và đảm bảo tính thực tế của hệ thống, nhóm quyết định triển khai các tính năng nâng cao sau:

### 4.1 Hệ thống Tài chính & Ví điện tử (Wallet System)
Hệ thống sẽ không chỉ là nơi đặt giá ảo mà tích hợp một luồng luân chuyển dòng tiền chuyên nghiệp:
*   **Thuộc tính ví tiền:** Mỗi người dùng (`User`) sẽ có thêm hai thuộc tính:
    *   `balance`: Số dư khả dụng (có thể dùng để đặt cọc hoặc rút ra).
    *   `frozenBalance`: Số dư đang bị đóng băng (dùng để bảo đảm cho các phiên đấu giá đang tham gia).
*   **Cơ chế Nạp/Rút (Mock Payment):** 
    *   Sử dụng API **VietQR** để tạo mã QR nạp tiền động (Số tiền + Nội dung chuyển khoản). Người dùng quét mã để thực hiện lệnh nạp "giả lập" nhưng mang lại trải nghiệm như thật.
    *   Hệ thống Server xử lý việc cộng tiền vào ví sau khi "xác nhận" thanh toán.
    *   Hỗ trợ lệnh rút tiền (`Withdraw`) từ ví hệ thống về tài khoản cá nhân của Seller và Bidder.

### 4.2 Cơ chế Đặt cọc & Đóng băng tiền (Deposit & Frozen Balance)
Đây là tính năng quan trọng để đảm bảo tính nghiêm túc của các phiên đấu giá:
*   **Quy tắc 10%:** Để tham gia đấu giá một sản phẩm, Bidder phải có số dư tối thiểu bằng **10% giá khởi điểm** của sản phẩm đó.
*   **Cơ chế Frozen Balance:** Khi bắt đầu Bid, hệ thống sẽ tự động chuyển 10% số tiền này từ `balance` sang `frozenBalance`.
*   **Lý do cần thiết:** 
    *   Ngăn chặn việc "hét giá ảo" (Shill Bidding) từ các tài khoản không có khả năng thanh toán.
    *   Đảm bảo người thắng cuộc có trách nhiệm với giao dịch. Nếu người thắng không thanh toán, tiền cọc sẽ bị xử lý theo chính sách hệ thống (ví dụ: bồi thường cho Seller hoặc nộp phạt).
    *   Tiền sẽ được giải phóng (`Unfreeze`) và hoàn trả vào `balance` ngay khi phiên đấu giá kết thúc mà Bidder không thắng cuộc.

### 4.3 Tự động đấu giá (Auto-Bidding)
*   Triển khai thuật toán đấu giá tự động giúp người dùng giữ vị thế dẫn đầu mà không cần túc trực 24/7.
*   Sử dụng `PriorityQueue` để xử lý thứ tự ưu tiên của các bước giá tự động khi có nhiều người cùng cài đặt Auto-bid.

### 4.4 Biểu đồ biến động giá (Realtime Price Chart)
*   Sử dụng thư viện biểu đồ (JavaFX Charts) để trực quan hóa lịch sử đặt giá.
*   Cập nhật dữ liệu theo thời gian thực (Real-time) mỗi khi có một Bid hợp lệ được ghi nhận.

### 4.5 Trí tuệ nhân tạo (AI Auction Assistant - Tùy chọn)
*   Tích hợp **Google Gemini API** để hỗ trợ Seller:
    *   **AI Appraisal:** Nhận diện và thẩm định giá trị sản phẩm qua mô tả/hình ảnh.
    *   **Smart Description:** Tự động viết lại mô tả sản phẩm chuyên nghiệp, hấp dẫn để tăng tỷ lệ chốt đơn.

### 4.6 Hệ thống tương tác đa tầng (Public & Private Comments)
Để tăng tính minh bạch và hỗ trợ giao tiếp giữa các bên, hệ thống tích hợp cơ chế bình luận linh hoạt:
*   **Public Comment (Q&A):** Cho phép mọi người đặt câu hỏi công khai trên trang chi tiết sản phẩm. Seller trả lời công khai để các Bidder khác cùng tham khảo thông tin.
*   **Private Comment (Seller-Bidder DM):** Cơ chế nhắn tin riêng tư giữa Bidder và Seller để trao đổi về vận chuyển, địa chỉ hoặc thương lượng chi tiết.
*   **AI Moderation (Tùy chọn):** Sử dụng AI để tự động lọc bỏ các bình luận rác hoặc ngôn từ không phù hợp.