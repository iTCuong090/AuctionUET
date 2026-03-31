# 📋 TUẦN 2 & 3 - PART 4: NHIỆM VỤ CỦA ANH (Exceptions, Utilities & Core Bid Logic)

## 1. MỤC TIÊU VÀ ĐỊNH HƯỚNG KIẾN TRÚC
Nhiệm vụ của bạn là bổ sung các công cụ hỗ trợ (Utilities & Exceptions) và chịu trách nhiệm xây dựng class Trung tâm dữ liệu (Singleton) và Logic Đặt Thầu - trái tim của hệ thống.
- **Custom Exceptions:** Giúp báo lỗi chính xác theo nghiệp vụ thay vì cứ dùng Exception chung chung. Các DEV khác sẽ phải bắt (catch) và trả về lỗi phản hồi chính xác.
- **Singleton DataManager:** Do Java chia server ra nhiều kết nối, làm sao để chỉ có 1 cửa sổ vào (DAO files), tránh đụng độ IO Read/Write?
- **BidService:** Giao dịch thầu nhạy cảm nhất. Bạn là người quản lý kết nối giữa Auction DAO và Data hiện tại.

## 2. TÀI LIỆU CẦN HỌC & NGHIÊN CỨU
> 💡 **KHÔNG code ngay.** Hãy đọc hiểu các bài viết này trước.
1. **Java Exceptions (Checked vs Unchecked):** Tại sao `AuctionException` nên ké thừa `RuntimeException` thay vì `Exception`?
2. **Regex trong Java:** Học Class `java.util.regex.Pattern` và `Matcher` để giải mã việc check chuỗi quy chuẩn.
3. **Singleton Design Pattern:** Tìm đọc Refactoring.guru "Singleton". Đặc biệt phải đọc hiểu cụm từ `private static instance`, tại sao constructor lại phải `private`. Hiểu cách Thread-safe singleton (double-checked locking).

---

## 3. CHI TIẾT CÁC LỚP CẦN XÂY DỰNG (GIAI ĐOẠN 1: T3-T4)

> ⚠️ Bắt buộc hoàn thành Calculator (Tuần 1) và STYLE_GUIDE.md vào đầu tuần trước 

### 3.1. `exception/AuctionException.java`
- **Mục đích:** Là ngoại lệ Cơ Sở. Gốc rễ của mọi lỗi báo lên trên.
- **Kế thừa:** `extends RuntimeException` (Không bắt code ở tầng UI phải có block try/catch cực nhọc).
- **Yêu cầu:** Gồm 2 constructor rỗng và `(String message)`.

### 3.2. Cây thư mục Custom Exceptions
Mỗi tên để vào 1 file `.java` độc lập, tất cả đều `extends AuctionException`:
- `InvalidBidException`: Quăng khi giá <= giả max, hoặc vi phạm bước nhảy gia.
- `AuctionClosedException`: Quăng khi tham gia mà cổng chợ đã khoá.
- `UserNotFoundException`: Hữu ích cho phần search user sau này.
- `DuplicateUserException`: Bảo vệ Register của Công.
- `AuthenticationException`: Bảo vệ Login của Công.

### 3.3. `util/ValidationUtils.java`
- **Mục đích:** Công cụ xài chung (Utility) giúp code giảm lặp vòng lặp check.
- **Yêu cầu triển khai:** Class này KHÔNG ĐƯỢC phép `new` hay khởi tạo. Mọi methods đều phải `public static`.
  - `boolean isValidEmail(String email)`: Regex `^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$`
  - `boolean isStrongPassword(String pw)`: Code kiểm tra từ 8 char trở lên. 1 chữ hoa, 1 chữ thường, 1 số, 1 kí tự đặc biệt (có thể regex hoặc chạy vòng for).
  - `boolean isValidUsername(String username)`: Không chứa dấu cách, độ dài từ 3-20 kí tự.
  - `void requireNonEmpty(String value, String fieldName)`: Nếu value là null hoặc `""` thì văng `IllegalArgumentException("Trường " + fieldName + " không được bỏ trống")`.

---

## 4. CHI TIẾT CÁC LỚP CẦN XÂY DỰNG (GIAI ĐOẠN 2: T6-T7)

### 4.1. `service/DataManager.java`
- **Mục đích:** Singleton duy nhất của phía Server nhằm nhốt các cái DAO (IO Files) vào một chỗ được phân phối.
- **Yêu cầu triển khai:**
  - `private static DataManager instance;`
  - Hàm tạo (Constructor) phải set về `private`.
  - `public static synchronized DataManager getInstance()`: Tạo singleton.
  - Bên trong class quản lý các biến `userDAO`, `itemDAO`, `auctionDAO`. Set vào từ phương thức init() tuỳ biến hoặc khi khởi tạo tĩnh (chờ giao tiếp DAO của Cường, Khánh).
  - Trả ra qua getter: `getUserDAO()`, `getItemDAO()`,...

### 4.2. `service/BidService.java`
- **Mục đích:** Trung tâm xử lý cho hành vi Đấu giá. Tách ra khỏi AuctionService vì đây là giao dịch nặng.
- **Yêu cầu triển khai:**
  - `BidTransaction placeBid(String auctionId, Bidder bidder, double amount)`: Quy trình:
    1. Nhận Object Auction từ AuctionDAO (qua ID). Nếu về null quăng lỗi.
    2. Gọi `auction.placeBid(bidder, amount)` (hàm uỷ nhiệm Công viết). Hàm này nếu thấy không hợp lệ sẽ quăng lỗi, để nguyên cho nó dội lỗi tiếp ngược lên.
    3. Trả về đúng giá trị. Tiến hành gọi Action update trở lại xuống đĩa ổ cứng -> `auctionDAO.update(auction)` để lưu nhận kết quả vĩnh viễn và ngăn data bốc hơi.
    4. Return đối tượng `BidTransaction` vừa được sinh ra (bạn có thể móc từ `auction.getBidHistory().getLast()` của Công).

---

## 5. YÊU CẦU TEST BẮT BUỘC (JUNIT 5)
Bạn cần viết test cho code của mình tại thư mục test (`src/test/java/...`).

**1. `ValidationUtilsTest.java`**
- Đút vào hàm Email những chuỗi sai quấy mặn nhạt rỗng bọt, kiểm tra tất cả expect = false.
- Test Password yếu bị từ chối. Password mạnh đúng chuẩn bị accept.

**2. `DataManagerTest.java`**
- Test Singleton Core: Khởi tạo biến A = `DataManager.getInstance()`. Khởi tạo biến B = `DataManager.getInstance()`.
- Dùng hàmf `assertSame(A, B)` trong Unit test để check coi 2 thứ có cùng trỏ về 1 cell bộ nhớ RAM hay không. Nếu không phải cùng 1 gốc thì đánh trượt.

**3. `BidServiceTest.java`**
- Mock Data: Bạn sẽ phải set up mock DB hoặc một Fake DAO do mình tuỳ biễn ra. Tạo 1 sàn đấu.
- Test việc đẩy tiền. Nếu giả sử thấp hơn giá hiện tại, hàm BidService bị đập Exception dội ngược ra từ lòng của model `Auction`. Validate nó văng ném chính xác.
- Test hoàn tất Flow lưu trả, check coi `update()` bên DAO gọi được một lần hông.

> **💡 Lời khuyên khi code:** Các Exception của bạn đứng ở lớp dưới cùng (đáy). Hãy push nó lên thật sớm để Công không bị báo đỏ trên công cụ IDE của anh ấy vì code thiếu exception. Singleton DataManager sẽ hữu ích trong Tuần 4 khi các em làm Concurrency Socket.
